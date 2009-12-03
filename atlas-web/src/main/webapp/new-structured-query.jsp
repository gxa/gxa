<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
Gene Expression Atlas Search Results - Gene Expression Atlas
<jsp:include page="end_head.jsp"></jsp:include>

<jsp:include page="query-includes.jsp" />

<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />

<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="scripts/jquery.flot.atlas.js"></script>
<script type="text/javascript" src="scripts/plots.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/common-query.js"></script>
<script type="text/javascript" src="scripts/structured-query.js"></script>
<script type="text/javascript" src="scripts/jquery.tablesorter.mod.js"></script>
<!-- >script type="text/javascript" src="scripts/jquery.tablesorter.pager.js"></script-->
<script type="text/javascript" src="scripts/jquery.tablesorter.collapsible.js"></script>
<script type="text/javascript" src="scripts/pure.js"></script>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right:10px;">
            <a href="<%=request.getContextPath()%>/" title="Gene Expression Atlas Homepage"><img border="0" width="55" src="<%= request.getContextPath()%>/images/atlas-logo.png" alt="Gene Expression Atlas"/></a>
        </td>
        <td align="right" valign="bottom">
            <a href="<%=request.getContextPath()%>/">home</a> |
            <a href="<%=request.getContextPath()%>/help/AboutAtlas">about the project</a> |
            <a href="<%=request.getContextPath()%>/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="<%=request.getContextPath()%>/help/AtlasDasSource">das</a> |
	    <a href="<%=request.getContextPath()%>/help/AtlasApis">api</a> <b>new</b> |
            <a href="<%=request.getContextPath()%>/help">help</a>
        </td>
    </tr>
</table>

<c:set var="simpleformvisible" value="${(query.none && !forcestruct) || (!query.none && query.simple)}" />
<div id="topcontainer">
    <form id="simpleform" action="qrs" style="display:${simpleformvisible ? 'inherit' : 'none'}">
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
                <td>
                    <label class="label" for="view">View</label>
                </td>
                <td></td>
            </tr>
            <tr>
                <td>
                    <input type="hidden" name="gprop_0" id="gprop0" value="${query.simple ? f:escapeXml(query.geneConditions[0].factor) : ''}">
                    <input type="text" class="value" name="gval_0" id="gene0" style="width:200px" value="${query.simple ? f:escapeXml(query.geneConditions[0].jointFactorValues) : ''}" /><br>
                </td>
                <td>
                    <select name="fexp_0" id="expr0">
                        <c:forEach var="s"
                                   items="${service.atlasQueryService.geneExpressionOptions}">
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
                <td rowspan="2" class="label" nowrap="nowrap" style="vertical-align: top;">
                    <c:if test="${heatmap}">
                        <input type="radio" id="view_hm" name="view" style="vertical-align:bottom" value="hm" checked="checked"><label for="view_hm">Heatmap</label><br>
                        <input type="radio" id="view_ls" name="view" style="vertical-align:bottom" value="list"><label for="view_ls">List</label>
                    </c:if>
                    <c:if test="${list}">
                        <input type="radio" id="view_hm" name="view" style="vertical-align:bottom" value="hm"><label for="view_hm">Heatmap</label><br>
                        <input type="radio" id="view_ls" name="view" style="vertical-align:bottom" value="list" checked="checked"><label for="view_ls">List</label>
                    </c:if>
                </td>
                <td align="right">
                    <input type="submit" value="Search Atlas" class="searchatlas">
                </td>
            </tr>
            <tr>
                <td class="label" colspan="3"><span style="font-style: italic" class="label">e.g. ASPM, "p53 binding"</span></td>
                <td class="label"><span style="font-style: italic" class="label">e.g. liver, cancer, diabetes</span></td>
                <td valign="top" align="right" nowrap="nowrap"><a class="smallgreen" style="font-size:12px" href="javascript:atlas.structMode();">advanced search</a></td>
            </tr>
        </table>
    </form>
    <form id="structform" name="atlasform" action="nqrs" style="display:${simpleformvisible ? 'none' : 'inherit'}">
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
                <table cellspacing="0">
                    <tr>
                        <td colspan="3" ></td>
                        <td><label class="label" for="view">View</label></td>
                        <td></td>
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
                                <option value="efo">EFO</option>
                                <c:forEach var="i" items="${service.atlasQueryService.experimentalFactorOptions}">
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
                        <td class="label" nowrap="nowrap">
                            <c:if test="${heatmap}">
                                <input type="radio" id="view" name="view" style="vertical-align:bottom" value="hm" checked="checked">Heatmap<br>
                                <input type="radio" name="view" style="vertical-align:bottom" value="list">List
                            </c:if>
                            <c:if test="${list}">
                                <input type="radio" id="view" name="view" style="vertical-align:bottom" value="hm">Heatmap<br>
                                <input type="radio" name="view" style="vertical-align:bottom" value="list" checked="checked">List
                            </c:if>
                        </td>
                        <td align="right">
                            <input id="structclear" disabled="disabled" type="button" value="New Query" onclick="atlas.clearQuery();">
                            <input id="structsubmit" disabled="disabled" type="submit" value="Search Atlas" class="searchatlas">
                        </td>
                    </tr>
                    <tr>
                        <td colspan="5" align="right">
                            <a class="smallgreen" style="font-size:12px" href="javascript:atlas.simpleMode();">simple search</a>
                        </td>
                </table>
            </div>
        </fieldset>
    </form>

