(ns puppetlabs.puppetserver.lockable-pool-test
  (:require [clojure.test :refer :all])
  (:import (com.puppetlabs.puppetserver.pool TwoSemaphoresLockablePool TwoQueuesLockablePool RRWLLockablePool ReentrantLockLockablePool)))

(defn create-empty-pool
  [size]
  ;; NOTE: uncomment one of the following two lines, to run the tests against
  ;; the preferred implementation.
  ;(TwoSemaphoresLockablePool. size)
  ;(TwoQueuesLockablePool. size)
  (RRWLLockablePool. size)
  ;(ReentrantLockLockablePool. size)
  )

(defn create-populated-pool
  [size]
  (let [pool (create-empty-pool size)]
    (dotimes [i size]
      (.register pool (str "foo" i)))
    pool))

(defn borrow-n-instances
  [pool n]
  (doall (for [_ (range n)]
           (.borrowItem pool))))

(defn return-instances
  [pool instances]
  (doseq [instance instances]
    (.returnItem pool instance)))

(deftest pool-lock-is-blocking-test
  (let [pool (create-populated-pool 3)
        instances (borrow-n-instances pool 3)
        future-started? (promise)
        lock-acquired? (promise)
        unlock? (promise)]
    (is (= 3 (count instances)))
    (let [lock-thread (future (do (deliver future-started? true)
                                  (.lock pool)
                                  (deliver lock-acquired? true)
                                  @unlock?
                                  (.unlock pool)))]
      @future-started?
      (testing "pool.lock() blocks until all instances are returned to the pool"
        (is (not (realized? lock-acquired?)))

        (testing "other threads may successfully return instances while pool.lock() is being executed"
          (.returnItem pool (first instances))
          (is (not (realized? lock-acquired?)))
          (.returnItem pool (second instances))
          (is (not (realized? lock-acquired?)))
          (.returnItem pool (nth instances 2)))

        @lock-acquired?
        (is (not (realized? lock-thread)))
        (deliver unlock? true)
        @lock-thread
        ;; make sure we got here
        (is (true? true)))

      (testing "borrows may be resumed after unlock()"
        (let [instance (.borrowItem pool)]
          (.returnItem pool instance))
        ;; make sure we got here
        (is (true? true))))))

(deftest pool-lock-blocks-borrows-test
  (testing "no other threads may borrow once pool.lock() has been invoked (before or after it returns)"
    (let [pool (create-populated-pool 2)
          instances (borrow-n-instances pool 2)
          lock-thread-started? (promise)
          lock-acquired? (promise)
          unlock? (promise)]
      (is (= 2 (count instances)))
      (let [lock-thread (future (do (deliver lock-thread-started? true)
                                    (.lock pool)
                                    (deliver lock-acquired? true)
                                    @unlock?
                                    (.unlock pool)))]
        @lock-thread-started?
        (is (not (realized? lock-acquired?)))
        (let [borrow-after-lock-requested-thread-started? (promise)
              borrow-after-lock-requested-instance-acquired? (promise)
              borrow-after-lock-requested-thread
              (future (do (deliver borrow-after-lock-requested-thread-started? true)
                          (let [instance (.borrowItem pool)]
                            (deliver borrow-after-lock-requested-instance-acquired? true)
                            (.returnItem pool instance))))]
          @borrow-after-lock-requested-thread-started?
          (is (not (realized? borrow-after-lock-requested-instance-acquired?)))

          (return-instances pool instances)
          @lock-acquired?
          (is (not (realized? borrow-after-lock-requested-instance-acquired?)))
          (is (not (realized? lock-thread)))

          (let [borrow-after-lock-acquired-thread-started? (promise)
                borrow-after-lock-acquired-instance-acquired? (promise)
                borrow-after-lock-acquired-thread
                (future (do (deliver borrow-after-lock-acquired-thread-started? (promise))
                            (let [instance (.borrowItem pool)]
                              (deliver borrow-after-lock-acquired-instance-acquired? true)
                              (.returnItem pool instance))))]
            @borrow-after-lock-acquired-thread-started?
            (is (not (realized? borrow-after-lock-acquired-instance-acquired?)))

            (deliver unlock? true)
            @lock-thread
            @borrow-after-lock-requested-instance-acquired?
            @borrow-after-lock-requested-thread
            @borrow-after-lock-acquired-instance-acquired?
            @borrow-after-lock-acquired-thread
            ;; just to assert that we got past the blocking calls
            (is (true? true))))))))

