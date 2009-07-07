<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f"%>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="java.util.*"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<c:set var="timeStart" value="${u:currentTime()}" />

<jsp:include page="AtlasHomeUrl.jsp" />


<%
	AtlasGene atlasGene = null;
	String geneId = request.getParameter("gid");
	String noAtlasExps = null;
	if (geneId != null || geneId!="") {


		if(AtlasGeneService.hitMultiGene(geneId)){
	        response.sendRedirect(request.getContextPath() + "/qrs?gprop_0=&gval_0="+geneId+"&fexp_0=UP_DOWN&fact_0=&specie_0=&fval_0=(all+conditions)&view=hm");
	        return;
		}

		atlasGene = AtlasGeneService.getAtlasGene(geneId);
		if ( atlasGene!=null){
			noAtlasExps = ArrayExpressSearchService.instance().getNumOfAtlasExps(atlasGene.getGeneId());
			request.setAttribute("heatMapRows", AtlasGeneService.getHeatMapRows(geneId));
			request.setAttribute("atlasGene",atlasGene);
			request.setAttribute("noAtlasExps",noAtlasExps);
		}
	}
    if(atlasGene == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorMessage", "There are no records for gene " + String.valueOf(geneId));
        request.getRequestDispatcher(request.getContextPath() + "/error.jsp").forward(request,response);
        return;
    }

	if (request.getParameter("format") != null	&& request.getParameter("format").equals("xml")) {
		//TODO: set this right (via REST WS perhaps)
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		Map<String, Collection<Object>> props = atlasGene.getGeneSolrDocument().getFieldValuesMap();
%>
<atlasGene>
<%
	for (String prop : props.keySet()) {
%>
<geneProperty name="<%=prop%>">
<%
	Collection propVals = props.get(prop);
			for (Object propVal : propVals) {
%><value><%=propVal%></value>
<%
	}
%>
</geneProperty>
<%
	}
%>
</atlasGene>
<%
	response.flushBuffer();
		return;
	}
%>
<jsp:include page="start_head.jsp" />
Gene Expression Atlas Summary for ${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas
<jsp:include page="end_head.jsp" />

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas Summary"/>
<meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />

<script type="text/javascript" language="javascript" src="<%=request.getContextPath()%>/scripts/jquery-1.3.2.min.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/plots.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/feedback.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.tablesorter.min.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.flot.atlas.js"></script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/atlas.css" type="text/css" />
<link rel="stylesheet" href="<%=request.getContextPath()%>/geneView.css" type="text/css" />


<script type="text/javascript">
<!--
    function  viewMore(id)
    {

        id = String(id);
        var extElt = $("#" + id + "_ext");
        var lnkEltLess = $("#" + id + "_atls_lnk_less");
        var lnkEltMore = $("#" + id + "_atls_lnk_more");
        if (extElt.hasClass("fullSectionViewExt")) {
            // collapse now
            extElt.removeClass("fullSectionViewExt");
            extElt.addClass("fullSectionView");
            extElt.toggle("fast");
            lnkEltMore.show();
        } else {
            extElt.addClass("fullSectionViewExt");
            extElt.toggle("fast");
            lnkEltMore.hide();

        }
    }
    function showStudyDetails(id) {
        id = String(id);
        var divElt = $("#" + id + "_desc_ext");
        var lnkElt = $("#" + id + "_std_lnk");
        divElt.slideToggle("fast");
        if (lnkElt.hasClass("expanded")) {
            lnkElt.removeClass("expanded");
            lnkElt.attr("src", "<%=request.getContextPath()%>/images/plus.gif");
        } else {
            lnkElt.addClass("expanded");
            lnkElt.attr("src", "<%=request.getContextPath()%>/images/minus.gif");
        }
    }

//-->
</script>
<script type="text/javascript">

	function loadExps(){

        return;
    }

    function reloadExps(){

		 $('#ExperimentResult').load("<%=request.getContextPath()%>/AtlasExpResults.jsp",{gid:${atlasGene.geneId},from:"1", to:"5"},drawPlots);
         $('#pagingSummary').empty();
         $(".heatmap_over").removeClass("heatmap_over");
         // Create pagination element
         <c:if test="${noAtlasExps > 5}">
        	$("#Pagination").pagination(${noAtlasExps}, {
				num_edge_entries: 2,
				num_display_entries: 5,
				items_per_page:5,
                link_to : "#?Page=__id__",
            	callback: pageselectCallback
         	});
         </c:if>
         //$("#expHeader_td").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");
         $("#pagingSummary").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");
	}

	function pageselectCallback(page_id, jq){
		var fromPage = (page_id*5) +1;
		var toPage = (page_id*5) + 5;
		$('#ExperimentResult').load("<%=request.getContextPath()%>/AtlasExpResults.jsp",{gid:${atlasGene.geneId},from:fromPage, to: toPage},drawPlots);
		//$('#pagingSummary').text("Showing experiments "+fromPage+"-"+toPage);
		//$("#expHeader_td").text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in "+ fv);
	}


	function FilterExps(el,fv,ef){

        $('#ExperimentResult').empty();

		$('#ExperimentResult').load("<%=request.getContextPath()%>/AtlasExpResults.jsp",{gid:${atlasGene.geneId}, efv: uni2ent(fv), factor:ef},
							function(){

                                //alert(exps.length);

								for (var i = 0; i < exps.length; ++i){
									eid = jQuery.trim(exps[i].id);
									redrawPlotForFactor(eid,'${atlasGene.geneId}',ef,true,fv);
								}
								//$("#expHeader_td").text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in "+ fv);
								$('#pagingSummary').text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in \""+ fv + "\"");
								var lnk = $("<a>Show all studies</a>").bind("click", reloadExps);
								$("#Pagination").empty().append(lnk);
							});

        //$("#expHeader_td").text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in "+ fv);
        //$('#pagingSummary').text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in \""+ fv + "\"");
        //var lnk = $("<a>Show all studies</a>").bind("click", reloadExps);
        //$("#Pagination").empty().append(lnk);


        old = $(".heatmap_over");
		old.removeClass("heatmap_over");
        old.addClass ("heatmap_row");
		el.className = "heatmap_over";
		scroll(0,0);
	}



    jQuery(document).ready(function()
    {
       loadExps();
       $("#heatmap_tbl").tablesorter({
               headers: {2: {sorter: false}}});
    });
