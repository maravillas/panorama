(ns panorama.templates
  (:use [net.cgrand.enlive-html
         :only [deftemplate content append html-snippet]]))

(deftemplate index "templates/index.html"
  [widgets updaters]
  [:#main] (content widgets)
  [:body] (append (html-snippet (str "<script>" updaters "</script>"))))
