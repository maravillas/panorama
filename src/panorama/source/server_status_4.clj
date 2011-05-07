(ns panorama.source.server-status-4
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [net.cgrand.enlive-html
         :only [defsnippet set-attr content]]))

(defn separate
  [n coll]
  (map (partial take-nth n)
       ((apply juxt (map #(partial drop %) (range n))) coll)))

(defsnippet server-status-4-widget "templates/widget-server-status-4.html" [:.server-status-4-widget]
  [id1 name1 & [id2 name2 id3 name3 id4 name4]]
  [:#source-1 :footer] (content name1)
  [:#source-1] (set-attr :id (str "source-" id1))
  [:#source-2 :footer] (content name2)
  [:#source-2] (set-attr :id (str "source-" id2))
  [:#source-3 :footer] (content name3)
  [:#source-3] (set-attr :id (str "source-" id3))
  [:#source-4 :footer] (content name4)
  [:#source-4] (set-attr :id (str "source-" id4)))

(defrecord ServerStatus4 [ids names periods channel state])

(defn server-status-4
  ([name & names]
     (let [id (name->id name)
           ids (map name->id names)
           channel (channel)
           state (ref {})
           periods (repeat (inc (count ids)) (* 60 1000))]
       (receive-all (fork channel) (fn [m] (dosync (alter state #(merge % m)))))
       (ServerStatus4. (cons id ids) (cons name names) periods channel state))))

(extend-type ServerStatus4
  Source
  (widget [source]
    (apply server-status-4-widget (interleave (:ids source) (:names source))))
  (update-state [source]
    (let [updates (apply merge
                         {:status "down"}
                         (filter :status (channel-seq channel)))]
      (alter-state (:state source) updates)))
  (client-update [source]
    @(:state source))
  (schedule-timer [source timer]
    (schedule source timer (:period source)))
  (receive-message [source message]
    (enqueue (:channel source) message)))

(defmethod make-source :server-status-4
  [config]
  (apply server-status-4 (:names config)))