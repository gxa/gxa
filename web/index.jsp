<%@ page import="ae3.service.AtlasSearch" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.xpath.XPathFactory" %>
<%@ page import="javax.xml.xpath.XPath" %>
<%@ page import="javax.xml.xpath.XPathConstants" %>
<%@ page import="org.w3c.dom.NodeList" %>
<%@ page import="output.HtmlTableWriter" %>
<%@ page session="false" buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Basic Query Page</title>
    <style type="text/css">
        table.helpT {
            text-align: left;
            font-family: Verdana;
            font-weight: normal;
            font-size: 11px;
            color: #404040;
            width: 500px;
            background-color: #fafafa;
            border: 1px #6699CC solid;
            border-collapse: collapse;
            border-spacing: 0px;
        }

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

        td.helpBod {
            border-bottom: 1px solid #9CF;
            border-top: 0px;
            border-left: 1px solid #9CF;
            border-right: 0px;
            text-align: left;
            text-indent: 10px;
            font-family: Verdana, sans-serif, Arial;
            font-weight: normal;
            font-size: 11px;
            color: #404040;
            background-color: #fafafa;
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
    <%String query = request.getParameter("query");%>
    <input type="text" name="query" size="40" value="<%=query != null ? query : ""%>"/>
    <input type="submit" value="get counts" name="get_counts"/>
    <input type="submit" value="query genes in all experiments"/>
    <input type="submit" value="query genes restricting to keyword-matching experiments" name="restrict_expt"/>

    <%
        if (query != null) {
            Document genes = AtlasSearch.instance().fullTextQueryGenes(query);
            Document expts = AtlasSearch.instance().fullTextQueryExpts(query);

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes;
            int nlen;

            nodes = (NodeList) xpath.evaluate("//str[@name='gene_id']", genes, XPathConstants.NODESET);

            StringBuilder inGeneIds = new StringBuilder();
            nlen = nodes.getLength();
            for (int i = 0; i < nlen; i++) {
                inGeneIds.append(nodes.item(i).getTextContent());
                if (i != nlen - 1) inGeneIds.append(",");
            }

            nodes = (NodeList) xpath.evaluate("//str[@name='exp_id']", expts, XPathConstants.NODESET);

            StringBuilder inExptIds = new StringBuilder();
            nlen = nodes.getLength();
            for (int i = 0; i < nlen; i++) {
                inExptIds.append(nodes.item(i).getTextContent());
                if (i != nlen - 1) inExptIds.append(",");
            }
    %>
    <pre>
Debug:   We have gene ids: <%=inGeneIds%>
         We have expt ids: <%=inExptIds%>
    </pre>
    <%
            response.flushBuffer();
            if (request.getParameter("get_counts") != null) {
                int full_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), "");
                int restr_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), inExptIds.toString());
                response.getWriter().println("<pre>Counts: " + full_count + ", " + restr_count + "</pre>");
            } else {
                if (request.getParameter("restrict_expt") == null)
                    inExptIds.setLength(0);
                AtlasSearch.instance().writeAtlasQuery(inGeneIds.toString(), inExptIds.toString(), new HtmlTableWriter(response.getWriter(), response));
            }
        }
    %>
</form>
</body>
</html>