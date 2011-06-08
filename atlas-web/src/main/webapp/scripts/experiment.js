/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

(function() {
    //TODO: move this code to atlas.js (logDebug, logError ?)

    window.atlasLog = function(msg) {
        if (window.console) {
            window.console.log("atlas: " + msg);
        }
    }
}());

(function() {

    var curatedProperties_ = null;

    var AssayProperties = window.AssayProperties = function(opts) {

        var experimentId = opts.experimentId;
        var arrayDesign = opts.arrayDesign;

        var assayProperties = this;
        var data = null;

        if (curatedProperties_ == null) {
            curatedProperties_ = $.extend(true, {}, curatedEFs, curatedSCs);
        }

        function processData(aData) {
            aData.allProperties = [];

            var toMerge = ["sampleCharacteristicValuesForPlot", "experimentalFactorValuesForPlot"];
            for (var j = 0; j < toMerge.length; j++) {
                var arr = aData[toMerge[j]];
                for (var i = 0; i < arr.length; i++) {
                    var d = arr[i];
                    d.assays = d.assayEfvs || d.assayScvs || [];
                    d.values = d.efvs || d.scvs || [];
                    aData.allProperties.push(d);

                    delete d.assayEfvs;
                    delete d.assayScvs;
                    delete d.scvs;
                    delete d.efvs;
                }
                delete aData[toMerge[j]];
            }

            return aData;
        }

        assayProperties.isEmpty = function() {
            return data == null;
        };

        assayProperties.load = function() {
            if (!experimentId) {
                atlasLog("ExperimentId (given " + experimentId + ") is required to load assay properties");
                return;
            }

            var url = "api/v0?";

            var params = [
                "experimentPageHeader",
                "experiment=" + experimentId,
                "format=json"
            ];
            if (arrayDesign) {
                params.push("hasArrayDesign=" + arrayDesign);
            }

            atlas.ajaxCall(url + params.join("&"), "", function(obj) {
                data = processData(obj.results[0]);
                $(assayProperties).trigger("dataDidLoad");
            });
        };

        assayProperties.forAssayIndex = function(assayIndex) {
            if (this.isEmpty()) {
                return;
            }

            var obj = [];
            var uniq = {};
            for (var i = 0; i < data.allProperties.length; i++) {
                var p = data.allProperties[i];
                var v = p.values[p.assays[assayIndex]];
                if (v == "" || v.toLowerCase() == "(empty)" || uniq[p.name] == v) {
                    continue;
                }
                obj.push([curatedProperties_[p.name] || p.name, v]);
                uniq[p.name] = v;
            }
            return obj;
        };
    }
}());

