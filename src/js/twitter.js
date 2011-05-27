(function() {
  var ns = window.panorama;
  
  if (!ns.currentTweets) {
    ns.currentTweets = {};
  }
    
  if (!ns.nextTweets) {
    ns.nextTweets = {};
  }

  var populateContainer = function(source, tweets) {
    source.empty();
    var tweetGroup = "<div class='tweets'>";

    $.each(tweets, function(index, tweet) {
      tweetGroup +=
        "<div class='tweet'><span class='user'>" + tweet.user
        + ":</span> <span class='text'>" + tweet.text
        + "</span> <span class='date'>" + tweet.date + "</span></div>";
    });
  
    source.append(tweetGroup + "</div>");

    $(".tweet", source).hide();
  };

  var rolloverTweets = function(source) {
    ns.currentTweets[source] = ns.nextTweets[source];
    populateContainer(source, ns.currentTweets[source]);
  };
  
  var cycleTweets = function(source) {
    populateContainer(source, ns.currentTweets[source]);
    
    var tweet = 0, lastTweet = -1;
    
    var nextTweet = function() {
      var tweets = $(".tweets .tweet", source);
      
      if (tweet >= tweets.length) {
        tweet = 0;
        rolloverTweets(source);
        tweets = $(".tweets .tweet", source);
      }
      
      tweets.eq(lastTweet).hide();
      tweets.eq(tweet).show();

      lastTweet = tweet;
      tweet += 1;
    };

    nextTweet();

    ns.twitterTimer = window.setInterval(nextTweet, 10000);
  };

  var mergeTweets = function(last, next) {
    var i = next.length - 1;
    
    while (next[i] !== last[last.length - 1] &&
           i >= 0) {
      i -= 1;
    }

    return last.concat(next.slice(i+1));
  };

  ns.updaters["twitter"] = function(source, updates) {
    if (ns.currentTweets[source] === undefined) {
      ns.currentTweets[source] = updates;
      ns.nextTweets[source] = updates;
      
      cycleTweets(source);
    }
    else {
      ns.currentTweets[source] = mergeTweets(ns.currentTweets[source], updates);
      ns.nextTweets[source] = updates;
    }    
  };
})();

