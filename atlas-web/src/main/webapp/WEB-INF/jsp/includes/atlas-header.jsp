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

<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<c:set var="isHomePage"><c:out value="${param.isHomePage}" default="false"/></c:set>

<table style=";width:100%;border-bottom:1px solid #DEDEDE;">
    <tr>
        <td align="left" valign="bottom">
            <a href='${pageContext.request.contextPath}/'>
                <img src="${pageContext.request.contextPath}/images/atlas-logo.png"
                     alt="Gene Expression Atlas"
                     title="Atlas Data Release ${atlasStatistics.dataRelease}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.factorValueCount} conditions"
                     <c:if test="${! isHomePage}">width="55"</c:if>
                     border="0"/>
            </a>
        </td>
        <td align="right" valign="bottom">
            <c:if test="${! isHomePage}">
                <a href="${pageContext.request.contextPath}/">home</a> |
            </c:if>
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks"
                                                                                         style="font-weight: bold; display: none">thanks!</span>
            |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>