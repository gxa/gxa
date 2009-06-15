<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
Atlas Search Results - ArrayExpress Atlas of Gene Expression
<jsp:include page="end_head.jsp"></jsp:include>

<!-- <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" /> -->
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />

<script type="text/javascript" src="scripts/jquery.min.js"></script>
<script type="text/javascript" src="scripts/jquery.cookie.js"></script>
<script type="text/javascript" src="scripts/jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="scripts/jquery.tooltip.js"></script>
<script type="text/javascript" src="scripts/jquery.dimensions.js"></script>
<script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->
<script type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="scripts/plots.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/common-query.js"></script>
<script type="text/javascript" src="scripts/structured-query.js"></script>
<script type="text/javascript" src="scripts/feedback.js"></script>
<script type="text/javascript" src="scripts/jquery.tablesorter.mod.js"></script>
<!-- >script type="text/javascript" src="scripts/jquery.tablesorter.pager.js"></script-->
<script type="text/javascript" src="scripts/jquery.tablesorter.collapsible.js"></script>
<script type="text/javascript" src="scripts/pure.js"></script>

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
                <td valign="top" align="right" nowrap="nowrap"><a class="smallgreen" class="visinsimple" style="font-size:12px" href="javascript:atlas.structMode();">advanced search</a></td>
            </tr>
        </table>
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
                            <a class="visinstruct smallgreen" style="font-size:12px" href="javascript:atlas.simpleMode();">simple search</a>
                        </td>
                </table>
            </div>
        </fieldset>
        </fieldset>
    </form>

    <c:forEach var="c" varStatus="s" items="${result.conditions}">
        <c:if test="${c.ignored}"><fieldset class="ignoretext top">
            <span class="ignored">Ignoring condition &quot;<b><fmt:message key="head.ef.${c.anyFactor ? 'anything' : c.factor}"/></b> matching <b><c:out value="${c.jointFactorValues}" /></b>&quot; as no matching factor values were found</span>
        </fieldset></c:if>
    </c:forEach>
