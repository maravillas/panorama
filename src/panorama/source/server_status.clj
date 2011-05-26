(ns panorama.source.server-status
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [clojure.contrib.logging]
        [net.cgrand.enlive-html
         :only [defsnippet set-attr content]]))

(defn expand-seconds [i]
  (interleave
   (if (zero? i)
     (repeat 0)
     (reverse
      (map (fn [div mod] (int (rem (int (/ i div)) mod)))
           (reductions * [1 60 60 24 7])
           [60 60 24 7 i])))
   ["w" "d" "h" "m" "s"]))

(defn to-time-str
  [msec]
  (if (< msec 1000)
    "0s"
    (let [parts (expand-seconds (Math/ceil (/ msec 1000)))]
      (->> parts
           (partition 2)
           (drop-while (comp zero? first))
           (take 2)
           (interpose " ")
           (flatten)
           (apply str)))))

(defn time-status-changes
  [state]
  (let [internal (:internal state)
        status-changed (or (:status-changed internal) 0)]
    (if (and (:last-status internal)
             (= (:status state) (:last-status internal)))
      {:time (to-time-str (- (System/currentTimeMillis) status-changed))
       :internal {:last-status (:status state)
                  :status-changed status-changed}}
      {:time (to-time-str 0)
       :internal {:last-status (:status state)
                  :status-changed (System/currentTimeMillis)}})))

(defsnippet server-status-widget "templates/widget-server-status.html" [:.server-status-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

(defrecord ServerStatus [id name period channel state])

(defn server-status
  ([name]
     (server-status name 30))
  ([name period]
     (let [id (name->id name)
           channel (channel)
           initial-state {:status "down" :time "0s"}
           state (ref (merge initial-state (time-status-changes initial-state)))
           period (* period 1000)]
       (ServerStatus. id name period channel state))))

(extend-type ServerStatus
  Source
  (widget [source]
    (server-status-widget (:id source) (:name source)))
  (js [source]
    (read-resource "js/server-status.js"))
  (update-state [source]
    (let [updates (apply merge
                         {:status "down"}
                         (filter :status (channel-seq (:channel source))))
          time (time-status-changes (merge @(:state source) updates))]
      (alter-state (:state source) updates time)))
  (client-update [source]
    (dissoc @(:state source) :internal))
  (schedule-timer [source timer]
    (schedule source timer (:period source)))
  (receive-message [source message]
    (enqueue (:channel source) message)))

(defmethod make-source :server-status
  [config]
  (let [source (server-status (:name config))]
    {(:id source) source}))