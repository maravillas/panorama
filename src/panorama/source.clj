(ns panorama.source
  (:use [clojure.contrib.string :only [lower-case replace-re]]
        [lamina.core :only [receive-all fork]]
        [clojure.contrib.logging :only [debug]])
  (:import [java.util TimerTask]))

(defprotocol Source
  (widget [source])
  (update-state [source])
  (client-update [source])
  (schedule-timer [source timer])
  (receive-message [source message]))

(defmulti make-source :type)

(defn name->id
  [name]
  (replace-re #"[ \t#]+" "-" (lower-case name)))

(defn alter-state
  [state & updates]
  (dosync
   (alter state #(apply merge % updates))))

(defn fork-state-channel
  [channel state]
  (receive-all (fork channel) #(alter-state state %)))

(defn make-source-timer
  [source]
  (proxy [TimerTask] []
    (run []
         (debug (str "Updating " source))
         (update-state source))))

(defn schedule
  [source timer period]
  (.schedule timer (make-source-timer source) (long period) (long period)))