<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
<jsp:useBean id="query" class="ae3.service.structuredquery.AtlasStructuredQuery" scope="request"/>
<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="result" type="ae3.service.structuredquery.AtlasStructuredQueryResult" scope="request"/>
<jsp:useBean id="forcestruct" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="heatmap" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="list" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="timeStart" type="java.lang.Long" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>

    <tmpl:stringTemplate name="queryResultPageHead" />

    <jsp:include page="/WEB-INF/jsp/includes/query-includes.jsp" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas-searchform.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css" />

<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.mod.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.collapsible.js"></script>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/pure2.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/structured-query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/atlas-searchform.js"></script>

<style type="text/css">
    @media print {
        body, .contents, .header, .contentsarea, .head {
            position: relative;
        }
    }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
<div class="ae_pagecontainer">

<jsp:include page="/WEB-INF/jsp/includes/atlas-header.jsp"/>

<c:set var="simpleformvisible" value="${query.none ? !forcestruct : query.simple}" />

<div id="topcontainer">
    <jsp:include page="/WEB-INF/jsp/includes/atlas-searchform.jsp">
        <jsp:param name="isInAdvancedState" value="${!simpleformvisible}"/>
    </jsp:include>
</div>

<script type="text/javascript">
    $(document).ready(function () {
        $(".tablesorter").collapsible("td.collapsible", {
            collapse: true,
            callback: atlas.showListThumbs
        }).tablesorter({
            // don't sort by first column
            headers: {0: {sorter: false}},
            // set the widgets being used - zebra stripping
            widgets: ['zebra'],
            onRenderHeader: function (){
                this.wrapInner("<span></span>");
            },
            debug: false
        });

    });
    var preloads = [ 'expp.gif', 'expm.gif', 'indicator.gif' ]; var img = [];
    for(var i = 0; i < preloads.length; ++i) {
        img[i] = new Image(); img[i].src = 'images/' + preloads[i];
    }
</script>

<c:if test="${!query.none}">
<c:set var="cn" value="${f:length(query.conditions)}"/>
<c:set var="sn" value="${f:length(query.species)}"/>
<c:set var="gn" value="${f:length(query.geneConditions)}"/>

