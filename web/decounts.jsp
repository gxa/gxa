<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="ae3.service.AtlasResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Preview
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="jquery-impromptu.1.5.js"></script>

    <script type="text/javascript">
        $(document).ready(function()
            {
                $("#atlasTable").tablesorter({
                    sortList: [[2,1]]
                });
            }
        );
    </script>

    <style type="text/css">
        table.heatmap th {
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

        table.heatmap {
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

        .label {
            font-size: 10px;
        }
    </style>


<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>
<table width="100%" style="position:relative;top:-10px;border-bottom:thin solid lightgray">
    <tr>
        <td align="left">
            <a href="index.jsp">home</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
        <td align="right">
            <a href="http://www.ebi.ac.uk/microarray"><img border="0" height="20" title="EBI ArrayExpress" src="aelogo.png"/></a>
        </td>
    </tr>
</table>

<table width="100%">
    <tr>
        <td width="100"><a href="index.jsp"><img border="0" src="atlasbeta.jpg"/></a></td>
        <td valign="top"><h1>Counts of genes and experiments up or down per factor/factor value</h1></td>
    </tr>
</table>

<table id="atlasTable" class="tablesorter">
    <thead>
        <tr>
            <th>Factor</th>
            <th>Factor Value</th>
            <th>Up Gene Count</th>
            <th>Up Experiment Count</th>
            <th>Down Gene Count</th>
            <th>Down Experiment Count</th>
        </tr>
    </thead>
    <tbody>
        <%
            List<HashMap> fullGeneEFVCounts = (List<HashMap>) application.getAttribute("fullGeneEFVCounts");
            if ( null == fullGeneEFVCounts ) {
                 fullGeneEFVCounts  = ArrayExpressSearchService.instance().getFullGeneEFVCounts();
                 application.setAttribute("fullGeneEFVCounts", fullGeneEFVCounts);
            }

            for(Map count :  fullGeneEFVCounts ) {
                String ef = (String) count.get("ef");
                String efv = (String) count.get("efv");

                String efurl = URLEncoder.encode(ef, "UTF-8");
                String efvurl = URLEncoder.encode(efv, "UTF-8");
            %>
                <tr>
                    <td><%=ef%></td>
                    <td><%=efv.startsWith("V1") ? efv.replaceFirst("V1","--") : efv%></td>
                    <td><a title="View table..." href="qr?q_orgn=any&view=table&q_updn=up&q_expt=aer_text%3A(<%=efurl%>)+AND+exp_factor_values%3A(<%=efvurl%>)"><%=count.get("gup_count")%></a></td>
                    <td><a title="View heatmap..." href="qr?q_orgn=any&view=heatmap&q_updn=up&q_expt=aer_text%3A(<%=efurl%>)+AND+exp_factor_values%3A(<%=efvurl%>)"><%=count.get("eup_count")%></a></td>
                    <td><a title="View table..." href="qr?q_orgn=any&view=table&q_updn=down&q_expt=aer_text%3A(<%=efurl%>)+AND+exp_factor_values%3A(<%=efvurl%>)"><%=count.get("gdn_count")%></a></td>
                    <td><a title="View heatmap..." href="qr?q_orgn=any&view=heatmap&q_updn=down&q_expt=aer_text%3A(<%=efurl%>)+AND+exp_factor_values%3A(<%=efvurl%>)"><%=count.get("edn_count")%></a></td>
                </tr>
            <%
        }
        %>
    </tbody>
</table>

<jsp:include page="end_body.jsp"></jsp:include>
