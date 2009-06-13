<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@page import="ae3.model.AtlasExperiment"%>
<%@page import="ae3.service.ExperimentService"%>
<%@page import="java.util.*"%>
<%@page import="ae3.service.ListResultRow"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<%
AtlasExperiment exp = null;
String expAcc = request.getParameter("eid");
String geneId = request.getParameter("gid");
String ef = request.getParameter("ef");
if (expAcc != null && expAcc!="") {
	exp = ExperimentService.getAtlasExperiment(expAcc);
	request.setAttribute("exp",exp);
	
	if(ef ==null || ef=="" ){
		HashMap rankInfo =  ArrayExpressSearchService.instance().getHighestRankEF(exp.getDwExpId().toString(), geneId);
		request.setAttribute("topRankEF",rankInfo.get("expfactor").toString());
		ef = rankInfo.get("expfactor").toString();
	}
}

request.setAttribute("gid",geneId);
request.setAttribute("ef",ef);
request.setAttribute("eid",exp.getDwExpId());
request.setAttribute("eAcc",expAcc);
ArrayList<String> genesToPlot = new ArrayList<String>();

%>

<%@page import="ae3.service.structuredquery.AtlasStructuredQueryResult"%>
<jsp:include page="start_head.jsp" />
Atlas Gene Expression Summary for experiment ${exp.dwExpAccession} - ArrayExpress Atlas of
Gene Expression
<jsp:include page="end_head.jsp" />

<script src="scripts/jquery-1.2.6.js" type="text/javascript"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->

<script language="javascript" type="text/javascript"	src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="scripts/jquery-ui.min.js"></script>
<script type="text/javascript" src="scripts/plots.js"></script>
<script type="text/javascript" src="scripts/feedback.js"></script>
<script type="text/javascript" src="scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="scripts/jquery.selectboxes.min.js"></script>
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />
<script type="text/javascript" src="scripts/common-query.js"></script>
<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.7.1/themes/ui-lightness/jquery-ui.css" type="text/css" media="screen" />
<link rel="stylesheet" href="http://static.jquery.com/ui/css/demo-docs-theme/ui.theme.css" type="text/css" media="screen" />
			
			<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.7.1/jquery-ui.min.js" type="text/javascript"></script>
			<script src="http://jquery-ui.googlecode.com/svn/tags/latest/external/bgiframe/jquery.bgiframe.min.js" type="text/javascript"></script>
			<script src="http://jquery-ui.googlecode.com/svn/tags/latest/ui/minified/i18n/jquery-ui-i18n.min.js" type="text/javascript"></script>

<style type="text/css">
.contents {
	top: 87px
}
.ui-tabs .ui-tabs-hide {
     display: none;
}
</style>


<link rel="stylesheet" href="structured-query.css" type="text/css" />
<script id="source" language="javascript" type="text/javascript">
var genesToPlot = new Array("${gid}");

var sampleAttrs;
var assayIds;
var assay2samples;
var characteristics;
var charValues;
var plot;
var prevSelections = new Array();
var geneCounter = 0;
var geneIndeces = new Array();
geneIndeces.push(geneCounter);
$(document).ready(function() 
    { 
        
        $("#topGenes").load("expGenes",{eid:'${eid}',eAcc:'${exp.dwExpAccession}',gid:'${gid}',query:'top'}, function(){
        	 
        	initPaging();
        });
        
        $("#accordion").accordion({
							collapsible: true,
							active:false,
							autoHeight: false
						
						});

        initExpPageAutoComplete();
        $("#gene_menu").accordion({
							collapsible: true,
							active:false,
							autoHeight: false
						});
        
        plotBigPlot('${gid}','${eid}','${ef}',true, geneIndeces.toString());
        
        $("button").click(function(){
        
        	$("#drill").slideToggle("fast");
        	
        });
        
        $("#zoom").click(function(){
        
        	if($("#zoom").text()=="(hide)")
        		$("#zoom").text("(show)");
        	else
        		$("#zoom").text("(hide)");
        	$("#plot_thm").toggle("fast");
        });
        
        $("#simForm").submit(function(){
        	$("#simResult").empty();
         	var name = $('select option:selected').text();
        	$("#simHeader").html("<img src='images/indicator.gif' />&nbsp;Searching similar profiles to "+name);
        	var DEid_ADid = $("select option:selected").val();
        	var tokens = DEid_ADid.split('_');
        	var DEid = tokens[0];
        	var ADid = tokens[1];
        	$("#simResult").load("expGenes",{eid:'${eid}', deid:DEid, adid:ADid, eAcc:'${exp.dwExpAccession}',query:'sim'},function(){
        		$("#simHeader").text("Genes with similar profile to "+ name);
        	});
        	return false;
        });
        
        $("#searchForm").submit(function(){
        	var qry = $("#geneInExp_qry").val();
        	$("#qryResult").load("expGenes",{eid:'${eid}', gene:qry, eAcc:'${exp.dwExpAccession}',query:'search' });
        	return false;
        });
       
    } 
);

