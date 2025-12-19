(ns sri.ruby-hash
  "Ruby Hash class implementation with proper mutability."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]
            [sri.ruby-kernel :as kernel]
            [sri.ruby-array :as ruby-array]))

;; =============================================================================
;; Mutable Hash Wrapper
;; =============================================================================

(defrecord RubyHash [data]  ; data is an atom containing a map
  RubyObject
  (ruby-class [_] "Hash")
  (ruby-ancestors [_] ["Hash" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :length :size "[]" "[]=" :keys :values
                 :empty? :== :!= :key? :include? :member? :delete :remove
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil? :puts :p :print} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))
  
  RubyInspectable
  (to-s [this] 
    (let [hash-data @(:data this)]
      (if (empty? hash-data)
        "{}"
        (let [pairs (map (fn [[k v]]
                           (str (cond
                                  (string? k) (str "\"" k "\"")
                                  (satisfies? RubyInspectable k) (str "\"" (to-s k) "\"")
                                  (keyword? k) (name k)
                                  :else (str k))
                                "=>"
                                (cond
                                  (string? v) (str "\"" v "\"")
                                  (satisfies? RubyInspectable v) (if (string? (to-s v))
                                                                    (str "\"" (to-s v) "\"")
                                                                    (to-s v))
                                  (keyword? v) (name v)
                                  :else (str v))))
                         hash-data)]
          (str "{" (str/join ", " pairs) "}")))))
  (inspect [this] (to-s this))
  
  RubyComparable
  (ruby-eq [this other]
    (and (instance? RubyHash other)
         (= @(:data this) @(:data other))))
  (ruby-compare [this other]
    (when (instance? RubyHash other)
      (compare @(:data this) @(:data other)))))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn ruby-hash?
  "Check if a value is a Ruby hash."
  [value]
  (instance? RubyHash value))

(defn hash-get
  "Get value for key from Ruby hash."
  [ruby-hash key]
  (get @(:data ruby-hash) key))

(defn hash-set!
  "Set value for key in Ruby hash (mutating operation)."
  [ruby-hash key value]
  (swap! (:data ruby-hash) assoc key value)
  value)

(defn hash-delete!
  "Delete key from Ruby hash, returning the old value (mutating operation)."
  [ruby-hash key]
  (let [old-value (get @(:data ruby-hash) key)]
    (swap! (:data ruby-hash) dissoc key)
    old-value))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-hash-methods!
  "Register all Hash methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Hash" :to_s #(to-s %))
  (register-method "Hash" :inspect #(inspect %))
  (register-method "Hash" :== #(ruby-eq %1 %2))
  (register-method "Hash" :!= #(not (ruby-eq %1 %2)))
  (register-method "Hash" :equal? #(identical? %1 %2))
  (register-method "Hash" :object_id #(System/identityHashCode %))
  (register-method "Hash" :class #(ruby-class %))
  (register-method "Hash" :nil? #(false)) ; Hashes are never nil
  (register-method "Hash" :respond_to? #(respond-to? %1 %2))
  (register-method "Hash" :methods #(class-methods (ruby-class %)))
  (register-method "Hash" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Hash" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Hash" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Hash-specific methods
  (register-method "Hash" :length #(count @(:data %)))
  (register-method "Hash" :size #(count @(:data %))) ; alias for length

  (register-method "Hash" "[]"
    (fn [ruby-hash key]
      (hash-get ruby-hash key)))

  (register-method "Hash" "[]="
    (fn [ruby-hash key value]
      (hash-set! ruby-hash key value)))

  (register-method "Hash" :keys
    (fn [ruby-hash]
      (ruby-array/vector->ruby-array (vec (keys @(:data ruby-hash))))))

  (register-method "Hash" :values
    (fn [ruby-hash]
      (ruby-array/vector->ruby-array (vec (vals @(:data ruby-hash))))))

  (register-method "Hash" :empty?
    (fn [ruby-hash]
      (empty? @(:data ruby-hash))))

  (register-method "Hash" :key?
    (fn [ruby-hash key]
      (contains? @(:data ruby-hash) key)))

  (register-method "Hash" :include?
    (fn [ruby-hash key]
      (contains? @(:data ruby-hash) key)))

  (register-method "Hash" :member?
    (fn [ruby-hash key]
      (contains? @(:data ruby-hash) key)))

  (register-method "Hash" :delete
    (fn [ruby-hash key]
      (hash-delete! ruby-hash key)))

  (register-method "Hash" :remove
    (fn [ruby-hash key]
      (hash-delete! ruby-hash key)))

  ;; Inherit Kernel methods
  (kernel/register-kernel-methods-for-class! "Hash"))

;; Register methods on namespace load
(register-hash-methods!)

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn create-hash
  "Create a new Ruby hash from key-value pairs."
  [& kvs]
  (->RubyHash (atom (apply hash-map kvs))))

(defn create-empty-hash
  "Create a new empty Ruby hash."
  []
  (->RubyHash (atom {})))

(defn map->ruby-hash
  "Convert a Clojure map to a Ruby hash."
  [m]
  (->RubyHash (atom m)))