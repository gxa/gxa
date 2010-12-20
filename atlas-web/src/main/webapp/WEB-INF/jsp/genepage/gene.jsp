<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>

<c:set var="timeStart" value="${u:currentTime()}"/>

<u:htmlTemplate file="look/genePage.head.html"/>
<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="differentiallyExpressedFactors" type="java.util.List<ae3.model.ExperimentalFactor>" scope="request"/>
<jsp:useBean id="atlasGene" type="ae3.model.AtlasGene" scope="request"/>
<jsp:useBean id="ef" class="java.lang.String" scope="request"/>
<jsp:useBean id="anatomogramMap" type="java.util.List<ae3.anatomogram.AnatomogramArea>" scope="request"/>

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas Summary"/>
<meta name="Keywords"
      content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

<jsp:include page="../includes/query-includes.jsp"/>
<!--[if IE]><script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script language="javascript" type="text/javascript"
        src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>


<script type="text/javascript">

function showThumbTooltip(x, y, contents) {
    if (contents != 'Mean') {
        $('<div id="tooltip" />').text(contents).css({
            position: 'absolute',
            display: 'none',
            top: (y + 5) + 'px',
            left: (x + 5) + 'px',
            border: '1px solid #005555',
            margin: '0px',
            backgroundColor: '#EEF5F5'
        }).appendTo("body").fadeIn('fast');
    }
}

function drawPlot(jsonObj, plot_id) {
    if (jsonObj.series) {
        var legend_id = plot_id.replace("_plot", "_legend");
        jsonObj.options.legend.container = '#' + legend_id;
        var arraydesign_id = plot_id.replace("_plot", "_arraydesign");
        jsonObj.options.arrayDesignContainer = '#' + arraydesign_id;

        var plotted_ef = jsonObj.options.ef;
        var tokens = plot_id.split('_');
        var eid = tokens[0];
        var eacc = tokens[1];
        var gid = tokens[2];
        var efv;
        drawEFpagination(eid, eacc, gid, plotted_ef, efv);

        var plot = $.plot($('#' + plot_id), jsonObj.series, jsonObj.options);

        var previousPoint = null;
        $('#' + plot_id).bind("plothover", function (event, pos, item) {
            if (item) {
                if (previousPoint != item.datapoint) {
                    previousPoint = item.datapoint;
                    $("#tooltip").remove();
                    showThumbTooltip(item.pageX, item.pageY, item.series.label);
                }
            } else {
                $("#tooltip").remove();
                previousPoint = null;
            }
        });
        return plot;
    }
    return null;
}

function drawPlots() {
    $(".plot").each(function() {
        var plot_id = this.id;
        var tokens = plot_id.split('_');
        var eid = tokens[0];
        var eacc = tokens[1];
        var gid = tokens[2];
        atlas.ajaxCall("plot", { gid: gid, eid: eid, eacc: eacc, plot: 'bar' }, function(o) {
            drawPlot(o, plot_id);
        });
    });
}

function redrawPlotForFactor(eid, eacc, gid, ef, mark, efv) { 
    var plot_id = eid + "_" + eacc + "_" + gid + "_plot";
    atlas.ajaxCall("plot", { gid: gid, eid: eid, eacc: eacc, ef: ef, efv: efv, plot: 'bar' }, function(o) {
        var plot = drawPlot(o, plot_id);
        if (mark) {
            markClicked(eid, eacc, gid, ef, efv, plot, o);
        }
    });
}

function drawEFpagination(eid, eacc, gid, currentEF, plotType, efv) {
    var panelContent = [];

    $("#" + eid + "_EFpagination *").each(function() {
        var ef = $(this).attr("id");
        var ef_txt = $(this).html();
        if (ef == currentEF) {
            panelContent.push("<span id='" + ef + "' class='current'>" + ef_txt + "</span>")
        }
        else {
            panelContent.push('<a id="' + ef + '" onclick="redrawPlotForFactor( \'' + eid + '\',\'' + eacc + '\',\'' + gid + '\',\'' + ef + '\',\'' + efv + '\',\'' + plotType + '\',false)">' + ef_txt + '</a>');
        }
    });

    $("#" + eid + "_EFpagination").empty();
    $("#" + eid + "_EFpagination").html(panelContent.join(""));
}


