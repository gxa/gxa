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
<%@include file="../includes/global-inc.jsp" %>

<c:set var="timeStart" value="${u:currentTime()}"/>
<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="differentiallyExpressedFactors" type="java.util.List<ae3.model.ExperimentalFactor>" scope="request"/>
<jsp:useBean id="atlasGene" type="ae3.model.AtlasGene" scope="request"/>
<jsp:useBean id="ef" class="java.lang.String" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>

<tmpl:stringTemplate name="genePageHead">
    <tmpl:param name="gene" value="${atlasGene}"/>
</tmpl:stringTemplate>

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas Summary"/>
<meta name="Keywords"
      content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

<jsp:include page="../includes/query-includes.jsp"/>
<!--[if IE]><script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css"
      media="print, projection, screen"/>
<style type="text/css">
    @media print {
        body, .contents, .header, .contentsarea, .head {
            position: relative;
        }
    }
    </style>
</head>

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
        var eacc = tokens[0];
        var gid = tokens[1];
        drawEFpagination(eacc, gid, plotted_ef, "");

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

        $('#' + plot_id).bind('mouseleave', function () {
            $("#tooltip").remove();
        });
        return plot;
    }
    return null;
}

function redrawPlotForFactor(eacc, gid, ef, mark, efv) {
    var plot_id = eacc + "_" + gid + "_plot";
    var el = $("#" + plot_id);
    if (el) {
        atlas.ajaxCall("plot", { gid: gid, eacc: eacc, ef: ef || el.attr("name"), efv: efv, plot: 'bar' }, function(o) {
            var plot = drawPlot(o, plot_id);
            if (mark && ef && efv) {
                markClicked(eacc, gid, ef, efv, plot, o);
            }
        });
    }
}

function drawEFpagination(eacc, gid, currentEF, plotType, efv) {
    var panelContent = [];

    $("#" + eacc + "_EFpagination *").each(function() {
        var ef = $(this).attr("id");
        var ef_txt = $(this).html();
        if (ef == currentEF) {
            panelContent.push("<span id='" + ef + "' class='current'>" + ef_txt + "</span>")
        }
        else {
            panelContent.push('<a id="' + ef + '" onclick="redrawPlotForFactor(\'' + eacc + '\',\'' + gid + '\',\'' + ef + '\',\'' + efv + '\',\'' + plotType + '\',false)">' + ef_txt + '</a>');
        }
    });

    $("#" + eacc + "_EFpagination").empty();
    $("#" + eacc + "_EFpagination").html(panelContent.join(""));
}


function markClicked(eacc, gid, ef, efv, plot, jsonObj) {

    var plot_id = eacc + '_' + gid + '_plot';
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


var ExperimentList = (function(geneId) {
    var PAGE_SIZE = 5;
    var currentParams = {};

    function load(elementId, params) {
        var el = $("#" + elementId);
        if (el) {
            el.empty();
            el.load("${pageContext.request.contextPath}/geneExpList", $.extend(params, {gid: geneId}));
        }
    }

    function loadFirstPage(params, el) {
        currentParams = params;
        load("experimentList", $.extend({}, params, {from:0, to: PAGE_SIZE, needPaging: true}));
        markRow(el);
    }

    function loadPage(pageId) {
        var from = pageId * PAGE_SIZE;
        var to = (pageId + 1) * PAGE_SIZE;
        load("experimentListPage", $.extend({}, currentParams, {from: from, to: to}));
    }

    function markRow(el) {
        if (el) {
            old = $(".heatmap_over");
            old.removeClass("heatmap_over");
            old.addClass("heatmap_row");
            el.className = "heatmap_over";
        } else {
            $(".heatmap_over").removeClass("heatmap_over");
        }
    }

    function redrawPagination(total, target) {
        $("#allStudiesLink").empty();
        $("#pagination").empty();

        if (target) {
            var lnk = $("<a>Show all studies</a>").bind("click", ExperimentList.loadAllExperiments);
            $("#allStudiesLink").append(lnk);
        }

        if (total > PAGE_SIZE) {
            $("#pagination").pagination(total, {
                num_edge_entries: 2,
                num_display_entries: 5,
                items_per_page: PAGE_SIZE,
                callback: function(pageId) {
                    loadPage(pageId);
                    return false;
                }
            });
        }
    }

    function redrawPlots(exps) {
        for (var i = 0; i < exps.length; ++i) {
            var eacc = jQuery.trim(exps[i].acc);
            redrawPlotForFactor(eacc, geneId, currentParams.ef, true, currentParams.efv);
        }
    }

    return {
        filterExperiments: function(params, el) {
            loadFirstPage(params, el);
        },

        loadAllExperiments: function() {
            loadFirstPage({ef:""});
        },

        drawPagination: function(totalNumerOfRows, target) {
            redrawPagination(totalNumerOfRows, target);
        },

        drawPlots: function(exps) {
            redrawPlots(exps);
        }
    }

})(${atlasGene.geneId});

function FilterExps(el, efv, ef) {
    ExperimentList.filterExperiments({ef: ef, efv: efv}, el);
}

function FilterExpsEfo(el, efo) {
    ExperimentList.filterExperiments({efo: efo}, el);
}

$(document).ready(function() {
    $("#heatmap_tbl").tablesorter({
        headers: {2: {sorter: false}}});

    ExperimentList.filterExperiments({ef: "${ef}"});
});
</script>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
<div class="ae_pagecontainer">

<jsp:include page="../includes/atlas-header.jsp"/>

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
                                        ${u:join(atlasGene.geneProperties[prop], ", ")}
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
                           href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=allebi&query=&quot;${atlasGene.geneIdentifier}&quot;&requestFrom=ebi_index&submit=+FIND+">
                            ${atlasGene.geneIdentifier}
                        </a>
                    </td>
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

                <map name="anatomogram" class="anatomogram">
                    <c:forEach var="area" items="${anatomogramMap}">
                        <area shape="poly" onclick="FilterExpsEfo(null, '${area.efo}', 'organism_part');return false;" coords="${u:join(area.coordinates, ",")}" href="#"/>
                    </c:forEach>
                </map>

                <img src="${pageContext.request.contextPath}/webanatomogram/${atlasGene.geneIdentifier}.png"
                         alt="anatomogram" border="none" usemap="#anatomogram"/>

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
                <%--
                generic ef - the above clause imposes the following rules:
                1. in multi-experimental factor experiment view:
                a. ef != 'organism_part' => always show a table
                b. ef == 'organism_part'
                - anatomogram exists => show anatomogram
                - anatomogram doesn't exist => show table
                2. in single-experimental factor experiment view: (ef != null):
                a. as 1a
                b ef == 'organism_part' => show anatomogram (if exists) AND the table
                --%>

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
                    <c:choose>
                        <c:when test="${not empty ef}">
                            <c:set var="values" value="${experimentalFactor.values}"/>
                        </c:when>
                        <c:otherwise>
                            <c:set var="values" value="${experimentalFactor.topValues}"/>
                        </c:otherwise>
                    </c:choose>
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
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) underexpressed in ${ud.downs} and not differentially expressed in ${ud.nones} experiment(s).">
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
<td valign="top" align="left" width="50%" id="experimentList">
</td>
</tr>
</table>
</td>
</tr>

</table>


<c:set var="timeFinish" value="${u:currentTime()}"/>
<div align="center">Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.</div>

</div>
<!-- ae_pagecontainer -->
</div>
<!-- /id="contents" -->

</tmpl:stringTemplateWrap>
</html>
