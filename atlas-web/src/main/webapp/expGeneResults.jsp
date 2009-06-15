<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<script type="text/javascript">
function initPaging(){
	$("#Pagination").pagination(${result.total}, {
				num_edge_entries: 2,
				num_display_entries: 5,
				items_per_page:5,
            	callback: pageselectCallback
         	});
         	
//    $("#grid").tablesorter({headers: { 0: { sorter: false}}});

}

	function pageselectCallback(page_id, jq){
		var fromPage = (page_id * ${result.rowsPerPage});
		//$('#topGenes').html("<img src='images/indicator.gif' />");
		$('#topGenes').load("expGenes",{eid:'${eid}',eAcc:'${eAcc}',gid:'${gid}',query:'top',from:fromPage}, function(){
			 $("#grid").tablesorter({headers: { 0: { sorter: false}}});
			//$('#topGenes').fadeIn("slow");
		});

	}
</script>

<c:choose>
	<c:when test="${!empty genes}">
		<table id="grid" cellspacing="0" cellpadding="5" width="100%">
	<thead>
		<tr>
			<th/>
			<th style="border-bottom:1px solid #CDCDCD;padding-left:3px">Gene</th>
			<th style="border-bottom:1px solid #CDCDCD">Organism</th>
			<th style="border-bottom:1px solid #CDCDCD">Condition</th>
			<th style="border-bottom:1px solid #CDCDCD">Expression</th>
<!--			<c:if test="${!empty simRS}"><th>Sim. score</th></c:if>-->
		</tr>
	</thead>

 <%
	java.util.Collection<ae3.service.ListResultRow> f_genes = new java.util.ArrayList<ae3.service.ListResultRow>();
  java.util.HashSet<String> h = new java.util.HashSet<String>();
  for(ae3.service.ListResultRow row : (java.util.Collection<ae3.service.ListResultRow>) request.getAttribute("genes")) {
    if(!h.contains(row.getGene().getGeneIdentifier())) f_genes.add(row);
		h.add(row.getGene().getGeneIdentifier());
  }
  pageContext.setAttribute("f_genes",f_genes);
 %>

	<tbody>
		<c:forEach var="row" items="${f_genes}" varStatus="r">
			<tr id="${row.gene_id}_${row.ef}_${u:escapeURL(row.fv)}">
				<td style="vertical-align: top;padding-top:2px"><a href="#" onclick="addGeneToPlot('${row.gene_id}','${row.gene_name}','${eid}','${row.ef}')" alt="plot" title="Click to plot profile">
				<img border="0" src="images/iconf.png"/>
				</a>
				</td>
				<td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px"
					class="padded" ><a class="genename" href="gene?gid=${f:escapeXml(row.gene.geneIdentifier)}">${row.gene_name}</a>
					<div class="gtooltip" style="display: none;">
						<div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:if test="${!empty row.gene.synonyms}">${row.gene.hilitSynonyms},</c:if>${row.gene.geneIdentifier})</div>
						<c:if test="${!empty row.gene.keyword}">
							<b>Keyword:</b> ${row.gene.hilitKeyword}<br>
						</c:if> <c:if test="${!empty row.gene.goTerm}">
							<b>Go Term:</b> ${row.gene.hilitGoTerm}<br>
						</c:if> <c:if test="${!empty row.gene.interProTerm}">
							<b>InterPro Term:</b> ${row.gene.hilitInterProTerm}<br>
						</c:if>
					</div>
				</td>
				<td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">${row.gene_species}</td>
				
				<td style="border-bottom: 1px solid #CDCDCD">${row.fv}</td>
				<c:forEach var="e" items="${row.exp_list}">
					<c:if test="${e.experimentAccession == eAcc}">
						<c:choose>
							<c:when test="${e.updn == 'UP'}">
								<td
									style="border-bottom: 1px solid #CDCDCD; color: red; text-align: left">&#8593;&nbsp;<fmt:formatNumber
									value="${e.pvalue}" pattern="#.##E0" /></td>
							</c:when>
							<c:otherwise>
								<td
									style="border-bottom: 1px solid #CDCDCD; color: blue; text-align: left">&#8595;&nbsp;<fmt:formatNumber
									value="${e.pvalue}" pattern="#.##E0" /></td>
							</c:otherwise>
						</c:choose>
					</c:if>
				</c:forEach>
<!--				<c:if test="${!empty simRS}">
					<td style="border-bottom:1px solid #CDCDCD"><fmt:formatNumber value="${simRS.scores[row.gene_id]}" pattern="#.##E0" /></td>
				</c:if>-->
			</tr>

		</c:forEach>
	</tbody>
	<tfoot>
		<input id="gid" type="hidden" name="geneid" value="${gid}">
	</tfoot>
</table>
	</c:when>
	<c:otherwise>
		<c:out value="No genes matched your query"></c:out>
	</c:otherwise>
 </c:choose>




