<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f"%>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="ae3.dao.AtlasDao"%>
<%@page import="java.util.*"%>
<%@page import="ae3.service.AtlasResultSet"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="ae3.model.AtlasTuple"%>
<%@page import="ae3.model.AtlasExperiment"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<%@page import="ae3.service.HeatmapRow"%>
<c:set var="timeStart" value="${u:currentTime()}" />

<%
	AtlasGene atlasGene = null;
	String geneId = request.getParameter("gid");
	String noAtlasExps = null;
	if (geneId != null) {
		//atlasGene = AtlasDao.getGeneByIdentifier(geneId);
		atlasGene = AtlasGeneService.getAtlasGene(geneId);
		noAtlasExps = ArrayExpressSearchService.instance().getNumOfAtlasExps(atlasGene.getGeneId());
		request.setAttribute("atlasGene",atlasGene);
		request.setAttribute("noAtlasExps",noAtlasExps);
	}
	request.setAttribute("heatMapRows",AtlasGeneService.getHeatMapRows());

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
ArrayExpress Atlas Gene View - ${(atlasGene.geneName)}
<jsp:include page="end_head.jsp" />
<style>
<!--
table.heatmap th {
	background-color: #bdd7d7;
	text-align: left;
	text-indent: 5px;
	font-family: Verdana;
	font-weight: bold;
	font-size: 11px;
	color: #404040;
}

table.heatmap {
	text-align: left;
	font-family: arial, sans-serif;
    font-size: 8pt;
	color: #404040;
	background-color: #fafafa;
	border-collapse: collapse;
	border-spacing: 0px;
}


.pagecontainer {
	padding-left: 15px;
	padding-top: 0px;
	padding-right: 15px;
	margin-bottom: 50px;
}

.exp_summary {
	padding-left: 15px;
	padding-top: 10px;
}

.separator {
	background-image: url( images/sep.png );
	background-repeat: repeat-x;
	height: 5px;
}

.exp_text {
	color: #404040;
	line-height: 15px;
}

.geneAnnotHeader {
	font-size: 9pt;
	font-weight: lighter;
	color: #408c8c;
	font-family: helvetica, arial, sans-serif;
	text-align: right;
	width: 10%;
	vertical-align: middle;
}

.moreLink {
	cursor: pointer;
	color: #408c8c;
	text-align: right;
	font-size: x-small;
	font-family: Verdana;
}

.sectionHeader {
	color: #9e9e9e;
	font-size: 11pt;
	text-align: left;
}

.titleHeader {
	color: #9e9e9e;
	font-size: 16pt;
	text-align: left;
}

.fullSectionView .fullSectionViewExt {
	color: #404040;
}

.RankInfoBar {
	width: 100px;
	height: 8px;
	border: solid 1px #5588ff;
	margin: 3px 4px 2px 0;
	float: left;
	overflow: hidden
}

.RankInfoInt {
	height: 100%;
	background: #99ccff;
	overflow: hidden
}

#panel {
	height: 50px;
}

.slide {
	margin: 0;
	padding: 0;
	background: url( images/searchAtlas.png ) no-repeat center top;
}

.btn-slide {
	background: url( images/white-arrow.gif ) no-repeat right -50px;
	text-align: center;
	width: 144px;
	height: 20px;
	padding: 8px 8px 0 0;
	margin: 0 auto;
	display: block;
	cursor: pointer;
	font-size: small;
	color: #000000;
}

.active {
	background-position: right 12px;
}

a:focus {
	outline: none;
}

.heatmap tr:hover {
	background-color: #bdd7d7;
	color: #404040;
	cursor: pointer;
}

.heatmap_out{
	background-color: #fafafa;
	color: #404040;
}

.heatmap_cell{
    width:20px; height:20px; position:relative;
}

}
-->
</style>
<script src="scripts/jquery-1.2.6.js" type="text/javascript"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->

