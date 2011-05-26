(ns panorama.source.simple-display
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [net.cgrand.enlive-html
         :only [defsnippet set-attr content]]))

(defsnippet simple-display-widget "templates/widget-simple-display.html" [:.simple-display-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

(defrecord SimpleDisplay [id name period channel state])

(defn simple-display
  ([name]
     (let [id (name->id name)
           channel (channel)
           state (ref {:value ""})
           period (* 60 1000)]
       (fork-state-channel channel state)
       (SimpleDisplay. id name period channel state))))

(extend-type SimpleDisplay
  Source
  (widget [source]
    (simple-display-widget (:id source) (:name source)))
  (js [source]
    (read-resource "js/simple-display.js"))
  (update-state [source]
    (apply alter-state (:state source) (filter :value (channel-seq (:channel source)))))
  (client-update [source]
    @(:state source))
  (schedule-timer [source timer]
    (schedule source timer (:period source)))
  (receive-message [source message]
    (enqueue (:channel source) message)))

(defmethod make-source :simple-display
  [config]
  (let [source (simple-display (:name config))]
    {(:id source) source}))