</script>


<link rel="stylesheet" href="<%= request.getContextPath() %>/blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="<%= request.getContextPath() %>/jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="<%= request.getContextPath() %>/structured-query.css" type="text/css" />
<jsp:include page='start_body_no_menus.jsp' />

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right:10px;">
            <a href="<%= request.getContextPath() %>/" title="Gene Expression Atlas Homepage"><img border="0" width="55" src="<%=request.getContextPath()%>/images/atlas-logo.png" alt="Gene Expression Atlas"/></a>
        </td>
        <td align="right" valign="bottom">
            <a href="./">home</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
    </tr>
</table>


<table width="100%" style="background-color: white">

    <tr>
        <td align="left" class="geneName">${atlasGene.geneName}</td>
        <td style="vertical-align: text-bottom">${atlasGene.geneSpecies}</td>
    </tr>
    <tr>
        <td class="geneAnnotHeader">Synonyms</td>
        <td align="left">${atlasGene.synonyms}</td>
    </tr>

    <c:if test="${!empty atlasGene.orthologs}">
        <tr>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Orthologs</td>

            <td align="left">
                <c:forEach var="ortholog" items="${atlasGene.orthoGenes}">
                    <a href="${u:GeneUrl(pageContext.request,ortholog.geneEnsembl)}" target="_self" title="Gene Atlas Data For ${ortholog.geneName} (${ortholog.geneSpecies})">${ortholog.geneName} (${ortholog.geneSpecies})</a>&nbsp;
                </c:forEach>
                (<a href="<%= request.getContextPath() %>/qrs?gprop_0=&gval_0=${atlasGene.orthologsIds}+${atlasGene.geneIdentifier}&fexp_0=UP_DOWN&fact_0=&specie_0=&fval_0=(all+conditions)&view=hm"
                    target="_self">Compare orthologs</a>)
            </td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.interProTerm}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">InterPro Term</td>
            <td align="left">${atlasGene.shortInterProTerms}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.disease}">
        <tr>
            <td></td>
            <td>
            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Diseases</td>
            <td align="left">${atlasGene.shortDiseases}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.goTerm}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">GO Terms</td>
            <td align="left">${atlasGene.shortGOTerms}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.uniprotIds}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Uniprot</td>
            <td align="left">
                <c:forEach var="uniprot" items="${atlasGene.geneSolrDocument.fieldValuesMap['gene_uniprot']}">
                    <a href="http://www.uniprot.org/uniprot/${uniprot}" target="_blank">${uniprot}</a>&nbsp;
                </c:forEach>
            </td>
        </tr>
    </c:if>

    <tr>
        <td></td>
        <td></td>
    </tr>

    <tr>
        <td class="geneAnnotHeader">Search EB-eye</td>
        <td align="left">
            <a title="Show gene annotation" target="_blank"	href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=${atlasGene.geneIdentifier}">
                ${atlasGene.geneIdentifier}
            </a>
        </td>

        <!-- td width="8%">&nbsp;</td-->

    </tr>

    <tr>
        <td colspan="2">

        </td>
    </tr>
