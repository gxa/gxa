<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ae3.service.structuredquery.AutoCompleteItem" %>
<%@ page import="ae3.service.GeneListCacheService" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="java.util.Collection" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
    String rec = request.getParameter("rec");
    String prefix = request.getParameter("start");

    if(null == prefix)
               prefix = "a";

    int recordCount = GeneListCacheService.PAGE_SIZE;

    //if anything passed in "rec=" URL param - retrieve all, otherwise - first PageSize
    if(null != rec)
    {
        recordCount = 100000;
    }

    GeneListCacheService geneListServlet = (GeneListCacheService)application.getAttribute(Atlas.GENES_CACHE.key());
    Collection<AutoCompleteItem> genes = null;
    try {
        genes = geneListServlet.getGenes(prefix, recordCount);
        request.setAttribute("Genes", genes);
    }
    catch (Exception e) {
        e.printStackTrace();
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


<jsp:include page="WEB-INF/jsp/includes/start_head.jsp" />
Gene Expression Atlas - Gene Index
<jsp:include page="WEB-INF/jsp/includes/end_head.jsp" />

<meta name="Description" content="Gene Expression Atlas Summary"/>
<meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />

<script type="text/javascript" language="javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.autocomplete.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css" />


<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery.autocomplete.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css" />
<jsp:include page='WEB-INF/jsp/includes/start_body_no_menus.jsp' />

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="width:100%;border-bottom:1px solid #dedede">
    <tr>
        <td align="left" valign="bottom">
            <a href="${pageContext.request.contextPath}/" title="Home"><img width="55" src="${pageContext.request.contextPath}/images/atlas-logo.png" alt="Gene Expression Atlas" title="Atlas Data Release ${f:escapeXml(atlasStatistics.dataRelease)}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.propertyValueCount} conditions" border="0"></a>
        </td>

        <td width="100%" valign="bottom" align="right">
            <a href="${pageContext.request.contextPath}/">home</a> |
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
        <td align="right" valign="bottom">
        </td>
    </tr>
</table>

    <div style="margin:100px; font-weight:bold; font-size:larger; text-align:center;">
 	<a class="alphabet"  href="index.htm?start=0" title="Gene Expression Atlas Genes Starting With Digit">123</a>
        <a class="alphabet"  href="index.htm?start=a" title="Gene Expression Atlas Genes Starting With A">A</a>
        <a class="alphabet"  href="index.htm?start=b" title="Gene Expression Atlas Genes Starting With B">B</a>
        <a class="alphabet"  href="index.htm?start=c" title="Gene Expression Atlas Genes Starting With C">C</a>
        <a class="alphabet"  href="index.htm?start=d" title="Gene Expression Atlas Genes Starting With D">D</a>
        <a class="alphabet"  href="index.htm?start=e" title="Gene Expression Atlas Genes Starting With E">E</a>
        <a class="alphabet"  href="index.htm?start=f" title="Gene Expression Atlas Genes Starting With F">F</a>
        <a class="alphabet"  href="index.htm?start=g" title="Gene Expression Atlas Genes Starting With G">G</a>
        <a class="alphabet"  href="index.htm?start=h" title="Gene Expression Atlas Genes Starting With H">H</a>
        <a class="alphabet"  href="index.htm?start=i" title="Gene Expression Atlas Genes Starting With I">I</a>
        <a class="alphabet"  href="index.htm?start=j" title="Gene Expression Atlas Genes Starting With J">J</a>
        <a class="alphabet"  href="index.htm?start=k" title="Gene Expression Atlas Genes Starting With K">K</a>
        <a class="alphabet"  href="index.htm?start=l" title="Gene Expression Atlas Genes Starting With L">L</a>
        <a class="alphabet"  href="index.htm?start=m" title="Gene Expression Atlas Genes Starting With M">M</a>
        <a class="alphabet"  href="index.htm?start=n" title="Gene Expression Atlas Genes Starting With N">N</a>
        <a class="alphabet"  href="index.htm?start=o" title="Gene Expression Atlas Genes Starting With O">O</a>
        <a class="alphabet"  href="index.htm?start=p" title="Gene Expression Atlas Genes Starting With P">P</a>
        <a class="alphabet"  href="index.htm?start=q" title="Gene Expression Atlas Genes Starting With Q">Q</a>
        <a class="alphabet"  href="index.htm?start=r" title="Gene Expression Atlas Genes Starting With R">R</a>
        <a class="alphabet"  href="index.htm?start=s" title="Gene Expression Atlas Genes Starting With S">S</a>
        <a class="alphabet"  href="index.htm?start=t" title="Gene Expression Atlas Genes Starting With T">T</a>
        <a class="alphabet"  href="index.htm?start=u" title="Gene Expression Atlas Genes Starting With U">U</a>
	<a class="alphabet"  href="index.htm?start=v" title="Gene Expression Atlas Genes Starting With V">V</a>
        <a class="alphabet"  href="index.htm?start=w" title="Gene Expression Atlas Genes Starting With W">W</a>
        <a class="alphabet"  href="index.htm?start=x" title="Gene Expression Atlas Genes Starting With X">X</a>
	<a class="alphabet"  href="index.htm?start=y" title="Gene Expression Atlas Genes Starting With Y">Y</a>
	<a class="alphabet"  href="index.htm?start=z" title="Gene Expression Atlas Genes Starting With Z">Z</a>
    </div>



    <c:forEach var="gene" items="${Genes}">
         <a href="${pageContext.request.contextPath}/gene/${gene.id}" title="Gene Expression Atlas Data For ${gene.value}" target="_self">${gene.value}</a>&nbsp;
    </c:forEach>

    <%
        String s = request.getRequestURI();

        if(null == prefix)
               prefix = "a";
        
        String NextURL = "index.htm?start="+prefix+"&rec="+Integer.toString(GeneListCacheService.PAGE_SIZE) ;

        boolean more = (genes.size() > (GeneListCacheService.PAGE_SIZE -1)) & (rec ==null);

        //AZ:2009-07-23:it can be less unique gene names then requested PageSize => cut corner and add "more" always.          
        more = true;

        request.setAttribute("more",more);
    %>

    <c:if test="${more}">
            <a href="<%= NextURL %>">more&gt;&gt;</a>        
    </c:if>

</div>

<!-- end page contents here -->
<jsp:include page='WEB-INF/jsp/includes/end_body.jsp' />
