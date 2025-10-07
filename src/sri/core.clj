(ns sri.core
  (:require [sri.tokenizer :as t]
            [sri.parser :as p]
            [sri.interpreter :as i])
  (:gen-class))

(defn evaluate
  "Internal evaluation function for Ruby source code."
  [source]
  (try
    (let [ast (p/parse (t/tokenize source))
          root-entity (p/find-root-entity ast)]
      (i/evaluate-directly ast root-entity)
      0)
    (catch clojure.lang.ExceptionInfo e
      1)))

(defn eval-string
  "Evaluate a Ruby string and return the result.
   
   Usage:
   (eval-string \"1 + 2\")           ; => 3
   (eval-string \"puts 'hello'\")    ; prints hello, returns nil
   (eval-string \"x = 42; x * 2\" opts)  ; => 84
   
   Options (future):
   - :namespaces - custom variables and methods
   - :allow-methods - allowed method names
   - :deny-methods - forbidden method names"
  ([source]
   (eval-string source {}))
  ([source opts]
   (try
     (let [ast (p/parse (t/tokenize source))
           root-entity (p/find-root-entity ast)]
       (i/evaluate-directly ast root-entity opts))
     (catch clojure.lang.ExceptionInfo e
       (throw (ex-info (str "Ruby evaluation error: " (.getMessage e))
                       {:source source
                        :error (.getMessage e)}
                       e))))))

(defn -main
  "Main entry point for command-line file execution.
   For library usage, use eval-string instead."
  [& args]
  (if (seq args)
    (let [filename (first args)]
      (try
        (let [source (slurp filename)
              exit-code (evaluate source)]
          (System/exit exit-code))
        (catch java.io.FileNotFoundException e
          (println "Error: File not found:" filename)
          (System/exit 1))
        (catch Exception e
          (println "Error reading file:" (.getMessage e))
          (System/exit 1))))
    (do
      (println "Sri - Ruby Interpreter")
      (println "Usage: sri <filename>")
      (println "For library usage: (require 'sri.core) (sri.core/eval-string \"ruby code\")")
      (System/exit 1))))
