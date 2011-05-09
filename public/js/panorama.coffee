panorama = {}

panorama.updates =
  updateSources: (updates) ->
    for sourceName, sourceUpdates of updates
      source = $("#source-#{sourceName}")

      for key, update of sourceUpdates
        @updateTypes[key].call(this, source, update) if key isnt "internal"

  statusClassRegex: /status-[^\s]+/g,

  updateTypes:
    status: (source, status) ->
      source.removeClass (index, classes) =>
        classes.match(@statusClassRegex).join " "

      source.addClass "status-#{status}"

    time: (source, time) ->
      $(".time", source).text time

    value: (source, value) ->
      $(".value", source).text value

panorama.scaling =
  pxToInt: (str) ->
    parseInt(str.replace(/px/, ""), 10)

  horizontalMargins: (elem) ->
    @pxToInt(elem.css("marginLeft")) + @pxToInt(elem.css("marginRight"))

  verticalMargins: (elem) ->
    @pxToInt(elem.css("marginTop")) + @pxToInt(elem.css("marginBottom"))

  calculateScale: (origWidth, origHeight) ->
    availableWidth = $(window).width() - @horizontalMargins($(document.body)) - 20
    availableHeight = $(window).height() - @verticalMargins($(document.body)) - 20

    xScale = availableWidth / origWidth
    yScale = availableHeight / origHeight

    Math.min(xScale, yScale)

  scale: (origWidth, origHeight) ->
    $(document.body).css "zoom", @calculateScale(origWidth, origHeight)


class panorama.Connection
  constructor: (@url) ->

  connect: ->
    @socket = new WebSocket @url

    @socket.onopen = =>
      console.log "Connected to #{@url}"

    @socket.onclose = =>
      console.log "Disconnected from #{@url}"

    @socket.onmessage = (msg) =>
      console.log "Message: #{msg.data}"
      try
        updates = $.parseJSON msg.data
        panorama.updates.updateSources updates
      catch ex
        console.error ex
        console.error "Invalid status message: #{msg.data}"

    null

$(document).ready ->
  connection = new panorama.Connection "ws://localhost:8888/client-socket"
  connection.connect()

  origWidth = $("#main").width()
  origHeight = $("#main").height()

  panorama.scaling.scale origWidth, origHeight

  $(window).resize ->
    panorama.scaling.scale origWidth, origHeight