(ns sri.final-spec-runner
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [sri.core :as sri]))

(defn extract-it-block-content
  [lines start-idx]
  (let [start-line (nth lines start-idx)
        description-match (re-find #"^\s*it\s+['\"]([^'\"]+)['\"].*do\s*$" start-line)]
    (when description-match
      (let [description (second description-match)]
        (loop [i (inc start-idx)
               content-lines []
               depth 1]
          (if (< i (count lines))
            (let [line (nth lines i)
                  trimmed (str/trim line)]
              (cond
                ;; Found an end
                (re-find #"^\s*end\s*$" trimmed)
                (let [new-depth (dec depth)]
                  (if (= new-depth 0)
                    ;; This is our closing end
                    {:description description
                     :content content-lines}
                    (recur (inc i) (conj content-lines line) new-depth)))

                ;; Found a block starter
                (re-find #"\b(do|for|while|case|class|def|begin|if)\b" trimmed)
                (recur (inc i) (conj content-lines line) (inc depth))

                :else
                (recur (inc i) (conj content-lines line) depth)))
            ;; Reached end
            {:description description
             :content content-lines}))))))

(defn extract-all-it-blocks
  [spec-content]
  (let [lines (str/split-lines spec-content)
        blocks (atom [])]

    (doseq [i (range (count lines))]
      (when (re-find #"^\s*it\s+['\"].*['\"].*do\s*$" (nth lines i))
        (when-let [block (extract-it-block-content lines i)]
          (swap! blocks conj block))))

    @blocks))

(defn run-it-block
  [block]
  (let [description (:description block)
        content-lines (:content block)
        cleaned-lines (->> content-lines
                          (remove #(str/blank? (str/trim %)))
                          (remove #(str/starts-with? (str/trim %) "#")))
        code (str/join "\n" cleaned-lines)]

    (println (str "\n=== " description " ==="))

    (if (str/blank? code)
      (println "No executable code found")
      (try
        (sri/eval-string code)
        (println "✓ Test completed")
        (catch Exception e
          (println (str "✗ Test failed: " (.getMessage e))))))))

(defn run-spec-file
  [spec-file-path]
  (println (str "\n=== Running " spec-file-path " ==="))
  (try
    (let [spec-content (slurp spec-file-path)
          blocks (extract-all-it-blocks spec-content)]

      (println (str "Found " (count blocks) " test blocks"))

      (doseq [block blocks]
        (run-it-block block))

      {:status :success :file spec-file-path :blocks (count blocks)})

    (catch Exception e
      (println (str "ERROR reading spec file: " (.getMessage e)))
      {:status :error :file spec-file-path :error (.getMessage e)})))

(defn run-all-specs
  []
  (let [specs-file "/Users/edipo/sri/specs-to-run.txt"
        spec-files (->> (slurp specs-file)
                       str/split-lines
                       (map str/trim)
                       (remove str/blank?)
                       (distinct))
        existing-files (filter #(.exists (io/file %)) spec-files)
        missing-files (filter #(not (.exists (io/file %))) spec-files)]

    (when (seq missing-files)
      (println "Warning: Missing files:")
      (doseq [missing missing-files]
        (println (str "  " missing))))

    (println (str "Running " (count existing-files) " spec files from " specs-file "..."))

    (doseq [spec-file existing-files]
      (run-spec-file spec-file))

    (println "\n=== All specs completed ===")))

(defn -main
  [& args]
  (cond
    (= (first args) "all") (run-all-specs)
    (and (first args) (.exists (io/file (first args)))) (run-spec-file (first args))
    :else (do
            (println "SRI Final Spec Runner")
            (println "Usage:")
            (println "  lein run -m sri.final-spec-runner all              # Run all listed specs")
            (println "  lein run -m sri.final-spec-runner <spec-file>      # Run specific file"))))