<script language="javascript" type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="jquery.autocomplete.js"></script>
<script type="text/javascript" src="jquerydefaultvalue.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/plots.js"></script>


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
		 $('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,from:"1", to:"5"},drawPlots);
         $('#pagingSummary').text("Showing search results "+((0*5)+1)+"-"+((0*5)+5)+" out of "+<%=noAtlasExps%>);
         // Create pagination element
        $("#Pagination").pagination(<%=noAtlasExps%>, {
			num_edge_entries: 2,
			num_display_entries: 5,
			items_per_page:5,
            callback: pageselectCallback
         });
         $("#expHeader_td").text("Experiments showing differential expression for <%=atlasGene.getGeneName()%>");
	}
	
	function pageselectCallback(page_id, jq){
		var fromPage = (page_id*5) +1;
		var toPage = (page_id*5) + 5;
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,from:fromPage, to: toPage},drawPlots);
		$('#pagingSummary').text("Showing search results "+fromPage+"-"+toPage+" out of "+<%=noAtlasExps%>);
	}
	

	function FilterExps(fv,ef){
	
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,efv:fv},
							function(){
								
								
								for (var i = 0; i < exp_ids.length; ++i){
									eid = jQuery.trim(exp_ids[i]);
									redrawPlotForFactor(eid+'_'+<%=atlasGene.getGeneId()%>+'_'+ef,true,fv);
								}
							});
		$("#expHeader_td").text("Experiments showing differential expression for <%=atlasGene.getGeneName()%> in "+ fv);
		var lnk = $("<a> Return to all experiments </a>").bind("click", loadExps);
		$("#Pagination").empty().append(lnk);

	}
	
		
		
    jQuery(document).ready(function()
    {
       				
       loadExps();
        
        
        $("#q_gene").defaultvalue("(all genes)");
        $("#q_expt").defaultvalue("(all conditions)");

        $("#searchSlider").click(function() {
            $("#panel").slideToggle("slow");
            $(this).toggleClass("active");
            return false;
        });

        $("#q_expt").autocomplete("autocomplete.jsp", {
            minChars:1,
            width:300,
            matchSubset: false,
            multiple: true,
            multipleSeparator: " ",
            extraParams: {type:"expt"},
            formatItem:function(row) {
                return row[0] + " (" + row[1] + ")";
            }
        });

        $("#q_gene").autocomplete("autocomplete.jsp", {
            minChars:1,
            width:300,
            matchCase: true,
            matchSubset: false,
            multiple: true,
            multipleSeparator: " ",
            extraParams: {type:"gene"},
            formatItem:function(row) {
                return row[0] + " (" + row[1] + ")";
            }
        });
        
              

    });
</script>



<link rel="stylesheet" href="stylesheets/ae_browse.css" type="text/css" />
<link rel="stylesheet" href="stylesheets/ae_index.css" type="text/css" />
<link rel="stylesheet" href="stylesheets/ae_common.css" type="text/css" />
<link rel="stylesheet" href="scripts/pagination.css" />
<link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<jsp:include page='start_body_no_menus.jsp' />
<jsp:include page='end_menu.jsp' />
<div class="pagecontainer">
<table width="100%" style="border-bottom: thin solid lightgray"	cellpadding="0" cellspacing="0">
	<tr>
		<td align="left" valign="bottom" width="55"><a href="index.jsp"><img
			border="0" src="atlasbeta.jpg" width="50" height="25" /></a></td>


		<td align="left" valign="center" style="padding-top: 5px"><a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about
		the project</a> | <a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> | <a
			id="feedback_href" href="javascript:showFeedbackForm()">feedback</a>
		<span id="feedback_thanks" style="font-weight: bold; display: none">thanks!</span>
		| <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a>
		| <a target="_blank"
			href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web
		services api</a> (<b>new!</b>) | <a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a></td>


		<td align="left" valign="bottom"><img style="cursor: pointer;" 
			id="searchSlider" alt="" src="images/searchAtlas.png" /></td>
		<td align="right" valign="center" width="50px"><a
			href="http://www.ebi.ac.uk/microarray"><img border="0"
			height="20" title="EBI ArrayExpress" src="aelogo.png" /></a></td>

	</tr>
</table>

<div id="panel" style="display: none">
<form name="atlasform" action="qr" target="_blank">
<%
	String q_gene = request.getParameter("q_gene");
	String q_expt = request.getParameter("q_expt");
	String q_updn = request.getParameter("q_updn");
	String q_orgn = request.getParameter("q_orgn");

	if (q_updn == null)
		q_updn = "";
	if (q_expt == null || q_expt.equals("(all conditions)"))
		q_expt = "";
	if (q_gene == null || q_gene.equals("(all genes)"))
		q_gene = "";
	if (q_orgn == null)
		q_orgn = "";
%>

