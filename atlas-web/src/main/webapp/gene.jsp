<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f"%>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="ae3.dao.AtlasDao"%>
<%@page import="java.util.*"%>
<%@page import="ae3.service.AtlasResultSet"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="ae3.model.AtlasTuple"%>
<%@page import="ae3.model.AtlasExperiment"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<c:set var="timeStart" value="${u:currentTime()}" />

<%
	AtlasGene atlasGene = null;
	String geneId = request.getParameter("gid");
	String noAtlasExps = null;
	if (geneId != null) {
		atlasGene = AtlasDao.getGeneByIdentifier(geneId);
		noAtlasExps = ArrayExpressSearchService.instance()
				.getNumOfAtlasExps(atlasGene.getGeneId());
	}

	if (request.getParameter("format") != null
			&& request.getParameter("format").equals("xml")) {
		//TODO: set this right (via REST WS perhaps)
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		Map<String, Collection<Object>> props = atlasGene
				.getGeneSolrDocument().getFieldValuesMap();
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
ArrayExpress Atlas Gene View -
<%=atlasGene.getGeneName()%>
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
	font-family: Verdana;
	font-weight: normal;
	font-size: 11px;
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
	font-size: 14pt;
	text-align: left;
}

.titleHeader {
	color: #1f1f1f;
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

.ac_over {
	background-color: #1f1f1f;
	color: white;
}
}
-->
</style>
<script src="scripts/jquery-1.2.6.js" type="text/javascript"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.js"></script><![endif]-->

<script language="javascript" type="text/javascript" src="scripts/jquery.flot.js"></script>
<script type="text/javascript" src="jquery.autocomplete.js"></script>
<script type="text/javascript" src="jquerydefaultvalue.js"></script>
<script type="text/javascript" src="scripts/jquery.pagination.js">



