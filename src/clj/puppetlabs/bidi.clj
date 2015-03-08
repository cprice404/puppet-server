(ns puppetlabs.bidi
  (:require [bidi.ring :as bidi-ring]
            [clojure.zip :as zip]
            [compojure.core :as compojure]))

(defmacro handler-fn
  [bindings body]
  `(fn [request#]
     (compojure/let-request [~bindings request#] ~@body)))

(defn update-route-info
  [route-info pattern]
  (cond
    (contains? #{:get :post :put :delete :head} pattern)
    (assoc-in route-info [:method] pattern)

    :else
    (update-in route-info [:path] concat (flatten [pattern]))))

(declare breadth-route-metadata*)

(defn depth-route-metadata*
  [routes route-info loc]
  (let [[pattern matched] (zip/node loc)]
    (cond
      (map? matched)
      (depth-route-metadata*
        routes
        route-info
        (-> loc zip/down zip/right (zip/edit #(into [] %)) zip/up))

      (vector? matched)
      (breadth-route-metadata*
        routes
        (update-route-info route-info pattern)
        (-> loc zip/down zip/right zip/down))

      :else
      (conj routes (update-route-info route-info pattern)))))

(defn breadth-route-metadata*
  [routes route-info loc]
  (loop [routes routes
         loc    loc]
    (let [routes (depth-route-metadata* routes route-info loc)]
      (if-let [next (zip/right loc)]
        (recur routes next)
        routes))))

(defn route-metadata
  [routes]
  (let [route-info {:path   []
                    :method :any}
        loc        (-> [routes] zip/vector-zip zip/down)]
    (breadth-route-metadata* [] route-info loc)))






(defn context->handler
  [context]
  (with-meta
    (bidi-ring/make-handler context)
    {:routes (route-metadata context)}))

(defn context [url-prefix & routes]
  [url-prefix (vec routes)])

(def context-handler (comp context->handler context))

(defmacro ANY
  [pattern bindings & body]
  `[~pattern (handler-fn ~bindings ~body)])

(defmacro GET
  [pattern bindings & body]
  `[~pattern {:get (handler-fn ~bindings ~body)}])

(defmacro PUT
  [pattern bindings & body]
  `[~pattern {:put (handler-fn ~bindings ~body)}])