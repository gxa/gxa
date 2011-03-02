<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.dao.AtlasSolrDAO" %>
<%@ page import="ae3.model.AtlasExperiment" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
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

<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<%
    AtlasSolrDAO atlasSolrDAO = (AtlasSolrDAO) application.getAttribute(Atlas.ATLAS_SOLR_DAO.key());
    List<AtlasExperiment> expz = new ArrayList(atlasSolrDAO.getExperiments());
    request.setAttribute("allexpts", expz);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <script type="text/javascript" src='<c:url value="/scripts/jquery-1.3.2.min.js"/>'></script>
    <!--[if IE]><script type="text/javascript" src='<c:url value="/scripts/excanvas.min.js"/>'></script><![endif]-->

    <script type="text/javascript" src='<c:url value="/scripts/jquery.pagination.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/feedback.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/jquery.tablesorter.min.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/jquery.flot.atlas.js"/>'></script>

    <script type="text/javascript">
        jQuery(document).ready(function() {
            $("#expts").tablesorter({});
        });
    </script>

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

        <table class="heatmap" cellspacing="0" cellpadding="2" border="0" id="expts">
            <thead>
            <tr>
                <th>#</th>
                <th>Accession</th>
                <th>Title</th>
                <th style="width:450px" colspan="2">Experimental Factors</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="exp" items="${allexpts}" varStatus="j">
                <tr valign="top">
                    <td class="atlastable">${j.index + 1}</td>
                    <td class="atlastable" style="white-space:nowrap;">
                        <c:choose>
                            <c:when test="${exp.DEGStatusEmpty}">
                                <span title="No differentially expressed genes found for this experiment">${exp.accession}&nbsp;</span>
                            </c:when>
                            <c:otherwise>
                                <a href='<c:url value="/experiment/${exp.accession}"/>' title="Experiment Data For ${exp.accession}"
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
