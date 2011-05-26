window.panorama.updaters["server-status"] = function(source, updates) {
  source.removeClass(function (i, classes) {
    return classes.match(/status-[^\s]+/g).join(" ");
  });

  source.addClass("status-" + updates.status);

  $(".time", source).text(updates.time);
};
