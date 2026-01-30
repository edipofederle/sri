(ns sri.ruby-kernel
  "Implementation of Ruby's Kernel module - methods available to all objects."
  (:require [clojure.string :as str]
            [sri.ruby-method-registry :refer [register-method]]
            [sri.ruby-protocols :refer [RubyInspectable to-s]])
  (:import [java.io File]))

;; =============================================================================
;; Kernel Module Implementation
;; =============================================================================

(defn puts-impl
  "Implementation of puts method from Kernel module."
  [_ & args]
  (if (empty? args)
    (println)
    (doseq [arg args]
      (cond
        (and (record? arg)
             (contains? arg :data)
             (instance? clojure.lang.Atom (:data arg))
             (vector? @(:data arg)))
        ;; Print ruby array elements on separate lines
        (doseq [item @(:data arg)]
          (cond
            (nil? item) (println)
            (keyword? item) (println (name item)) ; Print symbols without colon prefix
            (satisfies? RubyInspectable item) (println (to-s item))
            ;; Class objects (maps with :name and :builtin) should show just the name
            (and (map? item) (:name item) (:builtin item)) (println (:name item))
            :else (println (str item))))

        (keyword? arg) (println (name arg)) ; Print symbols without colon prefix (Ruby behavior)
        (nil? arg) (println "nil")
        (satisfies? RubyInspectable arg) (println (to-s arg))
        ;; Class objects (maps with :name and :builtin) should show just the name
        (and (map? arg) (:name arg) (:builtin arg)) (println (:name arg))
        :else (println (str arg)))))
  nil)

(defn p-impl
  "Implementation of p method from Kernel module."
  [_ & args]
  (if (empty? args)
    (println)
    (let [output-parts (map (fn [arg]
                              (cond
                                (nil? arg) "nil"
                                (string? arg) (str "\"" arg "\"")
                                (and (map? arg) (contains? arg :value)) (str "\"" (:value arg) "\"") ; RubyString
                                (satisfies? RubyInspectable arg) (to-s arg)
                                ;; Class objects (maps with :name and :builtin) should show just the name
                                (and (map? arg) (:name arg) (:builtin arg)) (:name arg)
                                :else (str arg))) args)]
      (println (str/join " " output-parts))))
  (if (= (count args) 1)
    (first args)  ; Return the original argument
    args))

(defn print-impl
  "Implementation of print method from Kernel module."
  [_ & args]
  (doseq [arg args]
    (cond
      (keyword? arg) (print (name arg)) ; Print symbols without colon prefix (Ruby behavior)
      (nil? arg) (print "")
      (satisfies? RubyInspectable arg) (print (to-s arg))
      ;; Class objects (maps with :name and :builtin) should show just the name
      (and (map? arg) (:name arg) (:builtin arg)) (print (:name arg))
      :else (print (str arg))))
  (flush)  ; Flush output after printing
  (.flush System/out)  ; Additional Java-level flush
  nil)

;; =============================================================================
;; Kernel Module Registration
;; =============================================================================

(defn register-kernel-methods!
  "Register all Kernel module methods. Should be called during initialization."
  []
  ;; Register Kernel methods on the Kernel module itself
  (register-method "Kernel" :puts puts-impl)
  (register-method "Kernel" :p p-impl)
  (register-method "Kernel" :print print-impl)
  
  ;; Since Kernel is mixed into Object, these methods are available on all objects
  ;; We register them directly on Object for now (later we can implement proper module inclusion)
  (register-method "Object" :puts puts-impl)
  (register-method "Object" :p p-impl)
  (register-method "Object" :print print-impl))

(defn register-kernel-methods-for-class!
  "Register Kernel methods for a specific Ruby class."
  [class-name]
  (register-method class-name :puts puts-impl)
  (register-method class-name :p p-impl)
  (register-method class-name :print print-impl))

;; =============================================================================
;; Module System Support
;; =============================================================================

(def kernel-module
  "The Kernel module definition with its methods."
  {:name "Kernel"
   :methods {:puts puts-impl
             :p p-impl
             :print print-impl}})

(defn get-kernel-methods
  "Get all methods defined in the Kernel module."
  []
  (:methods kernel-module))

;; =============================================================================
;; require_relative Implementation
;; https://www.rubydoc.info/stdlib/core/Kernel:require_relative
;; =============================================================================

;; Track already required files to avoid double-loading
(defonce ^:private required-files (atom #{}))

;; Dynamic var for current file path (set by interpreter during file execution)
(def ^:dynamic *current-file* nil)

(defn resolve-relative-path
  "Resolve a relative path against the current file's directory.
   Adds .rb extension if not present."
  [relative-path current-file]
  (let [path-str (cond
                   (string? relative-path) relative-path
                   (satisfies? RubyInspectable relative-path) (to-s relative-path)
                   :else (str relative-path))
        ;; Add .rb extension if not present
        path-with-ext (if (str/ends-with? path-str ".rb")
                        path-str
                        (str path-str ".rb"))
        ;; Resolve relative to current file's directory
        base-dir (if current-file
                   (.getParent (File. ^String current-file))
                   (System/getProperty "user.dir"))]
    (.getCanonicalPath (File. ^String base-dir ^String path-with-ext))))

(defn already-required?
  "Check if a file has already been required."
  [absolute-path]
  (contains? @required-files absolute-path))

(defn mark-as-required!
  "Mark a file as required."
  [absolute-path]
  (swap! required-files conj absolute-path))

(defn unmark-as-required!
  "Remove a file from the required set (on error)."
  [absolute-path]
  (swap! required-files disj absolute-path))

(defn reset-required-files!
  "Reset the required files set (useful for testing)."
  []
  (reset! required-files #{}))