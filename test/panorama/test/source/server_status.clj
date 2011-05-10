(ns panorama.test.source.server-status
  (:use panorama.source
        panorama.source.server-status
        lamina.core
        [lazytest.describe :only [describe it testing]]))

(def config {:type :server-status
             :name "Production"})

(describe "Splitting seconds"
  (it "splits 8707 seconds"
    (= (expand-seconds 8707)
       [0 "w" 0 "d" 2 "h" 25 "m" 7 "s"]))
  (it "splits 25 seconds"
    (= (expand-seconds 25)
       [0 "w" 0 "d" 0 "h" 0 "m" 25 "s"]))
  (it "splits 12519702 seconds"
    (= (expand-seconds 12519702)
       [20 "w" 4 "d" 21 "h" 41 "m" 42 "s"]))
  (it "handles 0 seconds"
    (= (expand-seconds 0)
       [0 "w" 0 "d" 0 "h" 0 "m" 0 "s"])))

(describe "Converting to a time string"
  (it "converts 0 milliseconds"
    (= (to-time-str 0)
       "0s"))
  (it "converts 8707 seconds"
    (= (to-time-str (* 8707 1000))
       "2h 25m"))
  (it "converts 12519702 seconds"
    (= (to-time-str (* 12519702 1000))
       "20w 4d"))
  (it "converts 2 seconds"
    (= (to-time-str 2000)
       "2s"))
  (it "converts 500 milliseconds"
    (= (to-time-str 500)
       "0s")))

(describe "Computing the time since the last status change"
  (it "returns 0 seconds in the initial state"
    (= (:time (time-status-changes {}))
       "0s"))
  (it "returns 0 seconds when the status changes"
    (= (:time (time-status-changes {:time nil
                                    :status "up"
                                    :internal {:last-status "down"
                                               :status-changed 1000}}))
       "0s"))
  (it "returns the time period when the status does not change"
    (let [time (- (System/currentTimeMillis) 1000)]
      (= (:time (time-status-changes {:time nil
                                      :status "up"
                                      :internal {:last-status "up"
                                                 :status-changed time}}))
         "1s"))))

(describe "Server status source creation"
  (it "creates a valid map entry"
    (= (class ((make-source config) "production"))
       panorama.source.server-status.ServerStatus))
  (it "defaults to a status of 'down'"
    (let [{src "production"} (make-source config)]
      (= (:status @(:state src))
         "down"))))

(describe "Updating state"
  (it "applies a status of 'down' when there are no messages from the server"
    (let [{src "production"} (make-source config)]
      (enqueue (:channel src) {:foo "bar"})
      (update-state src)
      (= (:status @(:state src))
         "down")))
   (it "applies the status provided by the server"
    (let [{src "production"} (make-source config)]
      (enqueue (:channel src) {:status "up"})
      (update-state src)
      (= (:status @(:state src))
         "up")))
  (it "updates the time when the status does not change"
    (let [{src "production"} (make-source config)
          src (assoc src :state (ref {:status "up"
                                      :internal {:last-status "up"
                                                 :status-changed (- (System/currentTimeMillis) 1000)}}))]
      (enqueue (:channel src) {:status "up"})
      (update-state src)
      (= (:time @(:state src))
         "1s")))
  (it "updates the time when the status changes"
    (let [{src "production"} (make-source config)
          src (assoc src :state (ref {:time "2s"
                                      :status "down"
                                      :internal {:last-status "down"
                                                 :status-changed (- (System/currentTimeMillis) 1000)}}))]
      (enqueue (:channel src) {:status "up"})
      (update-state src)
      (= (:time @(:state src))
         "0s"))))

(describe "Generating client updates"
  (it "excludes internal state"
    (let [{src "production"} (make-source config)]
      (not (:internal (client-update src)))))
  (it "includes time and status"
    (let [{src "production"} (make-source config)
          update (client-update src)]
      (and (= (:status update)
              "down")
           (= (:time update)
              "0s")))))