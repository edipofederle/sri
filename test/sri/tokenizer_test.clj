(ns sri.tokenizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [sri.tokenizer :refer [tokenize]]))

(deftest test-keywords
  (testing "Keywords are recognized correctly"
    (let [tokens (tokenize "if else end def")]
      (is (= 4 (count tokens)))
      (is (= :keyword (:type (first tokens))))
      (is (= "if" (:value (first tokens))))
      (is (= :keyword (:type (second tokens))))
      (is (= "else" (:value (second tokens)))))))

(deftest test-self-keyword
  (testing "Self keyword is recognized correctly"
    (let [tokens (tokenize "self")]
      (is (= 1 (count tokens)))
      (is (= :keyword (:type (first tokens))))
      (is (= "self" (:value (first tokens)))))))

(deftest test-identifiers
  (testing "Identifiers are recognized correctly"
    (let [tokens (tokenize "variable_name method? method!")]
      (is (= 3 (count tokens)))
      (is (= :identifier (:type (first tokens))))
      (is (= "variable_name" (:value (first tokens))))
      (is (= :identifier (:type (second tokens))))
      (is (= "method?" (:value (second tokens))))
      (is (= :identifier (:type (nth tokens 2))))
      (is (= "method!" (:value (nth tokens 2)))))))

(deftest test-integers
  (testing "Integer literals are tokenized correctly"
    (let [tokens (tokenize "42 0 123")]
      (is (= 3 (count tokens)))
      (is (= :integer (:type (first tokens))))
      (is (= "42" (:value (first tokens))))
      (is (= :integer (:type (second tokens))))
      (is (= "0" (:value (second tokens)))))))

(deftest test-floats
  (testing "Float literals are tokenized correctly"
    (let [tokens (tokenize "3.14 0.5 42.0")]
      (is (= 3 (count tokens)))
      (is (= :float (:type (first tokens))))
      (is (= "3.14" (:value (first tokens))))
      (is (= :float (:type (second tokens))))
      (is (= "0.5" (:value (second tokens)))))))

(deftest test-symbols
  (testing "Symbol literals are tokenized correctly"
    (let [tokens (tokenize ":hello :world :method_name?")]
      (is (= 3 (count tokens)))
      (is (= :symbol (:type (first tokens))))
      (is (= "hello" (:value (first tokens))))
      (is (= :symbol (:type (second tokens))))
      (is (= "world" (:value (second tokens))))
      (is (= :symbol (:type (nth tokens 2))))
      (is (= "method_name?" (:value (nth tokens 2)))))))

(deftest test-colon-vs-symbol
  (testing "Distinguish between colon operator and symbol literals"
    (let [tokens (tokenize "{key: value, :symbol => other}")]
      (is (= 9 (count tokens)))
      ;; { key : value , :symbol => other }
      (is (= :operator (:type (first tokens))))      ; {
      (is (= :identifier (:type (second tokens))))   ; key
      (is (= :operator (:type (nth tokens 2))))      ; :
      (is (= ":" (:value (nth tokens 2))))
      (is (= :identifier (:type (nth tokens 3))))    ; value
      (is (= :operator (:type (nth tokens 4))))      ; ,
      (is (= :symbol (:type (nth tokens 5))))        ; :symbol
      (is (= "symbol" (:value (nth tokens 5))))
      (is (= :operator (:type (nth tokens 6))))      ; =>
      (is (= :identifier (:type (nth tokens 7))))    ; other
      (is (= :operator (:type (nth tokens 8))))      ; }
      )))

(deftest test-strings
  (testing "String literals are tokenized correctly"
    (let [tokens (tokenize "\"hello\" 'world'")]
      (is (= 2 (count tokens)))
      (is (= :string (:type (first tokens))))
      (is (= "hello" (:value (first tokens))))
      (is (= :string (:type (second tokens))))
      (is (= "world" (:value (second tokens)))))))

(deftest test-string-escapes
  (testing "String escape sequences work correctly"
    (let [tokens (tokenize "\"hello\\nworld\" \"tab\\there\"")]
      (is (= 2 (count tokens)))
      (is (= "hello\nworld" (:value (first tokens))))
      (is (= "tab\there" (:value (second tokens)))))))

