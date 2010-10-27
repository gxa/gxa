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
                obj.push([curatedProperties_[p.name], v]);
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
            name: "large"
        };
    };

    var BoxPlotType = function() {
        return {
            name: "box",
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

                //step = obj.series[0].data.length;
                for (var i = 0; i < obj.series.length; i++) {
                    var s = obj.series[i];
                    s.points = {show: false};
                    s.lines = {show: false};
                    s.boxes = {show: true};
                    s.legend = {show:true};
                    s.color = parseInt(s.color);

                    x = 0;
                    for (var j=0; j< s.data.length; j++) {
                        s.data[j].x = j*step + i;
                        x += step;
                    }
                }
            }
        };
    };

    var plotTypes = {
        large: BarPlotType,
        box: BoxPlotType
    };

    var ExperimentPlot = window.ExperimentPlot = function(target_, plotType_) {

        if (!(this instanceof arguments.callee)) {
            return new ExperimentPlot(target_, plotType_);
        }

        var plotType = plotTypes[plotType_ || "box"]();
        var plot = null;
        var overview = null;

        var assayOrder = [];
        var prevSelections = {};
        var options = {};

        var target = target_;
        var targetThm = target_ + "_thm";
        var targetLgd = "#legend";

        var expPlot = this;
        var ajaxCall = null;

        $.template("genePlotLabel",
                "<div>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='width:180px'>" +
                        "<tr valign='top' >" +
                            "<td style='width:50px'>${gene}</td>" +
                            "<td>${designElement}</td>" +
                            "<td width='20' valign='bottom' align='left'>" +
                        "<img title='Remove from plot' style='position:relative;top:3px' id='rmgene${designElementId}' class='rmButton' height='8' src='images/closeButton.gif'/>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</div>");

        init();

        function init() {
            load(function(jsonObj) {
                if (currentEF == '' && jsonObj.ef) {
                    currentEF = jsonObj.ef;
                }
                drawZoomControls();
                bindZooming(expPlot);
                bindPlotEvents();
            });
        }

        function load(callback) {
            var geneids = $.map(designElementsToPlot, function (e) {
                return e.id;
            }).join(',');

            var designelements = $.map(designElementsToPlot, function (e) {
                return e.designelement;
            }).join(',');

            //            if (ajaxCall != null) {
            //                ajaxCall.abort();
            //                ajaxCall = null;
            //            }

            //            ajaxCall = atlas.ajaxCall("plot", {gid: geneids, eid: experiment.id, ef: currentEF, plot: plotType.name, de: designelements},
            //                    function(expPlot) {
            //                        return function(response) {
            //                            ajaxCall = null;
            //
            //                            //try {
            //                            var jsonObj = eval(response);
            //                            onload(jsonObj);
            //                            if (callback) {
            //                                callback.call(this, jsonObj);
            //                            }
            //
            //                            //} catch(e) {
            //                            //    if (console) {
            //                            //        console.log(e);
            //                            //    }
            //                            //}
            //                        }
            //                    }(this));

            var genePlots = $('#expressionTableBody').data('json').results[0].genePlots;

            if (!currentEF) {
                for (var key in genePlots) {
                    if (genePlots.hasOwnProperty(key)) {
                        currentEF = key;
                        break;
                    }
                }
            }

            var series = [];
            var genePlot = genePlots[currentEF][plotType.name];

            for (var i = 0; i < genePlot.series.length; i++) {
                var s = genePlot.series[i];
                for (g in designElementsToPlot) {
                    if (designElementsToPlot[g].id == s.label.designelement) {
                        series.label = designElementsToPlot[g];
                        series.push(s);
                        break;
                    }
                }
            }

            var proxy = {};
            for(p in genePlot) {
                proxy[p] = genePlot[p];
            }
            proxy.series = series;

            onload(proxy);

            if (callback) {
                callback.call(this, genePlots[currentEF].box);
            }

            //            if(plotType.onload) plotType.onload(jsonObj);
            //            createPlot(jsonObj);
            //            drawEFpagination();
            //            drawZoomControls()
            //            bindZooming(expPlot);
            //            bindPlotEvents();
        }

        function onload(jsonObj) {
            if (plotType.onload) {
                plotType.onload(jsonObj);
            }

            createPlot(jsonObj);
            drawEFpagination();
        }

        function createPlot(jsonObj) {

            options = $.extend(true, {}, jsonObj.options,
            {
                legend: {
                    labelFormatter: function (gene) {
                        return $.tmpl("genePlotLabel", {
                                gene: gene.name,
                                designElement: designElementIdToAccession[gene.designelement],
                                designElementId: gene.designelement
                        }).html();
                    },
                    container: targetLgd,
                    show: true
                },
                yaxis: {
                    labelWidth:40
                }
            });

            plot = $.plot($(target), jsonObj.series, options);


            if (prevSelections.length > 0) {
                // remap selections according to current assay order
                for (var si = 0; si < prevSelections.length; ++si) {
                    var selAssay = assayOrder[prevSelections[si]];
                    for (var ai = 0; ai < jsonObj.assayOrder.length; ++ai) {
                        if (jsonObj.assayOrder[ai] == selAssay) {
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

            assayOrder = jsonObj.assayOrder || [];

            /*if (jsonObj.assayProperties) {
                assayProperties.test(jsonObj.assayProperties, jsonObj.assayOrder);
            }*/

            createPlotOverview(jsonObj);
            populateSimMenu(jsonObj.simInfo);

            $(".rmButton").hover(function() {
                $(this).attr("src", "images/closeButtonO.gif");
            }, function() {
                $(this).attr("src", "images/closeButton.gif");
            }).click(function() {
                expPlot.removeDesignElementFromPlot($(this).attr('id').substring(6));
            });
        }

        function createPlotOverview(jsonObj) {
            var divElt = $(targetThm);
            divElt.width(500);
            divElt.height(60);

            overview = $.plot($(targetThm), jsonObj.series, $.extend(true, {}, options,
            { yaxis: {
                ticks: 0,
                labelWidth: 40,
                min: -plot.getData()[0].yaxis.datamax * 0.25},
                series:{
                    points:{show: false}
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

            $(target).unbind("plotselected");
            $(target).bind("plotselected", function (event, ranges) {
                /*var min = 0.00001;
                 // clamp the zooming to prevent eternal zoom
                 if (ranges.xaxis.to - ranges.xaxis.from < min)
                 ranges.xaxis.to = ranges.xaxis.from + min;
                 if (ranges.yaxis.to - ranges.yaxis.from < min)
                 ranges.yaxis.to = ranges.yaxis.from + min;*/

                // do zooming
                plot = $.plot($(target), plot.getData(), $.extend(true, {}, options,
                {
                    xaxis: {
                        min: ranges.xaxis.from,
                        max: ranges.xaxis.to
                    },
                    yaxis: {
                        labelWidth: 40
                    }
                }));

                // don't fire event on the overview to prevent eternal loop
                overview.setSelection(ranges, true);
                return plot;
            });

            $(targetThm).unbind("plotselected");
            $(targetThm).bind("plotselected", function (event, ranges) {
                plot.setSelection(ranges);
            });
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

        function bindPlotEvents() {
            $(target).bind("mouseleave", function() {
                $("#tooltip").remove();
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

            $(target).bind("plothover", (function() {
                var previousPoint = null;
                return function(event, pos, item) {
                    if (item) {
                        if (previousPoint != item.datapoint) {
                            previousPoint = item.datapoint;
                            $("#tooltip").remove();
                            showSampleTooltip(item.dataIndex, item.pageX, item.pageY);
                        }
                    } else {
                        $("#tooltip").remove();
                        previousPoint = null;
                    }
                }
            }()));
        }

        function showSampleTooltip(dataIndex, x, y) {
            if (assayProperties.isEmpty()) {
                return;
            }

            var ul = $('<ul/>');
            var assayIndex = assayOrder[dataIndex];
            var props = assayProperties.forAssayIndex(assayIndex);
            for(var i=0; i<props.length; i++) {
                ul.append($('<li/>')
                        .css('padding', '0px')
                        .text(props[i][1])
                        .prepend($('<span/>').css('fontWeight', 'bold').text(props[i][0] + ': ')));
            }


            $('<div id="tooltip"/>').append(ul).css({
                position: 'absolute',
                display: 'none',
                top: y + 5,
                'text-align':'left',
                left: x + 5,
                border: '1px solid #005555',
                margin: '0px',
                'background-color': '#EEF5F5'
            }).appendTo("body").fadeIn("fast");
        }

        function drawEFpagination() {
            var root = $('#EFpagination').empty();
            $.each(experimentEFs, function(i, ef) {
                if (ef != currentEF)
                    root.append($('<a/>').text(curatedEFs[ef]).click(function () {
                        currentEF = ef;
                        expPlot.reload();
                    }));
                else
                    root.append($('<span/>').text(curatedEFs[ef]).addClass('current'));
            });
        }

        function drawZoomControls() {
            var contents = [
                '<div id="zoomin"  style="z-index:1; position:relative; left: 0px; top: 5px;cursor:pointer;"><img style="cursor:pointer" src="images/zoomin.gif" title="Zoom in"></div>',
                '<div id="zoomout" style="z-index:1; position: relative; left: 0px; top: 5px;cursor:pointer;"><img src="images/zoomout.gif" title="Zoom out"></div>',
                '<div id="panright" style="z-index:2;position: relative; left: 20px; top: -35px;cursor:pointer;"><img src="images/panright.gif" title="pan right"></div>',
                '<div id="panleft" style="z-index:2;position: relative; left: -15px; top: -69px;cursor:pointer;"><img src="images/panleft.gif" title="pan left"></div>'];

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

        function bindZooming(expPlot) {
            $("#zoomin").show();
            $("#zoomout").show();

            $("#zoomin").bind("click", function() {
                expPlot.zoomIn();
            });

            $("#zoomout").bind("click", function() {
                expPlot.zoomOut();
            });

            // zoom out completely on double click
            $("#zoomout").bind("dblclick", function() {
                expPlot.zoomOut(true);
            });

            $("#panright > img").unbind("click");
            $("#panright > img").bind("click", function() {
                expPlot.panRight();
            });

            $("#panleft > img").unbind("click");
            $("#panleft > img ").bind("click", function() {
                expPlot.panLeft();
            });
        }

        expPlot.reload = function() {
            $(target).html('');
            $(targetThm).html('');
            $(targetLgd).html('');
            load();
        };

        expPlot.zoomIn = function() {
            var f,t,min,max,range,oldf,oldt;

            max = overview.getXAxes()[0].max;
            min = overview.getXAxes()[0].min;

            if (overview.getSelection() != null) {
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
            } else {
                range = max;
                oldt = max;
                oldf = min;
            }
            var windowSize = Math.floor(2 / 3 * range);
            var offset = Math.floor((range - windowSize) / 2);
            f = oldf + offset;
            t = Math.floor(oldt - offset);
            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
        };

        expPlot.zoomOut = function(completely) {
            var f,t,min,max,range,oldf,oldt;

            max = overview.getXAxes()[0].max;
            min = overview.getXAxes()[0].min;

            if (completely) {
                $(target).trigger("plotselected", { xaxis: { from: min, to: max }});
                overview.clearSelection(true);
                return;
            }

            if (overview.getSelection() != null) {
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
            } else {
                return;
            }
            var windowSize = Math.floor(3 / 2 * range);//alert(windowSize);
            var offset = Math.max(Math.floor((windowSize - range) / 2), 2);
            f = Math.max(oldf - offset, min);
            t = Math.min(Math.floor(oldt + offset), max);

            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
            if (f == min && t == max) overview.clearSelection(true);
        };


        expPlot.panRight = function() {
            var f,t,max,range,oldf,oldt;

            max = overview.getXAxes()[0].max;

            if (overview.getSelection() != null) {
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
            } else {
                return;
            }
            t = Math.min(oldt + 3, max);
            f = t - range;

            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
        };

        expPlot.panLeft = function() {
            var f,t,min,max,range,oldf,oldt;

            max = overview.getXAxes()[0].max;
            min = overview.getXAxes()[0].min;

            if (overview.getSelection() != null) {
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
            } else {
                return;
            }
            f = Math.max(oldf - 3, min);
            t = f + range;
            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
        };

        expPlot.addDesignElementToPlot = function(deId, geneId, geneidentifier, genename, ef, designelement) {
            for (var i = 0; i < designElementsToPlot.length; ++i) {
                if ((designElementsToPlot[i].id == deId) && (designElementsToPlot[i].designelement == designelement))
                    return;
            }

            designElementsToPlot.push({id: deId, geneId: geneId, identifier: geneidentifier, name: genename, designelement: designelement});
            currentEF = ef;

            expPlot.reload();
        };

        expPlot.removeDesignElementFromPlot = function(deId) {

            if (designElementsToPlot.length == 1)
                return;

            for (var i = 0; i < designElementsToPlot.length; i++) {
                if (designElementsToPlot[i].id == deId) {
                    designElementsToPlot.splice(i, 1);
                    break;
                }
            }

            expPlot.reload();
        };

        expPlot.changePlottingType = function(type) {
            if (plotTypes[type]) {
                plotType = plotTypes[type]();
                expPlot.reload();
            } else {
                  atlasLog("unknown plot type: " + type);
            }
        };

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

        //bindTableFromJson();

        return false;
    }
}());

var assayProperties = null;

function loadData(experiment, arrayDesign, gene, ef, efv, updn) {

    assayProperties = new AssayProperties({
        experimentId: experiment,
        arrayDesign: arrayDesign
    });

    $(assayProperties).bind("dataDidLoad", function() {
        bindTableFromJson(experiment, gene, ef, efv, updn);
    });
    
    assayProperties.load();
}

function bindTableFromJson(experiment, gene, ef, efv, updn) {
    $("#qryHeader").html("<img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Loading...");
    $("#qryHeader").show();

    //alert($("#squery").position.top);
    //alert($("#squery").position.width);

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

        for(var eaIdx in data.results[0].expressionAnalyses) {
            var ea = data.results[0].expressionAnalyses[eaIdx]
            r.push({
                 deId: ea.deid,
                 gene: ea.geneName,
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
            
            if(plotGeneCounter-- > 0)
              designElementsToPlot.push({id:ea.deid, geneId: ea.geneId, identifier:ea.geneIdentifier, name: ea.geneName, designelement: ea.designElementAccession});
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
    //$.template("expressionValueTableRowTemplate","<tr><td><a onclick=\"addGeneToPlot('${geneId}','${geneIdentifier}','${geneName}','${rawef}','${de}');return false;\"'><img border='0' src='images/iconf.png'/></a></td><td>${gene}</td><td>${de}</td><td>${ef}</td><td>${efv}</td><td>${expr}</td><td>${tstat}</td><td>${pvalue}</td></tr>");
    $("#expressionValueTableRowTemplate1").tmpl(expressionValues).appendTo($("#expressionTableBody").empty());
}

function defaultQuery(){
    $("#geneFilter").val('');
    $("#efvFilter").attr('selectedIndex', 0);
    $("#updownFilter").attr('selectedIndex', 0);
    $("#divErrorMessage").css("visibility","hidden");
    loadData(experiment.accession, arrayDesign, '', '', '', '');
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
            addGeneToolTips();
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
            /*
            var dataUrl = "api?geneIs=ENSG00000001167&format=json";
            var resultData = "<div id='oneAndOnlyTooltip'><img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Searching...</div>";
            atlas.ajaxCall(dataUrl,"", function(data) {
                //alert("received" + data.length);
                var str = "";
                $("#geneInfoTemplate").tmpl(data.results[0].gene).appendTo($("#oneAndOnlyTooltip").empty());
            });
            return resultData; */
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
        url += '&gene=' + designElementsToPlot[i].identifier;
    return url;
}


var expPlot;
var designElementIdToAccession = {};
var arrayDesign;
var geneToolTips = {};

function drawPlot(plotType) {
    if (!expPlot) {
        expPlot = new ExperimentPlot("#plot", plotType);
    } else {
        expPlot.reload();
    }
}

function changePlotType(plotType) {
    expPlot.changePlottingType(plotType);
}

function addDesignElementToPlot(deId, geneidentifier, genename, ef, designelement) {
    expPlot.addDesignElementToPlot(deId, geneidentifier, genename, ef, designelement);
}