<script type="text/javascript">

    $("#loading_display").hide();
    resultGenes = [
            <c:forEach var="row" varStatus="s" items="${result.results}">${u:escapeJS(row.gene.geneId)}<c:if test="${!s.last}">,</c:if></c:forEach>
    ];

    resultEfvs = [
            <c:forEach var="e" varStatus="s" items="${result.resultEfvs.valueSortedList}">{ ef: '${u:escapeJS(e.ef)}', efv: '${u:escapeJS(e.efv)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
    ];

    atlas.resultConditions = ${u:toJson(result.conditions)};
</script>

<c:if test="${result.size > 0}">
<c:url var="pageUrl" value="/qrs">
    <c:forEach var="g" varStatus="gs" items="${query.geneConditions}">
        <c:param name="gnot_${gs.index}" value="${g.negated ? '1' : ''}" />
        <c:param name="gval_${gs.index}" value="${g.jointFactorValues}" />
        <c:param name="gprop_${gs.index}" value="${g.factor}" />
    </c:forEach>
    <c:forEach var="i" varStatus="s" items="${query.species}"><c:param name="specie_${s.index}" value="${i}"/></c:forEach>
    <c:forEach varStatus="cs" var="c" items="${query.conditions}">
        <c:param name="fact_${cs.index}" value="${c.factor}"/>
        <c:param name="fexp_${cs.index}" value="${c.expression}"/>
        <c:param name="fmex_${cs.index}" value="${c.minExperiments}"/>
        <c:param name="fval_${cs.index}" value="${c.jointFactorValues}"/>
    </c:forEach>
    <c:if test="${heatmap}"><c:param name="view" value="hm"/></c:if>
    <c:if test="${list}"><c:param name="view" value="list"/></c:if>
</c:url>

<script type="text/javascript">
    $(document).ready(function () {
    <c:if test="${!empty result && result.size < result.total}">
        var opts = {
            current_page: ${result.page},
            num_edge_entries: 1,
            items_per_page: ${result.rowsPerPage},
            link_to: '${pageUrl}&p=__id__',
            next_text: '»',
            prev_text: '«',
            callback: function(page) { return true; }
        };
        opts.num_display_entries = 2;
        $(".page_short").pagination(${result.total}, opts);
        opts.num_display_entries = 5;
        $(".page_long").pagination(${result.total}, opts);
    </c:if>

        $(".export_lnk").bind("click",function() {
            $.ajax({
                url: '${pageUrl}&export=true',
                cache: false,
                dataType: "json",
                success: function(qry, status) {
                    if(qry.qid > 0) {
                        var count = parseInt($("#dwnldCounter").text())+1;
                        $("#dwnldCounter").text(count);
                        $("#dwnldCounter").parent().show();
                    }
			        atlas.popup('downloads');
                }
            });

            alert("Download request sent. Please check the downloads window for status.");
            return false;
        });
    });

</script>

<div id = "result_cont">
<table id="twocol" style="margin-top:20px">
<tr class="top">
<c:if test="${result.total >= atlasProperties.queryDrilldownMinGenes}">
    <td class="atlastable" id="drilldowns">
        <div style="font-size:11px">
            <b>REFINE YOUR QUERY</b>
        </div>
        <div id="drill" style="padding:0px;">
            <c:forEach var="ef" items="${result.efvFacet.valueSortedTrimmedTree}">
                <div class="drillsect">
                    <c:set var="efname">${f:escapeXml(atlasProperties.curatedEfs[ef.ef])}</c:set>
                    <div class="name">${efname}</div>
                    <ul><c:forEach var="efv" items="${ef.efvs}" varStatus="s">
                        <li><nobr><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=UP_DOWN&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="ftot" title="Filter for genes up or down in ${efname}: ${f:escapeXml(efv.efv)}"><c:out value="${u:truncate(efv.efv, 30)}"/></a>
                            (<c:if test="${efv.payload.up > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=UP&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="fup" title="Filter for genes up in ${efname}: ${f:escapeXml(efv.efv)}"><c:out value="${efv.payload.up}"/>&#8593;</a></c:if><c:if test="${efv.payload.up > 0 && efv.payload.down > 0}">&nbsp;</c:if><c:if test="${efv.payload.down > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=DOWN&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="fdn" title="Filter for genes down in ${efname}: ${f:escapeXml(efv.efv)}"><c:out value="${efv.payload.down}"/>&#8595;</a></c:if>)
                        </nobr></li>
                    </c:forEach></ul>
                </div>

            </c:forEach>
            <c:if test="${!empty result.geneFacets['species']}">
                <div class="drillsect">
                    <div class="name">Organism</div>
                    <ul>
                        <c:forEach var="sp" items="${result.geneFacets['species']}" varStatus="s">
                            <li><nobr><a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(sp.name)}" class="ftot" title="Filter by organism ${f:escapeXml(sp.name)}"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</nobr></li>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>
            <c:forEach var="facet" items="${result.geneFacets}">
                <c:if test="${!empty facet.key && facet.key!='species'}">
                    <div class="drillsect">
                        <c:set var="gpname">${f:escapeXml(atlasProperties.curatedEfs[facet.key])}</c:set>
                        <div class="name">${gpname}</div>
                        <ul>
                            <c:forEach var="fv" items="${facet.value}" varStatus="s">
                                <li><nobr><a href="${pageUrl}&amp;gval_${gn}=${u:escapeURL(u:optionalQuote(fv.name))}&amp;gprop_${gn}=${u:escapeURL(facet.key)}" title="Filter for genes with ${gpname}: ${f:escapeXml(fv.name)}" class="ftot"><c:out value="${u:truncate(fv.name, 30)}"/></a>&nbsp;(<c:out value="${fv.count}"/>)</nobr></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </td>
</c:if>


<td class="atlastable" id="resultpane" width="900px">
<div id="summary">
    <span id="pagetop" class="pagination_ie page_long"></span>
    Genes <c:out value="${result.page * result.rowsPerPage == 0 ? 1 : result.page * result.rowsPerPage}"/>-<c:out value="${(result.page + 1) * result.rowsPerPage > result.total ? result.total : (result.page + 1) * result.rowsPerPage }"/> of <b><c:out value="${result.total}" /></b> total found
    <c:if test="${result.total >= atlasProperties.queryDrilldownMinGenes}">
        <span>(you can <a href="#" onclick="$('#drilldowns').animate({width:'show'});$(this).parent().remove();return false;">refine your query</a>)</span>
    </c:if>
    &nbsp;•&nbsp;
    <a class="export_lnk" title="Download results in a tab-delimited format." href="#" >Download all results</a>
    <span style="display:${noDownloads > 0 ? 'inline' : 'none' };">- <span id="dwnldCounter">${noDownloads}</span> download(s) <a href="javascript:void(0)" onclick="atlas.popup('downloads')">in progress</a></span>
    &nbsp;•&nbsp; <c:import url="../includes/apilinks.jsp"><c:param name="apiUrl" value="${query.apiUrl}"/></c:import>
</div>
<div id="legendexpand" style="width:100%;height:30px">
    
    <div style="line-height:30px;white-space:nowrap">Legend: <img style="position:relative;top:6px" src="${pageContext.request.contextPath}/images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">over</span>/<span style="color:blue;font-weight:bold">under</span> expressed in
           (&#126; in <c:if test="${list}"> <img alt="plus" src="${pageContext.request.contextPath}/images/expp.gif" /> </c:if>
            experiment pop-ups indicates non-differential expression)
    </div>

</div>

<c:choose>
<c:when test="${heatmap}">
    <c:set var="efoSubTree" value="${result.resultEfos.markedSubTreeList}" />
    <c:set var="efoSubTreeLength" value="${f:length(efoSubTree)}" />

    <c:if test="${efoSubTreeLength > 0}">
        <map id="efomap" name="efomap">
            <c:set var="efohgt" value="${150 - 9 - 4 * u:max(efoSubTree, 'getDepth')}"/>
            <c:forEach var="i" items="${efoSubTree}" varStatus="s">
                <area alt="${f:escapeXml(i.term)}" title="${f:escapeXml(i.term)}${empty i.alternativeTerms ? '' : ' ['}${empty i.alternativeTerms ? '' : u:join(i.alternativeTerms, ', ')}${empty i.alternativeTerms ? '' : ']'}" shape="poly" coords="${s.index*27},${efohgt - 20},${s.index*27+efohgt-20},0,${s.index*27+efohgt+17},0,${s.index*27+17},${efohgt-1},${s.index*27},${efohgt-1},${s.index*27},${efohgt - 20}" onclick="return false;">
                <c:choose>
                    <c:when test="${i.expandable}">
                        <area style="cursor:pointer;" alt="" title="Narrow down your search with EFO" shape="poly"
                              coords="${s.index*27},150,${s.index*27},${efohgt},${s.index*27 + 26},${efohgt},${s.index*27 + 26},150,${s.index*27},150"
                              onclick="atlas.expandEfo(${s.index*27},${efohgt},'${u:escapeJS(i.id)}','childrenOf');return false;"
                              href="#">
                    </c:when>
                    <c:otherwise>
                        <c:if test="${i.depth == 0 && !i.root}">
                            <area style="cursor:pointer;" alt="" title="Broaden your search with EFO" shape="poly"
                                  coords="${s.index*27},150,${s.index*27},${efohgt},${s.index*27 + 26},${efohgt},${s.index*27 + 26},150,${s.index*27},150"
                                  onclick="atlas.expandEfo(${s.index*27},${efohgt},'${u:escapeJS(i.id)}','parentsOf');return false;"
                                  href="#">
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </map>
    </c:if>
    <c:if test="${result.resultEfvs.numEfvs > 0}">
        <map id="efvmap" name="efvmap">
            <c:forEach var="i" items="${result.resultEfvs.valueSortedList}" varStatus="s">
                <c:set var="efvhgt" value="150"/>
                <area alt="${f:escapeXml(i.efv)}" title="${f:escapeXml(i.efv)}" shape="poly" coords="${s.index*27},${efvhgt-20},${s.index*27+efvhgt-20},0,${s.index*27+efvhgt-1+17},0,${s.index*27+17},${efvhgt-1},${s.index*27},${efvhgt-1},${s.index*27},${efvhgt-20}" onclick="return false;">
            </c:forEach>
        </map>
    </c:if>

    <table id="squery">
        <tbody>
        <tr class="header">
            <th class="padded" rowspan="2">Gene</th>
            <c:if test="${f:length(query.species) != 1}">
                <th class="padded" rowspan="2">Organism</th>
            </c:if>
            <c:if test="${efoSubTreeLength > 0}">
                <c:set scope="session" var="resultEfos" value="${efoSubTree}" />
                <c:url var="efoImgUrl" value="/thead">
                    <c:param name="random" value="${u:currentTime()}" />
                    <c:param name="st" value="resultEfos" />
                    <c:param name="mt" value="getTerm" />
                    <c:param name="md" value="getDepth" />
                    <c:param name="ie" value="isExpandable" />
                    <c:param name="s" value="27" />
                    <c:param name="fs" value="11" />
                    <c:param name="h" value="150" />
                    <c:param name="lh" value="15" />
                    <c:param name="lc" value="cdcdcd" />
                    <c:param name="tc" value="000000" />
                    <c:param name="tlc" value="000000" />
                    <c:param name="tds" value="4" />
                    <c:param name="tsx" value="7" />
                    <c:param name="tsy" value="5" />
                </c:url>
                <td colspan="${efoSubTreeLength}" class="${result.resultEfvs.numEfvs > 0 ? 'divider' : 'nope'}"><div style="width:${efoSubTreeLength * 27 - 1}px;" class="diaghead">Ontology</div><div style="position:relative;height:150px;">
                    <div id="efoheader" style="position:absolute;bottom:0;left:-1px;"><img onload="fixpng(this);" src="${efoImgUrl}" usemap="#efomap" alt=""></div>
                    <c:forEach var="i" items="${efoSubTree}" varStatus="s">
                        <c:if test="${!i.expandable && i.depth == 0 && !i.root}">
                            <img style="position:absolute;left:${s.index*27}px;bottom:0;cursor:pointer;" alt="" title="Broaden your search with EFO" onclick="atlas.expandEfo(${s.index*27},${efohgt},'${u:escapeJS(i.id)}');return false;" src="${pageContext.request.contextPath}/images/goup.gif" width="5" height="12">
                        </c:if>
                    </c:forEach>
                </div></td>
            </c:if>
            <c:if test="${result.resultEfvs.numEfvs > 0}">
                <c:set scope="session" var="resultEfvs" value="${result.resultEfvs.valueSortedList}" />
                <c:url var="efoImgUrl" value="/thead">
                    <c:param name="random" value="${u:currentTime()}" />
                    <c:param name="st" value="resultEfvs" />
                    <c:param name="mt" value="getEfv" />
                    <c:param name="s" value="27" />
                    <c:param name="fs" value="11" />
                    <c:param name="h" value="150" />
                    <c:param name="lh" value="15" />
                    <c:param name="lc" value="cdcdcd" />
                    <c:param name="tc" value="000000" />
                </c:url>
                <td class="atlastable" colspan="${result.resultEfvs.numEfvs}"><div style="width:${result.resultEfvs.numEfvs * 27 - 1}px;" class="diaghead">Keywords</div><div style="position:relative;height:150px;"><div style="position:absolute;bottom:0;left:-1px;"><img onload="fixpng(this);" src="${efoImgUrl}" usemap="#efvmap" alt=""></div></div></td>
            </c:if>
        </tr>
        <tr>
            <c:if test="${efoSubTreeLength > 0}">
                <th colspan="${efoSubTreeLength}" class="${result.resultEfvs.numEfvs > 0 ? 'divider' : 'nope'}">&nbsp;</th>
            </c:if>
            <c:forEach var="c" items="${result.resultEfvs.efValueSortedTree}" varStatus="i">
                <c:set var="eftitle">${f:escapeXml(atlasProperties.curatedEfs[c.ef])}</c:set>
                <th colspan="${f:length(c.efvs)}" class="factor" title="${eftitle}">
                    <div style="width:${f:length(c.efvs) * 27 - 1}px;">${eftitle}</div>
                    <c:choose>
                        <c:when test="${u:isIn(query.expandColumns, c.ef)}">
                            <a title="Collapse factor values for ${eftitle}" href="${pageUrl}&amp;p=${result.page}">&#0171;<c:if test="${f:length(c.efvs) > 1}">&nbsp;fewer</c:if></a>
                        </c:when>
                        <c:when test="${u:isIn(result.expandableEfs, c.ef)}">
                            <a title="Show more factor values for ${eftitle}..." href="${pageUrl}&amp;p=${result.page}&amp;fexp=${c.ef}"><c:if test="${f:length(c.efvs) > 1}">more&nbsp;</c:if>&#0187;</a>
                        </c:when>
                    </c:choose>
                </th>
            </c:forEach>
        </tr>
        <c:forEach var="row" items="${result.results}" varStatus="i">
            <tr id="squeryrow${i.index}">
                <td class="padded genename">
                    <a href="gene/${f:escapeXml(row.gene.geneIdentifier)}">${row.gene.hilitGeneName}<c:if test="${empty row.gene.geneName}"><c:out value="${row.gene.geneIdentifier}"/></c:if></a>
                    <div class="gtooltip">
                        <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:forEach items="${atlasProperties.geneAutocompleteNameFields}" var="prop"><c:if test="${!empty row.gene.geneProperties[prop]}">${row.gene.hilitGeneProperties[prop]}, </c:if></c:forEach>${row.gene.geneIdentifier})</div>
                        <c:forEach items="${atlasProperties.geneTooltipFields}" var="prop">
                            <c:if test="${!empty row.gene.geneProperties[prop]}"><div><b>${f:escapeXml(atlasProperties.curatedGeneProperties[prop])}:</b> ${row.gene.hilitGeneProperties[prop]}</div></c:if>
                        </c:forEach>
                    </div>
                </td>
                <c:if test="${f:length(query.species) != 1}">
                    <td class="padded"><c:out value="${row.gene.geneSpecies}"/></td>
                </c:if>
                <c:forEach var="e" items="${efoSubTree}" varStatus="j">
                    <c:set var="ud" value="${row.counters[e.payload.position]}"/>
                    <c:choose>
                        <c:when test="${empty ud || ud.ups + ud.downs + ud.nones == 0}">
                            <td class="counter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''}"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs == 0 && ud.nones > 0}">
                            <td class="acounter" style="color:black;"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} is non-differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="osq">${ud.nones}</div></td>
                        </c:when>
                        <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones == 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''} upback"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} is overexpressed in ${ud.ups} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="osq">${ud.ups}</div></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones == 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''} downback"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} is underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="osq">${ud.downs}</div></td>
                        </c:when>
                        <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones > 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} overexpressed in ${ud.ups} and non-differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="sq"><div class="nuduo"></div><div class="nunoval">${ud.nones}</div><div class="nuupval">${ud.ups}</div></div></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones > 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} underexpressed in ${ud.downs} and non-differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="sq"><div class="ndduo"></div><div class="ndnoval">${ud.nones}</div><div class="nddnval">${ud.downs}</div></div></td>
                        </c:when>                        
                        <c:when test="${ud.ups > 0 && ud.downs > 0 && ud.nones == 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="sq"><div class="udduo"></div><div class="uddnval">${ud.downs}</div><div class="udupval">${ud.ups}</div></div></td>
                        </c:when>
                        <c:otherwise>
                            <td class="acounter${j.last && result.resultEfvs.numEfs > 0 ? ' divider' : ''}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} overexpressed in ${ud.ups}, underexpressed in ${ud.downs} and non-differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="sq"><div class="tri"></div><div class="tdnval">${ud.downs}</div><div class="tupval">${ud.ups}</div><div class="tnoval">${ud.nones}</div></div></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
                <c:forEach var="e" items="${result.resultEfvs.valueSortedList}" varStatus="j">
                    <c:set var="ud" value="${row.counters[e.payload.position]}"/>
                    <c:choose>
                        <c:when test="${empty ud || ud.ups + ud.downs + ud.nones == 0}">
                            <td class="counter"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs == 0 && ud.nones > 0}">
                            <td class="acounter" style="color:black;"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is non-differentially in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.nones}</div></td>
                        </c:when>
                        <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones == 0}">
                            <td class="acounter upback"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.ups}</div></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones == 0}">
                            <td class="acounter downback"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.downs}</div></td>
                        </c:when>
                        <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones > 0}">
                            <td class="acounter"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and not differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="nuduo"></div><div class="nunoval">${ud.nones}</div><div class="nuupval">${ud.ups}</div></div></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones > 0}">
                            <td class="acounter"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) underexpressed in ${ud.downs} and not differentially expressed in ${ud.nones} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="ndduo"></div><div class="ndnoval">${ud.nones}</div><div class="nddnval">${ud.downs}</div></div></td>
                        </c:when>
                        <c:when test="${ud.ups > 0 && ud.downs > 0 && ud.nones == 0}">
                            <td class="acounter"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="udduo"></div><div class="uddnval">${ud.downs}</div><div class="udupval">${ud.ups}</div></div></td>
                        </c:when>
                        <c:otherwise>
                            <td class="acounter"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="tri"></div><div class="tdnval">${ud.downs}</div><div class="tupval">${ud.ups}</div><div class="tnoval">${ud.nones}</div></div></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</c:when>
