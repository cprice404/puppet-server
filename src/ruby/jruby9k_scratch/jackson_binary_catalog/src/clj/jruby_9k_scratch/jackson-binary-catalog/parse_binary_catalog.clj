(ns jruby-9k-scratch.jackson-binary-catalog.parse-binary-catalog
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import (java.io FileReader FileInputStream)))

(def binary-catalog-path "binary_catalog.pson")

(defn img-file-resource?
  [resource]
  (println "CHECKING RESOURCE" resource)
  (if (and (= "File" (get resource "type"))
           (= "/tmp/foo.jpeg" (get resource "title")))
    resource))

(defn parse-binary-catalog
  []
  (let [parsed (json/parse-stream (io/reader binary-catalog-path))
        serialized (json/generate-string parsed)
        roundtripped (json/parse-string serialized)
        file-resource (-> roundtripped
                          (get "resources")
                          (#(some img-file-resource? %)))
        img-content (get-in file-resource ["parameters" "content"])]
    (println img-content)
    (spit "./foo.jpeg" img-content)))

(defn next-bytes-equal?
  [r s]
  (let [bytes (.getBytes s "UTF-8")]
    (loop [i 0]
      (println "looking for byte " i "/" (count bytes) " : " (get bytes i))
      (if (= (count bytes) i)
        (do
          (println "found all chars, returning true")
          -1)
        (do
          (let [b (.read r)]
            (println "Read byte: " b)
            (if (= (get bytes i) b)
              (do
                (println "match! recurring.")
                (recur (inc i)))
              (do
                (println "no match, bailing.")
                (inc i)))))))))

(defn find-quote-suffix
  [r start-ct suffix]
  (loop [b (.read r)
         ct start-ct]
    (if (= b 34)
      (do
        (println "FOUND QUOTE BYTE COUNT: " ct b)
        (let [found-or-bytes-read (next-bytes-equal? r suffix)]
          (if (= -1 found-or-bytes-read)
            (do (println "FOUND CONTENT STUFF" ct)
                (+ 1 ct (count suffix)))
            (let [next-byte (.read r)]
              (if-not (= -1 next-byte)
                (if (< ct 5000)
                  (recur next-byte (+ 1 ct found-or-bytes-read))))))))
      (do
        (println "NO QUOTE COUNT: " ct b (String. (byte-array [b]) "UTF-8"))
        (let [next-byte (.read r)]
          (if-not (= -1 next-byte)
            (if (< ct 5000)
              (recur next-byte (inc ct)))))))))

(defn extract-image-from-binary-catalog-stream
  []
  (let [reader (FileInputStream. ^String binary-catalog-path)]
    (let [img-content-start (find-quote-suffix reader 0 "content\":\"")
          img-content-end (find-quote-suffix reader img-content-start "}}],\"edges\"")]
      (println "IMG START/END:" img-content-start img-content-end)
      (loop [b (.read reader)
             i img-content-end]
        (if-not (= -1 b)
          (do
           (println "READING OUT BYTES: " i b (String. (byte-array [b]) "UTF-8"))
           (recur (.read reader) (inc i))))))))

(defn count-bytes-in-file
  []
  (let [reader (FileInputStream. ^String binary-catalog-path)]
    (loop [b (.read reader)
           ct 0]
      (if-not (= -1 b)
        (do
          (println "read byte #" ct (String. (byte-array [b]) "UTF-8"))
          (recur (.read reader) (inc ct)))))))

(extract-image-from-binary-catalog-stream)
#_(count-bytes-in-file)