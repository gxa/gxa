<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.dao.AtlasSolrDAO" %>
<%@ page import="ae3.model.AtlasExperiment" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
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

<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<%
    AtlasSolrDAO atlasSolrDAO = (AtlasSolrDAO) application.getAttribute(Atlas.ATLAS_SOLR_DAO.key());
    List<AtlasExperiment> expz = atlasSolrDAO.getExperiments();
    request.setAttribute("allexpts", expz);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
    <head>
<u:htmlTemplate file="look/experimentNavigator.head.html" />

<meta name="Description" content="Gene Expression Atlas Summary"/>
<meta name="Keywords"
      content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

<script type="text/javascript" src='<c:url value="/scripts/jquery-1.3.2.min.js"/>'></script>
<!--[if IE]><script type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src='<c:url value="/scripts/jquery.pagination.js"/>'></script>
<script type="text/javascript" src='<c:url value="/scripts/feedback.js"/>'></script>
<script type="text/javascript" src='<c:url value="/scripts/jquery.tablesorter.min.js"/>'></script>
<script type="text/javascript" src='<c:url value="/scripts/jquery.flot.atlas.js"/>'></script>

<script type="text/javascript">
    jQuery(document).ready(function(){
        $("#expts").tablesorter({});
    });
</script>

<link rel="stylesheet" href='<c:url value="/atlas.css"/>' type="text/css"/>
<link rel="stylesheet" href='<c:url value="/geneView.css"/>' type="text/css"/>

<link rel="stylesheet" href='<c:url value="/blue/style.css"/>' type="text/css" media="print, projection, screen"/>
<link rel="stylesheet" href='<c:url value="/structured-query.css"/>' type="text/css"/>

<base href='<c:url value="/"/>' />

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

<body onLoad="if(navigator.userAgent.indexOf('MSIE') != -1) {document.getElementById('head').allowTransparency = true;}">
	<div class="headerdiv" id="headerdiv" style="position:absolute; z-index: 1;">
		<iframe src="http://www.ebi.ac.uk/inc/head.html" name="head" id="head" frameborder="0" marginwidth="0px" marginheight="0px" scrolling="no"  width="100%" style="position:absolute; z-index: 1; height: 57px;"></iframe>
	</div>


<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <table style="width:100%;border-bottom:1px solid #dedede">
            <tr>
                <td class="atlastable" align="left" valign="bottom">
                    <a href="." title="Home"><img width="55"
                                                  src="images/atlas-logo.png"
                                                  alt="Gene Expression Atlas"
                                                  title="Atlas Data Release ${f:escapeXml(atlasStatistics.dataRelease)}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.propertyValueCount} conditions"
                                                  border="0"></a>
                </td>

                <td class="atlastable" width="100%" valign="bottom" align="right">
                    <a href=".">home</a> |
                    <a href="help/AboutAtlas">about the project</a> |
                    <a href="help/AtlasFaq">faq</a> |
                    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks"
                                                                                                  style="font-weight:bold;display:none">thanks!</span>
                    |
                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
                    <a href="help/AtlasDasSource">das</a> |
                    <a href="help/AtlasApis">api</a> <b>new</b> |
                    <a href="help">help</a>
                </td>
                <td class="atlastable" align="right" valign="bottom">
                </td>
            </tr>
        </table>

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
            <% int j = 0; %>
            <% for (AtlasExperiment i : expz) { %>

            <tr valign="top">
                <td class="atlastable">
                    <%=++j%>
                </td>
                <td class="atlastable" style="white-space:nowrap;">

                    <% if (AtlasExperiment.DEGStatus.EMPTY == i.getDEGStatus()) { %>
                    <span title="No differentially expressed genes found for this experiment"><%=i.getAccession()%>&nbsp;</span>
                    <% }
                    else { %>
                    <a href="experiment/<%= i.getAccession() %>"
                       title="Experiment Data For <%= i.getAccession() %>"
                       target="_self"><%= i.getAccession() %>
                    </a>&nbsp;
                    <% } %>
                </td>
                <td class="atlastable">
                    <%= i.getDescription() %>
                </td>
                <td class="atlastable">
                    <nobr><%=i.getExperimentFactors().size() + " EFs"%>
                    </nobr>
                </td>
                <td class="atlastable">
                    <%for (String f : i.getExperimentFactors()) {%>
                    <c:set var="f"><%=f%></c:set>
                    ${f:escapeXml(atlasProperties.curatedGeneProperties[f])} [<%=i.getFactorValuesForEF().get(f).size()%> FVs]<br/>
                    <%}%>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>

    </div>

<u:htmlTemplate file="look/footer.html" />
</body></html>