<c:otherwise>

    <table id="squery" class="tablesorter">
        <thead>
            <tr class="header">
                <th style="border-right:none;width:20px;"></th>
                <th style="border-left:none;width:110px;" class="padded">Gene</th>
                <th class="padded">Organism</th>
                <th class="padded">Experimental Factor</th>
                <th class="padded">Factor Value</th>
                <th></th>
                <th class="padded" style="width:90px;">P-value</th>
            </tr>
        </thead>

        <tbody>

        <c:forEach var="row" items="${result.listResults}" varStatus="r">
            <tr id="${row.gene_id}_${row.ef}_${r.index}">
                <td class="collapsible" title="Click here to show experiments..."></td>
                <td class="padded genename" style="border-left:none">
                    <a href="gene/${f:escapeXml(row.gene.geneIdentifier)}">${row.gene_name}</a>
                    <div class="gtooltip">
                        <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:forEach items="${atlasProperties.geneAutocompleteNameFields}" var="prop"><c:if test="${!empty row.gene.geneProperties[prop]}">${row.gene.hilitGeneProperties[prop]}, </c:if></c:forEach>${row.gene.geneIdentifier})</div>
                        <c:forEach items="${atlasProperties.geneTooltipFields}" var="prop">
                            <c:if test="${!empty row.gene.geneProperties[prop]}"><div><b>${f:escapeXml(atlasProperties.curatedGeneProperties[prop])}:</b> ${row.gene.hilitGeneProperties[prop]}</div></c:if>
                        </c:forEach>
                    </div>
                </td>
                <td class="padded">${row.gene_species}</td>
                <td class="padded">${f:escapeXml(atlasProperties.curatedEfs[row.ef])}</td>
                <td class="padded lvrowefv">${row.fv}</td>
                <c:choose>
                    <c:when test="${row.ups == 0 && row.downs == 0 && row.nones > 0}">
                        <td class="acounter" style="color:black;"><div class="osq">${row.nones}</div></td>
                    </c:when>
                    <c:when test="${row.ups > 0 && row.downs == 0 && row.nones == 0}">
                        <td class="acounter upback"><div class="osq">${row.ups}</div></td>
                    </c:when>
                    <c:when test="${row.ups == 0 && row.downs > 0 && row.nones == 0}">
                        <td class="acounter downback"><div class="osq">${row.downs}</div></td>
                    </c:when>
                    <c:when test="${row.ups > 0 && row.downs == 0 && row.nones > 0}">
                        <td class="acounter"><div class="sq"><div class="nuduo"></div><div class="nunoval">${row.nones}</div><div class="nuupval">${row.ups}</div></div></td>
                    </c:when>
                    <c:when test="${row.ups == 0 && row.downs > 0 && row.nones > 0}">
                        <td class="acounter"><div class="sq"><div class="ndduo"></div><div class="ndnoval">${row.nones}</div><div class="nddnval">${row.downs}</div></div></td>
                    </c:when>
                    <c:when test="${row.ups > 0 && row.downs > 0 && row.nones == 0}">
                        <td class="acounter"><div class="sq"><div class="udduo"></div><div class="uddnval">${row.downs}</div><div class="udupval">${row.ups}</div></div></td>
                    </c:when>
                    <c:otherwise>
                        <td class="acounter"><div class="sq"><div class="tri"></div><div class="tdnval">${row.downs}</div><div class="tupval">${row.ups}</div><div class="tnoval">${row.nones}</div></div></td>
                    </c:otherwise>
                </c:choose>
                <td class="padded">${u:prettyFloatFormat(row.minPval)}</td>
            </tr>
            <tr class="expand-child">
                <td class="empty"></td>
                <td colspan="6">
                    <div style="width:100%">
                        <table style="width:100%;border-collapse:collapse;" border="0">
                            <c:forEach var="exp" items="${row.exp_list}">
                                <tr>
                                    <td class="padded genename" style="width:110px;">
                                        <a target="_blank"
                                           href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=${exp.experimentAccession}">${exp.experimentAccession}</a>
                                    </td>
                                    <td class="expdesc wrapok" colspan="3">
                                            ${exp.experimentDescription}
                                    </td>
                                    <td class="expthumb">
                                        <div class="outer">
                                            <a title="Show expression profile"
                                               href="experiment/${exp.experimentAccession}/${row.gene.geneIdentifier}/${row.ef}">
                                                <div id="${exp.experimentId}_${exp.experimentAccession}_${exp.updn}"
                                                     class="thumb thumb${r.index}">
                                                    <img alt="Waiting..."
                                                         src="${pageContext.request.contextPath}/images/indicator.gif"
                                                         style="position:relative;top:10px"/>
                                                </div>
                                            </a>
                                        </div>
                                    </td>
                                    <c:choose>
                                        <c:when test="${exp.updn == 'UP'}">
                                            <td style="color:red;width:90px;" class="pvalue padded">
                                                &#8593;&nbsp;${u:prettyFloatFormat(exp.pvalue)}
                                            </td>
                                        </c:when>
                                        <c:when test="${exp.updn == 'DOWN'}">
                                            <td style="color:blue;width:90px;" class="pvalue padded">
                                                &#8595;&nbsp;${u:prettyFloatFormat(exp.pvalue)}
                                            </td>
                                        </c:when>
                                        <c:otherwise>
                                            <td style="color:black;width:90px;" class="pvalue padded">
                                                ~&nbsp;
                                            </td>
                                        </c:otherwise>
                                    </c:choose>
                                </tr>
                            </c:forEach>
                        </table>
                    </div>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:otherwise>
