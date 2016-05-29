(ns jruby-9k-scratch.pson-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import (org.jruby.embed ScriptingContainer)
           (org.jruby RubyInstanceConfig$CompileMode CompatVersion RubyString RubyHash RubyArray)))

(defn create-scripting-container
  []
  (let [sc (ScriptingContainer.)]
    (.setCompatVersion sc CompatVersion/RUBY1_9)
    (.setCompileMode sc RubyInstanceConfig$CompileMode/OFF)
    (.setLoadPaths sc ["../../../../ruby/puppet/lib"])
    (let [pson (.runScriptlet
                sc
                (str "require 'puppet/external/pson/pure'\n"
                     "PSON"))
          converter (.runScriptlet
                     sc
                     (str "class Converter\n"
                          "   def self.to_a(a)\n"
                          "      a.to_a\n"
                          "   end\n"
                          "\n"
                          "   def self.to_h(h)\n"
                          "      h.to_h\n"
                          "   end\n"
                          "\n"
                          "   def self.read_file(f)\n"
                          "      File.new(f).read\n"
                          "   end\n"
                          "end\n"
                          "Converter\n"))]
      {:sc sc
       :pson pson
       :converter converter})))

(def scripting-container
  (memoize create-scripting-container))

(defn to-a
  [a]
  (let [{:keys [sc converter]} (scripting-container)]
    (.callMethod sc converter "to_a" a RubyArray)))

(defn to-h
  [h]
  (let [{:keys [sc converter]} (scripting-container)]
    (.callMethod sc converter "to_h" h RubyHash)))

(defn ruby-read-file
  [f]
  (let [{:keys [sc converter]} (scripting-container)
        filepath (str "dev-resources/jruby9k_scratch/pson_test/" f)]
    (.callMethod sc converter "read_file" filepath RubyString)))

(defn to-pson
  [x]
  (let [{:keys [sc pson]} (scripting-container)]
    (.callMethod sc pson "generate" x RubyString)))

(defn from-pson
  [rs]
  (let [{:keys [sc pson]} (scripting-container)]
    (.callMethod sc pson "parse" rs Object)))

(deftest pson-roundtrip
  (testing "Can roundtrip a simple array w/pson"
    (let [a (to-a ["funky", "town"])
          serialized (to-pson a)
          deserialized (from-pson serialized)]
      (is (= a deserialized))))

  (testing "Can roundtrip a simple map w/pson"
    (let [m (to-h {"foo" "fooval"
                   "bar" "barval"})
          serialized (to-pson m)
          deserialized (from-pson serialized)]
      (is (= m deserialized))))

  (testing "Can roundtrip an array with jpeg contents w/pson"
    (let [m (to-a [(ruby-read-file "foo.jpeg")])
          serialized (to-pson m)
          _ (io/copy (.getBytes serialized) (io/file "./target/jpeg-serialized-as-array.pson"))
          deserialized (from-pson serialized)]
      (is (= m deserialized)))))
