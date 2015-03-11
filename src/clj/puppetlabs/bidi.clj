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
    (assoc-in route-info [:request-method] pattern)

    :else
    (update-in route-info [:path] concat (flatten [pattern]))))

(declare breadth-route-metadata*)

(defn depth-route-metadata*
  [route-meta route-info loc]
  (let [[pattern matched] (zip/node loc)]
    (cond
      (map? matched)
      (depth-route-metadata*
        route-meta
        route-info
        (-> loc zip/down zip/right (zip/edit #(into [] %)) zip/up))

      (vector? matched)
      (breadth-route-metadata*
        route-meta
        (update-route-info route-info pattern)
        (-> loc zip/down zip/right zip/down))

      :else
      (let [route-info (update-route-info route-info pattern)]
        (-> route-meta
            (update-in [:routes] conj route-info)
            (assoc-in [:handlers matched] route-info))))))

(defn breadth-route-metadata*
  [route-meta route-info loc]
  (loop [route-meta route-meta
         loc    loc]
    (let [routes (depth-route-metadata* route-meta route-info loc)]
      (if-let [next (zip/right loc)]
        (recur routes next)
        routes))))

(defn route-metadata
  [routes]
  (let [route-info {:path   []
                    :request-method :any}
        loc        (-> [routes] zip/vector-zip zip/down)]
    (breadth-route-metadata* {:routes []
                              :handlers {}} route-info loc)))

(defn make-handler
  "Create a Ring handler from the route definition data
  structure. Matches a handler from the uri in the request, and invokes
  it with the request as a parameter."
  [route-meta compiled-route handler-fn callback-fn]
  (assert compiled-route "Cannot create a Ring handler with a nil Route(s) parameter")
  (fn [{:keys [uri path-info] :as req}]
    (let [path (or path-info uri)
          {:keys [handler route-params] :as match-context}
          (apply bidi/match-route compiled-route path (apply concat (seq req)))]
      (when handler
        (when callback-fn
          (callback-fn (get-in route-meta [:handlers handler]) req))
        (bidi-ring/request
          (handler-fn handler)
          (-> req
              (update-in [:params] merge route-params)
              (update-in [:route-params] merge route-params))
          (apply dissoc match-context :handler (keys req))
          )))))


(defn routes
  [& routes]
  ["" (vec routes)])

(defn context->handler
  ([context callback-fn]
   (let [route-meta (route-metadata context)]
     (with-meta
       (make-handler route-meta
                     (bidi/compile-route context)
                     identity
                     callback-fn)
       {:route-metadata route-meta})))
  ([context]
   (context->handler context nil)))

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
