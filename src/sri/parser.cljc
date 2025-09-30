(ns sri.parser
  "Parser for the Ruby programming language.

   Implements a recursive descent parser that converts tokens into
   an ECS-based AST representation."
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
                                          :value (Integer/parseInt (:value token))
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

(defn parse-string-literal
  "Parse a string literal."
  [state]
  (when (match-token? state :string)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :string-literal
                                          :value (:value token)
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

(defn parse-symbol-literal
  "Parse a symbol literal."
  [state]
  (when (match-token? state :symbol)
    (let [[token new-state] (consume-token state)
          [new-ast entity-id] (create-node (:ast new-state) :symbol-literal
                                          :value (keyword (:value token))
                                          :position {:line (:line token) :column (:column token)})]
      [(assoc new-state :ast new-ast) entity-id])))

(declare parse-expression parse-statement parse-primary)

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

(defn parse-block-body
  "Parse block body statements until closing }."
  [state]
  (loop [current-state state
         statements []]
    (let [current-token (current-token current-state)]
      (cond
        ;; Found closing } - end of block body
        (match-token? current-state :operator "}")
        [current-state statements]

        ;; End of input without closing }
        (nil? current-token)
        (throw (ex-info "Unexpected end of input in block body" {}))

        ;; Parse a statement
        :else
        (let [[state-after-stmt stmt-id] (parse-statement current-state)]
          (recur state-after-stmt (conj statements stmt-id)))))))

(defn parse-ruby-block
  "Parse Ruby block literal { |param1, param2| statements }"
  [state]
  (when (match-token? state :operator "{")
    (let [[open-brace state-after-open] (consume-token state)]
      ;; Check if this is a block (has |) or hash (doesn't have | immediately)
      (if (match-token? state-after-open :operator "|")
        ;; This is a block - parse parameters and body
        (try
          (let [;; Skip the opening |
                [_ state-after-pipe] (consume-token state-after-open)
                ;; Parse parameters until closing |
                [state-after-params params] (parse-block-parameters state-after-pipe)
                ;; Skip the closing |
                [_ state-after-closing-pipe] (consume-token state-after-params)
                ;; Parse block body until closing }
                [state-after-body body-statements] (parse-block-body state-after-closing-pipe)
                ;; Skip the closing }
                [_ final-state] (consume-token state-after-body)
                ;; Create the block node with actual parameters and body
                [new-ast entity-id] (create-node (:ast final-state) :block
                                               :block-params params
                                               :block-body body-statements
                                               :position {:line (:line open-brace) :column (:column open-brace)})]
            [(assoc final-state :ast new-ast) entity-id])
          (catch Exception e
            (binding [*out* *err*]
              (println "DEBUG: Error parsing block:" (.getMessage e))
              (flush))
            nil))
        ;; Not a block, return nil so parse-hash-literal can handle it
        nil))))

(defn parse-method-call
  "Parse a method call, either standalone or with receiver."
  [state receiver-id method-name token]
  (let [[state-after-args args] (parse-argument-list state)
        ;; Check for block after arguments
        [final-state block-id] (if (match-token? state-after-args :operator "{")
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
                                  (let [[state-after-key key-id] (parse-expression current-state)
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
      (parse-string-literal state)
      (parse-boolean-literal state)
      (parse-nil-literal state)
      (parse-self-literal state)
      (parse-symbol-literal state)
      (parse-array-literal state)
      (parse-hash-literal state)
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
        (let [[_ state] (consume-token state)
              [state expr-id] (parse-expression state)
              [_ state] (expect-token state :operator ")")]
          [state expr-id]))))

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
  {"or" 1
   "and" 2
   "==" 3 "!=" 3
   "<" 4 "<=" 4 ">" 4 ">=" 4
   "+" 5 "-" 5
   "*" 6 "/" 6 "%" 6})

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

              ;; Handle right-associative operators (none in our current set)
              next-min-precedence (inc op-precedence)

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
          [state-after-body body-id] (parse-block state-skip-newlines)
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

(defn parse-statement
  "Parse a statement (expression or control flow)."
  [state]
  (or (parse-class-definition state)
      (parse-method-definition state)
      (parse-if-statement state)
      (parse-while-statement state)
      (parse-return-statement state)
      (parse-break-statement state)
      (parse-continue-statement state)
      (parse-instance-variable-assignment state)
      (parse-class-variable-assignment state)
      (parse-indexed-assignment-statement state)
      (parse-assignment-statement state)
      (parse-expression state)))

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
            ;; End of input
            (if (= 1 (count statements))
              ;; Single statement, return as-is
              (:ast current-state)
              ;; Multiple statements, wrap in a program block
              (let [[new-ast] (create-node (:ast current-state) :program
                                                     :statements statements)]
                new-ast))
            ;; Parse next statement
            (let [[state-after-stmt stmt-id] (parse-statement current-state)
                  new-statements (conj statements stmt-id)]
              (recur state-after-stmt new-statements))))))))
