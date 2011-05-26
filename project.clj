(defproject panorama "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.6.2"]
                 [aleph "0.2.0-alpha1"]
                 [enlive "1.0.0"]
                 [ring "0.3.7"]
                 [org.danlarkin/clojure-json "1.2-SNAPSHOT"]
                 [com.stuartsierra/lazytest "1.1.2"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})
