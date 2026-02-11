(ns sri.ruby-symbol
  "Ruby Symbol class implementation."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]
            [sri.ruby-kernel :as kernel]
            [sri.ruby-string :as ruby-str]))

;; =============================================================================
;; Symbol - Ruby Symbol class
;; =============================================================================

(def ^:private bare-symbol-re
  "Matches symbol names that don't need quoting: identifiers, @var, @@var, $var."
  #"(?:@@?|\$)?[a-zA-Z_][a-zA-Z0-9_]*[?!=]?")

(def ^:private bare-operator-symbols
  #{"~" "-" "+" "*" "/" "%" "<" ">" "&" "|" "^" "!" "="
    "==" "!=" "<=>" "<<" ">>" "<=" ">=" "=~" "!~" "**"
    "[]" "[]=" "`"})

(defn- bare-symbol-name?
  "Check if a symbol name can be represented without quoting in inspect output."
  [s]
  (or (boolean (re-matches bare-symbol-re s))
      (contains? bare-operator-symbols s)))

(defn- escape-symbol-name
  "Escape special characters in a symbol name for quoted inspect output."
  [s]
  (-> s
      (str/replace "\n" "\\n")
      (str/replace "\t" "\\t")
      (str/replace "\r" "\\r")))

(defrecord RubySymbol [name]
  RubyObject
  (ruby-class [_] "Symbol")
  (ruby-ancestors [_] ["Symbol" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :to_sym :id2name :length :size
                 :== :!= :=== :<=> :empty?
                 :upcase :downcase :capitalize
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil? :puts :p :print} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))

  RubyInspectable
  (to-s [this] (:name this))  ; :foo -> "foo"
  (inspect [this]
    (if (bare-symbol-name? (:name this))
      (str ":" (:name this))              ; :foo -> ":foo"
      (str ":\"" (escape-symbol-name (:name this)) "\"")))

  RubyComparable
  (ruby-eq [this other]
    (cond
      (instance? RubySymbol other) (= (:name this) (:name other))
      (keyword? other) (= (:name this) (name other))
      :else false))
  (ruby-compare [this other]
    (cond
      (instance? RubySymbol other) (compare (:name this) (:name other))
      (keyword? other) (compare (:name this) (name other))
      :else nil)))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn ruby-symbol?
  "Check if a value is a Ruby symbol."
  [value]
  (instance? RubySymbol value))

(defn create-symbol
  "Create a new Ruby symbol from a string name or Clojure keyword."
  [name]
  (->RubySymbol (if (keyword? name) (clojure.core/name name) name)))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-symbol-methods!
  "Register all Symbol methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Symbol" :to_s #(ruby-str/create-string (to-s %)))
  (register-method "Symbol" :inspect #(ruby-str/create-string (inspect %)))
  (register-method "Symbol" :== #(ruby-eq %1 %2))
  (register-method "Symbol" :!= #(not (ruby-eq %1 %2)))
  (register-method "Symbol" :equal? #(identical? %1 %2))
  (register-method "Symbol" :object_id #(System/identityHashCode %))
  (register-method "Symbol" :class #(ruby-class %))
  (register-method "Symbol" :nil? (fn [_] false)) ; Symbols are never nil
  (register-method "Symbol" :respond_to? #(respond-to? %1 %2))
  (register-method "Symbol" :methods #(class-methods (ruby-class %)))
  (register-method "Symbol" :instance_of?
    (fn [obj klass]
      (let [class-name (if (map? klass) (:name klass) klass)]
        (= (ruby-class obj) class-name))))
  (let [kind-of-fn (fn [obj klass]
                     (let [class-name (if (map? klass) (:name klass) klass)]
                       (contains? (set (ruby-ancestors obj)) class-name)))]
    (register-method "Symbol" :kind_of? kind-of-fn)
    (register-method "Symbol" :is_a? kind-of-fn))

  ;; Symbol-specific methods
  (let [length-fn #(count (:name %))]
    (register-method "Symbol" :length length-fn)
    (register-method "Symbol" :size length-fn))

  (register-method "Symbol" :to_sym identity)
  (register-method "Symbol" :id2name to-s)

  (register-method "Symbol" :empty? #(empty? (:name %)))

  (register-method "Symbol" :upcase
    #(->RubySymbol (str/upper-case (:name %))))

  (register-method "Symbol" :downcase
    #(->RubySymbol (str/lower-case (:name %))))

  (register-method "Symbol" :capitalize
    #(->RubySymbol (str/capitalize (:name %))))

  ;; Include Kernel methods
  (kernel/register-kernel-methods-for-class! "Symbol"))

;; Register methods on namespace load
(register-symbol-methods!)
