<%@ page import="ae3.service.AtlasSearch" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.xpath.XPathFactory" %>
<%@ page import="javax.xml.xpath.XPath" %>
<%@ page import="javax.xml.xpath.XPathConstants" %>
<%@ page import="org.w3c.dom.NodeList" %>
<%@ page import="output.HtmlTableWriter" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.fileupload.util.Streams" %>
<%@ page import="org.apache.commons.fileupload.FileItemStream" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="org.apache.commons.fileupload.FileItemIterator" %>
<%@ page import="java.util.HashMap" %>
<%@ page buffer="0kb" %>
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

<table>
<tr>
<td valign="top">
<form action="index.jsp">
        <%
            HashMap<String,String> sessionQueryFiles = (HashMap<String,String>) session.getAttribute("queryFiles");
            if(sessionQueryFiles == null) sessionQueryFiles = new HashMap<String,String>();

            boolean isMultipart = ServletFileUpload.isMultipartContent(request);

            String query = null;
            String get_counts = null;
            String restrict_expt = null;

            if (isMultipart) {
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iter = upload.getItemIterator(request);

                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    String name = item.getFieldName();
                    InputStream stream = item.openStream();
                    String s = Streams.asString(stream);

                    if (name.equals("queryFile")) {
                        sessionQueryFiles.put(item.getName(), s);
                        session.setAttribute("queryFiles", sessionQueryFiles);
                    }
                }
            }

            get_counts = request.getParameter("get_counts");
            restrict_expt = request.getParameter("restrict_expt");

            if ( request.getParameter("query")!= null && !request.getParameter("query").equals("") ) {
                query = request.getParameter("query");
            } else {
                query = sessionQueryFiles.get(request.getParameter("queryFileChoose"));
            }

            if (query != null)
                query = query.replaceAll("\n"," ");
        %>
        Search keyword(s): <input type="text" name="query" size="40" value="<%=query == null || request.getParameter("query").equals("") ? "" : query %>"/>
        or use an uploaded file:
            <select name="queryFileChoose">
                <option value="">Available files:</option>
            <%
            for ( String queryFile : sessionQueryFiles.keySet() ) {
                String data = sessionQueryFiles.get(queryFile);
                %>
                <option value="<%=queryFile%>" <%=queryFile.equals(request.getParameter("queryFileChoose")) ? "selected" : ""%>><%=queryFile%> (<span style="font-size:small"><%=data.length() > 20 ? data.substring(0,20) + " ..." : data %></span>)</option>
                <%
            }
            %></select><%
        %>

        <br/>
        <input type="submit" value="get counts" name="get_counts"/>
        <input type="submit" value="query genes in all experiments"/>
        <input type="submit" value="query genes restricting to keyword-matching experiments" name="restrict_expt"/>
        <input type="reset"/>

    </form>
</td>
<td valign="top">
<form enctype="multipart/form-data" method="post" action="index.jsp">
    upload a query file <input type="file" name="queryFile"/>
    <input type="submit" value="Upload"/>
</form>
</td>
</tr>
</table>

    <%
        if (query != null) {
            int geneIdsLen, exptIdsLen;
            String gene_query = query;
            String expt_query = query;

            if ( query.indexOf(" in ") != -1 ) {
                gene_query = query.substring(0, query.indexOf(" in "));
                expt_query = query.substring(query.indexOf(" in ") + 4);
                restrict_expt = "restrict_expt";
            }

            Document genes = AtlasSearch.instance().fullTextQueryGenes(gene_query);
            Document expts = AtlasSearch.instance().fullTextQueryExpts(expt_query);

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes;

            nodes = (NodeList) xpath.evaluate("//str[@name='gene_id']", genes, XPathConstants.NODESET);

            StringBuilder inGeneIds = new StringBuilder();
            geneIdsLen = nodes.getLength();
            for (int i = 0; i < geneIdsLen; i++) {
                inGeneIds.append(nodes.item(i).getTextContent());
                if (i != geneIdsLen - 1) inGeneIds.append(",");
            }

            nodes = (NodeList) xpath.evaluate("//str[@name='exp_id']", expts, XPathConstants.NODESET);

            StringBuilder inExptIds = new StringBuilder();
            exptIdsLen = nodes.getLength();
            for (int i = 0; i < exptIdsLen; i++) {
                inExptIds.append(nodes.item(i).getTextContent());
                if (i != exptIdsLen - 1) inExptIds.append(",");
            }
    %>
    <pre>
Debug:   We have <%=geneIdsLen%> gene ids: <%=inGeneIds.length() > 200 ? inGeneIds.substring(0,100) + " ..." : inGeneIds %>
         We have <%=exptIdsLen%> expt ids: <%=inExptIds.length() > 200 ? inExptIds.substring(0,100) + " ..." : inExptIds %>
    </pre>
    <%
            response.flushBuffer();
            if (geneIdsLen > 100 || exptIdsLen > 100) {
                response.getWriter().println(String.format("Not running atlas query -- too many ids (genes: %d, expts: %d)", geneIdsLen, exptIdsLen));
            } else {
                if (get_counts != null) {
                    int full_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), "");
                    int restr_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), inExptIds.toString());
                    response.getWriter().println("<pre>Counts: " + full_count + ", " + restr_count + "</pre>");
                } else {
                    if (restrict_expt == null)
                        inExptIds.setLength(0);
                    AtlasSearch.instance().writeAtlasQuery(inGeneIds.toString(), inExptIds.toString(), new HtmlTableWriter(response.getWriter(), response));
                }
            }
        }
    %>
</body>
</html>