(ns panorama.source.mini-server-status
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [net.cgrand.enlive-html
         :only [defsnippet set-attr content]]))

(defsnippet mini-server-status-widget "templates/widget-mini-server-status.html" [:.mini-server-status-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

(defrecord MiniServerStatus [id name period channel state])

(defn mini-server-status
  ([name]
     (mini-server-status name 60))
  ([name period]
     (let [id (name->id name)
           channel (channel)
           state (ref {})
           period (* period 1000)]
       (fork-state-channel channel state)
       (MiniServerStatus. id name period channel state))))

(extend-type MiniServerStatus
  Source
  (widget [source]
    (mini-server-status-widget (:id source) (:name source)))
  (update-state [source]
    (let [updates (apply merge
                         {:status "down"}
                         (filter :status (channel-seq (:channel source))))]
      (alter-state (:state source) updates)))
  (client-update [source]
    {(:id source) @(:state source)})
  (schedule-timer [source timer]
    (schedule source timer (:period source)))
  (receive-message [source message]
    (enqueue (:channel source) message)))

(defmethod make-source :mini-server-status
  [config]
  (mini-server-status (:name config)))