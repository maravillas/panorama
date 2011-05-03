(ns panorama.sources
  (:use lamina.core))

(defn make-source
  ([f period]
     (let [channel (channel)
           state (ref {})]
       (receive-all (fork channel)
                    (fn [m] (dosync (alter state #(merge % m)))))
       (merge
        (when period {:period (* period 1000)})
        {:fn f
         :channel channel
         :state state})))
  ([f]
     (make-source f nil)))

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

(defmulti config-entry->sources :type)

(defn update-timed-server-status
  [channel state]
  (let [new-state (apply merge state {:status "down"} (filter :status (channel-seq channel)))
        time (time-status-changes new-state)]
    (merge new-state time)))

(defmethod config-entry->sources :server-status
  [config]
  (map (fn [id period] [id (make-source update-timed-server-status period)])
       (:ids config)
       (:periods config)))


(defn update-server-status
  [channel state]
  (apply merge state {:status "down"} (filter :status (channel-seq channel))))

(defmethod config-entry->sources :server-status-4
  [config]
  (map (fn [id period] [id (make-source update-server-status period)])
       (:ids config)
       (:periods config)))


(defn update-passive-numeric
  [channel state]
  (apply merge state (filter :value (channel-seq channel))))

(defmethod config-entry->sources :passive-numeric
  [config]
  (map (fn [id] [id (make-source update-passive-numeric)])
       (:ids config)))