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
<%@ page import="org.apache.lucene.search.Hits" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="org.apache.solr.common.SolrDocumentList" %>
<%@ page import="org.apache.solr.common.SolrDocument" %>
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
        Search keyword(s): <input type="text" name="query" size="40" value="<%=query == null || request.getParameter("query").equals("") ? "" : query.replaceAll("\"","&quot;") %>"/>
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
            long geneIdsLen = 0;
            long exptIdsLen = 0;
            String gene_query = query;
            String expt_query = query;

            // find possible index of " in " in the query. can't do toLowerCase() since query might be big.
            int restrQuery = query.indexOf(" in ");
            if ( restrQuery == -1 ) restrQuery = query.indexOf(" IN ");

            if ( restrQuery != -1) {
                gene_query = query.substring(0, restrQuery);
                expt_query = query.substring(restrQuery + 4);
                restrict_expt = "restrict_expt";
            }

            SolrDocumentList geneHits = AtlasSearch.instance().fullTextQueryGenes(gene_query).getResults();
            SolrDocumentList exptHits = AtlasSearch.instance().fullTextQueryExpts(expt_query).getResults();

            StringBuilder inGeneIds = new StringBuilder();
            if ( geneHits != null ) {
                geneIdsLen = geneHits.getNumFound();
                for (SolrDocument doc : geneHits ) {
                    inGeneIds.append("'").append((String) doc.getFieldValue("gene_id")).append("',");
                }
                inGeneIds.deleteCharAt(inGeneIds.length()-1);
            }

            StringBuilder inExptAccs = new StringBuilder();
            if ( exptHits != null ) {
                exptIdsLen = exptHits.getNumFound();
                for (SolrDocument doc : exptHits) {
                    String expid = (String) doc.getFieldValue("exp_accession");
                    inExptAccs.append("'").append(expid).append("',");
                }
                inExptAccs.deleteCharAt(inExptAccs.length()-1);
            }
    %>
    <pre>
Debug:   We have <%=geneIdsLen%> gene ids: <%=inGeneIds.length() > 200 ? inGeneIds.substring(0,100) + " ..." : inGeneIds %>
         We have <%=exptIdsLen%> expt ids: <%=inExptAccs.length() > 200 ? inExptAccs.substring(0,100) + " ..." : inExptAccs %>
    </pre>
    <%
            response.flushBuffer();
            if ( 1 == 0 /*geneIdsLen > 100 || exptIdsLen > 100*/) {
                response.getWriter().println(String.format("Not running atlas query -- too many ids (genes: %d, expts: %d)", geneIdsLen, exptIdsLen));
            } else {
                if (get_counts != null) {
                    response.getWriter().println("Getting counts... ");
                    response.flushBuffer();
                    long full_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), "");
                    long restr_count = AtlasSearch.instance().getAtlasQueryCount(inGeneIds.toString(), inExptAccs.toString());
                    response.getWriter().println("<pre>Counts: " + full_count + ", " + restr_count + "</pre>");
                } else {
                    if (restrict_expt == null)
                        inExptAccs.setLength(0);

                    response.getWriter().println("Getting atlas data... ");
                    response.flushBuffer();
                    long recs = AtlasSearch.instance().writeAtlasQuery(inGeneIds.toString(), inExptAccs.toString(), new HtmlTableWriter(response.getWriter(), response));
                    response.getWriter().println("Done (" +  recs+  " records).");
                }
            }
        }
    %>
</body>
</html>