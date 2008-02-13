<%@ page import="java.util.Vector" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="ae3.service.AtlasSearch" %>
<%@ page import="ae3.servlet.QueryServlet" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
      <title>Basic Query Page</title>
      <style type="text/css">
          table.helpT
{ text-align: left;
font-family: Verdana;
font-weight: normal;
font-size: 11px;
color: #404040;
width: 500px;
background-color: #fafafa;
border: 1px #6699CC solid;
border-collapse: collapse;
border-spacing: 0px; }

th
{ border-bottom: 2px solid #6699CC;
border-left: 1px solid #6699CC;
background-color: #BEC8D1;
text-align: left;
text-indent: 5px;
font-family: Verdana;
font-weight: bold;
font-size: 11px;
color: #404040; }

td.helpBod
{ border-bottom: 1px solid #9CF;
border-top: 0px;
border-left: 1px solid #9CF;
border-right: 0px;
text-align: left;
text-indent: 10px;
font-family: Verdana, sans-serif, Arial;
font-weight: normal;
font-size: 11px;
color: #404040;
background-color: #fafafa; }

table.sofT
{ text-align: left;
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
        <input type="submit" value="query genes in all experiments"/>
        <input type="submit" value="query genes restricting to keyword-matching experiments" name="restrict_expt"/>

    <% if (query != null) {
        String genes = QueryServlet.fullTextQueryGenes(query);
        String expts = QueryServlet.fullTextQueryExpts(query);

        StringBuilder inGeneIds = new StringBuilder();

        int last_idx = genes.indexOf("<str name=\"gene_id\">");
        while(last_idx > 0) {
            if (inGeneIds.length() != 0) inGeneIds.append(",");
            inGeneIds.append(genes.substring(last_idx + 20, genes.indexOf("<", last_idx + 1)));

            last_idx = genes.indexOf("<str name=\"gene_id\">", last_idx + 1);
        }

        last_idx = -1;

        StringBuilder inExptIds = new StringBuilder();

        last_idx = expts.indexOf("<str name=\"exp_id\">");
        while(last_idx > 0) {
            if (inExptIds.length() != 0) inExptIds.append(",");
            inExptIds.append(expts.substring(last_idx + 19, expts.indexOf("<", last_idx + 1)));

            last_idx = expts.indexOf("<str name=\"exp_id\">", last_idx + 1);
        }
        %>
    <pre>
Debug:   We have gene ids: <%=inGeneIds%>
         We have expt ids: <%=inExptIds%>
    </pre>
        <%
            if(request.getParameter("restrict_expt") == null) inExptIds.setLength(0);
        Vector<HashMap<String,Object>> atlas_results = QueryServlet.atlasQuery(inGeneIds.toString(), inExptIds.toString());
        if ( atlas_results.size() > 0 ) {
            %>
    <table class="sofT" border="1" cellpadding="2">
        <tr><th>Experiment</th><th>Description</th><th>Factor</th><th>Factor Value</th><th>Up/Dn</th><th>Gene</th><th>p-value</th></tr>
        <tr><th colspan="7">Found <%=atlas_results.size() == 1000 ? ">1000" : atlas_results.size() %> results</th></tr>
            <%
                String last_expt_acc = "";
                String last_ef = "";
            for ( HashMap<String,Object> a_result : atlas_results ) {
                String this_expt_acc = (String) a_result.get("expt_acc");
                String this_ef = (String) a_result.get("ef");
                %>
        <tr style="height:2.4em">
            <td valign="top" style="<%=(last_expt_acc.equals(this_expt_acc) ? "" : "background-color:lightgray")%>"><%=(last_expt_acc.equals(this_expt_acc) ? "&nbsp;" : this_expt_acc)%></td>
            <td valign="top"><%=(last_expt_acc.equals(this_expt_acc) ? "&nbsp;" : a_result.get("expt_desc"))%></td>
            <td valign="top"><%=(last_ef.equals(this_ef)             ? "&nbsp;" : a_result.get("ef"))%></td>
            <td valign="top"><%=a_result.get("efv")%></td>
            <td valign="top"><%=a_result.get("updn")%></td>
            <td valign="top"><%=a_result.get("gene")%></td>
            <td valign="top"><%=String.format("%.3g", a_result.get("rank"))%></td>
        </tr>
                <%
                last_expt_acc = this_expt_acc;
                last_ef = this_ef;
            }
            %>
    </table>
            <%
        }

    } %>
    </form>
  </body>
</html>