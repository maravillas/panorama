(ns panorama.test.source
  (:use panorama.source
        lamina.core
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

(describe "Updating a state ref"
  (it "combines all updates into the ref"
    (let [state (ref {})]
      (alter-state state {:foo 1} {:bar 2 :baz 3})
      (= @state
         {:foo 1 :bar 2 :baz 3})))
  (it "overwrites stale state"
    (let [state (ref {:foo 10})]
      (alter-state state {:foo 1} {:bar 2 :baz 3})
      (= @state
         {:foo 1 :bar 2 :baz 3})))
  (it "preserves state with no updates"
    (let [state (ref {:quux 10})]
      (alter-state state {:foo 1} {:bar 2 :baz 3})
      (= @state
         {:foo 1 :bar 2 :baz 3 :quux 10}))))

(describe "Recieving from a state channel"
  (it "updates the state"
    (let [state (ref {})
          ch (channel)]
      (fork-state-channel ch state)
      (enqueue ch {:foo 1})
      (= @state
         {:foo 1}))))
