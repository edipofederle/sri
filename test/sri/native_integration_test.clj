(ns sri.native-integration-test
  "Integration tests that run Ruby programs from examples folder using the native binary."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def native-binary "./target/sri")


(defn parse-expected-result
  "Parse expected result from file comments starting with '# expected-output:'."
  [file-content]
  (->> (str/split-lines file-content)
       (filter #(str/starts-with? % "# expected-output:"))
       (map #(str/replace % #"^# expected-output:\s*" ""))
       (str/join "\n")
       (str/trim)))

(defn run-program
  "Run a Ruby program file using the native binary and return its output."
  [file-path]
  (let [binary-path (if (.exists (io/file "./target/sri"))
                      "./target/sri"
                      native-binary)
        result (sh binary-path file-path)]
    (if (zero? (:exit result))
      (str/trim (:out result))
      (throw (ex-info (str "Ruby program failed: " file-path " (using " binary-path ")")
                     {:exit-code (:exit result)
                      :stderr (:err result)
                      :stdout (:out result)
                      :binary binary-path})))))

(defn get-example-files
  "Get all .rb files from the examples directory that have expected results."
  []
  (let [examples-dir (io/file "examples")]
    (if (.exists examples-dir)
      (->> (.listFiles examples-dir)
           (filter #(and (.isFile %)
                        (str/ends-with? (.getName %) ".rb")))
           (filter (fn [file]
                    (let [content (slurp file)]
                      (str/includes? content "# expected-output:"))))
           (sort-by #(.getName %)))
      [])))

(deftest ^:integration test-native-example-programs
  (println "\n🧪 Running native integration tests...")

  (let [example-files (get-example-files)]
    (if (empty? example-files)
      (println "⚠️  No example files with expected-output comments found")
      (do
        (println (format "📁 Found %d test files with expected output" (count example-files)))
        (doseq [file example-files]
          (let [file-path (.getAbsolutePath file)
                file-name (.getName file)
                file-content (slurp file)
                expected-result (parse-expected-result file-content)]
            (when-not (str/blank? expected-result)
              (println (format "🧪 Testing: %s" file-name))
              (testing (str "Native Example: " file-name)
                (try
                  (let [actual-result (run-program file-path)]
                    (is (= expected-result actual-result)
                        (format "File: %s\nExpected: %s\nActual: %s"
                               file-name (pr-str expected-result) (pr-str actual-result))))
                  (catch Exception e
                    (println (format "❌ Error in %s: %s" file-name (.getMessage e)))
                    (when-let [ex-data (ex-data e)]
                      (when (:stderr ex-data)
                        (println (format "   stderr: %s" (:stderr ex-data)))))
                    (is false (format "Test %s threw exception: %s" file-name (.getMessage e)))))))))

        (println "✅ Native integration tests completed!")))))

;; Convenience function to run tests manually
(defn run-native-tests []
  (test-native-example-programs))
