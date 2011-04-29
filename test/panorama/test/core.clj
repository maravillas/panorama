(ns panorama.test.core
  (:use panorama.core
        lamina.core
        [lazytest.describe :only [describe it]])
  (:require [org.danlarkin.json :as json])
  (:import [java.util Timer]))

(describe "Building client updates"
  (it "creates an JSON update"
    (let [sources {"source1" {:state (ref {:status "ok"})}
                   "source2" {:state (ref {:status "down" :time 5})}
                   "source3" {:state (ref {})}}]
      (= (json/decode (build-client-update sources))
         {:source1 {:status "ok"}
          :source2 {:status "down" :time 5}
          :source3 {}})))
  (it "creates an empty JSON update"
    (= (json/decode (build-client-update {}))
       {})))

(describe "Updating the client on a timer"
  (it "enqueues updates"
    (let [config (ref {:sources {"source1" {:state (ref {:status "ok"})}}})
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

(describe "Iterating source states"
  (it "iterates with the source update function"
    (let [source {:fn (fn [ch state] [ch state])
                  :channel "channel"
                  :state (ref "state")}]
      (= (next-state source)
         ["channel" "state"])))
  (it "updates the state"
    (let [source {:fn (new-state-fn "up")
                  :channel "channel"
                  :state (ref {:status "down"})}]
      (update-state source)
      (= @(:state source)
         {:status "up"}))))

(describe "Updating statuses on a timer"
  (it "updates the status for a source"
    (let [source {:state (ref {:status "ok"})
                  :channel nil
                  :fn (new-state-fn "down")}
          timer (Timer.)]
      (.schedule timer (make-source-timer source) (long 100))
      (Thread/sleep 500)
      (.cancel timer)
      (= @(:state source)
         {:status "down"})))
  (it "updates all source statuses"
    (let [sources {"source1" {:state (ref {:status "ok"})
                              :channel nil
                              :fn (new-state-fn "down")
                              :period 100}
                   "source2" {:state (ref {:status "?"})
                              :channel nil
                              :fn (new-state-fn "up")
                              :period 100}}
          timer (Timer.)]
      (start-source-updates timer sources)
      (Thread/sleep 500)
      (.cancel timer)
      (and (= @(:state (sources "source1"))
              {:status "down"})
           (= @(:state (sources "source2"))
              {:status "up"})))))

(describe "Accepting new client connections"
  (it "sends an initial update"
    (let [ch (channel)
          client-ch (channel)
          config (ref {:sources {"source1" {:state (ref {:status "ok"})}
                                 "source2" {:state (ref {:status "?"})}}})
          handler (make-client-handler config client-ch)]
      (handler ch nil)
      (= (json/decode (first (channel-seq ch)))
         {:source1 {:status "ok"}
          :source2 {:status "?"}})))
  (it "siphons to the client"
    (let [ch (channel)
          client-ch (channel)
          config (ref {:sources []})
          handler (make-client-handler config client-ch)]
      (handler ch nil)
      (enqueue client-ch :test)
      (some #{:test} (channel-seq ch)))))

(describe "Enqueuing updates"
  (it "enqueues updates to sources"
    (let [ch (channel)
          config (ref {:sources {"source1" {:channel ch}}})]
      (enqueue-value config "source1" "status" "down")
      (= (channel-seq ch)
         [{:status "down"}])))
  (it "returns a string"
    (let [ch (channel)
          config (ref {:sources {"source1" {:channel ch}}})]
      (string? (enqueue-value config "source1" "status" "down"))))
  (it "does not enqueue updates with unspecified keys"
    (let [ch (channel)
          config (ref {:sources {"source1" {:channel ch}}})]
      (enqueue-value config "source1" nil "down")
      (zero? (count (channel-seq ch))))))