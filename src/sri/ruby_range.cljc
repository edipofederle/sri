(ns sri.ruby-range
  "Ruby Range class implementation."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]))

;; =============================================================================
;; Range - Ruby Range class
;; =============================================================================

(defrecord RubyRange [start end inclusive?]
  RubyObject
  (ruby-class [_] "Range")
  (ruby-ancestors [_] ["Range" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :== :!= :< :> :<= :>= :include? :member?
                 :size :count :to_a :each :first :last :min :max
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil? :puts :p :print} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))

  RubyInspectable
  (to-s [{:keys [start end inclusive?]}]
    (if (and (string? start) (string? end)
             (= 1 (count start)) (= 1 (count end)))
      ;; Character range: "a".."z"
      (str "\"" start "\"" (if inclusive? ".." "...") "\"" end "\"")
      ;; Numeric range: 1..5
      (str start (if inclusive? ".." "...") end)))
  (inspect [this] (to-s this))

  RubyComparable
  (ruby-eq [{:keys [start end inclusive?]} other]
    (and (instance? RubyRange other)
         (= start (:start other))
         (= end (:end other))
         (= inclusive? (:inclusive? other))))
  (ruby-compare [{:keys [start end inclusive?]} other]
    (when (instance? RubyRange other)
      (let [start-cmp (compare start (:start other))]
        (if (= start-cmp 0)
          (let [end-cmp (compare end (:end other))]
            (if (= end-cmp 0)
              (compare inclusive? (:inclusive? other))
              end-cmp))
          start-cmp)))))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn ruby-range?
  "Check if a value is a Ruby range."
  [value]
  (instance? RubyRange value))

(defn char-range?
  "Check if a range is a character range (string endpoints)."
  [range]
  (and (ruby-range? range)
       (string? (:start range))
       (string? (:end range))
       (= 1 (count (:start range)))
       (= 1 (count (:end range)))))

(defn char-to-int
  "Convert a single character string to its ASCII value."
  [char-str]
  (int (first char-str)))

