(ns sri.spec-runner
  "Basic Ruby spec runner for testing SRI against ruby/spec test suite"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [sri.core :as sri]))

(defn parse-it-block
  "Parse a single it block and extract its test code"
  [lines start-idx]
  (let [start-line (nth lines start-idx)
        description-match (re-find #"^\s*it\s+['\"]([^'\"]+)['\"].*do\s*$" start-line)]
    (when description-match
      (let [description (second description-match)
            end-idx (loop [idx (inc start-idx)
                          depth 1]
                     (if (>= idx (count lines))
                       idx
                       (let [line (nth lines idx)]
                         (cond
                           ;; Count constructs that start blocks: do, if, for, while, case, class, def, begin
                           (re-find #"\b(do|if|for|while|case|class|def|begin)\b" line) (recur (inc idx) (inc depth))
                           ;; Count end keywords that close blocks
                           (re-find #"^\s*end\s*$" line) (if (= depth 1)
                                                           idx
                                                           (recur (inc idx) (dec depth)))
                           :else (recur (inc idx) depth)))))
            test-lines (subvec (vec lines) (inc start-idx) end-idx)
            ;; Remove leading and trailing whitespace from each line to normalize indentation
            normalized-lines (map str/trim test-lines)
            test-code (str/join "\n" normalized-lines)]
        {:description description
         :code test-code
         :type :it-block
         :start-line start-idx
         :end-line end-idx}))))

(defn parse-simple-spec
  "Parse a very basic Ruby spec file and extract executable test cases.
   
   This parser handles:
   - Multi-line it blocks: it 'description' do ... end
   - Various should assertions within blocks
   
   Returns a map with :description and :code for each test."
  [spec-content]
  (let [lines (str/split-lines spec-content)
        spec-blocks (atom [])
        processed-lines (atom #{})]
    
    ;; Parse it blocks
    (doseq [i (range (count lines))]
      (when (and (not (contains? @processed-lines i))
                 (re-find #"^\s*it\s+['\"]([^'\"]+)['\"].*do\s*$" (nth lines i)))
        (when-let [it-block (parse-it-block lines i)]
          (swap! spec-blocks conj it-block)
          ;; Mark all lines in this block as processed
          (doseq [line-idx (range (:start-line it-block) (inc (:end-line it-block)))]
            (swap! processed-lines conj line-idx)))))
    
    @spec-blocks))

(defn parse-test-assertions
  "Parse assertions from test code and return a list of {expression, expected} maps"
  [test-code]
  (let [lines (str/split-lines test-code)
        assertions (atom [])]
    (doseq [line lines]
      (let [trimmed-line (str/trim line)]
        (cond
          ;; Handle .should == value
          (re-find #"\.should\s*==" trimmed-line)
          (when-let [match (re-find #"^(.+)\.should\s*==\s*(.+)$" trimmed-line)]
            (swap! assertions conj {:expression (str/trim (second match))
                                    :expected (str/trim (nth match 2))
                                    :assertion-type :equals}))
          
          ;; Handle .should be_nil
          (re-find #"\.should\s+be_nil" trimmed-line)
          (when-let [match (re-find #"^(.+)\.should\s+be_nil" trimmed-line)]
            (swap! assertions conj {:expression (str/trim (second match))
                                    :expected "nil"
                                    :assertion-type :be_nil}))
          
          ;; Handle .should be_true / be_false  
          (re-find #"\.should\s+be_(true|false)" trimmed-line)
          (when-let [match (re-find #"^(.+)\.should\s+be_(true|false)" trimmed-line)]
            (swap! assertions conj {:expression (str/trim (second match))
                                    :expected (nth match 2)
                                    :assertion-type :be_boolean}))
          
          ;; Handle .should be_kind_of(Class)
          (re-find #"\.should\s+be_kind_of\(" trimmed-line)
          (when-let [match (re-find #"^(.+)\.should\s+be_kind_of\((.+)\)" trimmed-line)]
            (swap! assertions conj {:expression (str/trim (second match))
                                    :expected (str/trim (nth match 2))
                                    :assertion-type :be_kind_of})))))
    @assertions))

(defn evaluate-assertion
  "Evaluate a Ruby expression and compare with expected result"
  [expression expected-str]
  (try
    (let [result (sri/eval-string expression)
          ;; Convert expected string to appropriate type
          expected (cond
                     (= expected-str "true") true
                     (= expected-str "false") false
                     (= expected-str "nil") nil
                     (re-matches #"^\d+$" expected-str) (try 
                                                          (Integer/parseInt expected-str)
                                                          (catch NumberFormatException _
                                                            (BigInteger. expected-str 10)))
                     (re-matches #"^\d+\.\d+$" expected-str) (Double/parseDouble expected-str)
                     (re-matches #"^\d+\.?\d*[eE][+-]?\d+$" expected-str) (Double/parseDouble expected-str)
                     (re-matches #"^['\"].*['\"]$" expected-str) (subs expected-str 1 (dec (count expected-str)))
                     (re-matches #"^Rational\((-?\d+),\s*(-?\d+)\)$" expected-str)
                     (let [match (re-find #"^Rational\((-?\d+),\s*(-?\d+)\)$" expected-str)]
                       (sri/eval-string (str "Rational(" (nth match 1) ", " (nth match 2) ")")))
                     (re-matches #"^Complex\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)$" expected-str)
                     (let [match (re-find #"^Complex\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)$" expected-str)]
                       (sri/eval-string (str "Complex(" (nth match 1) ", " (nth match 2) ")")))
                     :else expected-str)]
      
      {:passed? (= result expected)
       :result result
       :expected expected
       :expression expression})
    
    (catch Exception e
      {:passed? false
       :error (.getMessage e)
       :expression expression
       :expected expected-str})))

(defn execute-line-by-line
  "Execute test code line by line, maintaining state and checking assertions"
  [test-code description]
  (let [lines (str/split-lines test-code)
        results (atom [])
        accumulated-code (atom [])]
    
    (doseq [line lines]
      (let [trimmed-line (str/trim line)]
        (if (and (not (str/blank? trimmed-line))
                 (not (str/starts-with? trimmed-line "#")))
          (if (re-find #"\.should\s+" trimmed-line)
            ;; This is an assertion line - parse and execute it
            (let [assertion-patterns [
                   ;; Pattern: expression.should == expected
                   #"^(.+?)\.should\s*==\s*(.+)$"
                   ;; Pattern: expression.should be_nil  
                   #"^(.+?)\.should\s+be_nil\s*$"
                   ;; Pattern: expression.should be_true/be_false
                   #"^(.+?)\.should\s+be_(true|false)\s*$"
                   ;; Pattern: expression.should be_kind_of(Class)
                   #"^(.+?)\.should\s+be_kind_of\((.+?)\)\s*$"]]
              
              (loop [patterns assertion-patterns
                     matched? false]
                (if (or matched? (empty? patterns))
                  (when-not matched?
                    (swap! results conj {:passed? false
                                       :error (str "Unknown assertion pattern: " trimmed-line)
                                       :expression trimmed-line
                                       :description description}))
                  (let [pattern (first patterns)
                        match (re-find pattern trimmed-line)]
                    (if match
                      (try
                        (let [expr (str/trim (second match))
                              expected-str (cond
                                           (re-find #"be_nil" trimmed-line) "nil"
                                           (re-find #"be_true" trimmed-line) "true" 
                                           (re-find #"be_false" trimmed-line) "false"
                                           :else (str/trim (nth match 2)))
                              ;; Execute accumulated code + expression
                              full-code (str (str/join "\n" @accumulated-code) "\n" expr)
                              result (sri/eval-string full-code)
                              passed? (cond
                                      (re-find #"be_kind_of" trimmed-line)
                                      ;; For be_kind_of, check if result is instance of the class
                                      (let [class-name expected-str]
                                        (cond
                                          (= class-name "Array") (or (vector? result) 
                                                                    (and result 
                                                                         (.contains (str (type result)) "RubyArray")))
                                          (= class-name "String") (or (string? result)
                                                                     (and result
                                                                          (.contains (str (type result)) "RubyString")))
                                          (= class-name "Integer") (integer? result)
                                          (= class-name "Float") (float? result)
                                          (= class-name "Hash") (or (map? result)
                                                                   (and result
                                                                        (.contains (str (type result)) "RubyHash")))
                                          :else false))
                                      :else
                                      ;; Regular equality check - use Ruby equality
                                      (let [expected-val (sri/eval-string expected-str)]
                                        (cond
                                          ;; If both are RubyObjects, use ruby-eq from result
                                          (and (satisfies? sri.ruby-protocols/RubyObject result)
                                               (satisfies? sri.ruby-protocols/RubyObject expected-val))
                                          (sri.ruby-protocols/ruby-eq result expected-val)
                                          
                                          ;; If only result is RubyObject, use ruby-eq from result
                                          (satisfies? sri.ruby-protocols/RubyObject result)
                                          (sri.ruby-protocols/ruby-eq result expected-val)
                                          
                                          ;; If only expected is RubyObject, use ruby-eq from expected
                                          (satisfies? sri.ruby-protocols/RubyObject expected-val)
                                          (sri.ruby-protocols/ruby-eq expected-val result)
                                          
                                          ;; Neither is RubyObject, use regular equality
                                          :else
                                          (= result expected-val))))]
                          
                          (swap! results conj {:passed? passed?
                                             :result result
                                             :expected (if (re-find #"be_kind_of" trimmed-line)
                                                        expected-str  ; For be_kind_of, show class name
                                                        (let [expected-val (sri/eval-string expected-str)] expected-val))
                                             :expression expr
                                             :description description
                                             :assertion-type (cond
                                                            (re-find #"be_nil" trimmed-line) :be_nil
                                                            (re-find #"be_true" trimmed-line) :be_true
                                                            (re-find #"be_false" trimmed-line) :be_false
                                                            (re-find #"be_kind_of" trimmed-line) :be_kind_of
                                                            :else :equals)}))
                        (catch Exception e
                          (swap! results conj {:passed? false
                                             :error (.getMessage e)
                                             :expression (if (>= (count match) 2) (second match) trimmed-line)
                                             :description description})))
                      (recur (rest patterns) false))))))
            ;; This is a regular code line - add to accumulated code
            (swap! accumulated-code conj line)))))
    
    @results))

(defn evaluate-test-block
  "Evaluate a complete test block and check all its assertions"
  [test-code description]
  (try
    ;; First try to execute the whole block - this works for simple cases and control structures
    (sri/eval-string test-code)
    ;; If no exception was thrown, the test passed
    [{:passed? true
      :result "Test completed successfully"
      :description description}]
    
    (catch Exception e
      ;; Check if this is an assertion failure from .should method
      (if-let [ex-data (ex-data e)]
        (if (= (:type ex-data) :assertion-failure)
          ;; This is a failed assertion
          [{:passed? false
            :result (:actual ex-data)
            :expected (:expected ex-data)
            :expression (str (:actual ex-data) ".should == " (:expected ex-data))
            :description description}]
          ;; This is some other kind of error
          [{:passed? false
            :error (.getMessage e)
            :description description
            :test-code test-code}])
        ;; No ex-data, so this is a different kind of error
        [{:passed? false
          :error (.getMessage e)
          :description description
          :test-code test-code}]))))

(defn run-spec-file
  "Run a single Ruby spec file against SRI"
  [spec-file-path]
  (try
    (let [spec-content (slurp spec-file-path)
          parsed-specs (parse-simple-spec spec-content)
          results (atom [])
          passed (atom 0)
          failed (atom 0)]
      
      (println (str "\n=== Running " spec-file-path " ==="))
      
      (doseq [spec parsed-specs]
        (cond
          ;; Handle it blocks  
          (= (:type spec) :it-block)
          (let [test-results (evaluate-test-block (:code spec) (:description spec))]
            (doseq [result test-results]
              (swap! results conj (assoc result :spec spec))
              
              (if (:passed? result)
                (do
                  (println (str "✓ " (:description spec) " - " (:expression result)))
                  (swap! passed inc))
                (do
                  (if (:error result)
                    (println (str "✗ " (:description spec) " - ERROR: " (:error result)))
                    (println (str "✗ " (:description spec) " - " (:expression result) " - Expected: " (:expected result) ", Got: " (:result result))))
                  (swap! failed inc)))))
          
          ;; Handle old-style single assertions
          (:expression spec)
          (let [result (evaluate-assertion (:expression spec) (:expected spec))]
            (swap! results conj (assoc result :spec spec))
            
            (if (:passed? result)
              (do
                (println (str "✓ " (:expression spec)))
                (swap! passed inc))
              (do
                (if (:error result)
                  (println (str "✗ " (:expression spec) " - ERROR: " (:error result)))
                  (println (str "✗ " (:expression spec) " - Expected: " (:expected result) ", Got: " (:result result))))
                (swap! failed inc))))))
      
      (when (> (+ @passed @failed) 0)
        (println (str "\nResults: " @passed " passed, " @failed " failed")))
      
      @results)
    
    (catch Exception e
      (println (str "Error running spec file: " (.getMessage e)))
      [])))

(defn run-basic-specs
  "Run a set of basic specs that should work with current SRI implementation"
  []
  (let [basic-specs [
        ;; String specs
        "/Users/edipo/spec/core/string/to_s_spec.rb"
        "/Users/edipo/spec/core/string/length_spec.rb"
        
        ;; Array specs  
        "/Users/edipo/spec/core/array/array_spec.rb"
        "/Users/edipo/spec/language/array_spec.rb"
        
        ;; Basic object specs
        "/Users/edipo/spec/core/basicobject/basicobject_spec.rb"]]
    
    (println "Running basic Ruby specs against SRI...")
    (println "This tests SRI's compatibility with ruby/spec test suite")
    
    (doseq [spec-file basic-specs]
      (when (.exists (io/file spec-file))
        (run-spec-file spec-file)))
    
    (println "\nNote: This is a basic spec runner. Many specs require:")
    (println "- Exception handling (begin/rescue)")  
    (println "- Constants and modules")
    (println "- File I/O and require statements")
    (println "- Advanced Ruby features not yet in SRI")))

(defn create-simple-test-spec
  "Create a simple test spec file to verify the runner works"
  []
  (let [test-spec-content "# Simple test spec for SRI
describe \"Basic SRI functionality\" do
  it \"should handle basic arithmetic\" do
    (1 + 2).should == 3
    (5 * 2).should == 10
  end
  
  it \"should handle string operations\" do
    \"hello\".length.should == 5
    \"world\".upcase.should == \"WORLD\"
  end
  
  it \"should handle array operations\" do
    [1, 2, 3].length.should == 3
    [1, 2, 3][0].should == 1
  end
end"]
    
    (spit "/Users/edipo/sri/test/simple_sri_spec.rb" test-spec-content)
    (println "Created simple test spec at: /Users/edipo/sri/test/simple_sri_spec.rb")
    test-spec-content))

(defn -main 
  "Main entry point for spec runner"
  [& args]
  (cond 
    (= (first args) "create-test") (create-simple-test-spec)
    (= (first args) "run-basic") (run-basic-specs)
    (and (first args) (.exists (io/file (first args)))) (run-spec-file (first args))
    :else (do 
            (println "SRI Ruby Spec Runner")
            (println "Usage:")
            (println "  lein run -m sri.spec-runner create-test   # Create simple test spec")
            (println "  lein run -m sri.spec-runner run-basic     # Run basic ruby/spec tests")
            (println "  lein run -m sri.spec-runner <spec-file>   # Run specific spec file"))))