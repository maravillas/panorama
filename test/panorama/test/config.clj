(ns panorama.test.config
  (:use panorama.config
        [lazytest.describe :only [describe it testing]]))

(describe "Converting names to ids"
  (it "replaces spaces"
    (= (name->id "a name")
       "a-name"))
  (it "lowercases"
    (= (name->id "CASE")
       "case"))
  (it "replaces tabs"
    (= (name->id "a\ttab")
       "a-tab"))
  (it "replaces hashes"
    (= (name->id "we're#1!")
       "we're-1!")))

(describe "Separate"
  (it "splits two interleaved collections"
    (= (separate 2 (interleave [:a :b :c] [1 2 3]))
       [[:a :b :c] [1 2 3]]))
  (it "does not modify the collection when n is 1"
    (= (separate 1 (range 4))
       [(range 4)]))
  (it "splits five interleaved collections"
    (= (separate 5 (interleave [1] [2] [3] [4] [5]))
       [[1] [2] [3] [4] [5]])))

(describe "Defining sources"
  (it "specifies a server status source"
    (= (server-status "Foo" 30)
       {:ids [(name->id "Foo")]
        :names ["Foo"]
        :periods [30]
        :type :server-status}))
  (it "specifies a server status 4 source"
    (let [names ["Foo" "Bar" "Baz" "Quux"]
          periods (range 4)]
      (println "asdfasdf" (apply server-status-4 (interleave names periods)))
      (= (apply server-status-4 (interleave names periods))
         {:ids (map name->id names)
          :names names
          :periods periods
          :type :server-status-4})))
  (it "specifies a passive numeric source"
    (= (passive-numeric "Foo")
       {:ids [(name->id "Foo")]
        :names ["Foo"]
        :type :passive-numeric})))