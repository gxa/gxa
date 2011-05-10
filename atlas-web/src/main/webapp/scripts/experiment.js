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

    var AssayProperties = function() {
        var data = null;

        function processData(aData) {
            data = null;

            aData.allProperties = [];

            var toMerge = ["scs", "efs"];
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

            data = aData;
        }

        $.extend(true, this, {
            isEmpty: function() {
                return data == null;
            },

            load: function(data) {
                processData(data);
            },

            findProperties: function(assayIndex) {
                if (!data || assayIndex < 0) {
                    return null;
                }

                var obj = [];
                var uniq = {};
                for (var i = 0; i < data.allProperties.length; i++) {
                    var p = data.allProperties[i];
                    var v = p.values[p.assays[assayIndex]];
                    if (v == "" || v.toLowerCase() == "(empty)" || uniq[p.name] == v) {
                        continue;
                    }
                    obj.push([p.name, v]);
                    uniq[p.name] = v;
                }
                return obj;
            }
        });
    };

    window.DataSeriesProvider = function() {

        var provider = null;

        function findDataProvider(eid, ad) {
            if (!eid) {
                return provider;
            }

            if (!ad) {
                provider = null;
                return provider;
            }

            if (provider != null && provider.suits(eid, ad)) {
                return provider;
            }

            var expressions = {};
            var boxAndWhisker = {};
            var efEfvAssays = {};
            var efNames = [];
            var efvNames = [];
            var assayProperties = new AssayProperties();

            var cache = {
                assayDistribution: null,
                efInfo: null,
                deIndices: null
            };

            function markingColor(i) {
                return ["#F0FFFF", "#F5F5DC"][i % 2];
            }

            function load(eid, ad, deIndices, callback) {
                var params = ["eid=" + eid, "ad=" + ad, "assayPropertiesRequired=" + (assayProperties.isEmpty()), "format=json"];
                for (var i in deIndices) {
                    params.push("de=" + deIndices[i]);
                }

                atlas.ajaxCall("experimentPlot?" + params.join("&"), "", callback, function(err) {
                    atlasLog("Data loading error: " + err);
                });
            }

            function onLoad(json) {
                var plotData = json.plot;

                var jsonExpressions = plotData.expressions;
                var jsonBoxAndWhisker = plotData.boxAndWhisker;

                var deIndices = plotData.deIndices;
                for (var i = 0; i < deIndices.length; i++) {
                    var deIndex = deIndices[i];
                    expressions[deIndex] = jsonExpressions[i];
                    boxAndWhisker[deIndex] = jsonBoxAndWhisker[i];
                }

                if (!efNames.length) {
                    efNames = plotData.efNames;
                    efvNames = plotData.efvNames;
                    efEfvAssays = plotData.efEfvAssays;
                }

                if ("assayProperties" in json) {
                    assayProperties.load(json.assayProperties);
                }
            }

            function findSeries(eid, ad, ef, deIndices, type, callback) {
                var missed = [];
                for (var i in deIndices) {
                    var idx = deIndices[i];
                    if (!expressions[idx]) {
                        missed.push(idx);
                    }
                }
                if (missed.length) {
                    load(eid, ad, missed, (function(aEf, aDeIndices, aType) {
                        return function(result) {
                            onLoad(result);
                            callback(prepareSeries(aEf, aDeIndices, aType));
                        }
                    })(ef, deIndices, type));
                } else {
                    callback(prepareSeries(ef, deIndices, type));
                }
            }

            function prepareSeries(efName, deIndices, type) {
                switch (type) {
                    case "box" :
                        return prepareBoxPlotSeries(efName, deIndices);
                    case "large" :
                        return prepareLinePlotSeries(efName, deIndices);
                }
                atlasLog("Undefined type of plot: " + type);
                return null;
            }

            function efInfo(efName) {
                var efIdx = 0;
                var efvOffset = 0;
                for (var i = 0; i < efNames.length; i++) {
                    if (efNames[i].name === efName) {
                        efIdx = i;
                        break;
                    }
                    efvOffset += efvNames[i].length;
                }
                return {name: efName, index: efIdx, efvOffset: efvOffset};
            }

            function prepareBoxPlotSeries(efName, deIndices) {
                var series = [];
                var markings = [];

                cache.efInfo = null;
                cache.deIndices = null;

                if (deIndices.length == 0) {
                    return null;
                }

                var ef = efInfo(efName);

                cache.efInfo = ef;
                cache.deIndices = deIndices;

                var efvs = efvNames[ef.index];
                var deStep = deIndices.length;

                for (var j = 0; j < deIndices.length; j++) {
                    var deIndex = deIndices[j];

                    var data = [];
                    for (var efvIndex = 0; efvIndex < efvs.length; efvIndex ++) {
                        var efv = efvs[efvIndex];

                        if (markings.length < efvIndex + 1) {
                            markings.push({
                                xaxis:{
                                    from: efvIndex * deStep,
                                    to: (efvIndex + 1) * deStep },
                                label: efv,
                                color: markingColor(efvIndex)
                            });
                        }

                        data.push(
                                $.extend(true, {}, boxAndWhisker[deIndex][ef.efvOffset + efvIndex], {x: (efvIndex * deStep) + j}));
                    }

                    series.push(addSeriesOptions(data, deIndex, {
                        points: {show: false},
                        lines: {show: false},
                        boxes: {show: true}
                    }));
                }
                return addPlotOptions(series, markings, {boxes: {hoverable: true}});
            }

            function prepareLinePlotSeries(efName, deIndices) {
                var series = [];
                var markings = [];

                cache.assayDistribution = null;

                if (deIndices.length == 0) {
                    return null;
                }

                var ef = efInfo(efName);

                var assayDistribution = optimizeAssayDistribution(ef, deIndices);
                cache.assayDistribution = assayDistribution;

                var efvs = efvNames[ef.index];

                for (var j = 0; j < deIndices.length; j++) {
                    var deIdx = deIndices[j];

                    var data = [];
                    var n = 0;
                    for (var efvIdx = 0; efvIdx < efvs.length; efvIdx++) {
                        var efv = efvs[efvIdx];
                        var assayIndices = assayDistribution[efvIdx];

                        if (j == 0) {
                            markings.push({
                                xaxis: {
                                    from: n - 0.5,
                                    to: n - 0.5 + assayIndices.length},
                                label: efv,
                                color: markingColor(markings.length + 1)
                            });
                        }
                        for (var assayIndex = 0; assayIndex < assayIndices.length; assayIndex += 1) {
                            var expr = expressions[deIdx][assayIndices[assayIndex]];
                            expr = typeof expr === "string" ? null : expr;
                            data.push([n++, expr]);
                        }
                    }

                    series.push(addSeriesOptions(data, deIdx));
                }

                return addPlotOptions(series, markings);
            }

            function optimizeAssayDistribution(efInfo, deIndices) {
                var efvs = efvNames[efInfo.index];
                var MAX_POINTS = Math.max(efvs.length, 800);

                var numberOfAssays = 0;
                var result = [];
                for (var i = 0; i < efvs.length; i++) {
                    var arr = efEfvAssays[efInfo.index][i];
                    numberOfAssays +=  arr.length;
                    result.push([].concat(arr));
                }

                if (numberOfAssays < MAX_POINTS) {
                    // nothing to optimize
                    return result;
                }

                var startTime = (new Date).getTime();

                var efvInterest = [];
                var iAssayCount = {};
                var iEfvCount = {};
                for (var k = 0; k < efvs.length; k++) {
                    var interest = getInterest(efInfo.efvOffset + k, deIndices);
                    efvInterest[k] = interest;
                    var t = iAssayCount[interest];
                    iAssayCount[interest] = (t ? t : 0) + efEfvAssays[efInfo.index][k].length;
                    t = iEfvCount[interest];
                    iEfvCount[interest] = (t ? t : 0) + 1;
                }

                var sum = 0;
                for (var z in iAssayCount) {
                    sum += iAssayCount[z] * parseInt(z);
                }
                var koef = (MAX_POINTS - efvs.length) / sum;

                var n = 0;
                for (var efvIdx = 0; efvIdx < efvs.length; efvIdx++) {
                    var assayIndices = result[efvIdx];
                    var intr = efvInterest[efvIdx];

                    var assayIndicesLength = Math.min(
                            Math.round(1.0 + iAssayCount[intr] * intr * koef / iEfvCount[intr]),
                            assayIndices.length);

                    var selectedAssays = [];
                    var step = Math.ceil(assayIndices.length / assayIndicesLength);
                    for (var assayIdx = 0; assayIdx < assayIndices.length; assayIdx += step) {
                        selectedAssays.push(assayIndices[assayIdx]);
                    }
                    result[efvIdx] = selectedAssays;
                    n += selectedAssays.length;
                }

                atlasLog("Optimized assay distribution (MAX_POINTS=" + MAX_POINTS + "): " +
                        numberOfAssays + " -> " + n + " in " + ((new Date).getTime() - startTime) + "ms");
                return result;
            }

            function getInterest(efEfvIndex, deIndices) {
                var c = 1;
                for (var j = 0; j < deIndices.length; j++) {
                    var bAndW = boxAndWhisker[deIndices[j]][efEfvIndex];
                    if (bAndW.up || bAndW.down) {
                        c++;
                    }
                }
                return c;
            }

            function addPlotOptions(series, markings, options) {
                return {
                    series: series,
                    options: $.extend(true, {
                        xaxis: {ticks: 0},
                        yaxis: {ticks: 3},
                        legend: {show: true},
                        grid: {
                            backgroundColor: "#fafafa",
                            autoHighlight: true,
                            hoverable: true,
                            clickable: true,
                            borderWidth: 0,
                            markings: markings
                        },
                        series: {
                            points: {
                                show: true,
                                fill: true,
                                radius: 1.5
                            },
                            lines:{
                                show: true,
                                steps: false
                            }
                        },
                        selection: {mode: "x"}
                    }, options || {})
                }
            }

            function addSeriesOptions(data, deIndex, options) {
                var obj = $.extend(true, {
                    lines: {
                        show: true,
                        lineWidth: 2,
                        fill: false,
                        steps: false
                    },
                    points: {
                        show: true,
                        fill: true
                    },
                    legend: {
                        show: true
                    },
                    label: {
                        deIndex: deIndex
                    }
                }, options || {});

                obj.data = data;

                return obj;
            }

            function getAssayIndex(x) {
                var assayDistribution = cache.assayDistribution || [];
                var offset = 0;
                for(var i=0; i<assayDistribution.length; i++) {
                    if (x >= offset + assayDistribution[i].length) {
                        offset += assayDistribution[i].length;
                        continue;
                    }
                    return assayDistribution[i][x - offset];
                }
                return -1;
            }

            function getBoxAndWhiskerProperties(x) {
                if (!cache.deIndices || !cache.efInfo) {
                    return null;
                }

                x = Math.floor(x);

                var deIndices = cache.deIndices;
                var ef = cache.efInfo;

                var step = deIndices.length;
                var i = Math.floor(x) % step;
                var j = Math.floor(x / step);

                var box = boxAndWhisker[deIndices[i]][ef.efvOffset + j];
                var efv = efvNames[ef.index][j];
                return {
                    box: box,
                    deIndex: deIndices[i],
                    efv: efv
                };
            }

            return (provider = {
                suits: function(anEid, anAd) {
                    return eid === anEid && ad === anAd;
                },

                getSeries: function(ef, deIndices, type, callback) {
                    findSeries(eid, ad, ef, deIndices, type, callback);
                },

                getAssayProperties: function(x) {
                    return assayProperties.findProperties(getAssayIndex(x));
                },

                getBoxProperties: function(x) {
                    return getBoxAndWhiskerProperties(x);
                },

                getExperimentFactors: function() {
                    return [].concat(efNames);
                }
            });
        }

        function withProvider(msg, errorReturn, doFunc) {
            if (provider) {
                return doFunc(provider);
            }
            atlasLog("The data provider is null; " + msg);
            return errorReturn;
        }

        return {
            getSeries: function(opts, callback) {
                opts = opts || {};
                findDataProvider(opts.eid || null, opts.ad || null);
                if (provider) {
                    provider.getSeries(opts.ef, opts.deIndices, opts.type, callback);
                } else {
                    atlasLog("The data provider is null (eid=" + opts.eid + ", ad=" + opts.ad + ") can't get series");
                    callback(null);
                }
            },

            getAssayProperties: function(x) {
                return withProvider("Can't get assay properties", null, function(provider) {
                    return provider.getAssayProperties(x);
                });
            },

            getBoxProperties: function(x) {
                return withProvider("Can't get box properties", null, function(provider) {
                    return provider.getBoxProperties(x);
                });
            },

            getExperimentFactors: function() {
                return withProvider("Can't get curated factor names", null, function(provider) {
                    return provider.getExperimentFactors();
                });
            }
        };
    }();

})();

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
        var flot = null;
        var flotOverview = null;
        var data = null;

        var target = "#" + (opts.target || "undefined");
        var targetOverview = "#" + (opts.targetOverview || "undefined");
        var targetLegend = "#" + (opts.targetLegend || "undefined");
        var labelFormatter = (opts.labelFormatter || function(label) { return label.deIndex });

        var initialWidth = $(target).width() || 500;
        var canZoom = opts.canZoom || false;
        var canPan = opts.canPan || false;

        var zoomControls = new ZoomControls({
            target : opts.targetZoomControls || null,
            canZoom: canZoom,
            canPan: canPan
        });

        subscribe(zoomControls);

        var basePlot = this;

        basePlot.adjustPlot = function(plot, data) {
            //overwrite
        };

        basePlot.getMaxX = function() {
            if (data.series && data.series.length > 0) {
                var firstSeries = data.series[0];
                if (firstSeries.data && firstSeries.data.length > 0) {
                    return firstSeries.data[firstSeries.data.length - 1][0];
                }
            }
            return 0;
        };

        basePlot.getMinX = function() {
            if (data.series && data.series.length > 0) {
                var firstSeries = data.series[0];
                if (firstSeries.data && firstSeries.data.length > 0) {
                    return firstSeries.data[0][0];
                }
            }
            return 0;
        };

        basePlot.bindToolTip = function(target, data) {
            //override
        };

        basePlot.createToolTip = function(name, convertFunc) {
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

        basePlot.getSeries = function() {
            return flot ? flot.getData() : null;
        };

        basePlot.load = function(opts, callback) {
            if (theSame(opts)) {
                callback();
                return;
            }

            startLoading();
            DataSeriesProvider.getSeries(opts, function(data) {
                stopLoading();
                updatePlot(data);
                callback();
            });
        };

        function theSame(opts) {
            var o = basePlot.opts;
            var newOpts = [[opts.eid || "", opts.ad || ""].join(""), (opts.ef || "") + ([].concat(opts.deIndices || []).sort()).join("")];
            if (!o || (newOpts[0] && o[0] !== newOpts[0]) || (o[1] !== newOpts[1])) {
                basePlot.opts = newOpts;
                atlasLog("Load options saved: " + (newOpts[0] || "*") + " | " + newOpts[1]);
                return false;
            }

            return true;
        }

        function clearPlot() {
            $(target).empty();
            $(targetOverview).empty();
            $(targetLegend).empty();

            zoomControls.hide();

            data = null;
        }

        function startLoading() {
            clearPlot();
            atlas.newWaiter2(target);
        }

        function stopLoading() {
            atlas.removeWaiter2();
        }

        function updatePlot(newData) {
            if (newData == null) {
                return;
            }

            zoomControls.show();

            data = newData;

            function refinePlotWidthAndSelection(width, plotData, selection) {
                var xRange = selection ? selection.xaxis : null;
                var numberOfPoints =
                        xRange ? Math.abs(xRange.from - xRange.to) :
                                basePlot.getMaxX(plotData);

                xRange = xRange == null ? {from:basePlot.getMinX(plotData), to:numberOfPoints} : xRange;

                var noScroll = true;

                var pxPerPoint = width / numberOfPoints;
                var minPx = 20, maxPx = 30, avPx = (minPx + maxPx) / 2;

                if (pxPerPoint < minPx) {
                    xRange = {from: xRange.from, to: xRange.from + ((xRange.to - xRange.from) * width / avPx / numberOfPoints)};
                    noScroll = false;
                }

                if (pxPerPoint > maxPx) {
                    width = maxPx * numberOfPoints + 60; //TODO: hack ?
                }

                return {selection: {xaxis: xRange}, width: width, noScroll: noScroll};
            }

            //restore the original width
            $(target).width(initialWidth);
            $(targetOverview).width(initialWidth);

            $.extend(true, data.options,
            {
                legend: {
                    labelFormatter: labelFormatter,
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
        }

        function createPlotOverview(ranges) {
            flotOverview = $.plot(targetOverview, data.series, $.extend(true, {}, data.options,
            {
                yaxis: {
                    ticks: 0,
                    labelWidth: 40
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
                flotOverview.setSelection(ranges, true);
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

            flot = $.plot($(target), data.series, o);
            basePlot.adjustPlot(flot, data);
            basePlot.bindToolTip(target, data);

            $(targetLegend).css({paddingLeft: flot.getPlotOffset().left});
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

            var max = flotOverview.getXAxes()[0].max;

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

            var min = flotOverview.getXAxes()[0].min;

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
            var xaxes = flotOverview.getXAxes()[0];

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
            var xaxes = flotOverview.getXAxes()[0];

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
                return flotOverview.getSelection();
            }

            if (scrollMode()) {
                return flotOverview.getScrollWindow();
            }
            return null;
        }

        function setSelection(ranges, preventEvent) {
            if (!ranges || !ranges.xaxis) {
                return;
            }

            if (selectionMode()) {
                flotOverview.setSelection(ranges, preventEvent);
            }

            if (scrollMode()) {
                flotOverview.setScrollWindow(ranges);
            }
        }


        function clearSelection(preventEvent) {
            if (selectionMode()) {
                flotOverview.clearSelection(preventEvent);
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
        var utils = opts.utils;

        var base = new BasePlot({
            target: "plot_large",
            targetOverview: "plot_overview_large",
            targetLegend: "legend_large",
            targetZoomControls: "zoomControls_large",
            labelFormatter: function(label) {
                return utils ? utils.deInfo(label.deIndex) : label.deIndex;
            },
            canPan : true,
            canZoom : true
        });

        base.type = "large";

        base.bindToolTip = function(target) {
            var tooltip = base.createToolTip("lineplot",
                    function(x) {
                        var assayProps = DataSeriesProvider.getAssayProperties(x);
                        if (!assayProps) {
                            return;
                        }

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

        var utils = opts.utils;

        var base = new BasePlot({
            target: "plot_box",
            targetOverview: "plot_overview_box",
            targetLegend:"legend_box",
            targetZoomControls: "zoomControls_box",
            labelFormatter: function(label) {
                return utils ? utils.deInfo(label.deIndex) : label.deIndex;
            },
            canZoom: false,
            canPan: true
        });

        base.type = "box";

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
                    function(x) {
                        var boxProps = DataSeriesProvider.getBoxProperties(x);
                        if (!boxProps) {
                            return;
                        }

                        var box = boxProps.box;
                        var efv = boxProps.efv;
                        var boxName = utils ? utils.deInfo(boxProps.deIndex) : boxProps.deIndex;

                        var titles = [
                            ["Max", box.max],
                            ["Upper quartile", box.uq],
                            ["Median", box.median],
                            ["Lower quartile", box.lq],
                            ["Min", box.min],
                            ["Expression", box.up ? "up in " + efv : null],
                            ["Expression", box.down ? "down in " + efv : null]
                        ];

                        var props = [];

                        for (var p in titles) {
                            var t = titles[p];
                            if (t[1] != null) {
                                props.push({pname: t[0], pvalue: t[1]});
                            }
                        }

                        var div = $("<div/>").append(
                                $.tmpl("plotTooltipTempl", {properties:props, title: boxName}));
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

    var ExperimentPlot = window.ExperimentPlot = function() {

        if (!(this instanceof arguments.callee)) {
            return new ExperimentPlot();
        }

        var _plots = {};
        var _plotTypes = {box: createBoxPlot, large: createLargePlot};
        var _plotType = "box";

        var _designElements = [];
        var _ef = "";

        var _expPlot = this;

        _expPlot.load = load;
        _expPlot.addOrRemoveDesignElement = addOrRemoveDesignElement;
        _expPlot.changePlotType = changePlotType;
        _expPlot.changeCurrentEf = changeCurrentEf;
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
                plot = _plotTypes[_plotType]({
                    utils: {
                            deByIndex: function(deIndex) {
                                for (var i in _designElements) {
                                    if (_designElements[i].deIndex == deIndex) {
                                        return _designElements[i];
                                    }
                                }
                                return null;
                            },
                            deInfo: function(deIndex) {
                                var de = this.deByIndex(deIndex);
                                return de ? de.geneName + ":" + de.deAcc : deIndex;
                            }
                        }
                });
                _plots[_plotType] = plot;
            }
            return plot;
        }

        function notifyDataDidLoad() {
            $(_expPlot).trigger("dataDidLoad");
        }

        function loadCallback() {
            drawEFpagination();
            notifyDataDidLoad();
        }

        function load(designElements, ef, expAcc, ad) {
            _designElements = designElements;
            _ef = ef;

            update({eid: expAcc, ad: ad});
        }

        function update(opts) {
            var deIndices = [];
            for (var i = 0; i < _designElements.length; i++) {
                deIndices.push(_designElements[i].deIndex);
            }

            getPlot().load($.extend(true, {ef:_ef, deIndices: deIndices, type: _plotType}, opts || {}), loadCallback);
        }

        function drawEFpagination() {
            var root = $('#EFpagination').empty();

            var efs = DataSeriesProvider.getExperimentFactors();
            for (var i in efs) {
                var ef = efs[i];
                if (ef.name != _ef)
                    root.append($('<div/>').append($('<a/>').text(ef.curatedName).click(
                                    (function(eff) {
                                        return function (event) {
                                            _expPlot.changeCurrentEf(eff);
                                        }
                                    }(ef.name))
                            )));
                else
                    root.append($('<div/>').text(ef.curatedName).addClass('current'));
            }
        }

        function getDesignElementColors() {
            var colors = {};
            var series = getPlot().getSeries();
            for (var i in series) {
                var s = series[i];
                var de = s.label.deIndex;
                colors[de] = s.color;
            }
            return colors;
        }

        function addOrRemoveDesignElement(de) {
            for (var i in _designElements) {
                if (_designElements[i].deIndex == de.deIndex) {
                    removeDesignElement(de);
                    return;
                }
            }

            _designElements.push(de);

            update();
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

            update();
        }

        function changePlotType(type) {
            if (!_plotTypes[type]) {
                atlasLog("unknown plot type: " + type);
                return;
            }

            _plotType = type;
            update();
        }

        function changeCurrentEf(ef) {
            _ef = ef;
            update();
        }
    };

}());

(function() {

    window.ExperimentPage = function(opts) {

        var _state = (function() {
            var s = null;

            function getOrSetValue(name, args) {
                if (args.length > 0) {
                    s[name] = args[0];
                }
                return s[name];
            }

            function decode(opts) {
                s = newState();

                var array = location.href.split("?");
                if (array.length > 2) {
                    var params = array[1].split("&");

                    for (var i = 0; i < params.length; i++) {
                        var p = params[i].split("=");
                        if (p.length < 2) {
                            continue;
                        }
                        if (s.hasOwnProperty(p[0])) {
                            s[p[0]] = decodeURIComponent(p[1]);
                        }
                    }
                }

                for (var o in opts) {
                    if (s.hasOwnProperty(o)) {
                        s[o] = opts[o];
                    }
                }
            }

            function newState() {
               return {eid:null, gid:null, ad:null, ef:null, efv:null, updown:"ANY", offset:0, limit:10};
            }

            return {
                clear: function() {
                    s = newState();
                },

                eid: function() {
                    return getOrSetValue("eid", arguments);
                },

                gid: function() {
                    return getOrSetValue("gid", arguments);
                },

                ad: function() {
                    return getOrSetValue("ad", arguments);
                },

                efEfv: function() {
                    if (arguments.length > 0) {
                        var arg = arguments[0].split("||");
                        if (arg.length > 1) {
                            getOrSetValue("ef", [arg[0]]);
                            getOrSetValue("efv", [arg[1]]);
                        }
                    }
                    return s["ef"] && s["efv"] ? s["ef"] + "||" + s["efv"] : "";
                },

                ef: function() {
                    return getOrSetValue("ef", arguments) || "";
                },

                efv: function() {
                    return getOrSetValue("efv", arguments) || "";
                },

                updown: function() {
                    return getOrSetValue("updown", arguments);
                },

                offset: function() {
                    var v = getOrSetValue("offset", arguments);
                    return v ? parseInt(v) : 0;
                },

                limit: function() {
                    var v = getOrSetValue("limit", arguments);
                    return v ? parseInt(v) : 0;
                },

                page: function() {
                    if (arguments.length > 0) {
                        var page = arguments[0];
                        getOrSetValue("offset", [page * this.limit()]);
                    }
                    return this.offset() / this.limit()
                },

                decode: function(opts) {
                    decode(opts);
                }
            }
        })();

        var _designElements = [];

        var _expPlot = new ExperimentPlot();
        $(_expPlot).bind("dataDidLoad", dataDidLoad);

        /**
         * Changes type of currently showing plot
         * @param plotType a string "box" or "large"
         */
        this.changePlotType = function(plotType) {
            _expPlot.changePlotType(plotType);
        };

        /**
         * Adds/removes design element to/from currently showing plot
         * @param deId
         */
        this.addOrRemoveDesignElement = function(deIndex) {
            for (var i in _designElements) {
                var de = _designElements[i];
                if (de.deIndex == deIndex) {
                    _expPlot.addOrRemoveDesignElement(de);
                    return;
                }
            }
        };

        /**
         * Generates paramString for JSON/XML API links
         * @param url an url to be enhanced
         */
        this.getApiLink = function(url) {
            var params = [];
            for (var i = 0; i < _designElements.length; ++i) {
                params.push('&gene=' + _designElements[i].geneIdentifier);
            }
            return url + params.join("");
        };

        /**
         * Runs default search
         */
        this.clearQuery = function() {
            clearForm();
            newSearch();
        };

        init(opts);

        function init(opts) {
            _state.decode(opts);
            initForm();
            search();
        }

        function initForm(opts) {
            $("#geneFilter").val(_state.gid());
            $("#efvFilter").val(_state.efEfv());
            $("#updownFilter").val(_state.updown());

            $("#geneFilter").change(function() {
                _state.gid($(this).val());
                newSearch();
            });

            $("#efvFilter").change(function() {
                _state.efEfv($(this).val());
                newSearch();
            });

            $("#updownFilter").change(function() {
                _state.updown($(this).val());
                newSearch();
            });

            $("#expressionListFilterForm").bind("submit", function() {
                return false;
            });
        }

        function clearForm() {
            _state.clear();
            $("#geneFilter").val("");
            $("#efvFilter").attr("selectedIndex", 0);
            $("#updownFilter").attr("selectedIndex", 0);
            $("#divErrorMessage").css("visibility", "hidden");
        }

        function changePage(index) {
            _state.page(index);
            submitQuery(processExpressionAnalysisOnly);
        }

        function newSearch() {
            _state.page(0);
            search();
        }

        function search() {
            updatePageLinks();
            submitQuery(process);
        }

        function submitQuery(callback) {
            loadExpressionAnalysis(
                    _state.eid(), _state.ad(), _state.gid(), _state.ef(), '"' + _state.efv() + '"', _state.updown(), _state.offset(), _state.limit(), callback);
        }

        function loadExpressionAnalysis(expAcc, ad, gene, ef, efv, updn, offset, limit, callback) {
            $("#divErrorMessage").css("visibility", "hidden");

            atlas.newWaiter2("#squery");

            //TODO: __upIn__ workaround
            var dataUrl = "api/v0?experimentPage&experiment=" + expAcc +
                    (gene ? "&geneIs=" + gene : "") +
                    (ad ? "&hasArrayDesign=" + ad : "") +
                    (ef ? "&upIn" + ef + "=" + efv : "") +
                    (updn ? "&updown=" + updn : "") +
                    "&offset=" + (offset + 1) + "&limit=" + limit;

            atlas.ajaxCall(dataUrl, "", callback, function() {
                callback(null);
            });
        }

        function processExpressionAnalysisOnly(data) {
            process(data, true);
        }

        function process(data, expressionAnalysisOnly) {
            atlas.removeWaiter2();

            var res = {};
            var eaItems = {};
            var eaTotalSize = 0;
            var geneToolTips = {};

            if (!data || !data.results || data.results.length == 0 || data.results[0].expressionAnalyses.items.length == 0) {
                $("#divErrorMessage").css("visibility", "visible");
                $("#expressionTableBody").empty();
                data = null;
            } else {
                res = data.results[0];
                eaItems = res.expressionAnalyses.items;
                eaTotalSize = res.expressionAnalyses.totalSize;
                geneToolTips = res.geneToolTips;
                $('#arrayDesign').html(res.arrayDesign);
            }

            $("#expressionTableBody").data("json", data);

            var eAs = [];
            _designElements = [];

            for (var i = 0; i < eaItems.length; i++) {
                var ea = eaItems[i];
                eAs.push({
                    deIndex: ea.deidx,
                    deAcc: ea.designElementAccession,
                    geneName: ea.geneName,
                    geneId: ea.geneId,
                    geneIdentifier: ea.geneIdentifier,
                    ef: curatedEFs[ea.ef] || ea.ef,
                    ef_enc: encodeURIComponent(encodeURIComponent(curatedEFs[ea.ef])).replace(/_/g, '%5F'),
                    rawef: ea.ef,
                    efv: ea.efv,
                    efv_enc: encodeURIComponent(encodeURIComponent(ea.efv)).replace(/_/g, '%5F'),
                    pvalue: ea.pvalPretty,
                    tstat: ea.tstatPretty,
                    expr: ea.expression
                });

                _designElements.push({
                    deAcc: ea.designElementAccession,
                    deIndex: ea.deidx,
                    geneId: ea.geneId,
                    geneName: ea.geneName,
                    geneIdentifier: ea.geneIdentifier
                });
            }

            updateTable(eAs);
            updateTableTooltips(geneToolTips);

            if (expressionAnalysisOnly) {
                dataDidLoad();
            } else {
                updatePagination(eaTotalSize);

                var ef = eaItems.length > 0 ? eaItems[0].ef : "";
                updatePlot(_designElements.slice(0, 3), ef, res.arrayDesign);
            }
        }

        function dataDidLoad() {
            updateRowColors();
        }

        function updateTable(expressionValues) {
            $("#expressionValueTableRowTemplate1").tmpl(expressionValues).appendTo($("#expressionTableBody").empty());
        }

        function updateTableTooltips(geneToolTips) {
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

        function updatePagination(total) {
            var target = "#topPagination";

            $(target).empty();

            if (total > _state.limit()) {
                $(target).pagination(total, {
                    num_edge_entries: 2,
                    num_display_entries: 5,
                    items_per_page: _state.limit(),
                    current_page: _state.page(),
                    callback: function(pageIndex) {
                        changePage(pageIndex);
                        return false;
                    }
                });
            }
        }

        function updatePlot(designElements, ef, ad) {
            _expPlot.load(designElements, ef, _state.eid(), ad);
        }

        function updateRowColors() {
            var deColors = _expPlot.getDesignElementColors();

            $(".designElementRow").each(function() {
                var el = $(this);
                var deIndex = el.attr('id').split("_")[1];
                var color = deColors[deIndex];
                if (color) {
                    $("#results_" + deIndex).css({backgroundColor: color});
                    var img = $("#results_" + deIndex + " img")[0];
                    img.src = "images/chart_line_delete.png";
                    img.title = "remove from plot";
                } else {
                    $("#results_" + deIndex).css({backgroundColor: "white"});
                    var img = $("#results_" + deIndex + " img")[0];
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
            var params = {gid: _state.gid(), ef: _state.ef(), efv: _state.efv(), updown: _state.updown()};
            var paramString = [];
            for (var p in params) {
                var v = params[p];
                url = url.replace(new RegExp("[&\\?]?(" + p + "=[^&$]*)", "g"), "");
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



