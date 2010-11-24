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

        var events = [
                "dataDidLoad"
        ];

        for(var e = 0; e<events.length; e++) {
            assayProperties[events[e]] = function(){};
        }

        if (curatedProperties_ == null) {
            curatedProperties_ = $.extend(true, {}, curatedEFs, curatedSCs);
        }

        function processData(aData) {
            function rleDecode(inArray) {

                inArray = inArray || [];
                var outArray = [];
                for (var i = 0; i < inArray.length; i++) {
                    var a = inArray[i];
                    if (a.length == 0) {
                        continue;
                    }

                    var m = a[0];
                    var n = a.length > 1 ? a[1] : 1;
                    for (var j = 0; j < n; j++) {
                        outArray.push(m);
                    }
                }
                return outArray;
            }

            aData.allProperties = [];

            var toMerge = ["sampleCharacteristicValuesForPlot", "experimentalFactorValuesForPlot"];
            for (var j = 0; j < toMerge.length; j++) {
                var arr = aData[toMerge[j]];
                for (var i = 0; i < arr.length; i++) {
                    var d = arr[i];
                    d.assays = rleDecode(d.assayEfvsRLE || d.assayScvsRLE);
                    d.values = (d.scvs ? d.scvs : d.efvs) || [];
                    aData.allProperties.push(d);

                    delete d.assayEfvsRLE;
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
            if (!experimentId || !arrayDesign) {
                atlasLog("ExperimentId (= " + experimentId + ") and arrayDesign (=" + arrayDesign + ") are requred to load assay properties");
                return;
            }

            var url = "api?";

            var params = [];
            params.push("experimentPageHeader");
            params.push("indent");
            params.push("experiment=" + experimentId);
            params.push("format=json");
            params.push("hasArrayDesign=" + arrayDesign);

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


        /*
         * A test to compare old and new assayProperties implementations; TODO: remove it as it becomes useless...
         */
        assayProperties.test = function(properties, order) {

            function extractProperties(props, dataIndex) {
                var keys = {}, obj = [];

                var efvs = props[dataIndex].efvs;
                if (efvs.k != undefined && efvs.v != undefined) {
                    obj.push([curatedEFs[efvs.k], efvs.v]);
                    keys[efvs.k] = efvs.v;
                }

                var scvs = props[dataIndex].scvs;
                for (var i = 0; i < scvs.length; ++i) {
                    if (scvs[i].v != '' && keys[scvs[i].k] != scvs[i].v) {
                        obj.push([curatedSCs[scvs[i].k], scvs[i].v]);
                    }
                }
                return obj;
            }

            function testLog(name, v1, v2, success, index) {
                if (success) {
                    return;
                }
                $("#testResults tbody").append("<tr><td>" + index + "</td><td>" + name + "</td><td>" + v1 + "</td><td>" + v2 + "</td></tr>");
            }

            function compareObjects(obj1, obj2, index) {
                testLog("size", obj1.length, obj2.length,  obj1.length == obj2.length, index);

                for(var i=0; i<obj1.length; i++) {
                    var ob1 = obj1[i];
                    var ob2 = i < obj2.length ? obj2[i] : null;
                    if (!ob2) {
                        testLog(ob1[0], ob1[1], "absence", false, index);
                        continue;
                    }

                    if (ob1[0]) {
                        ob1[0] = ob1[0].toLowerCase();
                    }

                    if (ob2[0]) {
                        ob2[0] = ob2[0].toLowerCase();
                    }

                    if (ob1[0] != ob2[0]) {
                        testLog("property order", ob1[0], ob2[0], false, index);
                        continue;
                    }


                    testLog(ob1[0], ob1[1], ob2[1], ob1[1] == ob2[1], index);
                }

            }

            function sortAlphabetically(arr) {
                function compareStrings(s1, s2) {
                    if (s1 == s2) {
                        return 0;
                    }

                    if (s1 == undefined) {
                        return 1;
                    }

                    if (s2 == undefined) {
                        return -1;
                    }
                    return (s1 > s2 ? 1 : -1);
                }

                return arr.sort(function(o1, o2) {
                    var res = compareStrings(o1[0], o2[0]);
                    return res == 0 ?
                            compareStrings(o1[1], o2[1]) : res;
                });
            }

            $("body").append('<div><table id="testResults"><tbody></tbody></table></div>');

            for (var i = 0; i < properties.length; i++) {
                var assayIndex = order[i];
                var arr1 = sortAlphabetically(this.forAssayIndex(assayIndex));
                var arr2 = sortAlphabetically(extractProperties(properties, i));
                compareObjects(arr1, arr2, i);
            }

        }
    }

}());


