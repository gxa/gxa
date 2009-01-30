<%@page contentType="text/plain;encoding=UTF-8"%>
<%
AtlasGene atlasGene = null;
String geneId = request.getParameter("gid");
String fromRow = request.getParameter("from");
String toRow = request.getParameter("to");
if (geneId != null) {
    atlasGene = AtlasDao.getGeneByIdentifier(geneId);
}    
ArrayList<AtlasExperiment> exps = ArrayExpressSearchService.instance().getRankedGeneExperiments(geneId,fromRow, toRow);
%>
<%@page import="java.util.ArrayList"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<%@page import="ae3.model.AtlasExperiment"%>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="ae3.dao.AtlasDao"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="ae3.model.AtlasTuple"%>
<%@page import="java.util.List"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@page import="java.util.HashSet"%>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.pack.js"></script><![endif]-->
<script type="text/javascript">
<!--
    function viewMore(id)
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
       // onWindowResize();
    }
    function showStudyDetails(id) {
        id = String(id);
        var divElt = $("#" + id + "_desc_ext");
        var lnkElt = $("#" + id + "_std_lnk");
       
        divElt.slideToggle("fast");
        if (lnkElt.hasClass("expanded")) {
            lnkElt.removeClass("expanded");
            lnkElt.attr("src", "images/minus.gif");
        } else {
            lnkElt.addClass("expanded");
            lnkElt.attr("src", "images/plus.gif");
        }
    }
    
    function showTooltip(x, y, contents, plot_id) {
		if(contents!='Mean'){
 		
        $('<div id="tooltip">' + contents + '</div>').css( {
            position: 'absolute',
            display: 'none',
            top: 70,
            left: x-40,
            border: '1px solid #fdd',
            padding: '2px',
            'background-color': '#fee',
            opacity: 0.80
        }).appendTo("#"+plot_id).fadeIn(200);
    }
    }
    
    function redrawPlotForFactor(id,mark,efv){
    	var id = String(id);
        var tokens = id.split('_');
        var eid = tokens[0];
        var gid = tokens[1];
        var ef = "ba_"+tokens[2];
        ef = ef.toLowerCase();
        var plot_id = eid+"_"+gid+"_plot";

       //$('#'+eid+'_'+gid+'_legend').empty();
            $.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid+"&ef="+ef,
   			dataType:"json",
   			success: function(o){
   				var plot = drawPlot(o,plot_id);
				bindMarkings(o,plot,plot_id);
				if(mark){
					markClicked(eid,gid,ef,efv,plot,o);
				}
			}
 			});
 			drawEFpagination(eid,gid,tokens[2]);
    }
    
    function drawEFpagination(eid,gid,currentEF){
    	var panelContent = [];
    	
    	var EFs = $("#"+eid+"_EFpagination *").each(function(){
    		var ef = $(this).html().toString();
    		ef = jQuery.trim(ef);
    					if(ef == currentEF){
    						panelContent.push("<span class='current'>"+ef+"</span>")
    					}
    					else{
    						panelContent.push('<a id="'+eid+'_'+gid+'_'+ef+ '" onclick="redrawPlotForFactor( \''+eid+'_'+gid+'_'+ef+'\',false)">'+ ef +' </a>');
    					}
    					
    					});
    				
    					
    	$("#"+eid+"_EFpagination").empty();
    	$("#"+eid+"_EFpagination").html(panelContent.join(""));


    	
    	
    	
    }
    
//-->

	function showThumbnail(id){
        var thumb = $("#" + id + '_plot_thm');
        var legend_ext = $("#" + id + '_legend_ext');
        var legend = $("#" + id + '_legend');
        thumb.show();
        legend.hide();
        legend_ext.show();
	}

	
</script>

