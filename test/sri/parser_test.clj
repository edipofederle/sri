(ns sri.parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [sri.parser :as parser]
            [sri.tokenizer :as tokenizer]))

(defn parse-string
  "Helper to parse a string into an AST."
  [input]
  (let [tokens (tokenizer/tokenize input)
        state (parser/create-parse-state tokens)
        [final-state root-id] (parser/parse-expression state)]
    [(:ast final-state) root-id]))

(defn parse-statement-string
  "Helper to parse a statement string into an AST."
  [input]
  (let [tokens (tokenizer/tokenize input)
        state (parser/create-parse-state tokens)
        [final-state root-id] (parser/parse-statement state)]
    [(:ast final-state) root-id]))

(deftest test-integer-literal
  (testing "Parse integer literal"
    (let [[ast root-id] (parse-string "42")]
      (is (= :integer-literal (parser/get-component ast root-id :node-type)))
      (is (= 42 (parser/get-component ast root-id :value))))))

(deftest test-float-literal
  (testing "Parse float literal"
    (let [[ast root-id] (parse-string "3.14")]
      (is (= :float-literal (parser/get-component ast root-id :node-type)))
      (is (= 3.14 (parser/get-component ast root-id :value))))))

(deftest test-string-literal
  (testing "Parse string literal"
    (let [[ast root-id] (parse-string "\"hello\"")]
      (is (= :string-literal (parser/get-component ast root-id :node-type)))
      (is (= "hello" (parser/get-component ast root-id :value))))))

(deftest test-boolean-literals
  (testing "Parse boolean literals"
    (let [[ast-true root-true] (parse-string "true")
          [ast-false root-false] (parse-string "false")]
      (is (= :boolean-literal (parser/get-component ast-true root-true :node-type)))
      (is (= true (parser/get-component ast-true root-true :value)))
      (is (= :boolean-literal (parser/get-component ast-false root-false :node-type)))
      (is (= false (parser/get-component ast-false root-false :value))))))

; NOTE: attr_accessor, attr_reader, and attr_writer statements are now handled
; in preprocessing phase (see interpreter.cljc preprocess-attr-statements)
; These are no longer parsed as AST nodes, so parser tests are not needed.

(deftest test-symbol-literal
  (testing "Parse symbol literal"
    (let [[ast root-id] (parse-string ":hello")]
      (is (= :symbol-literal (parser/get-component ast root-id :node-type)))
      (is (= :hello (parser/get-component ast root-id :value))))))

(deftest test-nil-literal
  (testing "Parse nil literal"
    (let [[ast root-id] (parse-string "nil")]
      (is (= :nil-literal (parser/get-component ast root-id :node-type)))
      (is (= nil (parser/get-component ast root-id :value))))))

(deftest test-self-literal
  (testing "Parse self literal"
    (let [[ast root-id] (parse-string "self")]
      (is (= :self-literal (parser/get-component ast root-id :node-type)))
      (is (= :self (parser/get-component ast root-id :value))))))

(deftest test-identifier
  (testing "Parse identifier"
    (let [[ast root-id] (parse-string "foo")]
      (is (= :identifier (parser/get-component ast root-id :node-type)))
      (is (= "foo" (parser/get-component ast root-id :value))))))

(deftest test-parenthesized-expression
  (testing "Parse parenthesized expression"
    (let [[ast root-id] (parse-string "(42)")]
      (is (= :integer-literal (parser/get-component ast root-id :node-type)))
      (is (= 42 (parser/get-component ast root-id :value))))))

