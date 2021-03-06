(ns panorama.test.source.mini-server-status
  (:use panorama.source
        panorama.source.mini-server-status
        lamina.core
        [lazytest.describe :only [describe it testing]])
  (:require [net.cgrand.enlive-html :as enlive]
            [panorama.test.util :as util])
  (:import java.io.StringReader))

(def config {:type :mini-server-status
             :name "Production"})

(describe "Server status source creation"
  (it "creates a valid map entry"
    (= (class ((make-source config) "production"))
       panorama.source.mini-server-status.MiniServerStatus))
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
         "up"))))

(describe "Generating client updates"
  (it "includes status"
    (let [{src "production"} (make-source config)
          update (client-update src)]
      (= (:status update)
         "down"))))

(describe "Generating widgets"
  (it "creates a widget"
    (let [{src "production"} (make-source config)
          w (util/widget->str (widget src))]
      (pos? (count w))))
  (it "sets the widget's id"
    (let [{src "production"} (make-source config)
          w (-> (widget src)
                util/widget->str
                StringReader.
                enlive/html-resource)]
      (pos? (count (enlive/select w [:#source-production])))))
  (it "sets the widget's name"
    (let [{src "production"} (make-source config)
          w (-> (widget src)
                util/widget->str
                StringReader.
                enlive/html-resource)]
      (= (first (:content (first (enlive/select w [:footer]))))
         "Production"))))