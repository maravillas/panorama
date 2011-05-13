(defconfig
  {:type :server-status
   :name "Production"}
  {:type :mini-group
   :sources [{:type :mini-server-status
              :name "Dev"}
             {:type :mini-server-status
              :name "Test"}
             {:type :mini-server-status
              :name "Staging"}
             {:type :mini-server-status
              :name "Demo"}]}
  {:type :simple-display
   :name "#clojure users"})