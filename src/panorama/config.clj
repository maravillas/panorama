(ns panorama.config
  (:use [panorama.source :only [make-source]]
        [clojure.contrib.string :only [lower-case replace-re]]
        [clojure.contrib logging pprint])
  (:require [panorama.source.server-status]
            [panorama.source.mini-group]
            [panorama.source.mini-server-status]
            [panorama.source.passive-display]))

(defn read-config
  ([filename]
     (try (let [sources (load-file filename)
                new-config (into (array-map) (map make-source sources))]
            (debug (str "As sources: " (with-out-str (pprint (map make-source sources)))))
            (debug (str "Loaded config: " (with-out-str (pprint new-config))))
            new-config)
          (catch java.io.FileNotFoundException _
            [])))
  ([]
     (read-config "config.clj")))

(defn defconfig
  [& body]
  body)