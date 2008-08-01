<%String svnBuildString = "$Rev: 4866 $ $Date: 2008-06-20 11:57:28 +0100 (Fri, 20 Jun 2008) $";%>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.service.AtlasResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="ae3.service.AtlasStructuredQuery" %>
<%@ page import="ae3.util.StructuredQueryHelper" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Preview
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.cookie.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="jquery.autocomplete.js"></script>
    <script type="text/javascript" src="jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="structured-query.js"></script>

    <script type="text/javascript">
        $("#atlasTable").tablesorter({
            sortAppend: [[6,0]],
            headers: {
                6: { sorter : 'digit' },
                7: { sorter : false },
                8: { sorter : false }
            }
        });

        $(document).ready(function()
            {
                $("#q_gene").defaultvalue("(all genes)");
                $("#q_gene").autocomplete("autocomplete.jsp", {
                        minChars:1,
                        matchCase: true,
                        matchSubset: false,
                        multiple: true,
                        multipleSeparator: " ",                    
                        extraParams: {type:"gene"},
                        formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
                });
                initQuery();
            }
        );

        var options = {
            expressions : [
                <% for(String[] i : ArrayExpressSearchService.instance().getGeneExpressionOptions()) { %>
                [ "<%= StringEscapeUtils.escapeJavaScript(i[0]) %>", "<%= StringEscapeUtils.escapeJavaScript(i[1]) %>" ],
                <% } %>
            ],
            factors : [
                <% for(String i : ArrayExpressSearchService.instance().getExperimentalFactorOptions()) { %>
                "<%= StringEscapeUtils.escapeJavaScript(i) %>",
                <% } %>
            ],
            species : [
                <% for(String i : ArrayExpressSearchService.instance().getAllAvailableAtlasSpecies()) { %>
                "<%= StringEscapeUtils.escapeJavaScript(i) %>",
                <% } %>
            ]
        };

        var lastquery;
        <% AtlasStructuredQuery atlasQuery;
           if(request.getParameterNames().hasMoreElements()) {
                atlasQuery = StructuredQueryHelper.parseRequest(request);
        %>
        lastquery = {
            gene : '<%= StringEscapeUtils.escapeJavaScript(atlasQuery.getGene()) %>',
            species : [<% for(String s : atlasQuery.getSpecies()) response.getWriter().print("'" + StringEscapeUtils.escapeJavaScript(s) + "',"); %>],
            conditions : [<% for(AtlasStructuredQuery.Condition c : atlasQuery.getConditions()) {
                                response.getWriter().print("{");
                                response.getWriter().print("factor:'" + StringEscapeUtils.escapeJavaScript(c.getFactor())
                                    + "', expression:'" + StringEscapeUtils.escapeJavaScript(c.getExpression().name())
                                    + "', values:[");
                                for(String v : c.getFactorValues()) response.getWriter().print("'" + StringEscapeUtils.escapeJavaScript(v) + "',");
                                response.getWriter().print("] }, ");
                            } %>]
        };
        <% } %>
    </script>

    <style type="text/css">
        .label {
            font-size: 10px;
        }

        .atlasHelp {
            display: none;
        }

        div.value select, div.value input.value { width :200px; }
        div.value div.buttons { float: right; }
        div.value div.input { float: left; }
        div.value { width: 300px; margin-bottom:2px; }
        #conditions td { vertical-align: top; padding: 2px; }
        #conditions td.factorvalue { padding-bottom: 10px }
        #conditions td.andbuttons { vertical-align: bottom; padding-bottom:10px; }

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


<form name="atlasform" action="qrs" onsubmit="renumberAll();">
    Search for genes 
    <input type="text" name="gene" id="gene" style="width:150px" value=""/>

    in
    
    <table id="species">
        <tbody></tbody> 
    </table>
    
    which are 

    <table id="conditions">
        <tbody></tbody>
    </table>

    
        <label>View results as:</label>
        <input type="radio" name="view" id="view_table" value="table"
               <%=request.getParameter("view") == null || request.getParameter("view").equals("table") ? "checked" : ""%>>
        <label for="view_table">table</label>
        
        <input type="radio" name="view" id="view_heatmap" value="heatmap"
               <%=request.getParameter("view") != null && request.getParameter("view").equals("heatmap") ? "checked" : ""%>>
        <label for="view_heatmap">heatmap</label>

        <input type="hidden" name="view"/>

        <br>
        
        <input type="submit">        
</form>


