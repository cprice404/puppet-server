(ns puppetlabs.bidi
  (:require [bidi.ring :as bidi-ring]
            [bidi.bidi :as bidi]
            [clojure.zip :as zip]
            [compojure.core :as compojure]
            [compojure.response :as response]
            [ring.util.response :as ring-response]))

(defmacro handler-fn
  [bindings body]
  `(fn [request#]
     (response/render
       (compojure/let-request [~bindings request#] ~@body)
       request#)))

(defn route-with-method
  [method pattern bindings body]
  `[~pattern {~method (handler-fn ~bindings ~body)}])

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






(defn routes
  [& routes]
  ["" (vec routes)])

(defn context->handler
  [context]
  (with-meta
    (bidi-ring/make-handler (bidi/compile-route context))
    {:routes (route-metadata context)}))

(defn context [url-prefix & routes]
  [url-prefix (vec routes)])

(defn context-handler
  [url-prefix & routes]
  (context->handler
    (apply context url-prefix routes)))


(defn not-found
  [body]
  [[#".*" :rest] (fn [request]
                   (-> (response/render body request)
                       (ring-response/status 404)))])


(defmacro ANY
  [pattern bindings & body]
  `[~pattern (handler-fn ~bindings ~body)])

(defmacro GET
  [pattern bindings & body]
  (route-with-method :get pattern bindings body))

(defmacro HEAD
  [pattern bindings & body]
  (route-with-method :head pattern bindings body))

(defmacro PUT
  [pattern bindings & body]
  (route-with-method :put pattern bindings body))

(defmacro POST
  [pattern bindings & body]
  (route-with-method :post pattern bindings body))
