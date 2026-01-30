(ns sri.ruby-false-class
  "Ruby FalseClass implementation.

   false is the only instance of FalseClass.
   https://docs.ruby-lang.org/en/master/FalseClass.html"
  (:require [sri.ruby-method-registry :refer [register-method]]))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-false-class-methods!
  "Register all FalseClass methods in the method registry."
  []
  ;; Core methods
  (register-method "FalseClass" :to_s (fn [_] "false"))
  (register-method "FalseClass" :inspect (fn [_] "false"))

  ;; Predicate methods
  (register-method "FalseClass" :nil? (fn [_] false))

  ;; Comparison
  (register-method "FalseClass" :== (fn [_ other] (false? other)))
  (register-method "FalseClass" :!= (fn [_ other] (not (false? other))))
  (register-method "FalseClass" :=== (fn [_ other] (false? other)))

  ;; Logical operations
  (register-method "FalseClass" "&" (fn [_ _other] false))
  (register-method "FalseClass" "|" (fn [_ other] (if other true false)))
  (register-method "FalseClass" "^" (fn [_ other] (if other true false)))

  ;; Object methods
  (register-method "FalseClass" :class (fn [_] {:name "FalseClass" :builtin true})))

;; Register methods on namespace load
(register-false-class-methods!)
