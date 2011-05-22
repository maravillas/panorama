(ns panorama.source.twitter
  (:use [panorama.source]
        [twitter :as twitter]
        [oauth.client :as oauth]
        [lamina.core :only [enqueue channel receive-all fork channel-seq]]
        [net.cgrand.enlive-html :only [defsnippet set-attr content]])
  (:require [clj-time
             [core :as time]
             [format :as time-format]]))

(def incoming-date-format (time-format/formatter "E MMM d HH:mm:ss Z YYYY"))
(def outgoing-date-format (time-format/formatter "M/d h:m a"))

(defn age-limit->period
  [age-limit]
  (let [period-fn (ns-resolve 'clj-time.core (symbol (name (:unit age-limit))))]
    (try
      (period-fn (:count age-limit))
      (catch IllegalStateException ex
        (throw (IllegalStateException. (str "Unknown age-limit unit: " (:unit age-limit)) ex))))))

(defn user-timelines
  [users]
  (doall
   (->> users
        (map #(vector :screen-name %))
        (mapcat #(apply twitter/user-timeline %)))))

(defn reply?
  [tweet]
  (:in_reply_to_screen_name tweet))

(defn reply-to?
  [tweet names]
  (some #{(:in_reply_to_screen_name tweet)} names))

(defn exclude-replies
  [exceptions tweets]
  (remove #(and (reply? %)
                (not (reply-to? % exceptions))) tweets))

(defn older-than?
  [period date]
  (time/before? date (time/minus (time/now) period)))

(defn exclude-older
  [age tweets]
  (remove #(older-than? age (:date %)) tweets))

(defn created-date
  [tweet]
  (time-format/parse incoming-date-format (:created_at tweet)))

(defn trim-tweet
  [tweet]
  (-> tweet
      (assoc :screen-name (:screen_name (:user tweet)))
      (assoc :date (created-date tweet))
      (select-keys [:text :date :screen-name])))

(defn compare-dates
  [t1 t2]
  (.compareTo (:date t1) (:date t2)))

(defn format-date
  [tweet]
  (assoc tweet :date (time-format/unparse outgoing-date-format (:date tweet))))

(defn fetch-tweets
  [source]
  (twitter/with-https
    (twitter/with-oauth
      (:consumer source)
      (:access-token source)
      (:access-secret source)
      (user-timelines (:users source)))))

(defn process-tweets
  [tweets users age-limit]
  (->> tweets
       (exclude-replies users)
       (map trim-tweet)
       (exclude-older age-limit)
       (sort compare-dates)
       (map format-date)))

(defsnippet twitter-widget "templates/widget-twitter.html" [:.twitter-widget]
  [id name]
  [:#source] (set-attr :id (str "source-" id)))

(defrecord Twitter [users tweets age-limit consumer access-token access-secret])

(defn twitter
  ([{:keys [users age-limit oauth]}]
     (Twitter. users
               (ref [])
               (age-limit->period age-limit)
               (oauth/make-consumer (:consumer-key oauth)
                                    (:consumer-secret oauth)
                                    "https://api.twitter.com/oauth/request_token"
                                    "https://api.twitter.com/oauth/access_token"
                                    "https://api.twitter.com/oauth/authorize"
                                    :hmac-sha1)
               (:access-token oauth)
               (:access-secret oauth))))

(extend-type Twitter
  Source
  (widget [source]
    (twitter-widget (:id source) (:name source)))
  (update-state [source]
    (let [tweets (process-tweets (fetch-tweets source)
                                 (:users source)
                                 (:age-limit source))]
      (dosync
       (ref-set (:tweets source) tweets))))
  (client-update [source]
    @(:tweets source))
  (schedule-timer [source timer]
    (schedule source timer 300))
  (receive-message [source message]
    (enqueue (:channel source) message)))
    
(defmethod make-source :twitter
  [config]
  (let [source (twitter config)]
    {(:id source) source}))