(ns sri.parser
  "Parser for the Ruby programming language.

   Implements a recursive descent parser that converts tokens into
   an ECS-based AST representation."
  (:require [sri.tokenizer :as tokenizer])
  (:require [clojure.string]))

(defonce ^:private entity-counter (atom 0))

(defn create-entity-id
  "Generate a unique entity ID for AST nodes.
   Uses incremental integers for better performance and memory usage."
  []
  (swap! entity-counter inc))

(defn reset-entity-counter!
  "Reset the entity counter (useful for testing)."
  []
  (reset! entity-counter 0))

(defn create-ast
  "Create an empty ECS AST structure."
  []
  {:entities #{}
   :components {:node-type {}
                :value {}
                :parent {}
                :children {}
                :position {}}})

(defn add-entity
  "Add a new entity to the AST."
  [ast entity-id]
  (update ast :entities conj entity-id))

(defn set-component
  "Set a component value for an entity."
  [ast entity-id component-type value]
  (assoc-in ast [:components component-type entity-id] value))

;; Generic component accessor
(defn get-component
  "Fast accessor for any AST component."
  [ast entity-id component-type]
  (get-in ast [:components component-type entity-id]))

(defn get-components
  "Get multiple components as a map for destructuring."
  [ast entity-id component-keys]
  (into {} (map (fn [k] [k (get-component ast entity-id k)]) component-keys)))

;; Macro to define multiple component accessor functions
(defmacro def-component-accessors
  "Define fast accessor functions for multiple component types."
  [& component-types]
  `(do ~@(for [comp-type component-types]
           (let [fn-name (symbol (str "get-" (name comp-type)))]
             `(defn ~fn-name
                ~(str "Fast accessor for " (name comp-type) " component.")
                [~'ast ~'entity-id]
                (get-component ~'ast ~'entity-id ~comp-type))))))

;; Define all the component accessors (avoiding clojure.core conflicts)
(def-component-accessors :node-type :value :operator :left :right
                        :variable :name :parameters :body :receiver
                        :arguments :condition :then :else :statements :class :type
                        :block :block-params :block-body)

;; Special case for :method to avoid conflict with clojure.core/get-method
(defn get-method-name
  "Fast accessor for method component."
  [ast entity-id]
  (get-component ast entity-id :method))

(defn create-node
  "Create a new AST node and return [updated-ast entity-id]."
  [ast node-type & {:as options}]
  (let [entity-id (create-entity-id)
        updated-ast (-> ast
                        (add-entity entity-id)
                        (set-component entity-id :node-type node-type))
        final-ast (reduce-kv
                    (fn [ast k v] (set-component ast entity-id k v))
                    updated-ast
                    options)]
    [final-ast entity-id]))

(defrecord ParseState [tokens pos ast])

(defn create-parse-state
  "Create initial parser state from tokens."
  [tokens]
  (->ParseState tokens 0 (create-ast)))

(defn current-token
  "Get the current token without consuming it."
  [state]
  (when (< (:pos state) (count (:tokens state)))
    (nth (:tokens state) (:pos state))))

(defn peek-token
  "Look ahead at a token without consuming it."
  [state & [offset]]
  (let [pos (+ (:pos state) (or offset 1))]
    (when (< pos (count (:tokens state)))
      (nth (:tokens state) pos))))

(defn consume-token
  "Consume and return the current token, advancing position."
  [state]
  (let [token (current-token state)]
    [token (update state :pos inc)]))

(defn match-token?
  "Check if current token matches expected type and optionally value."
  [state expected-type & [expected-value]]
  (when-let [token (current-token state)]
    (and (= (:type token) expected-type)
         (or (nil? expected-value)
             (= (:value token) expected-value)))))

(defn expect-token
  "Consume a token, throwing error if it doesn't match expectation."
  [state expected-type & [expected-value]]
  (let [token (current-token state)]
    (if (match-token? state expected-type expected-value)
      (consume-token state)
      (throw (ex-info (str "Expected " expected-type
                          (when expected-value (str " '" expected-value "'"))
                          ", got " (if token
                                    (str (:type token) " '" (:value token) "'")
                                    "EOF"))
                     {:expected-type expected-type
                      :expected-value expected-value
                      :actual-token token
                      :position (when token {:line (:line token) :column (:column token)})})))))

