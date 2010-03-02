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

var genesToPlot = [];
var currentEF = [];
var experiment = {};
var curatedSCs = {};
var curatedEFs = {};
var assayProperties = [];
var assayOrder = [];
var plot;
var prevSelections = {};

function plotZoomOverview(jsonObj, plot){
    var divElt = $('#plot_thm');
    divElt.width(500);
    divElt.height(60);

    var overview = $.plot($('#plot_thm'), jsonObj.series, $.extend(true,{},jsonObj.options,{yaxis: {ticks: 0, labelWidth: 40, min: -plot.getData()[0].yaxis.datamax*0.25},series:{points:{show: false}}, grid:{backgroundColor:'#F2F2F2', markings:null,autoHighlight: false},legend:{show:false}, colors:['#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3']}));
    $("#plot_thm #plotHeader").remove();
    bindZooming(overview,jsonObj);

}

function bindZooming(overview, jsonObj){
    $("#zoomin").show();
    $("#zoomout").show();

    $('#plot').unbind("plotselected");
    $('#plot').bind("plotselected", function (event, ranges) {
        // do the zooming
        plot = $.plot($('#plot'), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
            xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }, yaxis: { labelWidth: 40 }
        }));

        // don't fire event on the overview to prevent eternal loop
        overview.setSelection(ranges, true);
        return plot;

    });
    $('#plot_thm').unbind("plotselected");
    $('#plot_thm').bind("plotselected", function (event, ranges) {
        plot.setSelection(ranges);
    });

    $("#zoomin").unbind("click");
    $("#zoomin").bind("click",function(){
        var f,t,max,range,oldf,oldt;
        max = plot.getData()[0].data.length;
        if(overview.getSelection() != null ){
            oldf=overview.getSelection().xaxis.from;
            oldt=overview.getSelection().xaxis.to;
            range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
        }else{
            range = max;
            oldt=max;
            oldf=0;
        }
        var windowSize = Math.floor(2/3*range);
        var offset = Math.floor((range-windowSize)/2);
        f=oldf+offset;
        t=Math.floor(oldt-offset);
        $('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});

    });
    $("#zoomout").unbind("click");
    $("#zoomout").bind("click",function(){
        var f,t,max,range,oldf,oldt;
        max = plot.getData()[0].data.length;
        if(overview.getSelection() != null ){
            range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
            oldf=overview.getSelection().xaxis.from;
            oldt=overview.getSelection().xaxis.to;
        }else{
            return;
        }
        var windowSize = Math.floor(3/2*range);//alert(windowSize);
        var offset = Math.max(Math.floor((windowSize-range)/2),2);
        f= Math.max(oldf-offset,0);
        t= Math.min(Math.floor(oldt+offset),max);

        $('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});
        if(f==0 && t == max) overview.clearSelection(true);
    });

    // zoom out completely on double click
    $("#zoomout").unbind("dblclick");
    $("#zoomout").bind("dblclick", function() {
        var max = plot.getData()[0].data.length;
        $('#plot').trigger("plotselected",{ xaxis: { from: 0, to: max }});
        overview.clearSelection(true);
    });

    $("#panright > img").unbind("click");
    $("#panright > img").bind("click",function(){
        var f,t,max,range,oldf,oldt;
        max = plot.getData()[0].data.length;
        if(overview.getSelection() != null ){
            range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
            oldf=overview.getSelection().xaxis.from;
            oldt=overview.getSelection().xaxis.to;
        }else{
            return;
        }
        t= Math.min(oldt+3,max);
        f= t-range;

        $('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});
    });

    $("#panleft > img").unbind("click");
    $("#panleft > img ").bind("click",function(){
        var f,t,max,range,oldf,oldt;
        max = plot.getData()[0].data.length;
        if(overview.getSelection() != null ){
            range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
            oldf=overview.getSelection().xaxis.from;
            oldt=overview.getSelection().xaxis.to;
        }else{
            return;
        }
        f= Math.max(oldf-3,0);
        t= f+range;
        $('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});
    });
}

function clearSelections() {
    for(var j=0; j < plot.getData().length; j++)
        for(var i=0; i < prevSelections.length; ++i)
            plot.unhighlight(j, prevSelections[i]);
    prevSelections = [];
}

function highlightPoints(sc, scv, assay, self){
    clearSelections();

    for(var pointIndex = 0; pointIndex < assayProperties.length; ++pointIndex) {
        var ap = assay ? assayProperties[pointIndex].efvs : assayProperties[pointIndex].scvs;
        for(var j = 0; j < ap.length; ++j) {
            if(ap[j].k == sc && ap[j].v == scv) {
                for(var geneIndex = 0; geneIndex < plot.getData().length; ++geneIndex)
                    plot.highlight(geneIndex, pointIndex);
                prevSelections.push(pointIndex);
            }
        }
    }

    $(".sample_attr_value").css('font-weight','normal');
    $(self).css('font-weight', 'bold');
}

function plotBigPlot() {

    var geneids = $.map(genesToPlot, function (e) { return e.id; }).join(',');
    $.ajax({
        type: "GET",
        url: atlas.homeUrl + "plot",
        data: { gid: geneids, eid: experiment.id, ef: currentEF, plot: 'large' },
        dataType:"json",
        success: function(jsonObj){
            if(jsonObj.series){

                jsonObj.options.legend.labelFormatter = function (gene) {
                    return $('<div/>').text(gene.name).append('&nbsp;<img id="rmgene' + gene.id + '"class="rmButton" height="8" src="images/closeButton.gif"/>').html();
                };

                jsonObj.options.legend.container = "#legend";

                plot = $.plot($('#plot'), jsonObj.series, $.extend(true, {}, jsonObj.options, {yaxis:{labelWidth:40}} ));

                assayProperties = jsonObj.assayProperties;

                if(currentEF == '')
                    currentEF = jsonObj.ef;

                if(prevSelections.length > 0) {
                    // remap selections according to current assay order
                    for(var si = 0; si < prevSelections.length; ++si) {
                        var selAssay = assayOrder[prevSelections[si]];
                        for(var ai = 0; ai < jsonObj.assayOrder.length; ++ai)
                            if(jsonObj.assayOrder[ai] == selAssay) {
                                prevSelections[si] = ai;
                                break;
                            }
                    }
                    for(var j=0; j < plot.getData().length; j++)
                        for(var i=0; i < prevSelections.length; ++i)
                            plot.highlight(j, prevSelections[i]);
                }

                assayOrder = jsonObj.assayOrder;

                redrawEFpagination();
                plotZoomOverview(jsonObj, plot);
                populateSimMenu(jsonObj.simInfo);

                $(".rmButton").hover(function() {
                    $(this).attr("src","images/closeButtonO.gif");
                }, function() {
                    $(this).attr("src","images/closeButton.gif");
                }).click(function() {
                    removeGene($(this).attr('id').substring(6));
                });
            }
        },
        error: atlas.onAjaxError
    });
}

function removeGene(geneId) {
    if (genesToPlot.length == 1)
        return;

    for (var i = 0; i < genesToPlot.length; i++)
    {
        if (genesToPlot[i].id == geneId) {
            genesToPlot.splice(i, 1);
        }
    }

    plotBigPlot();
}

function drawZoomControls(){
    var contents="";
    contents= '<div id="zoomin"  style="z-index:1; position:relative; left: 0px; top: 5px;cursor:pointer;display:hidden"><img style="cursor:pointer" src="images/zoomin.gif" title="Zoom in"></div>' +
              '<div id="zoomout" style="z-index:1; position: relative; left: 0px; top: 5px;cursor:pointer; display:hidden"><img src="images/zoomout.gif" title="Zoom out"></div>' +
              '<div id="panright" style="z-index:2;position: relative; left: 20px; top: -35px;cursor:pointer; display:hidden"><img src="images/panright.gif" title="pan right"></div>' +
              '<div id="panleft" style="z-index:2;position: relative; left: -15px; top: -69px;cursor:pointer; display:hidden"><img src="images/panleft.gif" title="pan left"></div>';
    $("#zoomControls").html(contents);

    $("#zoomin > img").hover(
            function(){
                $(this).attr("src","images/zoominO.gif");},
            function(){
                $(this).attr("src","images/zoomin.gif");
            }).mousedown(function(){
        $(this).attr("src","images/zoominC.gif");
    }).mouseup(function(){
        $(this).attr("src","images/zoominO.gif");
    });
    $("#zoomout > img").hover(
            function(){
                $(this).attr("src","images/zoomoutO.gif");},
            function(){
                $(this).attr("src","images/zoomout.gif");
            }).mousedown(function(){
        $(this).attr("src","images/zoomoutC.gif");
    }).mouseup(function(){
        $(this).attr("src","images/zoomoutO.gif");
    });
    $("#panright > img").hover(
            function(){
                $(this).attr("src","images/panrightO.gif");},
            function(){
                $(this).attr("src","images/panright.gif");
            }).mousedown(function(){
        $(this).attr("src","images/panrightC.gif");
    }).mouseup(function(){
        $(this).attr("src","images/panrightO.gif");
    });
    $("#panleft > img").hover(
            function(){
                $(this).attr("src","images/panleftO.gif");},
            function(){
                $(this).attr("src","images/panleft.gif");
            }).mousedown(function(){
        $(this).attr("src","images/panleftC.gif");
    }).mouseup(function(){
        $(this).attr("src","images/panleftO.gif");
    });
}

function showSampleTooltip(dataIndex,x,y){
    var ul = $('<ul/>');

    var scvs = assayProperties[dataIndex].scvs;
    for (var i = 0; i < scvs.length; ++i) {
        ul.append($('<li/>')
                .css('padding', '0px')
                .text(scvs[i].v)
                .prepend($('<span/>').css('fontWeight','bold').text(curatedSCs[scvs[i].k] + ': ')));
    }

    var efvs = assayProperties[dataIndex].efvs;
    for (i = 0; i < efvs.length; ++i) {
        ul.append($('<li/>')
                .css('padding', '0px')
                .text(efvs[i].v)
                .prepend($('<span/>').css('fontWeight','bold').text(curatedEFs[efvs[i].k] + ': ')));
    }

    $('<div id="tooltip"/>').append(ul).css( {
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

function populateSimMenu(simInfo){
    $("#simSelect").empty();
    for(var i=0; i < simInfo.length; i++){
        var key = simInfo[i].deId + "_" + simInfo[i].adId;
        $("#simSelect").append($('<option/>').val(key).text(simInfo[i].name));
    }
    $("#simSelect").selectOptions("select gene", true);
}

function redrawForEF(ef) {
    currentEF = ef;
    plotBigPlot();
    var efTxt = $('#efpage' + ef).text();
    $('#sortHeader').text("Expression profile sorted by " + efTxt);
}

function redrawEFpagination() {
    var root = $('#EFpagination');

    var old =$("span", root);
    if(old.length) {
        var oldef = old.attr('id').substring(6);
        old.replaceWith($('<a/>').attr('id', old.attr('id')).text(old.text()).click(function () { redrawForEF(oldef); }));
    }
    
    var curr = $('#efpage' + currentEF);
    if(curr.length) {
        var curref = curr.attr('id');
        var curreftext = curr.text();
        curr.replaceWith($('<span/>').attr('id', curref).text(curreftext).addClass('current'));
    }
}

function addGeneToPlot(geneid, geneidentifier, genename, ef) {
    for(var i = 0; i < genesToPlot.length; ++i) {
        if(genesToPlot[i].id == geneid)
            return;
    }

    genesToPlot.push({ id: geneid, identifier: geneidentifier, name: genename });
    currentEF = ef;

    plotBigPlot();
}

function addGeneToolTips() {
    $("#grid a.genename").tooltip({
        bodyHandler: function () {
            return $(this).next('.gtooltip').html();
        },
        showURL: false
    });
}

function bindPlotEvents() {
    $("#plot").bind("mouseleave",function(){
        $("#tooltip").remove();
    });

    $("#plot").bind("plotclick", function (event, pos, item) {
        if (item) {
            clearSelections();

            var pointIndex = Math.round(item.datapoint[0] - 0.5);
            for(var geneIndex = 0; geneIndex < plot.getData().length; ++geneIndex)
                plot.highlight(geneIndex, pointIndex);

            prevSelections = [ pointIndex ];
        }
    });

    var previousPoint = null;
    $("#plot").bind("plothover",function(event,pos,item) {
        if(item) {
            if (previousPoint != item.datapoint) {
                previousPoint = item.datapoint;
                $("#tooltip").remove();
                showSampleTooltip(item.dataIndex, item.pageX, item.pageY);
            }
        } else {
            $("#tooltip").remove();
            previousPoint = null;
        }
    });
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
        $("#simHeader").html("<img src='${pageContext.request.contextPath}/images/indicator.gif' />&nbsp;Searching for profiles similar to " +
                             name + "...");
        $("#simHeader").show();
        var DEid_ADid = $("select option:selected").val();
        var tokens = DEid_ADid.split('_');
        var DEid = tokens[0];
        var ADid = tokens[1];
        $("#simResult").load("${pageContext.request.contextPath}/expGenes", {eid: experiment.id, deid: DEid, adid: ADid, query:'sim'}, function() {
            $("#simHeader").hide();
            addGeneToolTips();
        });
        return false;
    });

    $("#searchForm").submit(function() {
        var qry = $("#geneInExp_qry").fullVal();
        $("#qryHeader").html("<img src='${pageContext.request.contextPath}/images/indicator.gif' />&nbsp;Loading...");
        $("#qryResult").load("${pageContext.request.contextPath}/expGenes", {eid: experiment.id, gene: qry, query:'search'}, function() {
            $("#qryHeader").hide()
            addGeneToolTips();
        });
        return false;
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
