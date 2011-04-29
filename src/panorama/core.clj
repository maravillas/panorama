(ns panorama.core
  (:use [panorama.middleware params]
        [panorama templates config]
        [clojure.contrib logging]
        aleph.http
        compojure.core
        lamina.core
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]])
  (:require [compojure.route :as route]
            [org.danlarkin.json :as json])
  (:import [java.util Timer TimerTask]))

(def *client-delay* 10000)
(def *default-source-period* 30000)

(defonce config (ref []))
(defonce client-channel (permanent-channel))

(defn state-as-map
  [[name source]]
  {name @(:state source)})

(defn build-client-update
  [sources]
  (let [states (if (pos? (count sources))
                 (apply merge (map state-as-map sources))
                 {})]
    (json/encode states)))

(defn make-client-timer
  [channel config]
  (proxy [TimerTask] []
    (run []
         (debug "Client timer running")
         (enqueue channel (build-client-update (:sources @config))))))

(defn next-state
  [source]
  ((:fn source) (:channel source) @(:state source)))

(defn update-state
  [source]
  (let [update (next-state source)]
    (dosync
     (alter (:state source) #(merge % update)))))

(defn make-source-timer
  [source]
  (proxy [TimerTask] []
    (run []
         (debug (str "Updating " source))
         (update-state source))))

(defn start-source-updates
  [timer sources]
  (doseq [[name source] sources]
    (let [period (or (:period source)
                     *default-source-period*)]
      (.schedule timer (make-source-timer source) (long period) (long period)))))

(defn make-client-handler
  [config client-channel]
  (fn [ch handshake]
    (debug (str "New websocket client connected: " handshake))
    (enqueue ch (build-client-update (:sources @config)))
    (siphon client-channel ch)))

(defn enqueue-value
  [config source-id key value]
  (let [source ((:sources @config) source-id)
        entry {(keyword key) value}]
    (cond
     (not source)
       (error (str "Source \"" source-id "\" not found. Available sources: " (keys (:sources @config))))
     (not key)
       (error "No key specified")
     :else
       (do
         (debug (str "Enqueuing " entry " into " source))
         (enqueue (:channel source) entry)
         (str key " for " source-id " is now " value)))))

(defn source-state
  [config source-name]
  (-> @config
      :sources
      (get source-name)
      :state))

(defroutes main-routes
  (GET "/" []
       (index (:widgets @config)))

  (POST "/source/:id" {{value "value"
                        key "key"
                        id :id} :params}
        (enqueue-value config id key value))

  (GET "/source/:id/:key" [id key]
       (str key " for " id " is: " (@(source-state config id) (keyword key))))
  
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
  (dosync
   (ref-set config (read-config)))
  (let [stop-server (start-http-server (var app) {:port 8888 :websocket true})
        client-timer (Timer. "panorama-status-timer")
        source-timer (Timer. "panorama-source-timer")
        client-channel-debug #(debug (str "Client update: " %))]

    (.schedule client-timer (make-client-timer client-channel config) (long *client-delay*) (long *client-delay*))
    (start-source-updates source-timer (:sources @config))

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