(defn parse-integer-literal
  "Parse an integer literal."
  [state]
  (when (match-token? state :integer)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :integer-literal
                                          :value (try
                                                   (Integer/parseInt (:value token))
                                                   (catch NumberFormatException _
                                                     (BigInteger. (:value token) 10)))
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-float-literal
  "Parse a float literal."
  [state]
  (when (match-token? state :float)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :float-literal
                                          :value (Double/parseDouble (:value token))
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-rational-literal
  "Parse a rational literal."
  [state]
  (when (match-token? state :rational)
    (let [[token new-state] (consume-token state)
          number-str (:value token)
          [numerator denominator] (if (clojure.string/includes? number-str ".")
                                    ;; Handle float rationals like "1.0" -> 1/1, "0.5" -> 1/2
                                    (let [parts (clojure.string/split number-str #"\.")
                                          integer-part (first parts)
                                          decimal-part (second parts)
                                          decimal-places (count decimal-part)
                                          ;; Use BigInteger for large denominators
                                          denominator (bigint (Math/pow 10 decimal-places))
                                          ;; Combine integer and decimal parts: "1.23" -> 123
                                          combined-str (str integer-part decimal-part)
                                          numerator (if (= combined-str "")
                                                      0
                                                      (BigInteger. combined-str 10))]
                                      [numerator denominator])
                                    ;; Handle integer rationals like "3" -> 3/1
                                    [(try
                                       (Integer/parseInt number-str)
                                       (catch NumberFormatException _
                                         (BigInteger. number-str 10))) 1])
          [new-ast entity-id] (create-node (:ast new-state) :rational-literal
                                          :numerator numerator
                                          :denominator denominator
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-complex-literal
  "Parse a complex literal."
  [state]
  (when (match-token? state :complex)
    (let [[token new-state] (consume-token state)
          number-str (:value token)
          imaginary (if (clojure.string/includes? number-str ".")
                      ;; Handle float complex literals like "5.0" -> 5.0i
                      (Double/parseDouble number-str)
                      ;; Handle integer complex literals like "5" -> 5i
                      (try
                        (Integer/parseInt number-str)
                        (catch NumberFormatException _
                          (BigInteger. number-str 10))))
          [new-ast entity-id] (create-node (:ast new-state) :complex-literal
                                          :real 0
                                          :imaginary imaginary
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-string-literal
  "Parse a string literal."
  [state]
  (when (match-token? state :string)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :string-literal
                                          :value (:value token)
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-interpolated-string
  "Parse an interpolated string with #{} expressions."
  [state]
  (when (match-token? state :interpolated-string)
    (let [[token new-state] (consume-token state)
          parts (:value token)
          [new-ast entity-id] (create-node (:ast new-state) :interpolated-string
                                          :parts parts
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-boolean-literal
  "Parse a boolean literal (true/false)."
  [state]
  (when (and (match-token? state :keyword)
             (contains? #{"true" "false"} (:value (current-token state))))
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :boolean-literal
                                          :value (= (:value token) "true")
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-nil-literal
  "Parse a nil literal."
  [state]
  (when (match-token? state :keyword "nil")
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :nil-literal
                                          :value nil
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-self-literal
  "Parse a self literal."
  [state]
  (when (match-token? state :keyword "self")
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :self-literal
                                          :value :self
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(defn parse-word-array-literal
  "Parse a word array literal %w(a b c) or %W(a #{expr} c)."
  [state]
  (cond
    (match-token? state :word-array)
    (let [[token state-after-token] (consume-token state)
          words (:value token)
          [new-ast entity-id] (create-node (:ast state-after-token) :word-array-literal
                                         :words words
                                         :interpolated false
                                         :position {:line (:line token) :column (:column token)})]
      [(assoc state-after-token :ast new-ast) entity-id])

    (match-token? state :interpolated-word-array)
    (let [[token state-after-token] (consume-token state)
          content (:value token)
          [new-ast entity-id] (create-node (:ast state-after-token) :word-array-literal
                                         :content content
                                         :interpolated true
                                         :position {:line (:line token) :column (:column token)})]
      [(assoc state-after-token :ast new-ast) entity-id])))

(defn parse-symbol-literal
  "Parse a symbol literal."
  [state]
  (when (match-token? state :symbol)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :symbol-literal
                                          :value (keyword (:value token))
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(declare parse-expression parse-statement parse-primary parse-case-statement)

(defn parse-argument-list
  "Parse a comma-separated argument list enclosed in parentheses."
  [state]
  (if (match-token? state :operator "(")
    (let [[_ state-after-paren] (consume-token state)]
      (if (match-token? state-after-paren :operator ")")
        (let [[_ final-state] (consume-token state-after-paren)]
          [final-state []])
        (loop [current-state state-after-paren
               args []
               last-token (current-token state-after-paren)]
          (let [[state-after-arg arg-id] (parse-expression current-state)
                new-args (conj args arg-id)
                current-token (current-token state-after-arg)]
            (cond
              (match-token? state-after-arg :operator ",")
              (let [[comma-token state-after-comma] (consume-token state-after-arg)]
                (recur state-after-comma new-args comma-token))

              (match-token? state-after-arg :operator ")")
              (let [[_ final-state] (consume-token state-after-arg)]
                [final-state new-args])

              :else
              (let [error-token (or current-token last-token)]
                (throw (ex-info "Expected ',' or ')' in argument list"
                               {:token error-token
                                :position (when error-token {:line (:line error-token) :column (:column error-token)})}))))))))
    [state []]))

(defn parse-statements-until
  "Parse statements until a specific token is encountered."
  [state token-type token-value]
  (loop [current-state state
         statements []]
    (if (match-token? current-state token-type token-value)
      [statements current-state]
      ;; Check if we've reached end of tokens
      (if (nil? (current-token current-state))
        (throw (ex-info (str "Unexpected end of input while looking for " token-value)
                       {:expected token-value}))
        (let [[state-after-stmt stmt-id] (parse-statement current-state)
              new-statements (conj statements stmt-id)]
          (recur state-after-stmt new-statements))))))

(defn parse-block-parameters
  "Parse block parameters between | and |."
  [state]
  (loop [current-state state
         params []]
    (let [current-token (current-token current-state)]
      (cond
        ;; Found closing | - end of parameters
        (match-token? current-state :operator "|")
        [current-state params]

        ;; Found identifier - add to parameters
        (and current-token (= (:type current-token) :identifier))
        (let [[token next-state] (consume-token current-state)]
          (recur next-state (conj params (:value token))))

        ;; Found comma - skip it
        (match-token? current-state :operator ",")
        (let [[_ next-state] (consume-token current-state)]
          (recur next-state params))

        ;; Unexpected token or end of input
        :else
        (throw (ex-info "Expected parameter name or closing |"
                       {:token current-token}))))))

(defn skip-separators
  "Skip over newline and semicolon tokens in the input stream."
  [state]
  (loop [current-state state]
    (cond
      (match-token? current-state :newline)
      (recur (second (consume-token current-state)))

      (match-token? current-state :operator ";")
      (recur (second (consume-token current-state)))

      :else
      current-state)))

(defn parse-block-body
  "Parse block body statements until closing } or end keyword."
  [state]
  (loop [current-state state
         statements []]
    (let [state-skip-newlines (skip-separators current-state)
          current-token (current-token state-skip-newlines)]
      (cond
        ;; Found closing } - end of block body
        (match-token? state-skip-newlines :operator "}")
        [state-skip-newlines statements]

        ;; Found 'end' keyword - end of block body
        (match-token? state-skip-newlines :keyword "end")
        [state-skip-newlines statements]

        ;; End of input without proper terminator
        (nil? current-token)
        (throw (ex-info "Unexpected end of input in block body" {}))

        ;; Parse a statement
        :else
        (let [[state-after-stmt stmt-id] (parse-statement state-skip-newlines)]
          (recur state-after-stmt (conj statements stmt-id)))))))

(defn parse-ruby-block
  "Parse Ruby block literal: { |params| body } or do |params| body end"
  [state]
  (cond
    ;; Handle { } blocks
    (match-token? state :operator "{")
    (let [[open-token state-after-open] (consume-token state)
          state-skip-newlines (skip-separators state-after-open)]
      ;; Check if this is a block (has |) or hash (doesn't have | immediately)
      (if (match-token? state-skip-newlines :operator "|")
        ;; This is a block - parse parameters and body
        (try
          (let [;; Skip the opening |
                [_ state-after-pipe] (consume-token state-skip-newlines)
                ;; Parse parameters until closing |
                [state-after-params params] (parse-block-parameters state-after-pipe)
                ;; Skip the closing |
                [_ state-after-closing-pipe] (consume-token state-after-params)
                ;; Parse block body until closing }
                [state-after-body body-statements] (parse-block-body state-after-closing-pipe)
                ;; Skip the closing }
                [_ final-state] (consume-token state-after-body)
                ;; Create the block node
                [new-ast entity-id] (create-node (:ast final-state) :block
                                               :block-params params
                                               :block-body body-statements
                                               :position {:line (:line open-token) :column (:column open-token)})]
            [(assoc final-state :ast new-ast) entity-id])
          (catch Exception e
            (binding [*out* *err*]
              (println "DEBUG: Error parsing { } block:" (.getMessage e))
              (flush))
            nil))
        ;; Not a block, return nil so parse-hash-literal can handle it
        nil))

    ;; Handle do end blocks
    (match-token? state :keyword "do")
    (let [[do-token state-after-do] (consume-token state)
          state-skip-newlines (skip-separators state-after-do)]
      (try
        (if (match-token? state-skip-newlines :operator "|")
          ;; Block with parameters
          (let [;; Skip the opening |
                [_ state-after-pipe] (consume-token state-skip-newlines)
                ;; Parse parameters until closing |
                [state-after-params params] (parse-block-parameters state-after-pipe)
                ;; Skip the closing |
                [_ state-after-closing-pipe] (consume-token state-after-params)
                ;; Parse block body until 'end'
                [state-after-body body-statements] (parse-block-body state-after-closing-pipe)
                ;; Skip the 'end'
                [_ final-state] (expect-token state-after-body :keyword "end")
                ;; Create the block node
                [new-ast entity-id] (create-node (:ast final-state) :block
                                               :block-params params
                                               :block-body body-statements
                                               :position {:line (:line do-token) :column (:column do-token)})]
            [(assoc final-state :ast new-ast) entity-id])
          ;; Block without parameters
          (let [;; Parse block body until 'end'
                [state-after-body body-statements] (parse-block-body state-skip-newlines)
                ;; Skip the 'end'
                [_ final-state] (expect-token state-after-body :keyword "end")
                ;; Create the block node
                [new-ast entity-id] (create-node (:ast final-state) :block
                                               :block-params []
                                               :block-body body-statements
                                               :position {:line (:line do-token) :column (:column do-token)})]
            [(assoc final-state :ast new-ast) entity-id]))
        (catch Exception e
          (binding [*out* *err*]
            (println "DEBUG: Error parsing do/end block:" (.getMessage e))
            (flush))
          nil)))

    ;; Not a block at all
    :else nil))

(defn parse-method-call
  "Parse a method call, either standalone or with receiver."
  [state receiver-id method-name token]
  (let [[state-after-args args] (parse-argument-list state)
        ;; Check for block after arguments (either { } or do end)
        [final-state block-id] (if (or (match-token? state-after-args :operator "{")
                                       (match-token? state-after-args :keyword "do"))
                                 (if-let [block-result (parse-ruby-block state-after-args)]
                                   block-result
                                   [state-after-args nil])
                                 [state-after-args nil])
        [new-ast entity-id] (create-node (:ast final-state) :method-call
                                       :receiver receiver-id
                                       :method method-name
                                       :arguments args
                                       :block block-id
                                       :position {:line (:line token) :column (:column token)})]
    (when block-id
      (binding [*out* *err*]
        (println "DEBUG: Created method call entity" entity-id "for" method-name "with block" block-id)
        (flush)))
    [(assoc final-state :ast new-ast) entity-id]))

(defn parse-postfix-expressions
  "Parse postfix expressions like method calls with dot notation."
  [state primary-id]
  (loop [current-state state
         current-id primary-id]
    (cond
        (match-token? current-state :operator ".")
        (let [[_ state-after-dot] (consume-token current-state)]
          (if (or (match-token? state-after-dot :identifier)
                  (match-token? state-after-dot :keyword))
            (let [[method-token state-after-method] (consume-token state-after-dot)
                  [final-state call-id] (parse-method-call state-after-method
                                                         current-id
                                                         (:value method-token)
                                                         method-token)]
              (recur final-state call-id))
            (throw (ex-info "Expected method name after '.'"
                           {:token (current-token state-after-dot)}))))

        (match-token? current-state :operator "[")
        (let [[_ state-after-bracket] (consume-token current-state)
              [state-after-index index-id] (parse-expression state-after-bracket)
              [_ state-after-close] (expect-token state-after-index :operator "]")
              [new-ast access-id] (create-node (:ast state-after-close) :array-access
                                             :receiver current-id
                                             :index index-id
                                             :position {:line (:line (current-token current-state))
                                                       :column (:column (current-token current-state))})]
          (recur (assoc state-after-close :ast new-ast) access-id))

        :else
        [current-state current-id])))

(defn parse-hash-key-with-shorthand
  "Parse a hash key, handling symbol shorthand syntax (key: value)"
  [state]
  (let [start-pos (:pos state)]
    ;; Try to parse as identifier followed by :
    (if (and (match-token? state :identifier)
             (match-token? (update state :pos inc) :operator ":"))
      ;; Symbol shorthand: convert identifier to symbol
      (let [[id-token state-after-id] (consume-token state)
            [new-ast symbol-id] (create-node (:ast state-after-id) :symbol-literal
                                           :value (keyword (:value id-token))
                                           :position {:line (:line id-token) :column (:column id-token)})]
        [(assoc state-after-id :ast new-ast) symbol-id])
      ;; Regular expression parsing
      (parse-expression state))))

(defn parse-inline-hash-in-array
  "Parse an inline hash in array context like [key => value, key2: value2]"
  [first-key-id value-state]
  ;; Use the already parsed first key, then continue with hash parsing
  (let [
        ;; Consume the => or : operator
        [arrow-token state-after-arrow] (cond
                                          (match-token? value-state :operator ":")
                                          (consume-token value-state)
                                          (match-token? value-state :operator "=>")
                                          (consume-token value-state)
                                          :else
                                          (throw (ex-info "Expected ':' or '=>' in hash literal"
                                                         {:token (current-token value-state)})))
        ;; Parse the first value
        [state-after-first-value first-value-id] (parse-expression state-after-arrow)
        ;; Start collecting pairs
        initial-pairs [[first-key-id first-value-id]]]

    ;; Continue parsing pairs until we hit ]
    (loop [current-state state-after-first-value
           pairs initial-pairs]
      (cond
        (match-token? current-state :operator "]")
        (let [[new-ast entity-id] (create-node (:ast current-state) :hash-literal
                                             :pairs pairs
                                             :position {:line (:line arrow-token) :column (:column arrow-token)})]
          [(assoc current-state :ast new-ast) entity-id])

        (match-token? current-state :operator ",")
        (let [[_ state-after-comma] (consume-token current-state)]
          (if (match-token? state-after-comma :operator "]")
            ;; Trailing comma
            (let [[new-ast entity-id] (create-node (:ast state-after-comma) :hash-literal
                                                 :pairs pairs
                                                 :position {:line (:line arrow-token) :column (:column arrow-token)})]
              [(assoc state-after-comma :ast new-ast) entity-id])
            ;; Parse next key-value pair
            (let [[state-after-key key-id] (parse-hash-key-with-shorthand state-after-comma)
                  [arrow-token state-after-arrow] (cond
                                                    (match-token? state-after-key :operator ":")
                                                    (consume-token state-after-key)
                                                    (match-token? state-after-key :operator "=>")
                                                    (consume-token state-after-key)
                                                    :else
                                                    (throw (ex-info "Expected ':' or '=>' in hash literal"
                                                                   {:token (current-token state-after-key)})))
                  [state-after-value value-id] (parse-expression state-after-arrow)
                  new-pairs (conj pairs [key-id value-id])]
              (recur state-after-value new-pairs))))

        :else
        (throw (ex-info "Expected ',' or ']' in hash literal"
                       {:token (current-token current-state)}))))))

(defn parse-array-literal
  "Parse array literal [1, 2, 3]"
  [state]
  (when (match-token? state :operator "[")
    (let [[open-bracket state-after-open] (consume-token state)
          [elements final-state] (if (match-token? state-after-open :operator "]")
                                   [[] state-after-open]
                                   (loop [current-state state-after-open
                                          elements []]
                                     (let [[state-after-element element-id] (parse-expression current-state)
                                           new-elements (conj elements element-id)]
                                       (cond
                                         (match-token? state-after-element :operator "]")
                                         [new-elements state-after-element]

                                         ;; Check for inline hash: if we see => or : after any element, parse as hash
                                         (or (match-token? state-after-element :operator "=>")
                                             (match-token? state-after-element :operator ":"))
                                         (let [;; Parse the rest as a hash literal starting from current element
                                               [hash-state hash-id] (parse-inline-hash-in-array element-id state-after-element)
                                               ;; Combine previous elements with the hash
                                               all-elements (conj (vec (butlast new-elements)) hash-id)]
                                           [all-elements hash-state])

                                         (match-token? state-after-element :operator ",")
                                         (let [[_ state-after-comma] (consume-token state-after-element)]
                                           (if (match-token? state-after-comma :operator "]")
                                             [new-elements state-after-comma] ; trailing comma
                                             (recur state-after-comma new-elements)))
                                         :else
                                         (throw (ex-info "Expected ',' or ']' in array literal"
                                                        {:token (current-token state-after-element)}))))))
          [_ state-after-close] (consume-token final-state)
          [new-ast entity-id] (create-node (:ast state-after-close) :array-literal
                                         :elements elements
                                         :position {:line (:line open-bracket) :column (:column open-bracket)})]
      [(assoc state-after-close :ast new-ast) entity-id])))

(defn parse-hash-literal
  "Parse hash literal {key: value, 'str' => obj}"
  [state]
  (when (match-token? state :operator "{")
    (let [[open-brace state-after-open] (consume-token state)
          [pairs final-state] (if (match-token? state-after-open :operator "}")
                                [[] state-after-open]
                                (loop [current-state state-after-open
                                       pairs []]
                                  (let [[state-after-key key-id] (parse-hash-key-with-shorthand current-state)
                                        ;; Handle both : and => syntax
                                        [arrow-token state-after-arrow] (cond
                                                                          (match-token? state-after-key :operator ":")
                                                                          (consume-token state-after-key)
                                                                          (match-token? state-after-key :operator "=>")
                                                                          (consume-token state-after-key)
                                                                          :else
                                                                          (throw (ex-info "Expected ':' or '=>' in hash literal"
                                                                                         {:token (current-token state-after-key)})))
                                        [state-after-value value-id] (parse-expression state-after-arrow)
                                        new-pairs (conj pairs [key-id value-id])]
                                    (cond
                                      (match-token? state-after-value :operator "}")
                                      [new-pairs state-after-value]
                                      (match-token? state-after-value :operator ",")
                                      (let [[_ state-after-comma] (consume-token state-after-value)]
                                        (if (match-token? state-after-comma :operator "}")
                                          [new-pairs state-after-comma] ; trailing comma
                                          (recur state-after-comma new-pairs)))
                                      :else
                                      (throw (ex-info "Expected ',' or '}' in hash literal"
                                                     {:token (current-token state-after-value)}))))))
          [_ state-after-close] (consume-token final-state)
          [new-ast entity-id] (create-node (:ast state-after-close) :hash-literal
                                         :pairs pairs
                                         :position {:line (:line open-brace) :column (:column open-brace)})]
      [(assoc state-after-close :ast new-ast) entity-id])))

(defn parse-atomic
  "Parse an atomic expression (literals, identifiers, parenthesized expressions) without postfix operations."
  [state]
  (or (parse-integer-literal state)
      (parse-float-literal state)
      (parse-rational-literal state)
      (parse-complex-literal state)
      (parse-string-literal state)
      (parse-interpolated-string state)
      (parse-boolean-literal state)
      (parse-nil-literal state)
      (parse-self-literal state)
      (parse-symbol-literal state)
      (parse-word-array-literal state)
      (parse-array-literal state)
      (parse-hash-literal state)
      (parse-case-statement state)
      (when (match-token? state :instance-variable)
        (let [[var-token state-after-var] (consume-token state)
              [new-ast entity-id] (create-node (:ast state-after-var) :instance-variable-access
                                             :variable (:value var-token)
                                             :position {:line (:line var-token) :column (:column var-token)})]
          [(assoc state-after-var :ast new-ast) entity-id]))
      (when (match-token? state :class-variable)
        (let [[var-token state-after-var] (consume-token state)
              [new-ast entity-id] (create-node (:ast state-after-var) :class-variable-access
                                             :variable (:value var-token)
                                             :position {:line (:line var-token) :column (:column var-token)})]
          [(assoc state-after-var :ast new-ast) entity-id]))
      (when (match-token? state :identifier)
        (let [[id-token state-after-id] (consume-token state)
              [new-ast entity-id] (create-node (:ast state-after-id) :identifier
                                             :value (:value id-token)
                                             :position {:line (:line id-token) :column (:column id-token)})
              state-with-ast (assoc state-after-id :ast new-ast)]
          [state-with-ast entity-id]))
      (when (match-token? state :operator "(")
        (let [[_ state-after-open] (consume-token state)]
          (if (match-token? state-after-open :operator ")")
            ;; Empty parentheses - create nil literal
            (let [[_ state-after-close] (consume-token state-after-open)
                  [new-ast entity-id] (create-node (:ast state-after-close) :nil-literal
                                                  :value nil
                                                  :position {:line (:line (current-token state)) :column (:column (current-token state))})]
              [(assoc state-after-close :ast new-ast) entity-id])
            ;; Non-empty parentheses - parse expression
            (let [[state expr-id] (parse-expression state-after-open)
                  [_ state] (expect-token state :operator ")")]
              [state expr-id]))))))

(defn parse-primary
  "Parse a primary expression (literals, identifiers, parenthesized expressions, method calls)."
  [state]
  (let [result (or (when (match-token? state :operator "-")
                     (let [[minus-token state-after-minus] (consume-token state)
                           [state-after-operand operand-id] (parse-atomic state-after-minus)
                           [new-ast unary-id] (create-node (:ast state-after-operand) :unary-operation
                                                         :operator "-"
                                                         :operand operand-id
                                                         :position {:line (:line minus-token) :column (:column minus-token)})]
                       [(assoc state-after-operand :ast new-ast) unary-id]))
                   (when (match-token? state :operator "*")
                     (let [[splat-token state-after-splat] (consume-token state)
                           [state-after-operand operand-id] (parse-atomic state-after-splat)
                           [new-ast splat-id] (create-node (:ast state-after-operand) :splat-operation
                                                         :operand operand-id
                                                         :position {:line (:line splat-token) :column (:column splat-token)})]
                       [(assoc state-after-operand :ast new-ast) splat-id]))
                   (when (match-token? state :identifier)
                     (let [[id-token state-after-id] (consume-token state)
                           [new-ast entity-id] (create-node (:ast state-after-id) :identifier
                                                          :value (:value id-token)
                                                          :position {:line (:line id-token) :column (:column id-token)})
                           state-with-ast (assoc state-after-id :ast new-ast)]
                       (cond
                         ;; Explicit parentheses - definitely a method call
                         (match-token? state-with-ast :operator "(")
                         (parse-method-call state-with-ast nil (:value id-token) id-token)

                         ;; Otherwise, treat as identifier
                         :else
                         [state-with-ast entity-id])))
                   (parse-atomic state))]
    (if result
      (let [[state primary-id] result]
        (parse-postfix-expressions state primary-id))
      (throw (ex-info "Expected primary expression"
                      {:token (current-token state)})))))

(def operator-precedence
  "Map of operator symbols to their precedence level.
   Higher numbers = higher precedence."
  {"and" 1 "or" 1             ; Low precedence logical operators
   "||" 2                     ; Logical OR
   "&&" 3                     ; Logical AND
   "=" 4 "+=" 4 "-=" 4 "*=" 4 "/=" 4  ; Assignment and compound assignment
   ".." 4.5 "..." 4.5
   "==" 5 "!=" 5
   "<" 6 "<=" 6 ">" 6 ">=" 6
   "+" 7 "-" 7
   "*" 8 "/" 8 "%" 8})

(defn get-precedence
  "Get the precedence of an operator, or 0 if not found."
  [op]
  (get operator-precedence op 0))

(defn binary-operator?
  "Check if a token represents a binary operator."
  [token]
  (when token
    (or (and (= (:type token) :operator)
             (contains? operator-precedence (:value token)))
        (and (= (:type token) :keyword)
             (contains? #{"and" "or"} (:value token))))))

(defn parse-binary-operation
  "Parse a binary operation with precedence climbing."
  [state left-expr min-precedence]
  (loop [current-state state
         left-id left-expr]
    (let [token (current-token current-state)]
      (if (and (binary-operator? token)
               (>= (get-precedence (:value token)) min-precedence))
        (let [operator (:value token)
              op-precedence (get-precedence operator)
              [_ state-after-op] (consume-token current-state)
              [state-after-right right-id] (parse-primary state-after-op)

              ;; Handle right-associative operators (assignment)
              next-min-precedence (if (= operator "=")
                                    op-precedence  ; Right associative for assignment
                                    (inc op-precedence))

              ;; Check if we need to parse more operators with higher precedence
              [final-state final-right-id]
              (if-let [next-token (current-token state-after-right)]
                (if (and (binary-operator? next-token)
                         (> (get-precedence (:value next-token)) op-precedence))
                  (parse-binary-operation state-after-right right-id next-min-precedence)
                  [state-after-right right-id])
                [state-after-right right-id])

              ;; Create the binary operation node
              [new-ast new-left-id] (create-node (:ast final-state) :binary-operation
                                               :operator operator
                                               :left left-id
                                               :right final-right-id
                                               :position {:line (:line token) :column (:column token)})
              new-state (assoc final-state :ast new-ast)]
          (recur new-state new-left-id))
        [current-state left-id]))))



(defn parse-block
  "Parse a block of statements until 'end' keyword."
  [state]
  (let [[new-ast block-id] (create-node (:ast state) :block :statements [])
        initial-state (skip-separators (assoc state :ast new-ast))]
    (loop [current-state initial-state
           statements []]
      (let [token (current-token current-state)]
        (cond
          (nil? token)
          (throw (ex-info "Expected 'end' to close block" {}))

          (match-token? current-state :keyword "end")
          (let [new-ast (set-component (:ast current-state) block-id :statements statements)]
            [(assoc current-state :ast new-ast) block-id])

          (match-token? current-state :keyword "else")
          (let [new-ast (set-component (:ast current-state) block-id :statements statements)]
            [(assoc current-state :ast new-ast) block-id])

          (or (match-token? current-state :newline)
              (match-token? current-state :operator ";"))
          (recur (second (consume-token current-state)) statements)

          :else
          (let [[state-after-stmt stmt-id] (parse-statement current-state)
                state-after-newlines (skip-separators state-after-stmt)]
            (recur state-after-newlines (conj statements stmt-id))))))))


(defn parse-if-statement
  "Parse an if/else statement."
  [state]
  (when (match-token? state :keyword "if")
    (let [[_ state-after-if] (consume-token state)
          [state-after-condition condition-id] (parse-expression state-after-if)
          state-skip-newlines (skip-separators state-after-condition)
          [state-after-then then-id] (parse-block state-skip-newlines)
          _token (current-token state-after-then)]
      (if (match-token? state-after-then :keyword "else")
        (let [[_ state-after-else] (consume-token state-after-then)
              [state-after-else-block else-id] (parse-block state-after-else)
              [_ final-state] (expect-token state-after-else-block :keyword "end")
              [new-ast entity-id] (create-node (:ast final-state) :if-statement
                                             :condition condition-id
                                             :then-branch then-id
                                             :else-branch else-id)]
          [(assoc final-state :ast new-ast) entity-id])
        (let [[_ final-state] (expect-token state-after-then :keyword "end")
              [new-ast entity-id] (create-node (:ast final-state) :if-statement
                                             :condition condition-id
                                             :then-branch then-id)]
          [(assoc final-state :ast new-ast) entity-id])))))

(defn parse-while-statement
  "Parse a while loop statement."
  [state]
  (when (match-token? state :keyword "while")
    (let [[_ state-after-while] (consume-token state)
          [state-after-condition condition-id] (parse-expression state-after-while)
          state-skip-newlines (skip-separators state-after-condition)
          [state-after-body body-id] (parse-block state-skip-newlines)
          [_ final-state] (expect-token state-after-body :keyword "end")
          [new-ast entity-id] (create-node (:ast final-state) :while-statement
                                         :condition condition-id
                                         :body body-id)]
      [(assoc final-state :ast new-ast) entity-id])))

(defn parse-for-variables
  "Parse variable list for for loop: 'i' or 'i, j' or 'i, *rest' or 'i,'."
  [state]
  (let [variables (atom [])
        current-state (atom state)]
    (loop []
      (cond
        ;; Check for splat variable (*rest)
        (match-token? @current-state :operator "*")
        (let [[_ state-after-splat] (consume-token @current-state)]
          (if (match-token? state-after-splat :identifier)
            (let [[var-token state-after-var] (consume-token state-after-splat)]
              (swap! variables conj {:type :splat :name (:value var-token)})
              (reset! current-state state-after-var))
            ;; Just * without variable name (anonymous splat)
            (do
              (swap! variables conj {:type :splat :name nil})
              (reset! current-state state-after-splat))))

        ;; Check for regular identifier
        (match-token? @current-state :identifier)
        (let [[var-token state-after-var] (consume-token @current-state)]
          (swap! variables conj {:type :regular :name (:value var-token)})
          (reset! current-state state-after-var))

        ;; Check for instance variable
        (match-token? @current-state :instance-variable)
        (let [[var-token state-after-var] (consume-token @current-state)]
          (swap! variables conj {:type :instance-variable :name (:value var-token)})
          (reset! current-state state-after-var))

        ;; No more variables
        :else
        nil)

      ;; Check if there's a comma to continue
      (if (match-token? @current-state :operator ",")
        (let [[_ state-after-comma] (consume-token @current-state)]
          (reset! current-state state-after-comma)
          ;; Continue if there's another variable or splat, otherwise break (trailing comma case)
          (when (or (match-token? @current-state :identifier)
                   (match-token? @current-state :instance-variable)
                   (match-token? @current-state :operator "*"))
            (recur)))
        ;; No comma, we're done
        nil))

    ;; Return the final state and variables list
    [@current-state @variables]))

(defn parse-for-statement
  "Parse a for loop statement (for item in array) with optional destructuring."
  [state]
  (when (match-token? state :keyword "for")
    (let [[_ state-after-for] (consume-token state)]
      ;; Check if this looks like a complex expression (identifier followed by [ or .)
      ;; Parse as expression only for complex cases like arr[1] or obj.attr
      (let [looks-like-complex-expr? (and (match-token? state-after-for :identifier)
                                          ;; Look ahead to see if there's [ or . after identifier
                                          (let [[_ state-after-id] (consume-token state-after-for)]
                                            (or (match-token? state-after-id :operator "[")
                                                (match-token? state-after-id :operator "."))))]
        (if looks-like-complex-expr?
          ;; Try parsing as expression for complex cases like arr[1]
          (if-let [[state-after-expr expr-id] (try
                                               (parse-expression state-after-for)
                                               (catch Exception _ nil))]
          ;; Check if we found 'in' after the expression
          (if (match-token? state-after-expr :keyword "in")
            ;; Single assignable expression case: for arr[1] in array
            (let [[_ state-after-in] (consume-token state-after-expr)
                  [state-after-iterable iterable-id] (parse-expression state-after-in)
                  state-skip-newlines (skip-separators state-after-iterable)
                  [state-after-body body-id] (parse-block state-skip-newlines)
                  [_ final-state] (expect-token state-after-body :keyword "end")
                  [new-ast entity-id] (create-node (:ast final-state) :for-statement
                                                 :target-expression expr-id
                                                 :iterable iterable-id
                                                 :body body-id)]
              [(assoc final-state :ast new-ast) entity-id])
            ;; Not followed by 'in', fall back to variable parsing
            (let [[state-after-vars variables] (parse-for-variables state-after-for)
                  [_ state-after-in] (expect-token state-after-vars :keyword "in")
                  [state-after-iterable iterable-id] (parse-expression state-after-in)
                  state-skip-newlines (skip-separators state-after-iterable)
                  [state-after-body body-id] (parse-block state-skip-newlines)
                  [_ final-state] (expect-token state-after-body :keyword "end")
                  [new-ast entity-id] (create-node (:ast final-state) :for-statement
                                                 :variables variables
                                                 :iterable iterable-id
                                                 :body body-id)]
              [(assoc final-state :ast new-ast) entity-id]))
          ;; Failed to parse as expression, use variable list parsing
          (let [[state-after-vars variables] (parse-for-variables state-after-for)
                [_ state-after-in] (expect-token state-after-vars :keyword "in")
                [state-after-iterable iterable-id] (parse-expression state-after-in)
                state-skip-newlines (skip-separators state-after-iterable)
                [state-after-body body-id] (parse-block state-skip-newlines)
                [_ final-state] (expect-token state-after-body :keyword "end")
                [new-ast entity-id] (create-node (:ast final-state) :for-statement
                                               :variables variables
                                               :iterable iterable-id
                                               :body body-id)]
            [(assoc final-state :ast new-ast) entity-id]))
        ;; Simple identifier or instance variable case - use variable parsing
        (let [[state-after-vars variables] (parse-for-variables state-after-for)
              [_ state-after-in] (expect-token state-after-vars :keyword "in")
              [state-after-iterable iterable-id] (parse-expression state-after-in)
              state-skip-newlines (skip-separators state-after-iterable)
              [state-after-body body-id] (parse-block state-skip-newlines)
              [_ final-state] (expect-token state-after-body :keyword "end")
              [new-ast entity-id] (create-node (:ast final-state) :for-statement
                                             :variables variables
                                             :iterable iterable-id
                                             :body body-id)]
          [(assoc final-state :ast new-ast) entity-id]))))))

(defn parse-until-statement
  "Parse an until loop statement."
  [state]
  (when (match-token? state :keyword "until")
    (let [[_ state-after-until] (consume-token state)
          [state-after-condition condition-id] (parse-expression state-after-until)
          state-skip-newlines (skip-separators state-after-condition)
          [state-after-body body-id] (parse-block state-skip-newlines)
          [_ final-state] (expect-token state-after-body :keyword "end")
          [new-ast entity-id] (create-node (:ast final-state) :until-statement
                                         :condition condition-id
                                         :body body-id)]
      [(assoc final-state :ast new-ast) entity-id])))

(defn when-body-terminator?
  "Check if current token terminates a when clause body."
  [state]
  (let [token (current-token state)]
    (or (nil? token)
        (match-token? state :keyword "when")
        (match-token? state :keyword "else")
        (match-token? state :keyword "end"))))

(defn parse-when-body
  "Parse when clause body statements until 'when', 'else', or 'end' keyword."
  [state]
  (let [[new-ast block-id] (create-node (:ast state) :block :statements [])
        initial-state (skip-separators (assoc state :ast new-ast))]
    (loop [current-state initial-state
           statements []]
      (cond
        (nil? (current-token current-state))
        (throw (ex-info "Expected 'when', 'else', or 'end' to close when clause body" {}))

        (when-body-terminator? current-state)
        (let [new-ast (set-component (:ast current-state) block-id :statements statements)]
          [(assoc current-state :ast new-ast) block-id])

        :else
        (let [[state-after-stmt stmt-id] (parse-statement current-state)
              state-skip-newlines (skip-separators state-after-stmt)]
          (recur state-skip-newlines (conj statements stmt-id)))))))

(defn parse-condition-list
  "Parse comma-separated condition list for when clauses."
  [state]
  (loop [conditions []
         current-state state]
    (let [[state-after-expr expr-id] (parse-expression current-state)
          state-after-newlines (skip-separators state-after-expr)]
      (if (match-token? state-after-newlines :operator ",")
        ;; Found comma, continue parsing
        (let [[_ state-after-comma] (consume-token state-after-newlines)]
          (recur (conj conditions expr-id) state-after-comma))
        ;; No more conditions
        [state-after-newlines (conj conditions expr-id)]))))

(defn parse-when-clause
  "Parse a single when clause with conditions and body."
  [state ast]
  (when (match-token? state :keyword "when")
    (let [[_ state-after-when] (consume-token state)
          [state-after-conditions condition-ids] (parse-condition-list state-after-when)
          [state-after-body body-id] (parse-when-body state-after-conditions)
          [new-ast when-node-id] (create-node (:ast state-after-body) :when-clause
                                            :conditions condition-ids
                                            :body body-id)]
      [(assoc state-after-body :ast new-ast) new-ast when-node-id])))

(defn parse-case-statement
  "Parse a case/when statement."
  [state]
  (when (match-token? state :keyword "case")
    (let [[_ state-after-case] (consume-token state)
          [state-after-expr expr-id] (parse-expression state-after-case)
          state-skip-newlines (skip-separators state-after-expr)]
      (loop [when-clause-ids []
             current-state state-skip-newlines
             current-ast (:ast state-skip-newlines)
             else-id nil]
        (cond
          ;; Parse when clause
          (match-token? current-state :keyword "when")
          (let [[state-after-when new-ast when-clause-id] (parse-when-clause current-state current-ast)]
            (recur (conj when-clause-ids when-clause-id)
                   state-after-when
                   new-ast
                   else-id))

          ;; Parse else clause
          (match-token? current-state :keyword "else")
          (let [[_ state-after-else] (consume-token current-state)
                [state-after-else-body else-body-id] (parse-block state-after-else)]
            (recur when-clause-ids
                   state-after-else-body
                   (:ast state-after-else-body)
                   else-body-id))

          ;; End case
          (match-token? current-state :keyword "end")
          (let [[_ final-state] (consume-token current-state)
                [new-ast entity-id] (create-node current-ast :case-statement
                                               :expression expr-id
                                               :when-clauses when-clause-ids
                                               :else-clause else-id)]
            [(assoc final-state :ast new-ast) entity-id])

          :else
          (throw (ex-info "Expected 'when', 'else', or 'end' in case statement"
                         {:token (current-token current-state)})))))))

(defn parse-parameter-list
  "Parse a comma-separated parameter list enclosed in parentheses."
  [state]
  (if (match-token? state :operator "(")
    (let [[_ state-after-paren] (consume-token state)]
      (if (match-token? state-after-paren :operator ")")
        (let [[_ final-state] (consume-token state-after-paren)]
          [final-state []])
        (loop [current-state state-after-paren
               params []]
          (if (match-token? current-state :identifier)
            (let [[param-token state-after-param] (consume-token current-state)
                  new-params (conj params (:value param-token))]
              (cond
                (match-token? state-after-param :operator ",")
                (let [[_ state-after-comma] (consume-token state-after-param)]
                  (recur state-after-comma new-params))

                (match-token? state-after-param :operator ")")
                (let [[_ final-state] (consume-token state-after-param)]
                  [final-state new-params])

                :else
                (throw (ex-info "Expected ',' or ')' in parameter list"
                               {:token (current-token state-after-param)}))))
            (throw (ex-info "Expected parameter name"
                           {:token (current-token current-state)}))))))
    [state []]))

(defn parse-method-definition
  "Parse a method definition (def method_name(params) ... end) or class method (def self.method_name(params) ... end)."
  [state]
  (when (match-token? state :keyword "def")
    (let [[_ state-after-def] (consume-token state)

          ;; Check if this is a class method (def self.method)
          [is-class-method method-token state-after-method]
          (if (match-token? state-after-def :keyword "self")
            (let [[_ state-after-self] (consume-token state-after-def)
                  [_ state-after-dot] (expect-token state-after-self :operator ".")
                  [method-token state-after-method] (expect-token state-after-dot :identifier)]
              [true method-token state-after-method])
            (let [[method-token state-after-method] (expect-token state-after-def :identifier)]
              [false method-token state-after-method]))

          [state-after-params params] (parse-parameter-list state-after-method)
          state-skip-newlines (skip-separators state-after-params)
          [state-after-body body-id] (parse-block state-skip-newlines)
          [_ final-state] (expect-token state-after-body :keyword "end")

          ;; Create appropriate AST node type
          [new-ast entity-id] (create-node (:ast final-state)
                                         (if is-class-method :class-method-definition :method-definition)
                                         :name (:value method-token)
                                         :parameters params
                                         :body body-id
                                         :position {:line (:line method-token) :column (:column method-token)})]
      [(assoc final-state :ast new-ast) entity-id])))

(declare parse-class-statement)

(defn parse-class-block
  "Parse a class body block of statements until 'end' keyword."
  [state]
  (let [[new-ast block-id] (create-node (:ast state) :block :statements [])
        initial-state (skip-separators (assoc state :ast new-ast))]
    (loop [current-state initial-state
           statements []]
      (let [token (current-token current-state)]
        (cond
          (nil? token)
          (throw (ex-info "Expected 'end' to close block" {}))

          (match-token? current-state :keyword "end")
          (let [new-ast (set-component (:ast current-state) block-id :statements statements)]
            [(assoc current-state :ast new-ast) block-id])

          (match-token? current-state :keyword "else")
          (let [new-ast (set-component (:ast current-state) block-id :statements statements)]
            [(assoc current-state :ast new-ast) block-id])

          (or (match-token? current-state :newline)
              (match-token? current-state :operator ";"))
          (recur (second (consume-token current-state)) statements)

          :else
          (let [[state-after-stmt stmt-id] (parse-class-statement current-state)
                state-after-newlines (skip-separators state-after-stmt)]
            (recur state-after-newlines (conj statements stmt-id))))))))

(defn parse-class-definition
  "Parse a class definition (class ClassName < ParentClass ... end)."
  [state]
  (when (match-token? state :keyword "class")
    (let [[_ state-after-class] (consume-token state)
          [class-token state-after-name] (expect-token state-after-class :identifier)

          ;; Check for inheritance (< ParentClass)
          [state-after-inheritance parent-class]
          (if (match-token? state-after-name :operator "<")
            (let [[_ state-after-lt] (consume-token state-after-name)
                  [parent-token state-after-parent] (expect-token state-after-lt :identifier)]
              [state-after-parent (:value parent-token)])
            [state-after-name nil])

          state-skip-newlines (skip-separators state-after-inheritance)
          [state-after-body body-id] (parse-class-block state-skip-newlines)
          [_ final-state] (expect-token state-after-body :keyword "end")
          [new-ast entity-id] (create-node (:ast final-state) :class-definition
                                         :name (:value class-token)
                                         :parent-class parent-class
                                         :body body-id
                                         :position {:line (:line class-token) :column (:column class-token)})]
      [(assoc final-state :ast new-ast) entity-id])))

(defn parse-return-statement
  "Parse a return statement (return [expression])."
  [state]
  (when (match-token? state :keyword "return")
    (let [[return-token state-after-return] (consume-token state)
          ;; Check if there's an expression after return
          has-expression? (and (current-token state-after-return)
                               (not (match-token? state-after-return :newline))
                               (not (match-token? state-after-return :keyword "end")))
          [state-after-expr expr-id] (if has-expression?
                                       (parse-expression state-after-return)
                                       [state-after-return nil])
          [new-ast entity-id] (create-node (:ast state-after-expr) :return-statement
                                         :value expr-id
                                         :position {:line (:line return-token) :column (:column return-token)})]
      [(assoc state-after-expr :ast new-ast) entity-id])))

(defn parse-break-statement
  "Parse a break statement."
  [state]
  (when (match-token? state :keyword "break")
    (let [[break-token state-after-break] (consume-token state)
          [new-ast entity-id] (create-node (:ast state-after-break) :break-statement
                                         :position {:line (:line break-token) :column (:column break-token)})]
      [(assoc state-after-break :ast new-ast) entity-id])))

(defn parse-continue-statement
  "Parse a continue statement."
  [state]
  (when (match-token? state :keyword "continue")
    (let [[continue-token state-after-continue] (consume-token state)
          [new-ast entity-id] (create-node (:ast state-after-continue) :continue-statement
                                         :position {:line (:line continue-token) :column (:column continue-token)})]
      [(assoc state-after-continue :ast new-ast) entity-id])))

(defn parse-next-statement
  "Parse a next statement (Ruby's continue)."
  [state]
  (when (match-token? state :keyword "next")
    (let [[next-token state-after-next] (consume-token state)
          [new-ast entity-id] (create-node (:ast state-after-next) :next-statement
                                         :position {:line (:line next-token) :column (:column next-token)})]
      [(assoc state-after-next :ast new-ast) entity-id])))

(defn parse-loop-statement
  "Parse a loop do...end infinite loop statement."
  [state]
  (when (match-token? state :keyword "loop")
    (let [[_ state-after-loop] (consume-token state)
          [_ state-after-do] (expect-token state-after-loop :keyword "do")
          state-skip-newlines (skip-separators state-after-do)
          [state-after-body body-id] (parse-block state-skip-newlines)
          [_ final-state] (expect-token state-after-body :keyword "end")
          [new-ast entity-id] (create-node (:ast final-state) :loop-statement
                                         :body body-id)]
      [(assoc final-state :ast new-ast) entity-id])))

(defn parse-indexed-assignment-statement
  "Parse an indexed assignment statement (identifier[expr] = expression)."
  [state]
  (when (match-token? state :identifier)
    (let [lookahead-state (update state :pos inc)]
      (when (match-token? lookahead-state :operator "[")
        ;; Try to parse: identifier[expr] = value
        (let [[id-token state-after-id] (consume-token state)
              [_ state-after-bracket] (consume-token state-after-id)
              [state-after-index index-id] (parse-expression state-after-bracket)
              [_ state-after-close] (expect-token state-after-index :operator "]")
              state-after-close-bracket state-after-close]
          ;; Check for assignment operator
          (when (match-token? state-after-close-bracket :operator "=")
            (let [[_ state-after-assign] (consume-token state-after-close-bracket)
                  [state-after-value value-id] (parse-expression state-after-assign)
                  [new-ast entity-id] (create-node (:ast state-after-value) :indexed-assignment-statement
                                                   :array (:value id-token)
                                                   :index index-id
                                                   :value value-id
                                                   :position {:line (:line id-token) :column (:column id-token)})]
              [(assoc state-after-value :ast new-ast) entity-id])))))))

(defn parse-method-assignment-statement
  "Parse a method assignment statement (obj.method = expression)."
  [state]
  (when (match-token? state :identifier)
    (let [lookahead-state (update state :pos inc)]
      (when (and (match-token? lookahead-state :operator ".")
                 (let [after-dot (update lookahead-state :pos inc)]
                   (and (match-token? after-dot :identifier)
                        (match-token? (update after-dot :pos inc) :operator "="))))
        ;; Parse: identifier.method = value
        (let [[receiver-token state-after-receiver] (consume-token state)
              [_ state-after-dot] (consume-token state-after-receiver)
              [method-token state-after-method] (consume-token state-after-dot)
              [_ state-after-assign] (consume-token state-after-method)
              [state-after-value value-id] (parse-expression state-after-assign)
              ;; Create receiver identifier node
              [ast-with-receiver receiver-id] (create-node (:ast state-after-value) :identifier
                                                          :value (:value receiver-token)
                                                          :position {:line (:line receiver-token) :column (:column receiver-token)})
              ;; Create method call for setter (method_name=)
              [new-ast entity-id] (create-node ast-with-receiver :method-call
                                             :receiver receiver-id
                                             :method (str (:value method-token) "=")
                                             :arguments [value-id]
                                             :position {:line (:line method-token) :column (:column method-token)})]
          [(assoc state-after-value :ast new-ast) entity-id])))))

(defn parse-variable-list
  "Parse a comma-separated list of variables (x, y, z)"
  [state]
  (loop [current-state state
         variables []]
    (if (match-token? current-state :identifier)
      (let [[var-token state-after-var] (consume-token current-state)
            new-variables (conj variables (:value var-token))]
        (if (match-token? state-after-var :operator ",")
          (let [[_ state-after-comma] (consume-token state-after-var)]
            (recur state-after-comma new-variables))
          [state-after-var new-variables]))
      [current-state variables])))

(defn parse-multiple-assignment-statement
  "Parse a multiple assignment statement (x, y = expression)."
  [state]
  (when (match-token? state :identifier)
    ;; Look ahead to see if this is a multiple assignment (has comma)
    (let [lookahead-pos (:pos state)
          has-comma? (loop [pos (inc lookahead-pos)]
                      (if (>= pos (count (:tokens state)))
                        false
                        (let [token (nth (:tokens state) pos)]
                          (cond
                            (= (:type token) :operator)
                            (case (:value token)
                              "," true
                              "=" false
                              (recur (inc pos)))
                            :else (recur (inc pos))))))]
      (when has-comma?
        (let [[state-after-vars variables] (parse-variable-list state)]
          (when (match-token? state-after-vars :operator "=")
            (let [[_ state-after-assign] (consume-token state-after-vars)
                  [state-after-value value-id] (parse-expression state-after-assign)
                  [new-ast entity-id] (create-node (:ast state-after-value) :multiple-assignment-statement
                                                 :variables variables
                                                 :value value-id
                                                 :position {:line (:line (first (:tokens state))) :column (:column (first (:tokens state)))})]
              [(assoc state-after-value :ast new-ast) entity-id])))))))

(defn parse-assignment-statement
  "Parse an assignment statement (identifier = expression)."
  [state]
  (when (match-token? state :identifier)
    (when (match-token? (update state :pos inc) :operator "=")
        (let [[id-token state-after-id] (consume-token state)
              [_ state-after-assign] (consume-token state-after-id)
              [state-after-value value-id] (parse-expression state-after-assign)
              [new-ast entity-id] (create-node (:ast state-after-value) :assignment-statement
                                             :variable (:value id-token)
                                             :value value-id
                                             :position {:line (:line id-token) :column (:column id-token)})]
          [(assoc state-after-value :ast new-ast) entity-id]))))


(defn parse-instance-variable-assignment
  "Parse an instance variable assignment (@var = expression)."
  [state]
  (when (match-token? state :instance-variable)
    (when (match-token? (update state :pos inc) :operator "=")
      (let [[var-token state-after-var] (consume-token state)
            [_ state-after-assign] (consume-token state-after-var)
            [state-after-value value-id] (parse-expression state-after-assign)
            [new-ast entity-id] (create-node (:ast state-after-value) :instance-variable-assignment
                                           :variable (:value var-token)
                                           :value value-id
                                           :position {:line (:line var-token) :column (:column var-token)})]
        [(assoc state-after-value :ast new-ast) entity-id]))))

(defn parse-class-variable-assignment
  "Parse a class variable assignment (@@var = expression)."
  [state]
  (when (match-token? state :class-variable)
    (when (match-token? (update state :pos inc) :operator "=")
      (let [[var-token state-after-var] (consume-token state)
            [_ state-after-assign] (consume-token state-after-var)
            [state-after-value value-id] (parse-expression state-after-assign)
            [new-ast entity-id] (create-node (:ast state-after-value) :class-variable-assignment
                                           :variable (:value var-token)
                                           :value value-id
                                           :position {:line (:line var-token) :column (:column var-token)})]
        [(assoc state-after-value :ast new-ast) entity-id]))))

(defn parse-attr-accessor-list
  "Parse a comma-separated list of attribute symbols."
  [state]
  (loop [current-state state
         attributes []
         expecting-symbol true]
    (let [current-token (current-token current-state)]
      (cond
        ;; Found symbol when expecting one
        (and expecting-symbol current-token (= (:type current-token) :symbol))
        (let [[symbol-token next-state] (consume-token current-state)]
          (recur next-state (conj attributes (:value symbol-token)) false))

        ;; Found comma when not expecting symbol
        (and (not expecting-symbol) (match-token? current-state :operator ","))
        (let [[_ next-state] (consume-token current-state)]
          (recur next-state attributes true))

        ;; End condition: no more symbols/commas or newline
        :else
        [current-state attributes]))))

(defn parse-attr-accessor-statement
  "Parse an attr_accessor statement (attr_accessor :value, :left, :right)."
  [state]
  (when (match-token? state :keyword "attr_accessor")
    (let [[attr-token state-after-attr] (consume-token state)
          [state-after-attrs attributes] (parse-attr-accessor-list state-after-attr)
          [new-ast entity-id] (create-node (:ast state-after-attrs) :attr-accessor-statement
                                         :attributes attributes
                                         :position {:line (:line attr-token) :column (:column attr-token)})]
      [(assoc state-after-attrs :ast new-ast) entity-id])))

(defn parse-attr-reader-statement
  "Parse an attr_reader statement (attr_reader :value, :left)."
  [state]
  (when (match-token? state :keyword "attr_reader")
    (let [[attr-token state-after-attr] (consume-token state)
          [state-after-attrs attributes] (parse-attr-accessor-list state-after-attr)
          [new-ast entity-id] (create-node (:ast state-after-attrs) :attr-reader-statement
                                         :attributes attributes
                                         :position {:line (:line attr-token) :column (:column attr-token)})]
      [(assoc state-after-attrs :ast new-ast) entity-id])))

(defn parse-attr-writer-statement
  "Parse an attr_writer statement (attr_writer :value, :left)."
  [state]
  (when (match-token? state :keyword "attr_writer")
    (let [[attr-token state-after-attr] (consume-token state)
          [state-after-attrs attributes] (parse-attr-accessor-list state-after-attr)
          [new-ast entity-id] (create-node (:ast state-after-attrs) :attr-writer-statement
                                         :attributes attributes
                                         :position {:line (:line attr-token) :column (:column attr-token)})]
      [(assoc state-after-attrs :ast new-ast) entity-id])))

(defn parse-class-statement
  "Parse a statement that can appear in class body (includes attr statements)."
  [state]
  (parse-statement state))


(defn parse-postfix-if
  "Parse a postfix if modifier (statement if condition)."
  [state stmt-id]
  (if (match-token? state :keyword "if")
    (let [[_ state-after-if] (consume-token state)
          [state-after-condition condition-id] (parse-expression state-after-if)
          [new-ast postfix-id] (create-node (:ast state-after-condition) :postfix-if
                                          :statement stmt-id
                                          :condition condition-id)]
      [(assoc state-after-condition :ast new-ast) postfix-id])
    [state stmt-id]))

(defn parse-statement
  "Parse a statement (expression or control flow)."
  [state]
  (when-let [result (or (parse-class-definition state)
                        (parse-method-definition state)
                        (parse-if-statement state)
                        (parse-while-statement state)
                        (parse-for-statement state)
                        (parse-until-statement state)
                        (parse-case-statement state)
                        (parse-return-statement state)
                        (parse-break-statement state)
                        (parse-continue-statement state)
                        (parse-next-statement state)
                        (parse-loop-statement state)
                        (parse-instance-variable-assignment state)
                        (parse-class-variable-assignment state)
                        (parse-indexed-assignment-statement state)
                        (parse-method-assignment-statement state)
                        (parse-attr-accessor-statement state)
                        (parse-attr-reader-statement state)
                        (parse-attr-writer-statement state)
                        (parse-multiple-assignment-statement state)
                        (parse-assignment-statement state)
                        (parse-expression state))]
    (let [[state-after-stmt stmt-id] result]
      ;; Check for postfix modifiers after parsing the statement
      (parse-postfix-if state-after-stmt stmt-id))))

(defn parse-expression
  "Parse any expression with binary operations and precedence."
  [state]
  (let [[state-after-primary primary-id] (parse-primary state)]
    (parse-binary-operation state-after-primary primary-id 0)))

(defn find-root-entity
  "Find the root entity (one with no parent and is not referenced by others)."
  [ast]
  (let [entities (:entities ast)
        components (:components ast)
        parent-map (:parent components)
        left-map (:left components)
        right-map (:right components)
        body-map (:body components)
        value-map (:value components)
        statements-map (:statements components)

        ;; Find all entities that are referenced as children
        ;; Only include values that are actually entity IDs (numbers in the entities set)
        parent-refs (filter entities (vals parent-map))
        left-refs (filter entities (vals left-map))
        right-refs (filter entities (vals right-map))
        body-refs (filter entities (vals body-map))
        ; value-refs removed - :value typically stores literal values, not entity references
        stmt-refs (filter entities (apply concat (vals statements-map)))
        arg-refs (filter entities (apply concat (vals (:arguments components))))
        receiver-refs (filter entities (filter number? (vals (:receiver components))))
        operand-refs (filter entities (filter number? (vals (:operand components))))
        block-refs (filter entities (filter number? (vals (:block components))))
        block-body-refs (filter entities (apply concat (vals (:block-body components))))

        referenced-entities (set (concat parent-refs left-refs right-refs body-refs
                                        stmt-refs arg-refs receiver-refs operand-refs
                                        block-refs block-body-refs))

        ;; Root candidates are those not referenced by others
        root-candidates (->> entities
                            (filter #(not (contains? referenced-entities %))))]

    ;; Prefer programs, then assignment statements, method calls, and method definitions as roots
    (or (first (filter #(#{:program}
                          (get-in components [:node-type %]))
                       root-candidates))
        (first (filter #(#{:assignment-statement :method-definition :class-method-definition :method-call}
                          (get-in components [:node-type %]))
                       root-candidates))
        (first root-candidates))))

(defn get-children
  "Get children of an entity using various relationship components."
  [ast entity-id]
  (let [components (:components ast)
        children-map (:children components)
        left (:left components)
        right (:right components)
        value-ref (:value components)
        body (:body components)
        statements (:statements components)
        arguments (:arguments components)
        receiver (:receiver components)
        index (:index components)
        condition (:condition components)
        then-branch (:then-branch components)
        else-branch (:else-branch components)
        node-type (get-in components [:node-type entity-id])]
    (cond
      ;; Direct children list
      (contains? children-map entity-id)
      (get children-map entity-id)

      ;; Binary operations have left and right
      (= :binary-operation node-type)
      (filter some? [(get left entity-id) (get right entity-id)])

      ;; Assignment statements have value reference
      (and (= :assignment-statement node-type)
           (number? (get value-ref entity-id)))
      [(get value-ref entity-id)]

      ;; Method definitions have body
      (or (= :method-definition node-type) (= :class-method-definition node-type))
      (if-let [body-id (get body entity-id)]
        [body-id]
        [])

      ;; Class definitions have body
      (= :class-definition node-type)
      (if-let [body-id (get body entity-id)]
        [body-id]
        [])

      ;; Instance and class variable assignments have value reference
      (or (= :instance-variable-assignment node-type)
          (= :class-variable-assignment node-type))
      (if-let [value-id (get value-ref entity-id)]
        [value-id]
        [])

      ;; Blocks and programs have statements
      (or (= :block node-type) (= :program node-type))
      (get statements entity-id [])

      ;; If statements have condition, then-branch, and optionally else-branch
      (= :if-statement node-type)
      (filter some? [(get condition entity-id)
                     (get then-branch entity-id)
                     (get else-branch entity-id)])

      ;; While statements have condition and body
      (= :while-statement node-type)
      (filter some? [(get condition entity-id)
                     (get body entity-id)])

      ;; For statements have iterable and body
      (= :for-statement node-type)
      (let [iterable-ref (get-in components [:iterable entity-id])
            body-ref (get body entity-id)]
        (filter some? [iterable-ref body-ref]))

      ;; Until statements have condition and body
      (= :until-statement node-type)
      (filter some? [(get condition entity-id)
                     (get body entity-id)])

      ;; Loop statements have just body
      (= :loop-statement node-type)
      (if-let [body-ref (get body entity-id)]
        [body-ref]
        [])

      ;; Method calls have receiver and arguments
      (= :method-call node-type)
      (let [args (get arguments entity-id [])
            recv (get receiver entity-id)]
        (if recv
          (concat [recv] args)
          args))

      ;; Array access has receiver and index
      (= :array-access node-type)
      (let [recv (get receiver entity-id)
            idx (get index entity-id)]
        (filter some? [recv idx]))

      ;; Unary operations have operand
      (= :unary-operation node-type)
      (if-let [operand-id (get-in components [:operand entity-id])]
        [operand-id]
        [])

      :else [])))

(defn format-entity
  "Format an entity for display in tree view."
  [ast entity-id]
  (let [components (:components ast)
        node-type (get-in components [:node-type entity-id])
        value (get-in components [:value entity-id])
        operator (get-in components [:operator entity-id])
        variable (get-in components [:variable entity-id])
        name (get-in components [:name entity-id])
        parameters (get-in components [:parameters entity-id])
        method (get-in components [:method entity-id])]
    (case node-type
      :assignment-statement (str variable " = ...")
      :binary-operation (str operator)
      :method-call (str method "(...)")
      :array-access "[...]"
      :integer-literal (str value)
      :float-literal (str value)
      :string-literal (str "\"" value "\"")
      :boolean-literal (str value)
      :nil-literal "nil"
      :self-literal "self"
      :variable (str value)
      :identifier (str value)
      :method-definition (str "def " name "(" (clojure.string/join ", " parameters) ")")
      :class-method-definition (str "def self." name "(" (clojure.string/join ", " parameters) ")")
      :if-statement "if"
      :while-statement "while"
      :for-statement (str "for " variable " in ...")
      :until-statement "until"
      :loop-statement "loop"
      :unary-operation (str operator)
      :block "block"
      :program "program"
      (str node-type))))

(defn visualize-tree
  "Create a tree-like visualization of the ECS AST."
  [ast]
  (letfn [(render-tree [entity-id prefix is-last]
            (let [current-prefix (if is-last "└── " "├── ")
                  child-prefix (if is-last "    " "│   ")
                  children (get-children ast entity-id)
                  formatted (format-entity ast entity-id)]
              (str prefix current-prefix formatted "\n"
                   (apply str
                          (map-indexed
                           (fn [idx child-id]
                             (render-tree child-id
                                          (str prefix child-prefix)
                                          (= idx (dec (count children)))))
                           children)))))]
    (if-let [root (find-root-entity ast)]
      (with-out-str
        (println "=== AST Tree Structure ===")
        (println)
        (print (render-tree root "" true)))
      (with-out-str
        (println "=== AST Tree Structure ===")
        (println "No root entity found")))))

(defn visualize-ast
  "Create a text visualization of the ECS AST."
  [ast]
  (let [entities (sort (:entities ast))
        components (:components ast)]
    (with-out-str
      (println "=== ECS AST Structure ===")
      (println)
      (println "Entities:" (count entities))
      (println "Components:" (keys components))
      (println)
      (doseq [entity-id entities]
        (println (str "Entity " entity-id ":"))
        (doseq [[component-type component-map] components]
          (when-let [value (get component-map entity-id)]
            (println (str "  " component-type ": " (pr-str value)))))
        (println)))))

(defn parse
  "Parse a sequence of tokens into an ECS AST."
  [tokens]
  (let [initial-state (create-parse-state tokens)]
    (if (empty? tokens)
      (:ast initial-state)
      (loop [current-state initial-state
             statements []]
        (let [current-state (skip-separators current-state)]
          (if (nil? (current-token current-state))
            ;; End of input - always create a program node
            (let [[new-ast] (create-node (:ast current-state) :program
                                                   :statements statements)]
              new-ast)
            ;; Parse next statement
            (let [[state-after-stmt stmt-id] (parse-statement current-state)
                  new-statements (conj statements stmt-id)]
              (recur state-after-stmt new-statements))))))))
