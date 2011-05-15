(ns panorama.test.util
  (:require [net.cgrand.enlive-html :as enlive])
  (:import java.io.StringReader))

(defn widget->str
  [widget]
  (apply str (enlive/emit* widget)))

(defn widget->nodes
  [widget]
  (-> widget
      widget->str
      StringReader.
      enlive/html-resource))