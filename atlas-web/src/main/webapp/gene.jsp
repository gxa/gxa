<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f"%>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="java.util.*"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<c:set var="timeStart" value="${u:currentTime()}" />

<%
	AtlasGene atlasGene = null;
	String geneId = request.getParameter("gid");
	String noAtlasExps = null;
	if (geneId != null || geneId!="") {
		atlasGene = AtlasGeneService.getAtlasGene(geneId);
		if ( atlasGene!=null){
			noAtlasExps = ArrayExpressSearchService.instance().getNumOfAtlasExps(atlasGene.getGeneId());
			request.setAttribute("heatMapRows",AtlasGeneService.getHeatMapRows(geneId));
			request.setAttribute("atlasGene",atlasGene);
			request.setAttribute("noAtlasExps",noAtlasExps);
		} else {
			request.getRequestDispatcher("/geneNotFound.jsp").forward(request,response);
                }
	} else {
		request.getRequestDispatcher("/geneNotFound.jsp").forward(request,response);
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
Gene Expression Summary for ${atlasGene.geneName} (${atlasGene.geneSpecies}) - ArrayExpress Atlas of Gene Expression
<jsp:include page="end_head.jsp" />

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - ArrayExpress Atlas Gene Expression Summary"/>
<meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />

<script src="scripts/jquery-1.2.6.js" type="text/javascript"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->

<script language="javascript" type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/plots.js"></script>
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />


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
            lnkElt.attr("src", "images/plus.gif");
        } else {
            lnkElt.addClass("expanded");
            lnkElt.attr("src", "images/minus.gif");
        }
    }
    
//-->
</script>
<script type="text/javascript">

	function loadExps(){
		 $('#ExperimentResult').load("AtlasExpResults.jsp",{gid:${atlasGene.geneId},from:"1", to:"5"},drawPlots);
         $('#pagingSummary').empty();
         $(".heatmap_over").removeClass("heatmap_over");
         // Create pagination element
         <c:if test="${noAtlasExps > 5}">
        	$("#Pagination").pagination(${noAtlasExps}, {
				num_edge_entries: 2,
				num_display_entries: 5,
				items_per_page:5,
            	callback: pageselectCallback
         	});
         </c:if>
         //$("#expHeader_td").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");
         $("#pagingSummary").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");
	}
	
	function pageselectCallback(page_id, jq){
		var fromPage = (page_id*5) +1;
		var toPage = (page_id*5) + 5;
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:${atlasGene.geneId},from:fromPage, to: toPage},drawPlots);
		//$('#pagingSummary').text("Showing experiments "+fromPage+"-"+toPage);
		//$("#expHeader_td").text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in "+ fv);
	}
	

	function FilterExps(el,fv,ef){
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:${atlasGene.geneId},efv:fv,factor:ef},
							function(){
								for (var i = 0; i < exps.length; ++i){
									eid = jQuery.trim(exps[i].id);
									redrawPlotForFactor(eid+'_${atlasGene.geneId}_'+ef,true,fv);
								}
								//$("#expHeader_td").text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in "+ fv);
								$('#pagingSummary').text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in \""+ fv + "\"");
								var lnk = $("<a>Show all studies</a>").bind("click", loadExps);
								$("#Pagination").empty().append(lnk);
							});

        old = $(".heatmap_over");
		old.removeClass("heatmap_over");
        old.addClass ("heatmap_row");
		el.className = "heatmap_over";
		scroll(0,0);
	}
	
		
		
    jQuery(document).ready(function()
    {		
       loadExps();
             
    });
</script>


<style type="text/css">.contents{top:87px}</style>

<link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<jsp:include page='start_body_no_menus.jsp' />

        <div style="padding-left:15px; padding-right:15px;">
        <table style="position:relative; z-index:1; top:58px;border-bottom:thin solid lightgray;width:100%;height:30px">
            <tr>
                <td align="left" valign="bottom" width="55" style="padding-right:10px;">
                     <a href="index.jsp" title="ArrayExpress Atlas Homepage"><img border="0" width="55" src="images/atlas-logo.png" alt="Atlas of Gene Expression"/></a>
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

<table width="100%" style="background-color: white">
		
			<tr>
				<td align="left" class="geneName">${atlasGene.geneName}</td>
				<td style="vertical-align: text-bottom">${atlasGene.geneSpecies}</td>
			</tr>
			<tr>
				<td class="geneAnnotHeader">Synonyms</td>
				<td align="left">${atlasGene.synonyms}</td>
			</tr>
			
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
<br/>
<table> 

	
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
					
					<table class="heatmap" cellpadding="0" cellspacing="5" border="0">
						<tr style="height:26px;border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD">
							<th style="padding-left:2px">Factor Value</th>
							<th>Factor</th>
							<th style="padding-right:2px">Up/Down</th>
							<!-- th style="border-right: medium solid; border-left: thin">Studies</th>
							<th style="border-right: medium solid" colspan="2" align="center">${atlasGene.geneName}</th-->
						</tr>
						
						
						<c:forEach var="row" items="${heatMapRows}" varStatus="i">
						<tr class="heatmap_row"
							style="border-bottom:1px solid #CDCDCD"
						    onclick="FilterExps(this,'${u:escapeJS(row.fv)}','${u:escapeJS(row.ef)}')" 
						    title="${atlasGene.geneName}${row.text}">
							<td nowrap="true" style="padding-right:5px;padding-left:2px">
								<span style="font-weight: bold">
									${row.shortFv}
								</span>
							</td>
								
							<td nowrap="true" style="padding-right:5px">
								<fmt:message key="head.ef.${row.ef}"/>
							</td>

							<td class="acounter" align="right">
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
					</table>
					
					</td>
				</tr>
			</table>
		</td> 
		<td valign="top" align="left">
			<table align="left">
			<tr>
				<td id="expHeader_td" class="sectionHeader" style="vertical-align: top">Expression Profiles</td>	
				<td align="right">
					<div id="Pagination" class="pagination_ie" style="padding-bottom: 0; padding-top: 2px; "></div>
				</td>
			</tr>
			
			<!--  -->
			<tr>
				<td align="left" colspan="2">
					<div id="pagingSummary" class="header"></div>
				</td>
				
			</tr>
			<tr>
				<td colspan="2" style="border-bottom:1px solid #CDCDCD">
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div id="ExperimentResult"></div>
				</td>
			</tr>
			</table>
		</td> 
	</tr>
</table>
		</td>
	</tr>

</table>










<c:set var="timeFinish" value="${u:currentTime()}" />
<div align="center">Processing time: <c:out
	value="${(timeFinish - timeStart) / 1000.0}" /> secs.</div>

</div>
<!-- end page contents here -->
<jsp:include page='end_body.jsp' />
