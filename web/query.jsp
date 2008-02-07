<%@ page import="org.apache.solr.servlet.DirectSolrConnection" %>
<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>

<c:set var="gene_xml" value="" scope="page"/>
<c:set var="expt_xml" value="" scope="page"/>

<c:import var="ixsl" url="/i.xsl"/>

<%
    Log log = LogFactory.getLog("ae3");

    try {
        DirectSolrConnection solr_gene = (DirectSolrConnection) application.getAttribute("solr_gene");
        DirectSolrConnection solr_expt = (DirectSolrConnection) application.getAttribute("solr_expt");

        pageContext.setAttribute("gene_xml", solr_gene.request( "/select?wt=xml&q=" + request.getParameter("query"), null ) );
        pageContext.setAttribute("expt_xml", solr_expt.request( "/select?wt=xml&q=" + request.getParameter("query"), null ) );
    } catch (Exception e) {
        log.error("Problem searching SOLR indexes", e);
    }

//    response.getWriter().print(pageContext.getAttribute("gene_xml"));
%>

<x:parse var="gene_doc"><%=pageContext.getAttribute("gene_xml")%></x:parse>
<x:parse var="expt_doc"><%=pageContext.getAttribute("expt_xml")%></x:parse>

<html>
<body>
Found <x:out select="$gene_doc/response/result[@name='response']/@numFound"/> genes:         <br/>
First few:  <br/>
<x:forEach varStatus="count" select="$gene_doc/response/result[@name='response']/doc">
    <x:out select="str[@name='gene_identifier']"/><br/>
</x:forEach>

Found <x:out select="$expt_doc/response/result[@name='response']/@numFound"/> experiments:         <br/>
First few:  <br/>

<x:forEach select="$expt_doc/response/result[@name='response']/doc">
    <x:out select="str[@name='exp_accession']"/><br/>
</x:forEach>

</body>
</html>
