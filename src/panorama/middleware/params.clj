(ns panorama.middleware.params
  "Parse form and query params."
  (:use [aleph.http.utils :only [query-params body-params]]
        [lamina.core :only [wait-for-result]])
  (:require [ring.util.codec :as codec]
            [clojure.string :as string]))

(defn wrap-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a urlencoded form). Adds the following keys to
  the request map:
    :query-params - a map of parameters from the query string
    :form-params  - a map of parameters from the body
    :params       - a merged map of all types of parameter"
  [handler & [opts]]
  (fn [request]
    (let [body (if-let [ch (body-params request)] (wait-for-result ch) {})
          query (if-let [ch (query-params request)] (wait-for-result ch) {})]
      (handler (merge-with merge
                           request
                           {:form-params body
                            :query-params query
                            :params (merge body query)})))))
