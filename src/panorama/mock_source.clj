(ns panorama.mock-source
  (:use [aleph http]
        [clojure.contrib logging])
  (:require [clojure.contrib.string :as string])
  (:import [java.util Timer TimerTask]))

(defn make-source-timer
  [request]
  (proxy [TimerTask] []
    (run [] (sync-http-request request))))

(defn map->params
  [m]
  (string/join "&" (map (fn [[k v]] (str (string/as-str k) "=" v)) m)))

(defn make-request
  [url params]
  {:method :post
   :url url
   :body (map->params params)
   :headers {"content-type" "application/x-www-form-urlencoded"}})

(defn start-mock-source
  [request period]
  (let [timer (Timer. "panorama-mock-source")]
    (.schedule timer (make-source-timer request) (long 0) (long period))
    (fn [] (.cancel timer))))