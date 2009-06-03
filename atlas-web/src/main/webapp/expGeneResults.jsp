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
}

	function pageselectCallback(page_id, jq){
		var fromPage = (page_id * ${result.rowsPerPage});
		
		$('#topGenes').load("expGenes",{eid:'${eid}',eAcc:'${eAcc}',gid:'${gid}',query:'top',from:fromPage});

	}
</script>

<table id="grid" "class="tablesorter" cellspacing="0" cellpadding="5"	width="100%">
	<thead>
		<tr>
			<th></th>
			<th>Gene</th>
			<th>Organism</th>
			<th>Condition</th>
			<th>Expression</th>
			<c:if test="${!empty simRS}"><th>Sim. score</th></c:if>
		</tr>
	</thead>

	<tbody>

		<c:forEach var="row" items="${genes}" varStatus="r">

			<tr id="${row.gene_id}_${row.ef}_${u:escapeURL(row.fv)}">
				<td style="vertical-align: middle;"><img src="images/iconf.png"
					alt="plot" title="Click to plot profile"
					onclick="addGeneToPlot('${row.gene_id}','${row.gene_name}','${eid}','${row.ef}')" />
				<td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px"
					class="padded "><a class="genename" href="#"
					onclick="javascript:window.open('gene?gid=${f:escapeXml(row.gene.geneIdentifier)}')">${row.gene_name}</a>
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
				<td style="border-bottom: 1px solid #CDCDCD">${row.gene_species}</td>
				
				<td style="border-bottom: 1px solid #CDCDCD">${row.fv}</td>
				<c:forEach var="e" items="${row.exp_list}">
					<c:if test="${e.experimentAccession == eAcc}">
						<c:choose>
							<c:when test="${e.updn == 'UP'}">
								<td
									style="border-bottom: 1px solid #CDCDCD; color: red; text-align: left">&#8593;&nbsp;<fmt:formatNumber
									value="${e.pvalue}" pattern="#.###E0" /></td>
							</c:when>
							<c:otherwise>
								<td
									style="border-bottom: 1px solid #CDCDCD; color: blue; text-align: left">&#8595;&nbsp;<fmt:formatNumber
									value="${e.pvalue}" pattern="#.###E0" /></td>
							</c:otherwise>
						</c:choose>
					</c:if>
				</c:forEach>
				<c:if test="${!empty simRS}">
					<td style="border-bottom:1px solid #CDCDCD"><fmt:formatNumber value="${simRS.scores[row.gene_id]}" pattern="#.###E0" /></td>
				</c:if>
			</tr>

		</c:forEach>
	</tbody>
	<tfoot>
		<input id="gid" type="hidden" name="geneid" value="${gid}">
	</tfoot>
</table>