<!-- add header scripts here -->
<script type="text/javascript">
<!--
    function
            viewMore(id)
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

	function pageselectCallback(page_id, jq){
		var fromPage = (page_id*5) +1;
		var toPage = (page_id*5) + 5;
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,from:fromPage, to: toPage},drawPlots);
		$('#pagingSummary').text("Showing search results "+fromPage+"-"+toPage+" out of "+<%=noAtlasExps%>);
	}


	function FilterExps(ids,fv){
		$('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,exp_ids:ids},drawPlots);
		//$('#pagingSummary').text("Showing filtered results");
		$("#expHeader_td").text("Experiments showing differential expression for <%=atlasGene.getGeneName()%> in "+ fv);
		$("#Pagination").text("Return to all experiments");
	}

	function drawPlot(jsonObj, plot_id){
	   	if(jsonObj.series){
   			//alert('start of draw plot');
			var plot = $.plot($('#'+plot_id), jsonObj.series,jsonObj.options); 
			var overview;
			var allSeries = plot.getData();
			if(allSeries.length >10){
				var divElt = $('#'+plot_id+'_thm');
				divElt.width(300);divElt.height(50);
				overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']})); 
				//overview.setSelection({ xaxis: { from: 0, to: allSeries.length } });
				
			}
				
			
			
					
			$('#'+plot_id).bind("plotselected", function (event, ranges) {
        		// do the zooming
        		plot = $.plot($('#'+plot_id), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
           					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
                    	 	}));

				// don't fire event on the overview to prevent eternal loop
				//overview.setSelection(ranges, true);
    		});
    
				    $('#'+plot_id+'_thm').bind("plotselected", function (event, ranges) {
				        plot.setSelection(ranges);
				    });
				    
				    
			//Tooltip
			var previousPoint = null;
			$('#'+plot_id).bind("plothover", function (event, pos, item) {

        		if (item) {
          			if (previousPoint != item.datapoint) {
				          previousPoint = item.datapoint;
				                $("#tooltip").remove();
				                 var x = item.datapoint[0].toFixed(2),
				                     y = item.datapoint[1].toFixed(2);
				                 showTooltip(pos.pageX, pos.pageY,item.series.label,plot_id);
				                    
				                }
        		}else {
				          $("#tooltip").remove();
				           previousPoint = null;            
				       }
			});
        				
        			/*	
        			//Legend Colors
        			var allSeries = plot.getData();
        			var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			for (var i = 0; i < allSeries.length; ++i){
        				var color = allSeries[i].color;
        				var label = allSeries[i].label.toLowerCase().replace(/ /g,'');
        				
        				$("#"+eid+'_'+gid+'_'+label+'_td').css('background-color',color);
        			}*/	
   					 

				return plot;	
				}// if(o.series)
	}
 

	function bindMarkings(jsonObj,plot, plot_id){
	
					//Markings 
					var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			
        			//unbind from previous events bound to previous plots (different EFs) otherwise they keep adding up.
        			$("#"+eid+'_'+gid+'_tbl').find("input").unbind();
        			
					$("#"+eid+'_'+gid+'_tbl').find("input").click(function(){
						var tokens = this.value.split('_');
						var EFV=tokens[0];
						var EF=tokens[1];
						if(this.checked){
							markClicked(eid,gid,EF,EFV,plot,jsonObj);
						}
						else{
						
						plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1}
                      					}));
                      	overview.clearSelection();
                      	
						}
					});
	
	}

	function markClicked(eid,gid,ef,efv,plot,jsonObj){
								
						
		//alert($("#"+eid+'_'+gid+'_'+EFV+'_chk').checked);
		var plot_id = eid+'_'+gid+'_plot';
		var allSeries = plot.getData();
		var series;
		var markColor;
      	for (var i = 0; i < allSeries.length; ++i){
      		if(allSeries[i].label){
       		 	if(allSeries[i].label.toLowerCase()==efv.toLowerCase()){
       		 		series = allSeries[i];
       		 		markColor = series.color
       		 		break;
       	 		}
       	 	}
		}
						//alert('inside clicked ');
						if(series==null){
						//alert('series is null')
							redrawPlotForFactor(eid+'_'+gid+'_'+ef,true,efv);
							
							return null;
							}
						var data = series.data;
						var xMin= data[0][0]
						var xMax= data[data.length-1][0]
						
						//var seriesoptions = $.extend(true,{},series.lines,{show:true,lineWidth:1, fill:true})
						
						//var testSeries=[];
						
						//testSeries.push(series);
						////allSeries.push(testSeries);
						//var modSeries = $.extend(true,{},series,{lines:{show:true,lineWidth:1, fill:true}})
						//alert(testSeries);
						
						//plot = $.plot($('#'+plot_id), allSeries,o.options);
						var overviewDiv = $('#'+plot_id+'_thm');
						if(allSeries.length>10 && data.length<5){
						
						
							
                      		//showThumbnail(eid+'_'+gid);
                      		
                      		
                      		if(overviewDiv.height()!=0){
                      		overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,jsonObj.options);
				
                      		overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
                      		}
                      		plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#e8cfac' }]},
                          				xaxis: { min: xMin-10, max: xMax+10 }
                      					}));
						}
						else{
						
						plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#e8cfac' }]}
                      					}));
                      					if(overviewDiv.height()!=0){
                      		overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']})); 
				
                      		overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
                      		}
                      	}
	}

	function drawPlots(){

			$(".plot").each(function(){
        	var plot_id = this.id;
        	var tokens = plot_id.split('_');
        	var eid = tokens[0];
        	var gid = tokens[1]; 
        	$.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid,
   			
   			dataType:"json",
   			
   			success: function(o){
   				var plot = drawPlot(o,plot_id);//success
   				bindMarkings(o,plot,plot_id);
   				}
 			});//ajax
        	
        }); //each



	}//drawPlots
		
		
    jQuery(document).ready(function()
    {
       				
        $('#ExperimentResult').load("AtlasExpResults.jsp",{gid:<%=atlasGene.getGeneId()%>,from:"1", to:"5"},drawPlots);
        
        
        
        $('#pagingSummary').text("Showing search results "+((0*5)+1)+"-"+((0*5)+5)+" out of "+<%=noAtlasExps%>);
        
        
        $("#q_gene").defaultvalue("(all genes)");
        $("#q_expt").defaultvalue("(all conditions)");

        $("#searchSlider").click(function() {
            $("#panel").slideToggle("slow");
            $(this).toggleClass("active");
            return false;
        });


		// Create pagination element
        $("#Pagination").pagination(<%=noAtlasExps%>, {
			num_edge_entries: 2,
			num_display_entries: 5,
			items_per_page:5,
            callback: pageselectCallback
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
<jsp:include page='start_body_no_menus.jsp' />
<jsp:include page='end_menu.jsp' />
<div class="pagecontainer">
<table width="100%" style="border-bottom: thin solid lightgray"
	cellpadding="0" cellspacing="0">
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

		<td class="titleHeader" valign="middle">ArrayExpress Atlas Gene
		View</td>
		<td align="right" valign="top">
		<div
			style="color: #e33e3e; text-align: right; font-size: 26pt; margin-top: 15px; font-weight: bold; vertical-align: top">
		<%=atlasGene.getGeneName()%></div>
		<span
			style="margin-top: 0; text-align: right; color: #1f7979; font-size: 9pt; font-weight: bold;">
		<%=atlasGene.getGeneSpecies()%> </span></td>
	</tr>

</table>

<table width="100%">

	<!--
    <tr>
        <td class="geneAnnotHeader">Description:</td>
        <td align="left">NF-kapp-B inhibitor alpha</td>

    </tr>
    <tr>
        <td></td>
        <td>
        <div class="separator"></div>
        </td>
    </tr>
-->
	<tr>
		<td class="geneAnnotHeader">Synonyms:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
							"gene_synonym").toString().replace(']', ' ')
							.replace('[', ' ')%></td>
	</tr>
	<%
		Collection uniprots = atlasGene.getGeneSolrDocument()
				.getFieldValues("gene_uniprot");
		if (uniprots != null && uniprots.size() > 0) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">Uniprot:</td>
		<td align="left">
		<%
			for (Object uniprot : uniprots) {
		%> <a href="http://www.uniprot.org/uniprot/<%=uniprot%>"><%=uniprot%></a>&nbsp;
		<%
			}
		%>
		</td>
	</tr>
	<%
		}
	%>

	<%
		if (atlasGene.getGeneSolrDocument().getFieldValue(
				"gene_interproterm") != null) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">InterPro Term:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
								"gene_interproterm").toString().replace(']',
								' ').replace('[', ' ')%></td>

	</tr>
	<%
		}
	%>
	<%
		if (atlasGene.getGeneSolrDocument().getFieldValue("gene_disease") != null) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">Diseases:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
								"gene_disease").toString().replace(']', ' ')
								.replace('[', ' ')%></td>
		<td></td>

	</tr>
	<%
		}
	%>
	<%
		if (atlasGene.getGeneSolrDocument().getFieldValue("gene_goterm") != null) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">GO Terms:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue("gene_goterm").toString().replace(']', ' ').replace('[', ' ')%></td>
	</tr>
	<%
		}
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
		<td></td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">Cross Refs:</td>
		<td align="left"><a title="Show gene annotation" target="_blank"
			href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=<%=atlasGene.getGeneSolrDocument().getFieldValue(
								"gene_identifier").toString()%>"><%=atlasGene.getGeneSolrDocument().getFieldValue(
							"gene_identifier").toString()%> </a></td>
		<td width="8%">&nbsp;</td>

	</tr>

	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>