<table width="100%" style="background-color: #bdd7d7;">


	<tr valign="middle" align="left">

		<td align="left">
		<table>
			<tr>

				<td><input type="text" name="q_gene" id="q_gene"
					style="width: 150px; font-size: small"
					value="<%=StringEscapeUtils.escapeHtml(q_gene)%>" /></td>
				<td><select name="q_updn">
					<option value="updn" <%=q_updn.equals("updn") ? "selected" : ""%>>up/down
					in</option>
					<option value="up" <%=q_updn.equals("up") ? "selected" : ""%>>up
					in</option>
					<option value="down" <%=q_updn.equals("down") ? "selected" : ""%>>down
					in</option>
				</select></td>
				<td><input type="text" name="q_expt" id="q_expt"
					style="width: 150px"
					value="<%=StringEscapeUtils.escapeHtml(q_expt)%>" /></td>
				<td><select id="q_orgn" name="q_orgn" style="width: 130px">
					<option value="any" <%=q_orgn.equals("") ? "selected" : ""%>>Any
					species</option>
					<%
						SortedSet<String> species = ArrayExpressSearchService.instance()
								.getAllAvailableAtlasSpecies();
						for (String s : species) {
					%>
					<option value="<%=s.toUpperCase()%>"
						<%=q_orgn.equals(s.toUpperCase()) ? "selected" : ""%>><%=s%>
					</option>
					<%
						}
					%>
				</select></td>
				<td align="left"><label>View results as:</label> <input
					type="radio" name="view" id="view_table" value="table"
					<%=request.getParameter("view") == null || request.getParameter("view").equals("table") ? "checked" : ""%>>
				<label for="view_table">table</label> <input type="radio"
					name="view" id="view_heatmap" value="heatmap"
					<%=request.getParameter("view") != null && request.getParameter("view").equals("heatmap") ? "checked" : ""%>>
				<label for="view_heatmap">heatmap</label> <input type="submit"
					value="Search Atlas"> <%--<input type="checkbox" name="expand_efo" id="expand_efo" value="expand_efo"--%>
				<%--<%=null == request.getParameter("expand_efo") ? "checked" : ""%> --%>
				<%--<%=null != request.getParameter("expand_efo") && request.getParameter("expand_efo").equals("expand_efo") ? "checked" : ""%>>--%>
				<%--<label for="expand_efo">expand conditions search with <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO" title="Experimental Factor Ontology">EFO</a></label>--%>
				<input type="hidden" name="expand_efo" id="expand_efo" value="on" />
				</td>
			</tr>
		</table>
		</td>
	</tr>

</table>

<input type="hidden" name="view" /></form>
</div>


<table width="100%" border="0" cellpadding="0" cellspacing="0">
	<tr>
		<td class="titleHeader" valign="middle">ArrayExpress Atlas Gene	View</td>
		<td align="right" valign="top">
		<div style="color: #e33e3e; text-align: right; font-size: 26pt; margin-top: 15px; font-weight: bold; vertical-align: top">
			${atlasGene.geneName}
		</div>
		<span style="margin-top: 0; text-align: right; color: #1f7979; font-size: 9pt; font-weight: bold;">
			${atlasGene.geneSpecies}
		</span>
		</td>
	</tr>
</table>

<table width="100%">

	<tr>
		<td class="geneAnnotHeader">Synonyms:</td>
		<td align="left">${atlasGene.synonyms}</td>
	</tr>
	
	<c:if test="${!empty atlasGene.interProTerm}">
		<tr>
			<td></td>
			<td>
			<div class="separator"></div>
			</td>
		</tr>
		<tr>
			<td class="geneAnnotHeader">InterPro Term:</td>
			<td align="left">${atlasGene.shortInterProTerms}</td>
		</tr>
	</c:if>
	
	<c:if test="${!empty atlasGene.disease}">
		<tr>
			<td></td>
			<td>
			<div class="separator"></div>
			</td>
		</tr>
		<tr>
			<td class="geneAnnotHeader">Diseases:</td>
			<td align="left">${atlasGene.shortDiseases}</td>
		</tr>
	</c:if>
	
	<c:if test="${!empty atlasGene.goTerm}">
		<tr>
			<td></td>
			<td>
			<div class="separator"></div>
			</td>
		</tr>
		<tr>
			<td class="geneAnnotHeader">GO Terms:</td>
			<td align="left">${atlasGene.shortGOTerms}</td>
		</tr>
	</c:if>
	
	<c:if test="${!empty atlasGene.uniprotIds}">
		<tr>
			<td></td>
			<td><div class="separator"></div></td>
		</tr>
		<tr>
			<td class="geneAnnotHeader">Uniprot:</td>
			<td align="left">
				<c:forEach var="uniprot" items="${atlasGene.geneSolrDocument.fieldValuesMap['gene_uniprot']}">
				 <a href="http://www.uniprot.org/uniprot/${uniprot}" target="_blank">${uniprot}</a>&nbsp;
				</c:forEach>
			</td>
		</tr>
	</c:if>
	
	<tr>
		<td></td>
		<td><div class="separator"></div></td>
	</tr>
	
	<tr>
		<td class="geneAnnotHeader">Cross Refs:</td>
		<td align="left">
			<a title="Show gene annotation" target="_blank"	href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=${atlasGene.geneIdentifier}">
				${atlasGene.geneIdentifier} 
			</a>
		</td>
		
		<!-- td width="8%">&nbsp;</td-->

	</tr>

	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>
