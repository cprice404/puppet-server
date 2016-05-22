(ns jruby-9k-scratch.jackson-binary-catalog.parse-binary-catalog
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def binary-catalog-path "./src/clj/jruby_9k_scratch/jackson-binary-catalog/binary_catalog.pson")

(defn parse-binary-catalog
  []
  (let [parsed (json/parse-stream (io/reader binary-catalog-path))]
    (println (json/generate-string parsed))))
