(ns panorama.config
  (:use [panorama.source :only [make-source]]
        [clojure.contrib.string :only [lower-case replace-re]]
        [clojure.contrib logging pprint])
  (:require [panorama.source.server-status]
            [panorama.source.server-status-4]
            [panorama.source.passive-display]))

(defn read-config
  ([filename]
     (try (let [sources (load-file filename)
                new-config sources]
            (debug (str "Loaded config: " (with-out-str (pprint new-config))))
            (map make-source new-config))
          (catch java.io.FileNotFoundException _
            [])))
  ([]
     (read-config "config.clj")))

(defn defconfig
  [& body]
  body)