function markClicked(eid, eacc, gid, ef, efv, plot, jsonObj) {

    var plot_id = eid + '_' + eacc + '_' + gid + '_plot';
    var allSeries = plot.getData();
    var series;
    var markColor;

    for (var i = 0; i < allSeries.length; ++i) {
        if (allSeries[i].label) {
            if (allSeries[i].label.toLowerCase() == efv.toLowerCase()) {
                series = allSeries[i];
                markColor = series.color;
                break;
            }
        }
    }

    if (series == null) {
        return;
    }

    var data = series.data;
    var xMin = data[0][0];
    var xMax = data[data.length - 1][0];

    var overviewDiv = $('#' + plot_id + '_thm');
    if (allSeries.length > 10 && data.length < 5) {
        if (overviewDiv.height() != 0) {
            var overview = $.plot($('#' + plot_id + '_thm'), jsonObj.series, jsonObj.options);

            overview.setSelection({ xaxis: { from: xMin - 10, to: xMax + 10 }});
        }
        plot = $.plot($('#' + plot_id), jsonObj.series, $.extend(true, {}, jsonObj.options, {
            grid:{ backgroundColor: '#fafafa',    autoHighlight: true, hoverable: true, borderWidth: 1, markings: [
                { xaxis: { from: xMin - 1, to: xMax + 1 }, color: '#FFFFCC' }
            ]},
            xaxis: { min: xMin - 10, max: xMax + 10  }, yaxis: {labelWidth:40}
        }));
    }
    else {

        plot = $.plot($('#' + plot_id), jsonObj.series, $.extend(true, {}, jsonObj.options, {
            grid:{ backgroundColor: '#fafafa',    autoHighlight: true, hoverable: true, borderWidth: 1, markings: [
                { xaxis: { from: xMin - 1, to: xMax + 1 }, color: '#FFFFCC' }
            ]},
            yaxis: {labelWidth:40}
        }));
        if (overviewDiv.height() != 0) {
            var overview = $.plot($('#' + plot_id + '_thm'), jsonObj.series, $.extend(true, {}, jsonObj.options, {color:['#999999','#D3D3D3']}));

            overview.setSelection({ xaxis: { from: xMin - 10, to: xMax + 10 }});
        }
    }
}


