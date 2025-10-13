(ns sri.interpreter
  "Direct AST interpreter for Ruby"
  (:require [sri.parser :as parser]
            [clojure.string]
            [sri.enumerable :as enum]
            [sri.ruby-classes-new :as ruby-classes]
            [sri.ruby-array :refer [->RubyArray ruby-array?]]))


(declare interpret-expression interpret-statement interpret-user-method execute-block)

(defn interpret-literal
  "Interpret a literal value node."
  [ast entity-id]
  (let [value (parser/get-value ast entity-id)]
    (cond
      (integer? value) value
      (string? value) value
      (boolean? value) value
      (nil? value) nil
      :else value)))

(defn interpret-interpolated-string
  "Interpret an interpolated string by evaluating expressions and concatenating."
  [ast entity-id variables]
  (let [parts (parser/get-component ast entity-id :parts)]
    (apply str (map (fn [part]
                      (cond
                        ;; Text part - return as is
                        (string? part)
                        part

                        ;; Expression part - parse and evaluate
                        (and (map? part) (:source part))
                        (let [expr-source (:source part)
                              expr-tokens (sri.tokenizer/tokenize expr-source)
                              temp-state (parser/create-parse-state expr-tokens)
                              [final-state expr-id] (parser/parse-expression temp-state)]
                          (str (interpret-expression (:ast final-state) expr-id variables)))

                        ;; Unknown part type
                        :else
                        (str "UNKNOWN: " part)))
                    parts))))

