<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="ae3.service.AtlasResultSet" %>
<%@ page import="java.util.*" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript">
    $(document).ready(function()
        {
            $("#atlasTable").tablesorter({
//                debug: true,
                sortAppend: [[6,0]],
                headers: {
                    6: { sorter : 'digit' },
                    7: { sorter : false },
                    8: { sorter : false }                    
                 }
//                ,
//                widgets: ['groups'],
//                emptyGroupCaption: "(none)",
//                collapsableGroups: false
            });
        }
    );
    </script>

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
    </style>
</head>
<body>
<form name="atlasform" action="index.jsp">
    <%
        String q_gene = request.getParameter("q_gene");
        String q_expt = request.getParameter("q_expt");
        String q_updn = request.getParameter("q_updn");
        String q_orgn = request.getParameter("q_orgn");        

        if (q_updn == null) q_updn = "";
        if (q_expt == null) q_expt = "";
        if (q_gene == null) q_gene = "";
        if (q_orgn == null) q_orgn = "";
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
            <td>Organism:
                <select name="q_orgn">
                    <option value="any" <%=q_orgn.equals("") ? "selected" : ""%>>Any species</option>
                    <%
                        SortedSet<String> species = ArrayExpressSearchService.instance().getAllAvailableAtlasSpecies();
                        for ( String s : species ) {
                            %><option value="<%=s.toUpperCase()%>" <%=q_orgn.equals(s.toUpperCase()) ? "selected" : ""%>><%=s%></option><%
                        }
                    %>
                </select>
            </td>
            <td>
                <input type="submit" onclick="atlasform.view.value = 'table'"   value="Results as table">
                <input type="submit" onclick="atlasform.view.value = 'heatmap'" value="Results as heatmap">
            </td>
            <!--<td>-->
                <!--Sort by p-value and-->
                <!--<select name="sortby">-->
                    <!--<option value="experiment">Experiment</option>-->
                    <!--<option value="experiment">Experiment</option>-->
                    <!--<option value="ef">Factor</option>-->
                    <!--<option value="efv">Factor Value</option>-->
                    <!--<option value="gene">Gene</option>-->
                <!--</select>-->
            <!--</td>-->
        </tr>
        <tr>
            <td></td>
            <td><em>(leave blank for all genes)</em></td>
            <td></td>
            <td><em>(leave blank for all conditions)</em></td>
        </tr>
    </table>
    <input type="hidden" name="view"/>
</form>