(deftest pool-lock-supersedes-existing-borrows-test
  (testing "if there are pending borrows when pool.lock() is called, they aren't fulfilled until after unlock()"
    (let [pool (create-populated-pool 2)
          instances (borrow-n-instances pool 2)
          blocked-borrow-thread-started? (promise)
          blocked-borrow-thread-borrowed? (promise)
          blocked-borrow-thread (future (do (deliver blocked-borrow-thread-started? true)
                                            (let [instance (.borrowItem pool)]
                                              (deliver blocked-borrow-thread-borrowed? true)
                                              (.returnItem pool instance))))
          lock-thread-started? (promise)
          lock-acquired? (promise)
          unlock? (promise)
          lock-thread (future (do (deliver lock-thread-started? true)
                                  (.lock pool)
                                  (deliver lock-acquired? true)
                                  @unlock?
                                  (.unlock pool)))]
      @blocked-borrow-thread-started?
      @lock-thread-started?
      (is (not (realized? blocked-borrow-thread-borrowed?)))
      (is (not (realized? lock-acquired?)))

      (return-instances pool instances)
      (is (not (realized? blocked-borrow-thread-borrowed?)))
      (is (not (realized? lock-thread)))
      @lock-acquired?

      (deliver unlock? true)
      @lock-thread
      @blocked-borrow-thread-borrowed?
      @blocked-borrow-thread

      ;; make sure we got here
      (is (true? true)))))

(deftest pool-lock-reentrant-test
  (testing "the thread that holds the pool lock may borrow instances while holding the lock"
    (let [pool (create-populated-pool 2)]
      (.lock pool)
      (is (true? true))
      (let [instance (.borrowItem pool)]
        (is (true? true))
        (.returnItem pool instance))
      (is (true? true))
      (.unlock pool))))

(deftest pool-lock-reentrant-with-many-borrows-test
  (testing "the thread that holds the pool lock may borrow instances while holding the lock, even with other borrows queued"
    (let [pool (create-populated-pool 2)]
      (.lock pool)
      (is (true? true))
      (let [borrow-thread-1 (future (do
                                      (let [instance (.borrowItem pool)]
                                        (.returnItem pool instance))))
            borrow-thread-2 (future (do
                                      (let [instance (.borrowItem pool)]
                                        (.returnItem pool instance))))]
        ;; this is racey, but the only ways i could think of to make it non-racey
        ;; depended on knowledge of the implementation
        (Thread/sleep 500)
        (is (not (realized? borrow-thread-1)))
        (is (not (realized? borrow-thread-2)))

        (let [instance (.borrowItem pool)]
          (is (true? true))
          (.returnItem pool instance))
        (is (true? true))
        (.unlock pool)
        @borrow-thread-1
        @borrow-thread-2))))

(deftest pool-lock-blocks-registration-test
  (testing "register blocks while the lock is held"
    (let [pool (create-empty-pool 2)
          register-thread-started? (promise)]
      (.register pool "foo")
      (.lock pool)
      (let [register-complete? (future (do (deliver register-thread-started? true)
                                           (.register pool "booyah")))]
        @register-thread-started?
        (is (not (realized? register-complete?)))
        (.unlock pool)
        @register-complete?
        ;; just making sure we get here
        (is (true? true))))))

;; TODO: test what happens if a 'returnItem' is executed while a registration is in progress?
;;  Only relevent for TwoQueues implementation.