(deftest test-unary-operations
  (testing "Parse unary minus operations"
    ;; Simple unary minus with integer
    (let [[ast root-id] (parse-string "-42")]
      (is (= :unary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [operand-id (parser/get-component ast root-id :operand)]
        (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
        (is (= 42 (parser/get-component ast operand-id :value)))))

    ;; Unary minus with float
    (let [[ast root-id] (parse-string "-3.14")]
      (is (= :unary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [operand-id (parser/get-component ast root-id :operand)]
        (is (= :float-literal (parser/get-component ast operand-id :node-type)))
        (is (= 3.14 (parser/get-component ast operand-id :value)))))

    ;; Unary minus with parentheses
    (let [[ast root-id] (parse-string "(-10)")]
      (is (= :unary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [operand-id (parser/get-component ast root-id :operand)]
        (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
        (is (= 10 (parser/get-component ast operand-id :value)))))

    ;; Nested parentheses with unary minus
    (let [[ast root-id] (parse-string "((-5))")]
      (is (= :unary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [operand-id (parser/get-component ast root-id :operand)]
        (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
        (is (= 5 (parser/get-component ast operand-id :value)))))

    ;; Unary minus with identifier
    (let [[ast root-id] (parse-string "-x")]
      (is (= :unary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [operand-id (parser/get-component ast root-id :operand)]
        (is (= :identifier (parser/get-component ast operand-id :node-type)))
        (is (= "x" (parser/get-component ast operand-id :value))))))

  (testing "Parse unary operations in complex expressions"
    ;; Unary minus in binary operation - left side
    (let [[ast root-id] (parse-string "-5 + 3")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :unary-operation (parser/get-component ast left-id :node-type)))
        (is (= "-" (parser/get-component ast left-id :operator)))
        (is (= :integer-literal (parser/get-component ast right-id :node-type)))
        (is (= 3 (parser/get-component ast right-id :value)))))

    ;; Unary minus in binary operation - right side
    (let [[ast root-id] (parse-string "5 + -3")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :integer-literal (parser/get-component ast left-id :node-type)))
        (is (= 5 (parser/get-component ast left-id :value)))
        (is (= :unary-operation (parser/get-component ast right-id :node-type)))
        (is (= "-" (parser/get-component ast right-id :operator)))))

    ;; Unary minus in parentheses within binary operation
    (let [[ast root-id] (parse-string "(-10) + 5")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :unary-operation (parser/get-component ast left-id :node-type)))
        (is (= "-" (parser/get-component ast left-id :operator)))
        (is (= :integer-literal (parser/get-component ast right-id :node-type)))
        (is (= 5 (parser/get-component ast right-id :value)))))

    ;; Complex expression: 5 - (-3)
    (let [[ast root-id] (parse-string "5 - (-3)")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :integer-literal (parser/get-component ast left-id :node-type)))
        (is (= 5 (parser/get-component ast left-id :value)))
        (is (= :unary-operation (parser/get-component ast right-id :node-type)))
        (is (= "-" (parser/get-component ast right-id :operator)))
        (let [right-operand-id (parser/get-component ast right-id :operand)]
          (is (= :integer-literal (parser/get-component ast right-operand-id :node-type)))
          (is (= 3 (parser/get-component ast right-operand-id :value)))))))

  (testing "Parse unary operations in method calls"
    ;; Unary minus as method argument
    (let [[ast root-id] (parse-string "puts(-42)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "puts" (parser/get-component ast root-id :method)))
      (let [args (parser/get-component ast root-id :arguments)]
        (is (= 1 (count args)))
        (let [arg-id (first args)]
          (is (= :unary-operation (parser/get-component ast arg-id :node-type)))
          (is (= "-" (parser/get-component ast arg-id :operator)))
          (let [operand-id (parser/get-component ast arg-id :operand)]
            (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
            (is (= 42 (parser/get-component ast operand-id :value)))))))

    ;; Multiple unary arguments
    (let [[ast root-id] (parse-string "foo(-1, -2, -3)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "foo" (parser/get-component ast root-id :method)))
      (let [args (parser/get-component ast root-id :arguments)]
        (is (= 3 (count args)))
        (doseq [[idx expected-value] (map-indexed vector [1 2 3])]
          (let [arg-id (nth args idx)]
            (is (= :unary-operation (parser/get-component ast arg-id :node-type)))
            (is (= "-" (parser/get-component ast arg-id :operator)))
            (let [operand-id (parser/get-component ast arg-id :operand)]
              (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
              (is (= expected-value (parser/get-component ast operand-id :value))))))))))

(deftest test-arithmetic-binary-operations
  (testing "Parse arithmetic binary operations"
    (let [[ast root-id] (parse-string "2 + 3")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :integer-literal (parser/get-component ast left-id :node-type)))
        (is (= 2 (parser/get-component ast left-id :value)))
        (is (= :integer-literal (parser/get-component ast right-id :node-type)))
        (is (= 3 (parser/get-component ast right-id :value)))))

    (testing "All arithmetic operators"
      (doseq [op ["-" "*" "/" "%"]]
        (let [[ast root-id] (parse-string (str "5 " op " 2"))]
          (is (= :binary-operation (parser/get-component ast root-id :node-type)))
          (is (= op (parser/get-component ast root-id :operator))))))))

(deftest test-comparison-binary-operations
  (testing "Parse comparison binary operations"
    (doseq [op ["==" "!=" "<" "<=" ">" ">="]]
      (let [[ast root-id] (parse-string (str "x " op " y"))]
        (is (= :binary-operation (parser/get-component ast root-id :node-type)))
        (is (= op (parser/get-component ast root-id :operator)))
        (let [left-id (parser/get-component ast root-id :left)
              right-id (parser/get-component ast root-id :right)]
          (is (= :identifier (parser/get-component ast left-id :node-type)))
          (is (= "x" (parser/get-component ast left-id :value)))
          (is (= :identifier (parser/get-component ast right-id :node-type)))
          (is (= "y" (parser/get-component ast right-id :value))))))))

(deftest test-logical-binary-operations
  (testing "Parse logical binary operations"
    (doseq [op ["and" "or"]]
      (let [[ast root-id] (parse-string (str "true " op " false"))]
        (is (= :binary-operation (parser/get-component ast root-id :node-type)))
        (is (= op (parser/get-component ast root-id :operator)))
        (let [left-id (parser/get-component ast root-id :left)
              right-id (parser/get-component ast root-id :right)]
          (is (= :boolean-literal (parser/get-component ast left-id :node-type)))
          (is (= true (parser/get-component ast left-id :value)))
          (is (= :boolean-literal (parser/get-component ast right-id :node-type)))
          (is (= false (parser/get-component ast right-id :value))))))))

(deftest test-operator-precedence
  (testing "Operator precedence is respected"
    ;; Test multiplication has higher precedence than addition
    (let [[ast root-id] (parse-string "2 + 3 * 4")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        ;; Left should be simple integer
        (is (= :integer-literal (parser/get-component ast left-id :node-type)))
        (is (= 2 (parser/get-component ast left-id :value)))
        ;; Right should be multiplication operation
        (is (= :binary-operation (parser/get-component ast right-id :node-type)))
        (is (= "*" (parser/get-component ast right-id :operator)))))

    ;; Test comparison has lower precedence than arithmetic
    (let [[ast root-id] (parse-string "2 + 3 == 5")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "==" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        ;; Left should be addition operation
        (is (= :binary-operation (parser/get-component ast left-id :node-type)))
        (is (= "+" (parser/get-component ast left-id :operator)))
        ;; Right should be simple integer
        (is (= :integer-literal (parser/get-component ast right-id :node-type)))
        (is (= 5 (parser/get-component ast right-id :value)))))

    ;; Test logical AND has lower precedence than comparison
    (let [[ast root-id] (parse-string "x > 0 and y < 10")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "and" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        ;; Both sides should be comparison operations
        (is (= :binary-operation (parser/get-component ast left-id :node-type)))
        (is (= ">" (parser/get-component ast left-id :operator)))
        (is (= :binary-operation (parser/get-component ast right-id :node-type)))
        (is (= "<" (parser/get-component ast right-id :operator)))))))

(deftest test-left-associativity
  (testing "Left associativity for same precedence operators"
    ;; Test 1 - 2 - 3 parses as (1 - 2) - 3
    (let [[ast root-id] (parse-string "1 - 2 - 3")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "-" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        ;; Left should be subtraction operation (1 - 2)
        (is (= :binary-operation (parser/get-component ast left-id :node-type)))
        (is (= "-" (parser/get-component ast left-id :operator)))
        ;; Right should be simple integer 3
        (is (= :integer-literal (parser/get-component ast right-id :node-type)))
        (is (= 3 (parser/get-component ast right-id :value)))))))

(deftest test-standalone-method-calls
  (testing "Parse standalone method calls"
    ;; Method call without arguments
    (let [[ast root-id] (parse-string "puts()")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "puts" (parser/get-component ast root-id :method)))
      (is (= nil (parser/get-component ast root-id :receiver)))
      (is (= [] (parser/get-component ast root-id :arguments))))

    ;; Method call with single argument
    (let [[ast root-id] (parse-string "puts(42)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "puts" (parser/get-component ast root-id :method)))
      (is (= nil (parser/get-component ast root-id :receiver)))
      (let [args (parser/get-component ast root-id :arguments)]
        (is (= 1 (count args)))
        (let [arg-id (first args)]
          (is (= :integer-literal (parser/get-component ast arg-id :node-type)))
          (is (= 42 (parser/get-component ast arg-id :value))))))

    ;; Method call with multiple arguments
    (let [[ast root-id] (parse-string "foo(1, \"hello\", true)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "foo" (parser/get-component ast root-id :method)))
      (is (= nil (parser/get-component ast root-id :receiver)))
      (let [args (parser/get-component ast root-id :arguments)]
        (is (= 3 (count args)))
        (let [[arg1 arg2 arg3] args]
          (is (= :integer-literal (parser/get-component ast arg1 :node-type)))
          (is (= 1 (parser/get-component ast arg1 :value)))
          (is (= :string-literal (parser/get-component ast arg2 :node-type)))
          (is (= "hello" (parser/get-component ast arg2 :value)))
          (is (= :boolean-literal (parser/get-component ast arg3 :node-type)))
          (is (= true (parser/get-component ast arg3 :value))))))))

(deftest test-method-calls-with-receiver
  (testing "Parse method calls with receiver"
    ;; Simple receiver method call
    (let [[ast root-id] (parse-string "obj.method()")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "method" (parser/get-component ast root-id :method)))
      (is (= [] (parser/get-component ast root-id :arguments)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :identifier (parser/get-component ast receiver-id :node-type)))
        (is (= "obj" (parser/get-component ast receiver-id :value)))))

    ;; Receiver method call with arguments
    (let [[ast root-id] (parse-string "user.greet(\"Hello\", name)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "greet" (parser/get-component ast root-id :method)))
      (let [receiver-id (parser/get-component ast root-id :receiver)
            args (parser/get-component ast root-id :arguments)]
        (is (= :identifier (parser/get-component ast receiver-id :node-type)))
        (is (= "user" (parser/get-component ast receiver-id :value)))
        (is (= 2 (count args)))
        (let [[arg1 arg2] args]
          (is (= :string-literal (parser/get-component ast arg1 :node-type)))
          (is (= "Hello" (parser/get-component ast arg1 :value)))
          (is (= :identifier (parser/get-component ast arg2 :node-type)))
          (is (= "name" (parser/get-component ast arg2 :value))))))

    ;; Chained method calls
    (let [[ast root-id] (parse-string "obj.method1().method2()")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "method2" (parser/get-component ast root-id :method)))
      (is (= [] (parser/get-component ast root-id :arguments)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :method-call (parser/get-component ast receiver-id :node-type)))
        (is (= "method1" (parser/get-component ast receiver-id :method)))
        (is (= [] (parser/get-component ast receiver-id :arguments)))
        (let [nested-receiver-id (parser/get-component ast receiver-id :receiver)]
          (is (= :identifier (parser/get-component ast nested-receiver-id :node-type)))
          (is (= "obj" (parser/get-component ast nested-receiver-id :value))))))))

(deftest test-method-calls-in-expressions
  (testing "Parse method calls as part of larger expressions"
      ;; Method call in binary operation
    (let [[ast root-id] (parse-string "foo() + bar()")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "+" (parser/get-component ast root-id :operator)))
      (let [left-id (parser/get-component ast root-id :left)
            right-id (parser/get-component ast root-id :right)]
        (is (= :method-call (parser/get-component ast left-id :node-type)))
        (is (= "foo" (parser/get-component ast left-id :method)))
        (is (= :method-call (parser/get-component ast right-id :node-type)))
        (is (= "bar" (parser/get-component ast right-id :method)))))

      ;; Method call as argument to another method call
    (let [[ast root-id] (parse-string "puts(getValue())")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "puts" (parser/get-component ast root-id :method)))
      (let [args (parser/get-component ast root-id :arguments)]
        (is (= 1 (count args)))
        (let [arg-id (first args)]
          (is (= :method-call (parser/get-component ast arg-id :node-type)))
          (is (= "getValue" (parser/get-component ast arg-id :method))))))))

(deftest test-if-statement-without-else
  (testing "Parse if statement without else clause"
    (let [[ast root-id] (parse-statement-string "if true\n42\nend")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))

        ;; Check condition
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :boolean-literal (parser/get-component ast condition-id :node-type)))
        (is (= true (parser/get-component ast condition-id :value))))

        ;; Check then branch
      (let [then-id (parser/get-component ast root-id :then-branch)]
        (is (= :block (parser/get-component ast then-id :node-type)))
        (let [statements (parser/get-component ast then-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :integer-literal (parser/get-component ast stmt-id :node-type)))
            (is (= 42 (parser/get-component ast stmt-id :value))))))

        ;; Check no else branch
      (is (nil? (parser/get-component ast root-id :else-branch))))))

