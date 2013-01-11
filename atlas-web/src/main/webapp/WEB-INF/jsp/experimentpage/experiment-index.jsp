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
<%@include file="../includes/global-inc.jsp" %>

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>
<jsp:useBean id="experiments" type="java.util.Collection<uk.ac.ebi.gxa.web.controller.ExperimentIndexLine>"
             scope="request"/>
<jsp:useBean id="count" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="total" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="query" type="java.lang.String" scope="request"/>
<jsp:useBean id="invalidquery" type="java.lang.Boolean" scope="request"/>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-gxa"/>
    <wro4j:all name="bundle-gxa-grid-support"/>
    <wro4j:all name="bundle-gxa-page-experiment-index"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

    <div class="contents" id="contents">
        <div class="ae_pagecontainer">

            <jsp:include page="../includes/atlas-header.jsp"/>

            <div style="margin:40px; font-weight:bold; font-size:larger; text-align:center;">
                Experiments loaded in the Gene Expression Atlas
            </div>

            <div class="expSearch">
                <form id="experimentFilterForm" name="experimentFilterForm" action="index.html" method="get">
                    <table>
                        <tr>
                            <td><input type="text" class="value" id="expFilter" name="q" value="${query}"/></td>
                            <td><input type="submit" value="Search" onclick="function searchSubmit() {
                                 experimentFilterForm.submit();
                            }
                            searchSubmit()"/></td>
                        </tr>
                    </table>
                </form>
            </div>

            <c:if test="${invalidquery}">
                <div style="margin-top:30px;margin-bottom:20px;font-weight:bold;">Query ${query} is not valid</div>
            </c:if>

           <c:if test="${!invalidquery}">
            <display:table name="${experiments}" sort="external" requestURI="./index.html"
                           defaultsort="2" defaultorder="descending"
                           requestURIcontext="false" id="experiment"
                           class="atlas-grid noborder hoverable sortable experiment-index"
                           size="${total}" partialList="true" pagesize="${count}">
                <display:column sortable="true" sortName="accession" title="Experiment" class="nowrap">
                    <a href="${pageContext.request.contextPath}/experiment/${experiment.accession}">${experiment.accession}</a>
                </display:column>
                <display:column property="loadDate" sortable="true" sortName="loaddate"
                                title="Loaded" class="nowrap" format="{0,date,dd-MM-yyyy}"/>
                <display:column property="description" sortable="false"/>
                <display:column sortable="true" sortName="pmid" title="PubMed ID"
                                class="number">
                    <c:if test="${not empty experiment.pubmedId}">
                        <a href="http://www.ncbi.nlm.nih.gov/pubmed/${experiment.pubmedId}"
                           class="external">${experiment.pubmedId}</a>
                    </c:if>
                </display:column>
                <display:column property="numAssays" sortable="true" sortName="numAssays"
                                title="Assays" class="number">
                    <a href="${pageContext.request.contextPath}/experimentDesign/${experiment.accession}">${experiment.accession}</a>
                </display:column>
                <display:column property="numSamples" sortable="true" sortName="numSamples"
                                title="Samples" class="number"/>
                <%-- Postponed until implemented
                <display:column property="studyType" sortable="true" sortName="studyType"
                                title="Type"/>
                                --%>
                <display:column title="Species" class="nowrap">
                    <c:if test="${f:length(experiment.organisms) == 0}">
                        <span class="note">none</span>
                    </c:if>
                    <c:forEach var="organism" items="${experiment.organisms}">
                        <c:out value="${organism.name}"/><br/>
                    </c:forEach>
                </display:column>
                <display:column title="Experiment Factors" class="nowrap">
                    <c:if test="${f:length(experiment.experimentFactors) == 0}">
                        <span class="note">in curation</span>
                    </c:if>
                    <c:forEach var="factor" items="${experiment.experimentFactors}">
                        <c:out value="${factor.displayName}"/><br/>
                    </c:forEach>
                </display:column>
            </display:table>
           </c:if>
        </div>
    </div>

</tmpl:stringTemplateWrap>
</html>