<table align="left" >

	<% int c = 0;
                    for (AtlasExperiment exp : exps) {
                        c++;
                %>
	<tr>
		<td colspan="3">
		<div class="separator"></div>
		</td>
	</tr>

	<tr align="left">
		<td align="left" nowrap="true" valign="top">
		<h3><%=exp.getDwExpAccession().trim()%>:</h3>
		</td>
		<td align="left">
		<h3><%=exp.getAerExpName()%></h3>
		</td>
		<td width="20px"><img style="cursor: pointer"
			title="Show study details"
			id="<%=exp.getDwExpId().toString()%>_std_lnk" src="images/minus.gif"
			onclick="showStudyDetails(<%=exp.getDwExpId().toString()%>)" /></td>
	</tr>
	<tr>
		<td colspan="3">
		<div style="display: block"
			id="<%=exp.getDwExpId().toString()%>_desc_ext">
		<table cellpadding="2" cellspacing="2">
			<tr>
				<td align="right">Title:</td>
				<td align="left"><%=exp.getTitle()%></td>
			</tr>
			<tr>
				<td align="right" valign="top">Summary:</td>
				<td align="left"><%=exp.getAerExpDescription()%></td>

			</tr>
			<tr>
				<td colspan="3">
					<div class="separator"></div>
				</td>
			</tr>
		</table>
		</div>
		</td>
	</tr>

	<%if (!exp.getExperimentFactors().isEmpty()) { %>
	<tr align="left">
		<td colspan="3" >
		<div style="color: #5e5e5e; padding-top: 3px;padding-bottom: 3px;" >
			<span>Experimental Factors:</span>
				<span id="<%=exp.getDwExpId().toString()%>_EFpagination" class="pagination_ie" style="padding-top: 5px; "><%HashSet<String> EFset = exp.getExperimentFactors();
                	for(String EF: EFset){if(EF.equals(exp.getHighestRankEF(atlasGene.getGeneId()))){%><span class="current"><%=EF%></span>
                 		<%}else{%>
                 	<a id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=EF%>" 
								onclick="redrawPlotForFactor('<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=EF%>',false)" >
							<%=EF%> </a>						
					 
				<%}}%>
		</span>
		</div>

		</td>
	</tr>
	<%} %>



	<tr>
		<td colspan="3">
		<table width="100%">
			<tr>

				<td valign="top" width="300px">
				<table>
					<!-- div style="position:relative"-->
					<tr>
						<td>
							<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_plot" class="plot" style="width: 300px; height: 150px;"></div>
							<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_plot_thm"> </div>
						</td>
					</tr>

					<tr>
						<td>
						
						</td>
					</tr>
					<tr>
						<td>
						<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_legend" style=" left: 30px;"></div>
						</td>
					</tr>
					<tr>
						<td>
						<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_legend_ext" style=" left: 30px; display: none"></div>
						</td>
					</tr>
					<!--
					<span class="moreLink" style="top: 5px;"
						onclick="showThumbnail('<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>')">Click
					here to zoom</span>
					</td>
					</tr> -->
					<!--/div-->
				</table>
				</td>

				<td align="left"   valign="top"><!-- div class="exp_summary"-->
				<table class="heatmap" cellpadding="2" border="1" id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_tbl"
					bordercolor="#ffffff" style="border-style: dotted; top: 10px">
					<tr><td colspan="3">Top conditions showing differential expression</td></tr>
					<tr>
						<th class="subheading">Mark</th>
						<th class="subheading">Factor Value</th>
						<th class="subheading">P-value</th>
						<!--th class="subheading">Text Summary</th-->
					</tr>
					<%
										                List<AtlasTuple> atlusTuples = AtlasGeneService.getAtlasResult(atlasGene.getGeneId(), exp.getDwExpId().toString());
										                int i = 0;
										                for (AtlasTuple atuple : atlusTuples) {
										                    if (!atuple.getEfv().startsWith("V1")) {
										                        i++;
										                        if (i < 11) {
										            %>
					<tr>
						<td align="center" id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=atuple.getEfv().toLowerCase().replaceAll(" ","")%>_td" ><input type="radio" id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=atuple.getEfv()%>_chk" name="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>" value="<%=atuple.getEfv()%>_<%=atuple.getEf()%>" class="markBox"></input></td>
						<td><%=atuple.getEfv()%></td>
						<td valign="top" nowrap="true">
						<%if (atuple.getUpdn().equals(1)) {%><img src="images/up_arrow.gif"
							align="top"> <%} else if (atuple.getUpdn().equals(-1)) {%><img
							src="images/dn_arrow.gif" align="top">
						<%}%> <%=atuple.getPval() > 1e-16D ? String.format("%.3g", atuple.getPval()) : "< 1e-16"%>
						</td>
						<!--td><%=atlasGene.getGeneName() + " " + atuple.getTxtSummary()%></td-->
					</tr>
					<%
										            } else {
										                if (i == 11) {%>
				<!--
					<tr>
						<td id="<%=exp.getDwExpId().toString()%>_atls_lnk_more"
							colspan="3" align="center"><span class="moreLink"
							onclick="viewMore('<%=exp.getDwExpId().toString() %>')">View
						more results</span></td>
					</tr> -->
				</table>
				<div id="<%=exp.getDwExpId().toString()%>_ext"
					class="fullSectionView" style="display: none"">
				<table class="heatmap" cellpadding="2" border="1" bordercolor="#ffffff" style="border-style: dotted" id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_tbl_ext">
					<%} %>
					<tr>
						<td align='center' id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=atuple.getEfv().toLowerCase().replaceAll(" ","")%>_td"><input type="checkbox" id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=atuple.getEfv()%>_chk" name="<%=atuple.getEfv()%>_<%=atuple.getEf()%>" class="markBox"></input></td>
						<td><%=atuple.getEfv()%></td>
						<td valign="top" nowrap="true">
						<%if (atuple.getUpdn().equals(1)) {%><img src="images/up_arrow.gif"
							align="top"> <%} else if (atuple.getUpdn().equals(-1)) {%><img
							src="images/dn_arrow.gif" align="top">
						<%}%> <%=atuple.getPval() > 1e-16D ? String.format("%.3g", atuple.getPval()) : "< 1e-16"%>
						</td>
						<!--td><%=atlasGene.getGeneName() + " " + atuple.getTxtSummary()%></td-->
					</tr>

					<%  }
										                }
										            }
										            if (i >= 11) {%>
					<tr>
						<td colspan="3"
							id="<%=exp.getDwExpId().toString()%>_atls_lnk_less" align="right">
						<img style="cursor: pointer" title="Collapse"
							id="<%=exp.getDwExpId().toString()%>_atls_lnk_less"
							src="images/minus_up.gif"
							onclick="viewMore(<%=exp.getDwExpId().toString()%>)" /></td>
					</tr>
				</table>
				</div>
				<%}else{%>
				
		</table>
		<%} %> <!-- /div--></td>
	</tr>
</table>
</td>
</tr>
<%
    }
%>


</table>