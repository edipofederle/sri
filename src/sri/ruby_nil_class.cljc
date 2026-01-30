(ns sri.ruby-nil-class
  "Ruby NilClass implementation.

   nil is the only instance of NilClass.
   https://docs.ruby-lang.org/en/master/NilClass.html"
  (:require [sri.ruby-method-registry :refer [register-method]]))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-nil-class-methods!
  "Register all NilClass methods in the method registry."
  []
  ;; Core methods
  (register-method "NilClass" :to_s (fn [_] ""))
  (register-method "NilClass" :inspect (fn [_] "nil"))
  (register-method "NilClass" :to_a (fn [_] []))
  (register-method "NilClass" :to_h (fn [_] {}))
  (register-method "NilClass" :to_i (fn [_] 0))
  (register-method "NilClass" :to_f (fn [_] 0.0))

  ;; Predicate methods
  (register-method "NilClass" :nil? (fn [_] true))

  ;; Comparison
  (register-method "NilClass" :== (fn [_ other] (nil? other)))
  (register-method "NilClass" :!= (fn [_ other] (not (nil? other))))
  (register-method "NilClass" :=== (fn [_ other] (nil? other)))

  ;; Logical operations
  (register-method "NilClass" "&" (fn [_ _other] false))
  (register-method "NilClass" "|" (fn [_ other] (if other true false)))
  (register-method "NilClass" "^" (fn [_ other] (if other true false)))

  ;; Object methods
  (register-method "NilClass" :class (fn [_] {:name "NilClass" :builtin true})))

;; Register methods on namespace load
(register-nil-class-methods!)