</div><!-- /id=topcontainer -->

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
        genes: [ <c:forEach var="g" varStatus="s" items="${query.geneConditions}">
            { query: '${g.jointFactorValues}', property:'${g.factor}', not: ${g.negated ? 1 : 0} }<c:if test="${!s.last}">,</c:if>
            </c:forEach>
        ],
        species : [<c:forEach var="i" varStatus="s" items="${query.species}">'${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if></c:forEach>],
        conditions : [
            <c:forEach var="c" varStatus="s" items="${result.conditions}">
            { factor: '${u:escapeJS(c.factor)}',
                expression: '${u:escapeJS(c.expression)}',
                values: '${u:escapeJS(c.jointFactorValues)}'
            }<c:if test="${!s.last}">,</c:if></c:forEach>
        ]
    };
    </c:if>
    $(document).ready(function () {
        atlas.initStructForm(lastquery);

        $('#simpleform, #structform').css('display', '');
        $('.visin${(query.none && !forcestruct) || (!query.none && query.simple) ? 'struct' : 'simple'}').hide();

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

        <c:if test="${list}">
        var cont = $('#resultpane');
	//	if(cont.get(0).offsetWidth > 900)
        //    cont.css({ width: '900px' });
        </c:if>

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
            <c:forEach var="row" varStatus="s" items="${result.results}">{ geneDwId: '${u:escapeJS(row.gene.geneIdentifier)}', geneAtlasId: '${u:escapeJS(row.gene.geneId)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
    ];

    var resultEfvs = [
            <c:forEach var="e" varStatus="s" items="${result.resultEfvs.nameSortedList}">{ ef: '${u:escapeJS(e.ef)}', efv: '${u:escapeJS(e.efv)}' }<c:if test="${!s.last}">,</c:if></c:forEach>
    ];
</script>

<c:if test="${result.size > 0}">
<c:url var="pageUrl" value="/qrs">
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

        $(".export_lnk").bind("click",function(){
       	var count = parseInt($("#dwnldCounter").text())+1;
       	$("#dwnldCounter").text(count);
            $.ajax({
                url: '${pageUrl}&export=true',
                cache: false,
                complete: function() {
                    // unblock when remote call returns
                    //$.blockUI({ message: $('#question'), css: { width: '275px' } });
			        atlas.popup('downloads.jsp');           
                }
            });
                    	
            
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


<td id="resultpane" width="900px">

<div id="summary">
    <span id="pagetop" class="pagination_ie page_long"></span>
    Genes <c:out value="${result.page * result.rowsPerPage == 0 ? 1 : result.page * result.rowsPerPage}"/>-<c:out value="${(result.page + 1) * result.rowsPerPage > result.total ? result.total : (result.page + 1) * result.rowsPerPage }"/> of <b><c:out value="${result.total}" /></b> total found
    <c:if test="${result.total >= u:getIntProp('atlas.drilldowns.mingenes')}">
        <span>(you can <a href="#" onclick="$('#drilldowns').animate({width:'show'});$(this).parent().remove();return false;">refine your query</a>)</span>
    </c:if>
</div>
<div id="legendexpand" style="width:100%;height:30px">
    <c:if test="${list}">
        <div style="line-height:30px;float:right" id="downloads"><a class="export_lnk" title="Download results in a tab-delimited format." href="javascript:void(0)" >Download all results</a> - <span id="dwnldCounter">${noDownloads}</span> download(s) <a href="javascript:void(0)" onclick="atlas.popup('downloads.jsp')">in progress</a></div>
    </c:if>
    <div style="line-height:30px;white-space:nowrap">Legend: <img style="position:relative;top:6px" src="images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">over</span>/<span style="color:blue;font-weight:bold">under</span> expressed in</div>
</div>

<c:choose>
<c:when test="${heatmap}">
    <table id="squery">
        <tbody>
        <tr class="header">
            <c:set var="efoSubTree" value="${result.resultEfos.markedSubTreeList}" />
            <c:set var="efoSubTreeLength" value="${f:length(efoSubTree)}" />
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
                    <c:param name="s" value="27" />
                    <c:param name="fs" value="11" />
                    <c:param name="h" value="150" />
                    <c:param name="lh" value="15" />
                    <c:param name="lc" value="#cdcdcd" />
                    <c:param name="tc" value="#000000" />
                    <c:param name="tlc" value="#000000" />
                    <c:param name="tds" value="4" />
                    <c:param name="tsx" value="7" />
                    <c:param name="tsy" value="5" />
                </c:url>
                <map name="efomap">
                    <c:set var="efohgt" value="${150 - 9 - 4 * u:max(efoSubTree, 'getDepth')}"/>
                    <c:forEach var="i" items="${efoSubTree}" varStatus="s">
                        <area alt="${f:escapeXml(i.term)}" title="${f:escapeXml(i.term)}" shape="poly" coords="${s.index*27},${efohgt - 20},${s.index*27+efohgt-20},0,${s.index*27+efohgt+17},0,${s.index*27+17},${efohgt-1},${s.index*27},${efohgt-1},${s.index*27},${efohgt - 20}" onclick="return false;">
                    </c:forEach>
                </map>
                <td colspan="${efoSubTreeLength}" class="${result.resultEfvs.numEfvs > 0 ? 'divider' : 'nope'}"><div style="width:${efoSubTreeLength * 27 - 1}px;" class="diaghead">Ontology</div><div style="position:relative;height:150px;"><div style="position:absolute;bottom:0;left:-1px;"><img src="${efoImgUrl}" usemap="#efomap"></div></div></td>
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
                    <c:param name="lc" value="#cdcdcd" />
                    <c:param name="tc" value="#000000" />
                </c:url>
                <map name="efvmap">
                    <c:forEach var="i" items="${result.resultEfvs.nameSortedList}" varStatus="s">
                        <c:set var="efvhgt" value="150"/>
                        <area alt="${f:escapeXml(i.efv)}" title="${f:escapeXml(i.efv)}" shape="poly" coords="${s.index*27},${efvhgt-20},${s.index*27+efvhgt-20},0,${s.index*27+efvhgt-1+17},0,${s.index*27+17},${efvhgt-1},${s.index*27},${efvhgt-1},${s.index*27},${efvhgt-20}" onclick="return false;">
                    </c:forEach>
                </map>
                <td colspan="${result.resultEfvs.numEfvs}"><div style="width:${result.resultEfvs.numEfvs * 27 - 1}px;" class="diaghead">Keywords</div><div style="position:relative;height:150px;"><div style="position:absolute;bottom:0;left:-1px;"><img src="${efoImgUrl}" usemap="#efvmap"></div></div></td>
            </c:if>
        </tr>
        <tr>
            <c:if test="${efoSubTreeLength > 0}">
                <th colspan="${efoSubTreeLength}" class="${result.resultEfvs.numEfvs > 0 ? 'divider' : 'nope'}">&nbsp;</th>
            </c:if>
            <c:forEach var="c" items="${result.resultEfvs.nameSortedTree}" varStatus="i">
                <c:set var="eftitle"><fmt:message key="head.ef.${c.ef}"/></c:set>
                <th colspan="${f:length(c.efvs)}" class="factor" title="${eftitle}">
                    <div style="width:${f:length(c.efvs) * 27 - 1}px;">${eftitle}</div>
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
                <c:forEach var="e" items="${efoSubTree}" varStatus="j">
                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                    <c:choose>
                        <c:when test="${empty ud || ud.zero}">
                            <td class="counter${j.last && result.resultEfvs.numEfvs > 0 ? ' divider' : ''}"><c:choose><c:when test="${j.first}"><div class="osq"></div></c:when></c:choose></td>
                        </c:when>
                        <c:when test="${ud.ups == 0 && ud.downs > 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfvs > 0 ? ' divider' : ''}" style="background-color:${u:expressionBack(ud,-1)};color:${u:expressionText(ud,-1)}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} is underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event)"><div class="osq">${ud.downs}</div></td>
                        </c:when>
                        <c:when test="${ud.downs == 0 && ud.ups > 0}">
                            <td class="acounter${j.last && result.resultEfvs.numEfvs > 0 ? ' divider' : ''}" style="background-color:${u:expressionBack(ud,1)};color:${u:expressionText(ud,1)}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} is overexpressed in ${ud.ups} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="osq">${ud.ups}</div></td>
                        </c:when>
                        <c:otherwise>
                            <td class="acounter${j.last && result.resultEfvs.numEfvs > 0 ? ' divider' : ''}"
                                title="${f:escapeXml(empty row.gene.geneName ? row.gene.geneIdentifier : row.gene.geneName)} in ${f:escapeXml(e.term)} overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s). Click to view..."
                                onclick="atlas.hmc(${i.index},'${e.id}',event || window.event)"><div class="sq"><div class="tri" style="border-right-color:${u:expressionBack(ud,-1)};border-top-color:${u:expressionBack(ud,1)};"></div><div style="color:${u:expressionText(ud,-1)}" class="dnval">${ud.downs}</div><div style="color:${u:expressionText(ud,1)}" class="upval">${ud.ups}</div></div></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
                <c:forEach var="e" items="${result.resultEfvs.nameSortedList}" varStatus="j">
                    <c:set var="ud" value="${row.counters[e.payload]}"/>
                    <c:choose>
                        <c:when test="${empty ud || ud.zero}">
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

</c:when>
<c:otherwise>

    <table id="squery" class="tablesorter" style="width:100%;table-layout:fixed;">
        <colgroup>
            <col style="width:20px" />
            <col style="width:110px" />
            <col style="width:150px" />
            <col style="width:180px"  />
            <col style="width:100%" />
            <col style="width:26px" />
            <col style="width:80px" />
        </colgroup>

        <thead>
            <tr class="header">
                <th style="border-right:none"></th>
                <th style="border-left:none" class="padded">Gene</th>
                <th class="padded">Organism</th>
                <th class="padded">Experimental Factor</th>
                <th class="padded">Factor Value</th>
                <th></th>
                <th class="padded">P-value</th>
            </tr>
        </thead>
					
        <tbody>
						
        <c:forEach var="row" items="${result.listResults}" varStatus="r">
            <tr id="${row.gene_id}_${row.ef}_${r.index}">
                <td rowspan="${f:length(row.exp_list)+3}" class="collapsible" style="border-right:none" title="Click here to show experiments..."></td>
                <td class="padded genename" style="border-left:none">
                    <a href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}">${row.gene_name}</a>
                    <div class="gtooltip">
                        <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:if test="${!empty row.gene.synonyms}">${row.gene.hilitSynonyms},</c:if>${row.gene.geneIdentifier})</div>
                        <c:if test="${!empty row.gene.keyword}"><b>Keyword:</b> ${row.gene.hilitKeyword}<br></c:if>
                        <c:if test="${!empty row.gene.goTerm}"><b>Go Term:</b> ${row.gene.hilitGoTerm}<br></c:if>
                        <c:if test="${!empty row.gene.interProTerm}"><b>InterPro Term:</b> ${row.gene.hilitInterProTerm}<br></c:if>
                    </div>
                </td>
                <td class="padded wrapok">${row.gene_species}</td>
                <td class="padded wrapok"><fmt:message key="head.ef.${row.ef}"/></td>
                <td class="padded wrapok lvrowefv">${row.fv}</td>
                <td class="acounter">
                    <c:choose>
                        <c:when test="${row.mixedCell}">
                                <div class="sq"><div class="tri" style="border-right-color:${row.cellColor['dn']};border-top-color:${row.cellColor['up']}"></div>
                                    <div style="color:${row.cellText['dn']}" class="dnval">${row.count_dn}</div>
                                    <div style="color:${row.cellText['up']}" class="upval">${row.count_up}</div></div>
                        </c:when>
                        <c:otherwise>
                                <div class="osq" style="background-color:${row.cellColor[row.expr]};color:${row.cellText[row.expr]}"> <c:if test="${row.count_dn!=0}"> <c:out value="${row.count_dn}"></c:out> </c:if>
                                    <c:if test="${row.count_up!=0}"> <c:out value="${row.count_up}"></c:out> </c:if> </div>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td class="padded"><fmt:formatNumber value="${row.minPval}" pattern="#.##E0" /></td>
            </tr>
            <tr class="expand-child">
              	<th colspan="6" class="header padded" style="text-align: left;padding:2px 0 0 0;background-color:#cdcdcd"></th>
            </tr>
            <c:forEach var="exp" items="${row.exp_list}">

                <tr class="expand-child">

                    <td class="padded genename" style="border-left: 4px solid #cdcdcd; border-right:none">
                        <a target="_blank" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=${exp.experimentAccession}">${exp.experimentAccession}</a>
                    </td>
                    <td class="padded wrapok" colspan="3" style="padding-right:70px;border-left:none">
                    	${exp.experimentName}
                    </td>
                    <td style="border-left:none">
                        <div style="position:relative;height:100%;min-height:45px;width:100%;">
                            <div style="position:absolute;right:0;top:0;min-height:45px;height:100%;width:90px;background:white;">
                                <a title="Show expression profile" href="experiment?eid=${exp.experimentAccession}&gid=${row.gene_id}&ef=${row.ef}" style="border:none;text-decoration:none;outline:none;">
                                    <div
                                     id="${exp.experimentId}_${exp.updn}_${r.index}" name="${exp.experimentId}_${r.index}"
                                     class="thumb" style="width:90px;height:45px;text-align:center" title="Click to view experiment detail">
                                      <img alt="Waiting..." src="images/indicator.gif" style="position:relative;top:10px"/>
                                    </div>
                                </a>
                            </div>
                        </div>
                    </td>
                    <!-- <td><div style="width:26px;background-color:${row.cellColor[row.expr]}"></div></td> -->
                    <c:choose>
                        <c:when test="${exp.updn == 'UP'}">
                            <td style="color: red;border-right:2px solid #cdcdcd" class="padded">
                                &#8593;&nbsp;<fmt:formatNumber value="${exp.pvalue}" pattern="#.##E0" />
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td style="color: blue;border-right:2px solid #cdcdcd" class="padded">
                                &#8595;&nbsp;<fmt:formatNumber value="${exp.pvalue}" pattern="#.##E0" />
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </c:forEach>
            <tr class="expand-child">
                <th colspan="6" style="padding:2px 0 0 0;background:#cdcdcd;"></th>
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

</div><!-- /id="ae_pagecontainer" -->
</td>
</tr>
</table><!-- /id="contentspane" -->
</div><!-- /id="contents" -->

<div  style="display:none">
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
                            <td><img src="images/indicator.gif" class="waiter" border="0" alt="Loading..." /><a class="proflink" title="Show expression profile" href="" style="border:none;outline:none;text-decoration:none" target="_blank"><div style="display:none" class="plot"></div></a></td>
                            <td><div style="display:none" class="efname"></div><div class="legend"></div></td>
                        </tr></table>
                
                        <div style="margin-top:5px;font-size:10px;">
                            Show <a class="proflink" target="_blank" title="Show expression profile" href="">expression profile</a>
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

