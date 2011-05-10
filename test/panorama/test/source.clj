(ns panorama.test.source
  (:use panorama.source
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


(describe "Updating sources"
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



