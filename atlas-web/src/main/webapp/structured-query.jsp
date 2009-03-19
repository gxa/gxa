<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
Search Results - ArrayExpress Atlas of Gene Expression
<jsp:include page="end_head.jsp"></jsp:include>

<link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="atlas.css" type="text/css" />

<script type="text/javascript" src="scripts/jquery.min.js"></script>
<script type="text/javascript" src="scripts/jquery.cookie.js"></script>
<script type="text/javascript" src="scripts/jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="scripts/jquery.tooltip.js"></script>
<script type="text/javascript" src="scripts/jquery.dimensions.js"></script>
<script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->
<script type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/common-query.js"></script>
<script type="text/javascript" src="scripts/structured-query.js"></script>
<script type="text/javascript" src="scripts/feedback.js"></script>

<style type="text/css">.contents{top: 87px}</style>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

        <div style="padding-left:15px; padding-right:15px;">
        <table style="position:relative; z-index:1; top:58px;border-bottom:1px solid #DEDEDE;width:100%;height:30px">
            <tr>
                <td align="left" valign="bottom" width="55" style="padding-right:10px;">
                   <a href="index.jsp" title="ArrayExpress Atlas Homepage"><img border="0" width="55" src="images/atlas-logo.png" alt="Atlas of Gene Expression"/></a>
                </td>
                <td align="right" valign="bottom">
                    <a href="./">home</a> |
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
                    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
                    <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> |
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
                </td>
            </tr>
        </table>        
	</div>

	<div class="contents" id="contents">
			<table class="contentspane" id="contentspane" summary="The main content pane of the page" style="width: 100%">
				<tr>
				  <td class="leftmargin"><img src="http://www.ebi.ac.uk/inc/images/spacer.gif" class="spacer" alt="spacer" /></td>
				  <td class="leftmenucell" id="leftmenucell"> 
					<div class="leftmenu" id="leftmenu" style="1px; visibility: hidden; display: none;">

<img src="http://www.ebi.ac.uk/inc/images/spacer.gif" class="spacer" alt="spacer" />
</div>
</td>
<td class="contentsarea" id="contentsarea">

