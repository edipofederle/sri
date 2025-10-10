(ns sri.ruby-basic-object
  "Ruby BasicObject class - root of the Ruby class hierarchy."
  (:require [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                         ruby-class ruby-ancestors respond-to?
                                         to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup]]))

;; =============================================================================
;; BasicObject - Root of Ruby class hierarchy
;; =============================================================================

(defrecord BasicObject []
  RubyObject
  (ruby-class [_] "BasicObject")
  (ruby-ancestors [_] ["BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :== :!= :equal? :object_id} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))

  RubyInspectable
  (to-s [_] "#<BasicObject>")
  (inspect [this] (to-s this))

  RubyComparable
  (ruby-eq [this other]
    (identical? this other))
  (ruby-compare [this other]
    (cond
      (identical? this other) 0
      :else nil))) ; BasicObject doesn't implement <=> by default

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-basic-object-methods!
  "Register all BasicObject methods in the method registry."
  []
  (register-method "BasicObject" :to_s #(to-s %))
  (register-method "BasicObject" :inspect #(inspect %))
  (register-method "BasicObject" :== #(ruby-eq %1 %2))
  (register-method "BasicObject" :!= #(not (ruby-eq %1 %2)))
  (register-method "BasicObject" :equal? #(identical? %1 %2))
  (register-method "BasicObject" :object_id #(System/identityHashCode %)))

;; Register methods on namespace load
(register-basic-object-methods!)

;; =============================================================================
;; Constructor Function
;; =============================================================================

(defn create-basic-object
  "Create a new BasicObject instance."
  []
  (->BasicObject))