</c:choose>

<div class="pagination_ie page_long"></div>

<c:set var="timeFinish" value="${u:currentTime()}"/>
<p>
    Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.
</p>

</td><!-- /id="resultpane" -->

</tr>
</table><!-- /id="twocol" -->
</div><!-- /id="result_cont" -->

</c:if><!-- /results.size > 0 -->
<c:if test="${result.size == 0}">
    <div style="margin-top:30px;margin-bottom:20px;font-weight:bold;">No matching results found.</div>
</c:if>

</c:if><!-- /!query.none -->

<div id="question" style="display:none; cursor: default">
    <input  type="button" id="no" value="Get file" />
</div>

</div><!-- ae_pagecontainer -->
</div><!-- /id="contents" -->

<div id="experimentsTemplate">
    <div class="head">
        <a href="gene/1"><b class="gname">Name</b></a> in <b class="efv">efv</b> (<span class="ef">ef</span>)<br>
        overexpressed in <b class="numup">1</b>, underexpressed in <b class="numdn">1</b>
        and non-differentially expressed in <b class="numno">1</b>
        experiment(s)
    </div>
    <div class="exptable">
        <table>
            <tr class="experRows">
                <td class="explot">
                    <b class="expaccession">E-ACC</b>:
                    <span class="expname"></span>

                    <table class="oneplot" border="0" cellpadding="0" cellspacing="0"><tr>
                        <td class="atlastable"><img src="${pageContext.request.contextPath}/images/indicator.gif" class="plotwaiter" border="0" alt="Loading...">
                            <a class="proflink" title="Show expression profile" href="" style="border:none;outline:none;text-decoration:none">
                                <div style="display:none" class="plot"></div></a>
                        </td>
                        <td class="atlastable"><div style="display:none" class="efname"></div><div class="legend"></div></td>
                    </tr>
                    <tr>
                        <td align="left" colspan="2">
                            <div align="left" id="" class="arraydesign"></div>
                        </td>
                    </tr>
                    </table>

                    <div style="margin-top:5px;font-size:10px;">
                        Show <a class="proflink2" title="Show expression profile" href="">expression profile</a>
                        &nbsp;/&nbsp;
                        <a class="detailink" target="_blank" title="Show experiment details in ArrayExpress Archive" href="">experiment details</a>
                    </div>
                </td>
            </tr>
        </table>
    </div>
</div>

</tmpl:stringTemplateWrap>
</html>
