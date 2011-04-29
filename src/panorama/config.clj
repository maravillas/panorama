(ns panorama.config
  (:use panorama.templates
        panorama.sources
        [clojure.contrib.string :only [lower-case replace-re]]
        [clojure.contrib logging pprint]))

(defn read-config
  ([filename]
     (try (let [config-file (load-file filename)
                sources (into {} (mapcat config-entry->sources config-file))
                widgets (map config-entry->widget config-file)
                new-config {:file config-file
                            :sources sources
                            :widgets widgets}]
            (debug (str "Loaded config: " (with-out-str (pprint new-config))))
            new-config)
          (catch java.io.FileNotFoundException _
            [])))
  ([]
     (read-config "config.clj")))

(defn defconfig
  [& body]
  body)

(defn name->id
  [name]
  (replace-re #"[ \t#]+" "-" (lower-case name)))

(defn separate
  [n coll]
  (map (partial take-nth n)
       ((apply juxt (map #(partial drop %) (range n))) coll)))

(defn server-status
  [name period]
  (let [id (name->id name)]
    {:ids [id]
     :names [name]
     :periods [period]
     :type :server-status}))

(defn server-status-4
  [name period & others]
  (let [id (name->id name)
        [names periods] (separate 2 others)
        ids (map name->id names)]
    {:ids (cons id ids)
     :names (cons name names)
     :periods (cons period periods)
     :type :server-status-4}))

(defn passive-numeric
  [name]
  (let [id (name->id name)]
    {:ids [id]
     :names [name]
     :type :passive-numeric}))