(deftest test-if-statement-with-else
  (testing "Parse if statement with else clause"
    (let [[ast root-id] (parse-statement-string "if false\n1\nelse\n2\nend")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :boolean-literal (parser/get-component ast condition-id :node-type)))
        (is (= false (parser/get-component ast condition-id :value))))

      ;; Check then branch
      (let [then-id (parser/get-component ast root-id :then-branch)]
        (is (= :block (parser/get-component ast then-id :node-type)))
        (let [statements (parser/get-component ast then-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :integer-literal (parser/get-component ast stmt-id :node-type)))
            (is (= 1 (parser/get-component ast stmt-id :value))))))

      ;; Check else branch
      (let [else-id (parser/get-component ast root-id :else-branch)]
        (is (= :block (parser/get-component ast else-id :node-type)))
        (let [statements (parser/get-component ast else-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :integer-literal (parser/get-component ast stmt-id :node-type)))
            (is (= 2 (parser/get-component ast stmt-id :value)))))))))

(deftest test-if-statement-with-complex-condition
  (testing "Parse if statement with complex condition"
    (let [[ast root-id] (parse-statement-string "if x > 5 and y < 10\nresult\nend")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition is a binary operation
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :binary-operation (parser/get-component ast condition-id :node-type)))
        (is (= "and" (parser/get-component ast condition-id :operator)))))))

