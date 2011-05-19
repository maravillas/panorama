(ns panorama.test.config
  (:use panorama.config
        [lazytest.describe :only [describe it testing]])
  (:require [panorama.source server-status]))

(describe "Defining a config"
  (it "creates a config from a file"
    (binding [load-file (fn [_] (defconfig {:type :server-status
                                            :name "Atlas"}))]
      (= (class ((read-config "") "atlas"))
         panorama.source.server-status.ServerStatus)))
  (it "creates an empty config from an empty file"
    (binding [load-file (fn [_] nil)]
      (= (read-config "")
         {})))
  (it "creates an empty config from a file with no sources"
    (binding [load-file (fn [_] (defconfig))]
      (= (read-config "")
         {}))))