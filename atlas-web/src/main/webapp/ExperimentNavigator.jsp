<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.service.structuredquery.AtlasStructuredQueryService" %>
<%@ page import="ae3.service.structuredquery.AutoCompleteItem" %>
<%@ page import="java.util.Collection" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.model.AtlasExperiment" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
    AtlasStructuredQueryService service = ae3.service.ArrayExpressSearchService.instance().getStructQueryService();

    Collection<AutoCompleteItem> Genes = service.getEfvListHelper().autoCompleteValues("experiment","",1000,null);

    request.setAttribute("Genes",Genes);

    List<AtlasExperiment> expz = (ArrayExpressSearchService.instance().getAtlasDao()).getExperiments();

    HashMap<String,String> ExperimentNames = new HashMap<String,String>();

    for(AtlasExperiment e : expz)
    {

        String s1 = e.getDwExpDescription();
        String s2 = e.getDwExpAccession();

        ExperimentNames.put(s2,s1);
    }
%>

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

<link rel="stylesheet" href="<%=request.getContextPath()%>/atlas.css" type="text/css" />
<link rel="stylesheet" href="<%=request.getContextPath()%>/geneView.css" type="text/css" />


<link rel="stylesheet" href="<%= request.getContextPath()%>/blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="<%= request.getContextPath()%>/jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="<%= request.getContextPath()%>/structured-query.css" type="text/css" />
<jsp:include page='start_body_no_menus.jsp' />

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="width:100%;border-bottom:1px solid #dedede">
    <tr>
        <td align="left" valign="bottom">
            <a href="<%= request.getContextPath()%>/" title="Home"><img width="55" src="<%= request.getContextPath()%>/images/atlas-logo.png" alt="Gene Expression Atlas" title="Atlas Data Release ${f:escapeXml(service.stats.dataRelease)}: ${service.stats.numExperiments} experiments, ${service.stats.numAssays} assays, ${service.stats.numEfvs} conditions" border="0"></a>
        </td>

        <td width="100%" valign="bottom" align="right">
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
        <td align="right" valign="bottom">
        </td>
    </tr>
</table>

    <div style="margin:100px; font-weight:bold; font-size:larger; text-align:center;">
    </div>



    <table cellspacing="0" cellpadding="2" border="0">


    <% for ( AutoCompleteItem i : Genes ) { %>

        <tr>
        <td>
         <a href="<%=request.getContextPath()%>/experiment/<%= i.getValue() %>" title="Experiment Data For <%= i.getValue() %>" target="_self"><%= i.getValue() %></a>&nbsp;
        </td>
            <td>
              <%= ExperimentNames.get(i.getValue()) %>

            </td>
        </tr>
    <% } %>


        </table>

    <%
        String s = request.getRequestURI();

        String Start = request.getParameter("start");
        String Rec = request.getParameter("rec");

        String NextURL = "index.htm" ;

    %>

    <c:if test="${fn:length(Genes) > 999}">
            <a href="<%= NextURL %>">more&gt;&gt;</a>        
    </c:if>

</div>

<!-- end page contents here -->
<jsp:include page='end_body.jsp' />
