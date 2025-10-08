(ns sri.inline-cache
  "Inline Cache implementation for SRI method dispatch optimization.
   
   IC States:
   - EMPTY: No cached data
   - MONOMORPHIC: Single type cached
   - POLYMORPHIC: Multiple types cached (up to 4)
   - MEGAMORPHIC: Too many types, fall back to hash lookup"
  (:require [clojure.core :as core]))

;; IC Configuration
(def ^:const MAX-POLYMORPHIC-ENTRIES 4)
(def ^:const MEGAMORPHIC-THRESHOLD 8)

;; IC State Types
(defn empty-ic []
  {:type :empty
   :hit-count 0
   :miss-count 0})

(defn monomorphic-ic [class-name method-info]
  {:type :monomorphic
   :class-name class-name
   :method-info method-info
   :hit-count 0
   :miss-count 0})

(defn polymorphic-ic [entries]
  {:type :polymorphic
   :entries entries  ; vector of [class-name method-info] pairs
   :hit-count 0
   :miss-count 0})

(defn megamorphic-ic []
  {:type :megamorphic
   :hit-count 0
   :miss-count 0})

;; IC State Transitions
(defn ic-lookup
  "Fast path IC lookup. Returns [hit? method-info] tuple."
  [ic-state class-name]
  (case (:type ic-state)
    :empty [false nil]
    
    :monomorphic
    (if (= (:class-name ic-state) class-name)
      [true (:method-info ic-state)]
      [false nil])
    
    :polymorphic
    (if-let [method-info (some #(when (= (first %) class-name) (second %))
                               (:entries ic-state))]
      [true method-info]
      [false nil])
    
    :megamorphic [false nil]))

(defn ic-update
  "Update IC state with new lookup result. Returns new IC state."
  [ic-state class-name method-info]
  (case (:type ic-state)
    :empty
    (monomorphic-ic class-name method-info)
    
    :monomorphic
    (if (= (:class-name ic-state) class-name)
      ic-state  ; Already cached
      (polymorphic-ic [[(:class-name ic-state) (:method-info ic-state)]
                       [class-name method-info]]))
    
    :polymorphic
    (let [entries (:entries ic-state)
          existing-entry (some #(when (= (first %) class-name) %) entries)]
      (if existing-entry
        ic-state  ; Already in polymorphic cache
        (let [new-entries (conj entries [class-name method-info])]
          (if (>= (count new-entries) MAX-POLYMORPHIC-ENTRIES)
            (megamorphic-ic)  ; Transition to megamorphic
            (polymorphic-ic new-entries)))))
    
    :megamorphic
    ic-state))  ; No further transitions

(defn ic-hit [ic-state]
  "Record a cache hit. Returns updated IC state."
  (update ic-state :hit-count inc))

(defn ic-miss [ic-state]
  "Record a cache miss. Returns updated IC state."
  (update ic-state :miss-count inc))

;; IC Statistics
(defn ic-hit-rate [ic-state]
  "Calculate hit rate percentage."
  (let [hits (:hit-count ic-state)
        misses (:miss-count ic-state)
        total (+ hits misses)]
    (if (zero? total)
      0.0
      (* 100.0 (/ hits total)))))

(defn ic-stats [ic-state]
  "Get IC statistics for profiling."
  {:type (:type ic-state)
   :hits (:hit-count ic-state)
   :misses (:miss-count ic-state)
   :hit-rate (ic-hit-rate ic-state)
   :entries (case (:type ic-state)
              :monomorphic 1
              :polymorphic (count (:entries ic-state))
              0)})

;; Helper for extracting class name from receiver
(defn get-receiver-class-name [receiver]
  "Extract class name from receiver object."
  (cond
    (and (map? receiver) (:class receiver))
    (:class receiver)
    
    (integer? receiver) "Integer"
    (string? receiver) "String"
    (boolean? receiver) "Boolean"
    (nil? receiver) "NilClass"
    (keyword? receiver) "Symbol"
    (vector? receiver) "Array"
    (map? receiver) "Hash"
    
    :else "Object"))

;; Detailed debugging functions
(defn ic-detailed-stats [ic-state]
  "Get detailed IC statistics with cache content information."
  (let [base-stats (ic-stats ic-state)]
    (merge base-stats
           {:total-calls (+ (:hits base-stats) (:misses base-stats))
            :cache-content (case (:type ic-state)
                             :empty nil
                             :monomorphic {:class (:class-name ic-state)
                                          :method-type (cond
                                                        (:attr-getter (:method-info ic-state)) "attr_getter"
                                                        (:attr-setter (:method-info ic-state)) "attr_setter"
                                                        (:ast (:method-info ic-state)) "user_method"
                                                        :else "unknown")}
                             :polymorphic {:classes (mapv first (:entries ic-state))
                                          :method-types (mapv (fn [[_ method-info]]
                                                               (cond
                                                                 (:attr-getter method-info) "attr_getter"
                                                                 (:attr-setter method-info) "attr_setter"
                                                                 (:ast method-info) "user_method"
                                                                 :else "unknown"))
                                                             (:entries ic-state))}
                             :megamorphic {:note "Too many types, using fallback"})})))

(defn format-ic-debug [ic-state method-name call-site]
  "Format IC state for debug output."
  (let [stats (ic-detailed-stats ic-state)
        total-calls (:total-calls stats)]
    (str "IC DEBUG [" call-site "] method=" method-name 
         " | state=" (:type stats)
         " | hits=" (:hits stats) "/" total-calls
         " (" (format "%.1f" (:hit-rate stats)) "%)"
         " | " (case (:type stats)
                 :empty "cache empty"
                 :monomorphic (str "cached: " (get-in stats [:cache-content :class]) 
                                  " (" (get-in stats [:cache-content :method-type]) ")")
                 :polymorphic (str "cached: " (count (:entries ic-state)) " types: " 
                                  (clojure.string/join ", " (get-in stats [:cache-content :classes])))
                 :megamorphic "fallback mode"))))

(defn ic-transition-message [old-state new-state class-name method-name]
  "Generate a message when IC state transitions."
  (when (not= (:type old-state) (:type new-state))
    (str "IC TRANSITION: " (:type old-state) " → " (:type new-state) 
         " for method=" method-name " class=" class-name)))