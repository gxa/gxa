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

<jsp:useBean id="exp" type="ae3.model.AtlasExperiment" scope="request"/>
<jsp:useBean id="expSpecies" type="java.util.Collection<java.lang.String>" scope="request"/>

<div style="margin-bottom:20px;">
    <a href="${pageContext.request.contextPath}/experiment/${exp.accession}"
        class="page-title"
       style="vertical-align: baseline">${exp.accession}</a>

    <div style="border:1px black solid;padding:5px;">
        <table cellpadding="2" cellspacing="0" border="0">
            <tr>
                <td style="text-align:right;">Platform:</td>
                <td>
                    <c:forEach var="arrayDesign" items="${exp.arrayDesigns}">
                        <a class="changeArrayDesignLink" href="${pageContext.request.contextPath}/experiment/${exp.accession}?ad=${arrayDesign}">${arrayDesign}</a>&nbsp;
                    </c:forEach>
                </td>
            </tr>
            <tr>
                <td style="text-align:right;">Organism:</td>
                <td>
                    <c:forEach var="species" items="${expSpecies}" varStatus="i">
                        ${species}${i.last ? "" : ", "}
                    </c:forEach>
                </td>
            </tr>

            <c:if test="${not empty exp.numSamples}">
                <tr>
                    <td style="text-align:right;">Samples:</td>
                    <td>${exp.numSamples}</td>
                </tr>
            </c:if>

            <tr>
                <td style="text-align:right;">ArrayExpress:</td>
                <td>
                    <a href="http://www.ebi.ac.uk/arrayexpress/experiments/${exp.accession}"
                       title="Experiment information and full data in ArrayExpress Archive"
                       class="external" rel="nofollow" target="_blank">${exp.accession}</a>
                </td>
            </tr>
        </table>
    </div>
</div>
    
