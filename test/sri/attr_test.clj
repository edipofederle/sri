(ns sri.attr-test
  (:require [clojure.test :refer :all]
            [sri.interpreter :as interpreter]
            [sri.core :as core]))

(deftest test-preprocess-attr-statements
  (testing "Preprocess attr_accessor statements"
    (let [source "class Test\n  attr_accessor :value, :left\n  def initialize(v)\n    @value = v\n  end\nend"
          result (interpreter/preprocess-attr-statements source)]
      (is (contains? result "Test"))
      (is (= 2 (count (get result "Test"))))
      (let [attrs (get result "Test")]
        (is (= "value" (:name (first attrs))))
        (is (= :accessor (:type (first attrs))))
        (is (= "left" (:name (second attrs))))
        (is (= :accessor (:type (second attrs)))))))

  (testing "Preprocess attr_reader statements"
    (let [source "class Test\n  attr_reader :height\nend"
          result (interpreter/preprocess-attr-statements source)]
      (is (contains? result "Test"))
      (let [attrs (get result "Test")]
        (is (= 1 (count attrs)))
        (is (= "height" (:name (first attrs))))
        (is (= :reader (:type (first attrs)))))))

  (testing "Preprocess attr_writer statements"
    (let [source "class Test\n  attr_writer :data\nend"
          result (interpreter/preprocess-attr-statements source)]
      (is (contains? result "Test"))
      (let [attrs (get result "Test")]
        (is (= 1 (count attrs)))
        (is (= "data" (:name (first attrs))))
        (is (= :writer (:type (first attrs)))))))

  (testing "Multiple classes with different attr types"
    (let [source "class A\n  attr_accessor :x\nend\nclass B\n  attr_reader :y\nend"
          result (interpreter/preprocess-attr-statements source)]
      (is (contains? result "A"))
      (is (contains? result "B"))
      (is (= :accessor (:type (first (get result "A")))))
      (is (= :reader (:type (first (get result "B")))))))

  (testing "Empty source"
    (let [result (interpreter/preprocess-attr-statements "")]
      (is (empty? result))))

  (testing "No attr statements"
    (let [source "class Test\n  def initialize\n  end\nend"
          result (interpreter/preprocess-attr-statements source)]
      (is (empty? result)))))

(deftest test-remove-attr-statements
  (testing "Remove attr_accessor statements"
    (let [source "class Test\n  attr_accessor :value\n  def initialize(v)\n    @value = v\n  end\nend"
          result (interpreter/remove-attr-statements source)
          expected "class Test\n  def initialize(v)\n    @value = v\n  end\nend"]
      (is (= expected result))))

  (testing "Remove multiple attr statements"
    (let [source "class Test\n  attr_reader :x\n  attr_writer :y\n  attr_accessor :z\n  def test\n  end\nend"
          result (interpreter/remove-attr-statements source)
          expected "class Test\n  def test\n  end\nend"]
      (is (= expected result))))

  (testing "No attr statements to remove"
    (let [source "class Test\n  def initialize\n  end\nend"
          result (interpreter/remove-attr-statements source)]
      (is (= source result)))))

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