(deftest test-if-statement-with-multiple-statements
  (testing "Parse if statement with multiple statements in branches"
    (let [[ast root-id] (parse-statement-string "if true\na\nb\nelse\nc\nd\nend")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))

      ;; Check then branch has 2 statements
      (let [then-id (parser/get-component ast root-id :then-branch)]
        (is (= :block (parser/get-component ast then-id :node-type)))
        (let [statements (parser/get-component ast then-id :statements)]
          (is (= 2 (count statements)))
          (let [[stmt1 stmt2] statements]
            (is (= :identifier (parser/get-component ast stmt1 :node-type)))
            (is (= "a" (parser/get-component ast stmt1 :value)))
            (is (= :identifier (parser/get-component ast stmt2 :node-type)))
            (is (= "b" (parser/get-component ast stmt2 :value))))))

      ;; Check else branch has 2 statements
      (let [else-id (parser/get-component ast root-id :else-branch)]
        (is (= :block (parser/get-component ast else-id :node-type)))
        (let [statements (parser/get-component ast else-id :statements)]
          (is (= 2 (count statements)))
          (let [[stmt1 stmt2] statements]
            (is (= :identifier (parser/get-component ast stmt1 :node-type)))
            (is (= "c" (parser/get-component ast stmt1 :value)))
            (is (= :identifier (parser/get-component ast stmt2 :node-type)))
            (is (= "d" (parser/get-component ast stmt2 :value)))))))))