</table>
<table cellspacing="0" cellpadding="0" style="margin-top:40px">


    <tr>
        <td>
            <table cellspacing="0" cellpadding="0" border="0">
                <tr>

                    <td valign="top" style="padding-right: 30px">
                        <table>
                            <tr>
                                <td class="sectionHeader">Expression Summary</td>
                            </tr>
                            <tr>
                                <td align="left" class="header">
                                    ${f:length(heatMapRows)} factor values, click each to filter
                                </td>

                            </tr>

                            <tr>
                                <td style="padding-top: 3px">

                                    <table class="heatmap" cellpadding="0" cellspacing="0" border="0" id ="heatmap_tbl">
                                        <thead>
                                            <tr style="height:26px;border-top:1px solid #CDCDCD">
                                                <th style="padding-left:2px;padding-right:5px;border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;border-left:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">Factor Value</th>
                                                <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">Factor</th>
                                                <th style="padding-right:2px;border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">Up/Down</th>
                                            </tr>
                                            <tr>
                                                <td valign="top" height="30" align="center" colspan="3" style="border-bottom:1px solid #CDCDCD;background-color:white;border-left:1px solid #CDCDCD;border-right:1px solid #CDCDCD">
                                                    Legend: <img style="position:relative;top:6px" src="<%=request.getContextPath()%>/images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">up</span>/<span style="color:blue;font-weight:bold">down</span> in
                                                </td>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="row" items="${heatMapRows}" varStatus="i">
                                                <tr class="heatmap_row"

                                                    onclick="FilterExps(this,'${u:escapeJS(row.fv)}','${u:escapeJS(row.ef)}')"
                                                    title="${atlasGene.geneName}${row.text}">
                                                    <td nowrap="true" style="padding-right:5px;padding-left:2px;border-bottom:1px solid #CDCDCD; min-width: 100px;border-left:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">
                                                        <span style="font-weight: bold">
                                                                ${row.shortFv}
                                                        </span>
                                                    </td>

                                                    <td nowrap="true" style="padding-right:5px;border-bottom:1px solid #CDCDCD;min-width: 80px;padding-left:4px;padding-top:1px;padding-bottom:1px">
                                                        <fmt:message key="head.ef.${row.ef}"/>
                                                    </td>

                                                    <td class="acounter" align="right" style="border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD">
                                                        <c:choose>
                                                            <c:when test="${row.mixedCell}">
                                                                <div style="width:26px">
                                                                    <div class="sq"><div class="tri" style="border-right-color:${row.cellColor['dn']};border-top-color:${row.cellColor['up']}"></div>
                                                                        <div style="color:${row.cellText['dn']}" class="dnval">${row.count_dn}</div>
                                                                        <div style="color:${row.cellText['up']}" class="upval">${row.count_up}</div></div>
                                                                </div>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <div style="width:26px;background-color:${row.cellColor[row.expr]};color:${row.cellText[row.expr]}">
                                                                    <div class="heatmap_cell"> <c:if test="${row.count_dn!=0}"> <c:out value="${row.count_dn}"></c:out> </c:if>
                                                                        <c:if test="${row.count_up!=0}"> <c:out value="${row.count_up}"></c:out> </c:if> </div>
                                                                </div>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>

                                </td>
                            </tr>
                        </table>
                    </td>
                    <td valign="top" align="left">
                        <jsp:include page="AtlasExpResultsTable.jsp">
                            <jsp:param name="GeneId" value="<%=atlasGene.getGeneId()%>" />
                        </jsp:include>
                    </td>
                </tr>
            </table>
        </td>
    </tr>

</table>

<c:set var="timeFinish" value="${u:currentTime()}" />
<div align="center">Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}" /> secs.</div>

</div><!-- /id="ae_pagecontainer" -->
</div><!-- /id="contents" -->

<!-- end page contents here -->
<jsp:include page='end_body.jsp' />
