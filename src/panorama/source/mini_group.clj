(ns panorama.source.mini-group
  (:use [panorama.source]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [clojure.contrib.generic.functor :only [fmap]]
        [net.cgrand.enlive-html
         :only [defsnippet add-class substitute do->]]))

(defn substitute-source
  [source & classes]
  (let [[id source] (first source)]
    (if source
      (do->
       (substitute (widget source))
       (apply add-class classes))
      identity)))

(defn without-widget
  [original]
  (reify
    Source
    (widget [source])
    (js [source] (js original))
    (update-state [source] (update-state original))
    (client-update [source] (client-update original))
    (schedule-timer [source timer] (schedule-timer original timer))
    (receive-message [source message] (receive-message original message))))

(defsnippet mini-group-widget "templates/widget-mini-group.html" [:.mini-group-widget]
  [[source1 source2 source3 source4]]
  [:#source-1] (substitute-source source1 "top" "left") 
  [:#source-2] (substitute-source source2 "top" "right")
  [:#source-3] (substitute-source source3 "bottom" "left")
  [:#source-4] (substitute-source source4 "bottom" "right"))

(defrecord MiniGroup [sources])

(defn mini-group
  ([sources]
     (MiniGroup. sources)))

(extend-type MiniGroup
  Source
  (widget [source]
    (mini-group-widget (:sources source)))
  (js [source])
  (update-state [source])
  (client-update [source])
  (schedule-timer [source timer])
  (receive-message [source message]))
    
(defmethod make-source :mini-group
  [config]
  (let [sources (map make-source (:sources config))
        group (mini-group sources)
        source-map (apply merge sources)]
    (merge {(str (gensym "mini-group")) group} (fmap without-widget source-map))))