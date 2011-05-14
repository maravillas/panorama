(ns panorama.test.source.simple-display
  (:use panorama.source
        panorama.source.simple-display
        lamina.core
        [lazytest.describe :only [describe it testing]])
  (:require [net.cgrand.enlive-html :as enlive]
            [panorama.test.util :as util])
  (:import java.io.StringReader))

(def config {:type :simple-display
             :name "Tickets"})

(describe "Server status source creation"
  (it "creates a valid map entry"
    (= (class ((make-source config) "tickets"))
       panorama.source.simple-display.SimpleDisplay))
  (it "defaults to a value of ''"
    (let [{src "tickets"} (make-source config)]
      (= (:value @(:state src))
         ""))))

(describe "Updating state"
  (it "does not change the value when an update contains no :value key"
    (let [{src "tickets"} (make-source config)]
      (enqueue (:channel src) {:value "Hi!"})
      (update-state src)
      (enqueue (:channel src) {:foo "bar"})
      (update-state src)
      (= (:value @(:state src))
         "Hi!")))
   (it "applies the value provided by the server"
    (let [{src "tickets"} (make-source config)]
      (enqueue (:channel src) {:value "4"})
      (update-state src)
      (= (:value @(:state src))
         "4"))))

(describe "Generating client updates"
  (it "includes the default value"
    (let [{src "tickets"} (make-source config)
          update (client-update src)]
      (= (:value update)
         "")))
  (it "includes an updated value"
    (let [{src "tickets"} (make-source config)]
      (enqueue (:channel src) {:value "Hello"})
      (update-state src)
      (let [update (client-update src)]
        (= (:value update)
           "Hello")))))

(describe "Generating widgets"
  (it "creates a widget"
    (let [{src "tickets"} (make-source config)
          w (util/widget->str (widget src))]
      (pos? (count w))))
  (it "sets the widget's id"
    (let [{src "tickets"} (make-source config)
          w (-> (widget src)
                util/widget->str
                StringReader.
                enlive/html-resource)]
      (pos? (count (enlive/select w [:#source-tickets])))))
  (it "sets the widget's name"
    (let [{src "tickets"} (make-source config)
          w (-> (widget src)
                util/widget->str
                StringReader.
                enlive/html-resource)]
      (= (first (:content (first (enlive/select w [:footer]))))
         "Tickets"))))