(defproject puppetlabs.scratch.jruby9k/jackson-binary-catalog "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.6.1"]
                 [commons-io "2.5"]
                 [commons-lang "2.6"]
                 [com.fasterxml.jackson.core/jackson-databind "2.7.3"]

                 [org.jruby/jruby-core "9.1.1.0"]
                 [org.jruby/jruby-stdlib "9.1.1.0"]

                 [org.slf4j/slf4j-api "1.7.13"]
                 [ch.qos.logback/logback-classic "1.1.3"]]

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj"]


  :profiles {:dev {:source-paths  ["dev"]
                   :java-source-paths ["test/java"]}}

  )
