<%@page contentType="text/plain;encoding=UTF-8"%>
<%
String geneId = request.getParameter("gid");
int fromRow = -1;
try { fromRow = Integer.valueOf(request.getParameter("from")); } catch (Exception e) { }
int toRow = -1;
try { toRow = Integer.valueOf(request.getParameter("to")); } catch (Exception e) { }

String ef = request.getParameter("factor");
String efv = request.getParameter("efv");

if (geneId != null) {
    AtlasDao dao = ArrayExpressSearchService.instance().getAtlasDao();
    AtlasDao.AtlasGeneResult atlasGene = dao.getGeneByIdentifier(geneId);
    if(atlasGene.isFound()) {
        List<AtlasExperiment> exps = dao.getRankedGeneExperiments(atlasGene.getGene(), ef, efv,  fromRow, toRow);
        request.setAttribute("exps",exps);
        request.setAttribute("atlasGene", atlasGene.getGene());
    }
}    

%>
<%@page import="ae3.dao.AtlasDao"%>
<%@page import="ae3.model.AtlasExperiment"%>
<%@page import="ae3.service.ArrayExpressSearchService"%>
<%@page import="java.util.List"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->
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
		window.open("/microarray-as/aew/DW?queryFor=gene&gene_query=${atlasGene.geneIdentifier}&exp_query="+exp_acc,"_blank");
	}

	
</script>

<table align="left" cellpadding="0" >

<c:forEach var="exp" items="${exps}">

	<tr align="left" class="exp_header">
		<td align="left" nowrap="true" valign="top">
			${exp.dwExpAccession}:
		</td>
		<td align="left">
			${exp.dwExpDescription}
		</td>
		
	</tr>
	<tr>
		<td colspan="2">

		</td>
	</tr>

	<c:if test="${!empty exp.experimentFactors}">
	
		<tr align="left">
			<td colspan="2" >
			<div class="header" style="padding-top: 5px;padding-bottom: 5px; valign:middle" >
				<span>Experimental Factors</span>
					<div id="${exp.dwExpId}_EFpagination" class="pagination_ie" style="padding-top: 10px;">
					<c:forEach var="EF" items="${exp.experimentFactors}">
						<c:choose>
							<c:when test="${EF == exp.highestRankEFs[atlasGene.geneId]}">
								<span class="current" id="${EF}"><fmt:message key="head.ef.${EF}"/></span>
							</c:when>
							<c:otherwise>
								<a id="${EF}" onclick="redrawPlotForFactor('${exp.dwExpId}','${atlasGene.geneId}','${EF}',false)" ><fmt:message key="head.ef.${EF}"/></a>	
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</div>
			</div>
			</td>
		</tr>
	</c:if>

	
	<tr align="left">
		<td colspan="3">
		<table width="100%">
			<tr>

				<td valign="top" width="300px">
				<table>
					<!-- div style="position:relative"-->
					<tr align="left">
						<td align="center">
							<a  title="Show expression profile" href="<%= request.getContextPath() %>/experiment?eid=${exp.dwExpAccession}&gid=${atlasGene.geneId}" style="border:none;text-decoration:none;outline:none;"><div id="${exp.dwExpId}_${atlasGene.geneId}_plot" class="plot" style="width: 300px; height: 150px; background:url('<%= request.getContextPath()%>/images/indicator.gif'); background-repeat:no-repeat; background-position:center;" ></div></a>
							<div id="${exp.dwExpId}_${atlasGene.geneId}_plot_thm" > </div>
						</td>
					</tr>

					
					<!--
					<span class="moreLink" style="top: 5px;"
						onclick="showThumbnail('${exp.dwExpId}_${atlasGene.geneId}')">Click
					here to zoom</span>
					</td>
					</tr> -->
					<!--/div-->
				</table>
				</td>
				<td>
					<div style="overflow-y: auto; width:150px; height:150px" id="${exp.dwExpId}_${atlasGene.geneId}_legend"></div>
				</td>





			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			Show <a title="Show expression profile in detail" href="<%= request.getContextPath() %>/experiment/${exp.dwExpAccession}?gid=${atlasGene.geneId}">expression profile</a>
			&nbsp;/&nbsp;
			<a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=${exp.dwExpAccession}&detailedview=on">experiment details</a>
			<br/><br/>
		</td>
	</tr>

	<tr>
		<td colspan="3" style="border-bottom:1px solid #CDCDCD">&nbsp;</td>
	</tr>
	
	</c:forEach>

</table>

<img src="<%= request.getContextPath() %>/images/1.gif" onload="drawPlots();" /> 




