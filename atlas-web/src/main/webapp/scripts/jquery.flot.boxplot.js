/**
 * { series: [
 *     {
 *       boxes: { show: true },
 *       data: [
 *           {
 *             id: "DUSP1:Kaposi's sarcoma-associated herpesvirus",
 *             x: 0,
 *             min: 7.975854873657227,
 *             median: 8.196444511413574,
 *             max: 8.518889427185059,
 *             lq: 8.16000747680664,
 *             uq: 8.400483131408691
 *           },
 *           {
 *             id: "DUSP1:uninfected",
 *             x: 5,
 *             min: 7.975854873657227,
 *             median: 8.196444511413574,
 *             max: 8.518889427185059,
 *             lq:8.16000747680664,
 *             uq:8.400483131408691
 *            },
 *            ...
 *       ]
 *     },
 *     {
 *       boxes: { show: true },
 *       data: [
 *           {
 *             id: "DUSP1:Kaposi's sarcoma-associated herpesvirus",
 *             x: 1,
 *             min: 7.010293006896973,
 *             median: 7.403378963470459,
 *             max: 7.66507625579834,
 *             lq: 7.36135196685791,
 *             uq:7.616357803344727
 *           },
 *           ...
 *       ]
 *     }
 *     ...
 * }
 *
 */
