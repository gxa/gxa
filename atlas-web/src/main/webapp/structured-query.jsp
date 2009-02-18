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
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="scripts/pagination.css" />

<script type="text/javascript" src="jquery.min.js"></script>
<script type="text/javascript" src="jquery.cookie.js"></script>
<script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="jquery.autocomplete.js"></script>
<script type="text/javascript" src="jquery.tooltip.js"></script>
<script type="text/javascript" src="jquery.dimensions.js"></script>
<script type="text/javascript" src="jquerydefaultvalue.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->
<script type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="structured-query.js"></script>
<script type="text/javascript" src="raphael-packed.js"></script>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>


<table width="100%" style="position:relative;top:-10px;border-bottom:thin solid lightgray">
    <tr>
        <td align="left" valign="bottom" width="55">
            <a href="index.jsp"><img border="0" src="atlasbeta.jpg" width="50" height="25" /></a>
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
            <a href="http://www.ebi.ac.uk/microarray"><img border="0" height="20" title="EBI ArrayExpress" src="aelogo.png" /></a>
        </td>
    </tr>
</table>
<div style="margin-bottom:50px">

    <c:if test="${!empty query}">
        <div style="margin-top:20px;margin-bottom:20px" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..." /></div>
        <c:set var="timeStart" value="${u:currentTime()}"/>
        <%
            response.flushBuffer();
            AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(atlasQuery);
            request.setAttribute("result", atlasResult);
        %>
    </c:if>

    <form id="simpleform" action="qrs" style="visibility:hidden;">
        <table>
            <tr valign="top">
                <td>
                    <label class="label" for="gene0">Find Genes</label>
                </td>

                <td>
                    <label class="label" for="species0">Organisms</label>
                </td>

                <td style="padding-left:5px">
                    <label class="label" for="fval0">Conditions</label>
                </td>

                <td colspan="2"></td>
            </tr>
            <tr valign="top">
                <td>
                    <input type="hidden" name="gprop_0" id="gprop0" value="${!empty query && query.simple ? f:escapeXml(query.geneQueries[0].factor) : ''}">
                    <input type="text" name="gval_0" id="gene0" style="width:150px" value="${!empty query && query.simple ? f:escapeXml(query.geneQueries[0].jointFactorValues) : ''}" />
                </td>
                <td>
                    <select name="specie_0" id="species0">
                        <option value="">(any)</option>
                        <c:forEach var="s"
                                   items="${service.allAvailableAtlasSpecies}">
                            <option ${!empty query.species && s == query.species[0] ? 'selected="selected"' : ''} value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
                        </c:forEach>
                    </select>   
                </td>
                <td style="padding-left:5px">
                    <select name="fexp_0">
                        <c:forEach var="s"
                                   items="${service.structQueryService.geneExpressionOptions}">
                            <option ${!empty query && query.simple && s[0] == query.conditions[0].expression ? 'selected="selected"' : ''} value="${f:escapeXml(s[0])}">${f:escapeXml(s[1])}</option>
                        </c:forEach>
                    </select>
                    in
                    <input type="hidden" name="fact_0" value="">
                    <input type="text" name="fval_0" id="fval0" style="width:150px" value="${!empty query && query.simple ? f:escapeXml(query.conditions[0].jointFactorValues) : ''}" />
                </td>
                <td align="left">
                    <input type="submit" value="Search Atlas">
                </td>
                <td>
                    <a href="javascript:structMode();">advanced mode</a>
                </td>
            </tr>
        </table>
        <c:if test="${query.simple && !empty result.conditions && result.conditions[0].expansion.numEfvs > 0}">
            <p class="expansion">
            	<c:forEach var="e" items="${result.conditions[0].expansion.valueSortedList}" varStatus="i">
            		<c:if test="${result.conditions[0].expansion.numEfs > 1 || (i.first && empty c.factor)}">
            			${f:escapeXml(e.ef)}: 
            		</c:if> 
            		${f:escapeXml(e.efv)}
            		<c:if test="${!i.last}">, </c:if>
            	</c:forEach>
            </p>
        </c:if>
        <input type="hidden" name="view" value="hm" />
    </form>
    
    <form id="structform" name="atlasform" action="qrs" onsubmit="renumberAll();" style="visibility:hidden;">
        <table>
            <tbody id="conditions"></tbody>
        </table>
        <input type="hidden" name="view" value="hm" />
        <div style="margin-top: 30px">
            <select id="factors">
                <option value="">-&gt; Add experimental factor</option>
                <option value="">(any)</option>
                <c:forEach var="i" items="${service.structQueryService.experimentalFactorOptions}">
                    <option value="${f:escapeXml(i)}">${f:escapeXml(i)}</option>
                </c:forEach>
            </select>
            <select id="species">
                <option value="">-&gt; Add species</option>
                <c:forEach var="i" items="${service.allAvailableAtlasSpecies}">
                    <option value="${f:escapeXml(i)}">${f:escapeXml(i)}</option>
                </c:forEach>
            </select>
            <select id="geneprops">
                <option value="">-&gt; Add gene property</option>
                <option value="">(any)</option>
                <c:forEach var="i" items="${service.geneProperties}">
                    <option value="${f:escapeXml(i)}">${f:escapeXml(i)}</option>
                </c:forEach>
            </select>
        </div>
        <div style="margin-top: 30px;margin-bottom:30px;">
            <input type="submit" value="Search Atlas">
            <a href="javascript:simpleMode();"> simple mode</a>
        </div>
    </form>

    <script type="text/javascript">
        var options = {
            expressions : [
                    <c:forEach var="i" varStatus="s" items="${service.structQueryService.geneExpressionOptions}">
                    [ '${u:escapeJS(i[0])}', '${u:escapeJS(i[1])}' ]<c:if test="${!s.last}">,</c:if>
                    </c:forEach>
            ],
            species : [
                <c:forEach var="i" varStatus="s" items="${service.allAvailableAtlasSpecies}">
                '${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if>
                </c:forEach>
            ]
        };

        var lastquery;
        <c:if test="${!empty query}">
        lastquery = {
            genes: [ <c:forEach var="g" varStatus="s" items="${query.geneQueries}">
            			{ query: '${g.jointFactorValues}', property:'${g.factor}', not: ${g.negated ? 1 : 0} }<c:if test="${!s.last}">,</c:if>
            		</c:forEach>
            	   ],
            species : [<c:forEach var="i" varStatus="s" items="${query.species}">'${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if></c:forEach>],
            conditions : [
                <c:forEach var="c" varStatus="s" items="${result.conditions}">
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
                    values: '${u:escapeJS(c.jointFactorValues)}'
                }<c:if test="${!s.last}">,</c:if></c:forEach>
            ]
        };
        </c:if>
        $(document).ready(function () {
            initQuery();

            $('#simpleform, #structform').css('visibility', 'visible');
            $('#${empty query || query.simple ? 'struct' : 'simple'}form').hide();
        });
    </script>

    <c:if test="${!empty query}">
        <c:set var="cn" value="${f:length(query.conditions)}"/>
        <c:set var="sn" value="${f:length(query.species)}"/>
        <c:set var="gn" value="${f:length(query.geneQueries)}"/>
        <script type="text/javascript">

            $("#loading_display").hide();
            var resultGenes = [
            <c:forEach var="row" varStatus="s" items="${result.results}">{ geneDwId: '${u:escapeJS(row.gene.geneIdentifier)}', geneAtlasId: '${u:escapeJS(row.gene.geneId)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
            ];

            var resultEfvs = [
            <c:forEach var="e" varStatus="s" items="${result.resultEfvs.nameSortedList}">{ ef: '${u:escapeJS(e.ef)}', efv: '${u:escapeJS(e.efv)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
            ];
        </script>

        <c:if test="${result.size > 0}">
            <c:url var="pageUrl" value="/qrs">
               
                <c:forEach var="g" varStatus="gs"items="${query.geneQueries}">
                    <c:param name="gnot_${gs.index}" value="${g.negated ? '1' : ''}" />
                    <c:param name="gval_${gs.index}" value="${g.jointFactorValues}" />
                	<c:param name="gprop_${gs.index}" value="${g.factor}" />
                </c:forEach>
                <c:forEach var="i" varStatus="s" items="${query.species}"><c:param name="specie_${s.index}" value="${i}"/></c:forEach>
                <c:forEach varStatus="cs" var="c" items="${result.conditions}">
                    <c:param name="fact_${cs.index}" value="${c.factor}"/>
                    <c:param name="fexp_${cs.index}" value="${c.expression}"/>
                    <c:param name="fval_${cs.index}" value="${c.jointFactorValues}"/>
                </c:forEach>
                <c:if test="${heatmap}"><c:param name="view" value="hm"/></c:if>
            </c:url>

            <c:if test="${!empty result && result.size < result.total}">
            <script type="text/javascript">
                $(document).ready(function () {
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
                    opts.num_display_entries = 10;
                    $(".page_long").pagination(${result.total}, opts);
                });
            </script>
            </c:if>

            <div class="summary">
                <c:out value="${result.total}" /> matching gene(s) found, displaying as
                <c:choose>
                    <c:when test="${heatmap}">
                        heatmap (<a href="${f:replace(pageUrl,'&view=hm','')}&amp;p=${query.start}">show as list</a>)
                    </c:when>
                    <c:otherwise>
                        list (<a href="${pageUrl}&amp;p=${query.start}&amp;view=hm">show as heatmap</a>)
                    </c:otherwise>
                </c:choose>
            </div>

            <table id="twocol"><tr>
            <c:if test="${result.total >= u:getIntProp('atlas.drilldowns.mingenes')}">
            <td id="drilldowns">
                <div class="pagination_ie page_short"></div>
                <c:forEach var="ef" items="${result.efvFacet.valueSortedTrimmedTree}">
                    <div class="drillsect">
                        <div class="name"><fmt:message key="head.ef.${ef.ef}"/>:</div>
                        <ul><c:forEach var="efv" items="${ef.efvs}" varStatus="s">
                            <li><nobr><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=UP_DOWN&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="ftot" title="${f:escapeXml(efv.efv)}"><c:out value="${u:truncate(efv.efv, 30)}"/></a>&nbsp;(<c:if test="${efv.payload.up > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=UP&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="fup"><c:out value="${efv.payload.up}"/>&#8593;</a></c:if><c:if test="${efv.payload.up > 0 && efv.payload.down > 0}">&nbsp;</c:if><c:if test="${efv.payload.down > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;fexp_${cn}=DOWN&amp;fval_${cn}=${u:escapeURL(u:optionalQuote(efv.efv))}" class="fdn"><c:out value="${efv.payload.down}"/>&#8595;</a></c:if>)</nobr></li>
                        </c:forEach></ul>
                    </div>
                </c:forEach>
                <c:if test="${!empty result.geneFacets['species']}">
                    <div class="drillsect">
                        <div class="name">Species:</div>
                        <ul>
                            <c:forEach var="sp" items="${result.geneFacets['species']}" varStatus="s">
                                <li><nobr><a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(sp.name)}" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</nobr></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <c:forEach var="facet" items="${result.geneFacets}">
                    <c:if test="${!empty facet.key && facet.key!='species'}">
                        <div class="drillsect">
                            <div class="name"><fmt:message key="head.gene.${facet.key}"/>:</div>
                            <ul>
                                <c:forEach var="fv" items="${facet.value}" varStatus="s">
                                    <li><nobr><a href="${pageUrl}&amp;gval_${gn}=${u:escapeURL(u:optionalQuote(fv.name))}&amp;gprop_${gn}=${u:escapeURL(facet.key)}" title="${f:escapeXml(fv.name)}" class="ftot"><c:out value="${u:truncate(fv.name, 30)}"/></a>&nbsp;(<c:out value="${fv.count}"/>)</nobr></li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                </c:forEach>
                <div style="clear:both;"></div>
            </td>
            </c:if>

            <c:if test="${true || heatmap}">
                <td id="resultpane">
                <table id="squery">
                    <tbody>
                        <tr class="header">
                            <th class="padded" rowspan="2">Gene</th>
                            <c:if test="${f:length(query.species) != 1}">
                                <th class="padded" rowspan="2">Species</th>
                            </c:if>
                            <map name="efvmap">
                                <c:forEach var="i" items="${result.resultEfvs.nameSortedList}" varStatus="s">
                                    <area alt="${f:escapeXml(i.efv)}" title="${f:escapeXml(i.efv)}" shape="poly" coords="${s.index*27},80,${s.index*27+80},0,${s.index*27+99+17},0,${s.index*27+17},99,${s.index*27},99,${s.index*27},80" onclick="return false;">
                                </c:forEach>
                            </map>
                            <c:set scope="session" var="diagonalTexts" value="${result.resultEfvs.efvArray}" />
                            <c:url var="imgUrl" value="/thead">
                                <c:param name="random" value="${u:currentTime()}" />
                                <c:param name="st" value="1" />
                                <c:param name="s" value="27" />
                                <c:param name="fs" value="11" />
                                <c:param name="h" value="100" />
                                <c:param name="lh" value="15" />
                                <c:param name="lc" value="#cdcdcd" />
                                <c:param name="tc" value="#000000" />
                            </c:url>
                            <td colspan="${result.resultEfvs.numEfvs}"><div style="position:relative;height:100px;"><div style="position:absolute;bottom:0;left:-1px;"><img src="${imgUrl}" usemap="#efvmap"></div></div></td>
                        </tr>
                        <tr>
                            <c:forEach var="c" items="${result.resultEfvs.nameSortedTree}" varStatus="i">
                                <c:set var="eftitle"><fmt:message key="head.ef.${c.ef}"/></c:set>
                                <th colspan="${f:length(c.efvs)}" class="factor" title="${eftitle}">
                                    <div style="width:${f:length(c.efvs) * 26 - 1}px;">${eftitle}</div>
                                    <c:choose>
                                        <c:when test="${u:isInSet(query.expandColumns, c.ef)}">
                                            <a title="Collapse factor values for ${eftitle}" href="${pageUrl}&amp;p=${result.start}">««&nbsp;less</a>
                                        </c:when>
                                        <c:when test="${u:isInSet(result.expandableEfs, c.ef)}">
                                            <a title="Show more factor values for ${eftitle}..." href="${pageUrl}&amp;p=${result.start}&amp;fexp=${c.ef}">more&nbsp;»»</a>
                                        </c:when>
                                    </c:choose>
                                </th>
                            </c:forEach>
                        </tr>
                        <c:forEach var="row" items="${result.results}" varStatus="i">
                            <tr id="squeryrow${i.index}">
                                <td class="padded genename">
                                    <a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}">${row.gene.hilitGeneName}<c:if test="${empty row.gene.geneName}"><c:out value="${row.gene.geneIdentifier}"/></c:if></a>
                                    <div class="gtooltip">
                                        <div class="genename"><b>${row.gene.hilitGeneName}</b> (${row.gene.geneIdentifier})</div>
                                        <c:if test="${!empty row.gene.keyword}"><b>Keyword:</b> ${row.gene.hilitKeyword}<br></c:if>
                                        <c:if test="${!empty row.gene.goTerm}"><b>Go Term:</b> ${row.gene.hilitGoTerm}<br></c:if>
                                        <c:if test="${!empty row.gene.interProTerm}"><b>InterPro Term:</b> ${row.gene.hilitInterProTerm}<br></c:if>
                                    </div>
                                </td>
                                <c:if test="${f:length(query.species) != 1}">
                                    <td class="padded"><a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(row.gene.geneSpecies)}"><c:out value="${row.gene.geneSpecies}"/></a></td>
                                </c:if>
                                <c:set var="first" value="true" />
                                <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                                    <c:choose>
                                        <c:when test="${ud.zero}">
                                            <td class="counter"><c:if test="${first}"><div class="osq"></div><c:set var="first" value="false" /></c:if></td>
                                        </c:when>
                                        <c:when test="${ud.ups == 0 && ud.downs > 0}">
                                            <td class="acounter" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s). Click to view..."
                                                onclick="hmc(${i.index},${j.index},event)"><div class="osq">${ud.downs}</div></td>
                                        </c:when>
                                        <c:when test="${ud.downs == 0 && ud.ups > 0}">
                                            <td class="acounter" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s). Click to view..."
                                                onclick="hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.ups}</div></td>
                                        </c:when>
                                        <c:otherwise>
                                            <td class="acounter"
                                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                                onclick="hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="tri" style="border-right-color:${u:expressionBack(ud,-1)};border-top-color:${u:expressionBack(ud,1)};"></div><div style="color:${u:expressionText(ud,-1)}" class="dnval">${ud.downs}</div><div style="color:${u:expressionText(ud,1)}" class="upval">${ud.ups}</div></div></td>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                    <div class="pagination_ie page_long"></div>

                    <c:set var="timeFinish" value="${u:currentTime()}"/>
                    <p>
                        Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.
                    </p>
                </td>
            </c:if>
            </tr></table>
        </c:if>
        <c:if test="${result.size == 0}">
            <div style="margin-top:30px;margin-bottom:20px;font-weight:bold;">No matching results found.</div>
        </c:if>
    </c:if>

</div>

<jsp:include page="end_body.jsp"></jsp:include>

