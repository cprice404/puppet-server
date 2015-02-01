(ns puppetlabs.services.jruby.jruby-pool-test
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.test :refer :all]
            [puppetlabs.kitchensink.core :as ks]
            [puppetlabs.services.jruby.jruby-puppet-core :refer :all :as core]
            [puppetlabs.services.jruby.jruby-testutils :as jruby-testutils]
            [puppetlabs.services.jruby.jruby-puppet-agents :as jruby-agents]
            [puppetlabs.services.jruby.jruby-puppet-core :as jruby-core]))

(use-fixtures :each jruby-testutils/mock-pool-instance-fixture)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests

(deftest configuration-validation
  (testing "malformed configuration fails"
    (let [malformed-config {:illegal-key [1 2 3]}]
      (is (thrown-with-msg? ExceptionInfo
                            #"Input to create-pool-from-config does not match schema"
                            (create-pool-context malformed-config nil)))))
  (let [minimal-config {:jruby-puppet {:gem-home "/dev/null"}
                        :os-settings  {:ruby-load-path ["/dev/null"]}}
        config        (jruby-core/initialize-config minimal-config)]
    (testing "max-active-instances is set to default if not specified"
      (is (= jruby-core/default-pool-size (:max-active-instances config))))
    (testing "max-requests-per-instance is set to 0 if not specified"
      (is (= 0 (:max-requests-per-instance config))))
    (testing "max-requests-per-instance is honored if specified"
      (is (= 5 (-> minimal-config
                   (assoc-in [:jruby-puppet :max-requests-per-instance] 5)
                   (jruby-core/initialize-config)
                   :max-requests-per-instance))))))

(deftest test-jruby-service-core-funcs
  (let [pool-size        2
        config           (jruby-testutils/jruby-puppet-config pool-size)
        profiler         jruby-testutils/default-profiler
        pool-context     (create-pool-context config profiler)
        pool             (get-pool pool-context)]

    (testing "The pool should not yet be full as it is being primed in the
             background."
      (is (= (free-instance-count pool) 0)))

    (jruby-agents/prime-pool! (:pool-state pool-context) config profiler)

    (testing "Borrowing all instances from a pool while it is being primed and
             returning them."
      (let [all-the-jrubys (jruby-testutils/drain-pool pool-context pool-size)]
        (is (= 0 (free-instance-count pool)))
        (doseq [instance all-the-jrubys]
          (is (not (nil? instance)) "One of JRubyPuppet instances is nil"))
        (jruby-testutils/fill-drained-pool all-the-jrubys)
        (is (= pool-size (free-instance-count pool)))))

    (testing "Borrowing from an empty pool with a timeout returns nil within the
             proper amount of time."
      (let [timeout              250
            all-the-jrubys       (jruby-testutils/drain-pool pool-context pool-size)
            test-start-in-millis (System/currentTimeMillis)]
        (is (nil? (borrow-from-pool-with-timeout pool timeout)))
        (is (>= (- (System/currentTimeMillis) test-start-in-millis) timeout)
            "The timeout value was not honored.")
        (jruby-testutils/fill-drained-pool all-the-jrubys)
        (is (= (free-instance-count pool) pool-size)
            "All JRubyPuppet instances were not returned to the pool.")))

    (testing "Removing an instance decrements the pool size by 1."
      (let [jruby-instance (borrow-from-pool pool)]
        (is (= (free-instance-count pool) (dec pool-size)))
        (return-to-pool jruby-instance)))

    (testing "Borrowing an instance increments its request count."
      (let [drain-via   (fn [borrow-fn] (doall (repeatedly pool-size borrow-fn)))
            assoc-count (fn [acc jruby]
                          (assoc acc (:id jruby)
                                     (:request-count @(:state jruby))))
            get-counts  (fn [jrubies] (reduce assoc-count {} jrubies))]
        (doseq [drain-fn [#(jruby-core/borrow-from-pool pool)
                          #(jruby-core/borrow-from-pool-with-timeout pool 20000)]]
          (let [jrubies (drain-via drain-fn)
                counts  (get-counts jrubies)]
            (jruby-testutils/fill-drained-pool jrubies)
            (let [jrubies    (drain-via drain-fn)
                  new-counts (get-counts jrubies)]
              (jruby-testutils/fill-drained-pool jrubies)
              (is (= (ks/keyset counts) (ks/keyset new-counts)))
              (doseq [k (keys counts)]
                (is (= (inc (counts k)) (new-counts k)))))))))))

(deftest prime-pools-failure
  (let [pool-size 2
        config        (jruby-testutils/jruby-puppet-config pool-size)
        profiler      jruby-testutils/default-profiler
        pool-context  (create-pool-context config profiler
                                           (jruby-agents ))
        pool          (get-pool pool-context)
        err-msg       (re-pattern "Unable to borrow JRuby instance from pool")]
    (with-redefs [core/create-pool-instance! (fn [_] (throw (IllegalStateException. "BORK!")))]
                 (is (thrown? IllegalStateException
                              (jruby-agents/prime-pool!
                                (:pool-state pool-context)
                                config
                                profiler))))
    (testing "borrow and borrow-with-timeout both throw an exception if the pool failed to initialize"
      (is (thrown-with-msg? IllegalStateException
            err-msg
            (borrow-from-pool pool pool-context)))
      (is (thrown-with-msg? IllegalStateException
            err-msg
            (borrow-from-pool-with-timeout pool pool-context 120))))
    (testing "borrow and borrow-with-timeout both continue to throw exceptions on subsequent calls"
      (is (thrown-with-msg? IllegalStateException
          err-msg
          (borrow-from-pool pool pool-context)))
      (is (thrown-with-msg? IllegalStateException
          err-msg
          (borrow-from-pool-with-timeout pool pool-context 120))))))

(defn create-pool
  [max-requests]
  (let [config (-> (jruby-testutils/jruby-puppet-config 1)
                   (assoc :max-requests-per-instance max-requests))
        profiler jruby-testutils/default-profiler
        pool-context (create-pool-context config profiler)]
    (jruby-agents/prime-pool! (:pool-state pool-context) config profiler)
    (get-pool pool-context)))

(deftest flush-jruby-after-max-requests
  (testing "JRuby instance is not flushed if it has not exceeded max requests"
    (let [pool      (create-pool 2)
          instance  (borrow-from-pool pool)
          id        (:id instance)]
      (return-to-pool instance)
      (let [instance (borrow-from-pool pool)]
        (is (= id (:id instance))))))
  (testing "JRuby instance is flushed after exceeding max requests"
    (let [pool      (create-pool 1)
          instance  (borrow-from-pool pool)
          id        (:id instance)]
      (return-to-pool instance)
      (let [instance (borrow-from-pool pool)]
        (is (not= id (:id instance))))))
  (testing "JRuby instance is not flushed if max requests setting is set to 0"
    (let [pool      (create-pool 0)
          instance  (borrow-from-pool pool)
          id        (:id instance)]
      (return-to-pool instance)
      (let [instance (borrow-from-pool pool)]
        (is (= id (:id instance)))))))

