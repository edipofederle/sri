(ns sri.ruby-true-class
  "Ruby TrueClass implementation.

   true is the only instance of TrueClass.
   https://docs.ruby-lang.org/en/master/TrueClass.html"
  (:require [sri.ruby-method-registry :refer [register-method]]))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-true-class-methods!
  "Register all TrueClass methods in the method registry."
  []
  ;; Core methods
  (register-method "TrueClass" :to_s (fn [_] "true"))
  (register-method "TrueClass" :inspect (fn [_] "true"))

  ;; Predicate methods
  (register-method "TrueClass" :nil? (fn [_] false))

  ;; Comparison
  (register-method "TrueClass" :== (fn [_ other] (true? other)))
  (register-method "TrueClass" :!= (fn [_ other] (not (true? other))))
  (register-method "TrueClass" :=== (fn [_ other] (true? other)))

  ;; Logical operations
  (register-method "TrueClass" "&" (fn [_ other] (if other true false)))
  (register-method "TrueClass" "|" (fn [_ _other] true))
  (register-method "TrueClass" "^" (fn [_ other] (if other false true)))

  ;; Object methods
  (register-method "TrueClass" :class (fn [_] {:name "TrueClass" :builtin true})))

;; Register methods on namespace load
(register-true-class-methods!)
