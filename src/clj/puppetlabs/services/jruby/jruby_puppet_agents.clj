(ns puppetlabs.services.jruby.jruby-puppet-agents
  (:import (clojure.lang IFn Agent)
           (com.puppetlabs.puppetserver PuppetProfiler)
           (puppetlabs.services.jruby.jruby_puppet_core PoisonPill))
  (:require [schema.core :as schema]
            [puppetlabs.services.jruby.jruby-puppet-core :as jruby-core]
            [clojure.tools.logging :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def JRubyPoolAgent
  "An agent configured for use in managing JRuby pools"
  (schema/both Agent
               (schema/pred
                 (fn [a]
                   (let [state @a]
                     (and
                       (map? state)
                       (ifn? (:shutdown-on-error state))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

(schema/defn ^:always-validate
  send-agent :- JRubyPoolAgent
  "Utility function; given a JRubyPoolAgent, send the specified function.
  Ensures that the function call is wrapped in a `shutdown-on-error`."
  [jruby-agent :- JRubyPoolAgent
   f :- IFn]
  (letfn [(agent-fn [agent-ctxt]
                    (let [shutdown-on-error (:shutdown-on-error agent-ctxt)]
                      (shutdown-on-error f))
                    agent-ctxt)]
    (send jruby-agent agent-fn)))

#_(schema/defn ^:always-validate
  flush-pool!
  "Given the current PoolContext and a reference to a new pool that is being
  populated by the prime-pool-agent, swap the new pool into the PoolContext and
  clean up the old pool.  NOTE: this function should never be called except by
  the flush-pool-agent."
  [pool-context :- jruby-core/PoolContext
   new-pool :- jruby-core/PoolState]
  (let [pool-state (:pool-state pool-context)
        old-pool    @pool-state]
    (log/info "Waiting for new JRuby pool to become ready for use.")
    (jruby-core/with-jruby-puppet jruby-puppet (:pool new-pool)
      (log/info "New JRuby pool is ready for use."))
    (reset! pool-state new-pool)
    (log/info "Swapped JRuby pools, beginning cleanup of old pool.")
    (doseq [i (range (:size old-pool))]
      (let [id (inc i)
            instance (jruby-core/borrow-from-pool (:pool old-pool))]
        (.terminate (:scripting-container instance))
        (log/info "Cleaned up old JRuby instance" id "of" (:size old-pool))))))

(schema/defn ^:always-validate
  flush-pool!
  "Flush of the current JRuby pool.  NOTE: this function should never
  be called except by the pool-agent."
  [pool-context :- jruby-core/PoolContext]
  ;; Since this function is only called by the pool-agent, and since this
  ;; is the only entry point into the pool flushing code that is exposed by the
  ;; service API, we know that if we receive multiple flush requests before the
  ;; first one finishes, they will be queued up and the body of this function
  ;; will be executed atomically; we don't need to worry about race conditions
  ;; between the steps we perform here in the body.
  (log/info "Flush request received; creating new JRuby pool.")
  (let [{:keys [config profiler pool-state]} pool-context
        new-pool-state (jruby-core/create-pool-from-config config)
        new-pool #spy/d (:pool @new-pool-state)
        old-pool @pool-state
        count    (:size old-pool)]
    (log/info "Replacing old JRuby pool with new instance.")
    (reset! pool-state new-pool-atom)
    (log/info "Swapped JRuby pools, beginning cleanup of old pool.")
    (doseq [i (range count)]
      (let [id (inc i)
            instance (jruby-core/borrow-from-pool (:pool old-pool))]
        (.terminate (:scripting-container instance))
        (log/infof "Cleaned up old JRuby instance %s of %s, creating replacement."
                   id count)
        (try
          (jruby-core/create-pool-instance! #spy/d new-pool id config profiler)
          (log/infof "Finished creating JRubyPuppet instance %d of %d"
                     id count)
          (catch Exception e
            (.clear new-pool)
            (.put new-pool (PoisonPill. e))
            (throw (IllegalStateException. "There was a problem adding a JRubyPuppet instance to the pool." e))))))


    ;; Tell the prime-pool-agent to start priming the new pool
    #_(send-agent pool-agent #(jruby-core/prime-pool! new-pool config profiler))
    ;; Tell the flush-pool-agent (which is the agent that is currently executing
    ;; this function) that it should swap in the new pool and flush the old one
    ;; as soon as everything is ready.  Because agents are guaranteed to
    ;; execute their actions in the order that they are 'sent', we know that
    ;; even if multiple flushes are queued up, the final action that the agent
    ;; will perform is this `flush-pool!` operation for the most recent flush
    ;; request.
    #_(send-agent flush-agent #(flush-pool! pool-context @new-pool))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate
  pool-agent :- JRubyPoolAgent
  "Given a shutdown-on-error function, create an agent suitable for use in managing
  JRuby pools."
  [shutdown-on-error-fn :- IFn]
  (agent {:shutdown-on-error shutdown-on-error-fn}))

(schema/defn ^:always-validate
  send-prime-pool! :- JRubyPoolAgent
  "Sends a request to the agent to prime the pool using the given pool context."
  [pool-context :- jruby-core/PoolContext
   pool-agent :- JRubyPoolAgent]
  (let [{:keys [pool-state config profiler]} pool-context]
    (send-agent pool-agent #(jruby-core/prime-pool! pool-state config profiler))))

(schema/defn ^:always-validate
  send-flush-pool! :- JRubyPoolAgent
  "Sends requests to the agent to flush the existing pool and create a new one."
  [pool-context :- jruby-core/PoolContext
   pool-agent :- JRubyPoolAgent]
  (send-agent pool-agent #(flush-pool! pool-context)))
