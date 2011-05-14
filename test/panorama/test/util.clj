(ns panorama.test.util
  (:require [net.cgrand.enlive-html :as enlive]))

(defn widget->str
  [widget]
  (apply str (enlive/emit* widget)))