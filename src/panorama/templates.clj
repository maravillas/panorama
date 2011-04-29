(ns panorama.templates
  (:use [net.cgrand.enlive-html
         :only [deftemplate defsnippet set-attr content emit*]]))

(defsnippet server-status-widget "templates/widget-server-status.html" [:.server-status-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

;; Surely there's a way to parameterize this.
;; Also handle blank mini widgets.
(defsnippet server-status-4-widget "templates/widget-server-status-4.html" [:.server-status-4-widget]
  [id1 name1 & [id2 name2 id3 name3 id4 name4]]
  [:#source-1 :footer] (content name1)
  [:#source-1] (set-attr :id (str "source-" id1))
  [:#source-2 :footer] (content name2)
  [:#source-2] (set-attr :id (str "source-" id2))
  [:#source-3 :footer] (content name3)
  [:#source-3] (set-attr :id (str "source-" id3))
  [:#source-4 :footer] (content name4)
  [:#source-4] (set-attr :id (str "source-" id4)))

(defsnippet passive-numeric-widget "templates/widget-passive-numeric.html" [:.passive-numeric-widget]
  [id name]
  [:#source :footer] (content name)
  [:#source] (set-attr :id (str "source-" id)))

(deftemplate index "templates/index.html"
  [config]
  [:#main] (content config))

(defmulti config-entry->widget :type)

(defmethod config-entry->widget :server-status
  [config]
  (apply server-status-widget (interleave (:ids config) (:names config))))

(defmethod config-entry->widget :server-status-4
  [config]
  (apply server-status-4-widget (interleave (:ids config) (:names config))))

(defmethod config-entry->widget :passive-numeric
  [config]
  (apply passive-numeric-widget (interleave (:ids config) (:names config))))