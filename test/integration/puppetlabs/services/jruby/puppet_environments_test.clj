(ns puppetlabs.services.jruby.puppet-environments-test
  (:require [clojure.test :refer :all]
            [puppetlabs.services.jruby.puppet-environments :as puppet-env]
            [puppetlabs.puppetserver.bootstrap-testutils :as bootstrap]
            [puppetlabs.http.client.sync :as http-client]
            [puppetlabs.services.jruby.jruby-testutils :as jruby-testutils]
            [me.raynes.fs :as fs]))

(use-fixtures :once
              (jruby-testutils/with-puppet-conf
                "./dev-resources/puppetlabs/services/jruby/puppet_environments_test/puppet.conf"))

(defn pem-file
  [& args]
  (str (apply fs/file bootstrap/master-conf-dir "ssl" args)))

(def ca-cert
  (pem-file "certs" "ca.pem"))

(def localhost-cert
  (pem-file "certs" "localhost.pem"))

(def localhost-key
  (pem-file "private_keys" "localhost.pem"))

(def request-options
  {:ssl-cert      localhost-cert
   :ssl-key       localhost-key
   :ssl-ca-cert   ca-cert
   :headers       {"Accept" "pson"}
   :as            :text})

(deftest ^:integration environment-flush-integration-test
  (testing "environments are flushed after marking stale"
    (println "bootstrapping")
    (bootstrap/with-puppetserver-running app {}
      (println "bootstrapped")
      (println
        (http-client/get
          "https://localhost:8140/production/catalog/localhost"
          request-options))
      (is (= "1" "2")))
    (is (false? true))))