(deftest test-while-statement-basic
  (testing "Parse basic while loop"
    (let [[ast root-id] (parse-statement-string "while x > 0\nresult\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :binary-operation (parser/get-component ast condition-id :node-type)))
        (is (= ">" (parser/get-component ast condition-id :operator))))

      ;; Check body
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :identifier (parser/get-component ast stmt-id :node-type)))
            (is (= "result" (parser/get-component ast stmt-id :value)))))))))

(deftest test-while-statement-with-multiple-statements
  (testing "Parse while loop with multiple statements in body"
    (let [[ast root-id] (parse-statement-string "while true\na\nb\nc\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :boolean-literal (parser/get-component ast condition-id :node-type)))
        (is (= true (parser/get-component ast condition-id :value))))

      ;; Check body has 3 statements
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 3 (count statements)))
          (let [[stmt1 stmt2 stmt3] statements]
            (is (= :identifier (parser/get-component ast stmt1 :node-type)))
            (is (= "a" (parser/get-component ast stmt1 :value)))
            (is (= :identifier (parser/get-component ast stmt2 :node-type)))
            (is (= "b" (parser/get-component ast stmt2 :value)))
            (is (= :identifier (parser/get-component ast stmt3 :node-type)))
            (is (= "c" (parser/get-component ast stmt3 :value)))))))))

(deftest test-while-statement-with-complex-condition
  (testing "Parse while loop with complex condition"
    (let [[ast root-id] (parse-statement-string "while x > 0 and y < 10\nresult\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition is a complex binary operation
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :binary-operation (parser/get-component ast condition-id :node-type)))
        (is (= "and" (parser/get-component ast condition-id :operator)))))))

(deftest test-while-statement-with-method-calls
  (testing "Parse while loop with method calls in condition and body"
    (let [[ast root-id] (parse-statement-string "while hasMore()\nprocess()\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))

      ;; Check condition is a method call
      (let [condition-id (parser/get-component ast root-id :condition)]
        (is (= :method-call (parser/get-component ast condition-id :node-type)))
        (is (= "hasMore" (parser/get-component ast condition-id :method))))

      ;; Check body contains method call
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :method-call (parser/get-component ast stmt-id :node-type)))
            (is (= "process" (parser/get-component ast stmt-id :method)))))))))

(deftest test-nested-while-loops
  (testing "Parse nested while loops"
    (let [[ast root-id] (parse-statement-string "while x > 0\nwhile y > 0\ninner\nend\nouter\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))

      ;; Check outer loop body contains inner while loop
      (let [outer-body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast outer-body-id :node-type)))
        (let [statements (parser/get-component ast outer-body-id :statements)]
          (is (= 2 (count statements)))
          (let [inner-while-id (first statements)]
            (is (= :while-statement (parser/get-component ast inner-while-id :node-type)))))))))

(deftest test-assignment-statement-basic
  (testing "Parse basic assignment statements"
    ;; Simple variable assignment
    (let [[ast root-id] (parse-statement-string "x = 42")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "x" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :integer-literal (parser/get-component ast value-id :node-type)))
        (is (= 42 (parser/get-component ast value-id :value)))))

    ;; Assignment with string literal
    (let [[ast root-id] (parse-statement-string "name = \"Alice\"")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "name" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :string-literal (parser/get-component ast value-id :node-type)))
        (is (= "Alice" (parser/get-component ast value-id :value)))))

    ;; Assignment with boolean literal
    (let [[ast root-id] (parse-statement-string "flag = true")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "flag" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :boolean-literal (parser/get-component ast value-id :node-type)))
        (is (= true (parser/get-component ast value-id :value)))))))

(deftest test-assignment-statement-with-expressions
  (testing "Parse assignment statements with complex expressions"
    ;; Assignment with arithmetic expression
    (let [[ast root-id] (parse-statement-string "result = 2 + 3 * 4")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "result" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :binary-operation (parser/get-component ast value-id :node-type)))
        (is (= "+" (parser/get-component ast value-id :operator)))))

    ;; Assignment with method call
    (let [[ast root-id] (parse-statement-string "output = puts(\"hello\")")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "output" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :method-call (parser/get-component ast value-id :node-type)))
        (is (= "puts" (parser/get-component ast value-id :method)))))

    ;; Assignment with identifier (variable reference)
    (let [[ast root-id] (parse-statement-string "y = x")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "y" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :identifier (parser/get-component ast value-id :node-type)))
        (is (= "x" (parser/get-component ast value-id :value)))))))

