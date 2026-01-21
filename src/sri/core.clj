(ns sri.core
  (:require [sri.tokenizer :as t]
            [sri.parser :as p]
            [sri.interpreter :as i])
  (:gen-class))

(defn evaluate
  "Internal evaluation function for Ruby source code.
   argv is a vector of command-line arguments passed to the Ruby program."
  ([source]
   (evaluate source []))
  ([source argv]
   (try
     (let [ast (p/parse (t/tokenize source))
           root-entity (p/find-root-entity ast)
           ;; Create ARGV as a Ruby array
           argv-array (apply sri.ruby-classes-new/create-array argv)]
       (i/evaluate-directly ast root-entity {:namespaces {"ARGV" argv-array}})
       0)
     (catch clojure.lang.ExceptionInfo e
       (println "Error:" (.getMessage e))
       (.printStackTrace e)
       1))))

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
           root-entity (p/find-root-entity ast)
           result (i/evaluate-directly ast root-entity opts)]
       ;; Convert RubyString objects to Java strings for user-friendly results
       (cond
         (satisfies? sri.ruby-protocols/RubyInspectable result)
         (sri.ruby-protocols/to-s result)
         :else result))
     (catch clojure.lang.ExceptionInfo e
       (throw (ex-info (str "Ruby evaluation error: " (.getMessage e))
                       {:source source
                        :error (.getMessage e)}
                       e))))))

(defn -main
  "Main entry point for command-line file execution.
   For library usage, use eval-string instead.
   Arguments after the filename are passed to the Ruby program as ARGV."
  [& args]
  (if (seq args)
    (let [filename (first args)
          ruby-argv (rest args)]  ; Remaining args become ARGV
      (if (= "-e" (first args))
        (eval-string (last args))
        (try
          (let [source (slurp filename)
                exit-code (evaluate source ruby-argv)]
            (System/exit exit-code))
          (catch java.io.FileNotFoundException _
            (println "Error: File not found:" filename)
            (System/exit 1))
          (catch Exception e
            (println "Error reading file:" (.getMessage e))
            (.printStackTrace e)
            (System/exit 1)))))
    (do
      (println "Sri - Ruby Interpreter")
      (println "=======================")
      (println "\n")
      (println "Usage: sri <filename>")
      (println "Usage: sri -e <code>")
      (println "\n")
      (println "For library usage: (require 'sri.core) (sri.core/eval-string \"ruby code\")")
      (System/exit 1))))
