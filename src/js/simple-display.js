window.panorama.updaters["simple-display"] = function(source, updates) {
  $(".value", source).text(updates.value);
};