(function() {

    var ZoomControls = function(opts) {
        if (!(this instanceof arguments.callee)) {
            return new ZoomControls();
        }

        var initialized = false;

        var target = opts.target;
        var zoomin = target + "_zoomin";
        var zoomout = target + "_zoomout";
        var panright = target + "_panright";
        var panleft = target + "_panleft";

        var controls = this;
        controls.hide = function() {
          $("#" + target).hide();
        };

        controls.show = function() {
          $("#" + target).show();
        };

        draw({
           canZoom: opts.canZoom || false,
           canPan: opts.canPan || false
        });

        function draw(mode) {
            if (!mode.canZoom && !mode.canPan) {
                return;
            }

            if (!initialized) {
                drawZoomControls();
                bindEvents();
                initialized = true;
            }

            if (mode.canZoom) {
                $("#" + zoomin).show();
                $("#" + zoomout).show();
            } else {
                $("#" + zoomin).hide();
                $("#" + zoomout).hide();
            }

            if (mode.canPan) {
                $("#" + panright).show();
                $("#" + panleft).show();
            } else {
                $("#" + panright).hide();
                $("#" + panleft).hide();
            }
        }

        function drawZoomControls() {
            var contents = [
                '<div id="' + zoomin + '"  style="z-index:1; position:relative; left: 0; top: 5px;cursor:pointer;display:none;"><img style="cursor:pointer" src="images/zoomin.gif" title="Zoom in"></div>',
                '<div id="' + zoomout + '" style="z-index:1; position: relative; left: 0; top: 5px;cursor:pointer;display:none;"><img src="images/zoomout.gif" title="Zoom out"></div>',
                '<div id="' + panright + '" style="z-index:2;position: relative; left: 20px; top: -35px;cursor:pointer;display:none;"><img src="images/panright.gif" title="pan right"></div>',
                '<div id="' + panleft + '" style="z-index:2;position: relative; left: -15px; top: -69px;cursor:pointer;display:none;"><img src="images/panleft.gif" title="pan left"></div>'];

            $("#" + target).css({paddingLeft: 15});
            $("#" + target).html(contents.join(""));

            $("#" + zoomin + " > img").hover(
                    function() {
                        $(this).attr("src", "images/zoominO.gif");
                    },
                    function() {
                        $(this).attr("src", "images/zoomin.gif");
                    }).mousedown(function() {
                $(this).attr("src", "images/zoominC.gif");
            }).mouseup(function() {
                $(this).attr("src", "images/zoominO.gif");
            });

            $("#" + zoomout + " > img").hover(
                    function() {
                        $(this).attr("src", "images/zoomoutO.gif");
                    },
                    function() {
                        $(this).attr("src", "images/zoomout.gif");
                    }).mousedown(function() {
                $(this).attr("src", "images/zoomoutC.gif");
            }).mouseup(function() {
                $(this).attr("src", "images/zoomoutO.gif");
            });

            $("#" + panright + " > img").hover(
                    function() {
                        $(this).attr("src", "images/panrightO.gif");
                    },
                    function() {
                        $(this).attr("src", "images/panright.gif");
                    }).mousedown(function() {
                $(this).attr("src", "images/panrightC.gif");
            }).mouseup(function() {
                $(this).attr("src", "images/panrightO.gif");
            });

            $("#" + panleft + " > img").hover(
                    function() {
                        $(this).attr("src", "images/panleftO.gif");
                    },
                    function() {
                        $(this).attr("src", "images/panleft.gif");
                    }).mousedown(function() {
                $(this).attr("src", "images/panleftC.gif");
            }).mouseup(function() {
                $(this).attr("src", "images/panleftO.gif");
            });
        }

        function bindEvents() {
            $("#" + zoomin).bind("click", function() {
                triggerZoomInEvent();
            });

            $("#" + zoomout).bind("click", function() {
                triggerZoomOutEvent();
            });

            // zoom out completely on double click
            $("#" + zoomout).bind("dblclick", function() {
                triggerZoomOutEvent(true);
            });

            $("#" + panright + " > img").unbind("click");
            $("#" + panright + " > img").bind("click", function() {
                triggerPanRightEvent();
            });

            $("#" + panleft + " > img").unbind("click");
            $("#" + panleft + " > img ").bind("click", function() {
                triggerPanLeftEvent();
            });
        }

        function triggerZoomInEvent() {
            $(controls).trigger("zoomIn");
        }

        function triggerZoomOutEvent(completely) {
            $(controls).trigger("zoomOut", {completely: completely});
        }

        function triggerPanRightEvent() {
            $(controls).trigger("panRight");
        }

        function triggerPanLeftEvent() {
            $(controls).trigger("panLeft");
        }
    };

    var BasePlot = function(opts) {

        var plot = null;
        var overview = null;
        var data = null;

        var target = "#" + (opts.target || "");
        var targetOverview = "#" + (opts.targetOverview || "");
        var targetLegend = "#" + (opts.targetLegend || "");

        var initialWidth = $(target).width() || 500;
        var canZoom = opts.canZoom || false;
        var canPan = opts.canPan || false;

        var zoomControls = new ZoomControls({
            target : opts.targetZoomControls || null,
            canZoom: canZoom,
            canPan: canPan
        });

        subscribe(zoomControls);

        var publicPlot = this;


        publicPlot.adjustData = function(data) {
            return data;
        };

        publicPlot.adjustPlot = function(plot, data) {
            //overwrite
        };

        publicPlot.getMaxX = function() {
            if (data.series && data.series.length > 0) {
                var firstSeries = data.series[0];
                if (firstSeries.data && firstSeries.data.length > 0) {
                    return firstSeries.data[firstSeries.data.length - 1][0];
                }
            }
            return 0;
        };

        publicPlot.getMinX = function() {
            if (data.series && data.series.length > 0) {
                var firstSeries = data.series[0];
                if (firstSeries.data && firstSeries.data.length > 0) {
                    return firstSeries.data[0][0];
                }
            }
            return 0;
        };

        publicPlot.clear = function() {
            $(target).empty();
            $(targetOverview).empty();
            $(targetLegend).empty();

            zoomControls.hide();

            data = null;
        };

        publicPlot.update = function(newData) {
            zoomControls.show();

            data = publicPlot.adjustData(newData);

            var self = this;

            function refinePlotWidthAndSelection(width, plotData, selection) {
                var xRange = selection ? selection.xaxis : null;
                var numberOfPoints =
                        xRange ? Math.abs(xRange.from - xRange.to) :
                                self.getMaxX(plotData);


                xRange = xRange == null ? {from:self.getMinX(plotData), to:numberOfPoints} : xRange;

                var noScroll = true;

                var pxPerPoint = width / numberOfPoints;
                var minPx = 20, maxPx = 30, avPx = (minPx + maxPx) / 2;

                if (pxPerPoint < minPx) {
                    xRange = {from: xRange.from, to: xRange.from + ((xRange.to - xRange.from) * width / avPx / numberOfPoints)};
                    noScroll = false;
                }

                if (pxPerPoint > maxPx) {
                    width = maxPx * numberOfPoints + 60; //TODO: it looks like a hack
                }

                return {selection: {xaxis: xRange}, width: width, noScroll: noScroll};
            }

            //restore the original width
            $(target).width(initialWidth);
            $(targetOverview).width(initialWidth);

            $.extend(true, data.options,
            {
                legend: {
                    labelFormatter: function (label) {
                        return label.geneName + ":" + label.deAcc;
                    },
                    container: targetLegend,
                    show: true
                },
                yaxis: {
                    labelWidth:40
                },
                selection: {
                    mode: canZoom ? "x" : null
                },
                scroll: {
                    mode: canPan && !canZoom ? "x" : null
                }
            });

            if (canPan || canZoom) {
                var widthAndSelection = refinePlotWidthAndSelection($(target).width(), data);
                if (!canZoom && widthAndSelection.noScroll) {
                    $(target).width(widthAndSelection.width);
                    $(targetOverview).width(widthAndSelection.width);
                    $(targetOverview).css({visibility: "hidden"});
                } else {
                    $(targetOverview).css({visibility: "visible"});
                }
                createPlotOverview(widthAndSelection.selection);
            } else {
                createPlot();
            }
        };

        publicPlot.bindToolTip = function(target, data) {
            //override
        };

        publicPlot.createToolTip = function(name, convert) {
            var convertFunc = convert;
            var id = name + "_tooltip_" + (new Date()).getTime();
            $('<div id="' + id + '"/>').css({
                position: 'absolute',
                textAlign:'left',
                left: 0,
                top: 0,
                border: '1px solid #005555',
                margin: '0',
                padding: '10px',
                backgroundColor: '#EEF5F5',
                display: 'none',
                zIndex: '3020'
            }).appendTo("body");

            $("#" + id).mouseout(function() {
                hide();
            });

            var prevData = null;

            function hide() {
                prevData = null;
                $("#" + id).hide();
            }

            function show(x, y, data) {
                if (prevData == data) {
                    return;
                }

                prevData = data;

                var args = Array.prototype.slice.call(arguments, 3);
                var html = convertFunc ? convertFunc.apply(this, args) : "";
                if (html != null) {
                    $("#" + id).css({top: y + 5, left: x + 5});
                    $("#" + id).html(html).show();
                }
            }

            return  {
                hide: hide,
                show: show
            }
        };

        publicPlot.getSeries = function() {
            return plot ? plot.getData() : null;
        };

        function createPlotOverview(ranges) {
            overview = $.plot(targetOverview, data.series, $.extend(true, {}, data.options,
            {
                yaxis: {
                    ticks: 0,
                    labelWidth: 40
                    // min: -plotData.series[0].yaxis.datamax * 0.25
                },
                series:{
                    points:{
                        show: false
                    }
                },
                grid:{
                    backgroundColor:'#F2F2F2',
                    markings:null,
                    autoHighlight: false
                },
                legend:{
                    show:false
                },
                colors:['#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3']
            }));

            $(targetOverview + " #plotHeader").remove();

            /* {{{{{{{{{ Overview <-> Plot selection events }}}}}}}}}*/
            $(target).unbind("plotselected");
            $(target).bind("plotselected", function (event, ranges) {
                createPlot(ranges);
                // don't fire event on the overview to prevent eternal loop
                overview.setSelection(ranges, true);
            });

            $(targetOverview).unbind("plotselected");
            $(targetOverview).bind("plotselected", function (event, ranges) {
                createPlot(ranges);
            });


            /* {{{{{{{{{ Overview -> Plot scroll events }}}}}}}}}*/
            var self = this;
            $(targetOverview).unbind("plotscrolled");
            $(targetOverview).bind("plotscrolled", function (event, ranges) {
                createPlot(ranges);
            });

            setSelection(ranges);
        }

        function createPlot(ranges) {
            var o = data.options;
            if (ranges) {
                /*var min = 0.00001;
                 // clamp the zooming to prevent eternal zoom
                 if (ranges.xaxis.to - ranges.xaxis.from < min)
                 ranges.xaxis.to = ranges.xaxis.from + min;
                 if (ranges.yaxis.to - ranges.yaxis.from < min)
                 ranges.yaxis.to = ranges.yaxis.from + min;*/

                o = $.extend(true, {}, o,
                {
                    xaxis: {
                        min: ranges.xaxis.from,
                        max: ranges.xaxis.to
                    },
                    scroll: {
                        mode: null
                    }
                });
            }

            $.extend(true, o, {
                legend: {
                    noColumns: 3
                },

                headers: {
                    mode: "rotated",
                    rotate: -45,
                    maxMargin: 150
                }
            });

            plot = $.plot($(target), data.series, o);
            publicPlot.adjustPlot(plot, data);
            publicPlot.bindToolTip(target, data);

            $(targetLegend).css({paddingLeft: plot.getPlotOffset().left});
        }

        function subscribe(zoomControls) {
            $(zoomControls).bind("zoomIn", zoomIn);
            $(zoomControls).bind("zoomOut", zoomOut);
            $(zoomControls).bind("panLeft", panLeft);
            $(zoomControls).bind("panRight", panRight);
        }

        function unsubscribe(zoomControls) {
            $(zoomControls).unbind("zoomIn", zoomIn);
            $(zoomControls).unbind("zoomOut", zoomOut);
            $(zoomControls).unbind("panLeft", panLeft);
            $(zoomControls).unbind("panRight", panRight);
        }

        function panRight() {
            var selection = getSelection();

            if (selection == null) {
                return;
            }

            var max = overview.getXAxes()[0].max;

            var xrange = selection.xaxis;
            var w = xrange.to - xrange.from;

            var t = Math.min(xrange.to + 3, max);
            var f = t - w;

            setSelection({ xaxis: { from: f, to: t }});
        }

        function panLeft() {
            var selection = getSelection();

            if (selection == null) {
                return;
            }

            var min = overview.getXAxes()[0].min;

            var xrange = selection.xaxis;
            var w = xrange.to - xrange.from;

            var f = Math.max(xrange.from - 3, min);
            var t = f + w;

            setSelection({ xaxis: { from: f, to: t }});
        }

        function zoomIn() {
            if (scrollMode()) {
                return;
            }

            var selection = getSelection();
            var xaxes = overview.getXAxes()[0];

            var f,t,min,max,range,oldf,oldt;

            max = xaxes.max;
            min = xaxes.min;

            if (selection != null) {
                oldf = selection.xaxis.from;
                oldt = selection.xaxis.to;
                range = Math.floor(selection.xaxis.to - selection.xaxis.from);
            } else {
                range = max;
                oldt = max;
                oldf = min;
            }

            var windowSize = Math.floor(2 / 3 * range);
            var offset = Math.floor((range - windowSize) / 2);
            f = oldf + offset;
            t = Math.floor(oldt - offset);

            setSelection({ xaxis: { from: f, to: t }});
        }

        function zoomOut(completely) {
            if (scrollMode()) {
                return;
            }

            var selection = getSelection();
            var xaxes = overview.getXAxes()[0];

            var f,t,min,max,range,oldf,oldt;

            max = xaxes.max;
            min = xaxes.min;

            if (completely) {
                setSelection({ xaxis: { from: min, to: max }});
                clearSelection(true);
                return;
            }

            if (selection == null) {
                return;
            }

            oldf = selection.xaxis.from;
            oldt = selection.xaxis.to;
            range = Math.floor(oldt - oldf);

            var windowSize = Math.floor(3 / 2 * range);
            var offset = Math.max(Math.floor((windowSize - range) / 2), 2);
            f = Math.max(oldf - offset, min);
            t = Math.min(Math.floor(oldt + offset), max);

            setSelection({ xaxis: { from: f, to: t }});
            if (f == min && t == max) {
                clearSelection(true);
            }
        }

        function getSelection() {
            if (selectionMode()) {
                return overview.getSelection();
            }

            if (scrollMode()) {
                return overview.getScrollWindow();
            }
            return null;
        }

        function setSelection(ranges, preventEvent) {
            if (!ranges || !ranges.xaxis) {
                return;
            }

            if (selectionMode()) {
                overview.setSelection(ranges, preventEvent);
            }

            if (scrollMode()) {
                overview.setScrollWindow(ranges);
            }
        }


        function clearSelection(preventEvent) {
            if (selectionMode()) {
                overview.clearSelection(preventEvent);
            }
        }

        function selectionMode() {
            var o = data.options;
            return o.selection && o.selection.mode;
        }

        function scrollMode() {
            var o = data.options;
            return o.scroll && o.scroll.mode;
        }
    };

    function createLargePlot(opts) {

        var base = new BasePlot({
            target: "plot_large",
            targetOverview: "plot_overview_large",
            targetLegend: "legend_large",
            targetZoomControls: "zoomControls_large",
            canPan : true,
            canZoom : true
        });

        base.type = "large";

        var assayOrder = [];
        var assayProperties = opts.assayProperties || {};

        base.adjustData = function(obj) {
            assayOrder = obj.assayOrder || [];
            return obj;
        };

        base.bindToolTip = function(target, data) {
            var tooltip = base.createToolTip("lineplot",
                    function(dataIndex) {
                        if (assayProperties.isEmpty()) {
                            return null;
                        }

                        var assayIndex = assayOrder[dataIndex];
                        var assayProps = assayProperties.forAssayIndex(assayIndex);
                        var props = [];
                        for (var i = 0; i < assayProps.length; i++) {
                            props.push({pname:assayProps[i][0], pvalue:assayProps[i][1]});
                        }

                        var div = $("<div/>").append(
                                $.tmpl("plotTooltipTempl", {properties:props}));
                        return div.html();
                    });

            $(target).bind("plothover", (
                    function(event, pos, item) {
                        if (item) {
                            tooltip.show(item.pageX, item.pageY, item.dataIndex, item.dataIndex);
                        } else {
                            tooltip.hide();
                        }
                    }));

            $(target).bind('mouseleave', function () {
                tooltip.hide();
            });
        };
        return base;
    }

    function createBoxPlot(opts) {
        var base = new BasePlot({
            target: "plot_box",
            targetOverview: "plot_overview_box",
            targetLegend:"legend_box",
            targetZoomControls: "zoomControls_box",
            canZoom: false,
            canPan: true
        });

        base.type = "box";

        var data = null;

        base.adjustData = function(obj) {
            if (!obj || !obj.series || !obj.series.length) {
                return;
            }

            var x = 0;
            var step = obj.series.length;

            var markings = obj.options && obj.options.grid && obj.options.grid.markings ? obj.options.grid.markings : null;
            if (markings) {
                for (var k = 0; k < markings.length; k++) {
                    markings[k].xaxis = {from: x, to: x + step};
                    x += step;
                }
            }

            for (var i = 0; i < obj.series.length; i++) {
                var s = obj.series[i];
                s.points = {show: false};
                s.lines = {show: false};
                s.boxes = {show: true};
                s.color = parseInt(s.color);

                x = 0;
                for (var j = 0; j < s.data.length; j++) {
                    s.data[j].x = j * step + i;
                    x += step;
                }
            }

            obj.options.boxes = {hoverable: true};

            data = obj;

            return obj;
        };

        base.adjustPlot = function(plot, data) {
            var points = [];

            for (var i = 0; i < data.series.length; i++) {
                var s = data.series[i];
                for (var j = 0; j < s.data.length; j++) {
                    var d = s.data[j];
                    if (d.up || d.down) {
                        points.push({x:d.x, y:d.max, isUp: d.up ? true : false });
                    }
                }
            }
            plot.highlightUpDown(points);
        };

        base.getMaxX = function(plotData) {
            return plotData.series && plotData.series.length > 0 ?
                    plotData.series.length * plotData.series[0].data.length : 0;
        };

        base.getMinX = function(plotData) {
            return 0;
        };

        base.bindToolTip = function(target) {

            var tooltip = this.createToolTip("boxplot",
                    function(boxX) {
                        var step = data.series.length;
                        var i = Math.floor(boxX) % step;
                        var j = Math.floor(boxX / step);

                        var box = data.series[i].data[j];

                        var props = [];

                        var round = function(v) {
                            return Math.round(v * 100) / 100;
                        };

                        var titles = [
                            {p:"max", title: "Max", func: round},
                            {p:"uq", title: "Upper quartile", func: round},
                            {p:"median", title: "Median", func: round},
                            {p:"lq", title: "Lower quartile", func: round},
                            {p:"min", title: "Min", func: round},
                            {p:"up", title: "Expression", func: function(v) {
                                return v ? "up in " + v : null;
                            }},
                            {p:"down", title: "Expression", func: function(v) {
                                return v ? "down in " + v : null;
                            }}

                        ];

                        for (var p in titles) {
                            var t = titles[p];
                            var v = t.func(box[t.p]);
                            if (v != null) {
                                props.push({pname: t.title, pvalue: v});
                            }
                        }

                        var div = $("<div/>").append(
                                $.tmpl("plotTooltipTempl", {properties:props, title: box.id}));
                        return div.html();
                    });

            $(target).bind("boxhover", function(event, e, x) {
                tooltip.show(e.pageX, e.pageY, x, x);
            });

            $(target).bind("boxout", function(event) {
                tooltip.hide();
            });

            $(target).bind("mouseleave", function() {
                tooltip.remove();
            });

        };

        return base;
    }

    var ExperimentPlot = window.ExperimentPlot = function(experiment, assayProperties, plotType) {

        if (!(this instanceof arguments.callee)) {
            return new ExperimentPlot(experiment, assayProperties, plotType);
        }

        var _plots = {};
        var _plotType = plotType;
        var _exp = experiment;
        var _assayProperties = assayProperties;
        var _plotTypes = {box: createBoxPlot, large: createLargePlot};

        var _designElements = [];
        var _ef = null;
        var _seriesBackup = {};

        var _expPlot = this;

        _expPlot.load = load;
        _expPlot.addOrRemoveDesignElement = addOrRemoveDesignElement;
        _expPlot.changePlottingType = changePlottingType;
        _expPlot.getDesignElementColors = getDesignElementColors;

        $.template("plotTooltipTempl", [
            '<div style="margin:20px"><h3>${title}</h3><ul style="margin-left:0;padding-left:1em">',
            '{{each properties}}',
            '<li style="padding:0;margin:0"><span style="font-weight:bold">${pname}:</span>&nbsp;${pvalue}</li>',
            '{{/each}}',
            '</ul></div>'
        ].join(""));

        function getPlot() {
            var plot = _plots[_plotType];
            if (!plot) {
                plot = _plotTypes[_plotType]({assayProperties:_assayProperties});
                _plots[_plotType] = plot;
            }
            return plot;
        }

        function load(designElements, ef) {
            _designElements = designElements || _designElements;
            _ef = ef || _ef;

            getPlot().clear();

            var rawData = $('#expressionTableBody').data('json');
            if (!rawData || !rawData.results || !rawData.results[0].genePlots || _designElements.length == 0) {
                return;
            }

            rawData = rawData.results[0].genePlots;


            var genePlot = rawData[_ef][_plotType];
            var selectedSeries = [];

            var backupKey = _plotType + '||' + _ef;
            var allSeries = [].concat(_seriesBackup[backupKey] || [], genePlot.series);
            var deCopy = [].concat(_designElements);

            for (var i in allSeries) {
                var s = allSeries[i];
                for (var j in deCopy) {
                    var de = deCopy[j];
                    if (de.deId == s.label.deId) {
                        s.label = de;
                        selectedSeries.push(s);
                        deCopy.splice(j, 1);
                        break;
                    }
                }
            }
            _seriesBackup[backupKey] = selectedSeries;

            var dataToPlot = {};
            for (var p in genePlot) {
                dataToPlot[p] = genePlot[p];
            }
            dataToPlot.series = selectedSeries;

            if (dataToPlot) {
                getPlot().update(dataToPlot);
                drawEFpagination(_ef);
            }

            $(_expPlot).trigger("dataDidLoad");
        }

        function drawEFpagination(currentEF) {
            var experimentEFs = _exp.experimentFactors;

            var root = $('#EFpagination').empty();

            $.each(experimentEFs, function(i, ef) {
                if (ef != currentEF)
                    root.append($('<div/>').append($('<a/>').text(curatedEFs[ef]).click(function () {
                        _expPlot.load(null, ef);
                    })));
                else
                    root.append($('<div/>').text(curatedEFs[ef]).addClass('current'));
            });
        }

        function getDesignElementColors() {
            var colors = {};
            var series = getPlot().getSeries();
            for (var i in series) {
                var s = series[i];
                var deId = s.label.deId;
                colors[deId] = s.color;
            }
            return colors;
        }

        function addOrRemoveDesignElement(de) {
            for (var i in _designElements) {
                if (_designElements[i].deId == de.deId) {
                    removeDesignElement(de);
                    return;
                }
            }

            _designElements.push(de);

            _expPlot.load();
        }

        function removeDesignElement(de) {
            if (_designElements.length == 1)
                return;

            for (var i in _designElements) {
                if (_designElements[i].deId == de.deId) {
                    _designElements.splice(i, 1);
                    break;
                }
            }

            _expPlot.load();
        }

        function changePlottingType(type) {
            var arr = ["box","large"];
            for (var i = 0; i < arr.length; i++) {
                if (arr[i] == type) {
                    _plotType = type;
                    load();
                    return;
                }
            }
            atlasLog("unknown plot type: " + type);
        }
    };

    window.ExperimentPage = function(opts) {
        var _exp = opts.experiment || {};
        var _arrayDesign = opts.arrayDesign || null;
        var _offset = opts.offset || 0;
        var _limit = opts.limit || 10;

        var _designElements = [];

        var _expPlot = null;

        this.changePlotType = function(plotType) {
            _expPlot.changePlottingType(plotType);
        };

        this.addOrRemoveDesignElement = function(deId) {
            for (var i in _designElements) {
                var de = _designElements[i];
                if (de.deId == deId) {
                    _expPlot.addOrRemoveDesignElement(de);
                    return;
                }
            }
        };

        this.getApiLink = function(url) {
            var params = [];
            for (var i = 0; i < _designElements.length; ++i) {
                params.push('&geneIs=' + _designElements[i].geneIdentifier);
            }
            return url + params.join("");
        };

        init(opts);

        function init(opts) {
            initForm(opts);

            var assayProperties = new AssayProperties({
                experimentId: _exp.id,
                arrayDesign: _arrayDesign
            });

            $(assayProperties).bind("dataDidLoad", function() {
                initPlot(assayProperties);
                search();
            });

            assayProperties.load();
        }

        function initForm(opts) {
            $("#geneFilter").val(opts.gene || '');
            $('#efvFilter').val(opts.ef && opts.efv ? opts.ef + '||' + opts.efv : '');
            $('#updownFilter').val(opts.updown || '');

            $('#efvFilter, #updownFilter').change(function() {
                newSearch();
            });

            $('#expressionListFilterForm').bind('submit', function() {
                newSearch();
                return false;
            });
        }

        function initPlot(assayProperties) {
            _expPlot = new ExperimentPlot(_exp, assayProperties, "box");

            $(_expPlot).bind("dataDidLoad", function() {
                updateRowColors();
            });
        }

        function clearQuery() {
            $("#geneFilter").val('');
            $("#efvFilter").attr('selectedIndex', 0);
            $("#updownFilter").attr('selectedIndex', 0);
            $("#divErrorMessage").css("visibility", "hidden");
            newSearch();
        }

        function changePage(pageId) {
            _offset = pageId * _limit;
            submitQuery(processExpressionAnalysisOnly);
        }

        function newSearch() {
            _offset = 0;
            search();
        }

        function search() {
            updatePageLinks();
            submitQuery(process);
        }

        function getSearchParameters() {
            var p = {
                ef: '',
                efv: '',
                gid: $('#geneFilter').val(),
                updown: $('#updownFilter').val()
            };

            var efEfv = $('#efvFilter').val();
            if (efEfv) {
                var s = efEfv.split("||");
                p.ef = s[0];
                p.efv = s[1];
            }
            return p;
        }

        function submitQuery(callback) {
            var p = getSearchParameters();
            loadExpressionAnalysis(_exp.accession, _arrayDesign, p.gid, p.ef, '"' + p.efv + '"', p.updown, callback);
        }

        function loadExpressionAnalysis(expAccession, arrayDesign, gene, ef, efv, updn, callback) {
            $("#divErrorMessage").css("visibility", "hidden");

            $("#qryHeader").css("top", $("#squery").position().top + "px");
            $("#qryHeader").css("left", $("#squery").position().left + "px");
            $("#qryHeader").css("height", $("#squery").height() + "px");
            $("#qryHeader").css("width", $("#squery").width() + "px");

            $("#qryHeader").show();

            //TODO: __upIn__ workaround
            var dataUrl = "api/v0?experimentPage&experiment=" + expAccession +
                    (gene ? "&geneIs=" + gene : "") +
                    (arrayDesign ? "&hasArrayDesign=" + arrayDesign : "") +
                    (ef ? "&upIn" + ef + "=" + efv : "") +
                    (updn ? "&updown=" + updn : "") +
                    "&offset=" + (_offset + 1) + "&limit=" + _limit;

            atlas.ajaxCall(dataUrl, "", callback, function() {
                callback(null);
            });
        }

        function processExpressionAnalysisOnly(data) {
            process(data, true);
        }

        function process(data, expressionAnalysisOnly) {
            $("#qryHeader").hide();

            var eaItems = {};
            var eaTotalSize = 0;
            var geneToolTips = {};

            if (!data || !data.results || data.results.length == 0) {
                $("#divErrorMessage").css("visibility", "visible");
                $("#expressionTableBody").empty();
                data = null;
            } else {
                var res = data.results[0];
                eaItems = res.expressionAnalyses.items;
                eaTotalSize = res.expressionAnalyses.totalSize;
                geneToolTips = res.geneToolTips;
                $('#arrayDesign').html(res.arrayDesign);
                _arrayDesign = res.arrayDesign;
            }

            $("#expressionTableBody").data('json', data);

            var eAs = [];
            _designElements = [];

            for (var i in eaItems) {
                var ea = eaItems[i];
                eAs.push({
                    deId: ea.deid,
                    geneName: ea.geneName,
                    geneId: ea.geneId,
                    geneIdentifier: ea.geneIdentifier,
                    de: ea.designElementAccession,
                    ef: curatedEFs[ea.ef] || ea.ef,
                    ef_enc: encodeURIComponent(encodeURIComponent(ea.ef)).replace(/_/g, '%5F'),
                    rawef: ea.ef,
                    efv: ea.efv,
                    efv_enc: encodeURIComponent(encodeURIComponent(ea.efv)).replace(/_/g, '%5F'),
                    pvalue: ea.pvalPretty,
                    tstat: ea.tstatPretty,
                    expr: ea.expression
                });

                _designElements.push({
                    deId: ea.deid,
                    deAcc: ea.designElementAccession,
                    deIndex: ea.deidx,
                    geneId: ea.geneId,
                    geneName: ea.geneName,
                    geneIdentifier: ea.geneIdentifier
                });
            }

            drawTable(eAs);
            initExpressionAnalysisTooltips(geneToolTips);

            if (expressionAnalysisOnly) {
                updateRowColors();
            } else {
                drawPagination(eaTotalSize);

                var ef = eaItems.length > 0 ? eaItems[0].ef : null;
                drawPlot(_designElements.slice(0, 3), ef);
            }
        }

        function drawTable(expressionValues) {
            $("#expressionValueTableRowTemplate1").tmpl(expressionValues).appendTo($("#expressionTableBody").empty());
        }

        function drawPagination(total) {
            var targets = ["#topPagination"];
            for (var i in targets) {
                var target = targets[i];

                $(target).empty();

                if (total > _limit) {
                    $(target).pagination(total, {
                        num_edge_entries: 2,
                        num_display_entries: 5,
                        items_per_page: _limit,
                        current_page: _offset / _limit,
                        callback: function(pageId) {
                            changePage(pageId);
                            return false;
                        }
                    });
                }
            }
        }

        function drawPlot(designElements, ef) {
            _expPlot.load(designElements, ef);
        }

        function initExpressionAnalysisTooltips(geneToolTips) {
            var toolTips = {};
            for (var i in geneToolTips) {
                var toolTip = geneToolTips[i];
                toolTips[toolTip.name] = toolTip;
            }

            $("#squery td.genename a").tooltip({
                bodyHandler: function () {
                    return $("#geneToolTipTemplate").tmpl(toolTips[this.text]);
                },
                showURL: false
            });


            $("#squery td.wiggle a").tooltip({
                bodyHandler: function () {
                    return "View in the Ensembl Genome Browser (new window)";
                },
                showURL: false
            });
        }

        function updateRowColors() {
            var deColors = _expPlot.getDesignElementColors();
            $(".designElementRow").each(function() {
                var el = $(this);
                var deId = el.attr('id').split("_")[1];
                var color = deColors[deId];
                if (color) {
                    $("#results_" + deId).css({backgroundColor: color});
                    var img = $("#results_" + deId + " img")[0];
                    img.src = "images/chart_line_delete.png";
                    img.title = "remove from plot";
                } else {
                    $("#results_" + deId).css({backgroundColor: "white"});
                    var img = $("#results_" + deId + " img")[0];
                    img.src = "images/chart_line_add.png";
                    img.title = "add to plot";
                }
            });
        }

        function updatePageLinks() {
            $("a.experimentLink").each(function() {
                var el = $(this);
                var href = el.attr("href");
                el.attr("href", enhanceUrlParameters(href));
            })
        }

        function enhanceUrlParameters(url) {
            var params = getSearchParameters();
            var paramString = [];
            for(var p in params) {
                var v = params[p];
                url = url.replace(new RegExp("&?" + p + "=([^&$]*)"), "");
                if (v) {
                    paramString.push(p + "=" + encodeURIComponent(v));
                }
            }
            var s = url.split("?");
            return  s[0] + "?" + (s.length > 1 ? s[1] + "&" : "") + paramString.join("&");
        }
    }
}());

var curatedSCs = {};
var curatedEFs = {};



