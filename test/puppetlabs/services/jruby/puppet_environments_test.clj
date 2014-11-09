(ns puppetlabs.services.jruby.puppet-environments-test
  (:require [clojure.test :refer :all]
            [puppetlabs.services.jruby.puppet-environments :as puppet-env]
            [puppetlabs.puppetserver.bootstrap-testutils :as bootstrap]
            [puppetlabs.http.client.sync :as http-client]))

(defn stale?
  [reg env-name]
  (-> reg puppet-env/environment-state deref env-name :stale))

(deftest environment-registry-test
  (testing "environments are not stale by default"
    (let [reg (puppet-env/environment-registry)]
      (.registerEnvironment reg "foo")
      (is (false? (.isExpired reg "foo")))
      (is (false? (stale? reg :foo)))
      (.registerEnvironment reg "bar")
      (is (false? (.isExpired reg "foo")))
      (is (false? (stale? reg :foo)))
      (is (false? (.isExpired reg "bar")))
      (is (false? (stale? reg :bar)))))
  (testing "mark-all-environments-stale"
    (let [reg (puppet-env/environment-registry)]
      (.registerEnvironment reg "foo")
      (.registerEnvironment reg "bar")
      (is (false? (stale? reg :foo)))
      (is (false? (stale? reg :bar)))
      (puppet-env/mark-all-environments-stale reg)
      (is (true? (.isExpired reg "foo")))
      (is (true? (stale? reg :foo)))
      (is (true? (.isExpired reg "bar")))
      (is (true? (stale? reg :bar)))))
  (testing "removing and re-registering an environment clears staleness"
    (let [reg (puppet-env/environment-registry)]
      (.registerEnvironment reg "foo")
      (is (false? (.isExpired reg "foo")))
      (is (false? (stale? reg :foo)))
      (puppet-env/mark-all-environments-stale reg)
      (is (true? (.isExpired reg "foo")))
      (is (true? (stale? reg :foo)))
      (.removeEnvironment reg "foo")
      (.registerEnvironment reg "foo")
      (is (false? (.isExpired reg "foo")))
      (is (false? (stale? reg :foo))))))

(deftest ^:integration environment-flush-integration-test
  (testing "environments are flushed after marking stale"
    (println "bootstrapping")
    (bootstrap/with-puppetserver-running app {}
      (println "bootstrapped")
      (println (http-client/get "http://localhost:8140/productionnode/foo"))
      (is (= "1" "2")))
    (is (false? true))))


