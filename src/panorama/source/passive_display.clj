(ns panorama.source.passive-display
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [net.cgrand.enlive-html
         :only [defsnippet set-attr content]]))

(defsnippet passive-display-widget "templates/widget-passive-numeric.html" [:.passive-numeric-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

(defrecord PassiveDisplay [id name period channel state])

(defn passive-display
  ([name]
     (let [id (name->id name)
           channel (channel)
           state (ref {})
           period (* 60 1000)]
       (fork-state-channel channel state)
       (PassiveDisplay. id name period channel state))))

(extend-type PassiveDisplay
  Source
  (widget [source]
    (passive-display-widget (:id source) (:name source)))
  (update-state [source]
    (alter-state (:state source) (filter :value (channel-seq channel))))
  (client-update [source]
    @(:state source))
  (schedule-timer [source timer]
    (schedule source timer (:period source)))
  (receive-message [source message]
    (enqueue (:channel source) message)))

(defmethod make-source :passive-display
  [config]
  (passive-display (:name config)))