(deftest test-assignment-statement-in-blocks
  (testing "Parse assignment statements inside control flow blocks"
    ;; Assignment in if statement
    (let [[ast root-id] (parse-statement-string "if true\nx = 10\nend")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))
      (let [then-id (parser/get-component ast root-id :then-branch)]
        (is (= :block (parser/get-component ast then-id :node-type)))
        (let [statements (parser/get-component ast then-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :assignment-statement (parser/get-component ast stmt-id :node-type)))
            (is (= "x" (parser/get-component ast stmt-id :variable)))))))

    ;; Assignment in while loop
    (let [[ast root-id] (parse-statement-string "while true\ncounter = counter + 1\nend")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :assignment-statement (parser/get-component ast stmt-id :node-type)))
            (is (= "counter" (parser/get-component ast stmt-id :variable)))))))))

(deftest test-assignment-vs-equality
  (testing "Parse assignment vs equality comparison correctly"
    ;; Single = is assignment
    (let [[ast root-id] (parse-statement-string "x = 5")]
      (is (= :assignment-statement (parser/get-component ast root-id :node-type)))
      (is (= "x" (parser/get-component ast root-id :variable))))

    ;; Double == is equality comparison (should be parsed as expression)
    (let [[ast root-id] (parse-statement-string "x == 5")]
      (is (= :binary-operation (parser/get-component ast root-id :node-type)))
      (is (= "==" (parser/get-component ast root-id :operator))))))

(deftest test-method-definition-basic
  (testing "Parse basic method definitions"
    ;; Method with no parameters
    (let [[ast root-id] (parse-statement-string "def hello()\nputs(\"Hello\")\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "hello" (parser/get-component ast root-id :name)))
      (is (= [] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :method-call (parser/get-component ast stmt-id :node-type)))
            (is (= "puts" (parser/get-component ast stmt-id :method)))))))

    ;; Method with single parameter
    (let [[ast root-id] (parse-statement-string "def greet(name)\nputs(name)\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "greet" (parser/get-component ast root-id :name)))
      (is (= ["name"] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))))

    ;; Method with multiple parameters
    (let [[ast root-id] (parse-statement-string "def add(x, y)\nx + y\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "add" (parser/get-component ast root-id :name)))
      (is (= ["x" "y"] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :binary-operation (parser/get-component ast stmt-id :node-type)))
            (is (= "+" (parser/get-component ast stmt-id :operator)))))))))

(deftest test-method-definition-complex
  (testing "Parse complex method definitions"
    ;; Method with control flow
    (let [[ast root-id] (parse-statement-string "def factorial(n)\nif n <= 1\n1\nelse\nn * factorial(n - 1)\nend\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "factorial" (parser/get-component ast root-id :name)))
      (is (= ["n"] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :if-statement (parser/get-component ast stmt-id :node-type)))))))

    ;; Method with multiple statements
    (let [[ast root-id] (parse-statement-string "def process(data)\nvalidate(data)\nresult = transform(data)\nresult\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "process" (parser/get-component ast root-id :name)))
      (is (= ["data"] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 3 (count statements)))
          (let [[stmt1 stmt2 stmt3] statements]
            (is (= :method-call (parser/get-component ast stmt1 :node-type)))
            (is (= "validate" (parser/get-component ast stmt1 :method)))
            (is (= :assignment-statement (parser/get-component ast stmt2 :node-type)))
            (is (= "result" (parser/get-component ast stmt2 :variable)))
            (is (= :identifier (parser/get-component ast stmt3 :node-type)))
            (is (= "result" (parser/get-component ast stmt3 :value)))))))))