function addGeneToPlot(gid,gname,eid,ef){
	
	if(genesToPlot[gname] != null){
		return false;
		// if ef is different from current one redraw plot for new ef
	}
	geneCounter++;
	genesToPlot.push(gid);
	genesToPlot[gname]=gid;
	geneIndeces.push(geneCounter);
	plotBigPlot(genesToPlot.toString(),eid,ef,false,geneIndeces.toString());
} 

function redrawForEF(eid, ef, efTxt){
	
	//redrawPlotForFactor(eid,genesToPlot.toString(),ef,'large',false,"",geneIndeces.toString());
	plotBigPlot(genesToPlot.toString(),eid,ef,false,geneIndeces.toString()); 
	$('#sortHeader').text("Expression profile sorted by "+efTxt);
}


function radioLabel(label){
	return label + '&nbsp;<img id="'+label+'"class="rmButton" height="8" src="images/closeButton.gif"/>';
}

function removeGene(gname){
	if(genesToPlot.length == 1)
		return false; 
		
	var gid = genesToPlot[gname];
	delete genesToPlot[gname];
    for(var i=0; i<genesToPlot.length;i++ )
     { 
        if(genesToPlot[i]==gid){
            genesToPlot.splice(i,1); 
            geneIndeces.splice(i,1);
        } 
     }
     //$("#"+gname+":parent").hide();
     plotBigPlot(genesToPlot.toString(),'${eid}','${ef}',false,geneIndeces.toString()); 
}

</script>


<jsp:include page='start_body_no_menus.jsp' />

<div style="padding-left: 15px; padding-right: 15px;">
<table
	style="position: relative; z-index: 1; top: 58px; border-bottom: 1px solid #dedede; width: 100%; height: 30px">
	<tr>
		<td align="left" valign="bottom" width="55"
			style="padding-right: 10px;"><a href="index.jsp"
			title="ArrayExpress Atlas Homepage"><img border="0" width="55"
			src="images/atlas-logo.png" alt="Atlas of Gene Expression" /></a></td>
		<td align="right" valign="bottom"><a href="./">home</a> | <a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about
		the project</a> | <a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> | <a
			id="feedback_href" href="javascript:showFeedbackForm()">feedback</a>
		<span id="feedback_thanks" style="font-weight: bold; display: none">thanks!</span>
		| <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a>
		| <a target="_blank"
			href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web
		services api</a> | <a
			href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a></td>
	</tr>
</table>
</div>
<script type="text/javascript">

    var curatedChars = new Array();
    <c:forEach var="char" varStatus="s" items="${exp.sampleCharacteristics}">
    	curatedChars['${char}']= '${u:getCurated(char)}';
   	</c:forEach>
    

</script>