(defn interpret-array-literal
  "Interpret an array literal like [1, 2, 3]."
  [ast entity-id variables]
  (let [children (parser/get-children ast entity-id)
        elements (parser/get-component ast entity-id :elements)
        element-ids (if (seq children) children elements)]
    (if element-ids
      ;; element-ids should be a collection of entity IDs that need interpretation
      (->RubyArray (atom (vec (map #(interpret-expression ast % variables) element-ids))))
      (->RubyArray (atom [])))))

(defrecord MutableHash [data])

(defn create-mutable-hash
  "Create a mutable hash object using an atom."
  [initial-map]
  (->MutableHash (atom initial-map)))

(defn mutable-hash?
  "Check if an object is a mutable hash."
  [obj]
  (instance? MutableHash obj))


(defn format-ruby-hash
  "Format a hash in Ruby style: {\"key\"=>\"value\"}"
  [hash-map]
  (if (empty? hash-map)
    "{}"
    (let [pairs (map (fn [[k v]]
                       (str (pr-str k) "=>" (pr-str v)))
                     hash-map)]
      (str "{" (clojure.string/join ", " pairs) "}"))))

(defn execute-block
  "Execute a block with given parameters."
  [ast block-id variables block-args]
  (let [block-params (parser/get-component ast block-id :block-params)
        block-body-ids (parser/get-component ast block-id :block-body)
        ;; Create a new variable scope with block parameters bound to arguments
        block-variables (atom @variables)]

    ;; Bind block parameters to arguments
    (when (and block-params (seq block-params) (seq block-args))
      (doseq [[param arg] (map vector block-params block-args)]
        (swap! block-variables assoc param arg)))

    ;; Execute block body with the new variable scope
    (if block-body-ids
      (let [results (map #(interpret-expression ast % block-variables) block-body-ids)]
        (last results))
      nil)))

(defn interpret-hash-literal
  "Interpret a hash literal like {key => value, key2 => value2}."
  [ast entity-id variables]
  (let [pairs (parser/get-component ast entity-id :pairs)]
    (if pairs
      ;; pairs is a collection of [key-id value-id] pairs that need interpretation
      (let [initial-map (reduce (fn [hash [key-id value-id]]
                                  (let [key (interpret-expression ast key-id variables)
                                        value (interpret-expression ast value-id variables)]
                                    (assoc hash key value)))
                                {}
                                pairs)]
        (create-mutable-hash initial-map))
      (create-mutable-hash {}))))

(defn interpret-array-access
  "Interpret array or hash access like arr[0] or hash[key]."
  [ast entity-id variables]
  (let [receiver-id (parser/get-receiver ast entity-id)
        index-id (parser/get-component ast entity-id :index)
        receiver-val (interpret-expression ast receiver-id variables)
        index-val (interpret-expression ast index-id variables)]
    (cond
      ;; Ruby array access (check first)
      (ruby-array? receiver-val)
      (cond
        (not (integer? index-val))
        (throw (ex-info "Array index must be integer" {:array receiver-val :index index-val}))

        :else
        (let [array-data @(:data receiver-val)
              idx (if (< index-val 0) (+ (count array-data) index-val) index-val)]
          (if (and (>= idx 0) (< idx (count array-data)))
            (get array-data idx)
            nil))) ; Ruby returns nil for out-of-bounds access

      ;; Mutable hash access
      (mutable-hash? receiver-val)
      (get @(:data receiver-val) index-val)

      ;; Legacy immutable hash access (for backward compatibility)
      (map? receiver-val)
      (get receiver-val index-val)

      ;; Legacy vector access (for backward compatibility)
      (vector? receiver-val)
      (cond
        (not (integer? index-val))
        (throw (ex-info "Array index must be integer" {:array receiver-val :index index-val}))

        :else
        (let [idx (if (< index-val 0) (+ (count receiver-val) index-val) index-val)]
          (when (= "true" (System/getenv "RUBY_VERBOSE"))
            (println "DEBUG: Vector access - array:" receiver-val "index:" index-val "computed idx:" idx))
          (if (and (>= idx 0) (< idx (count receiver-val)))
            (get receiver-val idx)
            nil)))

      :else
      (throw (ex-info "Access on invalid type" {:receiver receiver-val :index index-val})))))

(defn interpret-array-assignment
  "Interpret array assignment like arr[0] = value."
  [ast entity-id variables]
  (let [array-id (parser/get-receiver ast entity-id)
        index-id (parser/get-component ast entity-id :index)
        value-id (parser/get-component ast entity-id :value)
        array-name (parser/get-value ast array-id)
        index-val (interpret-expression ast index-id variables)
        new-value (interpret-expression ast value-id variables)]
    (when-let [current-array (get @variables array-name)]
      (let [idx (if (< index-val 0) (+ (count current-array) index-val) index-val)
            ;; Expand array if needed
            expanded-array (if (>= idx (count current-array))
                            (vec (concat current-array (repeat (- idx (count current-array) -1) nil)))
                            current-array)
            updated-array (assoc expanded-array idx new-value)]
        (swap! variables assoc array-name updated-array)
        new-value))))

(defn interpret-indexed-assignment
  "Interpret indexed assignment like arr[0] = value or hash[key] = value."
  [ast entity-id variables]
  (let [{:keys [array index value]} (parser/get-components ast entity-id [:array :index :value])
        index-val (interpret-expression ast index variables)
        new-value (interpret-expression ast value variables)]
    (when-let [current-receiver (get @variables array)]
      (cond
        ;; Mutable hash assignment
        (mutable-hash? current-receiver)
        (do
          (swap! (:data current-receiver) assoc index-val new-value)
          new-value)

        ;; Ruby array assignment
        (ruby-array? current-receiver)
        (if (integer? index-val)
          (let [current-data @(:data current-receiver)
                idx (if (< index-val 0) (+ (count current-data) index-val) index-val)]
            (if (>= idx 0)
              (let [;; Expand array if needed
                    expanded (if (>= idx (count current-data))
                              (vec (concat current-data (repeat (- idx (count current-data) -1) nil)))
                              current-data)
                    updated (assoc expanded idx new-value)]
                (reset! (:data current-receiver) updated)
                new-value)
              (throw (ex-info "Negative index too large" {:index index-val :array-size (count current-data)}))))
          (throw (ex-info "Array index must be integer" {:array current-receiver :index index-val})))

        ;; Legacy immutable hash assignment
        (map? current-receiver)
        (let [updated-hash (assoc current-receiver index-val new-value)]
          (swap! variables assoc array updated-hash)
          new-value)

        ;; Array assignment
        (vector? current-receiver)
        (if (integer? index-val)
          (let [idx (if (< index-val 0) (+ (count current-receiver) index-val) index-val)
                ;; Expand array if needed
                expanded-array (if (>= idx (count current-receiver))
                                (vec (concat current-receiver (repeat (- idx (count current-receiver) -1) nil)))
                                current-receiver)
                updated-array (assoc expanded-array idx new-value)]
            (swap! variables assoc array updated-array)
            new-value)
          (throw (ex-info "Array index must be integer" {:array current-receiver :index index-val})))

        :else
        (throw (ex-info "Assignment to invalid type" {:receiver current-receiver :index index-val}))))))

(defn interpret-binary-operation
  "Interpret a binary operation like 1 + 2."
  [ast entity-id variables]
  (let [operator (parser/get-operator ast entity-id)
        left-id (parser/get-left ast entity-id)
        right-id (parser/get-right ast entity-id)
        left-val (interpret-expression ast left-id variables)
        right-val (interpret-expression ast right-id variables)]

    (case operator
      "+" (cond
            ;; Ruby object method call
            (satisfies? ruby-classes/RubyObject left-val)
            (ruby-classes/invoke-ruby-method left-val :+ right-val)
            ;; String concatenation for primitive strings
            (or (string? left-val) (string? right-val))
            (str left-val right-val)
            ;; Numeric addition
            :else (+ left-val right-val))
      "-" (- left-val right-val)
      "*" (* left-val right-val)
      "/" (if (and (integer? left-val) (integer? right-val))
            ;; Ruby-style integer division
            (quot left-val right-val)
            ;; Floating point division
            (/ left-val right-val))
      "%" (rem left-val right-val)
      "==" (cond
             (satisfies? ruby-classes/RubyObject left-val)
             (ruby-classes/invoke-ruby-method left-val :== right-val)
             :else (= left-val right-val))
      "!=" (cond
             (satisfies? ruby-classes/RubyObject left-val)
             (ruby-classes/invoke-ruby-method left-val :!= right-val)
             :else (not= left-val right-val))
      "<" (cond
            (satisfies? ruby-classes/RubyObject left-val)
            (ruby-classes/invoke-ruby-method left-val :< right-val)
            :else (< left-val right-val))
      ">" (cond
            (satisfies? ruby-classes/RubyObject left-val)
            (ruby-classes/invoke-ruby-method left-val :> right-val)
            :else (> left-val right-val))
      "<=" (cond
             (satisfies? ruby-classes/RubyObject left-val)
             (ruby-classes/invoke-ruby-method left-val :<= right-val)
             :else (<= left-val right-val))
      ">=" (cond
             (satisfies? ruby-classes/RubyObject left-val)
             (ruby-classes/invoke-ruby-method left-val :>= right-val)
             :else (>= left-val right-val))
      "&&" (and left-val right-val)
      "||" (or left-val right-val)
      ".." (ruby-classes/create-range left-val right-val true)
      "..." (ruby-classes/create-range left-val right-val false)
      (throw (ex-info (str "Unknown binary operator: " operator)
                      {:operator operator})))))

(defn interpret-unary-operation
  "Interpret a unary operation like -5 or !true."
  [ast entity-id variables]
  (let [operator (parser/get-operator ast entity-id)
        operand-id (parser/get-component ast entity-id :operand)
        operand-val (interpret-expression ast operand-id variables)]
    (case operator
      "-" (- operand-val)
      "!" (not operand-val)
      "+" operand-val
      (throw (ex-info (str "Unknown unary operator: " operator)
                      {:operator operator})))))

(defn handle-builtin-class-method
  "Handle built-in class methods like Integer.max, Integer.sqrt."
  [method-name args]
  (case method-name
    "max" (if (= 2 (count args))
            (let [[a b] args] (max a b))
            (throw (ex-info "Integer.max requires exactly 2 arguments" {:args args})))
    "sqrt" (if (= 1 (count args))
             (int (Math/sqrt (first args)))
             (throw (ex-info "Integer.sqrt requires exactly 1 argument" {:args args})))
    (throw (ex-info (str "Unknown built-in class method: " method-name) {:method method-name}))))

(defn handle-ruby-class-method
  "Handle Ruby class methods from our new class hierarchy."
  [class-name method-name args]
  (case [class-name method-name]
    ["Object" "new"] (if (empty? args)
                      (ruby-classes/create-object)
                      (ruby-classes/create-object (first args)))
    ["String" "new"] (if (empty? args)
                      (ruby-classes/create-string "")
                      (ruby-classes/create-string (first args)))
    (throw (ex-info (str "Unknown Ruby class method: " class-name "." method-name) 
                    {:class class-name :method method-name}))))

(defn handle-user-defined-class-method
  "Handle user-defined class methods with proper parameter binding and return handling."
  [method-info args variables ast]
  (let [method-params (:parameters method-info)
        method-body (:body method-info)
        method-ast (:ast method-info)
        method-vars (atom @variables)]
    (when (and method-params (seq args))
      (doseq [[param arg] (map vector method-params args)]
        (swap! method-vars assoc param arg)))
    (if method-body
      (try
        (interpret-expression method-ast method-body method-vars)
        (catch clojure.lang.ExceptionInfo e
          (let [ex-data (ex-data e)]
            (if (= (:type ex-data) :return)
              (:value ex-data)
              (throw e)))))
      nil)))

(defn try-class-method-call
  "Try to execute a class method call, return ::method-not-found if method not found."
  [receiver method-name args variables ast]
  (if (and (map? receiver) (:name receiver) (:class-methods receiver))
    (let [class-methods (if (instance? clojure.lang.Atom (:class-methods receiver))
                          @(:class-methods receiver)  ; User-defined class (atom)
                          (:class-methods receiver))] ; Built-in class (regular map)
      (if-let [method-info (get class-methods method-name)]
        (cond
          (:builtin-class-method method-info)
          (handle-builtin-class-method method-name args)
          
          (:ruby-class-method method-info)
          (handle-ruby-class-method (:name receiver) method-name args)
          
          :else
          (handle-user-defined-class-method method-info args variables ast))
        ::method-not-found))
    ::method-not-found))

(defn try-instance-method-call
  "Try to execute an instance method call, return ::method-not-found if method not found."
  [receiver method-name args variables ast]
  (if (and (map? receiver) (:class receiver) (:class-info receiver))
    (let [instance receiver
          class-info (:class-info instance)
          class-methods @(:methods class-info)]
      (if-let [method-info (get class-methods method-name)]
        (let [method-params (:parameters method-info)
              method-body (:body method-info)
              method-ast (:ast method-info)
              ;; Create instance method scope with self and parameters
              method-vars (atom (assoc @variables "self" instance))]

          ;; Bind method parameters to arguments
          (when (and method-params (seq args))
            (doseq [[param arg] (map vector method-params args)]
              (swap! method-vars assoc param arg)))

          ;; Execute method body with return handling, or handle special generated methods
          (let [result (cond
                        ;; Handle attr getter methods
                        (:attr-getter method-info)
                        (let [attr-var-name (str "@" (:attr-name method-info))]
                          (get @(:instance-variables instance) attr-var-name))

                        ;; Handle attr setter methods
                        (:attr-setter method-info)
                        (let [attr-var-name (str "@" (:attr-name method-info))
                              value (first args)]
                          (swap! (:instance-variables instance) assoc attr-var-name value)
                          value)

                        ;; Handle normal method body
                        method-body
                        (try
                          (interpret-expression method-ast method-body method-vars)
                          (catch clojure.lang.ExceptionInfo e
                            (let [ex-data (ex-data e)]
                              (if (= (:type ex-data) :return)
                                (:value ex-data)
                                (throw e)))))

                        ;; No method body
                        :else nil)]
            result))
        ::method-not-found))
    ::method-not-found))

(defn try-block-method
  "Try to execute a block-based method like each, map, select, etc using Enumerable."
  [receiver method-name ast entity-id variables]
  (when (enum/enumerable? receiver)
    (case method-name
      "each" (enum/enumerable-each receiver ast entity-id variables execute-block)
      "map" (enum/enumerable-map receiver ast entity-id variables execute-block)
      "collect" (enum/enumerable-map receiver ast entity-id variables execute-block) ; alias for map
      "select" (enum/enumerable-select receiver ast entity-id variables execute-block)
      "filter" (enum/enumerable-select receiver ast entity-id variables execute-block) ; alias for select
      "reject" (enum/enumerable-reject receiver ast entity-id variables execute-block)
      "find" (enum/enumerable-find receiver ast entity-id variables execute-block)
      "detect" (enum/enumerable-find receiver ast entity-id variables execute-block) ; alias for find
      "any?" (enum/enumerable-any? receiver ast entity-id variables execute-block)
      "all?" (enum/enumerable-all? receiver ast entity-id variables execute-block)
      ;; No block method matched
      nil)))


(def method-not-found ::method-not-found)

(defn try-ruby-object-method
  "Try to execute a method on a Ruby object, return ::method-not-found if not a Ruby object or method not found."
  [receiver method-name args]
  (if (satisfies? ruby-classes/RubyObject receiver)
    (try
      (apply ruby-classes/invoke-ruby-method receiver (keyword method-name) args)
      (catch Exception e
        (if (re-find #"NoMethodError" (.getMessage e))
          ::method-not-found
          (throw e))))
    ::method-not-found))

(defn try-builtin-instance-method
  "Try to execute a built-in instance method, return ::method-not-found sentinel if method not found."
  [receiver method-name args]
  (case method-name
    "length" (cond
               (ruby-array? receiver) (count @(:data receiver))
               (vector? receiver) (count receiver)
               (string? receiver) (count receiver)
               (keyword? receiver) (count (name receiver)) ; Length of symbol name
               (mutable-hash? receiver) (count @(:data receiver))
               (map? receiver) (count receiver)
               :else ::method-not-found)
    "size" (cond
             (ruby-classes/ruby-range? receiver)
             (ruby-classes/invoke-ruby-method receiver :size)
             (ruby-array? receiver) (count @(:data receiver))
             (vector? receiver) (count receiver)
             (string? receiver) (count receiver)
             (keyword? receiver) (count (name receiver)) ; Size of symbol name
             (mutable-hash? receiver) (count @(:data receiver))
             (map? receiver) (count receiver)
             :else ::method-not-found)
    "to_s" (cond
             (ruby-classes/ruby-range? receiver)
             (ruby-classes/invoke-ruby-method receiver :to_s)
             (ruby-array? receiver)
             ;; Ruby array to_s with proper formatting
             (let [array-data @(:data receiver)]
               (str "[" 
                    (clojure.string/join " " 
                      (map (fn [item]
                             (cond
                               (ruby-classes/ruby-range? item)
                               (ruby-classes/invoke-ruby-method item :to_s)
                               (keyword? item)
                               (name item)
                               (string? item)
                               (str "\"" item "\"")
                               :else
                               (str item)))
                           array-data))
                    "]"))

             (vector? receiver)
             ;; Legacy vector to_s with proper range formatting
             (str "[" 
                  (clojure.string/join " " 
                    (map (fn [item]
                           (cond
                             (ruby-classes/ruby-range? item)
                             (ruby-classes/invoke-ruby-method item :to_s)
                             (keyword? item)
                             (name item)
                             (string? item)
                             (str "\"" item "\"")
                             :else
                             (str item)))
                         receiver))
                  "]")
             (mutable-hash? receiver) (format-ruby-hash @(:data receiver))
             (keyword? receiver) (name receiver) ; Convert :hello to "hello"
             :else (str receiver))
    "first" (cond
              (ruby-array? receiver) (first @(:data receiver))
              (vector? receiver) (first receiver)
              :else ::method-not-found)
    "last" (cond
             (ruby-array? receiver) (last @(:data receiver))
             (vector? receiver) (last receiver)
             :else ::method-not-found)
    "negative?" (if (number? receiver) (< receiver 0) ::method-not-found)
    "positive?" (if (number? receiver) (> receiver 0) ::method-not-found)
    "zero?" (if (number? receiver) (= receiver 0) ::method-not-found)
    "even?" (if (integer? receiver) (even? receiver) ::method-not-found)
    "real?" (if (number? receiver) true ::method-not-found)
    "integer?" (if (number? receiver) (integer? receiver) ::method-not-found)
    "inc" (if (number? receiver) (+ receiver 1) ::method-not-found)
    "incn" (if (and (number? receiver) (= 1 (count args)) (number? (first args)))
             (+ receiver (first args))
             ::method-not-found)
    "double" (if (number? receiver) (* receiver 2) ::method-not-found)
    "empty?" (cond
               (ruby-array? receiver) (empty? @(:data receiver))
               (vector? receiver) (empty? receiver)
               (string? receiver) (empty? receiver)
               (mutable-hash? receiver) (empty? @(:data receiver))
               (map? receiver) (empty? receiver)
               :else ::method-not-found)
    "keys" (cond
             (mutable-hash? receiver) (vec (keys @(:data receiver)))
             (map? receiver) (vec (keys receiver))
             :else ::method-not-found)
    "values" (cond
               (mutable-hash? receiver) (vec (vals @(:data receiver)))
               (map? receiver) (vec (vals receiver))
               :else ::method-not-found)
    "key?" (cond
             (mutable-hash? receiver) (contains? @(:data receiver) (first args))
             (map? receiver) (contains? receiver (first args))
             :else ::method-not-found)
    "include?" (cond
                 (ruby-classes/ruby-range? receiver)
                 (ruby-classes/invoke-ruby-method receiver :include? (first args))
                 (mutable-hash? receiver) (contains? @(:data receiver) (first args))
                 (map? receiver) (contains? receiver (first args))
                 :else ::method-not-found)
    "member?" (cond
                (mutable-hash? receiver) (contains? @(:data receiver) (first args))
                (map? receiver) (contains? receiver (first args))
                :else ::method-not-found)
    "delete" (cond
               (mutable-hash? receiver)
               (let [key-to-delete (first args)
                     old-value (get @(:data receiver) key-to-delete)]
                 (swap! (:data receiver) dissoc key-to-delete)
                 old-value)
               (map? receiver)
               (get receiver (first args)) ; Legacy immutable behavior
               :else ::method-not-found)
    "remove" (cond
               (mutable-hash? receiver)
               (let [key-to-remove (first args)
                     old-value (get @(:data receiver) key-to-remove)]
                 (swap! (:data receiver) dissoc key-to-remove)
                 old-value)
               (map? receiver)
               (get receiver (first args)) ; Legacy immutable behavior
               :else ::method-not-found)
    "inspect" (cond
                (keyword? receiver) (str receiver) ; :hello -> ":hello"
                :else (str receiver))
    "id2name" (if (keyword? receiver)
                (name receiver)         ; Same as to_s for symbols
                ::method-not-found)
    ;; Range methods (each is handled by enumerable module)
    "to_a" (if (ruby-classes/ruby-range? receiver)
             (ruby-classes/invoke-ruby-method receiver :to_a)
             ::method-not-found)
    "count" (if (ruby-classes/ruby-range? receiver)
              (ruby-classes/invoke-ruby-method receiver :count)
              ::method-not-found)
    ;; Default case
    ::method-not-found))

(defn try-class-instantiation
  "Try to instantiate a class with Class.new"
  [receiver args variables ast]
  (when (and (map? receiver) (:name receiver))
    ;; Handle class instantiation: Class.new
    (let [class-info receiver
          class-methods @(:methods class-info)
          ;; Create new instance
          instance {:class (:name class-info)
                    :instance-variables (atom {})
                    :class-info class-info}]

      ;; Call initialize method if it exists
      (if-let [initialize-method (get class-methods "initialize")]
        (let [init-params (:parameters initialize-method)
              init-body (:body initialize-method)
              init-ast (:ast initialize-method)
              ;; Create instance variable scope with self reference
              instance-vars (atom (assoc @variables "self" instance))]

          ;; Bind parameters to arguments
          (when (and init-params (seq args))
            (doseq [[param arg] (map vector init-params args)]
              (swap! instance-vars assoc param arg)))

          ;; Execute initialize method body
          (when init-body
            (interpret-expression init-ast init-body instance-vars)))

        ;; No initialize method, just return instance
        nil)

      instance))) ; Return the new instance

(defn interpret-method-call
  "Interpret a method call like puts(value) or obj.method(args)."
  [ast entity-id variables]
  (let [method-name (parser/get-component ast entity-id :method)
        receiver-id (parser/get-receiver ast entity-id)
        args-ids (or (parser/get-component ast entity-id :arguments)
                     (parser/get-children ast entity-id))
        args (map #(interpret-expression ast % variables) args-ids)]

    (if receiver-id
      ;; Method call on object: obj.method(args)
      (let [receiver (interpret-expression ast receiver-id variables)]
        ;; Try different method resolution strategies
        (let [class-result (try-class-method-call receiver method-name args variables ast)
              instance-result (when (= class-result ::method-not-found)
                               (try-instance-method-call receiver method-name args variables ast))
              block-result (when (and (= class-result ::method-not-found) (= instance-result ::method-not-found))
                            (try-block-method receiver method-name ast entity-id variables))
              ruby-result (when (and (= class-result ::method-not-found) (= instance-result ::method-not-found) (nil? block-result))
                           (try-ruby-object-method receiver method-name args))
              builtin-result (when (and (= class-result ::method-not-found) (= instance-result ::method-not-found) (nil? block-result)
                                       (= ruby-result ::method-not-found))
                              (try-builtin-instance-method receiver method-name args))
              new-result (when (and (= class-result ::method-not-found) (= instance-result ::method-not-found) (nil? block-result)
                                   (= ruby-result ::method-not-found) (= builtin-result ::method-not-found)
                                   (= method-name "new"))
                          (try-class-instantiation receiver args variables ast))]
          (cond
            ;; Handle class methods - check for sentinel
            (not= class-result ::method-not-found) class-result
            ;; Handle instance methods - check for sentinel
            (not= instance-result ::method-not-found) instance-result
            block-result block-result
            ;; Handle Ruby object methods - check for sentinel  
            (not= ruby-result ::method-not-found) ruby-result
            ;; Handle builtin methods - check for sentinel
            (not= builtin-result ::method-not-found) builtin-result
            new-result new-result
            ;; No method found
            :else (throw (ex-info (str "Unknown method: " method-name " on " (type receiver))
                                 {:method method-name :receiver receiver :args args})))))

      ;; Function call: method(args)
      (case method-name
        "puts" (do
                 (if (empty? args)
                   (println)
                   (let [arg (first args)]
                     (cond
                       (ruby-array? arg)
                       ;; Print ruby array elements on separate lines
                       (doseq [item @(:data arg)]
                         (cond
                           (nil? item) (println)
                           (ruby-classes/ruby-range? item)
                           ;; Use Ruby to_s for ranges
                           (println (ruby-classes/invoke-ruby-method item :to_s))
                           (keyword? item)
                           ;; Print symbols without colon prefix
                           (println (name item))
                           :else
                           (println item)))

                       (vector? arg)
                       ;; Print legacy vector elements on separate lines
                       (doseq [item arg]
                         (cond
                           (nil? item) (println)
                           (ruby-classes/ruby-range? item)
                           ;; Use Ruby to_s for ranges
                           (println (ruby-classes/invoke-ruby-method item :to_s))
                           (keyword? item)
                           ;; Print symbols without colon prefix
                           (println (name item))
                           :else
                           (println item)))

                       (mutable-hash? arg)
                       ;; Print hash in Ruby format
                       (println (format-ruby-hash @(:data arg)))

                       (keyword? arg)
                       ;; Print symbols without colon prefix (Ruby behavior)
                       (println (name arg))

                       (ruby-classes/ruby-range? arg)
                       ;; Print ranges using Ruby to_s
                       (println (ruby-classes/invoke-ruby-method arg :to_s))

                       :else
                       (println arg))))
                 nil)
        "print" (do
                  (let [arg (first args)]
                    (if (keyword? arg)
                      (print (name arg))
                      (print arg)))
                  (flush)
                  nil)
        "p" (do
              (prn (first args))
              (first args))

        ;; Check if it's a user-defined method
        (if-let [method-def (get @variables (str "method:" method-name))]
          (interpret-user-method ast entity-id variables method-def args)
          (throw (ex-info (str "Unknown method: " method-name)
                         {:method method-name :args args})))))))

(defn interpret-user-method
  "Interpret a call to user-defined method."
  [ast entity-id variables method-def args]
  (let [{:keys [params body-id]} method-def
        local-vars (atom @variables)]
    ;; Bind parameters to arguments
    (doseq [[param arg] (map vector params args)]
      (swap! local-vars assoc param arg))
    ;; Execute method body with return handling
    (try
      (interpret-expression ast body-id local-vars)
      (catch clojure.lang.ExceptionInfo e
        (let [ex-data (ex-data e)]
          (if (= (:type ex-data) :return)
            (:value ex-data)  ; Return the explicit return value
            (throw e)))))))

(defn interpret-identifier
  "Interpret an identifier (variable reference or method call with no args)."
  [ast entity-id variables]
  (let [var-name (parser/get-value ast entity-id)]
    (cond
      ;; Check if it's a variable first
      (contains? @variables var-name)
      (get @variables var-name)

      ;; Check if it's a method definition (method call with no args)
      (contains? @variables (str "method:" var-name))
      (let [method-def (get @variables (str "method:" var-name))]
        (interpret-user-method ast entity-id variables method-def []))

      ;; Neither variable nor method found
      :else
      (throw (ex-info (str "Undefined variable: " var-name)
                      {:variable var-name})))))

(defn interpret-assignment
  "Interpret an assignment statement."
  [ast entity-id variables]
  (let [{:keys [variable value]} (parser/get-components ast entity-id [:variable :value])]
    (if value
      (let [interpreted-value (interpret-expression ast value variables)]
        (swap! variables assoc variable interpreted-value)
        interpreted-value)
      ;; Handle case where value is stored directly
      (let [direct-value (parser/get-value ast entity-id)]
        (swap! variables assoc variable direct-value)
        direct-value))))

(defn interpret-method-definition
  "Interpret a method definition like def add(a, b) ... end."
  [ast entity-id variables]
  (let [{:keys [method-name name parameters body]} (parser/get-components ast entity-id [:method-name :name :parameters :body])
        method-name (or method-name name)]
    ;; Store method definition in variables with special prefix
    (swap! variables assoc (str "method:" method-name) {:params parameters :body-id body})
    nil))

(defn interpret-if-statement
  "Interpret if/else statement."
  [ast entity-id variables]
  (let [{:keys [condition then-branch else-branch]} (parser/get-components ast entity-id [:condition :then-branch :else-branch])
        condition-val (interpret-expression ast condition variables)]
    (if condition-val
      (when then-branch (interpret-expression ast then-branch variables))
      (when else-branch (interpret-expression ast else-branch variables)))))

(defn interpret-while-statement
  "Interpret while loop with break/continue support."
  [ast entity-id variables]
  (let [{:keys [condition body]} (parser/get-components ast entity-id [:condition :body])
        result (atom nil)
        running? (atom true)]
    (while (and @running? (interpret-expression ast condition variables))
      (try
        (reset! result (interpret-expression ast body variables))
        (catch clojure.lang.ExceptionInfo e
          (let [ex-data (ex-data e)]
            (case (:type ex-data)
              :break (reset! running? false)
              :continue nil  ; Just continue to next iteration
              (throw e))))))
    @result))

(defn interpret-for-statement
  "Interpret for loop with break/continue support."
  [ast entity-id variables]
  (let [{:keys [variable iterable body]} (parser/get-components ast entity-id [:variable :iterable :body])
        iterable-value (interpret-expression ast iterable variables)
        result (atom nil)
        should-break (atom false)]
    (when (or (vector? iterable-value) (ruby-array? iterable-value))
      (let [items-to-iterate (if (ruby-array? iterable-value) 
                               @(:data iterable-value) 
                               iterable-value)]
        (loop [items items-to-iterate]
        (when (and (seq items) (not @should-break))
          (let [item (first items)]
            (try
              ;; Set the loop variable to the current item
              (swap! variables assoc variable item)
              (reset! result (interpret-expression ast body variables))
              (catch clojure.lang.ExceptionInfo e
                (let [ex-data (ex-data e)]
                  (case (:type ex-data)
                    :break (reset! should-break true)  ; Exit the loop entirely
                    :continue nil   ; Continue to next iteration
                    (throw e)))))
            (when (not @should-break)
              (recur (rest items))))))))
    @result))

(defn interpret-until-statement
  "Interpret until loop with break/continue support."
  [ast entity-id variables]
  (let [{:keys [condition body]} (parser/get-components ast entity-id [:condition :body])
        result (atom nil)
        running? (atom true)]
    (while (and @running? (not (interpret-expression ast condition variables)))
      (try
        (reset! result (interpret-expression ast body variables))
        (catch clojure.lang.ExceptionInfo e
          (let [ex-data (ex-data e)]
            (case (:type ex-data)
              :break (reset! running? false)
              :continue nil  ; Just continue to next iteration
              (throw e))))))
    @result))

(defn interpret-loop-statement
  "Interpret infinite loop with break/continue support."
  [ast entity-id variables]
  (let [body (parser/get-component ast entity-id :body)
        result (atom nil)
        running? (atom true)]
    (while @running?
      (try
        (reset! result (interpret-expression ast body variables))
        (catch clojure.lang.ExceptionInfo e
          (let [ex-data (ex-data e)]
            (case (:type ex-data)
              :break (reset! running? false)
              :continue nil  ; Just continue to next iteration
              (throw e))))))
    @result))

(defn case-equal?
  "Implement Ruby's case equality (===) operator."
  [pattern value]
  (cond
    ;; Class === instance (e.g., String === "hello")
    (and (map? pattern) (:builtin pattern))
    (case (:name pattern)
      "String" (string? value)
      "Integer" (integer? value)
      "Array" (vector? value)
      false)

    ;; Range === value
    (ruby-classes/ruby-range? pattern)
    (ruby-classes/invoke-ruby-method pattern :include? value)

    ;; Default: regular equality
    :else (= pattern value)))

(defn when-clause-matches?
  "Check if a when clause matches the given value."
  [ast clause-id expr-val variables]
  (let [condition-ids (parser/get-component ast clause-id :conditions)]
    (some (fn [condition-id]
            (let [condition-val (interpret-expression ast condition-id variables)]
              (case-equal? condition-val expr-val)))
          condition-ids)))

(defn interpret-case-statement
  "Interpret case/when statement."
  [ast entity-id variables]
  (let [{:keys [expression when-clauses else-clause]} (parser/get-components ast entity-id [:expression :when-clauses :else-clause])
        expr-val (interpret-expression ast expression variables)]

    ;; Find first matching when clause
    (if-let [matching-clause (first (filter #(when-clause-matches? ast % expr-val variables)
                                           when-clauses))]
      ;; Execute matching clause body
      (let [body-id (parser/get-component ast matching-clause :body)]
        (interpret-expression ast body-id variables))
      ;; No match found, try else clause
      (when else-clause
        (interpret-expression ast else-clause variables)))))

(defn interpret-break-statement
  "Interpret a break statement - throws a control flow exception."
  [ast entity-id variables]
  (throw (ex-info "break" {:type :break})))

(defn interpret-continue-statement
  "Interpret a continue statement - throws a control flow exception."
  [ast entity-id variables]
  (throw (ex-info "continue" {:type :continue})))

(defn interpret-next-statement
  "Interpret a next statement (Ruby's continue) - throws a control flow exception."
  [ast entity-id variables]
  (throw (ex-info "next" {:type :continue})))

(defn interpret-return-statement
  "Interpret a return statement - throws a control flow exception with the return value."
  [ast entity-id variables]
  (let [value-id (parser/get-component ast entity-id :value)
        return-value (if value-id
                       (interpret-expression ast value-id variables)
                       nil)]
    (throw (ex-info "return" {:type :return :value return-value}))))




(defn interpret-class-definition
  "Interpret a class definition - stores class info in variables for later instantiation."
  ([ast entity-id variables]
   (interpret-class-definition ast entity-id variables {}))
  ([ast entity-id variables opts]
   (let [class-name (parser/get-component ast entity-id :name)
         parent-class (parser/get-component ast entity-id :parent-class)
         body-id (parser/get-component ast entity-id :body)]

     ;; Create class object with instance methods and class methods
     (let [class-methods (atom {})
           class-class-methods (atom {})
           class-info {:name class-name
                       :parent-class parent-class
                       :methods class-methods
                       :class-methods class-class-methods
                       :ast ast
                       :body-id body-id}]

      ;; Process class body to extract method definitions
      (when body-id
        (let [method-statements (parser/get-children ast body-id)]
          (doseq [method-id method-statements]
            (let [method-type (parser/get-node-type ast method-id)]
              (cond
                ;; Instance methods (def method_name)
                (= method-type :method-definition)
                (let [method-name (parser/get-component ast method-id :name)
                      method-params (parser/get-component ast method-id :parameters)
                      method-body (parser/get-component ast method-id :body)]
                  (swap! class-methods assoc method-name
                         {:name method-name
                          :parameters method-params
                          :body method-body
                          :ast ast}))

                ;; Class methods (def self.method_name)
                (= method-type :class-method-definition)
                (let [method-name (parser/get-component ast method-id :name)
                      method-params (parser/get-component ast method-id :parameters)
                      method-body (parser/get-component ast method-id :body)]
                  (swap! class-class-methods assoc method-name
                         {:name method-name
                          :parameters method-params
                          :body method-body
                          :ast ast}))

                ;; Handle attr statements - generate getter/setter methods
                (= method-type :attr-accessor-statement)
                (let [attributes (parser/get-component ast method-id :attributes)]
                  (doseq [attr-name attributes]
                    ;; Generate getter method
                    (swap! class-methods assoc attr-name
                           {:name attr-name
                            :parameters []
                            :attr-getter true
                            :attr-name attr-name})
                    ;; Generate setter method
                    (swap! class-methods assoc (str attr-name "=")
                           {:name (str attr-name "=")
                            :parameters ["value"]
                            :attr-setter true
                            :attr-name attr-name})))

                (= method-type :attr-reader-statement)
                (let [attributes (parser/get-component ast method-id :attributes)]
                  (doseq [attr-name attributes]
                    ;; Generate only getter method
                    (swap! class-methods assoc attr-name
                           {:name attr-name
                            :parameters []
                            :attr-getter true
                            :attr-name attr-name})))

                (= method-type :attr-writer-statement)
                (let [attributes (parser/get-component ast method-id :attributes)]
                  (doseq [attr-name attributes]
                    ;; Generate only setter method
                    (swap! class-methods assoc (str attr-name "=")
                           {:name (str attr-name "=")
                            :parameters ["value"]
                            :attr-setter true
                            :attr-name attr-name})))
                
                ;; Skip any other statements during class definition
                :else
                nil))) ; Skip all other statement types during class definition


       ;; Store class in variables with "class:" prefix
       (swap! variables assoc (str "class:" class-name) class-info)

       ;; Also make class name available for Class.new syntax
       (swap! variables assoc class-name class-info)

       nil))))))

(defn interpret-instance-variable-access
  "Interpret instance variable access (@var)."
  [ast entity-id variables]
  (let [var-name (parser/get-component ast entity-id :variable)]
    ;; Look for 'self' in the current variable scope
    (if-let [instance (get @variables "self")]
      (if (and (map? instance) (:instance-variables instance))
        (get @(:instance-variables instance) var-name)
        (throw (ex-info (str "Instance variable " var-name " accessed outside of instance context")
                       {:variable var-name})))
      (throw (ex-info (str "Instance variable " var-name " accessed outside of instance context")
                     {:variable var-name})))))

(defn interpret-instance-variable-assignment
  "Interpret instance variable assignment (@var = value)."
  [ast entity-id variables]
  (let [var-name (parser/get-component ast entity-id :variable)
        value-id (parser/get-component ast entity-id :value)
        value (interpret-expression ast value-id variables)]
    ;; Look for 'self' in the current variable scope
    (if-let [instance (get @variables "self")]
      (if (and (map? instance) (:instance-variables instance))
        (do
          (swap! (:instance-variables instance) assoc var-name value)
          value)
        (throw (ex-info (str "Instance variable " var-name " assigned outside of instance context")
                       {:variable var-name :value value})))
      (throw (ex-info (str "Instance variable " var-name " assigned outside of instance context")
                     {:variable var-name :value value})))))

(defn interpret-block
  "Interpret a block of statements."
  [ast entity-id variables]
  (let [statement-ids (parser/get-component ast entity-id :statements)]
    (if statement-ids
      (let [results (map #(interpret-expression ast % variables) statement-ids)]
        (last results)) ; Return the last expression result
      nil)))

(defn interpret-expression
  "Main expression interpreter dispatcher."
  ([ast entity-id]
   (interpret-expression ast entity-id (atom {})))
  ([ast entity-id variables]
   (let [node-type (parser/get-node-type ast entity-id)]
     (case node-type
       :integer-literal (interpret-literal ast entity-id)
       :string-literal (interpret-literal ast entity-id)
       :interpolated-string (interpret-interpolated-string ast entity-id variables)
       :boolean-literal (interpret-literal ast entity-id)
       :nil-literal (interpret-literal ast entity-id)
       :symbol-literal (interpret-literal ast entity-id)
       :array-literal (interpret-array-literal ast entity-id variables)
       :hash-literal (interpret-hash-literal ast entity-id variables)
       :binary-operation (interpret-binary-operation ast entity-id variables)
       :unary-operation (interpret-unary-operation ast entity-id variables)
       :method-call (interpret-method-call ast entity-id variables)
       :identifier (interpret-identifier ast entity-id variables)
       :assignment-statement (interpret-assignment ast entity-id variables)
       :array-access (interpret-array-access ast entity-id variables)
       :array-assignment (interpret-array-assignment ast entity-id variables)
       :indexed-assignment-statement (interpret-indexed-assignment ast entity-id variables)
       :instance-variable-access (interpret-instance-variable-access ast entity-id variables)
       :instance-variable-assignment (interpret-instance-variable-assignment ast entity-id variables)
       :method-definition (interpret-method-definition ast entity-id variables)
       :class-definition (interpret-class-definition ast entity-id variables)
       :if-statement (interpret-if-statement ast entity-id variables)
       :while-statement (interpret-while-statement ast entity-id variables)
       :for-statement (interpret-for-statement ast entity-id variables)
       :until-statement (interpret-until-statement ast entity-id variables)
       :loop-statement (interpret-loop-statement ast entity-id variables)
       :case-statement (let [result (interpret-case-statement ast entity-id variables)]
                         (when (= "true" (System/getenv "RUBY_VERBOSE"))
                           (println "DEBUG: Case statement returned:" result))
                         result)
       :when-clause nil  ; When clauses are handled by case statements, not directly
       :break-statement (interpret-break-statement ast entity-id variables)
       :continue-statement (interpret-continue-statement ast entity-id variables)
       :next-statement (interpret-next-statement ast entity-id variables)
       :return-statement (interpret-return-statement ast entity-id variables)
       :block (interpret-block ast entity-id variables)
       :attr-accessor-statement nil ; These are handled during class definition
       :attr-reader-statement nil   ; These are handled during class definition
       :attr-writer-statement nil   ; These are handled during class definition

       ;; For unknown node types, try to get the value
       (parser/get-value ast entity-id)))))

(defn interpret-expression-with-opts
  "Main expression interpreter dispatcher with options support."
  ([ast entity-id opts]
   (interpret-expression-with-opts ast entity-id (atom {}) opts))
  ([ast entity-id variables opts]
   (let [node-type (parser/get-node-type ast entity-id)]
     (case node-type
       :integer-literal (interpret-literal ast entity-id)
       :string-literal (interpret-literal ast entity-id)
       :interpolated-string (interpret-interpolated-string ast entity-id variables)
       :boolean-literal (interpret-literal ast entity-id)
       :nil-literal (interpret-literal ast entity-id)
       :symbol-literal (interpret-literal ast entity-id)
       :array-literal (interpret-array-literal ast entity-id variables)
       :hash-literal (interpret-hash-literal ast entity-id variables)
       :binary-operation (interpret-binary-operation ast entity-id variables)
       :unary-operation (interpret-unary-operation ast entity-id variables)
       :method-call (interpret-method-call ast entity-id variables)
       :identifier (interpret-identifier ast entity-id variables)
       :assignment-statement (interpret-assignment ast entity-id variables)
       :array-access (interpret-array-access ast entity-id variables)
       :array-assignment (interpret-array-assignment ast entity-id variables)
       :indexed-assignment-statement (interpret-indexed-assignment ast entity-id variables)
       :instance-variable-access (interpret-instance-variable-access ast entity-id variables)
       :instance-variable-assignment (interpret-instance-variable-assignment ast entity-id variables)
       :method-definition (interpret-method-definition ast entity-id variables)
       :class-definition (interpret-class-definition ast entity-id variables opts)
       :if-statement (interpret-if-statement ast entity-id variables)
       :while-statement (interpret-while-statement ast entity-id variables)
       :for-statement (interpret-for-statement ast entity-id variables)
       :until-statement (interpret-until-statement ast entity-id variables)
       :loop-statement (interpret-loop-statement ast entity-id variables)
       :case-statement (let [result (interpret-case-statement ast entity-id variables)]
                         (when (= "true" (System/getenv "RUBY_VERBOSE"))
                           (println "DEBUG: Case statement returned:" result))
                         result)
       :when-clause nil  ; When clauses are handled by case statements, not directly
       :break-statement (interpret-break-statement ast entity-id variables)
       :continue-statement (interpret-continue-statement ast entity-id variables)
       :next-statement (interpret-next-statement ast entity-id variables)
       :return-statement (interpret-return-statement ast entity-id variables)
       :block (interpret-block ast entity-id variables)
       :attr-accessor-statement nil ; These are handled during class definition
       :attr-reader-statement nil   ; These are handled during class definition
       :attr-writer-statement nil   ; These are handled during class definition

       ;; For unknown node types, try to get the value
       (parser/get-value ast entity-id)))))

(defn create-builtin-classes
  "Create built-in classes like Integer, String, etc."
  []
  {"Integer" {:name "Integer"
              :builtin true
              :class-methods {"max" {:builtin-class-method true :name "max"}
                              "sqrt" {:builtin-class-method true :name "sqrt"}}}
   "Object" {:name "Object"
             :builtin true
             :ruby-class true  ; Mark as new Ruby class
             :class-methods {"new" {:ruby-class-method true :name "new"}}}
   "String" {:name "String"
             :builtin true
             :ruby-class true  ; Mark as new Ruby class
             :class-methods {"new" {:ruby-class-method true :name "new"}}}})

(defn interpret-program
  "Interpret a complete program (sequence of statements)."
  ([ast root-entity-id]
   (interpret-program ast root-entity-id {}))
  ([ast root-entity-id opts]
   (let [builtin-vars (create-builtin-classes)
         custom-vars (get opts :namespaces {})
         initial-vars (merge builtin-vars custom-vars)
         variables (atom initial-vars)
         root-type (parser/get-node-type ast root-entity-id)]
     (if (= :program root-type)
       ;; Handle program with multiple statements
       (let [statement-ids (parser/get-children ast root-entity-id)]
         (let [results (map #(interpret-expression-with-opts ast % variables opts) statement-ids)]
           (last results))) ; Return the last expression result
       ;; Handle single expression
       (interpret-expression-with-opts ast root-entity-id variables opts)))))

(defn evaluate-directly
  "Main entry point for direct AST interpretation."
  ([ast root-entity-id]
   (evaluate-directly ast root-entity-id {}))
  ([ast root-entity-id opts]
   (try
     (interpret-program ast root-entity-id opts)
     (catch Exception e
       (println "Interpreter Error:" (.getMessage e))
       (throw e)))))
