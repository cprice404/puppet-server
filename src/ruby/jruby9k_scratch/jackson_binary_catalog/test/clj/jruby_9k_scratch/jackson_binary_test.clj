(ns jruby-9k-scratch.jackson-binary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import (java.io FileInputStream InputStream ByteArrayInputStream)
           (org.apache.commons.io.output ByteArrayOutputStream)
           (puppetlabs.jackson.unencoded JackedSonMapper)
           (org.apache.commons.io IOUtils)
           (com.fasterxml.jackson.databind JsonMappingException)
           (jruby_9k_scratch EscapedQuoteInputStream UnescapedQuoteInputStream)))

(defn roundtrip
  ([test-file-name]
   (roundtrip test-file-name identity))
  ([test-file-name instream-wrap-fn]
   (let [test-file (str "./dev-resources/jruby9k_scratch/jackson_binary_test/" test-file-name)
         instream (FileInputStream. test-file)
         bytes (IOUtils/toByteArray instream)
         byte-instream (instream-wrap-fn (ByteArrayInputStream. bytes))
         orig-map {"foo" "bar"
                   "file-contents" byte-instream}
         serialized-outstream (ByteArrayOutputStream.)
         mapper (JackedSonMapper.)
         _ (.writeValue mapper serialized-outstream orig-map)
         serialized-instream (.toInputStream serialized-outstream)
         deserialized (.readMapWithUnencodedInputStreams mapper serialized-instream)]
     {:input-bytes bytes
      :deserialized deserialized})))

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

  (testing "can roundtrip text from a single-line ascii file with an escaped quote"
    (let [{:keys [deserialized input-bytes]} (roundtrip "single-line-ascii-with-escaped-quote.txt")]
      (is (instance? InputStream (.get deserialized "foo")))
      (is (instance? InputStream (.get deserialized "file-contents")))
      (is (bytes-match? input-bytes (.get deserialized "file-contents")))))

  (testing "can roundtrip text with unescaped quote, when using escaped stream"
    (let [{:keys [deserialized input-bytes]} (roundtrip "single-line-ascii-with-quote.txt"
                                                        (fn [is] (EscapedQuoteInputStream. is)))]
      (is (instance? InputStream (.get deserialized "foo")))
      (is (instance? InputStream (.get deserialized "file-contents")))
      (is (bytes-match? input-bytes (UnescapedQuoteInputStream. (.get deserialized "file-contents"))))))

  (testing "can't read a jpeg file that happens to contain an unescaped quote char"
    (is (thrown-with-msg? JsonMappingException #"Derp!  You must escape any quote characters"
                          (roundtrip "foo.jpeg"))))

  (testing "can roundtrip a jpeg file that happens to contain an unescaped quote char, when using escaped stream"
    (let [{:keys [deserialized input-bytes]} (roundtrip "foo.jpeg"
                                                        (fn [is] (EscapedQuoteInputStream. is)))]
      (is (instance? InputStream (.get deserialized "foo")))
      (is (instance? InputStream (.get deserialized "file-contents")))
      ;(io/copy (UnescapedQuoteInputStream. (.get deserialized "file-contents")) (io/file "./FOOCOPY.jpeg") )
      (is (bytes-match? input-bytes (UnescapedQuoteInputStream. (.get deserialized "file-contents")))))))
