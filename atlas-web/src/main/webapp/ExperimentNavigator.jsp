<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.dao.AtlasDao" %>
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

<jsp:useBean id="atlasStatistics" class="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<%
    AtlasDao dao = (AtlasDao) application.getAttribute(Atlas.ATLAS_SOLR_DAO.key());
    List<AtlasExperiment> expz = dao.getExperiments();
    request.setAttribute("allexpts", expz);
%>

<jsp:include page="WEB-INF/jsp/includes/start_head.jsp"/>
Gene Expression Atlas - Experiment Index
<jsp:include page="WEB-INF/jsp/includes/end_head.jsp"/>

<meta name="Description" content="Gene Expression Atlas Summary"/>
<meta name="Keywords"
      content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

<script type="text/javascript"
        language="javascript"
        src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script language="javascript"
        type="text/javascript"
        src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>

<script type="text/javascript">
    jQuery(document).ready(function()
    {
        $("#expts").tablesorter({});
    });
</script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/blue/style.css"
      type="text/css"
      media="print, projection, screen"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery.autocomplete.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>

<style type="text/css">

    .alertNotice > p {
        margin: 10px;
    }

</style>

<jsp:include page='WEB-INF/jsp/includes/start_body_no_menus.jsp'/>

<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <table style="width:100%;border-bottom:1px solid #dedede">
            <tr>
                <td align="left" valign="bottom">
                    <a href="${pageContext.request.contextPath}/" title="Home"><img width="55"
                                                                                src="${pageContext.request.contextPath}/images/atlas-logo.png"
                                                                                alt="Gene Expression Atlas"
                                                                                title="Atlas Data Release ${f:escapeXml(atlasStatistics.dataRelease)}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.propertyValueCount} conditions"
                                                                                border="0"></a>
                </td>

                <td width="100%" valign="bottom" align="right">
                    <a href="${pageContext.request.contextPath}/">home</a> |
                    <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
                    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks"
                                                                                                  style="font-weight:bold;display:none">thanks!</span>
                    |
                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
                    <a href="${pageContext.request.contextPath}/help">help</a>
                </td>
                <td align="right" valign="bottom">
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
                <td>
                    <%=++j%>
                </td>
                <td style="white-space:nowrap;">

                    <% if (AtlasExperiment.DEGStatus.EMPTY == i.getDEGStatus()) { %>
                    <span title="No differentially expressed genes found for this experiment"><%=i.getAccession()%>&nbsp;</span>
                    <% }
                    else { %>
                    <a href="${pageContext.request.contextPath}/experiment/<%= i.getAccession() %>"
                       title="Experiment Data For <%= i.getAccession() %>"
                       target="_self"><%= i.getAccession() %>
                    </a>&nbsp;
                    <% } %>
                </td>
                <td>
                    <%= i.getDescription() %>
                </td>
                <td>
                    <nobr><%=i.getExperimentFactors().size() + " EFs"%>
                    </nobr>
                </td>
                <td>
                    <%for (String f : i.getExperimentFactors()) {%>
                    <%=ae3.util.CuratedTexts.getCurated(f) + " [" + i.getFactorValuesForEF().get(f).size() +
                            " FVs]<br/> "%>
                    <%}%>
                    <!-- <%=org.apache.commons.lang.StringUtils.join(i.getExperimentFactors(), ", ")%>-->
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>

    </div>

    <!-- end page contents here -->
    <jsp:include page='WEB-INF/jsp/includes/end_body.jsp' />
