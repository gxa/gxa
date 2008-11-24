<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.service.structuredquery.AtlasStructuredQuery" %>
<%@ page import="ae3.service.structuredquery.AtlasStructuredQueryResult" %>
<%@ page import="ae3.service.structuredquery.AtlasStructuredQueryParser" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    request.setAttribute("service", ArrayExpressSearchService.instance());

    AtlasStructuredQuery atlasQuery = null;
    if(request.getParameterNames().hasMoreElements())
         atlasQuery = AtlasStructuredQueryParser.parseRequest(request);
    request.setAttribute("query", atlasQuery);

    request.setAttribute("heatmap", "hm".equals(request.getParameter("view")));

%>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Preview
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.cookie.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="jquery.autocomplete.js"></script>
    <script type="text/javascript" src="jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="structured-query.js"></script>
    <script type="text/javascript" src="raphael-packed.js"></script>

    <script type="text/javascript">
        $(document).ready(function()
            {
                $("#gene").defaultvalue("(all genes)");
                $("#gene").autocomplete("autocomplete.jsp", {
                        minChars:1,
                        matchCase: true,
                        matchSubset: false,
                        multiple: false,
                        selectFirst: false,
                        extraParams: {type:"gene"},
                        formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
                });
            }
        );

        var options = {
            expressions : [
                    <c:forEach var="i" items="${service.geneExpressionOptions}">
                    [ '${u:escapeJS(i[0])}', '${u:escapeJS(i[1])}' ],
                    </c:forEach>
            ],
            factors : [
                <c:forEach var="i" items="${service.experimentalFactorOptions}">
                '${u:escapeJS(i)}',
                </c:forEach>
            ],
            species : [
                <c:forEach var="i" items="${service.allAvailableAtlasSpecies}">
                '${u:escapeJS(i)}',
                </c:forEach>
            ]
        };
    </script>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>