(defn int-to-char
  "Convert an ASCII value to a single character string."
  [ascii-val]
  (str (char ascii-val)))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-range-methods!
  "Register all Range methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Range" :to_s #(to-s %))
  (register-method "Range" :inspect #(inspect %))
  (register-method "Range" :== #(ruby-eq %1 %2))
  (register-method "Range" :!= #(not (ruby-eq %1 %2)))
  (register-method "Range" :equal? #(identical? %1 %2))
  (register-method "Range" :object_id #(System/identityHashCode %))
  (register-method "Range" :class #(ruby-class %))
  (register-method "Range" :nil? #(false)) ; Ranges are never nil
  (register-method "Range" :respond_to? #(respond-to? %1 %2))
  (register-method "Range" :methods #(class-methods (ruby-class %)))
  (register-method "Range" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Range" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Range" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Range-specific methods
  (register-method "Range" :size
    (fn [{:keys [start end inclusive?] :as ruby-range}]
      (if (char-range? ruby-range)
        ;; Character range size
        (let [start-ascii (char-to-int start)
              end-ascii (char-to-int end)
              limit (if inclusive? (inc end-ascii) end-ascii)]
          (max 0 (- limit start-ascii)))
        ;; Numeric range size
        (let [limit (if inclusive? (inc end) end)]
          (max 0 (- limit start)))))))

  (register-method "Range" :count
    (fn [{:keys [start end inclusive?] :as ruby-range}]
      (if (char-range? ruby-range)
        ;; Character range count
        (let [start-ascii (char-to-int start)
              end-ascii (char-to-int end)
              limit (if inclusive? (inc end-ascii) end-ascii)]
          (max 0 (- limit start-ascii)))
        ;; Numeric range count
        (let [limit (if inclusive? (inc end) end)]
          (max 0 (- limit start))))))

  (register-method "Range" :include?
    (fn [{:keys [start end inclusive?] :as ruby-range} value]
      (if (char-range? ruby-range)
        ;; Character range comparison
        (if (and (string? value) (= 1 (count value)))
          (let [start-ascii (char-to-int start)
                end-ascii (char-to-int end)
                value-ascii (char-to-int value)]
            (if inclusive?
              (and (>= value-ascii start-ascii) (<= value-ascii end-ascii))
              (and (>= value-ascii start-ascii) (< value-ascii end-ascii))))
          false)
        ;; Numeric range comparison
        (if inclusive?
          (and (>= value start) (<= value end))
          (and (>= value start) (< value end))))))

  (register-method "Range" :member?
    (fn [ruby-range value]
      ;; Alias for include?
      (let [include-fn (method-lookup ruby-range :include?)]
        (include-fn ruby-range value))))

  (register-method "Range" :to_a
    (fn [{:keys [start end inclusive?] :as ruby-range}]
      (if (char-range? ruby-range)
        ;; Character range
        (let [start-ascii (char-to-int start)
              end-ascii (char-to-int end)
              limit (if inclusive? (inc end-ascii) end-ascii)]
          (vec (map int-to-char (clojure.core/range start-ascii limit))))
        ;; Numeric range
        (let [limit (if inclusive? (inc end) end)]
          (vec (clojure.core/range start limit))))))

  (register-method "Range" :first
    (fn [ruby-range & args]
      (if (empty? args)
        ;; Return the first element
        (:start ruby-range)
        ;; Return first n elements as array
        (let [n (first args)
              to-a-fn (method-lookup ruby-range :to_a)
              array (to-a-fn ruby-range)]
          (vec (take n array))))))

  (register-method "Range" :last
    (fn [{:keys [start end inclusive?] :as ruby-range} & args]
      (if (empty? args)
        ;; Return the last element
        (if inclusive?
          end
          (if (char-range? ruby-range)
            (int-to-char (dec (char-to-int end)))
            (dec end)))
        ;; Return last n elements as array
        (let [n (first args)
              to-a-fn (method-lookup ruby-range :to_a)
              array (to-a-fn ruby-range)]
          (vec (take-last n array))))))

  (register-method "Range" :min
    (fn [ruby-range]
      (:start ruby-range)))

  (register-method "Range" :max
    (fn [{:keys [end inclusive?] :as ruby-range}]
      (if inclusive?
        end
        (if (char-range? ruby-range)
          (int-to-char (dec (char-to-int end)))
          (dec end)))))

  ;; Comparison operators
  (register-method "Range" :<
    (fn [ruby-range1 ruby-range2]
      (when (instance? RubyRange ruby-range2)
        (< (ruby-compare ruby-range1 ruby-range2) 0))))

  (register-method "Range" :>
    (fn [ruby-range1 ruby-range2]
      (when (instance? RubyRange ruby-range2)
        (> (ruby-compare ruby-range1 ruby-range2) 0))))

  (register-method "Range" :<=
    (fn [ruby-range1 ruby-range2]
      (when (instance? RubyRange ruby-range2)
        (<= (ruby-compare ruby-range1 ruby-range2) 0))))

  (register-method "Range" :>=
    (fn [ruby-range1 ruby-range2]
      (when (instance? RubyRange ruby-range2)
        (>= (ruby-compare ruby-range1 ruby-range2) 0))))

  ;; Inherit Kernel methods
  (register-method "Range" :puts
    (fn [this & args]
      (if (empty? args)
        (println)
        (doseq [arg args]
          (println (if (satisfies? RubyInspectable arg) (to-s arg) (str arg)))))
      nil))

  (register-method "Range" :p
    (fn [this & args]
      (if (empty? args)
        nil
        (let [results (mapv #(if (satisfies? RubyInspectable %) (inspect %) (pr-str %)) args)]
          (println (str/join " " results))
          (if (= 1 (count results)) (first args) (vec args))))))

  (register-method "Range" :print
    (fn [this & args]
      (doseq [arg args]
        (print (if (satisfies? RubyInspectable arg) (to-s arg) (str arg))))
      nil))

;; Register methods on namespace load
(register-range-methods!)

;; =============================================================================
;; Constructor Function
;; =============================================================================

(defn create-range
  "Create a new Ruby range."
  ([start end]
   (->RubyRange start end false))
  ([start end inclusive?]
   (->RubyRange start end inclusive?)))