<%
    if(!request.getParameterNames().hasMoreElements()) return;
    response.flushBuffer();

    long t0 = System.currentTimeMillis();

    if(q_expt.endsWith("*")) q_expt = q_expt.replaceAll("[*]$","?*");
    QueryResponse exptHitsResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(q_expt);
    long t1 = System.currentTimeMillis();

    if(q_gene.endsWith("*")) q_gene= q_gene.replaceAll("[*]$","?*");
    QueryResponse geneHitsResponse = ArrayExpressSearchService.instance().fullTextQueryGenes(q_gene);
    long t2 = System.currentTimeMillis();

    long recs = 0;
    String viewParam = request.getParameter("view");

    AtlasResultSet atlasResultSet = ArrayExpressSearchService.instance().doAtlasQuery(geneHitsResponse, exptHitsResponse, q_updn, q_orgn);
    HashSet<AtlasResultSet> sessionARS = (HashSet<AtlasResultSet>) session.getAttribute("sessionARS");

    if (sessionARS == null) {
        sessionARS = new HashSet<AtlasResultSet>();
    }

    sessionARS.add(atlasResultSet);
    session.setAttribute("sessionARS", sessionARS);

    if(atlasResultSet != null)
        recs = atlasResultSet.getFullRecordCount();

    if(recs > 0 && viewParam == null || viewParam.equals("heatmap")) {
        %>
        <table border="1" class="sofT">
            <tr>
                <th valign="bottom">Factor Value</th>
                <th style="border-right: thick solid"><img src="vtext?<%=response.encodeURL("Number of studies")%>" title="Number of studies"/></th>
                <%--<th><img src="tmp/<%=VerticalTextRenderer.drawString("Total up", application.getRealPath("tmp"))%>" title="Total up"/></th>--%>
                <%--<th style="border-right: thick solid"><img src="tmp/<%=VerticalTextRenderer.drawString("Total down", application.getRealPath("tmp"))%>" title="Total down"/></th>--%>
                <%
                    List<HashMap> genes = atlasResultSet.getAtlasResultGenes();
                    for(HashMap<String,String> gene : genes ) {
                        %>
                            <th align="center"><a target="_blank" href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=<%=gene.get("gene_identifier")%>&species=&displayInsitu=on&exp_query="><img border="0" src="vtext?<%=response.encodeURL(gene.get("gene_name"))%>" title="Show expression for <%=gene.get("gene_name") + " (" + gene.get("gene_identifier") + ")"%> in AEW..."/></a></th>
                        <%
                    }
                %>
            </tr>

            <%
                HashMap<String,HashMap<String,String>> gars = atlasResultSet.getAtlasResultAllGenesByEfv();
                for (HashMap<String,String> ar : atlasResultSet.getAtlasEfvCounts() ) {
                    %>
                    <tr>
                        <td nowrap="true"><span style="font-weight:bold" title="Matched in experiment(s) <%=ar.get("experiments")%>"><%=ar.get("efv")%></span></td>
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

                                    String sumup = gar.get("sumup");
                                    String sumdn = gar.get("sumdn");

                                    String display = "";
                                    String title   = "Probes for " + gene.get("gene_identifier") + " found in experiment(s) " + gar.get("experiment_count") + 
                                                     ", observed up "   + (sumup == null ? 0 : sumup) + " times (mean p=" + (mpvup == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvup))) + ")" +
                                                     ", observed down " + (sumdn == null ? 0 : sumdn) + " times (mean p=" + (mpvdn == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvdn))) + ")";

                                    if (mpvup == null && mpvdn == null) {
                                        r = g = b = 255L;
                                    } else if (mpvup == null && mpvdn != null) {
                                        b = 255L;
                                        g = 255 - Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        r = 255 - Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        display = "0/" + sumdn;
                                    } else if (mpvup != null && mpvdn == null) {
                                        r = 255L;
                                        g = 255 - Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        b = 255 - Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        display = sumup + "/0";
                                    } else {
                                        g = 0L;
                                        r = Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
                                        b = Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
                                        display = sumup + "/" + sumdn;
                                    }

                                    %>
                                        <td align="center" style="background-color:rgb(<%=r%>,<%=g%>, <%=b%>)">
                                            <span title="<%=title%>" style="text-decoration:none;font-weight:bold;color: white"><%=display%></span>
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
        %>
         <table class="tablesorter" id="atlasTable">
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
                for(Map ar : atlasResultSet.getAllAtlasResults(request.getParameter("sortby"))) {
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

                    %>
                    <tr style="height:2.4em">
                        <td><a title="Show experiment annotation in repository" target="_blank" href="http://www.ebi.ac.uk/arrayexpress/experiments/<%=ar.get("experiment_accession")%>"><%=ar.get("experiment_accession")%></a></td>
                        <td><%=ar.get("experiment_description")%></td>
                        <td><%=ar.get("efv") + " (" + ar.get("ef") + ")"%></td>
                        <td><%=ar.get("gene_name")%></td>
                        <td><a title="Show gene annotation" target="_blank" href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=<%=ar.get("gene_identifier")%>"><%=ar.get("gene_identifier")%></a></td>
                        <td><%=ar.get("gene_species")%></td>
                        <td align="right" style="background-color: <%=rgb%>; font-size:14px; font-weight:bold; color:<%=color%>"><%=String.format("%.3g", updn_pvaladj)%></td>
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
    long t3 = System.currentTimeMillis();
%>

    <%
    response.getWriter().println("<br/>Done: " +  recs +  " atlas records found in " + (t3-t2) + " ms. Total processing time: " + (t3-t0) + " ms.<br/><br/>");

    if (exptHitsResponse != null)
        response.getWriter().println("<br/>" + exptHitsResponse.getResults().getNumFound() +  " Solr experiment hits (" + exptHitsResponse.getResults().size() + " used), in " + (t1-t0) + " ms." );

    if (geneHitsResponse != null)
        response.getWriter().println("<br/>" + geneHitsResponse.getResults().getNumFound() +  " Solr gene hits  (" + geneHitsResponse.getResults().size() + " used), in " + (t2-t1) + " ms." );

    if (q_expt.equals("") && q_gene.equals(""))
        response.getWriter().println("Try entering some query parameters!" );
%>

</body>
</html>