function reloadExps() {

    $('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList", {gid:${atlasGene.geneId},from:"1", to:"5", factor:"${ef}"}, drawPlots);
    $('#pagingSummary').empty();
    $(".heatmap_over").removeClass("heatmap_over");
    paginateExperiments();
    countExperiments();
}

function countExperiments() {
    $("#pagingSummary").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");
}

function paginateExperiments() {
    // Create pagination element
<c:if test="${noAtlasExps > 5}">
    $("#Pagination").pagination(${noAtlasExps}, {
        num_edge_entries: 2,
        num_display_entries: 5,
        items_per_page:5,
        callback: pageselectCallback
    });
</c:if>
}

function pageselectCallback(page_id) {
    var fromPage = (page_id * 5) + 1;
    var toPage = (page_id * 5) + 5;
    $('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList", {gid:${atlasGene.geneId},from:fromPage, to: toPage, factor:"${ef}"}, drawPlots);
}


function FilterExps(el, fv, ef) {

    $('#ExperimentResult').empty();

    $('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList", {gid:${atlasGene.geneId}, efv: fv, factor:ef},
            function() {
                for (var i = 0; i < exps.length; ++i) {
                    var eid = jQuery.trim(exps[i].id);
                    var eacc = jQuery.trim(exps[i].acc);
                    redrawPlotForFactor(eid, eacc, '${atlasGene.geneId}', ef, true, fv);
                }
                $('#pagingSummary').text(exps.length + " experiment" + (exps.length > 1 ? "s" : '') + " showing differential expression in \"" + fv + "\"");
                var lnk = $("<a>Show all studies</a>").bind("click", reloadExps);
                $("#Pagination").empty().append(lnk);
            });


    old = $(".heatmap_over");
    old.removeClass("heatmap_over");
    old.addClass("heatmap_row");
    el.className = "heatmap_over";
    //window.scrollTo(0,0);
}


function FilterExpsEfo(el, efo) {
    $('#ExperimentResult').empty();
    $('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList", {gid:${atlasGene.geneId}, efo: efo},
            function() {
                for (var i = 0; i < exps.length; ++i) {
                    var eid = jQuery.trim(exps[i].id);
                    var eacc = jQuery.trim(exps[i].acc);
                    redrawPlotForFactor(eid, eacc, '${atlasGene.geneId}', 'organism_part', true, '');
                }
                $('#pagingSummary').text(exps.length + " experiment" + (exps.length > 1 ? "s" : '') + " showing differential expression in \"" + efo + "\"");
                var lnk = $("<a>Show all studies</a>").bind("click", reloadExps);
                $("#Pagination").empty().append(lnk);
            });
    old = $(".heatmap_over");
    old.removeClass("heatmap_over");
    old.addClass("heatmap_row");
    el.className = "heatmap_over";
    //window.scrollTo(0,0);
}

jQuery(document).ready(function()
{
    countExperiments();
    paginateExperiments();
    $("#heatmap_tbl").tablesorter({
        headers: {2: {sorter: false}}});
    drawPlots();
});
</script>


<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css"
      media="print, projection, screen"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
${atlasProperties.htmlBodyStart}

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right:10px;">
            <a href="${pageContext.request.contextPath}/" title="Gene Expression Atlas Homepage"><img border="0"
                                                                                                      width="55"
                                                                                                      src="${pageContext.request.contextPath}/images/atlas-logo.png"
                                                                                                      alt="Gene Expression Atlas"/></a>
        </td>
        <td align="right" valign="bottom">
            <a href="${pageContext.request.contextPath}/">home</a> |
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks"
                                                                                          style="font-weight:bold;display:none">thanks!</span>
            |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>


<table cellspacing="0" cellpadding="0" border="0" width="100%">
    <tr>
        <td style="vertical-align:top;">
            <table width="100%" style="background-color: white" class="geneAnnotations">

                <tr>
                    <td align="left" class="geneName">${atlasGene.geneName}
                        <div style="font:normal"><c:import url="../includes/apilinks.jsp"><c:param name="apiUrl"
                                                                                                   value="geneIs=${atlasGene.geneIdentifier}"/></c:import></div>
                    </td>
                    <td style="vertical-align: text-bottom">${atlasGene.geneSpecies}</td>
                    <td rowspan="">

                    </td>
                </tr>

                <tr>
                    <td colspan="2" align="left" style="padding-bottom:1em;padding-top:1em">
                        ${f:escapeXml(atlasGeneDescription)}
                    </td>
                </tr>


                <c:set var="synonyms">
                    <c:forEach items="${atlasProperties.geneAutocompleteNameFields}" var="prop" varStatus="s"><c:if
                            test="${!empty atlasGene.geneProperties[prop]}">${u:join(atlasGene.geneProperties[prop], ", ")}
                        <c:if test="${!s.last}">, </c:if></c:if></c:forEach>
                </c:set>
                <c:if test="${!empty synonyms}">
                    <tr>
                        <td class="geneAnnotHeader">Synonyms</td>
                        <td align="left">${synonyms}</td>
                    </tr>
                </c:if>

                <c:if test="${!empty orthologs}">
                    <tr>
                        <td class="geneAnnotHeader">Orthologs</td>

                        <td align="left">
                            <c:set var="orthoIds" value=""/>
                            <c:forEach var="ortholog" items="${orthologs}">
                                <a href="${pageContext.request.contextPath}/gene/${ortholog.geneIdentifier}"
                                   target="_self"
                                   title="Gene Atlas Data For ${ortholog.geneName} (${ortholog.geneSpecies})">${ortholog.geneName}
                                    (${ortholog.geneSpecies})</a>&nbsp;
                                <c:set var="orthoIds" value="${orthoIds}${ortholog.geneIdentifier}+"/>
                            </c:forEach>
                            (<a href="${pageContext.request.contextPath}/qrs?gprop_0=&gval_0=${orthoIds}${atlasGene.geneIdentifier}&fexp_0=UP_DOWN&fact_0=&specie_0=&fval_0=(all+conditions)&view=hm"
                                target="_self">Compare orthologs</a>)
                        </td>
                    </tr>
                </c:if>

                <c:forEach items="${atlasGene.genePropertiesIterator}" var="prop">
                    <c:if test="${!u:isIn(atlasProperties.geneAutocompleteNameFields, prop) && !u:isIn(atlasProperties.genePageIgnoreFields, prop)}">
                        <tr class="${u:isIn(atlasProperties.genePageDefaultFields, prop) ? '' : 'expandable'}"
                            style="${u:isIn(atlasProperties.genePageDefaultFields, prop) ? '' : 'display:none'}">
                            <td class="geneAnnotHeader">${f:escapeXml(atlasProperties.curatedGeneProperties[prop])}</td>
                            <td align="left">
                                <c:choose>
                                    <c:when test="${!empty atlasProperties.genePropertyLinks[prop]}">
                                        <c:forEach items="${atlasGene.geneProperties[prop]}" var="v" varStatus="s">
                                            <a href="${f:replace(atlasProperties.genePropertyLinks[prop], '$$', v)}"
                                               target="_blank">${f:escapeXml(v)}</a><c:if test="${!s.last}">, </c:if>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        ${u:limitedJoin(atlasGene.geneProperties[prop], 5, ", ", "...")}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:if>
                </c:forEach>

                <tr>
                    <td class="geneAnnotHeader">Search EB-eye</td>
                    <td align="left">
                        <a title="Show gene annotation" target="_blank"
                           href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=${atlasGene.geneIdentifier}">
                            ${atlasGene.geneIdentifier}
                        </a>
                    </td>

                    <!-- td width="8%">&nbsp;</td-->

                </tr>

                <tr>
                    <td colspan="2" style="padding-top:5px">
                        <a href="#"
                           onclick="javascript:$(this).parents('table:first').find('.expandable').show();$(this).remove();return false;"><img
                                src="${pageContext.request.contextPath}/images/expp.gif" alt="" border="none"> Show more
                            properties</a>
                    </td>
                </tr>
            </table>
        </td>
        <td style="padding-top:15px;">
            <c:if test="${hasAnatomogram}">
                <!--
                <img src="${pageContext.request.contextPath}/anatomogram/${atlasGene.geneIdentifier}.png" alt="anatomogram" border="1px" />
                -->
            </c:if>
        </td>
    </tr>
</table>

<table cellspacing="0" cellpadding="0" style="margin-top:40px">


<tr>
<td>
<table cellspacing="0" cellpadding="0" border="0">
<tr>
<td valign="top" width="50%">
<table>
<tr>
    <td class="sectionHeader" style="padding-right:20px;">


        <c:choose>
            <c:when test="${not empty ef}">
                ${f:escapeXml(atlasProperties.curatedEfs[ef])}
                <div style="font-size:10px; font-weight:normal;"><a
                        href="${pageContext.request.contextPath}/gene/${atlasGene.geneIdentifier}">&lt;&lt;view all
                    experimental factors</a></div>
            </c:when>
            <c:otherwise>
                Experimental Factors
            </c:otherwise>
        </c:choose>

    </td>
</tr>

<tr>
<td style="padding-top: 3px; width:60%;">

<table cellpadding="0" style="width:100%;" border="0">
    <tr>

        <c:forEach var="experimentalFactor" items="${differentiallyExpressedFactors}" varStatus="i" end="5">
        <c:if test="${(i.index mod 2)==0}">
    </tr>
    <tr>
        </c:if>

        <td style="vertical-align:top; padding-right:20px;">
            <c:if test="${empty ef}">
                <div class="geneAnnotHeader"
                     style="width:200px;">${f:escapeXml(atlasProperties.curatedEfs[experimentalFactor.name])}</div>
            </c:if>
            studied in
            <c:forEach var="experiment" items="${experimentalFactor.experiments}" varStatus="i_e">
                <c:if test="${(i_e.index<5)||(not empty ef)}">
                    <a href="${pageContext.request.contextPath}/experiment/${experiment}/${atlasGene.geneIdentifier}"
                       title="${experiment}">${experiment}</a><c:if test="${!i_e.last}">, </c:if>
                </c:if>
                <c:if test="${i_e.last}">
                    <c:if test="${(i_e.count>=5)&&(empty ef)}">
                        ... (${i_e.count} experiments)
                    </c:if>
                </c:if>
            </c:forEach>

             <!-- Output legend for the first experimental factor only -->
             <c:if test="${i.index==0}">
                   <table cellspacing="2" cellpadding="0" border="0" width="100%">
                        <tr>
                            <td style="vertical-align:middle;">
                                <div class="sq">
                                    <div class="udduo"></div>
                                    <div class="uddnval">3</div>
                                    <div class="udupval">1</div>
                                </div>
                            </td>
                            <td>
                                <div style="padding-left:0px; font-size:12px;">
                                    Number of published studies where the gene is <span
                                        style="color:red">over</span>/<span
                                        style="color:blue">under</span> expressed compared to the gene's overall mean
                                    expression level in the study.
                                </div>
                            </td>
                        </tr>
                    </table>
            </c:if>

            <c:if test='${experimentalFactor.name=="organism_part" && hasAnatomogram}'>
                <br/>

                <div style="overflow:hidden; <c:if test="${empty ef}">width:300px;</c:if>">
                    <img src="${pageContext.request.contextPath}/<c:if test="${empty ef}">web</c:if>anatomogram/${atlasGene.geneIdentifier}.png"
                         alt="anatomogram" border="none" usemap="#anatomogram"/>
                </div>
                <!--
                <map name="anatomogram">
                <c:forEach var="anatomogramArea" items="${anatomogramMap}">
                    <area shape="rect"
                    coords="${anatomogramArea.x0},${anatomogramArea.y0},${anatomogramArea.x1},${anatomogramArea.y1}"
                    alt="${anatomogramArea.efo}"
                    href="/${anatomogramArea.efo}.html"
                    onclick="FilterExpsEfo(this,'${u:escapeJS(anatomogramArea.efo)}'); return false;">

                </c:forEach>
                </map>
                -->

                <!--
                <c:forEach var="anatomogramArea" items="${anatomogramMap}">
                    "${anatomogramArea.efo}"
                </c:forEach>
                -->

                <c:if test="${empty ef}">
                    <div style="padding-left:0px; font-size:10px;">
                       <c:choose>
                           <c:when test="${experimentalFactor.name != 'organism_part'}">
                                <a href="${pageContext.request.contextPath}/gene/${atlasGene.geneIdentifier}?ef=${experimentalFactor.name}">show
                            this factor only&gt;&gt;</a>
                            </c:when>
                            <c:otherwise>
                                 <a href="${pageContext.request.contextPath}/gene/${atlasGene.geneIdentifier}?ef=${experimentalFactor.name}">show
                            expression data for <b>all</b> values of this factor&gt;&gt;</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>

            </c:if>
            <c:if test='${experimentalFactor.name!="organism_part" || not hasAnatomogram || not empty ef}'>
                <!--generic ef - the above clause imposes the following rules:
                1. in multi-experimental factor experiment view:
                a. ef != 'organism_part' => always show a table
                b. ef == 'organism_part'
                - anatomogram exists => show anatomogram
                - anatomogram doesn't exist => show table
                2. in single-experimental factor experiment view: (ef != null):
                a. as 1a
                b ef == 'organism_part' => show anatomogram (if exists) AND the table
                -->

                <table class="heatmap" cellpadding="0" cellspacing="0" border="0"
                       style="margin-top:5px; margin-left:0px; width:100%;">
                    <thead>
                    <tr style="height:26px;border-top:1px solid #CDCDCD">
                        <th style="border: 1px solid #CDCDCD;padding: 1px 5px 1px 4px; width:180px;">Factor Value</th>
                        <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding: 1px 2px 1px 4px; width:20px;">
                            <span style="font-size:8px">U/D</span></th>
                        <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding: 1px 2px 1px 4px;">
                            Experiments
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="values" value="${experimentalFactor.topValues}"/>
                    <c:if test="${not empty ef}">
                        <c:set var="values" value="${experimentalFactor.values}"/>
                    </c:if>
                    <c:forEach var="e" items="${values}" varStatus="i">
                        <c:if test='${e.efv!="(empty)"}'>
                            <tr class="heatmap_row"
                                onclick="FilterExps(this,'${u:escapeJS(e.efv)}','${u:escapeJS(e.ef)}'); return false;"
                                title="${u:upcaseFirst(e.efv)}">
                                <td style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD; min-width: 100px;border-left:1px solid #CDCDCD;">
                                                                    <span style="font-weight: bold">
                                                                            ${u:upcaseFirst(e.efv)}
                                                                    </span>
                                </td>

                                <c:set var="ud" value="${e.payload}"/>
                                <c:choose>
                                    <c:when test="${empty ud || ud.ups + ud.downs + ud.nones == 0}">
                                        <td class="counter"><c:choose><c:when test="${j.first}">
                                            <div class="osq"></div>
                                        </c:when></c:choose></td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs == 0 && ud.nones > 0}">
                                        <td class="acounter" style="color:black;"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is non-differentially in ${ud.nones} experiment(s)."
                                                >
                                            <div class="osq">${ud.nones}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones == 0}">
                                        <td class="acounter upback"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s)."
                                                >
                                            <div class="osq">${ud.ups}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones == 0}">
                                        <td class="acounter downback"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="osq">${ud.downs}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones > 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and not differentially expressed in ${ud.nones} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="nuduo"></div>
                                                <div class="nunoval">${ud.nones}</div>
                                                <div class="nuupval">${ud.ups}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones > 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) underexpressed in ${ud.downs} and not differentially expressed in ${ud.nones} experiment(s)."
                                            onclick="atlas.hmc(${i.index},${j.index},event || window.event)">
                                            <div class="sq">
                                                <div class="ndduo"></div>
                                                <div class="ndnoval">${ud.nones}</div>
                                                <div class="nddnval">${ud.downs}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs > 0 && ud.nones == 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="udduo"></div>
                                                <div class="uddnval">${ud.downs}</div>
                                                <div class="udupval">${ud.ups}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:otherwise>
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="tri"></div>
                                                <div class="tdnval">${ud.downs}</div>
                                                <div class="tupval">${ud.ups}</div>
                                                <div class="tnoval">${ud.nones}</div>
                                            </div>
                                        </td>
                                    </c:otherwise>
                                </c:choose>

                                <td style="border:1px solid #CDCDCD; padding-left:5px;">
                                                            <span style="font-size:smaller">
                                                            <c:forEach var="experimentID" items="${ud.experiments}"
                                                                       varStatus="i_e">
                                                                <c:if test="${(i_e.index<5)}">
                                                                    <a href="${pageContext.request.contextPath}/experiment/${experimentalFactor.experimentAccessions[experimentID]}/${atlasGene.geneIdentifier}"
                                                                       onclick="window.location=this.href;"
                                                                       title="${experimentalFactor.experimentAccessions[experimentID]}">${experimentalFactor.experimentAccessions[experimentID]}</a><c:if
                                                                        test="${!i_e.last}">, </c:if>
                                                                </c:if>
                                                                <c:if test="${i_e.last}">
                                                                    <c:if test="${(i_e.count>=5)}">
                                                                        ... (${i_e.count} experiments)
                                                                    </c:if>
                                                                </c:if>
                                                            </c:forEach>
                                                            </span>
                                </td>

                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>


                <div style="padding-left:0px">
                    <c:if test="${(experimentalFactor.moreValuesCount>0)&&(empty ef)}">
                        ${experimentalFactor.moreValuesCount} more value(s).
                    </c:if>
                    <c:if test="${empty ef}">
                        <div style="font-size:10px;">
                            <a href="${pageContext.request.contextPath}/gene/${atlasGene.geneIdentifier}?ef=${experimentalFactor.name}">show
                                this factor only&gt;&gt;</a>
                        </div>
                    </c:if>
                </div>
                <!--generic ef -->
            </c:if>

            <br/><br/>

        </td>
        </c:forEach>

        <c:forEach var="k" begin="${4 - i.index mod 2}" end="2">
            <td>

            </td>
        </c:forEach>


    </tr>
