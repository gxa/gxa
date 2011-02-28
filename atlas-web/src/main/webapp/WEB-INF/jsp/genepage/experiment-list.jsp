<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

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

<script type="text/javascript">
	var exps = [ <c:forEach var="exp" varStatus="s" items="${exps}">{ id: '${exp.id}', acc: '${exp.accession}' }<c:if test="${!s.last}">,</c:if></c:forEach> ];
</script>

<table align="left" cellpadding="0" >

<c:forEach var="exp" items="${exps}">
	<tr align="left" class="exp_header">
		<td align="left" nowrap="true" valign="top">
			${exp.accession}:
		</td>
		<td align="left">
			${exp.description}
		</td>
        <c:if test="${exp.pubmedId!=null}">
            <td align="left" valign="top">
                <a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}" target="_blank">PubMed ${exp.pubmedId}</a>
            </td>
        </c:if>
		
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
					<div id="${exp.id}_EFpagination" class="pagination_ie" style="padding-top: 10px;">
                        <c:forEach var="EF" items="${exp.experimentFactors}">
                            <a id="${EF}"
                               onclick="redrawPlotForFactor('${exp.id}','${exp.accession}', '${atlasGene.geneId}','${EF}',false)">${f:escapeXml(atlasProperties.curatedEfs[EF])}</a>
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

							<a  title="Show expression profile" href="${pageContext.request.contextPath}/experiment/${exp.accession}/${atlasGene.geneIdentifier}" style="border:none;text-decoration:none;outline:none;">
                                <div id="${exp.accession}_${atlasGene.geneId}_plot" class="plot" name="${exp.highestRankEF}" style="width: 300px; height: 150px; background:url('${pageContext.request.contextPath}/images/indicator.gif'); background-repeat:no-repeat; background-position:center;" >
                                </div>
                            </a>
							<div id="${exp.accession}_${atlasGene.geneId}_plot_thm"> </div>
						</td>
					</tr>
					<!--/div-->
				</table>
				</td>
				<td>
					<div style="overflow-y: auto; width:330px; height:150px" id="${exp.accession}_${atlasGene.geneId}_legend"></div>
				</td>
			</tr>
            <tr>
                <td align="left">
                    <div align="left" id="${exp.accession}_${atlasGene.geneId}_arraydesign"></div>
                </td>
            </tr>
		</table>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			&nbsp;Show <a title="Show expression profile in detail" href="${pageContext.request.contextPath}/experiment/${exp.accession}/${atlasGene.geneIdentifier}">expression profile</a>
			&nbsp;/&nbsp;
			<a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=${exp.accession}&detailedview=on">experiment details</a>
			<br/><br/>
		</td>
	</tr>

	<tr>
		<td colspan="3" style="border-bottom:1px solid #CDCDCD">&nbsp;</td>
	</tr>
	
	</c:forEach>
</table>
