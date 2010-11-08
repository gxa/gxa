/**
 *  A fixed window scroll plugin.
 *
 */
(function ($) {

    var options = {
        scroll: {
            mode: null, // "x", "y", "xy"
            windowColor: "#e8cfac"
        }
    };

    function init(plot) {

        var scrollWindow = {
            first: {x:0, y:0},
            second: {x:0, y:0},
            mouse: {x:0, y:0},
            active: false,
            contains: function(pos) {
                return pos.x >= this.first.x && pos.x <= this.second.x &&
                        pos.y >= this.first.y && pos.y <= this.second.y;

            },
            move: function(pos, aPlot) {
                var mode = aPlot.getOptions().scroll.mode;

                if (mode.indexOf("x") >= 0) {
                    var dx = (pos.x - this.mouse.x);
                    if (this.first.x + dx < 0) {
                        dx = -this.first.x;
                    } else if (this.second.x + dx > aPlot.width()) {
                        dx = aPlot.width() - this.second.x;
                    }
                    this.first.x += dx;
                    this.second.x += dx;
                }
                if (mode.indexOf("y") >= 0) {
                    var dy = (pos.y - this.mouse.y);
                    var h = this.height();
                    if (this.first.y + dy < 0) {
                        dy = -this.first.y;
                    } else if (this.second.y + dy > aPlot.height()) {
                        dy = a.Plot.height() - this.second.y;
                    }
                    this.first.y += dy;
                    this.second.y += dy;
                }
                this.mouse = pos;
            },
            moveStart: function(pos) {
                this.mouse = pos;
                this.active = true;
            },
            moveStop: function() {
                this.active = false;
            }
        };

        // FIXME: The drag handling implemented here should be
        // abstracted out, there's some similar code from a library in
        // the navigation plugin, this should be massaged a bit to fit
        // the Flot cases here better and reused. Doing this would
        // make this plugin much slimmer.
        var savedhandlers = {};

        function onMouseDown(e) {
            if (e.which != 1)  // only accept left-click
                return;

            // cancel out any text selections
            document.body.focus();

            // prevent text selection and drag in old-school browsers
            if (document.onselectstart !== undefined && savedhandlers.onselectstart == null) {
                savedhandlers.onselectstart = document.onselectstart;
                document.onselectstart = function () {
                    return false;
                };
            }
            if (document.ondrag !== undefined && savedhandlers.ondrag == null) {
                savedhandlers.ondrag = document.ondrag;
                document.ondrag = function () {
                    return false;
                };
            }


            var pos = getPos(e);
            /*            if (!scrollWindow.contains(pos)) {
             return;
             }*/
            scrollWindow.moveStart(pos);

            $(document).one("mouseup", onMouseUp);
        }

        function onMouseUp(e) {
            if (!scrollWindow.active) {
                return  false;
            }

            // revert drag stuff for old-school browsers
            if (document.onselectstart !== undefined)
                document.onselectstart = savedhandlers.onselectstart;
            if (document.ondrag !== undefined)
                document.ondrag = savedhandlers.ondrag;

            scrollWindow.moveStop();
            triggerScrollEvent();

            return false;
        }

        function onMouseMove(e) {
            if (scrollWindow.active) {
                redrawScrollWindow(e);
            }
        }

        function setScrollWindow(ranges, preventEvent) {
            var axis, range, axes = plot.getAxes();

            var options = plot.getOptions();

            if (options.scroll.mode == "y") {
                scrollWindow.first.x = 0;
                scrollWindow.second.x = plot.width();
            }
            else {
                axis = ranges["xaxis"] ? axes["xaxis"] : (ranges["x2axis"] ? axes["x2axis"] : axes["xaxis"]);
                range = ranges["xaxis"] || ranges["x2axis"] || { from:ranges["x1"], to:ranges["x2"] };
                scrollWindow.first.x = axis.p2c(Math.min(range.from, range.to));
                scrollWindow.second.x = axis.p2c(Math.max(range.from, range.to));
            }

            if (options.scroll.mode == "x") {
                scrollWindow.first.y = 0;
                scrollWindow.second.y = plot.height();
            }
            else {
                axis = ranges["yaxis"] ? axes["yaxis"] : (ranges["y2axis"] ? axes["y2axis"] : axes["yaxis"]);
                range = ranges["yaxis"] || ranges["y2axis"] || { from:ranges["y1"], to:ranges["y2"] };
                scrollWindow.first.y = axis.p2c(Math.min(range.from, range.to));
                scrollWindow.second.y = axis.p2c(Math.max(range.from, range.to));
            }

            plot.triggerRedrawOverlay();
            if (!preventEvent)
                triggerScrollEvent();
        }

        function getScrollWindow() {
            var x1 = Math.round(scrollWindow.first.x),
                    x2 = Math.round(scrollWindow.second.x),
                    y1 = Math.round(scrollWindow.first.y),
                    y2 = Math.round(scrollWindow.second.y);

            var r = {};
            var axes = plot.getAxes();
            if (axes.xaxis.used)
                r.xaxis = { from: axes.xaxis.c2p(x1), to: axes.xaxis.c2p(x2) };
            if (axes.x2axis.used)
                r.x2axis = { from: axes.x2axis.c2p(x1), to: axes.x2axis.c2p(x2) };
            if (axes.yaxis.used)
                r.yaxis = { from: axes.yaxis.c2p(y1), to: axes.yaxis.c2p(y2) };
            if (axes.y2axis.used)
                r.y2axis = { from: axes.y2axis.c2p(y1), to: axes.y2axis.c2p(y2) };
            return r;
        }

        function redrawScrollWindow(e) {
            if (e.pageX == null)
                return;

            scrollWindow.move(getPos(e), plot);
            plot.triggerRedrawOverlay();
        }

        function getPos(e) {
            var offset = plot.getPlaceholder().offset();
            var plotOffset = plot.getPlotOffset();
            return {
                x: clamp(0, e.pageX - offset.left - plotOffset.left, plot.width()),
                y: clamp(0, e.pageY - offset.top - plotOffset.top, plot.height())
            };
        }

        function triggerScrollEvent() {
            var r = getScrollWindow();
            plot.getPlaceholder().trigger("plotscrolled", [ r ]);
        }

        function clamp(min, value, max) {
            return value < min ? min : (value > max ? max : value);
        }

        function drawOverlay(aPlot, ctx) {
            var options = aPlot.getOptions();
            var plotOffset = aPlot.getPlotOffset();

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            var c = $.color.parse(options.scroll.windowColor);

            ctx.strokeStyle = c.scale('a', 0.8).toString();
            ctx.lineWidth = 1;
            ctx.lineJoin = "round";
            ctx.fillStyle = c.scale('a', 0.4).toString();

            var x = Math.max(scrollWindow.first.x, 0);
            var y = Math.max(scrollWindow.first.y, 0);

            var w = Math.abs(Math.min(scrollWindow.second.x, plot.width()) - x);
            var h = Math.abs(Math.min(scrollWindow.second.y, plot.height()) - y);

            ctx.fillRect(x, y, w, h);
            ctx.strokeRect(x, y, w, h);

            ctx.restore();
        }

        function bindEvents(aPlot, eventHolder) {
            var options = aPlot.getOptions();
            if (options.scroll.mode) {
                eventHolder.mousedown(onMouseDown);
                eventHolder.mousemove(onMouseMove);
            }
        }

        plot.getScrollWindow = getScrollWindow;
        plot.setScrollWindow = setScrollWindow;
        plot.hooks.bindEvents.push(bindEvents);
        plot.hooks.drawOverlay.push(drawOverlay);
    }

    $.plot.plugins.push({
        init: init,
        options: options,
        name: "scroll",
        version: "1.0"
    });
})(jQuery);
