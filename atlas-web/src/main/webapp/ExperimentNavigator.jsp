<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.model.AtlasExperiment" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
    List<AtlasExperiment> expz = (List<AtlasExperiment>) application.getAttribute("allexpts");

    if(null == expz) {
        expz = (ArrayExpressSearchService.instance().getAtlasDao()).getExperiments();
        application.setAttribute("allexpts", expz);
    }
%>

<jsp:include page="start_head.jsp" />
Gene Expression Atlas - Experiment Index
<jsp:include page="end_head.jsp" />

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas Summary"/>
<meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />

<script type="text/javascript" language="javascript" src="<%=request.getContextPath()%>/scripts/jquery-1.3.2.min.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/plots.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/feedback.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.tablesorter.min.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.flot.atlas.js"></script>

<script type="text/javascript">
    jQuery(document).ready(function()
    {
       $("#expts").tablesorter({});
    });
</script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/atlas.css" type="text/css" />
<link rel="stylesheet" href="<%=request.getContextPath()%>/geneView.css" type="text/css" />

<link rel="stylesheet" href="<%= request.getContextPath()%>/blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="<%= request.getContextPath()%>/jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="<%= request.getContextPath()%>/structured-query.css" type="text/css" />

<style type="text/css">

    .alertNotice {
        padding: 50px 10px 10px 10px;
        text-align: center;
        font-weight: bold;
    }

    .alertNotice > p {
        margin: 10px;
    }

    .alertHeader {
        color: red;
    }

    #centeredMain {
        width:740px;
        margin: 0 auto;
        padding:50px 0;
        height: 100%;
    }

    .roundCorner {
        background-color: #EEF5F5;
    }

    a.Alphabet{
       margin:10px;
    }

</style>

<jsp:include page='start_body_no_menus.jsp' />

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="width:100%;border-bottom:1px solid #dedede">
    <tr>
        <td align="left" valign="bottom">
            <a href="<%= request.getContextPath()%>/" title="Home"><img width="55" src="<%= request.getContextPath()%>/images/atlas-logo.png" alt="Gene Expression Atlas" title="Atlas Data Release ${f:escapeXml(service.stats.dataRelease)}: ${service.stats.numExperiments} experiments, ${service.stats.numAssays} assays, ${service.stats.numEfvs} conditions" border="0"></a>
        </td>

        <td width="100%" valign="bottom" align="right">
            <a href="<%=request.getContextPath()%>/">home</a> |
            <a href="<%=request.getContextPath()%>/help/AboutAtlas">about the project</a> |
            <a href="<%=request.getContextPath()%>/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="<%=request.getContextPath()%>/help/AtlasDasSource">das</a> |
            <a href="<%=request.getContextPath()%>/help/AtlasApis">api</a> <b>new</b> |
            <a href="<%=request.getContextPath()%>/help">help</a>
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
          <th>#</th><th>Accession</th><th>Title</th><th style="width:450px" colspan="2">Experimental Factors</th>
        </tr>
        </thead>
        <tbody>
    <% int j = 0; %>
    <% for ( AtlasExperiment i : expz ) { %>

        <tr valign="top">
          <td>
             <%=++j%>
          </td>
        <td style="white-space:nowrap;">
        
            <% if(AtlasExperiment.DEGStatus.EMPTY == i.getDEGStatus()) { %>
                <span title="No differentially expressed genes found for this experiment"><%=i.getDwExpAccession()%>&nbsp;</span>       
            <% } else { %>
                <a href="<%=request.getContextPath()%>/experiment/<%= i.getDwExpAccession() %>" title="Experiment Data For <%= i.getDwExpAccession() %>" target="_self"><%= i.getDwExpAccession() %></a>&nbsp;
            <% } %>
        </td>
            <td>
              <%= i.getDwExpDescription() %>
            </td>
            <td><nobr><%=i.getExperimentFactors().size() + " EFs"%></nobr></td>
            <td>
                <%for(String f : i.getExperimentFactors()) {%>
                    <%=ae3.util.CuratedTexts.getCurated(f) + " [" + i.getFactorValuesForEF().get(f).size() + " FVs]<br/> "%>
                <%}%>
               <!-- <%=org.apache.commons.lang.StringUtils.join(i.getExperimentFactors(), ", ")%>-->
            </td>
        </tr>
    <% } %>
        </tbody>
    </table>

</div>

<!-- end page contents here -->
<jsp:include page='end_body.jsp' />
