(ns panorama.core
  (:use [panorama.middleware params]
        [panorama templates config source]
        [clojure.contrib logging]
        [clojure pprint]
        [aleph.http]
        [compojure.core]
        [lamina.core]
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]])
  (:require [compojure.route :as route]
            [org.danlarkin.json :as json])
  (:import [java.util Timer TimerTask]))

;; Send client updates at a slightly offset time to avoid overlapping with 
;; source update times, most of the time. Otherwise, the client update may
;; go out before the source is updated, resulting in a stale update.
;; This deserves a better solution at some point.

(def *client-delay* 10100)

(defonce config (ref []))
(defonce client-channel (permanent-channel))

(defn build-client-update
  [sources]
  (let [states (if (pos? (count sources))
                 (apply merge (map (fn [[k v]] {k (client-update v)}) sources))
                 {})]
    (json/encode states)))

(defn make-client-timer
  [channel config]
  (proxy [TimerTask] []
    (run []
         (debug "Client timer running")
         (enqueue channel (build-client-update @config)))))

(defn make-client-handler
  [config client-channel]
  (fn [ch handshake]
    (debug (str "New websocket client connected: " handshake))
    (enqueue ch (build-client-update @config))
    (siphon client-channel ch)))

(defn schedule-sources
  [timer sources]
  (doseq [source sources]
    (schedule-timer source timer)))

(defn enqueue-value
  [config source-id key value]
  (let [source (@config source-id)
        entry {(keyword key) value}]
    (cond
     (not source)
       (error (str "Source \"" source-id "\" not found. Available sources: " (keys @config)))
     (not key)
       (error "No key specified")
     :else
       (do
         (debug (str "Enqueuing " entry " into " source))
         (receive-message source entry)
         (str key " for " source-id " is now " value)))))

(defroutes main-routes
  (GET "/" []
       (index (map widget (vals @config))))

  (POST "/source/:id" {{value "value"
                        key "key"
                        id :id} :params}
        (enqueue-value config id key value))

  (GET "/client-socket" []
       (wrap-aleph-handler (make-client-handler config client-channel)))
  
  (route/not-found "404"))

(def app (-> main-routes
             wrap-params
             (wrap-file "public")
             wrap-file-info
             wrap-ring-handler))

(defn start-server
  []
  (dosync (ref-set config (read-config)))
  (let [stop-server (start-http-server (var app) {:port 8888 :websocket true})
        client-timer (Timer. "panorama-status-timer")
        source-timer (Timer. "panorama-source-timer")
        client-channel-debug #(debug (str "Client update: " %))]

    (.schedule client-timer (make-client-timer client-channel config) (long *client-delay*) (long *client-delay*))
    (schedule-sources source-timer (vals @config))

    ;; This receive-all is deceptively necessary.
    ;; even though client-channel is permanent, it seems to pass on
    ;; un-enqueueability to new siphon channels after it loses all of its
    ;; recipients. So, we'll keep a receive-all always open.
    (receive-all client-channel client-channel-debug)
    (fn []
      (cancel-callback client-channel client-channel-debug)
      (.cancel client-timer)
      (.cancel source-timer)
      (stop-server))))