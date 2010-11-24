/**
 * A flot plugin to insert marking labels at the top of a chart.
 *
 * Supported label rendering modes:
 *  > normal   :simple horizontal labels;
 *  > rotated  :labels which rotated on some (default is 45) angle
 *  > auto     :horizontal or rotated labels depending on the horizontal space available.
 *
 * Note: Rotated labels add margin-top space at the chart placeholder element.
 * Note: Rotated labels require jquery 2d transform plugin
 * (http://plugins.jquery.com/project/2d-transform) to be added to the page.
 * 
 */
(function ($) {

    var options = {
        headers: {
            mode: "auto", //"normal","rotated"
            show: false,
            maxMargin: 50, //in pixels //margin-top for rotated headings
            rotate: -45 //in degrees //how to rotate the headings
        }
    };

    function init(plot) {

        function extractRange(aPlot, ranges, coord) {
            var axis, from, to, axes, key;

            axes = aPlot.getUsedAxes();
            for (i = 0; i < axes.length; ++i) {
                axis = axes[i];
                if (axis.direction == coord) {
                    key = coord + axis.n + "axis";
                    if (!ranges[key] && axis.n == 1)
                        key = coord + "axis"; // support x1axis as xaxis
                    if (ranges[key]) {
                        from = ranges[key].from;
                        to = ranges[key].to;
                        break;
                    }
                }
            }

            // auto-reverse as an added bonus
            if (from != null && to != null && from > to) {
                var tmp = from;
                from = to;
                to = tmp;
            }

            return { from: from, to: to, axis: axis };
        }

        function insertHeader(aPlot) {
            var options = aPlot.getOptions();
            if (!options.grid.markings) {
                return;
            }

            var header = {
                id: "flotTableHeader" + (new Date()).getMilliseconds(),
                color: options.grid.color,
                labels: []
            };

            var markings = options.grid.markings;
            var averageLabelWidth = 0;

            var plotHeight = aPlot.height();
            var plotOffset = aPlot.getPlotOffset();

            for (var i = 0; i < markings.length; i++) {
                var m = markings[i];

                if (!m.label)
                    continue;

                var xrange = extractRange(aPlot, m, "x");
                var yrange = extractRange(aPlot, m, "y");

                if (xrange.to < xrange.axis.min || xrange.from > xrange.axis.max)
                    continue;

                xrange.from = Math.max(xrange.from, xrange.axis.min);
                xrange.to = Math.min(xrange.to, xrange.axis.max);

                yrange.from = Math.max(yrange.from, yrange.axis.min);

                if (xrange.from == xrange.to)
                    continue;

                var o1 = aPlot.pointOffset({x:xrange.from, y:yrange.to});
                var o2 = aPlot.pointOffset({x:xrange.to, y:yrange.to});

                var width = (Math.floor(o2.left) - Math.floor(o1.left));
                header.labels.push({
                    title: m.label,
                    color: m.color,
                    width: width,
                    bottom: (plotOffset.bottom + plotHeight + options.grid.labelMargin - 4),
                    left: o1.left
                });

                averageLabelWidth += width;
            }

            if (header.labels.length == 0) {
                return;
            }

            averageLabelWidth /= header.labels.length;

            var mode = options.headers.mode;
            if (mode == "auto") {
                mode = averageLabelWidth > 50 ? "normal" : "rotated";
            }

            if (mode == "normal") {
                insertNormalLabels(aPlot, header);
            } else {
                insertRotatedLabels(aPlot, header);
            }

        }

        function insertNormalLabels(aPlot, header) {
            var placeholder = aPlot.getPlaceholder();
            placeholder.css({marginTop: 0});

            var headerDiv = $('<div class="tickLabels" id="' + header.id + '" style="position:relative;cursor:default;font-size:smaller;font-weight:bold;color:' + header.color + '"/>');

            for (var i = 0; i < header.labels.length; i++) {
                var label = header.labels[i];
                var title = label.width > 50 ? label.title : "&#9670;";
                headerDiv.append('<div style="background-color:' + label.color + ';position:absolute;bottom:' + label.bottom + 'px;left:' + label.left + 'px;width:' + label.width + 'px;text-align:center; height:15px; overflow:hidden;" title="' + label.title + '" class="tickLabel">' + title + "</div>");
            }

            placeholder.append(headerDiv);
        }

        function insertRotatedLabels(aPlot, header) {
            var options = aPlot.getOptions();

            var placeholder = aPlot.getPlaceholder();

            var headerDiv = $('<div class="tickLabels" id="' + header.id + '" style="position:relative;cursor:default;font-size:smaller;font-weight:bold;color:' + header.color + '"/>');
            placeholder.prepend(headerDiv);

            var maxWidth = 0;
            for (var i = 0; i < header.labels.length; i++) {
                var label = header.labels[i];
                var div = $('<div class="diagonal-header" style="float:left;background-color:white;position:relative;font-family:Verdana, helvetica, arial, sans-serif;font-size:10px;padding:0;margin:0;overflow:hidden;"/>').html("<nobr>" + label.title + "</nobr>");
                headerDiv.append(div);

                var w = div.width();
                maxWidth = maxWidth < w ? w : maxWidth;
            }

            headerDiv.append('<div style="clear:left;"></div>');

            var sinAlpha = Math.abs(Math.sin(options.headers.rotate*Math.PI/180));
            var maxMargin = options.headers.maxMargin / sinAlpha;

            if (maxWidth > maxMargin) {
                maxWidth = maxMargin;
            } else {
                maxMargin = maxWidth * sinAlpha;
            }
            placeholder.css({marginTop: maxMargin + "px"});

            $("#" + header.id + " .diagonal-header").each(
                    function() {
                        var j = 0;
                        return function() {
                            var el = $(this);
                            el.width(maxWidth);

                            var w = el.height();

                            var angle = aPlot.getOptions().headers.rotate;
                            el.transform({origin: [0, 0], rotate: angle});
                            w = Math.abs(w*Math.sin(angle*Math.PI/180));

                            var label = header.labels[j++];

                            el.css({ position: "absolute",
                                top: -el.height() + 5, left: label.left + (label.width/2) - (w/2), "float":"none"});
                        }
                    }());
        }

        function draw(aPlot, ctx) {
            insertHeader(aPlot);
        }

        plot.hooks.draw.push(draw);
    }

    $.plot.plugins.push({
        init: init,
        options: options,
        name: "headers",
        version: "1.0"
    });
})(jQuery);