<div id="ae_pagecontainer">
            <div id="topcontainer">
		    <form id="simpleform" class="visinsimple" action="qrs">
                <table style="width:850px">
                    <tr>
                        <td><label class="label" for="gene0">Genes</label></td>
                        <td></td>
                        <td>
                            <label class="label" for="species0">Organism</label>
                        </td>
                        <td>
                            <label class="label" for="fval0">Conditions</label>
                        </td>
                        <td></td>
                    </tr>
                    <tr>
                        <td>
                            <input type="hidden" name="gprop_0" id="gprop0" value="${query.simple ? f:escapeXml(query.geneQueries[0].factor) : ''}">
                            <input type="text" class="value" name="gval_0" id="gene0" style="width:200px" value="${query.simple ? f:escapeXml(query.geneQueries[0].jointFactorValues) : ''}" /><br>
                        </td>
                        <td>
                            <select name="fexp_0" id="expr0">
                                <c:forEach var="s"
                                           items="${service.structQueryService.geneExpressionOptions}">
                                    <option ${query.simple && s[0] == query.conditions[0].expression ? 'selected="selected"' : ''} value="${f:escapeXml(s[0])}">${f:escapeXml(s[1])} in</option>
                                </c:forEach>
                            </select>
                            <input type="hidden" name="fact_0" value="">
                        </td>
                        <td>
                            <select name="specie_0" id="species0" style="width:180px">
                                <option value="">(any)</option>
                                <c:forEach var="s"
                                           items="${service.allAvailableAtlasSpecies}">
                                    <option ${!empty query.species && f:toLowerCase(s) == f:toLowerCase(query.species[0]) ? 'selected="selected"' : ''} value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
                                </c:forEach>
                            </select>
                        </td>
                        <td>
                            <input type="text" class="value" name="fval_0" id="fval0" style="width:200px" value="${query.simple ? f:escapeXml(query.conditions[0].jointFactorValues) : ''}" />
                        </td>
                        <td align="right">
                            <input type="submit" value="Search Atlas" class="searchatlas">
		        </td>
                    </tr>
                    <tr>
			<td class="label" colspan="3"><span style="font-style: italic" class="label">e.g. ASPM, "p53 binding"</span></td>
			<td class="label"><span style="font-style: italic" class="label">e.g. liver, cancer, diabetes</span></td>
			<td valign="top" align="right"><a class="smallgreen" class="visinsimple" style="font-size:12px" href="javascript:atlas.structMode();">advanced search</a></td>
                    </tr>
                </table>
            <input type="hidden" name="view" value="hm" />
    </form>
    <form id="structform" class="visinstruct" name="atlasform" action="qrs" style="display:none">
		  <fieldset style="border:1px solid #DEDEDE;width: 850px">
		  <legend  style="padding-left:5px;padding-right:5px;color: black">Find genes matching all of the following conditions</legend>
               <table >
                    <tbody id="conditions">
		          <tr id="helprow"><td colspan="4"><em>Empty query</em></td></tr>
		    </tbody>
               </table>
	       </fieldset>
	       <fieldset style="border:1px solid #DEDEDE; margin-top:10px;width: 850px">
	       <legend  style="padding-left:5px;padding-right:5px;color: black">Add conditions to the query</legend>

               <div style="">
		    <table>
		      <tr>	
			<td colspan="4" ></td>
		      </tr>
		      <tr>
			<td>
			  <select id="geneprops" >
                            <option value="" selected="selected">Gene property</option>                         
			    <option value="">(any)</option>
			      <c:forEach var="i" items="${service.geneProperties}">
			    <option value="${f:escapeXml(i)}"><fmt:message key="head.gene.${i}"/></option>
			      </c:forEach>
			  </select>
			</td>
			<td>
			  <select id="factors" >
                            <option value="" selected="selected">Experimental factor</option>                         
			    <option value="">(any)</option>
			      <c:forEach var="i" items="${service.structQueryService.experimentalFactorOptions}">
			    <option value="${f:escapeXml(i)}"><fmt:message key="head.ef.${i}"/></option>
			      </c:forEach>
			  </select>
			</td>
			<td>
			  <select id="species">
                            <option value="" selected="selected">Organism</option>                         
			    <c:forEach var="i" items="${service.allAvailableAtlasSpecies}">
			    <option value="${f:escapeXml(i)}">${f:escapeXml(i)}</option>
			      </c:forEach>
			  </select>
			</td>
			<td align="right">
			  <input id="structclear" disabled="disabled" type="button" value="New Query" onclick="atlas.clearQuery();">
			  <input id="structsubmit" disabled="disabled" type="submit" value="Search Atlas" class="searchatlas">
			</td>
		      </tr>
		      <tr>
			<td colspan="4" align="right">
			  <a class="visinstruct smallgreen" style="font-size:12px" href="javascript:atlas.simpleMode();">simple search</a>
			  </td>
		    </table>
               </div>
		    </fieldset>
            <input type="hidden" name="view" value="hm" />
		</fieldset>
        </form>

        <c:if test="${result.hasEFOExpansion}">
	 <fieldset id="efotext" style="width:850px;margin-top:15px;margin-bottom:15px;padding:0px">
            <img src="images/expp.gif" id="efotoggle" onclick="$('#efoexpand').toggle();this.src=this.src.indexOf('expp')>=0?'images/expm.gif':'images/expp.gif';">
            Your query was expanded via <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO">EFO</a>, an ontology of experimental variables developed by ArrayExpress Production Team
            <table id="efoexpand">
                <c:forEach var="c" varStatus="s" items="${result.conditions}">
                    <c:forEach var="ef" items="${c.expansion.nameSortedTree}">
                        <tr>
                            <th width="150"><fmt:message key="head.ef.${ef.ef}"/></th>
                            <td><c:forEach var="efv" items="${ef.efvs}" varStatus="s"><c:out value="${efv.efv}"/><c:if test="${!s.last}">, </c:if></c:forEach></td>
                        </tr>
                    </c:forEach>
                </c:forEach>
            </table>
          </fieldset>
	</c:if>
        <c:forEach var="c" varStatus="s" items="${result.conditions}">
            <c:if test="${c.ignored}"><fieldset class="ignoretext top">
                <span class="ignored">Ignoring condition &quot;<b><fmt:message key="head.ef.${c.anyFactor ? 'anything' : c.factor}"/></b> matching <b><c:out value="${c.jointFactorValues}" /></b>&quot; as no matching factor values were found</span>
            </fieldset></c:if>
        </c:forEach>
   </div>

    <script type="text/javascript">
        var options = {
            expressions : [
                    <c:forEach var="i" varStatus="s" items="${service.structQueryService.geneExpressionOptions}">
                    [ '${u:escapeJS(i[0])}', 'is ${u:escapeJS(i[1])} in' ]<c:if test="${!s.last}">,</c:if>
                    </c:forEach>
            ],
            species : [
                <c:forEach var="i" varStatus="s" items="${service.allAvailableAtlasSpecies}">
                '${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if>
                </c:forEach>
            ]
        };

        var lastquery = null;
        <c:if test="${!query.none}">
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
                            '<span class="ignored">no matching experiments were found, ignored</span>'
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
            atlas.initStructForm(lastquery);

            $('#simpleform, #structform').css('display', '');
            $('.visin${(query.none && !forcestruct) || (!query.none && query.simple) ? 'struct' : 'simple'}').hide();
        });
        var preloads = [ 'expp.gif', 'expm.gif', 'indicator.gif' ]; var img = [];
        for(var i = 0; i < preloads.length; ++i) {
            img[i] = new Image(); img[i].src = 'images/' + preloads[i];
        }
    </script>

    <c:if test="${!query.none}">
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
                    opts.num_display_entries = 5;
                    $(".page_long").pagination(${result.total}, opts);
                });
            </script>
            </c:if>

            <table id="twocol" style="margin-top:20px">
	      <tr class="top">
                <c:if test="${result.total >= u:getIntProp('atlas.drilldowns.mingenes')}">
                    <td id="drilldowns">
		     <c:if test="${result.size > 0}">
		    <div id="summary" style="font-size:11px">
		      <b>REFINE YOUR QUERY</b>
		    </div>
		    </c:if>
		    <div id="drill" style="padding:0px;">
                           <c:forEach var="ef" items="${result.efvFacet.valueSortedTrimmedTree}">
                                <div class="drillsect">
                                    <c:set var="efname"><fmt:message key="head.ef.${ef.ef}"/></c:set>
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
                                        <c:set var="gpname"><fmt:message key="head.gene.${facet.key}"/></c:set>
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

                <c:if test="${true || heatmap}">
                   <td id="resultpane">

		     <c:if test="${result.size > 0}">
            <div id="summary">
                <span id="pagetop" class="pagination_ie page_long"></span>
                Genes <c:out value="${result.page * result.rowsPerPage == 0 ? 1 : result.page * result.rowsPerPage}"/>-<c:out value="${(result.page + 1) * result.rowsPerPage > result.total ? result.total : (result.page + 1) * result.rowsPerPage }"/> of <b><c:out value="${result.total}" /></b> total found
            </div>
        </c:if>
                        <table id="squery">
                            <tbody>
                            <tr class="header">
                                <th class="padded" rowspan="2">Gene</th>
                                <c:if test="${f:length(query.species) != 1}">
                                    <th class="padded" rowspan="2">Organism</th>
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
                                                <a title="Collapse factor values for ${eftitle}" href="${pageUrl}&amp;p=${result.page}">&#0171;&nbsp;fewer</a>
                                            </c:when>
                                            <c:when test="${u:isInSet(result.expandableEfs, c.ef)}">
                                                <a title="Show more factor values for ${eftitle}..." href="${pageUrl}&amp;p=${result.page}&amp;fexp=${c.ef}">more&nbsp;&#0187;</a>
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
                                            <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:if test="${!empty row.gene.synonyms}">${row.gene.hilitSynonyms},</c:if>${row.gene.geneIdentifier})</div>
                                            <c:if test="${!empty row.gene.keyword}"><b>Keyword:</b> ${row.gene.hilitKeyword}<br></c:if>
                                            <c:if test="${!empty row.gene.goTerm}"><b>Go Term:</b> ${row.gene.hilitGoTerm}<br></c:if>
                                            <c:if test="${!empty row.gene.interProTerm}"><b>InterPro Term:</b> ${row.gene.hilitInterProTerm}<br></c:if>
                                        </div>
                                    </td>
                                    <c:if test="${f:length(query.species) != 1}">
                                        <td class="padded"><c:out value="${row.gene.geneSpecies}"/></td>
                                    </c:if>
                                    <c:set var="first" value="true" />
                                    <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                                        <c:set var="ud" value="${row.counters[e.payload]}"/>
                                        <c:choose>
                                            <c:when test="${ud.zero}">
                                                <td class="counter"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                                            </c:when>
                                            <c:when test="${ud.ups == 0 && ud.downs > 0}">
                                                <td class="acounter" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                                    title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s). Click to view..."
                                                    onclick="atlas.hmc(${i.index},${j.index},event)"><div class="osq">${ud.downs}</div></td>
                                            </c:when>
                                            <c:when test="${ud.downs == 0 && ud.ups > 0}">
                                                <td class="acounter" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                                    title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s). Click to view..."
                                                    onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.ups}</div></td>
                                            </c:when>
                                            <c:otherwise>
                                                <td class="acounter"
                                                    title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                                    onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="tri" style="border-right-color:${u:expressionBack(ud,-1)};border-top-color:${u:expressionBack(ud,1)};"></div><div style="color:${u:expressionText(ud,-1)}" class="dnval">${ud.downs}</div><div style="color:${u:expressionText(ud,1)}" class="upval">${ud.ups}</div></div></td>
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

