<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>

<c:choose>
	<c:when test="${!empty geneList}">
		<table id="grid" cellspacing="0" cellpadding="5" width="100%">
	<thead>
		<tr>
			<th/>
			<th style="border-bottom:1px solid #CDCDCD;padding-left:3px">Gene</th>
			<th style="border-bottom:1px solid #CDCDCD">Organism</th>
			<th style="border-bottom:1px solid #CDCDCD">Condition</th>
			<th style="border-bottom:1px solid #CDCDCD">Expression</th>
		</tr>
	</thead>

	<tbody>
		<c:forEach var="row" items="${geneList}" varStatus="r">
			<tr id="${row.gene_id}_${row.ef}_${u:escapeURL(row.fv)}">
				<td style="vertical-align: top;padding-top:2px"><a href="#" onclick="addGeneToPlot('${row.gene_id}','${u:escapeJS(row.gene_identifier)}','${u:escapeJS(row.gene_name)}','${u:escapeJS(row.ef)}');return false;" alt="plot" title="Click to plot profile">
				<img border="0" src="${pageContext.request.contextPath}/images/iconf.png" alt="" />
				</a>
				</td>
				<td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px; white-space:nowrap"
					class="padded" ><a class="genename" href="${u:GeneUrl(pageContext.request, row.gene.geneIdentifier)}">${row.gene_name}</a>
					<div class="gtooltip" style="display: none;">
						<div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:if test="${!empty row.gene.synonym}">${row.gene.hilitSynonym},</c:if>${row.gene.geneIdentifier})</div>
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
					<c:if test="${e.experimentId == eid}">
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
			</tr>

		</c:forEach>
	</tbody>
</table>
	</c:when>
	<c:otherwise>
		<c:out value="No genes matched your query"></c:out>
	</c:otherwise>
 </c:choose>




