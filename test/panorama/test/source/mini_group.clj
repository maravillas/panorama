(ns panorama.test.source.mini-group
  (:use panorama.source
        panorama.source.mini-group
        lamina.core
        [lazytest.describe :only [describe it testing]])
  (:require [net.cgrand.enlive-html :as enlive]
            [panorama.test.util :as util])
  (:import java.io.StringReader))

(def config1 {:type :mini-group
              :sources [{:type :mini-server-status
                         :name "Production"}]})

(def config4 {:type :mini-group
              :sources [{:type :mini-server-status
                         :name "Production"}
                        {:type :mini-server-status
                         :name "Dev"}
                        {:type :mini-server-status
                         :name "Staging"}
                        {:type :mini-server-status
                         :name "Demo"}]})

(defn find-mini-group
  [coll]
  (some (fn [[k v]] (and (.startsWith k "mini-group") v))
        coll))

(describe "Server status source creation"
  (it "includes the group"
    (let [sources (make-source config1)
          w (find-mini-group sources)]
      (and w (= (class w) panorama.source.mini-group.MiniGroup))))
  (it "includes the subsources"
    (let [sources (make-source config1)]
      (sources "production")))
  (it "creates subsources without accessible widgets"
    (let [sources (make-source config1)
          w (widget (sources "production"))]
      (not w))))

(describe "Generating widgets"
  (it "creates a widget"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:.mini-group-widget]))
         1)))
  (it "creates 4 subwidgets"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:.mini-group-widget :.source-widget]))
         4)))
  (it "places subwidget 1 at top left"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:#source-production.top.left]))
         1)))
  (it "places subwidget 2 at top right"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:#source-dev.top.right]))
         1)))
  (it "places subwidget 3 at bottom left"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:#source-staging.bottom.left]))
         1)))
  (it "places subwidget 4 at bottom right"
    (let [src (find-mini-group (make-source config4))
          w (util/widget->nodes (widget src))]
      (= (count (enlive/select w [:#source-demo.bottom.right]))
         1))))