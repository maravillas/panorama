(ns panorama.test.core
  (:use panorama.core
        panorama.source
        lamina.core
        [lazytest.describe :only [describe it]])
  (:require [org.danlarkin.json :as json])
  (:import [java.util Timer]))

(defrecord TestSource [state next-state])

(extend-type TestSource
  Source
  (widget [source])
  (update-state [source]
    (when (= (class (:state source)) clojure.lang.Ref)
      (dosync (ref-set (:state source) {:status (:next-state source)}))))
  (client-update [source]
    (:state source))
  (schedule-timer [source timer]
    (schedule source timer 100))
  (receive-message [source message]
    (dosync (ref-set (:state source) message))))

(describe "Building client updates"
  (it "creates an JSON update"
    (let [sources {"source1" (TestSource. {:status "ok"} nil)
                   "source2" (TestSource. {:status "down" :time 5} nil)
                   "source3" (TestSource. {} nil)}]
      (= (json/decode (build-client-update sources))
         {:source1 {:status "ok"}
          :source2 {:status "down" :time 5}
          :source3 {}})))
  (it "creates an empty JSON update"
    (= (json/decode (build-client-update {}))
       {})))

(describe "Updating the client on a timer"
  (it "enqueues updates"
    (let [config (ref {"source1" (TestSource. {:status "ok"} nil)})
          ch (channel)
          timer (Timer.)]
      (.schedule timer (make-client-timer ch config) (long 1))
      (Thread/sleep 500)
      (.cancel timer)
      (= (json/decode (first (channel-seq ch)))
         {:source1 {:status "ok"}}))))

(defn new-state-fn
  [new-state]
  (fn [_ _] {:status new-state}))

(describe "Updating states"
  (it "updates the state"
    (let [source (TestSource. (ref {:status "down"}) "up")]
      (update-state source)
      (= @(:state source)
         {:status "up"})))
  (it "updates the status for a source on a timer"
    (let [source (TestSource. (ref {:status "down"}) "up")
          timer (Timer.)]
      (schedule-timer source timer)
      (Thread/sleep 500)
      (.cancel timer)
      (= @(:state source)
         {:status "up"})))
  (it "updates all source statuses on a timer"
    (let [sources [(TestSource. (ref {:status "ok"}) "super")
                   (TestSource. (ref {:status "?"}) "!")]
          timer (Timer.)]
      (schedule-sources timer sources)
      (Thread/sleep 500)
      (.cancel timer)
      (and (= @(:state (sources 0))
              {:status "super"})
           (= @(:state (sources 1))
              {:status "!"})))))

(describe "Accepting new client connections"
  (it "sends an initial update"
    (let [ch (channel)
          client-ch (channel)
          config (ref {"source1" (TestSource. {:status "ok"} nil)
                       "source2" (TestSource. {:status "?"} nil)})
          handler (make-client-handler config client-ch)]
      (handler ch nil)
      (= (json/decode (first (channel-seq ch)))
         {:source1 {:status "ok"}
          :source2 {:status "?"}})))
  (it "siphons to the client"
    (let [ch (channel)
          client-ch (channel)
          config (ref {})
          handler (make-client-handler config client-ch)]
      (handler ch nil)
      (enqueue client-ch :test)
      (some #{:test} (channel-seq ch)))))

(describe "Enqueuing updates"
  (it "sends messages to sources"
    (let [ch (channel)
          config (ref {"source1" (TestSource. (ref {}) nil)})]
      (enqueue-value config "source1" "status" "down")
      (= @(:state (@config "source1"))
         {:status "down"})))
  (it "returns a string"
    (let [ch (channel)
          config (ref {"source1" (TestSource. (ref {}) nil)})]
      (string? (enqueue-value config "source1" "status" "down"))))
  (it "does not enqueue updates with unspecified keys"
    (let [ch (channel)
          config (ref {"source1" (TestSource. (ref {}) nil)})]
      (enqueue-value config "source1" nil "down")
      (zero? (count (channel-seq ch))))))