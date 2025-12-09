(ns sri.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [sri.core :as sri]))

(deftest test-basic-arithmetic
  (testing "Basic arithmetic expressions"
    (is (= 3 (sri/eval-string "1 + 2")))
    (is (= 10 (sri/eval-string "5 * 2")))
    (is (= 2 (sri/eval-string "10 / 5")))
    (is (= 3 (sri/eval-string "8 - 5")))))

(deftest test-string-operations
  (testing "String operations and interpolation"
    (is (= "Hello World" (sri/eval-string "'Hello ' + 'World'")))
    (is (= "Hello Ruby" (sri/eval-string "\"Hello #{'Ruby'}\"")))
    (is (= 11 (sri/eval-string "'Hello World'.length")))))

(deftest test-variable-assignment
  (testing "Variable assignment and usage"
    (is (= 42 (sri/eval-string "x = 42; x")))
    (is (= 84 (sri/eval-string "x = 42; x * 2")))
    (is (= "Alice30" (sri/eval-string "name = 'Alice'; age = 30; name + age.to_s")))))

(deftest test-method-definitions
  (testing "Method definitions and calls"
    (is (= "Hello Bob" (sri/eval-string "def greet(name); 'Hello ' + name; end; greet('Bob')")))
    (is (= 25 (sri/eval-string "def square(x); x * x; end; square(5)")))
    (is (= 6 (sri/eval-string "def add(a, b); a + b; end; add(2, 4)")))))

(deftest test-array-operations
  (testing "Array creation and methods"
    (is (= 5 (sri/eval-string "[1, 2, 3, 4, 5].length")))
    (is (= 1 (sri/eval-string "[1, 2, 3].first")))
    (is (= 3 (sri/eval-string "[1, 2, 3].last")))
    (is (= 2 (sri/eval-string "[1, 2, 3][1]")))))

(deftest test-puts-output
  (testing "puts returns nil but produces output"
    (is (= nil (sri/eval-string "puts('Hello World!')")))))

(deftest test-boolean-operations
  (testing "Boolean values and operations"
    (is (= true (sri/eval-string "true")))
    (is (= false (sri/eval-string "false")))
    (is (= true (sri/eval-string "5 > 3")))
    (is (= false (sri/eval-string "2 > 5")))))

(deftest test-conditionals
  (testing "If-else conditionals in methods"
    (is (= "positive" (sri/eval-string "def test_cond(n); if n > 0; 'positive'; else 'negative'; end; end; test_cond(5)")))
    (is (= "negative" (sri/eval-string "def test_cond(n); if n > 0; 'positive'; else 'negative'; end; end; test_cond(-1)")))))

(deftest test-loops
  (testing "Loop constructs"
    (is (= 15 (sri/eval-string "sum = 0; for i in [1, 2, 3, 4, 5]; sum = sum + i; end; sum")))
    (is (= 5 (sri/eval-string "i = 0; until i == 5; i = i + 1; end; i")))))

(deftest test-error-handling
  (testing "Error handling for invalid Ruby code"
    (is (thrown? clojure.lang.ExceptionInfo
                 (sri/eval-string "invalid syntax here $$$ @#@")))
    (is (thrown? clojure.lang.ExceptionInfo
                 (sri/eval-string "undefined_variable"))))

  (testing "Error contains source information"
    (try
      (sri/eval-string "invalid syntax")
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (is (contains? data :source))
          (is (contains? data :error))
          (is (= "invalid syntax" (:source data))))))))

(deftest test-complex-expressions
  (testing "Complex multi-line expressions"
    (is (= 120 (sri/eval-string "
      def factorial(n)
        if n <= 1
          1
        else
          n * factorial(n - 1)
        end
      end

      factorial(5)
    ")))

    (is (= "Alice is 30 years old" (sri/eval-string "
      name = 'Alice'
      age = 30
      name + ' is ' + age.to_s + ' years old'
    ")))))

(deftest test-eval-string-with-options
  (testing "eval-string with empty options map"
    (is (= 3 (sri/eval-string "1 + 2" {}))))

  (testing "eval-string single-arity form"
    (is (= 3 (sri/eval-string "1 + 2")))))