</table>






<table width="100%" cellspacing="5" cellpadding="10">
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
	
				<%
					AtlasResultSet atlasResultSet = AtlasGeneService.getExprSummary(atlasGene.getGeneId());
				%>
	
				<tr>
					<td>
					<div>
					<table border="1" class="heatmap" cellpadding="3" cellspacing="0"
						bordercolor="#ffffff">
						<tr>
							<th rowspan="2">Factor Value</th>
							<th rowspan="2"	style="border-right: medium solid; border-left: thin">Studies</th>
							<%--<th><img src="tmp/<%=VerticalTextRenderer.drawString("Total up", application.getRealPath("tmp"))%>" title="Total up"/></th>--%>
							<%--<th style="border-right: thick solid"><img src="tmp/<%=VerticalTextRenderer.drawString("Total down", application.getRealPath("tmp"))%>" title="Total down"/></th>--%>
							<%
								List<HashMap> genes = atlasResultSet.getAtlasResultGenes();
								for (HashMap<String, String> gene : genes) {
							%>
							<th style="border-right: medium solid" colspan="2" align="center"><%=gene.get("gene_name")%>
							</th>
							<%
								}
							%>
						</tr>
						<tr>
							<th style="border-left: medium solid">UP</th>
							<th style="border-right: medium solid">DN</th>
						</tr>
	
						<%
							HashMap<String, HashMap<String, String>> gars = atlasResultSet.getAtlasResultAllGenesByEfv();
							for (HashMap<String, String> ar : atlasResultSet.getAtlasEfvCounts()) {
								if (!ar.get("efv").startsWith("V1") && !ar.get("efv").contains("(time)") && !ar.get("efv").contains("(dose)") && !ar.get("efv").contains("(age)")) {
						%>
						<tr>
							<td nowrap="true">
								<span style="font-weight: bold"	title="<%=ar.get("efv")%> Matched in experiment(s) <%=ar.get("experiments")%>" >
									<a class="pagination_ie" onclick="FilterExps('<%=ar.get("exp_ids")%>','<%=ar.get("efv")%>')"> <%=ar.get("efv").length() > 50 ? ar.get("efv").substring(0, 50) + "..." : ar.get("efv")%> </a> 
								</span>
							</td>
							<td style="border-right: medium solid" align="right"><b><%=ar.get("experiment_count")%>
							</b></td>
	
							<%
								for (HashMap<String, String> gene : genes) {
											HashMap<String, String> gar = gars.get(gene.get("gene_identifier") + ar.get("efv"));
	
											if (gar != null && gar.size() != 0) {
												Long r_dn = 255L;
												Long b_dn = 255L;
												Long g_dn = 255L;
	
												Long r_up = 255L;
												Long b_up = 255L;
												Long g_up = 255L;
												String mpvup = gar.get("mpvup");
												String mpvdn = gar.get("mpvdn");
	
												String countup = gar.get("countup").equals("0") ? " "
														: gar.get("countup");
												String countdn = gar.get("countdn").equals("0") ? " "
														: gar.get("countdn");
	
												String display = "";
												String display_up = "";
												String display_dn = "";
												String title = "Probes for "
														+ gene.get("gene_identifier")
														+ " found in experiment(s) "
														+ gar.get("experiment_count")
														+ ", observed up "
														+ (countup == null ? 0 : countup)
														+ " times (mean p="
														+ (mpvup == null ? "N/A" : String.format(
																"%.3g", Double.valueOf(mpvup)))
														+ ")"
														+ ", observed down "
														+ (countdn == null ? 0 : countdn)
														+ " times (mean p="
														+ (mpvdn == null ? "N/A" : String.format(
																"%.3g", Double.valueOf(mpvdn)))
														+ ")";
	
												if (mpvup == null && mpvdn == null) {
													r_up = g_up = b_up = g_dn = r_dn = b_dn = 255L;
												}
												if (mpvdn != null) {
													b_dn = 255L;
													g_dn = 255 - Math.round(Double.valueOf(mpvdn)
															* (-255D / 0.05D) + 255);
													r_dn = 255 - Math.round(Double.valueOf(mpvdn)
															* (-255D / 0.05D) + 255);
													display_up = "0";
													display_dn = countdn;
												}
												if (mpvup != null) {
													r_up = 255L;
													g_up = 255 - Math.round(Double.valueOf(mpvup)
															* (-255D / 0.05D) + 255);
													b_up = 255 - Math.round(Double.valueOf(mpvup)
															* (-255D / 0.05D) + 255);
													display_up = countup;
													display_dn = "0";
												}
							%>
							<td align="center"
								style="background-color: rgb(<%=         r_up %>, <%=         g_up %>, <%=         b_up %>)"><span
								title="<%=title%>"
								style="text-decoration: none; font-weight: bold; color: lightgray"><%=countup == "" ? " " : countup%></span>
							</td>
							<td align="center"
								style="background-color: rgb(<%=         r_dn %>, <%=         g_dn %>, <%=         b_dn %>); border-right: medium solid"><span
								title="<%=title%>"
								style="text-decoration: none; font-weight: bold; color: lightgray;"><%=countdn == "" ? " " : countdn%></span>
							</td>
							<%
								} else {
							%>
							<td>&nbsp;</td>
							<%
								}
							%>
							<%
								}
							%>
	
						</tr>
						<%
							}
							}
						%>
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
			<table>
			<tr>
				<td id="expHeader_td" class="sectionHeader">Experiments showing Differential expression for <%=atlasGene.getGeneName()%>
				</td>
				<td>
				<div id="Pagination" class="pagination_ie"></div>
				</td>
			</tr>
			<tr>
					<td colspan="2">
					<div class="separator"></div>
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