</table>

<c:forEach var="experimentalFactor" items="${differentiallyExpressedFactors}" varStatus="i" begin="6">
    <c:if test="${empty ef}">
        <div class="geneAnnotHeader">${f:escapeXml(atlasProperties.curatedEfs[experimentalFactor.name])}</div>
    </c:if>
    studied in
    <c:forEach var="experiment" items="${experimentalFactor.experiments}" varStatus="i_e">
        <c:if test="${(i_e.index<5)||(not empty ef)}">
            <a href="${pageContext.request.contextPath}/experiment/${experiment}/${atlasGene.geneIdentifier}">${experiment}</a><c:if
                test="${!i_e.last}">, </c:if>
        </c:if>
        <c:if test="${i_e.last}">
            <c:if test="${(i_e.count>=5)&&(empty ef)}">
                ... (${i_e.count} experiments)
            </c:if>
        </c:if>
    </c:forEach>
    <c:if test="${empty ef}">
        <div style="font-size:10px;">
            <a href="${pageContext.request.contextPath}/gene/${atlasGene.geneIdentifier}?ef=${experimentalFactor.name}">show
                this factor only&gt;&gt;</a>
        </div>
    </c:if>
    <br/><br/>
</c:forEach>
</td>
</tr>
</table>

</td>
<td valign="top" align="left" width="50%">
    <table align="left">
        <tr>
            <td id="expHeader_td" class="sectionHeader" style="vertical-align: top">Expression Profiles</td>
            <td align="right">
                <div id="Pagination" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; "></div>
                <div id="Pagination1" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; ">
                </div>
            </td>
        </tr>

        <tr>
            <td align="left" valign="top" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                <div id="pagingSummary" class="header"></div>
            </td>
            <td align="right" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                <div id="expSelect"></div>
            </td>

        </tr>
        <tr>
            <td colspan="2">
                <div id="ExperimentResult">
                    <c:import url="/geneExpList">
                        <c:param name="gid" value="${atlasGene.geneId}"/>
                        <c:param name="from" value="1"/>
                        <c:param name="to" value="5"/>
                        <c:param name="factor" value="${ef}"/>
                    </c:import>
                </div>
            </td>
        </tr>
    </table>

