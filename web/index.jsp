<%@ page import="ae3.service.AtlasSearch" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="output.HtmlTableWriter" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Atlas Query Prototype</title>
    <style type="text/css">
        th {
            border-bottom: 2px solid #6699CC;
            border-left: 1px solid #6699CC;
            background-color: #BEC8D1;
            text-align: left;
            text-indent: 5px;
            font-family: Verdana;
            font-weight: bold;
            font-size: 11px;
            color: #404040;
        }

        table.sofT {
            text-align: left;
            font-family: Verdana;
            font-weight: normal;
            font-size: 11px;
            color: #404040;
            background-color: #fafafa;
            border: 1px #6699CC solid;
            border-collapse: collapse;
            border-spacing: 0px;
        }
    </style>    
</head>
<body>
<form action="index.jsp">
    <%
        String q_gene = request.getParameter("q_gene");
        String q_expt = request.getParameter("q_expt");
        String q_updn = request.getParameter("q_updn");

        if (q_updn == null) q_updn = "";
        if (q_expt == null) q_expt = "";
        if (q_gene == null) q_gene = "";
    %>

    <a href="index.jsp"><img border="0" src="atlasbeta.jpg" style="float:right"/></a>

    <table>
        <tr>
            <td>Find genes matching</td>
            <td><input type="text" name="q_gene" size="30" value="<%=q_gene%>"/> that are</td>
            <td>
                <select name="q_updn">
                    <option value="updn" <%=q_updn.equals("updn") ? "selected" : ""%>>up or down</option>
                    <option value="up"   <%=q_updn.equals("up")   ? "selected" : ""%>>up</option>
                    <option value="down" <%=q_updn.equals("down") ? "selected" : ""%>>down</option>
                </select>
                in
            </td>
            <td>
                <input type="text" name="q_expt" size="30" value="<%=q_expt%>"/>
            </td>
            <td>
                <input type="submit" value="Search"/>
                <input type="reset" value="Clear"/>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><em>(leave blank for all genes)</em></td>
            <td></td>
            <td><em>(leave blank for all conditions)</em></td>
        </tr>
    </table>
</form>

<%
    if(!request.getParameterNames().hasMoreElements()) return;
    response.flushBuffer();

    long t0 = System.currentTimeMillis();

    if(q_expt.endsWith("*")) q_expt = q_expt.replaceAll("[*]$","?*");
    QueryResponse exptHitsResponse = AtlasSearch.instance().fullTextQueryExpts(q_expt);
    long t1 = System.currentTimeMillis();

    if(q_gene.endsWith("*")) q_gene= q_gene.replaceAll("[*]$","?*");
    QueryResponse geneHitsResponse = AtlasSearch.instance().fullTextQueryGenes(q_gene);
    long t2 = System.currentTimeMillis();

//    if (exptHitsResponse != null)
//        response.getWriter().println("Debug: " + exptHitsResponse.getResults().getNumFound() +  " Solr experiment hits in " + (t1-t0) + " ms.<br/>" );
//
//    if (geneHitsResponse != null)
//        response.getWriter().println("Debug: " + geneHitsResponse.getResults().getNumFound() +  " Solr gene hits in " + (t2-t1) + " ms.<br/>" );
//
//    response.getWriter().println("Retrieving data...");
//    response.flushBuffer();

    long recs = AtlasSearch.instance().
            writeAtlasQuery(geneHitsResponse, exptHitsResponse, q_updn,
                            new HtmlTableWriter(response.getWriter(), response));
    long t3 = System.currentTimeMillis();

    response.getWriter().println("<br/>Done: " +  recs +  " atlas records found in " + (t3-t2) + " ms. Total processing time: " + (t3-t0) + " ms.<br/><br/>");

    if (exptHitsResponse != null)
        response.getWriter().println("<br/>" + exptHitsResponse.getResults().getNumFound() +  " Solr experiment hits in " + (t1-t0) + " ms." );

    if (geneHitsResponse != null)
        response.getWriter().println("<br/>" + geneHitsResponse.getResults().getNumFound() +  " Solr gene hits in " + (t2-t1) + " ms." );

    if (q_expt.equals("") && q_gene.equals(""))
        response.getWriter().println("Try entering some query parameters!" );
%>

</body>
</html>
