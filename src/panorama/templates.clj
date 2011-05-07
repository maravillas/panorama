(ns panorama.templates
  (:use [net.cgrand.enlive-html
         :only [deftemplate defsnippet set-attr content emit*]]))

(deftemplate index "templates/index.html"
  [config]
  [:#main] (content config))