</div><!-- /id=topcontainer -->

<script type="text/javascript">
    var options = {
        expressions : [
            <c:forEach var="i" varStatus="s" items="${service.atlasQueryService.geneExpressionOptions}">
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
        genes: [ <c:forEach var="g" varStatus="s" items="${query.geneConditions}">
            { query: '${g.jointFactorValues}', property:'${g.factor}', not: ${g.negated ? 1 : 0} }<c:if test="${!s.last}">,</c:if>
            </c:forEach>
        ],
        species : [<c:forEach var="i" varStatus="s" items="${query.species}">'${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if></c:forEach>],
        conditions : [
        ],
        view: '${heatmap ? 'hm' : 'list'}'
    };
    </c:if>
    $(document).ready(function () {
        atlas.initStructForm(lastquery);

        $(".tablesorter").collapsible("td.collapsible", {
            collapse: true,
            callback: showExps
        })
          .tablesorter({
            // don't sort by first column
            headers: {0: {sorter: false}}
            // set the widgets being used - zebra stripping
            , widgets: ['zebra']
            , onRenderHeader: function (){
                this.wrapInner("<span></span>");
            }
            , debug: false
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
    var resultGenes = [
            <c:forEach var="row" varStatus="s" items="${result.results}">{ geneDwId: '${u:escapeJS(row.first.id)}', geneAtlasId: '${u:escapeJS(row.first.id)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
    ];

    var resultEfvs = [
            <c:forEach var="e" varStatus="s" items="${result.resultEfvs.nameSortedList}">{ ef: '${u:escapeJS(e.ef)}', efv: '${u:escapeJS(e.efv)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
    ];
</script>

<c:if test="${result.size > 0}">
<c:url var="pageUrl" value="/nqrs">
    <c:forEach var="g" varStatus="gs"items="${query.geneConditions}">
        <c:param name="gnot_${gs.index}" value="${g.negated ? '1' : ''}" />
        <c:param name="gval_${gs.index}" value="${g.jointFactorValues}" />
        <c:param name="gprop_${gs.index}" value="${g.factor}" />
    </c:forEach>
    <c:forEach var="i" varStatus="s" items="${query.species}"><c:param name="specie_${s.index}" value="${i}"/></c:forEach>
    <c:forEach varStatus="cs" var="c" items="${query.conditions}">
        <c:param name="fact_${cs.index}" value="${c.factor}"/>
        <c:param name="fexp_${cs.index}" value="${c.expression}"/>
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
			        atlas.popup('downloads.jsp');
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
<c:if test="${result.total >= u:getIntProp('atlas.drilldowns.mingenes')}">
    <td id="drilldowns">
        <div style="font-size:11px">
            <b>REFINE YOUR QUERY</b>
        </div>
        <div id="drill" style="padding:0px;">
        </div>
    </td>
</c:if>


<td id="resultpane" width="900px">
<div id="summary">
    <span id="pagetop" class="pagination_ie page_long"></span>
    Genes <c:out value="${result.page * result.rowsPerPage == 0 ? 1 : result.page * result.rowsPerPage}"/>-<c:out value="${(result.page + 1) * result.rowsPerPage > result.total ? result.total : (result.page + 1) * result.rowsPerPage }"/> of <b><c:out value="${result.total}" /></b> total found
    <c:if test="${result.total >= u:getIntProp('atlas.drilldowns.mingenes')}">
        <span>(you can <a href="#" onclick="$('#drilldowns').animate({width:'show'});$(this).parent().remove();return false;">refine your query</a>)</span>
    </c:if>
    &nbsp;•&nbsp;
    <a class="export_lnk" title="Download results in a tab-delimited format." href="#" >Download all results</a>
    <span style="display:${noDownloads > 0 ? 'inline' : 'none' };">- <span id="dwnldCounter">${noDownloads}</span> download(s) <a href="javascript:void(0)" onclick="atlas.popup('downloads.jsp')">in progress</a></span>
    &nbsp;•&nbsp; <c:import url="apilinks.jsp"><c:param name="apiUrl" value="${query.apiUrl}"/></c:import>
</div>
<div id="legendexpand" style="width:100%;height:30px">

    <div style="line-height:30px;white-space:nowrap">Legend: <img style="position:relative;top:6px" src="images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">over</span>/<span style="color:blue;font-weight:bold">under</span> expressed in</div>
</div>

<c:choose>
<c:when test="${heatmap}">
    <c:if test="${result.resultEfvs.numEfvs > 0}">
        <map id="efvmap" name="efvmap">
            <c:forEach var="i" items="${result.resultEfvs.nameSortedList}" varStatus="s">
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
            <c:if test="${result.resultEfvs.numEfvs > 0}">
                <c:set scope="session" var="resultEfvs" value="${result.resultEfvs.nameSortedList}" />
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
                <td colspan="${result.resultEfvs.numEfvs}"><div style="width:${result.resultEfvs.numEfvs * 27 - 1}px;" class="diaghead">Keywords</div><div style="position:relative;height:150px;"><div style="position:absolute;bottom:0;left:-1px;"><img onload="fixpng(this);" src="${efoImgUrl}" usemap="#efvmap" alt=""></div></div></td>
            </c:if>
        </tr>
        <tr>
            <c:forEach var="c" items="${result.resultEfvs.nameSortedTree}" varStatus="i">
                <c:set var="eftitle"><fmt:message key="head.ef.${c.ef}"/></c:set>
                <th colspan="${f:length(c.efvs)}" class="factor" title="${eftitle}">
                    <div style="width:${f:length(c.efvs) * 27 - 1}px;">${eftitle}</div>
                </th>
            </c:forEach>
        </tr>
        <c:forEach var="row" items="${result.results}" varStatus="i">
            <tr id="squeryrow${i.index}">
                <td class="padded genename">
                    <a href="gene/${f:escapeXml(row.first.id)}">${f:escapeXml(row.first.name)}<c:if test="${empty row.first.name}"><c:out value="${row.first.id}"/></c:if></a>
                    <div class="gtooltip">
                        <div class="genename"><b>${f:escapeXml(row.first.name)}</b> (<c:if test="${!empty row.first.synonym}">${f:escapeXml(row.first.synonym)},</c:if>${row.first.id})</div>
                        <c:if test="${!empty row.first.keyword}"><b>Keyword:</b> ${f:escapeXml(row.first.keyword)}<br></c:if>
                        <c:if test="${!empty row.first.goterm}"><b>Go Term:</b> ${f:escapeXml(row.first.goterm)}<br></c:if>
                        <c:if test="${!empty row.first.interproterm}"><b>InterPro Term:</b> ${f:escapeXml(row.first.interproterm)}<br></c:if>
                    </div>
                </td>
                <c:if test="${f:length(query.species) != 1}">
                    <td class="padded"><c:out value="${row.first.species}"/></td>
                </c:if>
                <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                    <c:set var="ud" value="${row.second[e]}"/>
                    <c:choose>
                        <c:when test="${empty ud || ud.upExperimentsCount + ud.dnExperimentsCount == 0}">
                            <td class="counter"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                        </c:when>
                        <c:when test="${ud.upExperimentsCount == 0 && ud.dnExperimentsCount > 0}">
                            <td class="acounter" style="background-color:${u:expressionBackNew(ud,-1)};color:${u:expressionTextNew(ud,-1)}"
                                title="${f:escapeXml(empty row.first.name ? row.first.id : row.first.name)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.dnExperimentsCount} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event)"><div class="osq">${ud.dnExperimentsCount}</div></td>
                        </c:when>
                        <c:when test="${ud.dnExperimentsCount == 0 && ud.upExperimentsCount > 0}">
                            <td class="acounter" style="background-color:${u:expressionBackNew(ud,1)};color:${u:expressionTextNew(ud,1)}"
                                title="${f:escapeXml(empty row.first.name ? row.first.id : row.first.name)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.upExperimentsCount} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="osq">${ud.upExperimentsCount}</div></td>
                        </c:when>
                        <c:otherwise>
                            <td class="acounter"
                                title="${f:escapeXml(empty row.first.name ? row.first.id : row.first.name)} in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.upExperimentsCount} and underexpressed in ${ud.dnExperimentsCount} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},${j.index},event || window.event)"><div class="sq"><div class="tri" style="border-right-color:${u:expressionBackNew(ud,-1)};border-top-color:${u:expressionBackNew(ud,1)};"></div><div style="color:${u:expressionTextNew(ud,-1)}" class="dnval">${ud.dnExperimentsCount}</div><div style="color:${u:expressionTextNew(ud,1)}" class="upval">${ud.upExperimentsCount}</div></div></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</c:when>
<c:otherwise>

    <!--- list results go here --->
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

</div><!-- /id="ae_pagecontainer" -->
</div><!-- /id="contents" -->

<div style="display:none">
    <div id="experimentsTemplate">
        <div class="head">
            <a href="gene?gid=1"><b class="gname">Name</b></a> in <b class="efv">efv</b> (<span class="ef">ef</span>)<br>
            overexpressed in <b class="numup">1</b> and underexpressed in <b class="numdn">1</b> experiments(s)
        </div>
        <div class="exptable">
            <table>
                <tr class="experRows">
                    <td class="explot">
                        <b class="expaccession">E-ACC</b>:
                        <span class="expname"></span>

                        <table class="oneplot" border="0" cellpadding="0" cellspacing="0"><tr>
                            <td><img src="images/indicator.gif" class="waiter" border="0" alt="Loading..."><a class="proflink" title="Show expression profile" href="" style="border:none;outline:none;text-decoration:none"><div style="display:none" class="plot"></div></a></td>
                            <td><div style="display:none" class="efname"></div><div class="legend"></div></td>
                        </tr></table>

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
</div>

<jsp:include page="end_body.jsp"></jsp:include>