(function() {

    var BarPlotType = function() {
        return {
            name: "large",
            canPan: true,
            canZoom: true,
            onload: function(obj) {
                if (!obj || !obj.series || !obj.series.length) {
                    return;
                }

                obj.options.headers = {
                    mode: "rotated",
                    rotate: -45,
                    maxMargin: 100
                };
            }

        };
    };

    var BoxPlotType = function() {
        return {
            name: "box",
            canPan: true,
            canZoom: false,
            oncreate: function(plot, plotData) {
                var points = [];

                for (var i = 0; i < plotData.series.length; i++) {
                    var s = plotData.series[i];
                    for (var j = 0; j < s.data.length; j++) {
                        var d = s.data[j];
                        if (d.isUp || d.isDown) {
                            points.push({x:d.x, y:d.max, isUp: d.isUp});
                        }
                    }
                }
                plot.highlightUpDown(points);
            },
            
            onload: function(obj) {
                if (!obj || !obj.series || !obj.series.length) {
                    return;
                }


                var x = 0;
                var step = obj.series.length;

                var markings = obj.options && obj.options.grid && obj.options.grid.markings ? obj.options.grid.markings : null;
                for(var k=0; k<markings.length; k++) {
                    markings[k].xaxis = {from: x, to: x + step};
                    x += step;
                }

                for (var i = 0; i < obj.series.length; i++) {
                    var s = obj.series[i];
                    s.points = {show: false};
                    s.lines = {show: false};
                    s.boxes = {show: true};
                    s.color = parseInt(s.color);

                    x = 0;
                    for (var j=0; j< s.data.length; j++) {
                        s.data[j].x = j*step + i;
                        x += step;
                    }
                }

                obj.options.headers = {
                    mode: "rotated",
                    rotate: -45,
                    maxMargin: 100
                };

                obj.options.boxes = {hoverable: true};
            }
        };
    };

    var plotTypes = {
        large: BarPlotType,
        box: BoxPlotType
    };

    var ZoomControls = function(expPlot) {
        if (!(this instanceof arguments.callee)) {
            return new ZoomControls(_expPlot);
        }

        var initialized = false;
        var controls = this;

        controls.setSelection = setSelection;
        controls.getSelection = getSelection;
        controls.clearSelection = clearSelection;
        controls.zoomIn = zoomIn;
        controls.zoomOut = zoomOut;
        controls.panLeft = panLeft;
        controls.panRight = panRight;
        controls.redraw = redraw;

        function redraw() {
            if (!initialized) {
                drawZoomControls();
                bindZooming();
                initialized = true;
            }

            var zoomAllowed = !scrollMode();
            var panAllowed = true;

            if (zoomAllowed) {
                $("#zoomin").show();
                $("#zoomout").show();
            } else {
                $("#zoomin").hide();
                $("#zoomout").hide();
            }

            if (panAllowed) {
                $("#panright").show();
                $("#panleft").show();
            } else {
                $("#panright").hide();
                $("#panleft").hide();
            }
        }

        function getSelection() {
            if (selectionMode()) {
                return expPlot.getOverview().getSelection();
            }

            if (scrollMode()) {
                return expPlot.getOverview().getScrollWindow();
            }
            return null;
        }

        function setSelection(ranges, preventEvent) {
            if (!ranges || !ranges.xaxis) {
                return;
            }

            if (selectionMode()) {
                expPlot.getOverview().setSelection(ranges, preventEvent);
            }

            if (scrollMode()) {
                expPlot.getOverview().setScrollWindow(ranges);
            }

        }

        function clearSelection(preventEvent) {
            if (selectionMode()) {
                expPlot.getOverview().clearSelection(preventEvent);
            }
        }

        function panRight() {
            var selection = this.getSelection();

            if (selection == null) {
                return;
            }

            var max = expPlot.getOverview().getXAxes()[0].max;

            var xrange = selection.xaxis;
            var w = xrange.to - xrange.from;

            var t = Math.min(xrange.to + 3, max);
            var f = t - w;

            controls.setSelection({ xaxis: { from: f, to: t }});
        }

        function panLeft() {
            var selection = controls.getSelection();

            if (selection == null) {
                return;
            }

            var min = expPlot.getOverview().getXAxes()[0].min;

            var xrange = selection.xaxis;
            var w = xrange.to - xrange.from;

            var f = Math.max(xrange.from - 3, min);
            var t = f + w;

            controls.setSelection({ xaxis: { from: f, to: t }});
        }

        function zoomIn() {
            if (scrollMode()) {
                return;
            }

            var selection = controls.getSelection();
            var xaxes = expPlot.getOverview().getXAxes()[0];

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

            controls.setSelection({ xaxis: { from: f, to: t }});
        }

        function zoomOut(completely) {
            if (scrollMode()) {
                return;
            }

            var selection = controls.getSelection();
            var xaxes = expPlot.getOverview().getXAxes()[0];

            var f,t,min,max,range,oldf,oldt;

            max = xaxes.max;
            min = xaxes.min;

            if (completely) {
                controls.setSelection({ xaxis: { from: min, to: max }});
                controls.clearSelection(true);
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

            controls.setSelection({ xaxis: { from: f, to: t }});
            if (f == min && t == max) {
                controls.clearSelection(true);
            }
        }


        function selectionMode() {
            var o = expPlot.getOptions();
            return o.selection && o.selection.mode;
        }

        function scrollMode() {
            var o = expPlot.getOptions();
            return o.scroll && o.scroll.mode;
        }

        function drawZoomControls() {
            var contents = [
                '<div id="zoomin"  style="z-index:1; position:relative; left: 0; top: 5px;cursor:pointer;display:none;"><img style="cursor:pointer" src="images/zoomin.gif" title="Zoom in"></div>',
                '<div id="zoomout" style="z-index:1; position: relative; left: 0; top: 5px;cursor:pointer;display:none;"><img src="images/zoomout.gif" title="Zoom out"></div>',
                '<div id="panright" style="z-index:2;position: relative; left: 20px; top: -35px;cursor:pointer;display:none;"><img src="images/panright.gif" title="pan right"></div>',
                '<div id="panleft" style="z-index:2;position: relative; left: -15px; top: -69px;cursor:pointer;display:none;"><img src="images/panleft.gif" title="pan left"></div>'];

            $("#zoomControls").css({paddingLeft: 15});
            $("#zoomControls").html(contents.join(""));

            $("#zoomin > img").hover(
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

            $("#zoomout > img").hover(
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

            $("#panright > img").hover(
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

            $("#panleft > img").hover(
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

        function bindZooming() {
            $("#zoomin").show();
            $("#zoomout").show();

            $("#zoomin").bind("click", function() {
                controls.zoomIn();
            });

            $("#zoomout").bind("click", function() {
                controls.zoomOut();
            });

            // zoom out completely on double click
            $("#zoomout").bind("dblclick", function() {
                controls.zoomOut(true);
            });

            $("#panright > img").unbind("click");
            $("#panright > img").bind("click", function() {
                controls.panRight();
            });

            $("#panleft > img").unbind("click");
            $("#panleft > img ").bind("click", function() {
                controls.panLeft();
            });
        }
    };

    var ExperimentPlot = window.ExperimentPlot = function(target_, plotType_) {

        if (!(this instanceof arguments.callee)) {
            return new ExperimentPlot(target_, plotType_);
        }

        var plotType = plotTypes[plotType_ || "box"]();
        var plotData = {};

        var plot = null;
        var overview = null;

        var assayOrder = [];
        var prevSelections = {};

        var target = target_;
        var targetThm = target_ + "_overview";
        var targetLgd = "#legend";

        var plotWidth = $(target).width() || 500;
        
        var plotControls = new ZoomControls(this);
        var plotLegend = {};
        var plotSelection = null;

        var expPlot = this;

        expPlot.getOverview = function() {
            return overview
        };
        expPlot.getOptions = function() {
            return plotData.options
        };
        expPlot.reload = reload;
        expPlot.addDesignElementToPlot = addDesignElementToPlot;
        expPlot.removeDesignElementFromPlot = removeDesignElementFromPlot;
        expPlot.changePlottingType = changePlottingType;

        $.template("plotTooltipTempl", [
            '<div style="margin:20px"><h3>${title}</h3><ul style="margin-left:0;padding-left:1em">',
            '{{each properties}}',
            '<li style="padding:0;margin:0"><span style="font-weight:bold">${pname}:</span>&nbsp;${pvalue}</li>',
            '{{/each}}',
            '</ul></div>'
        ].join(""));

        init();

        function init() {
            load();
            bindPlotEvents();
       }

        function load() {
            var allPlots = $('#expressionTableBody').data('json').results[0].genePlots;
            if (!allPlots) {
                atlasLog("Error: No data to plot");
                return;
            }

            if (!currentEF) {
                for (var key in allPlots) {
                    if (allPlots.hasOwnProperty(key)) {
                        currentEF = key;
                        break;
                    }
                }
            }

            var series = [];
            var genePlot = allPlots[currentEF][plotType.name];

            for (var i = 0; i < genePlot.series.length; i++) {
                var s = genePlot.series[i];
                for (g in designElementsToPlot) {
                    if (designElementsToPlot[g].deId == s.label.deId) {
                        series.label = designElementsToPlot[g];
                        series.push(s);
                        break;
                    }
                }
            }

            var dataToPlot = {};
            for(var p in genePlot) {
                dataToPlot[p] = genePlot[p];
            }
            dataToPlot.series = series;

            onload(dataToPlot);
        }

        function onload(dataToPlot) {
            if (plotType.onload) {
                plotType.onload(dataToPlot);
            }

            updatePlot(dataToPlot);
            drawEFpagination();
        }

        function updatePlot(dataToPlot) {

            function refinePlotWidthAndSelection(target, plotData, selection) {
                var xRange = selection ? selection.xaxis : null;
                var numberOfPoints =
                        xRange ? Math.abs(xRange.from - xRange.to) :
                        plotData.series.length * plotData.series[0].data.length;


                xRange = xRange == null ? {from:0, to:numberOfPoints} : xRange;

                var width = $(target).width();
                var noScroll = true;

                var pxPerPoint = width/numberOfPoints;
                var minPx = 15, maxPx = 30, avPx = (minPx + maxPx) / 2;

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
            $(target).width(plotWidth);
            $(targetThm).width(plotWidth);

            plotData = dataToPlot;

            $.extend(true, plotData.options,
            {
                legend: {
                    labelFormatter: function (label) {
                        return label.geneName + ":" + designElementIdToAccession[label.deId];
                    },
                    container: targetLgd,
                    show: true
                },
                yaxis: {
                    labelWidth:40
                },
                selection: {
                    mode: plotType.canZoom ? "x" : null
                },
                scroll: {
                    mode: plotType.canPan && !plotType.canZoom ? "x" : null   
                }
            });

            if (plotType.canPan || plotType.canZoom) {
                var widthAndSelection = refinePlotWidthAndSelection(target, plotData);
                if (!plotType.canZoom && widthAndSelection.noScroll) {
                    $(target).width(widthAndSelection.width);
                    $(targetThm).width(widthAndSelection.width);
                    $(targetThm).css({visibility: "hidden"});
                } else {
                    $(targetThm).css({visibility: "visible"});
                }
                createPlotOverview(widthAndSelection.selection);
            } else {
                createPlot();
            }

            if (prevSelections.length > 0) {
                // remap selections according to current assay order
                for (var si = 0; si < prevSelections.length; ++si) {
                    var selAssay = assayOrder[prevSelections[si]];
                    for (var ai = 0; ai < plotData.assayOrder.length; ++ai) {
                        if (plotData.assayOrder[ai] == selAssay) {
                            prevSelections[si] = ai;
                            break;
                        }
                    }
                }

                for (var j = 0; j < plot.getData().length; j++) {
                    for (var i = 0; i < prevSelections.length; ++i) {
                        plot.highlight(j, prevSelections[i]);
                    }
                }
            }

            assayOrder = plotData.assayOrder || [];

            /*if (jsonObj.assayProperties) {
                assayProperties.test(jsonObj.assayProperties, jsonObj.assayOrder);
            }*/

            populateSimMenu(plotData.simInfo);
        }

        function updatePlotLegend(series) {
            var newLegend = {};
            for(var i=0; i<series.length; i++) {
                var s = series[i];
                var deId = s.label.deId;
                $("#results_" + deId).css({backgroundColor: s.color});
                var img = $("#results_" + deId + " img")[0];
                img.src = "images/chart_line_delete.png";
                img.title = "remove from plot";
                if (plotLegend[deId]) {
                    delete plotLegend[deId];
                }
                newLegend[deId] = true;
            }

            for(var p in plotLegend) {
                $("#results_" + p).css({backgroundColor: "white"});
                var img = $("#results_" + p + " img")[0];
                img.src = "images/chart_line_add.png";
                img.title = "add to plot";
            }

            plotLegend = newLegend;
        }

        function createPlot(ranges) {
            var o = plotData.options;
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
                }
            });

            plot = $.plot($(target), plotData.series, o);

            if (plotType.oncreate) {
                plotType.oncreate(plot, plotData);
            }

            $(targetLgd).css({paddingLeft: plot.getPlotOffset().left});

            updatePlotLegend(plot.getData());

            $(".rmButton").hover(function() {
                $(this).attr("src", "images/closeButtonO.gif");
            }, function() {
                $(this).attr("src", "images/closeButton.gif");
            }).click(function() {
                expPlot.removeDesignElementFromPlot($(this).attr('id').substring(6));
            });
        }

        function createPlotOverview(ranges) {

            overview = $.plot($(targetThm), plotData.series, $.extend(true, {}, plotData.options,
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

            $(targetThm + " #plotHeader").remove();

            /* {{{{{{{{{ Overview <-> Plot selection events }}}}}}}}}*/
            $(target).unbind("plotselected");
            $(target).bind("plotselected", function (event, ranges) {
                createPlot(ranges);
               // don't fire event on the overview to prevent eternal loop
                overview.setSelection(ranges, true);
            });

            $(targetThm).unbind("plotselected");
            $(targetThm).bind("plotselected", function (event, ranges) {
                plot.setSelection(ranges);
            });


            /* {{{{{{{{{ Overview -> Plot scroll events }}}}}}}}}*/
            $(targetThm).unbind("plotscrolled");
            $(targetThm).bind("plotscrolled", function (event, ranges) {
                createPlot(ranges);
            });

            plotControls.redraw();
            plotControls.setSelection(ranges);
        }


        function clearSelections() {
            for (var j = 0; j < plot.getData().length; j++)
                for (var i = 0; i < prevSelections.length; ++i)
                    plot.unhighlight(j, prevSelections[i]);
            prevSelections = [];
        }

        function populateSimMenu(simInfo) {
            $("#simSelect").empty();

            if (!simInfo) {
                return;
            }

            for (var i = 0; i < simInfo.length; i++) {
                var key = simInfo[i].deId + "_" + simInfo[i].adId;
                $("#simSelect").append($('<option/>').val(key).text(simInfo[i].name));
            }
            $("#simSelect").selectOptions("select gene", true);
        }

        function createPlotTooltip(name, convert) {
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
        }

        function bindPlotEvents() {
            $(target).bind("mouseleave", function() {
                $("#tooltipPlot").remove();
            });

            $(target).bind("plotclick", function (event, pos, item) {
                if (!item) {
                    return;
                }

                clearSelections();

                var pointIndex = Math.round(item.datapoint[0] - 0.5);
                for (var geneIndex = 0; geneIndex < plot.getData().length; ++geneIndex) {
                    plot.highlight(geneIndex, pointIndex);
                }

                prevSelections = [pointIndex];
            });

            var boxplotTooltip = createPlotTooltip( "boxplot",
                    function(boxX) {
                        var step = plotData.series.length;
                        var i = Math.floor(boxX) % step;
                        var j = Math.floor(boxX / step);

                        var box = plotData.series[i].data[j];

                        var props = [];

                        var round = function(v) {
                            return Math.round(v * 100)/100;
                        };

                        var titles = [
                            {p:"max", title: "Max", func: round},
                            {p:"uq", title: "Upper quertile", func: round},
                            {p:"median", title: "Median", func: round},
                            {p:"lq", title: "Lower quartile", func: round},
                            {p:"min", title: "Min", func: round},
                            {p:"isUp", title: "Up/Down Expression", func: function(v) {
                                return v ? "Up" : null;
                            }},
                            {p:"isDown", title: "Expression", func: function(v) {
                                return v ? "Down" : null;
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
                boxplotTooltip.show(e.pageX, e.pageY, x, x);
            });

            $(target).bind("boxout", function(event) {
                boxplotTooltip.hide();
            });

            var lineplotTooltip = createPlotTooltip( "lineplot",
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
                            lineplotTooltip.show(item.pageX, item.pageY, item.dataIndex, item.dataIndex);
                        } else {
                            lineplotTooltip.hide();
                        }
                   }));
        }

        function drawEFpagination() {
            var root = $('#EFpagination').empty();
            $.each(experimentEFs, function(i, ef) {
                if (ef != currentEF)
                    root.append($('<div/>').append($('<a/>').text(curatedEFs[ef]).click(function () {
                        currentEF = ef;
                        expPlot.reload();
                    })));
                else
                    root.append($('<div/>').text(curatedEFs[ef]).addClass('current'));
            });
        }

        function reload(completely) {
            if (completely) {
                plotLegend = {};
            }
            $(target).html('');
            $(targetThm).html('');
            $(targetLgd).html('');
            load();
        }

        function addDesignElementToPlot(deId, geneId, geneIdentifier, geneName, ef) {
            for (var i = 0; i < designElementsToPlot.length; ++i) {
                if (designElementsToPlot[i].deId == deId) {
                    removeDesignElementFromPlot(deId);
                    return;
                }
            }

            designElementsToPlot.push({deId: deId, geneId: geneId, geneIdentifier: geneIdentifier, geneName: geneName});
            currentEF = ef;

            expPlot.reload();
        }

        function removeDesignElementFromPlot(deId) {

            if (designElementsToPlot.length == 1)
                return;

            for (var i = 0; i < designElementsToPlot.length; i++) {
                if (designElementsToPlot[i].deId == deId) {
                    designElementsToPlot.splice(i, 1);
                    break;
                }
            }

            expPlot.reload();
        }

        function changePlottingType(type) {
            if (plotTypes[type]) {
                plotType = plotTypes[type]();
                expPlot.reload();
            } else {
                  atlasLog("unknown plot type: " + type);
            }
        }

    };
}());

var designElementsToPlot = [];
var currentEF = [];
var experiment = {};
var curatedSCs = {};
var curatedEFs = {};
var experimentEFs = [];

var bindTable = (function() {
    var currentPage = null;

    return function(page) {
        currentPage = page;

        //var qry = $("#geneInExp_qry").fullVal();

        //var qry1 = $("#searchForm").serialize();

        //$("#qryHeader").html("<img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Loading...");
        //$("#qryResult").load(atlas.homeUrl + "expGenes?" + qry1, function() {
        //    $("#qryHeader").hide();
        //    addGeneToolTips();
        //});

        //showExpressionTable();

        return false;
    }
}());

var assayProperties = null;

function loadData(experiment, arrayDesign, gene, ef, efv, updn) {

    if (! assayProperties) {
        assayProperties = new AssayProperties({
            experimentId: experiment,
            arrayDesign: arrayDesign
        });

        $(assayProperties).bind("dataDidLoad", function() {
            showExpressionTable(experiment, gene, ef, efv, updn);
        });

        assayProperties.load();
    } else {
        showExpressionTable(experiment, gene, ef, efv, updn);
    }
}

function showExpressionTable(experiment, gene, ef, efv, updn) {
    $("#qryHeader").html("<img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Loading...");
    $("#qryHeader").show();

    //alert($("#squery").position.top);
    //alert($("#squery").position.width);

    $("#divErrorMessage").css("visibility","hidden");

    $("#qryHeader").css("top",$("#squery").position().top + "px");
    $("#qryHeader").css("left",$("#squery").position().left + "px");
    $("#qryHeader").css("height",$("#squery").height() + "px");
    $("#qryHeader").css("width",$("#squery").width() + "px");

    var updnFilter = "&updownIn";

    if(updn=='UP') updnFilter= "&upIn";
    if(updn=='DOWN') updnFilter="&downIn";

    var dataUrl = "api?experimentPage&experiment=" + experiment
            + (gene != '' ? "&geneIs=" + gene : '')
            + "&hasArrayDesign=" + arrayDesign
            + (ef != '' && efv != '' ? updnFilter + ef + '=' + efv : '')
            + (ef != '' && efv == '' ? updnFilter + ef + '=' : '')
            + (ef == '' && efv != '' ? updnFilter + efv.split("||")[0] + '=' + efv.split("||")[1]: '');

    if(window.console != undefined)
      console.log(dataUrl);

    atlas.ajaxCall(dataUrl,"", function(data) {
        var plotGeneCounter = 3;
        var r = [];

        if(null == data.results[0]){
            alert("data.results[0]");
            return;
        }

        if(null == data.results[0].expressionAnalyses){
            alert("data.results[0].expressionAnalyses");
            return;
        }

        if(0 == data.results[0].expressionAnalyses.length){
            errorHandler();
            return;
        }

        currentEF = null;

        for(var eaIdx in data.results[0].expressionAnalyses) {
            var ea = data.results[0].expressionAnalyses[eaIdx];
            r.push({
                 deId: ea.deid,
             geneName: ea.geneName,
               geneId: ea.geneId,
       geneIdentifier: ea.geneIdentifier,
                   de: ea.designElementAccession,
                   ef: curatedEFs[ea.ef],
                rawef: ea.ef,
                  efv: ea.efv,
               pvalue: ea.pvalPretty,
                tstat: ea.tstatPretty,
                 expr: ea.expression
            });

            designElementIdToAccession[ea.deid] = ea.designElementAccession;
            if (!currentEF) {
                currentEF = ea.ef;
            }

            if(plotGeneCounter-- > 0)
              designElementsToPlot.push({deId:ea.deid, geneId: ea.geneId, geneIdentifier:ea.geneIdentifier, geneName: ea.geneName});
        }

        showTable(r);
        $('#expressionTableBody').data('json', data);
        drawPlot();

        for(var i in data.results[0].geneToolTips){
            var toolTip = data.results[0].geneToolTips[i];
            geneToolTips[toolTip.name] = toolTip;
        }
        addGeneToolTips();

        $("#qryHeader").hide();
    }
    //forth parameter - errorFunc
    ,function(error){
        errorHandler();
    })
}

function errorHandler(){
    $("#divErrorMessage").css("visibility","visible");
    $("#expressionTableBody").empty();
    //alert(error);
    $("#qryHeader").hide();
}

function showTable(expressionValues){
    $("#expressionValueTableRowTemplate1").tmpl(expressionValues).appendTo($("#expressionTableBody").empty());
}

function defaultQuery(){
    $("#geneFilter").val('');
    $("#efvFilter").attr('selectedIndex', 0);
    $("#updownFilter").attr('selectedIndex', 0);
    $("#divErrorMessage").css("visibility","hidden");
    loadData(experiment.accession, arrayDesign, '', '', '', '');
}

function filteredQuery(){
    loadData(experiment.accession, arrayDesign, $('#geneFilter').val(), '', $('#efvFilter').val(), $('#updownFilter').val());
}

function bindGeneMenus() {
    $("#gene_menu").accordion({
        collapsible: true,
        active: 2,
        autoHeight: false
    });

    atlas.tokenizeGeneInput($("#geneInExp_qry"), '', '(all genes)');

    $("#simForm").submit(function() {
        $("#simResult").empty();
        var name = $('select option:selected').text();
        $("#simHeader").html("<img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Searching for profiles similar to " +
                name + "...");
        $("#simHeader").show();
        var DEid_ADid = $("select option:selected").val();
        var tokens = DEid_ADid.split('_');
        var DEid = tokens[0];
        var ADid = tokens[1];
        $("#simResult").load(atlas.homeUrl + "expGenes", {eid: experiment.id, deid: DEid, adid: ADid, query:'sim'}, function() {
            $("#simHeader").hide();
            //addGeneToolTips();
        });
        return false;
    });

    $("#searchForm").submit(function() {
        bindTable(0);
    });
}

function addGeneToolTips() {
    $("#squery td.genename a").tooltip({
        bodyHandler: function () {
            return $("#geneToolTipTemplate").tmpl(geneToolTips[this.text]);
        },
        showURL: false
    });
}

function bindSampleAttrsSelector() {
    $(".sample_attr_title").click(function(e) {
        var savals = $(this).parent().next().clone();
        $("#display_attr_values").empty().append(savals);
        savals.show();

        $(".sample_attr_title").css('font-weight', 'normal');
        $(this).css('font-weight', 'bold');
        e.preventDefault();
        return false;
    });
}

function calcApiLink(url) {
    for (var i = 0; i < designElementsToPlot.length; ++i)
        url += '&gene=' + designElementsToPlot[i].geneIdentifier;
    return url;
}


var expPlot;
var designElementIdToAccession = {};
var arrayDesign;
var geneToolTips = {};

function drawPlot(plotType) {
    if (!expPlot) {
        expPlot = new ExperimentPlot("#plot_main", plotType);
    } else {
        expPlot.reload(true);
    }
}

function changePlotType(plotType) {
    expPlot.changePlottingType(plotType);
}

function addDesignElementToPlot(deId, geneId, geneIdentifier, geneName, ef) {
    expPlot.addDesignElementToPlot(deId, geneId, geneIdentifier, geneName, ef);
}