(function ($) {
    var options = {
        series: {
            lines: {
                show: false
            },
            boxes: {
                show: false
            }
        },
        boxes: {
            hoverable: false,
            arrowUp: "red",
            arrowDown: "blue",
            boxWidth: 0.6 // value from range [0..1]
        }
    };

    function init(plot) {

        var boxes = [];
        var arrows = [];

        /**
         * Draws UP / DOWN arrows over boxes.
         *
         * @param points an array of box values to highlight
         */
        function highlightUpDown(points) { // points = [{x:number, y:number, isUp:boolean}, ...]
            var axes = plot.getAxes();
            var xaxis = axes.xaxis;
            var yaxis = axes.yaxis;

            arrows = [];
            for(var i=0; i<points.length; i++) {
                var point = points[i];
                if (point.x < xaxis.min || point.x > xaxis.max ||
                        point.y < yaxis.min || point.y > yaxis.max) {
                    continue;
                }

                arrows.push(point);
            }

            plot.triggerRedrawOverlay();
        }

        function onMouseMove(e) {
            var pos = getPos(e);
            for(var i=0; i<boxes.length; i++) {
                var box = boxes[i];
                if (box.contains(pos.x, pos.y)) {
                    triggerBoxHoverEvent(e, box.x);
                    return;
                }
            }

            triggerBoxOutEvent(pos);
        }

        function triggerBoxHoverEvent(e, x) {
            plot.getPlaceholder().trigger("boxhover", [ e, x ]);
        }

        function triggerBoxOutEvent(pos) {
            plot.getPlaceholder().trigger("boxout", []);
        }

        function getPos(e) {
            var offset = plot.getPlaceholder().offset();
            var plotOffset = plot.getPlotOffset();
            return {
                x: clamp(0, e.pageX - offset.left - plotOffset.left, plot.width()),
                y: clamp(0, e.pageY - offset.top - plotOffset.top, plot.height())
            };
        }

        function clamp(min, value, max) {
            return value < min ? min : (value > max ? max : value);
        }

        function drawBox(x1, x2, min, lq, median, uq, max, axisx, axisy, color, ctx) {
            ctx.lineWidth = 1;

            var d = Math.abs(x2 - x1) / 2.0;

            if (x1 > axisx.max || x1 + 2*d < axisx.min || max < axisy.min || min > axisy.max) {
                return;
            }

            var xLeft = axisx.p2c(Math.max(axisx.min, x1));
            var xRight = axisx.p2c(Math.min(axisx.max, x1 + 2 * d));

            var yLq = axisy.p2c(Math.max(axisy.min, lq));
            var yUq = axisy.p2c(Math.min(axisy.max, uq));

            var yMedian = axisy.p2c(median);
            var yZero = axisy.p2c(0);

            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.moveTo(xLeft, yLq);
            ctx.lineTo(xRight, yLq);
            ctx.lineTo(xRight, yZero);
            ctx.lineTo(xLeft, yZero);
            ctx.closePath();
            ctx.fill();

            ctx.fillStyle = $.color.parse(color).scale('a', 0.6).toString();
            ctx.beginPath();
            ctx.moveTo(xLeft, yMedian);
            ctx.lineTo(xRight, yMedian);
            ctx.lineTo(xRight, yLq);
            ctx.lineTo(xLeft, yLq);
            ctx.closePath();
            ctx.fill();

            ctx.fillStyle = $.color.parse(color).scale('a', 0.3).toString();
            ctx.beginPath();
            ctx.moveTo(xLeft, yUq);
            ctx.lineTo(xRight, yUq);
            ctx.lineTo(xRight, yMedian);
            ctx.lineTo(xLeft, yMedian);
            ctx.closePath();
            ctx.fill();

            boxes.push({x1: xLeft, x2: xRight, y1: yZero, y2: yUq, x: x1, contains: function(x,y) {return this.x1 <= x && this.x2 >= x && (this.y1 + 2) >= y && (this.y2 - 2) <= y;} });

            var dx1 = x1 + d;
            if (dx1 >= axisx.min && dx1 <= axisx.max) {

                var xMiddle = axisx.p2c(dx1);
                var yMax = axisy.p2c(max);
                var yMin = axisy.p2c(min);

                xLeft = axisx.p2c(dx1 - d * 0.5);
                xRight = axisx.p2c(dx1 + d * 0.5);

                ctx.strokeStyle = color;
                ctx.beginPath();
                ctx.moveTo(xMiddle, yUq);
                ctx.lineTo(xMiddle, yMax);
                ctx.closePath();
                ctx.stroke();

                ctx.beginPath();
                ctx.moveTo(xLeft, yMax);
                ctx.lineTo(xRight, yMax);
                ctx.closePath();
                ctx.stroke();

                ctx.strokeStyle = $.color.parse(color).scale('rgb', 1.3).toString();
                ctx.beginPath();
                ctx.moveTo(xMiddle, yLq);
                ctx.lineTo(xMiddle, yMin);
                ctx.closePath();
                ctx.stroke();

                ctx.beginPath();
                ctx.moveTo(xLeft, yMin);
                ctx.lineTo(xRight, yMin);
                ctx.closePath();
                ctx.stroke();
            }
        }
        
        function drawSeriesBoxes(ctx, plotOffset, series) {
            
            function plotBoxes(datapoints, axisx, axisy, color) {
                var points = datapoints.points, ps = datapoints.pointsize;
                
                for (var i = 0; i < points.length; i += ps*5) {
                    if (points[i] == null) 
                    continue;
                    drawBox(points[i], points[i + 2], points[i + 1], points[i + 3], points[i + 5], points[i + 7], points[i + 9], axisx, axisy, color, ctx);
                }
            }

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            plotBoxes(series.datapoints, series.xaxis, series.yaxis, series.color);

            ctx.restore();
        }
        
        function drawSeries(plot, ctx) {
            var series = plot.getData();
            var offset = plot.getPlotOffset();
                
            for(var i=0; i<series.length; i++) { 
                if (series[i].boxes.show) { 
                    drawSeriesBoxes(ctx, offset, series[i]);
                }
            }
        }
        
        function processRawData(plot, series, seriesData, datapoints) {
            if (!series.boxes.show) {
                return;
            }
            
            var points = [];
            var boxWidth = plot.getOptions().boxes.boxWidth;
            var boxOffset = (1 - boxWidth) / 2;
                       
            for(var i=0; i<seriesData.length; i++) {
               var d = seriesData[i];

               if (d.x == undefined) {
                   continue;
               }

               var x = d.x + boxOffset;

               points.push([x, d.min]);
               
               points.push([x + boxWidth, d.lq]);
               
               points.push([x, d.median]);
               
               points.push([x, d.uq]);
               
               points.push([x, d.max]);
               
            } 

            if (points.length > 0) {
                series.data = points;
            }
        }

        function bindEvents(aPlot, eventHolder) {
            var options = aPlot.getOptions();
            if (options.boxes.hoverable) {
                eventHolder.mousemove(onMouseMove);
            }
        }

        function drawOverlay(plot, ctx) {
            function drawArrow(arrow, opts, axes, ctx) {
                var boxWidth = opts.boxes.boxWidth;
                var boxOffset = (1 - boxWidth) / 2;

                var cy = axes.yaxis.p2c(arrow.y) - 2;
                var cx1 = axes.xaxis.p2c(arrow.x + boxOffset);
                var cx2 = axes.xaxis.p2c(arrow.x + boxOffset + boxWidth);
                var d = Math.abs(cx2 - cx1);

                var cx = (cx1 + cx2) / 2;
                var dy = 5 * (arrow.isUp ? 1 : -1);
                cy -= arrow.isUp ? 2*dy : 0;
                cy = cy < 0 ? 0 : cy;
                cy = cy + 2*dy < 0 ? 2*dy : cy;
                
                var dx = d / 4;

                ctx.fillStyle = arrow.isUp ? opts.boxes.arrowUp : opts.boxes.arrowDown;
                ctx.beginPath();
                ctx.moveTo(cx, cy);
                ctx.lineTo(cx - 2*dx, cy + dy);
                ctx.lineTo(cx - dx, cy + dy);
                ctx.lineTo(cx - dx, cy + 2*dy);
                ctx.lineTo(cx + dx, cy + 2*dy);
                ctx.lineTo(cx + dx, cy + dy);
                ctx.lineTo(cx + 2*dx, cy + dy);

                ctx.closePath();
                ctx.fill();
            }

            var plotOffset = plot.getPlotOffset();

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            for(var i=0; i<arrows.length; i++) {
                drawArrow(arrows[i], plot.getOptions(), plot.getAxes(), ctx);
            }

            ctx.restore();
        }

        plot.highlightUpDown = highlightUpDown;
        plot.hooks.processRawData.push(processRawData);
        plot.hooks.draw.push(drawSeries);
        plot.hooks.bindEvents.push(bindEvents);
        plot.hooks.drawOverlay.push(drawOverlay);
    }
    
    $.plot.plugins.push({
        init: init,
        options: options,
        name: "boxplot",
        version: "1.0"
    });
})(jQuery);
