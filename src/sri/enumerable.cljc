(ns sri.enumerable
  "Enumerable module for Sri - provides iteration methods for collections"
  (:require [sri.parser :as parser]
            [sri.ruby-array :refer [ruby-array?]]))

;; Forward declaration for execute-block (defined in interpreter)
(declare execute-block)

;; Enumerable Protocol (similar to Ruby's Enumerable module)
(defn enumerable?
  "Check if an object supports enumerable operations."
  [obj]
  (or (vector? obj) 
      (ruby-array? obj) ; Ruby arrays
      (and (record? obj) 
           (contains? obj :start) ; duck typing for ranges
           (contains? obj :end)
           (contains? obj :inclusive?))))

(defn ruby-range?
  "Check if a value is a Ruby range (duck typing approach)."
  [value]
  (and (record? value) 
       (contains? value :start) 
       (contains? value :end)
       (contains? value :inclusive?)))

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

(defn each-impl
  "Core iteration implementation for enumerable objects.
   Takes an object and a function, calls the function for each element."
  [obj element-fn]
  (cond
    (vector? obj)
    (doseq [item obj]
      (element-fn item))
    
    (ruby-array? obj) ; Ruby arrays
    (doseq [item @(:data obj)]
      (element-fn item))
    
    (ruby-range? obj)
    (let [{:keys [start end inclusive?]} obj]
      (if (char-range? obj)
        ;; Character range iteration
        (let [start-ascii (char-to-int start)
              end-ascii (char-to-int end)
              limit (if inclusive? (inc end-ascii) end-ascii)]
          (doseq [ascii-val (range start-ascii limit)]
            (element-fn (int-to-char ascii-val))))
        ;; Numeric range iteration  
        (let [limit (if inclusive? (inc end) end)]
          (doseq [item (range start limit)]
            (element-fn item)))))
    
    :else
    (throw (ex-info "Object is not enumerable" {:object obj}))))

;; Enumerable Methods (built on each-impl)
(defn enumerable-each
  "Enumerable each implementation - executes block for each element."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (do
        (each-impl receiver 
                   (fn [item] 
                     (execute-block-fn ast block-id variables [item])))
        receiver) ; Return the original receiver like Ruby
      (throw (ex-info "each requires a block" {:receiver receiver})))))

(defn enumerable-map
  "Enumerable map implementation - transforms each element with block."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [results (atom [])]
        (each-impl receiver 
                   (fn [item]
                     (let [result (execute-block-fn ast block-id variables [item])]
                       (swap! results conj result))))
        @results)
      (throw (ex-info "map requires a block" {:receiver receiver})))))

(defn enumerable-select
  "Enumerable select/filter implementation - keeps elements where block returns truthy."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [results (atom [])]
        (each-impl receiver 
                   (fn [item]
                     (let [result (execute-block-fn ast block-id variables [item])]
                       (when result ; truthy in Ruby
                         (swap! results conj item)))))
        @results)
      (throw (ex-info "select requires a block" {:receiver receiver})))))

(defn enumerable-reject
  "Enumerable reject implementation - keeps elements where block returns falsy."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [results (atom [])]
        (each-impl receiver 
                   (fn [item]
                     (let [result (execute-block-fn ast block-id variables [item])]
                       (when-not result ; falsy in Ruby
                         (swap! results conj item)))))
        @results)
      (throw (ex-info "reject requires a block" {:receiver receiver})))))

(defn enumerable-find
  "Enumerable find/detect implementation - returns first element where block returns truthy."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [found (atom nil)
            found? (atom false)]
        (try
          (each-impl receiver 
                     (fn [item]
                       (let [result (execute-block-fn ast block-id variables [item])]
                         (when result
                           (reset! found item)
                           (reset! found? true)
                           (throw (ex-info "found" {:type :found})))))) ; Use exception for early exit
          (catch clojure.lang.ExceptionInfo e
            (when (not= (:type (ex-data e)) :found)
              (throw e))))
        (if @found? @found nil))
      (throw (ex-info "find requires a block" {:receiver receiver})))))

(defn enumerable-any?
  "Enumerable any? implementation - returns true if any element makes block return truthy."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [found? (atom false)]
        (try
          (each-impl receiver 
                     (fn [item]
                       (let [result (execute-block-fn ast block-id variables [item])]
                         (when result
                           (reset! found? true)
                           (throw (ex-info "found" {:type :found})))))) ; Use exception for early exit
          (catch clojure.lang.ExceptionInfo e
            (when (not= (:type (ex-data e)) :found)
              (throw e))))
        @found?)
      (throw (ex-info "any? requires a block" {:receiver receiver})))))

(defn enumerable-all?
  "Enumerable all? implementation - returns true if all elements make block return truthy."
  [receiver ast entity-id variables execute-block-fn]
  (let [block-id (parser/get-component ast entity-id :block)]
    (if block-id
      (let [all-true? (atom true)]
        (try
          (each-impl receiver 
                     (fn [item]
                       (let [result (execute-block-fn ast block-id variables [item])]
                         (when-not result
                           (reset! all-true? false)
                           (throw (ex-info "found-false" {:type :found-false})))))) ; Use exception for early exit
          (catch clojure.lang.ExceptionInfo e
            (when (not= (:type (ex-data e)) :found-false)
              (throw e))))
        @all-true?)
      (throw (ex-info "all? requires a block" {:receiver receiver})))))