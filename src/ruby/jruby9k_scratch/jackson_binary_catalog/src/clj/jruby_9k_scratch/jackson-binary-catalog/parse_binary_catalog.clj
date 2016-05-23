(ns jruby-9k-scratch.jackson-binary-catalog.parse-binary-catalog
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import (java.io FileReader FileInputStream FileOutputStream FileWriter)
           (org.apache.commons.io IOUtils)
           (org.apache.commons.lang StringUtils StringEscapeUtils)))

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
  [r s outstream]
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
                (when outstream
                  (println "writing quote to file")
                  (.write outstream 34)
                  (doseq [x (range i)]
                    (println "writing matched byte" x "to file:" (get bytes x))
                    (.write outstream (get bytes x)))
                  (.write outstream b))
                (inc i)))))))))

(defn find-quote-suffix
  [r start-ct suffix outstream]
  (loop [b (.read r)
         ct start-ct]
    (if (= b 34)
      (do
        (println "FOUND QUOTE BYTE COUNT: " ct b)
        (let [found-or-bytes-read (next-bytes-equal? r suffix outstream)]
          (if (= -1 found-or-bytes-read)
            (do (println "FOUND CONTENT STUFF" ct)
                (+ 1 ct (count suffix)))
            (let [next-byte (.read r)]
              (if-not (= -1 next-byte)
                (recur next-byte (+ 1 ct found-or-bytes-read)))))))
      (do
        (println "NO QUOTE COUNT: " ct b (String. (byte-array [b]) "UTF-8"))
        (if outstream
          (.write outstream b))
        (let [next-byte (.read r)]
          (if-not (= -1 next-byte)
            (recur next-byte (inc ct))))))))

(defn extract-image-from-binary-catalog-stream
  []
  (let [reader (FileInputStream. ^String binary-catalog-path)]
    (let [img-content-start (find-quote-suffix reader 0 "content\":\"" nil)
          fileoutput (FileOutputStream. "./foob.jpeg.raw")
          img-content-end (find-quote-suffix reader img-content-start "}}],\"edges\"" fileoutput)]
      (println "IMG START/END:" img-content-start img-content-end)
      (loop [b (.read reader)
             i img-content-end]
        (if-not (= -1 b)
          (do
           (println "READING OUT BYTES: " i b (String. (byte-array [b]) "UTF-8"))
           (recur (.read reader) (inc i)))))
      (.close fileoutput)))
  (let [reader (FileInputStream. "./foob.jpeg.raw")
        out (FileOutputStream. "./foob.jpeg")]
    (loop [b (.read reader)
           i 0]
      (println "READ BYTE" i "from raw jpeg file")
      (if-not (= -1 b)
        (do
          (println "not at end")
          (if (= 92 b)
            (do
              (println "Found a backslash, looking for a 'u'.")
              (let [next-byte (.read reader)]
                (if (= 117 next-byte)
                  (do
                    (println "Found a 'u', reading unicode char.")
                    (.write out (.getBytes
                                 (StringEscapeUtils/unescapeJava
                                  (String. (byte-array [92
                                                        117
                                                        (.read reader)
                                                        (.read reader)
                                                        (.read reader)
                                                        (.read reader)])))
                                 "UTF-8")))
                  (do
                    (println "Not a 'u', writing backslash and other byte")
                    (.write out (.getBytes
                                 (StringEscapeUtils/unescapeJava
                                  (String. (byte-array [92 next-byte])))))))))
            (do
              (println "not a backslash, writing byte")
              (.write out b)))
          (recur (.read reader) (inc i)))))
    (.close reader)
    (.close out)))

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

(println "FIN")