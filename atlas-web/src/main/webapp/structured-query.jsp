<%String svnBuildString = "$Rev: 4866 $ $Date: 2008-06-20 11:57:28 +0100 (Fri, 20 Jun 2008) $";%>
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

    <script type="text/javascript">
        $(document).ready(function()
            {
                $("#gene").defaultvalue("(all genes)");
                $("#gene").autocomplete("autocomplete.jsp", {
                        minChars:1,
                        matchCase: true,
                        matchSubset: false,
                        multiple: true,
                        multipleSeparator: " ",                    
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
    #squery, .factab {
        font-family: arial, sans-serif;
        background-color: #CDCDCD;
        font-size: 8pt;
        text-align: left;
    }
    #squery {
        margin:10px 0pt 15px;
    }
    .factab {
        margin-bottom:3px;
    }
    #squery th, .factab th {
        background-color: #e6EEEE;
        border: 1px solid #FFF;
        font-size: 8pt;
        padding: 4px;
    }
    #squery td, .factab td {
        color: #3D3D3D;
        padding: 4px;
        background-color: #FFF;
        vertical-align: top;
    }

    .prefix { padding-right:5px;}
    .speciesSelect { width: 120px; margin-right:5px; }

    .expansion { padding-left: 10px;font-size: smaller;color:gray; }
    div.value select, div.value input.value { width:150px; }
    div.value div.buttons { float: right; }
    div.value div.input { float: left; }
    div.value { width: 250px; margin-bottom:2px; }
    #conditions td { vertical-align: top; }
    #conditions td.factorvalue { padding-bottom: 10px; padding-left: 5px; }
    #conditions td.andbuttons {  padding-bottom:10px; padding-left: 10px}
    div.countup, div.countdn { width:50%;float:left;text-align:center; }
    div.expref { clear:both;text-align:left;width:100%;background-color:#f0f0f0; }
    #squery td.counter, th.counter {text-align:center;width:80px; }
    td.common { white-space:nowrap; }
    th.factor { text-align:center;font-weight:normal; }
    th.factor em { font-weight:bold;font-style:normal;}
    th.gene { vertical-align:middle;text-align:center; }
    div.exps { clear:both;margin-top:2px; }
    ul.upexp, ul.dnexp { list-style:none; float: left;width:50%; padding:0px; margin:0px;list-style-position:outside;overflow:hidden;  }
    ul.upexp li, ul.dnexp li { padding:0px; margin:0px;font-size:7pt; }
    ul.upexp li { text-align:left; }
    ul.dnexp li { text-align:right; }
    ul.upexp li { background-color: #fff0f0; }
    ul.dnexp li { background-color: #f0f0ff; }

    a.fup:link, a.fup:active, a.fup:hover, a.fup:visited { color: #660000;text-decoration: underline; }
    a.fdn:link, a.fdn:active, a.fdn:hover, a.fdn:visited { color: #000066;text-decoration: underline; }
    a.ftot:link, aftot:active, a.ftot:hover, a.ftot:visited { color: #666666;text-decoration: underline; }

    .pagination { margin-top: 20px; text-align: center; }

    .genelist { width: 90%; }
    .genelist .item { margin-top: 10px;margin-bottom: 20px; }
    .genelist .item .genename { font-size: 20px }
    .genelist .item .ensname { font-size: smaller; }
    .genelist .item .label { color: #666666;font-size: smaller; }
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
                    <c:if test="${!empty query}">
                        <div style="margin:10px auto;width:150px;text-align:center;clear:both" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..."/></div>
                    </c:if>    
                </td>
            </tr>
        </table>
    </form>

    <c:if test="${!empty query}">
        <c:set var="timeStart" value="${u:currentTime()}"/>
        <%
            response.flushBuffer();
            AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().doStructuredAtlasQuery(atlasQuery);
            request.setAttribute("result", atlasResult);
        %>
    </c:if>

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
                    expansion: '<c:forEach var="e" items="${c.expansion.valueSortedList}" varStatus="i"><c:if test="${c.expansion.numEfs > 1 || (i.first && empty c.factor)}">${u:escapeJS(e.ef)}: </c:if> ${u:escapeJS(e.efv)}<c:if test="${!i.last}">, </c:if></c:forEach>',
                    values: [<c:forEach var="v" items="${c.factorValues}">'${u:escapeJS(v)}',</c:forEach>]
                },</c:forEach>
            ]
        };
        </c:if>
        initQuery();
    </script>

    <c:if test="${!empty query}">
        <c:set var="queryEfvsTree" value="${result.queryEfvs.nameSortedTree}"/>
        <c:set var="cn" value="${result.queryEfvs.numEfvs}"/>
        <c:set var="sn" value="${f:length(query.species)}"/>
        <script type="text/javascript">

            $("#loading_display").hide();
            var resultGenes = [
            <c:forEach var="row" items="${result.results}">{ geneDWId: '${u:escapeJS(row.gene.geneIdentifier)}', geneAtlasId: '${u:escapeJS(row.gene.geneId)}' },</c:forEach>
            ];

            <c:url var="urlExps" value="/sexpt">
                <c:forEach var="c" varStatus="s" items="${queryEfvsTree}">
                    <c:param name="ef${s.index}" value="${c.ef}"/>
                    <c:forEach var="v" items="${c.efvs}"><c:param name="fv${s.index}" value="${v.efv}"/></c:forEach>
                </c:forEach>
            </c:url>
            var exptUrlBase = '${u:escapeJS(urlExps)}';
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
            </c:url>


            <div class="summary">
                <c:out value="${result.total}" /> matching gene(s) found, displaying <c:out value="${result.start} - ${result.start + result.size}"/> as
                <c:choose>
                    <c:when test="${heatmap}">
                        heatmap (<a href="${pageUrl}&p=${query.start}">show as list</a>)
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
                                    <li><a href="${pageUrl}" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                </c:forEach>
                <div style="clear:both;"></div>
            </div>

            <c:if test="${heatmap}">
                <table id="squery">
                    <thead>
                        <tr>
                            <th colspan="2" rowspan="2" class="gene">Gene</th>
                            <c:forEach var="c" items="${queryEfvsTree}">
                                <th colspan="${f:length(c.efvs)}" class="factor">
                                    <em><c:out value="${c.ef}"/></em>
                                </th>
                            </c:forEach>
                            <th rowspan="2" valign="top"><img class="expexp" onclick="loadExperiments();" src="expandopen.gif" alt="&gt;" title="Toggle all experiments" width="11" height="11" style="border:0px;"/></th>
                        </tr>
                        <tr>
                            <c:forEach var="c" items="${queryEfvsTree}">
                                <c:forEach var="v" items="${c.efvs}">
                                    <th class="counter"><c:out value="${v.efv}"/></th>
                                </c:forEach>
                            </c:forEach>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${result.results}" varStatus="i">
                            <tr id="squeryrow${i.index}">
                                <td>
                                    <c:url var="urlGeneAnnotation" value="http://www.ebi.ac.uk/ebisearch/search.ebi">
                                        <c:param name="db" value="genomes"/>
                                        <c:param name="t" value="${row.gene.geneIdentifier}"/>
                                    </c:url>
                                    <a title="Show gene annotation" target="_blank" href="${urlGeneAnnotation}"><c:out value="${row.gene.geneIdentifier}"/></a>
                                </td>
                                <c:set var="geneName" value="${f:split(row.gene.geneName,';')}"/>
                                <td><a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}" title="${f:join(geneName, ', ')}"><c:out value="${f:substring(geneName[0],0,20)}${f:length(geneName[0]) > 20 || f:length(geneName) > 1 ? '...' : ''}"/><c:if test="${empty row.gene.geneName}">(none)</c:if></a></td>
                                <c:forEach var="e" items="${result.queryEfvs.nameSortedList}">
                                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                                    <td class="counter">
                                        <c:set var="upc" value="${ud.ups != 0 ? (ud.mpvUp > 0.05 ? 0.05 : ud.mpvUp) * 240 / 0.05 : 240}"/>
                                        <c:set var="dnc" value="${ud.downs != 0 ? (ud.mpvDn > 0.05 ? 0.05 : ud.mpvDn) * 240 / 0.05 : 240}"/>
                                        <div>
                                            <c:set var="upc"><fmt:formatNumber maxFractionDigits="0" value="${ud.ups != 0 ? (ud.mpvUp > 0.05 ? 0.05 : ud.mpvUp) * 240 / 0.05 : 240}"/></c:set>
                                            <c:set var="dnc"><fmt:formatNumber maxFractionDigits="0" value="${ud.downs != 0 ? (ud.mpvDn > 0.05 ? 0.05 : ud.mpvDn) * 240 / 0.05 : 240}"/></c:set>
                                            <div class="countup" title="${ud.ups != 0 ? 'p-value is ' : ''}${ud.ups != 0 ? ud.mpvUp : ''}" style="background-color:rgb(255,${upc},${upc});color:${upc > 200 ? 'black' : 'white'}">&nbsp;${ud.ups == 0 ? '-' : ud.ups}&nbsp;</div>
                                            <div class="countdn" title="${ud.downs != 0 ? 'p-value is ' : ''}${ud.downs != 0 ? ud.mpvDn : ''}" style="background-color:rgb(${dnc},${dnc},255);color:${dnc > 200 ? 'black' : 'white'}">&nbsp;${ud.downs == 0 ? '-' : ud.downs}&nbsp;</div>
                                        </div>
                                    </td>
                                </c:forEach>
                                <td><img class="expexp" onclick="loadExperiments(${i.index});" src="expandopen.gif" alt="&gt;" title="Toggle experiments for this gene" width="11" height="11" style="border:0px;"/></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
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
                                <a href="javascript:void"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}"/></a>
                            </c:if>
                            <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}">
                                <span class="label">&nbsp;&nbsp;InterPro Term:</span>
                                <a href="javascript:void"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}"/></a>
                            </c:if></div>
                            <div class="efvs">
                                <c:forEach var="ef" items="${queryEfvsTree}">
                                    <c:set var="xx" value="" />
                                    <c:forEach var="efv" items="${ef.efvs}">
                                        <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                        <c:if test="${(ud.ups > 0 || ud.downs > 0)}">
                                            <c:set var="xx" value="1" />
                                        </c:if>
                                    </c:forEach>
                                    <c:if test="${!empty xx}"><table class="factab">
                                        <tr><th class="factor" rowspan="2"><c:out value="${ef.ef}"/></th><c:forEach var="efv" items="${ef.efvs}">
                                            <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                            <c:if test="${ud.ups > 0 || ud.downs > 0}">
                                                <th align="center" class="counter"><c:out value="${efv.efv}"/></th>
                                            </c:if>
                                        </c:forEach></tr>
                                        <tr><c:forEach var="efv" items="${ef.efvs}">
                                            <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                            <c:if test="${ud.ups > 0 || ud.downs > 0}"><td class="counter">
                                                <c:set var="upc"><fmt:formatNumber maxFractionDigits="0" value="${ud.ups != 0 ? (ud.mpvUp > 0.05 ? 0.05 : ud.mpvUp) * 240 / 0.05 : 240}"/></c:set>
                                                <c:set var="dnc"><fmt:formatNumber maxFractionDigits="0" value="${ud.downs != 0 ? (ud.mpvDn > 0.05 ? 0.05 : ud.mpvDn) * 240 / 0.05 : 240}"/></c:set>
                                                <div class="countup" title="${ud.ups != 0 ? 'p-value is ' : ''}${ud.ups != 0 ? ud.mpvUp : ''}" style="background-color:rgb(255,${upc},${upc});color:${upc > 200 ? 'black' : 'white'}">&nbsp;${ud.ups == 0 ? '-' : ud.ups}&nbsp;</div>
                                                <div class="countdn" title="${ud.downs != 0 ? 'p-value is ' : ''}${ud.downs != 0 ? ud.mpvDn : ''}" style="background-color:rgb(${dnc},${dnc},255);color:${dnc > 200 ? 'black' : 'white'}">&nbsp;${ud.downs == 0 ? '-' : ud.downs}&nbsp;</div>
                                            </td></c:if>
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
        <c:if test="${empty result}">
            No results found!
        </c:if>
        <c:set var="timeFinish" value="${u:currentTime()}"/>
        <p>
            Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.
        </p>        
    </c:if>

</div>

<jsp:include page="end_body.jsp"></jsp:include>

