(ns panorama.test.sources
  (:use panorama.sources
        lamina.core
        [lazytest.describe :only [describe it testing]]))

(describe "Source creation"
  (it "includes an update function"
    (let [source (make-source identity)]
      (= (:fn source) identity)))
  (it "initializes a channel"
    (let [source (make-source identity)
          ch (:channel source)
          val {:test true}]
      (enqueue ch val)
      (= @(:state source)
         val)))
  (it "does not consume from the original channel"
    (let [source (make-source identity)
          ch (:channel source)
          val {:test true}]
      (enqueue ch val)
      (= (count (channel-seq (:channel source)))
         1)))
  (it "specifies an update period"
    (let [source (make-source identity 10)]
      (= (:period source) 10000))))

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

(describe "Updating sources"
  (testing "of type server-status"
    (it "applies a status of 'down' when there are no messages from the server"
      (let [ch (channel)]
        (enqueue ch {:foo "bar"})
        (= (:status (update-timed-server-status ch {:status "up"}))
           "down")))
    (it "applies the status provided by the server"
      (let [ch (channel)]
        (enqueue ch {:status "up"})
        (= (:status (update-timed-server-status ch {:status "down"}))
           "up")))
    (it "updates the time when the status does not change"
      (let [ch (channel)
            time (- (System/currentTimeMillis) 1000)]
        (enqueue ch {:status "up"})
        (= (:time (update-timed-server-status ch {:status "up"
                                                  :internal {:last-status "up"
                                                             :status-changed time}}))
           "1s")))
    (it "updates the time when the status changes"
      (let [ch (channel)
            time (- (System/currentTimeMillis) 1000)]
        (enqueue ch {:status "down"})
        (= (:time (update-timed-server-status ch {:status "up"
                                                  :internal {:last-status "up"
                                                             :status-changed time}}))
           "0s"))))
  (testing "of type server-status-4"
    (it "applies a status of 'down' when there are no messages from the server"
      (let [ch (channel)]
        (enqueue ch {:foo "bar"})
        (= (:status (update-server-status ch {:status "up"}))
           "down")))
    (it "applies the status provided by the server"
      (let [ch (channel)]
        (enqueue ch {:status "up"})
        (= (:status (update-server-status ch {:status "down"}))
           "up")))))

(describe "Creating sources from config entries"
  (it "creates a server-status source"
    (map #(every? % [:fn :channel :state])
         (config-entry->sources {:type :server-status
                                 :ids ["id"]
                                 :names ["name"]})
            [:fn :channel :state]))
  (it "creates server-status-4 sources"
    (map #(every? % [:fn :channel :state])
         (config-entry->sources {:type :server-status-4
                                 :ids ["id1 id2 id3 id4"]
                                 :names ["name1 name2 name3 name4"]}))))