</td>
</tr>
</table>
<!--
<div class="sectionHeader">Old School Vertical Heatmap</div>
<table class="heatmap" cellpadding="0" cellspacing="0" border="0" id ="heatmap_tbl">
                           <thead>
                               <tr style="height:26px;border-top:1px solid #CDCDCD">
                                   <th style="border: 1px solid #CDCDCD;padding: 1px 5px 1px 4px;">Factor Value</th>
                                   <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">Factor</th>
                                   <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding: 1px 2px 1px 4px;">E.</th>
                               </tr>
                               <tr>
                                   <td valign="top" height="30" align="center" colspan="3" style="border-bottom:1px solid #CDCDCD;background-color:white;border-left:1px solid #CDCDCD;border-right:1px solid #CDCDCD">
                                       Legend: <img style="position:relative;top:6px" src="${pageContext.request.contextPath}/images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">up</span>/<span style="color:blue;font-weight:bold">down</span> in
                                   </td>
                               </tr>
                           </thead>
                           <tbody>
                               <c:forEach var="e" items="${heatMapRows}" varStatus="i">
                                   <tr class="heatmap_row"

                                       onclick="FilterExps(this,'${u:escapeJS(e.efv)}','${u:escapeJS(e.ef)}')"
                                       title="${atlasGene.geneName}">
                                       <td nowrap="true" style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD; min-width: 100px;border-left:1px solid #CDCDCD;">
                                           <span style="font-weight: bold">
                                                   ${u:truncate(u:upcaseFirst(e.efv), 30)}
                                           </span>
                                       </td>

                                       <td nowrap="true" style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD;min-width: 80px;">
                                           ${f:escapeXml(atlasProperties.curatedEfs[e.ef])}
                                       </td>

                                       <c:set var="ud" value="${e.payload}"/>
                                       <c:choose>
                                           <c:when test="${empty ud || ud.ups + ud.downs + ud.nones == 0}">
                                               <td class="counter"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                                           </c:when>
                                           <c:when test="${ud.ups == 0 && ud.downs == 0 && ud.nones > 0}">
                                               <td class="acounter" style="color:black;"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is non-differentially in ${ud.nones} experiment(s)."
                                                   ><div class="osq">${ud.nones}</div></td>
                                           </c:when>
                                           <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones == 0}">
                                               <td class="acounter upback"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s)."
                                                   ><div class="osq">${ud.ups}</div></td>
                                           </c:when>
                                           <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones == 0}">
                                               <td class="acounter downback"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s)."
                                                   ><div class="osq">${ud.downs}</div></td>
                                           </c:when>
                                           <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones > 0}">
                                               <td class="acounter"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and not differentially expressed in ${ud.nones} experiment(s)."
                                                   ><div class="sq"><div class="nuduo"></div><div class="nunoval">${ud.nones}</div><div class="nuupval">${ud.ups}</div></div></td>
                                           </c:when>
                                           <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones > 0}">
                                               <td class="acounter"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) underexpressed in ${ud.downs} and not differentially expressed in ${ud.nones} experiment(s)."
                                                   onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="ndduo"></div><div class="ndnoval">${ud.nones}</div><div class="nddnval">${ud.downs}</div></div></td>
                                           </c:when>
                                           <c:when test="${ud.ups > 0 && ud.downs > 0 && ud.nones == 0}">
                                               <td class="acounter"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                   ><div class="sq"><div class="udduo"></div><div class="uddnval">${ud.downs}</div><div class="udupval">${ud.ups}</div></div></td>
                                           </c:when>
                                           <c:otherwise>
                                               <td class="acounter"
                                                   title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                   ><div class="sq"><div class="tri"></div><div class="tdnval">${ud.downs}</div><div class="tupval">${ud.ups}</div><div class="tnoval">${ud.nones}</div></div></td>
                                           </c:otherwise>
                                       </c:choose>
                                   </tr>
                               </c:forEach>
                           </tbody>
                       </table>
-->

</td>
</tr>

</table>


<c:set var="timeFinish" value="${u:currentTime()}"/>
<div align="center">Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.</div>

</div>
<!-- /id="ae_pagecontainer" -->
</div>
<!-- /id="contents" -->

<u:htmlTemplate file="look/footer.html"/>
</body></html>
