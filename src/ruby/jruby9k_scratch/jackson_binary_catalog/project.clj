(defproject puppetlabs.scratch.jruby9k/jackson-binary-catalog "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.6.1"]]

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  :profiles {:dev {:source-paths  ["dev"]}}

  )