<style type="text/css">
    .label {
        font-size: 10px;
    }

    .atlasHelp {
        display: none;
    }

    /* tables */
    .squery, .factab {
        font-family: arial, sans-serif;
        background-color: #CDCDCD;
        font-size: 8pt;
        text-align: left;
    }
    .squery {
        margin:0px 0pt 15px;
    }
    .factab {
        margin-bottom:3px;
    }
    .squery th, .factab th {
        background-color: #e6EEEE;
        border: 1px solid #FFF;
        font-size: 8pt;
        padding: 4px;
    }
    .squery td, .factab td {
        color: #3D3D3D;
        padding: 4px;
        background-color: #FFF;
        vertical-align: top;
    }

    .prefix { padding-right:5px;}
    .speciesSelect { width: 120px; margin-right:5px; }

    .expansion { padding-left: 10px;font-size: smaller;color:gray; }
    .expansion .ignored { color: #cc0000; }
    div.value select, div.value input.value { width:150px; }
    div.value div.buttons { float: right; }
    div.value div.input { float: left; }
    div.value { width: 250px; margin-bottom:2px; }
    #conditions td { vertical-align: top; }
    #conditions td.factorvalue { padding-bottom: 10px; padding-left: 5px; }
    #conditions td.andbuttons {  padding-bottom:10px; padding-left: 10px}
    div.countup, div.countdn { width:50%;float:left;text-align:center; }
    div.expref { clear:both;text-align:left;width:100%;background-color:#f0f0f0; }
    td.counter,td.acounter, th.counter {text-align:center;width:25px;min-width:25px;vertical-align:middle; }
    td.acounter:hover { cursor: pointer; }
    td.common { white-space:nowrap; }
    th.factor { text-align:center;font-weight:normal; }
    th.factor em { font-weight:bold;font-style:normal;}
    th.gene { vertical-align:middle;text-align:center; }

    a.fup:link, a.fup:active, a.fup:hover, a.fup:visited { color: #660000;text-decoration: underline; }
    a.fdn:link, a.fdn:active, a.fdn:hover, a.fdn:visited { color: #000066;text-decoration: underline; }
    a.ftot:link, aftot:active, a.ftot:hover, a.ftot:visited { color: #666666;text-decoration: underline; }

    .pagination { margin-top: 20px; text-align: left; }

    .genelist { width: 90%; }
    .genelist .item { margin-top: 10px;margin-bottom: 20px; }
    .genename { font-size: 20px }
    .ensname { font-size: smaller; }
    .label { color: #666666;font-size: smaller; }
    .genelist .item .efvs { margin-left: 30px; background:#f7f7f7;
/*        border-bottom: 1px solid #cccccc;
        border-left: 1px solid #cccccc;
        border-right: 1px solid #cccccc;*/
        padding: 10px; }

    .genelist .itemhead { padding: 5px; background: #f0f0f0;border: 2px solid #cccccc; }

    .summary { margin-top: 10px; margin-bottom: 10px; }
    .drilldowns { margin-bottom: 10px; margin-top: 10px; padding: 10px; }
    .drillsect { float:left;padding: 5px; border-left:1px solid #cccccc; }
    .drillsect ul { padding-left: 15px; }
    .drillsect .name { font-weight: bold; }

    .expopup {
        position: absolute;
        width: 500px;
        height: 300px;
        overflow: auto;
        margin-top: -5px;
        margin-left: -5px;
        background-color: #ffffe0;
        border: 1px solid black;
        padding: 10px;
        color: black;
        text-align:left;
    }
    .waiter {
        position: absolute;
        overflow: auto;
        margin-top: -5px;
        margin-left: -5px;
        border: 1px solid black;
        padding: 3px;
        background-color: white;
    }

    .expopup h1 {
        font-weight:bold;
        font-size:larger;
    }
    .expopup td {
        vertical-align:top;
        text-align: left;
    }
    .expopup th {
        vertical-align:top;
        text-align: left;
        font-weight: bold;
    }
    .expopup th.expup {
        background-color: #660000;
        color: #ff6666;
        font-weight: bold;
    }
    .expopup th.expdn {
        background-color: #000066;
        color: #6666ff;
        font-weight: bold;
    }
    .expopup td.expup {
        color: #990000;
    }
    .expopup td.expdn {
        color: #000099;
    }
    .expopup em { font-weight: bold; }
    .closebox {
        float: right;
        color: black;
        font: bold 16px sans-serif;
        background-color:white;
        padding: 4px;
        border: 1px solid black; 
        cursor: pointer;
    }
</style>

<table width="100%" style="position:relative;top:-10px;border-bottom:thin solid lightgray">
    <tr>
        <td align="left" valign="bottom" width="55">
            <a href="index.jsp"><img border="0" src="atlasbeta.jpg" width="50" height="25"/></a>
        </td>

        <td align="left">
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> (<b>new!</b>) |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
        <td align="right">
            <a href="http://www.ebi.ac.uk/microarray"><img border="0" height="20" title="EBI ArrayExpress" src="aelogo.png"/></a>
        </td>
    </tr>
</table>
<div style="margin-bottom:50px">

    <c:if test="${!empty query}">
        <div style="margin-top:20px;mragin-bottom:20px" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..."/></div>
        <c:set var="timeStart" value="${u:currentTime()}"/>
        <%
            response.flushBuffer();
            AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().doStructuredAtlasQuery(atlasQuery);
            request.setAttribute("result", atlasResult);
        %>
    </c:if>

    <form name="atlasform" action="qrs" onsubmit="renumberAll();">
        <table>
            <tr valign="top">
                <td>
                    <label class="label" for="gene">Genes</label>
                </td>

                <td>
                    <label class="label" for="species">Organisms</label>
                </td>

                <td style="padding-left:5px">
                    <label class="label" for="conditions">Conditions</label>
                </td>
            </tr>
            <tr valign="top">
                <td>
                    <input type="text" name="gene" id="gene" style="width:150px" value="${query.gene}"/>
                </td>
                <td>
                    <table id="species" cellpadding="0" cellspacing="0">
                        <tbody></tbody>
                    </table>
                </td>
                <td style="padding-left:5px">
                    <table id="conditions" cellpadding="0" cellspacing="0">
                        <tbody></tbody>
                    </table>
                </td>
            </tr>
            <tr>
                <td colspan="3" align="left">
                    <input type="submit" value="Search Atlas">
                </td>
            </tr>
        </table>
        <c:if test="${heatmap}"><input type="hidden" name="view" value="hm" /></c:if>
        <input type="hidden" name="view" value="hm"/>
    </form>

    <script type="text/javascript">
        var lastquery;
        <c:if test="${!empty query}">
        lastquery = {
            gene : '${u:escapeJS(query.gene)}',
            species : [<c:forEach var="i" items="${query.species}">'${u:escapeJS(i)}',</c:forEach>],
            conditions : [
                <c:forEach var="c" items="${result.conditions}">
                { factor: '${u:escapeJS(c.factor)}',
                    expression: '${u:escapeJS(c.expression)}',
                    expansion: <c:choose>
                            <c:when test="${!c.anything && c.expansion.numEfvs == 0}">
                            '<span class="ignored">no matching factor values found, ignored</span>'
                            </c:when>
                            <c:otherwise>
                            '<c:forEach var="e" items="${c.expansion.valueSortedList}" varStatus="i"><c:if test="${c.expansion.numEfs > 1 || (i.first && empty c.factor)}">${u:escapeJS(f:escapeXml(e.ef))}: </c:if> ${u:escapeJS(f:escapeXml(e.efv))}<c:if test="${!i.last}">, </c:if></c:forEach>'
                            </c:otherwise>
                    </c:choose>,
                    values: [<c:forEach var="v" items="${c.factorValues}">'${u:escapeJS(v)}',</c:forEach>]
                },</c:forEach>
            ]
        };
        </c:if>
        initQuery();
    </script>

    <c:if test="${!empty query}">
        <c:set var="resultEfvsTree" value="${result.resultEfvs.nameSortedTree}"/>
        <c:set var="cn" value="${result.resultEfvs.numEfvs}"/>
        <c:set var="sn" value="${f:length(query.species)}"/>
        <script type="text/javascript">

            $("#loading_display").hide();
            var resultGenes = [
            <c:forEach var="row" items="${result.results}">{ geneDwId: '${u:escapeJS(row.gene.geneIdentifier)}', geneAtlasId: '${u:escapeJS(row.gene.geneId)}' },</c:forEach>
            ];

            var resultEfvs = [
            <c:forEach var="e" items="${result.resultEfvs.nameSortedList}">{ ef: '${u:escapeJS(e.ef)}', efv: '${u:escapeJS(e.efv)}' },</c:forEach>
            ];
        </script>

        <c:if test="${result.size > 0}">
            <c:url var="pageUrl" value="/qrs">
                <c:param name="gene" value="${u:escapeJS(query.gene)}"/>
                <c:forEach var="i" varStatus="s" items="${query.species}"><c:param name="specie_${s.index}" value="${i}"/></c:forEach>
                <c:forEach varStatus="cs" var="c" items="${result.conditions}">
                    <c:param name="fact_${cs.index}" value="${c.factor}"/>
                    <c:param name="gexp_${cs.index}" value="${c.expression}"/>
                    <c:forEach varStatus="vs" var="v" items="${c.factorValues}"><c:param name="fval_${cs.index}_${vs.index}" value="${v}"/></c:forEach>
                </c:forEach>
                <c:if test="${heatmap}"><c:param name="view" value="hm"/></c:if>
            </c:url>


            <div class="summary">
                <c:out value="${result.total}" /> matching gene(s) found, displaying <c:out value="${result.start + 1} - ${result.start + result.size}"/> as
                <c:choose>
                    <c:when test="${heatmap}">
                        heatmap (<a href="${f:replace(pageUrl,'&view=hm','')}&p=${query.start}">show as list</a>)
                    </c:when>
                    <c:otherwise>
                        list (<a href="${pageUrl}&p=${query.start}&view=hm">show as heatmap</a>)
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="drilldowns">
                <c:forEach var="ef" items="${result.efvFacet.valueSortedTrimmedTree}">
                    <div class="drillsect">
                        <div class="name"><c:out value="${f:toUpperCase(f:substring(ef.ef, 0, 1))}${f:toLowerCase(f:substring(ef.ef, 1, -1))}"/>:</div>
                        <ul><c:forEach var="efv" items="${ef.efvs}" varStatus="s">
                            <li><a href="${pageUrl}&fact_${cn}=${u:escapeURL(ef.ef)}&gexp_${cn}=UP_DOWN&fval_${cn}_0=${u:escapeURL(efv.efv)}" class="ftot"><c:out value="${efv.efv}"/></a>&nbsp;(<c:if test="${efv.payload.up > 0}"><a href="${pageUrl}&fact_${cn}=${u:escapeURL(ef.ef)}&gexp_${cn}=UP&fval_${cn}_0=${u:escapeURL(efv.efv)}" class="fup"><c:out value="${efv.payload.up}"/>&#8593;</a></c:if><c:if test="${efv.payload.up > 0 && efv.payload.down > 0}">&nbsp;</c:if><c:if test="${efv.payload.down > 0}"><a href="${pageUrl}&fact_${cn}=${u:escapeURL(ef.ef)}&gexp_${cn}=DOWN&fval_${cn}_0=${u:escapeURL(efv.efv)}" class="fdn"><c:out value="${efv.payload.down}"/>&#8595;</a></c:if>)</li>
                        </c:forEach></ul>
                    </div>
                </c:forEach>
                <c:if test="${!empty result.geneFacets['species']}">
                    <div class="drillsect">
                        <div class="name">Specie:</div>
                        <ul>
                            <c:forEach var="sp" items="${result.geneFacets['species']}" varStatus="s">
                                <li><a href="${pageUrl}&specie_${sn}=${u:escapeURL(sp.name)}" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <c:forEach var="facet" items="${result.geneFacets}">
                    <c:if test="${!empty facet.key && facet.key!='species'}">
                        <div class="drillsect">
                            <div class="name"><c:out value="${f:toUpperCase(f:substring(facet.key, 0, 1))}${f:toLowerCase(f:substring(facet.key, 1, -1))}"/>:</div>
                            <ul>
                                <c:forEach var="sp" items="${facet.value}" varStatus="s">
                                    <li><a href="javascript:alert('sorry, not implemented yet')" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                </c:forEach>
                <div style="clear:both;"></div>
            </div>

            <c:if test="${heatmap}">
                <div id="fortyfive"></div>
                <table id="squery" class="squery">
                    <thead>
                        <tr>
                            <th class="gene">Gene</th>
                            <c:forEach var="c" items="${resultEfvsTree}" varStatus="i">
                                <th colspan="${f:length(c.efvs)}" class="factor">
                                    <em style="color:#${i.index % 2 == 0 ? '000000':'999999'}"><c:out value="${c.ef}"/></em>
                                </th>
                            </c:forEach>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${result.results}" varStatus="i">
                            <tr id="squeryrow${i.index}">
                                <td>
                                    <nobr>
                                        <c:set var="geneName" value="${f:split(row.gene.geneName,';')}"/>
                                        <a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}" title="${f:join(geneName, ', ')}"><b><c:out value="${f:substring(geneName[0],0,20)}${f:length(geneName[0]) > 20 || f:length(geneName) > 1 ? '...' : ''}"/></b><c:if test="${empty row.gene.geneName}">(none)</c:if></a>
                                        <span class="ensname">
                                            &nbsp;<c:url var="urlGeneAnnotation" value="http://www.ebi.ac.uk/ebisearch/search.ebi">
                                            <c:param name="db" value="genomes"/>
                                            <c:param name="t" value="${row.gene.geneIdentifier}"/>
                                        </c:url>
                                            <a title="Show gene annotation" target="_blank" href="${urlGeneAnnotation}"><c:out value="${row.gene.geneIdentifier}"/></a>
                                        </span>
                                    </nobr><br />
                                    <nobr>
                                        <span class="label">GO:</span>
                                        <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}">
                                            <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}"/></a>
                                        </c:if>
                                        <c:if test="${empty row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}">(none)</c:if>
                                        <span class="label"> InterPro:</span>
                                        <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}">
                                            <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}"/></a>
                                        </c:if>
                                        <c:if test="${empty row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}">(none)</c:if>
                                    </nobr>
                                </td>
                                <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                                    <c:choose>
                                        <c:when test="${ud.zero}">
                                            <td class="counter"></td>
                                        </c:when>
                                        <c:otherwise>
                                            <td class="acounter" style="background-color:${u:expressionBack(ud)};color:${u:expressionText(ud)}"
                                                title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                onclick="hmc(${i.index},${j.index},this)">
                                                <b>${ud.ups == 0 ? '-' : ud.ups}</b>&nbsp;/&nbsp;<b>${ud.downs == 0 ? '-' : ud.downs}</b>
                                            </td>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <script type="text/javascript">
                    var cs = 0.707106781186548;
                    var attr = {"font": '12px Tahoma', 'text-anchor': 'start'};

                    var testR = Raphael(0,0,10,10);
                    var maxH = 0;
                    var lastW = 0;
                    for(var k = 0; k < resultEfvs.length; ++k)
                    {
                        var txt = testR.text(0, 0, resultEfvs[k].efv).attr(attr);
                        var bw = txt.getBBox().width * cs;
                        if(maxH < bw)
                            maxH = bw;
                        if(k == resultEfvs.length - 1)
                            lastW = bw;
                    }
                    testR.remove();

                    var ff = document.getElementById("fortyfive");
                    var sq = document.getElementById("squery");
                    
                    var R = Raphael("fortyfive", sq.offsetWidth + Math.round(lastW) + 20, Math.round(maxH) + 20);

                    var colors = ['#000000','#999999'];
                    var k = 0;
                    var cp = -1;
                    var curef = null;
                    $("#squery tbody tr:first td:gt(0)").each(function () {
                        if(curef == null || curef != resultEfvs[k].ef)
                        {
                            if(++cp == colors.length)
                                cp = 0;
                            curef = resultEfvs[k].ef;
                        }
                        var x = this.offsetLeft;
                        var txt = R.text(x + 5, R.height - 5, resultEfvs[k].efv).attr(attr).attr({fill: colors[cp]});
                        var bb = txt.getBBox();
                        txt.matrix(cs, cs, -cs, cs, bb.x - cs * bb.x - cs * bb.y, bb.y + cs * bb.x - cs * bb.y);
                        R.path({stroke: "#cdcdcd", 'stroke-width': 2}).moveTo(x - 1, R.height).lineTo(x - 1, R.height - 20);
                        ++k;
                    });
                </script>

            </c:if>
            <c:if test="${!heatmap}">
                <div class="genelist">
                    <c:forEach var="row" items="${result.results}" varStatus="i">
                        <div class="item">
                            <div class="itemhead"><span class="genename">
                                <c:set var="geneName" value="${f:split(row.gene.geneName,';')}"/>
                                <a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}" title="${f:join(geneName, ', ')}"><c:out value="${f:substring(geneName[0],0,20)}${f:length(geneName[0]) > 20 || f:length(geneName) > 1 ? '...' : ''}"/><c:if test="${empty row.gene.geneName}">(none)</c:if></a>
                            </span>
                            <span class="ensname">
                                &nbsp;<c:url var="urlGeneAnnotation" value="http://www.ebi.ac.uk/ebisearch/search.ebi">
                                    <c:param name="db" value="genomes"/>
                                    <c:param name="t" value="${row.gene.geneIdentifier}"/>
                                </c:url>
                                <a title="Show gene annotation" target="_blank" href="${urlGeneAnnotation}"><c:out value="${row.gene.geneIdentifier}"/></a>
                            </span>
                            <span class="label">&nbsp;&nbsp;Specie:</span>
                            <a href="${pageUrl}&specie_${sn}=${u:escapeURL(row.gene.geneSpecies)}"><c:out value="${row.gene.geneSpecies}"/></a>
                            <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}">
                                <span class="label">&nbsp;&nbsp;GO Term:</span>
                                <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}"/></a>
                            </c:if>
                            <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}">
                                <span class="label">&nbsp;&nbsp;InterPro Term:</span>
                                <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}"/></a>
                            </c:if></div>
                            <div class="efvs">
                                <c:forEach var="ef" items="${resultEfvsTree}">
                                    <c:set var="xx" value="" />
                                    <c:forEach var="efv" items="${ef.efvs}">
                                        <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                        <c:if test="${!ud.zero}">
                                            <c:set var="xx" value="1" />
                                        </c:if>
                                    </c:forEach>
                                    <c:if test="${!empty xx}"><table class="factab">
                                        <tr><th class="factor" rowspan="2"><c:out value="${ef.ef}"/></th><c:forEach var="efv" items="${ef.efvs}">
                                            <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                            <c:if test="${!ud.zero}">
                                                <th align="center" class="counter"><c:out value="${efv.efv}"/></th>
                                            </c:if>
                                        </c:forEach></tr>
                                        <tr><c:forEach var="efv" items="${ef.efvs}">
                                            <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                            <c:if test="${!ud.zero}">
                                                        <td class="acounter" style="background-color:${u:expressionBack(ud)};color:${u:expressionText(ud)}"
                                                            title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                            onclick="hmc(${i.index},${efv.number},this);">
                                                            <b>${ud.ups == 0 ? '-' : ud.ups}</b>&nbsp;/&nbsp;<b>${ud.downs == 0 ? '-' : ud.downs}</b>
                                                        </td>
                                            </c:if>
                                        </c:forEach></tr>
                                    </table></c:if>
                                </c:forEach>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

            <c:if test="${result.size < result.total}">
                <div class="pagination">
                    <c:if test="${result.start >= result.rows}">
                        <a href="${pageUrl}&p=${result.start - result.rows}">prev</a>
                    </c:if>
                    <c:forEach var="p" begin="0" end="${result.total}" step="${result.rows}">
                        <c:if test="${result.start == p}">
                            <b><fmt:formatNumber value="${(p / result.rows) + 1}" maxFractionDigits="0"/></b>
                        </c:if>
                        <c:if test="${result.start != p}">
                            <a href="${pageUrl}&p=${p}"><fmt:formatNumber value="${(p / result.rows) + 1}" maxFractionDigits="0"/></a>
                        </c:if>
                    </c:forEach>
                    <c:if test="${result.total - result.start > result.rows}">
                        <a href="${pageUrl}&p=${result.start + result.rows}">next</a>
                    </c:if>
                </div>
            </c:if>
        </c:if>
        <c:if test="${result.size == 0}">
            <div style="margin-top:30px;margin-bottom:20px;font-weight:bold;">No matching results found.</div>
        </c:if>
        <c:set var="timeFinish" value="${u:currentTime()}"/>
        <p>
            Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.
        </p>        
    </c:if>

</div>

<jsp:include page="end_body.jsp"></jsp:include>

