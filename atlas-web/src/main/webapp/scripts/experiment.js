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

    var BarPlotType = function() {
        return {
           name: "large"
        };
    };

    var BoxPlotType = function() {
        return {
            name: "boxplot",
            onload: function(jsonObj) {
                //TODO: move ui specific options to client
                var x = 0;
                for (var i = 0; i < jsonObj.series.length; i++) {

                    var s = jsonObj.series[i];
                    s.points = {show: false};
                    s.lines = {show: false};
                    s.boxes = {show: true};
                    s.legend = {show:true};
                    s.color = parseInt(s.color);
                }
            }
        };
    };

    var plotTypes = {
        large: BarPlotType,
        boxplot: BoxPlotType
    };

    var ExperimentPlot = window.ExperimentPlot = function(target_, plotType_) {

        var plotType = plotTypes[plotType_ || "boxplot"]();
        var plot = null;
        var overview = null;
        var assayProperties = [];
        var assayOrder = [];
        var prevSelections = {};
        var options = {};

        var target = target_;
        var targetThm = target_ + "_thm";
        var targetLgd = "#legend";

        var expPlot = this;
        var ajaxCall = null;

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
            var geneids = $.map(genesToPlot, function (e) {
                return e.id;
            }).join(',');

            var designelements = $.map(genesToPlot, function (e) {
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

            if(currentEF == '') {
                var efKeys = []
                for(var key in genePlots) {
                    if(genePlots.hasOwnProperty(key))
                    efKeys.push(key)
                }

                currentEF = efKeys[0];
            }

            var series = []
            if(!genePlots[currentEF].box.seriesBackup)
                genePlots[currentEF].box.seriesBackup = genePlots[currentEF].box.series;

            for(var x = 0; x < genePlots[currentEF].box.seriesBackup.length; x++) {
               var s = genePlots[currentEF].box.seriesBackup[x];
               for(g in genesToPlot) {
                   if(genesToPlot[g].id==s.label.id) {
                       series.label = genesToPlot[g];
                       series.push(s);
                       break;
                   }
               }
            }

            genePlots[currentEF].box.series = series;
            onload(genePlots[currentEF].box);
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
                        var arr = [];
                        arr.push(gene.name || "");
                        arr.push(gene.designelement ? ":" + gene.designelement: "");
                        return $('<div/>').text(arr.join("") || "no label").append('&nbsp;<img id="rmgene' + gene.id + '"class="rmButton" height="8" src="images/closeButton.gif"/>').html();
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

            assayProperties = jsonObj.assayProperties || [];
            assayOrder = jsonObj.assayOrder || [];

            createPlotOverview(jsonObj);
            populateSimMenu(jsonObj.simInfo);

            $(".rmButton").hover(function() {
                $(this).attr("src", "images/closeButtonO.gif");
            }, function() {
                $(this).attr("src", "images/closeButton.gif");
            }).click(function() {
                expPlot.removeGeneFromPlot($(this).attr('id').substring(6));
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
            if (assayProperties.length == 0) {
                return;
            }

            var ul = $('<ul/>');

            var keys = {};
            var efvs = assayProperties[dataIndex].efvs;
            for (i = 0; i < efvs.length; ++i) if (efvs[i].v != '') {
                ul.append($('<li/>')
                        .css('padding', '0px')
                        .text(efvs[i].v)
                        .prepend($('<span/>').css('fontWeight', 'bold').text(curatedEFs[efvs[i].k] + ': ')));
                keys[efvs[i].k] = efvs[i].v;
            }

            var scvs = assayProperties[dataIndex].scvs;
            for (var i = 0; i < scvs.length; ++i) if (scvs[i].v != '' && keys[scvs[i].k] != scvs[i].v) {
                ul.append($('<li/>')
                        .css('padding', '0px')
                        .text(scvs[i].v)
                        .prepend($('<span/>').css('fontWeight', 'bold').text(curatedSCs[scvs[i].k] + ': ')));
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
            var f,t,max,range,oldf,oldt;
            max = plot.getData()[0].data.length;
            if (overview.getSelection() != null) {
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
            } else {
                range = max;
                oldt = max;
                oldf = 0;
            }
            var windowSize = Math.floor(2 / 3 * range);
            var offset = Math.floor((range - windowSize) / 2);
            f = oldf + offset;
            t = Math.floor(oldt - offset);
            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
        };

        expPlot.zoomOut = function(completely) {
            var f,t,max,range,oldf,oldt;

            if (completely) {
                max = plot.getData()[0].data.length;
                $(target).trigger("plotselected", { xaxis: { from: 0, to: max }});
                overview.clearSelection(true);
                return;
            }

            max = plot.getData()[0].data.length;
            if (overview.getSelection() != null) {
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
            } else {
                return;
            }
            var windowSize = Math.floor(3 / 2 * range);//alert(windowSize);
            var offset = Math.max(Math.floor((windowSize - range) / 2), 2);
            f = Math.max(oldf - offset, 0);
            t = Math.min(Math.floor(oldt + offset), max);

            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
            if (f == 0 && t == max) overview.clearSelection(true);
        };


        expPlot.panRight = function() {
            var f,t,max,range,oldf,oldt;
            max = plot.getData()[0].data.length;
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
            var f,t,max,range,oldf,oldt;
            max = plot.getData()[0].data.length;
            if (overview.getSelection() != null) {
                range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
                oldf = overview.getSelection().xaxis.from;
                oldt = overview.getSelection().xaxis.to;
            } else {
                return;
            }
            f = Math.max(oldf - 3, 0);
            t = f + range;
            $(target).trigger("plotselected", { xaxis: { from: f, to: t }});
        };

        expPlot.highlightPoints = function(sc, scv, assay, self) {
            clearSelections();

            for (var pointIndex = 0; pointIndex < assayProperties.length; ++pointIndex) {
                var ap = assay ? assayProperties[pointIndex].efvs : assayProperties[pointIndex].scvs;
                for (var j = 0; j < ap.length; ++j) {
                    if (ap[j].k == sc && ap[j].v == scv) {
                        for (var geneIndex = 0; geneIndex < plot.getData().length; ++geneIndex)
                            plot.highlight(geneIndex, pointIndex);
                        prevSelections.push(pointIndex);
                    }
                }
            }

            $(".sample_attr_value").css('font-weight', 'normal');
            $(self).css('font-weight', 'bold');
        };

        expPlot.addGeneToPlot = function(geneid, geneidentifier, genename, ef, designelement) {
            for (var i = 0; i < genesToPlot.length; ++i) {
                if ((genesToPlot[i].id == geneid)&&(genesToPlot[i].designelement == designelement))
                    return;
            }

            genesToPlot.push({ id: geneid, identifier: geneidentifier, name: genename, designelement: designelement});
            currentEF = ef;

            expPlot.reload();
        };

        expPlot.removeGeneFromPlot = function(geneId) {

            if (genesToPlot.length == 1)
                return;

            for (var i = 0; i < genesToPlot.length; i++) {
                if (genesToPlot[i].id == geneId) {
                    genesToPlot.splice(i, 1);
                }
            }

            expPlot.reload();
        };

        expPlot.changePlottingType = function(type) {
            if (plotTypes[type]) {
                plotType = plotTypes[type]();
                expPlot.reload();
            } else {
                if (console){
                    console.log("unknown plot type: " + type);
                }
            }
        };

    };
}());

var genesToPlot = [];
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
            + (ef != '' && efv != '' ? updnFilter + ef + '=' + efv : '')
            + (ef != '' && efv == '' ? updnFilter + ef + '=' : '')
            + (ef == '' && efv != '' ? updnFilter + efv.split("||")[0] + '=' + efv.split("||")[1]: '');

    if(window.console != undefined)
      console.log(dataUrl)

    atlas.ajaxCall(dataUrl,"", function(data) {
        var plotGeneCounter = 3;
        var r = [];
        for(var eaIdx in data.results[0].expressionAnalyses) {
            var ea = data.results[0].expressionAnalyses[eaIdx]
            r.push({
                 gene: ea.geneName,
             geneName: ea.geneName,
               geneId: ea.geneId,
       geneIdentifier: ea.geneIdentifier,
                   de: ea.designElementAccession,
                   ef: curatedEFs[ea.ef],
                rawef: ea.ef,
                  efv: ea.efv,
               pvalue: ea.pvalPretty,
                tstat: ea.tstat,
                 expr: ea.expression
            })

            if(plotGeneCounter-- > 0)
              genesToPlot.push({id:ea.geneId, identifier:ea.geneIdentifier, name: ea.geneName, designelement: ea.designElementAccession});
        }

        showTable(r);
        $('#expressionTableBody').data('json', data);
        drawPlot("boxplot");

        $("#qryHeader").hide();

    })
}

function showTable(expressionValues){
    //$.template("expressionValueTableRowTemplate","<tr><td><a onclick=\"addGeneToPlot('${geneId}','${geneIdentifier}','${geneName}','${rawef}','${de}');return false;\"'><img border='0' src='images/iconf.png'/></a></td><td>${gene}</td><td>${de}</td><td>${ef}</td><td>${efv}</td><td>${expr}</td><td>${tstat}</td><td>${pvalue}</td></tr>");
    $("#expressionValueTableRowTemplate1").tmpl(expressionValues).appendTo($("#expressionTableBody").empty());

    addGeneToolTips();
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

            var dataUrl = "api?geneIs=ENSG00000001167&format=json";

            var resultData = "<div id='oneAndOnlyTooltip'><img src='" + atlas.homeUrl + "images/indicator.gif' />&nbsp;Searching...</div>";

            atlas.ajaxCall(dataUrl,"", function(data) {
                //alert("received" + data.length);
                var str = "";

                $("#geneInfoTemplate").tmpl(data.results[0].gene).appendTo($("#oneAndOnlyTooltip").empty());
            });

            return resultData;
            //return $(this).next('.gtooltip').html();
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
    for (var i = 0; i < genesToPlot.length; ++i)
        url += '&gene=' + genesToPlot[i].identifier;
    return url;
}


var expPlot;

function drawPlot(plotType) {
    expPlot = new ExperimentPlot("#plot", plotType);
}

function changePlotType(plotType) {
    expPlot.changePlottingType(plotType);
}

function highlightPoints(sc, scv, assay, self) {
    expPlot.highlightPoints(sc, scv, assay, self);
}

function addGeneToPlot(geneid, geneidentifier, genename, ef, designelement) {
    expPlot.addGeneToPlot(geneid, geneidentifier, genename, ef, designelement);
}



