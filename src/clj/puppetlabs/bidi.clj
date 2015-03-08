(ns puppetlabs.bidi
  (:require [bidi.ring :as bidi-ring]
            [clojure.zip :as zip]
            [clojure.tools.logging :as log]))

(defn- assoc-&-binding [binds req sym]
  (assoc binds sym `(dissoc (:params ~req)
                            ~@(map keyword (keys binds))
                            ~@(map str (keys binds)))))

(defn- assoc-symbol-binding [binds req sym]
  (assoc binds sym `(get-in ~req [:params ~(keyword sym)]
                            (get-in ~req [:params ~(str sym)]))))

(defn- vector-bindings [args req]
  (loop [args args, binds {}]
    (if-let [sym (first args)]
      (cond
        (= '& sym)
        (recur (nnext args) (assoc-&-binding binds req (second args)))
        (= :as sym)
        (recur (nnext args) (assoc binds (second args) req))
        (symbol? sym)
        (recur (next args) (assoc-symbol-binding binds req sym))
        :else
        (throw (Exception. (str "Unexpected binding: " sym))))
      (mapcat identity binds))))

(defn- warn-on-*-bindings! [bindings]
  (when (and (vector? bindings) (contains? (set bindings) '*))
    (binding [*out* *err*]
      (log/warn "WARNING: * should not be used as a route binding."))))

(defmacro ^:no-doc let-request [[bindings request] & body]
  (warn-on-*-bindings! bindings)
  (if (vector? bindings)
    `(let [~@(vector-bindings bindings request)] ~@body)
    `(let [~bindings ~request] ~@body)))

(defmacro handler-fn
  [bindings body]
  `(fn [request#]
     (let-request [~bindings request#] ~@body)))

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