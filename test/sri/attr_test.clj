(ns sri.attr-test
  (:require [clojure.test :refer :all]
            [sri.parser :as parser]
            [sri.tokenizer :as tokenizer]
            [sri.core :as core]))

(defn parse-statement-string [source]
  "Helper function to parse a source string and return AST and root entity."
  (let [ast (parser/parse (tokenizer/tokenize source))
        root-entity (parser/find-root-entity ast)]
    [ast root-entity]))

(deftest test-attr-statement-parsing
  (testing "Parse attr_accessor statement"
    (let [[ast root-id] (parse-statement-string "attr_accessor :value, :left, :right")
          [program-ast program-root] (parse-statement-string "class Test\n  attr_accessor :value, :left\nend")
          class-children (parser/get-children program-ast program-root)
          class-id (first class-children)
          class-body-id (parser/get-component program-ast class-id :body)
          attr-statements (parser/get-children program-ast class-body-id)
          attr-id (first attr-statements)]
      (is (= :attr-accessor-statement (parser/get-component program-ast attr-id :node-type)))
      (let [attributes (parser/get-component program-ast attr-id :attributes)]
        (is (= ["value" "left"] attributes)))))

  (testing "Parse attr_reader statement"
    (let [[ast root-id] (parse-statement-string "class Test\n  attr_reader :height\nend")
          class-children (parser/get-children ast root-id)
          class-id (first class-children)
          class-body-id (parser/get-component ast class-id :body)
          attr-statements (parser/get-children ast class-body-id)
          attr-id (first attr-statements)]
      (is (= :attr-reader-statement (parser/get-component ast attr-id :node-type)))
      (let [attributes (parser/get-component ast attr-id :attributes)]
        (is (= ["height"] attributes)))))

  (testing "Parse attr_writer statement"
    (let [[ast root-id] (parse-statement-string "class Test\n  attr_writer :data\nend")
          class-children (parser/get-children ast root-id)
          class-id (first class-children)
          class-body-id (parser/get-component ast class-id :body)
          attr-statements (parser/get-children ast class-body-id)
          attr-id (first attr-statements)]
      (is (= :attr-writer-statement (parser/get-component ast attr-id :node-type)))
      (let [attributes (parser/get-component ast attr-id :attributes)]
        (is (= ["data"] attributes))))))

(deftest test-attr-integration
  (testing "Full attr_accessor integration"
    (let [source "class Person\n  attr_accessor :name\n  def initialize(n)\n    @name = n\n  end\nend\nperson = Person.new(\"Alice\")\nputs(person.name)"
          result (with-out-str (core/eval-string source))]
      (is (= "Alice\n" result))))

  (testing "Attr setter integration"
    (let [source "class Counter\n  attr_accessor :value\n  def initialize\n    @value = 0\n  end\nend\nc = Counter.new\nc.value = 42\nputs(c.value)"
          result (with-out-str (core/eval-string source))]
      (is (= "42\n" result))))

  (testing "Multiple attr types"
    (let [source "class Test\n  attr_reader :x\n  attr_writer :y\n  attr_accessor :z\n  def initialize\n    @x = 1\n    @y = 2\n    @z = 3\n  end\nend\nt = Test.new\nputs(t.x)\nt.y = 99\nputs(t.z)\nt.z = 88\nputs(t.z)"
          result (with-out-str (core/eval-string source))]
      (is (= "1\n3\n88\n" result)))))