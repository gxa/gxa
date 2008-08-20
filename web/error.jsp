<%@ page isErrorPage="true" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="ae3.service.AtlasResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Preview
<jsp:include page="end_head.jsp"></jsp:include>

<link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

<script type="text/javascript" src="jquery.min.js"></script>
<script type="text/javascript" src="jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="jquery.autocomplete.js"></script>
<script type="text/javascript" src="jquerydefaultvalue.js"></script>

<script type="text/javascript">
    $(document).ready(function()
        {
            $("#q_gene").defaultvalue("(all genes)");
            $("#q_expt").defaultvalue("(all conditions)");

            $("#atlasTable").tablesorter({
                sortAppend: [[6,0]],
                headers: {
                    6: { sorter : 'digit' },
                    7: { sorter : false },
                    8: { sorter : false }
                 }
            });

            $("#q_expt").autocomplete("autocomplete.jsp", {
                    minChars:1,
                    matchSubset: false,
                    multiple: true,
                    multipleSeparator: " ",
                    extraParams: {type:"expt"},
                    formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
            });

            $("#q_gene").autocomplete("autocomplete.jsp", {
                    minChars:1,
                    matchCase: true,
                    matchSubset: false,
                    multiple: true,
                    multipleSeparator: " ",
                    extraParams: {type:"gene"},
                    formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
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
            <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> (<b>new!</b>) |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
        <td align="right">
            <a href="http://www.ebi.ac.uk/microarray"><img border="0" height="20" title="EBI ArrayExpress" src="aelogo.png"/></a>
        </td>
    </tr>
</table>
<form name="atlasform" style="position:relative;top:-10px">
    <%
        String q_gene = "";
        String q_expt = "";
        String q_updn = "";
        String q_orgn = "";
    %>

    <table>
        <tr valign="middle">
            <td valign="top">
                <a href="index.jsp"><img border="0" src="atlasbeta.jpg"/></a>
            </td>
            <td>
                <table>
                    <tr>
                        <td>
                            <label class="label" for="q_gene">Genes</label>
                        </td>
                        <td/>
                        <td>
                            <label class="label" for="q_expt">Conditions</label>
                        </td>
                        <td>
                            <label class="label" for="q_orgn">Organism</label>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <input type="text" name="q_gene" id="q_gene" style="width:150px" value="<%=StringEscapeUtils.escapeHtml(q_gene)%>"/>
                        </td>
                        <td>
                            <select name="q_updn">
                                <option value="updn" <%=q_updn.equals("updn") ? "selected" : ""%>>up/down in</option>
                                <option value="up"   <%=q_updn.equals("up") ? "selected" : ""%>>up in</option>
                                <option value="down" <%=q_updn.equals("down") ? "selected" : ""%>>down in</option>
                            </select>
                        </td>
                        <td>
                            <input type="text" name="q_expt" id="q_expt" style="width:150px" value="<%=StringEscapeUtils.escapeHtml(q_expt)%>"/>
                        </td>
                        <td>
                            <select id="q_orgn" name="q_orgn" style="width:150px">
                                <option value="any" <%=q_orgn.equals("") ? "selected" : ""%>>Any species</option>
                                <%
                                    SortedSet<String> species = ArrayExpressSearchService.instance().getAllAvailableAtlasSpecies();
                                    for (String s : species) {
                                %>
                                <option value="<%=s.toUpperCase()%>" <%=q_orgn.equals(s.toUpperCase()) ? "selected" : ""%>><%=s%>
                                </option>
                                <%
                                    }
                                %>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="4" align="left" valign="bottom">
                            <input style="margin-top:10px" type="submit" value="Search Atlas">

                            <label>View results as:</label>
                            <input type="radio" name="view" id="view_table" value="table"
                                <%=request.getParameter("view") == null || request.getParameter("view").equals("table") ? "checked" : ""%>>
                            <label for="view_table">table</label>

                            <input type="radio" name="view" id="view_heatmap" value="heatmap"
                                <%=request.getParameter("view") != null && request.getParameter("view").equals("heatmap") ? "checked" : ""%>>
                            <label for="view_heatmap">heatmap</label>

                            <%--<input type="checkbox" name="expand_efo" id="expand_efo" value="expand_efo"--%>
                                <%--<%=null == request.getParameter("expand_efo") ? "checked" : ""%> --%>
                                <%--<%=null != request.getParameter("expand_efo") && request.getParameter("expand_efo").equals("expand_efo") ? "checked" : ""%>>--%>
                            <%--<label for="expand_efo">expand conditions search with <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO" title="Experimental Factor Ontology">EFO</a></label>--%>
                            <input type="hidden" name="expand_efo" id="expand_efo" value="on"/>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>

    <input type="hidden" name="view"/>
</form>

<div align="center" style="color:red;font-weight:bold;margin-top:150px">
    We're sorry an error has occurred! We will try to remedy this as soon as possible. Responsible parties have been notified and heads will roll.
</div>
<jsp:include page="end_body.jsp"></jsp:include>