<div class="contents" id="contents">
<table class="contentspane" id="contentspane"	summary="The main content pane of the page" style="width: 100%">
	<tr>
		<td class="leftmargin"><img
			src="http://www.ebi.ac.uk/inc/images/spacer.gif" class="spacer"
			alt="spacer" /></td>
		<td class="leftmenucell" id="leftmenucell">
		<div class="leftmenu" id="leftmenu"
			style="visibility: hidden; display: none;"><img 
			src="http://www.ebi.ac.uk/inc/images/spacer.gif" class="spacer"
			alt="spacer" /></div>
		</td>
		<td class="contentsarea" id="contentsarea">
		<div id="ae_pagecontainer">
		<table width="100%" style="background-color: white">

			<tr>
				<td align="left" class="geneName"><a title="Show experiment details in the archive"
					target="_blank"
					href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=${exp.dwExpAccession}">${exp.dwExpAccession}</a></td>
				<td style="vertical-align: text-bottom"></td>
			</tr>
			<tr>
				<td class="header" colspan="2" style="border-bottom: 1px solid #dedede;">${exp.dwExpDescription}</td>
			</tr>
			<!-- 
			<tr>
				<td class="geneAnnotHeader">ArrayExpress Archive</td>
				<td align="left"><a title="Show experiment details"
					target="_blank"
					href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=${exp.dwExpAccession}">
				${exp.dwExpAccession} </a></td>
			</tr>
 -->
			
		</table>
		<div id = "result_cont" style="width: 100%" >
		
		<table id="twocol" style="margin-top:5px; width:100%; ">
			<tr>
				<td colspan="1">
								<div id="sortHeader" class="sectionHeader" style="">Expression profile sorted by <fmt:message key="head.ef.${ef}"/></div>
								<!--  <div class="header">Other Experimental Factors</div>-->
								<div class="header"  style="padding-bottom: 10px; padding-top: 5px;">
									<span></span>
										 
										<div id="${exp.dwExpId}_EFpagination" class="pagination_ie" style="padding-top: 10px;">
										<c:forEach var="EF" items="${exp.experimentFactors}">
											<c:choose>
												<c:when test="${EF == topRankEF}">
													<span class="current" id="${EF}"><fmt:message key="head.ef.${EF}"/></span>
												</c:when>
												<c:otherwise>
													<a id="${EF}" onclick="redrawForEF('${exp.dwExpId}','${EF}','<fmt:message key="head.ef.${EF}"/>')" ><fmt:message key="head.ef.${EF}"/></a>	
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</div> 
								</div>
								
							</td>
							<td>
								<div class="sectionHeader">Add more profiles</div>
							</td>
			</tr>
			<tr>
				
				<td style="width:650px; padding-right: 0px;">
					<table>
						<tr>
							<td align="left">
							<!-- <div><img src="plotHeader.jsp" usemap="#efvmap"> </div> -->
								<div class="bigplot" id="plot" style="width: 600px; height: 300px;top: 10px;"></div>
								<!-- <div style="font-size:x-small; color: gray;padding-top: 10px;">Click and drag a selection below to zoom in above <span style="cursor: pointer; text-decoration: underline" id="zoom">(hide)</span></div> -->
								<div id="plot_thm" style="border:thin; width: 600px; height: 100px;top:0px;left:25px"> </div>
								<div id ="zoomControls" style="position:absolute; left:616px; top:435px;"></div>
							</td>
							<td nowrap="nowrap">
								<div id="legend"></div>
							</td>
						</tr>
						
						<tr>
							<td>
								<div class="sectionHeader" style="padding-top: 20px; padding-bottom: 10px">Sample Attributes</div>
								<table>
									<tr>
										<td>
											<div class="header">Select attribues to highlight on graph</div>
											<div style="overflow-y: auto; height:350px">
												<div id="accordion" style="width:250px">

													<c:forEach var="char" items="${exp.sampleCharacteristics}" >
														<div><a href="#"><fmt:message key="head.ef.${char}"/><c:if test="${!empty exp.factorValuesForEF[char]}">&nbsp;(EF)</c:if></a>
														</div>
														<div>
															<ul>
														 	<c:forEach var="value" items="${exp.sampleCharacterisitcValues[char]}">
														 		<li><a style="text-transform: capitalize;" href="javascript:highlightSamples('${char}','${u:escapeJS(value)}','<fmt:message key="head.ef.${char}"/>')">${value}</a></li>
														 	</c:forEach>
														 	</ul>
														</div>
													</c:forEach>
													<c:forEach var="EF" items="${exp.experimentFactors}">
														<c:if test="${empty exp.sampleCharacterisitcValues[EF]}">
															<div><a href="#"><fmt:message key="head.ef.${EF}"/>&nbsp;(EF)</a></div>
															<div>
																<ul>
															 	<c:forEach var="value" items="${exp.factorValuesForEF[EF]}">
															 		<li><a style="text-transform: capitalize;" href="javascript:highlightSamples('${EF}','${u:escapeJS(value)}','<fmt:message key="head.ef.${EF}"/>',true)">${value}</script></a></li>
															 	</c:forEach>
															 	</ul>
															</div>
														</c:if>
													</c:forEach>
												
												
												
												</div>
											</div>
										</td>			
										<td>
											<div class="header">Selected sample attributes</div>
											<div id="bioSampleData" style="font-size:small; color: gray;">
												<span style="font-size:xx-small; color: gray"> Click data points on plot to show corresponding sample attributes</span>
											</div>
										</td>
									</tr>
								</table>
							</td>
							<td></td>
						</tr>
					</table>
				</td>
				<td>
					<table>
						<tr>
							<td nowrap="nowrap">
								<div id="gene_menu" style="">
									<div><a href="#">Top Differentially expressed genes</a></div>
									<div>
										<!--  <div id="Pagination" class="pagination_ie" style="padding-bottom: 10px;text-align:right;"></div>-->
										<div id="topGenes" style="height:250px"></div>
									</div>
									
									<div><a href="#">Search by gene name</a></div>
									<div>
										<form id="searchForm" class="visinsimple" action="javascript:void()">
											<input type="text" class="value" name="gval_0" id="geneInExp_qry" style="width:200px" />
											<button type="submit">Search</button>
										</form>
										<div id="qryResult" style="padding-top: 10px;">
											
										</div>										
									</div>
									<div><a href="#">Search for similar profiles</a></div>
									<div>
										<form class="visinsimple" id="simForm" action="javascript:void()">
											<select id="simSelect">
												<option selected="selected">Select gene</option>
											</select>
											<button type="submit">Search</button>
										</form>
										<div id="simHeader" style="padding-top: 10px;"></div>
										<div id="simResult" style="padding-top: 10px;"></div>										
									</div>
								</div>
							</td>
						</tr>
					</table>							
				</td>
			</tr>
		</table>
		</div>
					
		
		<jsp:include page='end_body.jsp' />