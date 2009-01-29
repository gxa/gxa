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

<script type="text/javascript" src="jquery.min.js"></script>
<script type="text/javascript" src="jquery.cookie.js"></script>
<script type="text/javascript" src="jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="jquery.autocomplete.js"></script>
<script type="text/javascript" src="jquery.truncator.js"></script>
<script type="text/javascript" src="jquerydefaultvalue.js"></script>
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
        <div style="margin-top:20px;mragin-bottom:20px" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..." /></div>
        <c:set var="timeStart" value="${u:currentTime()}"/>
        <%
            response.flushBuffer();
            AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(atlasQuery);
            request.setAttribute("result", atlasResult);
        %>
    </c:if>

    <form id="simpleform" action="qrs">
        <table>
            <tr valign="top">
                <td>
                    <label class="label" for="gene">Find Genes</label>
                </td>

                <td>
                    <label class="label" for="species">Organisms</label>
                </td>

                <td style="padding-left:5px">
                    <label class="label" for="conditions">Conditions</label>
                </td>

                <td colspan="2"></td>
            </tr>
            <tr valign="top">
                <td>
                    <input type="text" name="gene_0" class="geneqry" id="gene0" style="width:150px" value="${f:escapeXml(query.gene)}" />
                </td>
                <td>
                    <select name="specie_0">
                        <option value="">(any)</option>
                        <c:forEach var="s"
                                   items="${service.allAvailableAtlasSpecies}">
                            <option ${!empty query.species && s == query.species[0] ? 'selected="selected"' : ''} value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
                        </c:forEach>
                    </select>   
                </td>
                <td style="padding-left:5px">
                    <select name="gexp">
                        <c:forEach var="s"
                                   items="${service.structQueryService.geneExpressionOptions}">
                            <option ${!empty query && query.simple && s[0] == query.conditions[0].expression ? 'selected="selected"' : ''} value="${f:escapeXml(s[0])}">${f:escapeXml(s[1])}</option>
                        </c:forEach>
                    </select>
                    in
                    <input type="text" name="fval" id="fval0" style="width:150px" value="${!empty query && query.simple ? f:escapeXml(query.conditions[0].jointFactorValues) : ''}" />
                </td>
                <td align="left">
                    <input type="submit" value="Search Atlas">
                </td>
                <td>
                    <a href="javascript:structuredMode();">advanced mode</a>
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
    
    <form id="structform" name="atlasform" action="qrs" onsubmit="renumberAll();">
        <table>
            <tr valign="top">
                <td colspan="1">
                    <label class="label" for="gene">Find Genes</label>
                </td>
                <!-- 
				<td>
                    <label class="label" for="geneProps">Gene Properties</label>
                </td>
                 -->
                <td>
                    <label class="label" for="species">Organisms</label>
                </td>

                <td style="padding-left:5px">
                    <label class="label" for="conditions">Conditions</label>
                </td>
            </tr>
            <tr valign="top">
                <!-- 
                <td>
                
                    <input type="text" name="gene" id="gene" style="width:150px" value="${f:escapeXml(query.gene)}" />
                </td>
                -->
                <td align="left">
                    <table id="geneprops" cellpadding="0" cellspacing="0">
                        <tbody></tbody>
                    </table>
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
                <td colspan="2" align="left">
                    <input type="submit" value="Search Atlas">
                </td>
                <td align="right">
                    <a href="javascript:simpleMode();">simple mode</a>
                </td>
            </tr>
        </table>
        <input type="hidden" name="view" value="hm" />
    </form>

    <script type="text/javascript">
        var options = {
            expressions : [
                    <c:forEach var="i" items="${service.structQueryService.geneExpressionOptions}">
                    [ '${u:escapeJS(i[0])}', '${u:escapeJS(i[1])}' ],
                    </c:forEach>
            ],
            factors : [
                <c:forEach var="i" items="${service.structQueryService.experimentalFactorOptions}">
                '${u:escapeJS(i)}',
                </c:forEach>
            ],
            species : [
                <c:forEach var="i" items="${service.allAvailableAtlasSpecies}">
                '${u:escapeJS(i)}',
                </c:forEach>
            ],
            geneProps :[
            	<c:forEach var="i" items="${service.geneProperties}">
            	'${u:escapeJS(i)}',
            	</c:forEach>
            ],
            geneOperators :['','OR','AND','NOT'
            ]
        };

        var lastquery;
        <c:if test="${!empty query}">
        lastquery = {
            
            genes: [ <c:forEach var="g" items="${query.geneQueries}">
            			{ qry:     '${g.qry}',
            			  property:'${g.property}',
            			  operator: '${g.operator}'
            			},
            		</c:forEach>
            	   ],
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
        $("#${empty query || query.simple ? 'struct' : 'simple'}form").hide();
    </script>

    <c:if test="${!empty query}">
        <c:set var="cn" value="${result.resultEfvs.numEfvs}"/>
        <c:set var="sn" value="${f:length(query.species)}"/>
        <c:set var="gn" value="${f:length(query.geneQueries)}"/>
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
               
                <c:forEach var="g" varStatus="gs"items="${query.geneQueries}">
                	<c:param name="gene_${gs.index}" value="${u:escapeJS(g.qry)}"></c:param>
                	<c:param name="geneprop_${gs.index}" value="${g.property}"></c:param>
                	<c:if test="${gs.index != 0}"><c:param name="geneoperator_${gs.index}" value="${g.operator}"></c:param></c:if>
                </c:forEach> 
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
                        heatmap (<a href="${f:replace(pageUrl,'&view=hm','')}&amp;p=${query.start}">show as list</a>)
                    </c:when>
                    <c:otherwise>
                        list (<a href="${pageUrl}&amp;p=${query.start}&amp;view=hm">show as heatmap</a>)
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="drilldowns">
                <c:forEach var="ef" items="${result.efvFacet.valueSortedTrimmedTree}">
                    <div class="drillsect">
                        <div class="name"><fmt:message key="head.ef.${ef.ef}"/>:</div>
                        <ul><c:forEach var="efv" items="${ef.efvs}" varStatus="s">
                            <li><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;gexp_${cn}=UP_DOWN&amp;fval_${cn}_0=${u:escapeURL(efv.efv)}" class="ftot"><c:out value="${efv.efv}"/></a>&nbsp;(<c:if test="${efv.payload.up > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;gexp_${cn}=UP&amp;fval_${cn}_0=${u:escapeURL(efv.efv)}" class="fup"><c:out value="${efv.payload.up}"/>&#8593;</a></c:if><c:if test="${efv.payload.up > 0 && efv.payload.down > 0}">&nbsp;</c:if><c:if test="${efv.payload.down > 0}"><a href="${pageUrl}&amp;fact_${cn}=${u:escapeURL(ef.ef)}&amp;gexp_${cn}=DOWN&amp;fval_${cn}_0=${u:escapeURL(efv.efv)}" class="fdn"><c:out value="${efv.payload.down}"/>&#8595;</a></c:if>)</li>
                        </c:forEach></ul>
                    </div>
                </c:forEach>
                <c:if test="${!empty result.geneFacets['species']}">
                    <div class="drillsect">
                        <div class="name">Species:</div>
                        <ul>
                            <c:forEach var="sp" items="${result.geneFacets['species']}" varStatus="s">
                                <li><a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(sp.name)}" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <c:forEach var="facet" items="${result.geneFacets}">
                    <c:if test="${!empty facet.key && facet.key!='species'}">
                        <div class="drillsect">
                            <div class="name"><fmt:message key="head.gene.${facet.key}"/>:</div>
                            <ul>
                                <c:forEach var="sp" items="${facet.value}" varStatus="s">
                                    <li><a href="${pageUrl}&amp;gene_${gn}=${u:escapeURL(sp.name)}&amp;geneprop_${gn}=${u:escapeURL(facet.key)}&amp;geneoperator_${gn}=AND" class="ftot"><c:out value="${f:toUpperCase(f:substring(sp.name, 0, 1))}${f:toLowerCase(f:substring(sp.name, 1, -1))}"/></a>&nbsp;(<c:out value="${sp.count}"/>)</li>
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
                            <c:forEach var="c" items="${result.resultEfvs.nameSortedTree}" varStatus="i">
                                <th colspan="${f:length(c.efvs) * 2}" class="factor">
                                    <c:choose>
                                       <c:when test="${u:isInSet(query.expandColumns, c.ef)}">
                                          <a style="float:right;display:block;text-decoration:none;" title="Collapse factor values" href="${pageUrl}&amp;p=${result.start}">&nbsp;«««</a>
                                       </c:when>
                                       <c:when test="${u:isInSet(result.expandableEfs, c.ef)}">
                                          <a style="float:right;display:block;text-decoration:none;" title="Show more factor values..." href="${pageUrl}&amp;p=${result.start}&amp;fexp=${c.ef}">&nbsp;»»»</a>
                                       </c:when>
                                    </c:choose>
                                    <em style="color:#${i.index % 2 == 0 ? '000000':'999999'}"><c:out value="${c.ef}"/></em>
                                </th>
                            </c:forEach>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${result.results}" varStatus="i">
                            <tr id="squeryrow${i.index}">
                                <td class="geneinfo">
                                    <nobr>
                                        <c:set var="geneName" value="${f:split(row.gene.geneName,';')}"/>
                                        <a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}" title="${f:join(geneName, ', ')}"><b>${row.gene.hilitGeneName}</b><c:if test="${empty row.gene.geneName}">(none)</c:if></a>
                                        <span class="ensname">
                                            &nbsp;<c:url var="urlGeneAnnotation" value="http://www.ebi.ac.uk/ebisearch/search.ebi">
                                                <c:param name="db" value="genomes"/>
                                                <c:param name="t" value="${row.gene.geneIdentifier}"/>
                                            </c:url>
                                            <a title="Show gene annotation" target="_blank" href="${urlGeneAnnotation}"><c:out value="${row.gene.geneIdentifier}"/></a>
                                            <span class="label">&nbsp;Specie:</span>
                                            <a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(row.gene.geneSpecies)}"><c:out value="${row.gene.geneSpecies}"/></a>
                                        </span>
                                    </nobr><br />
                                    <nobr>
                                        <span class="label">GO:</span>
                                        <c:choose>
                                            <c:when test="${!empty row.gene.goTerm}">
                                                <span class="hitrunc" title="${f:escapeXml(row.gene.goTerm)}"><a href="javascript:alert('sorry, not implemented yet')">${row.gene.hilitGoTerm}</a></span>
                                            </c:when>
                                            <c:otherwise>(none)</c:otherwise>
                                        </c:choose>
                                        <span class="label"> InterPro:</span>
                                        <c:choose>
                                            <c:when test="${!empty row.gene.interProTerm}">
                                                <span class="hitrunc" title="${f:escapeXml(row.gene.interProTerm)}"><a href="javascript:alert('sorry, not implemented yet')">${row.gene.hilitInterProTerm}</a></span>
                                            </c:when>
                                            <c:otherwise>(none)</c:otherwise>
                                        </c:choose>
                                    </nobr>
                                </td>
                                <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                                    <c:choose>
                                        <c:when test="${ud.zero}">
                                            <td class="counter" colspan="2"></td>
                                        </c:when>
                                        <c:when test="${ud.ups == 0 && ud.downs > 0}">
                                            <td class="acounter" colspan="2" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                title="Click to view experiments. Average p-value is ${ud.mpvDn}"
                                                onclick="hmc(${i.index},${j.index},this)">${ud.downs}</td>
                                        </c:when>
                                        <c:when test="${ud.downs == 0 && ud.ups > 0}">
                                            <td class="acounter" colspan="2" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                title="Click to view experiments. Average p-value is ${ud.mpvUp}"
                                                onclick="hmc(${i.index},${j.index},this)">${ud.ups}</td>
                                        </c:when>
                                        <c:otherwise>
                                            <td class="ucounter" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                onclick="hmc(${i.index},${j.index},this)">${ud.ups}</td>
                                            <td class="dcounter" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                onclick="hmc(${i.index},${j.index},$(this).prev())">${ud.downs}</td>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <script type="text/javascript">drawEfvNames();</script>
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
                            <a href="${pageUrl}&amp;specie_${sn}=${u:escapeURL(row.gene.geneSpecies)}"><c:out value="${row.gene.geneSpecies}"/></a>
                            <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}">
                                <span class="label">&nbsp;&nbsp;GO Term:</span>
                                <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_goterm']}"/></a>
                            </c:if>
                            <c:if test="${!empty row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}">
                                <span class="label">&nbsp;&nbsp;InterPro Term:</span>
                                <a href="javascript:alert('sorry, not implemented yet')"><c:out value="${row.gene.geneSolrDocument.fieldValueMap['gene_interproterm']}"/></a>
                            </c:if></div>
                            <div class="efvs">
                                <c:forEach var="ef" items="${result.resultEfvs.nameSortedTree}">
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
                                                <th align="center" class="counter" colspan="2"><c:out value="${efv.efv}"/></th>
                                            </c:if>
                                        </c:forEach></tr>
                                        <tr><c:forEach var="efv" items="${ef.efvs}">
                                            <c:set var="ud" value="${row.counters[efv.payload]}"/>
                                            <c:if test="${!ud.zero}">
                                                <c:choose>
                                                    <c:when test="${ud.ups == 0 && ud.downs > 0}">
                                                        <td class="acounter" colspan="2" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                            title="Click to view experiments. Average p-value is ${ud.mpvDn}"
                                                            onclick="hmc(${i.index},${j.index},this)">${ud.downs}</td>
                                                    </c:when>
                                                    <c:when test="${ud.downs == 0 && ud.ups > 0}">
                                                        <td class="acounter" colspan="2" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                            title="Click to view experiments. Average p-value is ${ud.mpvUp}"
                                                            onclick="hmc(${i.index},${j.index},this)">${ud.ups}</td>
                                                    </c:when>
                                                    <c:when test="${!ud.zero}">
                                                        <td class="ucounter" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                            title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                            onclick="hmc(${i.index},${j.index},this)">${ud.ups}</td>
                                                        <td class="dcounter" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                            title="Click to view experiments. Average p-value is ${ud.ups != 0 ? ud.mpvUp : '-'} / ${ud.downs != 0 ? ud.mpvDn : '-'}"
                                                            onclick="hmc(${i.index},${j.index},$(this).prev())">${ud.downs}</td>
                                                    </c:when>
                                                </c:choose>
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
                        <a href="${pageUrl}&amp;p=${result.start - result.rows}">prev</a>
                    </c:if>
                    <c:forEach var="p" begin="0" end="${result.total}" step="${result.rows}">
                        <c:if test="${result.start == p}">
                            <b><fmt:formatNumber value="${(p / result.rows) + 1}" maxFractionDigits="0"/></b>
                        </c:if>
                        <c:if test="${result.start != p}">
                            <a href="${pageUrl}&amp;p=${p}"><fmt:formatNumber value="${(p / result.rows) + 1}" maxFractionDigits="0"/></a>
                        </c:if>
                    </c:forEach>
                    <c:if test="${result.total - result.start > result.rows}">
                        <a href="${pageUrl}&amp;p=${result.start + result.rows}">next</a>
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

