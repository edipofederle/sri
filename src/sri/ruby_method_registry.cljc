(ns sri.ruby-method-registry
  "Method registry and lookup system for Ruby objects."
  (:require [sri.ruby-protocols :refer [ruby-ancestors]]))

;; =============================================================================
;; Method Registry and Lookup
;; =============================================================================

(def ^:private method-registry
  "Registry of methods for each Ruby class."
  (atom {}))

(defn register-method
  "Register a method implementation for a Ruby class."
  [class-name method-name method-fn]
  (swap! method-registry
         assoc-in [class-name method-name] method-fn))

(defn has-method?
  "Check if a class has a specific method."
  [class-name method-name]
  (contains? (get @method-registry class-name {}) method-name))

(defn get-ruby-method-impl
  "Get method implementation for a class."
  [class-name method-name]
  (get-in @method-registry [class-name method-name]))

(defn method-lookup
  "Look up method in ancestor chain following Ruby's method resolution order."
  [obj method-name]
  (let [class-chain (ruby-ancestors obj)]
    (loop [chain class-chain]
      (when (seq chain)
        (let [current-class (first chain)]
          (if (has-method? current-class method-name)
            (get-ruby-method-impl current-class method-name)
            (recur (rest chain))))))))

(defn call-ruby-method
  "Call a method on a Ruby object with proper method lookup."
  [obj method-name & args]
  (if-let [method-fn (method-lookup obj method-name)]
    (apply method-fn obj args)
    (throw (ex-info (str "NoMethodError: undefined method `" method-name "` for " (ruby-ancestors obj))
                    {:object obj :method method-name :args args}))))

;; =============================================================================
;; Debugging and Introspection
;; =============================================================================

(defn debug-method-registry
  "Return the current method registry for debugging."
  []
  @method-registry)

(defn class-methods
  "Get all methods for a Ruby class."
  [class-name]
  (keys (get @method-registry class-name {})))

(defn all-ruby-classes
  "Get all registered Ruby classes."
  []
  (keys @method-registry))

(defn get-method-registry
  "Get the method registry atom for direct access."
  []
  method-registry)