(deftest test-operators
  (testing "Operators are tokenized correctly"
    (let [tokens (tokenize "+ - * / % == != < <= > >= =")]
      (is (= 12 (count tokens)))
      (is (every? #(= :operator (:type %)) tokens))
      (is (= "+" (:value (first tokens))))
      (is (= "==" (:value (nth tokens 5))))
      (is (= "!=" (:value (nth tokens 6)))))))

(deftest test-punctuation
  (testing "Punctuation is tokenized correctly"
    (let [tokens (tokenize "( ) [ ] { } , ; .")]
      (is (= 9 (count tokens)))
      (is (every? #(= :operator (:type %)) tokens))
      (is (= "(" (:value (first tokens))))
      (is (= ")" (:value (second tokens)))))))

(deftest test-hash-arrow
  (testing "Hash arrow operator is tokenized correctly"
    (let [tokens (tokenize "=>")]
      (is (= 1 (count tokens)))
      (is (= :operator (:type (first tokens))))
      (is (= "=>" (:value (first tokens)))))))

(deftest test-comments
  (testing "Comments are skipped correctly"
    (let [tokens (tokenize "variable # this is a comment\nother")]
      (is (= 3 (count tokens)))
      (is (= "variable" (:value (first tokens))))
      (is (= :newline (:type (second tokens))))
      (is (= "other" (:value (nth tokens 2)))))))

(deftest test-newlines
  (testing "Newlines are preserved as tokens"
    (let [tokens (tokenize "line1\nline2")]
      (is (= 3 (count tokens)))
      (is (= "line1" (:value (first tokens))))
      (is (= :newline (:type (second tokens))))
      (is (= "line2" (:value (nth tokens 2)))))))

(deftest test-position-tracking
  (testing "Token positions are tracked correctly"
    (let [tokens (tokenize "hello\nworld")]
      (is (= 1 (:line (first tokens))))
      (is (= 1 (:column (first tokens))))
      (is (= 1 (:line (second tokens))))
      (is (= 6 (:column (second tokens))))
      (is (= 2 (:line (nth tokens 2))))
      (is (= 1 (:column (nth tokens 2)))))))

(deftest test-complex-expression
  (testing "Complex expression tokenization"
    (let [tokens (tokenize "def factorial(n)\n  if n <= 1\n    1\n  else\n    n * factorial(n - 1)\n  end\nend")]
      (is (> (count tokens) 20))
      (is (= "def" (:value (first tokens))))
      (is (= "factorial" (:value (second tokens))))
      (is (= "(" (:value (nth tokens 2))))
      (is (= "n" (:value (nth tokens 3))))
      (is (= ")" (:value (nth tokens 4)))))))

(deftest test-array-syntax
  (testing "Array syntax tokenization"
    (let [tokens (tokenize "[1, 2, 3]")]
      (is (= 7 (count tokens)))
      (is (= "[" (:value (first tokens))))
      (is (= "1" (:value (second tokens))))
      (is (= "," (:value (nth tokens 2))))
      (is (= "]" (:value (nth tokens 6)))))))

(deftest test-hash-syntax
  (testing "Hash syntax tokenization"
    (let [tokens (tokenize "{\"key\" => \"value\"}")]
      (is (= 5 (count tokens)))
      (is (= "{" (:value (first tokens))))
      (is (= "key" (:value (second tokens))))
      (is (= "=>" (:value (nth tokens 2))))
      (is (= "value" (:value (nth tokens 3))))
      (is (= "}" (:value (nth tokens 4)))))))

(deftest test-instance-variables
  (testing "Instance variable tokenization"
    (let [tokens (tokenize "@name")]
      (is (= 1 (count tokens)))
      (is (= :instance-variable (:type (first tokens))))
      (is (= "@name" (:value (first tokens)))))

    (let [tokens (tokenize "@count = 5")]
      (is (= 3 (count tokens)))
      (is (= :instance-variable (:type (first tokens))))
      (is (= "@count" (:value (first tokens))))
      (is (= "=" (:value (second tokens))))
      (is (= "5" (:value (nth tokens 2)))))))

(deftest test-class-variables
  (testing "Class variable tokenization"
    (let [tokens (tokenize "@@total")]
      (is (= 1 (count tokens)))
      (is (= :class-variable (:type (first tokens))))
      (is (= "@@total" (:value (first tokens)))))

    (let [tokens (tokenize "@@count = 10")]
      (is (= 3 (count tokens)))
      (is (= :class-variable (:type (first tokens))))
      (is (= "@@count" (:value (first tokens))))
      (is (= "=" (:value (second tokens))))
      (is (= "10" (:value (nth tokens 2)))))))

(deftest test-class-module-keywords
  (testing "Class and module keywords"
    (let [tokens (tokenize "class Integer")]
      (is (= 2 (count tokens)))
      (is (= :keyword (:type (first tokens))))
      (is (= "class" (:value (first tokens))))
      (is (= :identifier (:type (second tokens))))
      (is (= "Integer" (:value (second tokens)))))

    (let [tokens (tokenize "module Math")]
      (is (= 2 (count tokens)))
      (is (= :keyword (:type (first tokens))))
      (is (= "module" (:value (first tokens))))
      (is (= :identifier (:type (second tokens))))
      (is (= "Math" (:value (second tokens)))))))

(deftest test-whitespace-handling
  (testing "Whitespace is properly skipped"
    (let [tokens (tokenize "  hello   world  ")]
      (is (= 2 (count tokens)))
      (is (= "hello" (:value (first tokens))))
      (is (= "world" (:value (second tokens)))))))

(deftest test-empty-input
  (testing "Empty input returns empty token list"
    (let [tokens (tokenize "")]
      (is (= 0 (count tokens))))))

(deftest test-only-whitespace
  (testing "Only whitespace input returns empty token list"
    (let [tokens (tokenize "   \t   ")]
      (is (= 0 (count tokens))))))

(deftest test-only-comments
  (testing "Only comments input returns empty token list"
    (let [tokens (tokenize "# just a comment")]
      (is (= 0 (count tokens))))))

(deftest test-method-call-syntax
  (testing "Method call syntax tokenization"
    (let [tokens (tokenize "puts \"hello\"\narray.length")]
      (is (= 6 (count tokens)))
      (is (= "puts" (:value (first tokens))))
      (is (= "hello" (:value (second tokens))))
      (is (= :newline (:type (nth tokens 2))))
      (is (= "array" (:value (nth tokens 3))))
      (is (= "." (:value (nth tokens 4))))
      (is (= "length" (:value (nth tokens 5)))))))

(deftest test-tokenizer-error-handling
  (testing "Tokenizer throws meaningful errors for invalid input"

    ;; Test unterminated string with double quotes
    (is (thrown-with-msg? Exception #"Unterminated string literal"
          (tokenize "\"unterminated string")))

    ;; Test unterminated string with single quotes
    (is (thrown-with-msg? Exception #"Unterminated string literal"
          (tokenize "'unterminated string")))

    ;; Test unterminated string at end of input
    (is (thrown-with-msg? Exception #"Unterminated string literal"
          (tokenize "valid_token \"unterminated")))

    ;; Test unexpected character after !
    (is (thrown-with-msg? Exception #"Unexpected character.*!"
          (tokenize "!x")))

    ;; Test instance variable requires identifier
    (is (thrown-with-msg? Exception #"Expected identifier after @"
          (tokenize "@")))

    ;; Test error includes position information
    (try
      (tokenize "valid\n\"unterminated")
      (catch Exception e
        (let [data (ex-data e)]
          (is (contains? data :line))
          (is (contains? data :column))
          (is (= 2 (:line data)))
          (is (= 1 (:column data))))))

    ;; Test multiple error conditions don't interfere
    (is (thrown? Exception (tokenize "good \"bad")))
    (is (thrown? Exception (tokenize "\"good\" !bad")))
    (is (thrown? Exception (tokenize "\"good\" @")))))
