<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>
<jsp:useBean id="experiments" type="java.util.Collection" scope="request"/>
<jsp:useBean id="count" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="start" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="total" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="sort" type="java.lang.String" scope="request"/>
<jsp:useBean id="dir" type="java.lang.String" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <script type="text/javascript" src='<c:url value="/scripts/jquery-1.3.2.min.js"/>'></script>

    <script type="text/javascript" src='<c:url value="/scripts/feedback.js"/>'></script>

    <link rel="stylesheet" href='<c:url value="/atlas.css"/>' type="text/css"/>
    <link rel="stylesheet" href='<c:url value="/geneView.css"/>' type="text/css"/>

    <link rel="stylesheet" href='<c:url value="/blue/style.css"/>' type="text/css" media="print, projection, screen"/>
    <link rel="stylesheet" href='<c:url value="/structured-query.css"/>' type="text/css"/>

    <style type="text/css">

        .alertNotice > p {
            margin: 10px;
        }

    </style>

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
                Complete list of experiments curated and loaded in the Gene Expression Atlas
            </div>

            <div>
                Total ${total} experiments; showing ${start + 1} to ${start + f:length(experiments)}
                <span id="pagination" class="pagination_ie">
                    <c:forEach begin="0" end="${total}" step="${count}" var="current">
                        <fmt:formatNumber var="pgno" value="${current/count + 1}" pattern="0"/>
                        <c:choose>
                            <c:when test="${(current == 2 * count) and (current < start - 2 * count)}">
                                <c:out escapeXml="false" value="<span>...</span>" />
                            </c:when>
                            <c:when test="${(current >= 2 * count) and (current < start - 2 * count)}"/>
                            <c:when test="${(current < (total - 2 * count)) and (current == start + 3 * count)}">
                                <c:out escapeXml="false" value="<span>...</span>" />
                            </c:when>
                            <c:when test="${(current < (total - 2 * count)) and (current > start + 2 * count)}"/>
                            <c:when test="${current == start}">
                                <c:out escapeXml="false" value="<span class='current'>${pgno}</span>"/>
                            </c:when>
                            <c:otherwise>
                                <c:out escapeXml="false" value="<a href='./index.html?start=${current}' title='${current} to ${current + count}'>${pgno}</a>"/>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </span>
            </div>

            <table class="heatmap" cellspacing="0" cellpadding="2" border="0" id="expts">
                <thead>
                <tr>
                    <th>Accession</th>
                    <th>Title</th>
                    <th style="width:450px" colspan="2">Experimental Factors</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="exp" items="${experiments}" varStatus="j">
                    <tr valign="top">
                        <td class="atlastable" style="white-space:nowrap;">
                            <c:choose>
                                <c:when test="${exp.DEGStatusEmpty}">
                                    <span title="No differentially expressed genes found for this experiment">${exp.accession}&nbsp;</span>
                                </c:when>
                                <c:otherwise>
                                    <a href='<c:url value="/experiment/${exp.accession}"/>'
                                       title="Experiment Data For ${exp.accession}"
                                       target="_self">${exp.accession}</a>&nbsp;
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="atlastable">${exp.description}</td>
                        <td class="atlastable">
                            <nobr>${f:length(exp.experimentFactors)}&nbsp;EFs</nobr>
                        </td>
                        <td class="atlastable">
                            <c:forEach var="factor" items="${exp.experimentFactors}">
                                ${f:escapeXml(atlasProperties.curatedGeneProperties[factor])}
                                [${f:length(exp.factorValuesForEF[factor])}&nbsp;FVs]<br/>
                            </c:forEach>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

        </div>
    </div>

</tmpl:stringTemplateWrap>
</html>