(deftest test-semicolon-handling
  (testing "Parse statements with semicolon separators"
    ;; Method definition with semicolons
    (let [[ast root-id] (parse-statement-string "def foo(); end")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "foo" (parser/get-component ast root-id :name)))
      (is (= [] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (is (= [] (parser/get-component ast body-id :statements)))))

    ;; Method definition with semicolon after parameter list
    (let [[ast root-id] (parse-statement-string "def foo(a); a + 2; end")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "foo" (parser/get-component ast root-id :name)))
      (is (= ["a"] (parser/get-component ast root-id :parameters)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :binary-operation (parser/get-component ast stmt-id :node-type)))
            (is (= "+" (parser/get-component ast stmt-id :operator)))))))

    ;; Multiple statements separated by semicolons
    (let [[ast root-id] (parse-statement-string "def process(); x = 1; y = 2; z = x + y; end")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (is (= "process" (parser/get-component ast root-id :name)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 3 (count statements)))
          (let [[stmt1 stmt2 stmt3] statements]
            ;; First statement: x = 1
            (is (= :assignment-statement (parser/get-component ast stmt1 :node-type)))
            (is (= "x" (parser/get-component ast stmt1 :variable)))
            ;; Second statement: y = 2
            (is (= :assignment-statement (parser/get-component ast stmt2 :node-type)))
            (is (= "y" (parser/get-component ast stmt2 :variable)))
            ;; Third statement: z = x + y
            (is (= :assignment-statement (parser/get-component ast stmt3 :node-type)))
            (is (= "z" (parser/get-component ast stmt3 :variable)))))))

    ;; If statement with semicolons
    (let [[ast root-id] (parse-statement-string "if true; puts(\"yes\"); else; puts(\"no\"); end")]
      (is (= :if-statement (parser/get-component ast root-id :node-type)))
      (let [then-id (parser/get-component ast root-id :then-branch)]
        (is (= :block (parser/get-component ast then-id :node-type)))
        (let [statements (parser/get-component ast then-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :method-call (parser/get-component ast stmt-id :node-type)))
            (is (= "puts" (parser/get-component ast stmt-id :method))))))
      (let [else-id (parser/get-component ast root-id :else-branch)]
        (is (= :block (parser/get-component ast else-id :node-type)))
        (let [statements (parser/get-component ast else-id :statements)]
          (is (= 1 (count statements)))
          (let [stmt-id (first statements)]
            (is (= :method-call (parser/get-component ast stmt-id :node-type)))
            (is (= "puts" (parser/get-component ast stmt-id :method)))))))

    ;; While statement with semicolons
    (let [[ast root-id] (parse-statement-string "while x > 0; x = x - 1; puts(x); end")]
      (is (= :while-statement (parser/get-component ast root-id :node-type)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 2 (count statements)))
          (let [[stmt1 stmt2] statements]
            ;; First statement: x = x - 1
            (is (= :assignment-statement (parser/get-component ast stmt1 :node-type)))
            (is (= "x" (parser/get-component ast stmt1 :variable)))
            ;; Second statement: puts(x)
            (is (= :method-call (parser/get-component ast stmt2 :node-type)))
            (is (= "puts" (parser/get-component ast stmt2 :method)))))))

    ;; Mixed semicolons and newlines
    (let [[ast root-id] (parse-statement-string "def mixed(); a = 1;\nb = 2\nc = 3; d = 4\nend")]
      (is (= :method-definition (parser/get-component ast root-id :node-type)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 4 (count statements)))
          (let [[stmt1 stmt2 stmt3 stmt4] statements]
            (is (= :assignment-statement (parser/get-component ast stmt1 :node-type)))
            (is (= "a" (parser/get-component ast stmt1 :variable)))
            (is (= :assignment-statement (parser/get-component ast stmt2 :node-type)))
            (is (= "b" (parser/get-component ast stmt2 :variable)))
            (is (= :assignment-statement (parser/get-component ast stmt3 :node-type)))
            (is (= "c" (parser/get-component ast stmt3 :variable)))
            (is (= :assignment-statement (parser/get-component ast stmt4 :node-type)))
            (is (= "d" (parser/get-component ast stmt4 :variable)))))))))

(deftest test-parser-error-handling
  (testing "Parser throws meaningful errors for invalid syntax"

    ;; Test unexpected token in expression - this is what actually happens with "foo("
    (is (thrown-with-msg? Exception #"Expected primary expression"
          (parse-string "foo(")))

    ;; Test starting with operator
    (is (thrown-with-msg? Exception #"Expected primary expression"
          (parse-string "+")))

    ;; Test invalid argument list
    (is (thrown-with-msg? Exception #"Expected.*,.*or.*\)"
          (parse-string "foo(1 2)")))

    ;; Test missing method name after dot
    (is (thrown-with-msg? Exception #"Expected method name after"
          (parse-string "obj.")))

    ;; Test missing 'end' keyword
    (is (thrown-with-msg? Exception #"Expected.*end"
          (parse-statement-string "if true\n42")))

    ;; Test unexpected EOF in block
    (is (thrown-with-msg? Exception #"Expected.*end.*close block"
          (parse-statement-string "if true\n")))

    ;; Test missing 'end' for while loop
    (is (thrown-with-msg? Exception #"Expected.*end"
          (parse-statement-string "while true\n42")))

    ;; Test expect-token function directly for structured error data
    (try
      (let [tokens (tokenizer/tokenize "puts(")
            state (parser/create-parse-state tokens)]
        (parser/expect-token state :operator ")"))
      (catch Exception e
        (let [data (ex-data e)]
          (is (contains? data :expected-type))
          (is (contains? data :actual-token))
          (is (= :operator (:expected-type data)))
          (is (contains? data :position))
          (is (contains? (:position data) :line))
          (is (contains? (:position data) :column)))))))

