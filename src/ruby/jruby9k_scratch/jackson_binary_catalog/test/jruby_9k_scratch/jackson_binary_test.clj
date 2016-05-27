(ns jruby-9k-scratch.jackson-binary-test
  (:require [clojure.test :refer :all])
  (:import (java.io FileInputStream InputStream ByteArrayInputStream)
           (org.apache.commons.io.output ByteArrayOutputStream)
           (puppetlabs.jackson.unencoded UnencodedInputStreamJsonMapper)
           (org.apache.commons.io IOUtils)
           (com.fasterxml.jackson.databind JsonMappingException)))

(defn roundtrip
  [test-file-name]
  (let [test-file (str "./dev-resources/jruby9k_scratch/jackson_binary_test/" test-file-name)
        instream (FileInputStream. test-file)
        bytes (IOUtils/toByteArray instream)
        byte-instream (ByteArrayInputStream. bytes)
        orig-map {"foo" "bar"
                  "file-contents" byte-instream}
        serialized-outstream (ByteArrayOutputStream.)
        mapper (UnencodedInputStreamJsonMapper.)
        _ (.writeValue mapper serialized-outstream orig-map)
        serialized-instream (.toInputStream serialized-outstream)
        deserialized (.readMapWithUnencodedInputStreams mapper serialized-instream)]
    {:input-bytes bytes
     :deserialized deserialized}))

(defn bytes-match?
  [orig-bytes roundtripped-stream]
  (= (seq orig-bytes) (seq (IOUtils/toByteArray roundtripped-stream))))

(deftest roundtrip-jpeg-in-map-test
  (testing "can roundtrip text from a single-line ascii file"
    (let [{:keys [deserialized input-bytes]} (roundtrip "single-line-ascii.txt")]
      (is (instance? InputStream (.get deserialized "foo")))
      (is (instance? InputStream (.get deserialized "file-contents")))
      (is (bytes-match? input-bytes (.get deserialized "file-contents")))))

  (testing "can roundtrip text from a multi-line ascii file"
    (let [{:keys [deserialized input-bytes]} (roundtrip "multi-line-ascii.txt")]
      (is (instance? InputStream (.get deserialized "foo")))
      (is (instance? InputStream (.get deserialized "file-contents")))
      (is (bytes-match? input-bytes (.get deserialized "file-contents")))))

  (testing "can't serialize a stream that contains unescaped quotes"
    (is (thrown-with-msg? JsonMappingException #"Derp!  You must escape any quote characters"
          (roundtrip "single-line-ascii-with-quote.txt"))))

  (testing "can't serialize a stream that contains unescaped quotes"
    (is (thrown-with-msg? JsonMappingException #"Derp!  You must escape any quote characters"
                          (roundtrip "foo.jpeg")))))