</table>






<table cellspacing="5" cellpadding="10">
	<tr>

		<td valign="top">
			<table>
				<tr>
					<td class="sectionHeader">Expression Summary</td>
				</tr>
				<tr>
					<td colspan="2">
					<div class="separator"></div>
					</td>
				</tr>
	
				<tr>
					<td>
					<div>
					<table class="heatmap" cellpadding="0" cellspacing="5" border="1" RULES=ROWS FRAME=HSIDES>
						<tr>
							<th>Factor Value</th>
							<th>Factor</th>
							<th>
								<div class="sq"><div class="tri" style="border-right-color:#0000CC;border-top-color:#FF0000"></div>
                                    	<div style="color:#FFFFFF" class="dnval">D</div>
                                    	<div style="color:#000000" class="upval">U</div></div>
							</th>
							<!-- th style="border-right: medium solid; border-left: thin">Studies</th>
							<th style="border-right: medium solid" colspan="2" align="center">${atlasGene.geneName}</th-->
						</tr>
						
						
						<c:forEach var="row" items="${heatMapRows}" varStatus="i">
						<tr onmouseover="this.className='heatmap_over'" 
						    onmouseout="this.className='heatmap_out'" 
						    onclick="FilterExps('${row.fv}','${u:escapeJS(row.ef)}')">
							<td nowrap="true">
								<span style="font-weight: bold"	title="${row.fv}  Matched in experiment(s)" >
									${row.shortFv}
								</span>
							</td>
								
							<td nowrap="true">
								<fmt:message key="head.ef.${row.ef}"/>
							</td>
							
							<c:choose>
								<c:when test="${row.mixedCell}">
									<td class="acounter"
                                    	title="">
                                    	<div class="sq"><div class="tri" style="border-right-color:${row.cellColor['dn']};border-top-color:${row.cellColor['up']}"></div>
                                    	<div style="color:${row.cellText['dn']}" class="dnval">${row.count_dn}</div>
                                    	<div style="color:${row.cellText['up']}" class="upval">${row.count_up}</div></div>
                               		</td>
								</c:when>
								<c:otherwise>
									<td class="acounter" style="background-color:${row.cellColor[row.expr]};color:${row.cellText[row.expr]}"
                                    	title="">
                                    	<div class="sq" style="text-align: center; vertical-align: middle" > <c:if test="${row.count_dn!=0}"> <c:out value="${row.count_dn}"></c:out> </c:if>
                                    			 <c:if test="${row.count_up!=0}"> <c:out value="${row.count_up}"></c:out> </c:if> </div>
                                    		
                                	</td>
								</c:otherwise>
							</c:choose>
						</tr>
						
						
						</c:forEach>
					</table>
					</div>
					</td>
				</tr>
	
				<tr>
					<td colspan="2">
					<div class="separator"></div>
					</td>
				</tr>
			</table>
		</td> 
		<td valign="top" align="left">
			<table align="left">
			<tr>
				<td id="expHeader_td" class="sectionHeader">Experiments showing Differential expression for <%=atlasGene.getGeneName()%>
				</td>	
			</tr>
			<tr>
				<td colspan="2">
					<div class="separator"></div>
				</td>
			</tr>
			<tr>
				<td align="right">
					<div id="Pagination" class="pagination_ie"></div>
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

<c:set var="timeFinish" value="${u:currentTime()}" />
<div align="center">Processing time: <c:out
	value="${(timeFinish - timeStart) / 1000.0}" /> secs.</div>

</div>
<!-- end page contents here -->
<jsp:include page='end_body.jsp' />
