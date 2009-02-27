<%@page contentType="text/plain;encoding=UTF-8"%>
<%
AtlasGene atlasGene = null;
String geneId = request.getParameter("gid");
String fromRow = request.getParameter("from");
String toRow = request.getParameter("to");
String efv = request.getParameter("efv");
if (geneId != null) {
    atlasGene = AtlasDao.getGeneByIdentifier(geneId);
}    
ArrayList<AtlasExperiment> exps = ArrayExpressSearchService.instance().getRankedGeneExperiments(geneId, efv, fromRow, toRow);
request.setAttribute("exps",exps);
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="java.util.HashSet"%>

<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.pack.js"></script><![endif]-->
<script type="text/javascript">
<!--

	var exp_ids;
	var exps;
	exps= [ <c:forEach var="exp" varStatus="s" items="${exps}">{
				id: '${exp.dwExpId}',
				acc: '${exp.dwExpAccession}'
				}<c:if test="${!s.last}">,</c:if>
			</c:forEach>
			];
	
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
    
    
    
    
    
//-->

	function showThumbnail(id){
        var thumb = $("#" + id + '_plot_thm');
        var legend_ext = $("#" + id + '_legend_ext');
        var legend = $("#" + id + '_legend');
        thumb.show();
        legend.hide();
        legend_ext.show();
	}
	
	function openInAEW(eid){
		for (var i = 0; i < exps.length; ++i){
			if(eid == exps[i].id)
				exp_acc = jQuery.trim(exps[i].acc);
		}
		window.open("http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=<%=atlasGene.getGeneIdentifier()%>&exp_query="+exp_acc,"_blank");
	}

	
</script>

<table align="left" cellpadding="0">

	<% int c = 0;
                    for (AtlasExperiment exp : exps) {
                        c++;
                %>
	

	<tr align="left" class="exp_header">
		<td align="right" nowrap="true" valign="top">
			<%=exp.getDwExpAccession().trim()%>:
		</td>
		<td align="left">
			<%=exp.getDwExpDescription().trim()%>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			<div class="separator"></div>
		</td>
	</tr>

	<%if (!exp.getExperimentFactors().isEmpty()) { %>
	<tr align="left">
		<td colspan="3" >
		<div class="header" style="padding-top: 5px;padding-bottom: 0px; valign:middle" >
			<span>Experimental Factors</span>
				<div id="<%=exp.getDwExpId().toString()%>_EFpagination" class="pagination_ie" style="padding-top: 15px;">
				<%HashSet<String> EFset = exp.getExperimentFactors();
                	for(String EF: EFset){
                		request.setAttribute("PlotEF",EF);
                		if(EF.equals(exp.getHighestRankEF(atlasGene.getGeneId()))){%>
                	<span class="current" id="${PlotEF}">
                		<fmt:message key="head.ef.${PlotEF}"/>
                	</span>
                 		<%}else{%>
                 	<a id="${PlotEF}" onclick="redrawPlotForFactor('<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_<%=EF%>',false)" >
							<fmt:message key="head.ef.${PlotEF}"/> 
					</a>						
					 
				<%}}%>
		</div>
		</div>

		</td>
	</tr>
	<%} %>


	
	<tr align="left">
		<td colspan="3">
		<table width="100%">
			<tr>

				<td valign="top" width="300px">
				<table>
					<!-- div style="position:relative"-->
					<tr align="left">
						<td align="left">
							<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_plot" class="plot" style="width: 300px; height: 150px;"></div>
							<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_plot_thm"> </div>
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
				<td>
					<div id="<%=exp.getDwExpId()%>_<%=atlasGene.getGeneId()%>_legend_ext"></div>
				</td>





			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			Show <a target="_blank" title="Show expression profile in ArrayExpress Warehouse" href="/microarray-as/aew/DW?queryFor=gene&gene_query=<%=atlasGene.getGeneIdentifier()%>&exp_query=<%=exp.getDwExpAccession().trim()%>">expression profile</a>
			&nbsp;/&nbsp;
			<a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=<%=exp.getDwExpAccession().trim()%>&detailedview=on">experiment details</a>
		</td>
	</tr>

	<tr>
		<td colspan="3">
		<div class="separator"></div>
		</td>
	</tr>
<%
    }
%>


</table>