(deftest test-parenthesized-method-calls
  (testing "Parse method calls on parenthesized expressions"
    ;; Basic parenthesized unary expression with method call
    (let [[ast root-id] (parse-string "(-1).positive?")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "positive?" (parser/get-component ast root-id :method)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :unary-operation (parser/get-component ast receiver-id :node-type)))
        (is (= "-" (parser/get-component ast receiver-id :operator)))
        (let [operand-id (parser/get-component ast receiver-id :operand)]
          (is (= :integer-literal (parser/get-component ast operand-id :node-type)))
          (is (= 1 (parser/get-component ast operand-id :value))))))

    ;; Parenthesized binary expression with method call
    (let [[ast root-id] (parse-string "(5 + 3).even?")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "even?" (parser/get-component ast root-id :method)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :binary-operation (parser/get-component ast receiver-id :node-type)))
        (is (= "+" (parser/get-component ast receiver-id :operator)))))

    ;; Nested parentheses with method call
    (let [[ast root-id] (parse-string "((10 - 5)).abs")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "abs" (parser/get-component ast root-id :method)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :binary-operation (parser/get-component ast receiver-id :node-type)))
        (is (= "-" (parser/get-component ast receiver-id :operator)))))

    ;; Method call with arguments on parenthesized expression
    (let [[ast root-id] (parse-string "(-7).clamp(0, 10)")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "clamp" (parser/get-component ast root-id :method)))
      (let [args (parser/get-component ast root-id :arguments)
            receiver-id (parser/get-component ast root-id :receiver)]
        (is (= 2 (count args)))
        (is (= :unary-operation (parser/get-component ast receiver-id :node-type)))
        (is (= "-" (parser/get-component ast receiver-id :operator)))))

    ;; Chained method calls on parenthesized expression
    (let [[ast root-id] (parse-string "(-42).abs.to_s")]
      (is (= :method-call (parser/get-component ast root-id :node-type)))
      (is (= "to_s" (parser/get-component ast root-id :method)))
      (let [receiver-id (parser/get-component ast root-id :receiver)]
        (is (= :method-call (parser/get-component ast receiver-id :node-type)))
        (is (= "abs" (parser/get-component ast receiver-id :method)))))))

(deftest test-class-definition-parsing
  (testing "Parse basic class definition"
    (let [[ast root-id] (parse-statement-string "class Integer\nend")]
      (is (= :class-definition (parser/get-component ast root-id :node-type)))
      (is (= "Integer" (parser/get-component ast root-id :name)))
      (is (nil? (parser/get-component ast root-id :parent-class)))))

  (testing "Parse class definition with inheritance"
    (let [[ast root-id] (parse-statement-string "class MyInt < Integer\nend")]
      (is (= :class-definition (parser/get-component ast root-id :node-type)))
      (is (= "MyInt" (parser/get-component ast root-id :name)))
      (is (= "Integer" (parser/get-component ast root-id :parent-class)))))

  (testing "Parse class with method definition"
    (let [[ast root-id] (parse-statement-string "class Integer\ndef zero?\nself == 0\nend\nend")]
      (is (= :class-definition (parser/get-component ast root-id :node-type)))
      (is (= "Integer" (parser/get-component ast root-id :name)))
      (let [body-id (parser/get-component ast root-id :body)]
        (is (= :block (parser/get-component ast body-id :node-type)))
        (let [statements (parser/get-component ast body-id :statements)]
          (is (= 1 (count statements)))
          (let [method-id (first statements)]
            (is (= :method-definition (parser/get-component ast method-id :node-type)))
            (is (= "zero?" (parser/get-component ast method-id :name)))))))))

(deftest test-instance-variable-parsing
  (testing "Parse instance variable assignment"
    (let [[ast root-id] (parse-statement-string "@name = \"John\"")]
      (is (= :instance-variable-assignment (parser/get-component ast root-id :node-type)))
      (is (= "@name" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :string-literal (parser/get-component ast value-id :node-type)))
        (is (= "John" (parser/get-component ast value-id :value))))))

  (testing "Parse instance variable access"
    (let [[ast root-id] (parse-statement-string "@name")]
      (is (= :instance-variable-access (parser/get-component ast root-id :node-type)))
      (is (= "@name" (parser/get-component ast root-id :variable))))))

(deftest test-class-variable-parsing
  (testing "Parse class variable assignment"
    (let [[ast root-id] (parse-statement-string "@@count = 42")]
      (is (= :class-variable-assignment (parser/get-component ast root-id :node-type)))
      (is (= "@@count" (parser/get-component ast root-id :variable)))
      (let [value-id (parser/get-component ast root-id :value)]
        (is (= :integer-literal (parser/get-component ast value-id :node-type)))
        (is (= 42 (parser/get-component ast value-id :value))))))

  (testing "Parse class variable access"
    (let [[ast root-id] (parse-statement-string "@@count")]
      (is (= :class-variable-access (parser/get-component ast root-id :node-type)))
      (is (= "@@count" (parser/get-component ast root-id :variable))))))
