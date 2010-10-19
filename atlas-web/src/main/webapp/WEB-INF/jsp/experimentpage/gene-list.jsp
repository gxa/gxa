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
<div style="padding:20px;">
<c:choose>
	<c:when test="${!empty geneList}">
		<table id="grid" cellspacing="0" cellpadding="5" width="100%">
	<thead>
		<tr>
            <th style="border-bottom:1px solid #CDCDCD;padding-left:3px">Plot</th>
            <th style="border-bottom:1px solid #CDCDCD">Probe</th>
			<th style="border-bottom:1px solid #CDCDCD">Gene</th>
            <th style="border-bottom:1px solid #CDCDCD">Factor</th>
			<th style="border-bottom:1px solid #CDCDCD">Value</th>
            <th style="border-bottom:1px solid #CDCDCD">UP/DOWN</th>
            <th style="border-bottom:1px solid #CDCDCD">Fold Change</th>
            <th style="border-bottom:1px solid #CDCDCD">T-Statistic</th>
            <th style="border-bottom:1px solid #CDCDCD">Effect Size</th>
            <th style="border-bottom:1px solid #CDCDCD">P-value</th>
		</tr>
	</thead>

	<tbody>
        <tr>
        <td>plot</td>
        <td>probe</td>
        <td>gene</td>
        <td>factor</td>
        <td>value</td>
        <td>updown</td>
        <td>fold</td>
        <td>tstat</td>
        <td>eff</td>
        <td>pval</td>
        </tr>
		<c:forEach var="row" items="${geneList}" varStatus="r">
			<tr id="${row.gene_id}_${row.ef}_${u:escapeURL(row.fv)}">
				<td style="vertical-align: top;padding-top:2px;border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    <a href="#" onclick="addGeneToPlot('${row.gene_id}','${u:escapeJS(row.gene_identifier)}','${u:escapeJS(row.gene_name)}','${u:escapeJS(row.ef)}','${u:escapeJS(row.designElement)}');return false;" alt="plot" title="Click to plot profile">
				    <img border="0" src="${pageContext.request.contextPath}/images/iconf.png" alt="" />
				    </a>
				</td>
                <td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px; white-space:nowrap">
                    ${row.designElement}
                </td>
				<td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px; white-space:nowrap" class="padded" >
                    <a class="genename" href="${pageContext.request.contextPath}/gene/${row.gene.geneIdentifier}">${row.gene_name}</a>
					<div class="gtooltip" style="display: none;">
                        <div class="genename"><b>${row.gene.hilitGeneName}</b></div>
					</div>
				</td>
                <td style="border-bottom: 1px solid #CDCDCD; padding-left: 4px;">
                    ${row.ef}
                </td>
				<td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
				    ${row.fv}
                </td>
                <td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    updown
                </td>

                <td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    foldchange
                </td>

                <td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    tstat
                </td>

                <td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    effect-size
                </td>

                <td style="border-bottom: 1px solid #CDCDCD;white-space:nowrap">
                    pval
                </td>
			</tr>

		</c:forEach>
	</tbody>
</table>
	</c:when>
	<c:otherwise>
		<c:out value="No genes matched your query"></c:out>
	</c:otherwise>
 </c:choose>

</div>