<%
    if(!request.getParameterNames().hasMoreElements()) {
        %>
        <jsp:include page="end_body.jsp"></jsp:include>
        <%
        return;
    }
    %>
    <%
    response.flushBuffer();

    %>
    <div style="margin:0px auto;width:150px;text-align:center;clear:both" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..."/></div>
    <%
    response.flushBuffer();

    long recs = 0;
    String viewParam = request.getParameter("view");

    long t0 = System.currentTimeMillis();

    AtlasStructuredQuery atlasRequest = StructuredQueryHelper.parseRequest(request);
    AtlasResultSet atlasResultSet = ArrayExpressSearchService.instance().doExtendedAtlasQuery(atlasRequest);

    if(atlasResultSet != null)
        recs = atlasResultSet.getFullRecordCount();

    %>
    <script type="text/javascript">$("#loading_display").hide()</script>

    <%if(recs>0 && viewParam.equals("table")){
        %><div style="text-align:right; padding:5px">Displaying results 1-<%=recs>1000 ? 1000 : recs%> of ~<%=recs%> found in the atlas</div><%
    } else if(recs>0 && viewParam.equals("heatmap")) {
        %><div style="text-align:right; padding:5px">Found ~<%=recs%> records in the atlas</div><%
    }%>

    <%
    if(recs > 0 && viewParam == null || viewParam.equals("heatmap")) {
        %>
        <table style="position:relative;top:-10px" border="1" class="heatmap" cellpadding="3" cellspacing="0">
            <tr>
                <th valign="bottom">Factor Value</th>
                <th style="border-right: thick solid"><img src="vtext?<%=response.encodeURL("Number of studies")%>" title="Number of studies"/></th>
                <%--<th><img src="tmp/<%=VerticalTextRenderer.drawString("Total up", application.getRealPath("tmp"))%>" title="Total up"/></th>--%>
                <%--<th style="border-right: thick solid"><img src="tmp/<%=VerticalTextRenderer.drawString("Total down", application.getRealPath("tmp"))%>" title="Total down"/></th>--%>
                <%
                    List<HashMap> genes = atlasResultSet.getAtlasResultGenes();
                    for(HashMap<String,String> gene : genes ) {
                        %>
                            <th align="center"><a target="_blank" href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=<%=gene.get("gene_identifier")%>&species=&displayInsitu=on&exp_query="><img border="0" src="vtext?<%=response.encodeURL(gene.get("gene_name").equals("") ? gene.get("gene_identifier") : gene.get("gene_name"))%>" title="Show expression for <%=gene.get("gene_name") + " (" + gene.get("gene_identifier") + ")"%> in AEW..."/></a></th>
                        <%
                    }
                %>
            </tr>

            <%
                HashMap<String,HashMap<String,String>> gars = atlasResultSet.getAtlasResultAllGenesByEfv();
                for (HashMap<String,String> ar : atlasResultSet.getAtlasEfvCounts() ) {
                    %>
                    <tr>
                        <td nowrap="true">
                            <span style="font-weight:bold" title="Matched in experiment(s) <%=ar.get("experiments")%>">
                                <%=ar.get("efv").startsWith("V1") ? "--" : ar.get("efv")%>
                            </span>
                        </td>
                        <td  style="border-right: thick solid" align="right"><b><%=ar.get("experiment_count")%></b></td>
                        <%--<td align="right"><b><%=ar.get("up_count")%></b></td>--%>
                        <%--<td style="border-right: thick solid" align="right"><b><%=ar.get("dn_count")%></b></td>--%>

                        <%

                            for(HashMap<String,String> gene : genes ) {
                                HashMap<String,String> gar = gars.get(gene.get("gene_identifier") + ar.get("efv"));

                                if(gar != null && gar.size() != 0) {
                                    Long r = 255L;
                                    Long b = 255L;
                                    Long g = 255L;

                                    String mpvup = gar.get("mpvup");
                                    String mpvdn = gar.get("mpvdn");

                                    String countup = gar.get("countup").equals("0") ? " ": gar.get("countup");
									String countdn = gar.get("countdn").equals("0") ? " ": gar.get("countdn");

                                    String display = "";
                                    String title   = "Probes for " + gene.get("gene_identifier") + " found in experiment(s) " + gar.get("experiment_count") +
                                                     ", observed up "   + (countup == null ? 0 : countup) + " times (mean p=" + (mpvup == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvup))) + ")" +
                                                     ", observed down " + (countdn == null ? 0 : countdn) + " times (mean p=" + (mpvdn == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvdn))) + ")";

                                    if (mpvup == null && mpvdn == null) {
                                        r = g = b = 255L;
                                    } else if (mpvup == null && mpvdn != null) {
                                        b = 255L;
                                        g = 255 - Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        r = 255 - Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        display = "0/" + countdn;
                                    } else if (mpvup != null && mpvdn == null) {
                                        r = 255L;
                                        g = 255 - Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        b = 255 - Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        display = countup + "/0";
                                    } else {
                                        g = 0L;
                                        r = Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        b = Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        display = countup + "/" + countdn;
                                    }

                                    %>
                                        <td align="center" style="background-color:rgb(<%=r%>,<%=g%>, <%=b%>)">
                                            <span title="<%=title%>" style=" text-decoration:none;font-weight:bold;color: white"><%=display%></span>
                                        </td>
                                    <%
                                } else {
                                    %>
                                        <td>&nbsp;</td>
                                    <%
                                }
                                %>
                                <%
                            }
                        %>

                    </tr>
                    <%
                }
            %>
        </table>
            <%
    } else if(recs > 0 && viewParam.equals("table")) {
        List<HashMap> atlasResults = atlasResultSet.getAllAtlasResults(request.getParameter("sortby"));
        if (recs>1000) atlasResults = atlasResults.subList(0,1000);

        %>
        <table style="position:relative; top:-10px;" class="tablesorter" id="atlasTable">
             <thead>
                 <tr>
                     <th width="100">Experiment</th>
                     <th>Description</th>
                     <th>Factor Value (Factor)</th>
                     <th width="100">Gene Name</th>
                     <th>Gene Id</th>
                     <th>Organism</th>
                     <th width="70">P-value</th>
                     <th>AEW</th>
                     <th>...</th>
                 </tr>
             </thead>
             <tbody>
             <%
                for(Map ar : atlasResults ) {
                    Double updn_pvaladj = (Double) ar.get("updn_pvaladj");
                    Integer updn = (Integer) ar.get("updn");

                    Long c = Math.round(updn_pvaladj * (-255D/0.05D) + 255);

                    String rgb = "white";

                    if (updn == -1)
                        rgb = "rgb(" + (255-c) + "," + (255-c) + ",255)";
                    else if (updn == 1)
                        rgb = "rgb(255," + (255-c) + "," + (255-c) + ")";

                    String color = "black";
                    if ( c > 200 ) color = "white";
                    String efv = (String) ar.get("efv");
                    if (efv.startsWith("V1")) efv = "--";
                    String gene_species = (String) ar.get("gene_species");
                    %>
                    <tr style="height:2.4em">
                        <td><a title="Show experiment annotation in repository" target="_blank" href="http://www.ebi.ac.uk/arrayexpress/experiments/<%=ar.get("experiment_accession")%>"><%=ar.get("experiment_accession")%></a></td>
                        <td><%=ar.get("experiment_description")%></td>
                        <td><%=efv + " (" + ar.get("ef") + ")"%></td>
                        <td><%=ar.get("gene_name")%></td>
                        <td><a title="Show gene annotation" target="_blank" href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=<%=ar.get("gene_identifier")%>"><%=ar.get("gene_identifier")%></a></td>
                        <td><%=gene_species.substring(0,1).toUpperCase() + gene_species.substring(1).toLowerCase()%></td>
                        <td align="right" style="background-color: <%=rgb%>; font-size:14px; font-weight:bold; color:<%=color%>"><span style="float:left"><%=updn == 1 ? "&uarr;" : "&darr;" %></span><span style="float:right"><%=updn_pvaladj > 1e-16D ? String.format("%.3g", updn_pvaladj) : "< 1e-16" %></span></td>
                        <td>
                            <a title="Show expression in warehouse"
                               target="_blank"
                               href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=<%=ar.get("gene_identifier")%>&species=&displayInsitu=on&exp_query=<%=ar.get("experiment_accession")%>">More...
                            </a>
                        </td>
                        <td>
                            <%=ar.get("gene_highlights")%>
                        </td>
                    </tr>
                    <%
                }
            %>
             </tbody>
         </table>
        <%
    }

//    atlasResultSet.cleanup();
    long t1 = System.currentTimeMillis();
%>

    <%
    response.getWriter().println("<br/>Done: " +  recs +  " atlas records found in " + (t1-t0) + " ms. <br/><br/>");
    %>

<jsp:include page="end_body.jsp"></jsp:include>

