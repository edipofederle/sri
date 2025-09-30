(ns sri.core
  (:require [sri.tokenizer :as t]
            [sri.parser :as p]
            [sri.interpreter :as i])
  (:gen-class))

(defn evaluate
  [source]
  (try
    (let [ast (p/parse (t/tokenize source))
          root-entity (p/find-root-entity ast)]
      (i/evaluate-directly ast root-entity)
      0)
    (catch clojure.lang.ExceptionInfo e
      1)))

(defn -main
  "Main entry point for the application."
  [& args]
  (if (seq args)
    (let [filename (first args)]
      (try
        (let [source (slurp filename)]
          (evaluate source))
        (catch java.io.FileNotFoundException e
          (println "Error: File not found:" filename)
          1)
        (catch Exception e
          (println "Error reading file:" (.getMessage e))
          1)))
    (println "Usage: sri <filename>"))
  (System/exit 0))
