(ns panorama.test.source.twitter
  (:use panorama.source
        panorama.source.twitter
        lamina.core
        [clj-time
         [core :only [now minus days minutes date-time before?]]
         [format :only [formatter unparse]]]
        [lazytest.describe :only [describe it testing]])
  (:require [net.cgrand.enlive-html :as enlive]
            [panorama.test.util :as util])
  (:import java.io.StringReader))

(defn make-tweet
  [& {:keys [reply-to created date text]}]
  {:text (or text "A tweet")
   :in_reply_to_screen_name reply-to
   :created_at (or created "Fri May 20 00:00:00 +0000 2011")
   :date date
   :extra-bits true})

;; Technically this is duplicated from the source, but try not to think about it.
;; We need relative dates in this format.
(def twitter-date-format (formatter "E MMM d HH:mm:ss Z YYYY"))

(defn time-ago->str
  [period]
  (unparse twitter-date-format (minus (now) period)))

(describe "Tweet processing"
  (testing "Reply filtering"
    (it "identifies replies"
      (reply? (make-tweet :reply-to "maravillas")))
    (it "identifies non-replies"
      (not (reply? (make-tweet))))
    (it "identifies replies to certain users"
      (reply-to? (make-tweet :reply-to "maravillas") ["maravillas"]))
    (it "does not identify replies to other users"
      (not (reply-to? (make-tweet :reply-to "example") ["maravillas"])))
    (it "filters all replies"
      (let [t1 (make-tweet)
            t2 (make-tweet :reply-to "maravillas")
            t3 (make-tweet :reply-to "example")]
        (= (exclude-replies []  [t1 t2 t3])
           [t1])))
    (it "returns an empty collection when all tweets are replies"
      (let [t1 (make-tweet :reply-to "example-2")
            t2 (make-tweet :reply-to "maravillas")
            t3 (make-tweet :reply-to "example")]
        (= (exclude-replies [] [t1 t2 t3])
           [])))
    (it "filters replies with exceptions"
      (let [t1 (make-tweet)
            t2 (make-tweet :reply-to "maravillas")
            t3 (make-tweet :reply-to "example")
            t4 (make-tweet :reply-to "another-example")]
        (= (exclude-replies ["maravillas" "another-example"] [t1 t2 t3 t4])
           [t1 t2 t4]))))
  (testing "Age filtering"
    (it "identifies dates older than a threshold"
      (let [date (minus (now) (days 10))]
        (older-than? (days 4) date)))
    (it "identifies dates not older than a threshold"
      (let [date (minus (now) (days 2))]
        (not (older-than? (days 4) date))))
    (it "identifies dates older than a smaller threshold"
      (let [date (minus (now) (minutes 2))]
        (not (older-than? (minutes 4) date))))
    (it "extracts the created date/time from a tweet"
      (let [tweet {:text "A tweet" :created_at "Fri May 20 00:00:00 +0000 2011"}]
        (= (created-date tweet)
           (date-time 2011 5 20 0 0 0))))
    (it "filters tweets older than a threshold"
      (let [t1 (make-tweet :date (minus (now) (days 1)))
            t2 (make-tweet :date (minus (now) (days 10)))
            t3 (make-tweet :date (now))]
        (= (exclude-older (days 2) [t1 t2 t3])
           [t1 t3]))))
  (testing "Trimming tweets"
    (it "removes unnecessary keys"
      (= (set (keys (trim-tweet {:text ""
                                 :created_at "Fri May 20 00:00:00 +0000 2011"
                                 :user {:screen_name "maravillas"}
                                 :foo 1
                                 :bar 2
                                 :baz 3})))
         #{:text :date :user}))
    (it "converts the date"
      (= (:date (trim-tweet (make-tweet :created "Fri May 20 00:00:00 +0000 2011")))
         (date-time 2011 5 20 0 0 0)))
    (it "pulls out the screen name"
      (= (:user (trim-tweet {:text ""
                             :created_at "Fri May 20 00:00:00 +0000 2011"
                             :user {:screen_name "maravillas" :foo 1}}))
         "maravillas")))
  (testing "Full tweet processing"
    (it "excludes replies"
      (= (process-tweets [(make-tweet :reply-to "maravillas")] [] (days 1))
         []))
    (it "trims tweets"
      (not (:extra-bits (first (process-tweets [(make-tweet)] [] (days 1))))))
    (it "excludes old tweets"
      (= (process-tweets [(make-tweet :created_at (time-ago->str (days 2)))] [] (days 1))
         []))
    (it "sorts tweets by date"
      (let [tweets (process-tweets [(make-tweet :created (time-ago->str (days 2))
                                                :text "Youngest")
                                    (make-tweet :created (time-ago->str (days 4))
                                                :text "Oldest")]
                                   []
                                   (days 5))]
        (= (:text (first tweets))
           "Oldest")))
    (it "formats the date"
      (string? (:date (first (process-tweets [(make-tweet :created (time-ago->str (days 2)))]
                                       []
                                       (days 4))))))))