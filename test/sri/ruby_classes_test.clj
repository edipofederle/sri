(ns sri.ruby-classes-test
  (:require [clojure.test :refer :all]
            [sri.ruby-classes-new :as rc]))

(deftest basic-object-test
  (testing "BasicObject creation and basic methods"
    (let [obj (rc/create-basic-object)]
      (is (= "BasicObject" (rc/ruby-class obj)))
      (is (= ["BasicObject"] (rc/ruby-ancestors obj)))
      (is (= "#<BasicObject>" (rc/to-s obj)))
      (is (= "#<BasicObject>" (rc/inspect obj)))
      (is (rc/respond-to? obj :to_s))
      (is (rc/respond-to? obj :inspect))
      (is (rc/respond-to? obj :==))
      (is (not (rc/respond-to? obj :nonexistent))))))

(deftest object-test
  (testing "Object creation and methods"
    (let [obj (rc/create-object)]
      (is (= "Object" (rc/ruby-class obj)))
      (is (= ["Object" "Kernel" "BasicObject"] (rc/ruby-ancestors obj)))
      (is (rc/respond-to? obj :to_s))
      (is (rc/respond-to? obj :class))
      (is (rc/respond-to? obj :nil?))
      (is (rc/respond-to? obj :puts))))
  
  (testing "Object with value"
    (let [obj (rc/create-object "hello")]
      (is (= "hello" (rc/to-s obj)))
      (is (= "Object" (rc/ruby-class obj)))
      (is (not (rc/ruby-eq obj (rc/create-object "world"))))
      (is (rc/ruby-eq obj (rc/create-object "hello"))))))

(deftest method-lookup-test
  (testing "Method registry and lookup"
    (let [obj (rc/create-object "test")]
      (is (rc/has-method? "Object" :to_s))
      (is (rc/has-method? "BasicObject" :inspect))
      (is (not (rc/has-method? "Object" :nonexistent)))
      (is (some? (rc/method-lookup obj :to_s)))
      (is (some? (rc/method-lookup obj :class)))
      (is (nil? (rc/method-lookup obj :nonexistent))))))

(deftest method-call-test
  (testing "Ruby method invocation"
    (let [obj (rc/create-object "hello")]
      (is (= "hello" (rc/invoke-ruby-method obj :to_s)))
      (is (= "Object" (rc/invoke-ruby-method obj :class)))
      (is (thrown? Exception (rc/invoke-ruby-method obj :nonexistent))))))

(deftest equality-test
  (testing "Ruby equality semantics"
    (let [obj1 (rc/create-object "test")
          obj2 (rc/create-object "test")
          obj3 obj1]
      (is (rc/ruby-eq obj1 obj2)) ; same value
      (is (not (identical? obj1 obj2))) ; different objects
      (is (identical? obj1 obj3)) ; same object reference
      (is (rc/invoke-ruby-method obj1 :== obj2))
      (is (not (rc/invoke-ruby-method obj1 :equal? obj2)))
      (is (rc/invoke-ruby-method obj1 :equal? obj3)))))

(deftest nil-true-false-test
  (testing "Ruby nil, true, false objects"
    (let [nil-obj (rc/ruby-nil)
          true-obj (rc/ruby-true)
          false-obj (rc/ruby-false)]
      (is (rc/invoke-ruby-method nil-obj :nil?))
      (is (not (rc/invoke-ruby-method true-obj :nil?)))
      (is (not (rc/invoke-ruby-method false-obj :nil?)))
      (is (= "Object" (rc/ruby-class nil-obj)))
      (is (= "Object" (rc/ruby-class true-obj)))
      (is (= "Object" (rc/ruby-class false-obj))))))

(deftest ancestor-chain-test
  (testing "Ancestor chain and method resolution"
    (let [basic-obj (rc/create-basic-object)
          obj (rc/create-object)]
      (is (= 1 (count (rc/ruby-ancestors basic-obj))))
      (is (= 3 (count (rc/ruby-ancestors obj))))
      (is (contains? (set (rc/ruby-ancestors obj)) "BasicObject"))
      (is (contains? (set (rc/ruby-ancestors obj)) "Kernel"))
      (is (contains? (set (rc/ruby-ancestors obj)) "Object")))))

(deftest debug-introspection-test
  (testing "Debug and introspection functions"
    (is (map? (rc/debug-method-registry)))
    (is (seq (rc/class-methods "Object")))
    (is (seq (rc/class-methods "BasicObject")))
    (is (contains? (set (rc/all-ruby-classes)) "Object"))
    (is (contains? (set (rc/all-ruby-classes)) "BasicObject"))))

(deftest protocol-implementation-test
  (testing "Protocol implementations"
    (let [obj (rc/create-object "test")]
      (is (satisfies? rc/RubyObject obj))
      (is (satisfies? rc/RubyInspectable obj))
      (is (satisfies? rc/RubyComparable obj))
      (is (= "Object" (rc/ruby-class obj)))
      (is (vector? (rc/ruby-ancestors obj)))
      (is (boolean? (rc/respond-to? obj :to_s))))))

(run-tests)