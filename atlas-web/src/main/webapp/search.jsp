<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse" %>
<%@ page import="ae3.service.AtlasResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page buffer="0kb" %>

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
        String q_gene = request.getParameter("q_gene");
        String q_expt = request.getParameter("q_expt");
        String q_updn = request.getParameter("q_updn");
        String q_orgn = request.getParameter("q_orgn");

        if (q_updn == null) q_updn = "";
        if (q_expt == null || q_expt.equals("(all conditions)")) q_expt = "";
        if (q_gene == null || q_gene.equals("(all genes)"))      q_gene = "";
        if (q_orgn == null) q_orgn = "";
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

                            <input type="checkbox" name="expand_efo" id="expand_efo" value="on"
                                <%=null == request.getParameter("expand_efo") ? "checked" : ""%>
                                <%=null != request.getParameter("expand_efo") && request.getParameter("expand_efo").equals("on") ? "checked" : ""%>>
                            <label for="expand_efo">expand conditions search with <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO" title="Experimental Factor Ontology">EFO</a></label>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>

    <input type="hidden" name="view"/>
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

    long t0 = System.currentTimeMillis();

    if(q_expt.endsWith("*")) q_expt = q_expt.replaceAll("[*]$","?*");

    QueryResponse exptHitsResponse;
    if ( null != request.getParameter("expand_efo") && request.getParameter("expand_efo").equals("on") && null != q_expt && !q_expt.equals("") ) {
        exptHitsResponse  = ArrayExpressSearchService.instance().fullTextQueryExptsWithOntologyExpansion(q_expt);
        String expanded_efo = (String) exptHitsResponse.getHeader().get("expanded_efo");
        if(null != expanded_efo && !expanded_efo.equals(q_expt)) {
            %>
            <div>
                Your query was expanded via <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO" title="Experimental Factor Ontology" target="_blank">EFO</a>,
                an ontology of experimental variables developed by ArrayExpress Production Team. You can rerun the query without the expansion: <input type="button" style="font:small" value="Search" onclick="atlasform.expand_efo.value='off';atlasform.submit()"/>
                <div style="border:1px inset; font-family: monospace; font-size: x-small; padding: 5px; margin: 5px; width:1000px">
                    <%=expanded_efo%>
                </div>
            </div><%
        }
    } else {
        exptHitsResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(q_expt);
    }
    %>
    <div style="margin:0px auto;width:150px;text-align:center;clear:both" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..."/></div>
    <%
    response.flushBuffer();

    long t1 = System.currentTimeMillis();

    if(q_gene.endsWith("*")) q_gene= q_gene.replaceAll("[*]$","?*");
    QueryResponse geneHitsResponse = ArrayExpressSearchService.instance().fullTextQueryGenes(q_gene);
    long t2 = System.currentTimeMillis();

    long recs = 0;
    String viewParam = request.getParameter("view");

    boolean exactmatch = false;
    if(q_expt.indexOf("exp_factor_value:(\"") != -1)
        exactmatch = true;

    AtlasResultSet atlasResultSet = ArrayExpressSearchService.instance().doAtlasQuery(geneHitsResponse, exptHitsResponse, q_updn, q_orgn);

    if(atlasResultSet != null)
        recs = atlasResultSet.getFullRecordCount();

    %>
    <script type="text/javascript">$("#loading_display").hide()</script>

    <%if(recs>0 && viewParam.equals("table")){
        %><div style="float:right; padding:5px">Displaying results 1-<%=recs>1000 ? 1000 : recs%> of ~<%=recs%> found in the atlas</div><%
    } else if(recs>0 && viewParam.equals("heatmap")) {
        %><div style="float:right; padding:5px">Found ~<%=recs%> records in the atlas</div><%
    }%>

    <%
    if(recs > 0 && viewParam == null || viewParam.equals("heatmap")) {
        %>
        <input type="button" style="font:small" value="View as table" onclick="$('#view_table').click();atlasform.submit()"/>
        <table style="position:relative;margin-bottom:50px" border="1" class="heatmap" cellpadding="3" cellspacing="0">
            <tr>
                <th <%if(q_updn.equals("updn")){ %>rowspan="2"<%}%>>Factor Value</th>
                <th <%if(q_updn.equals("updn")){ %>rowspan="2"<%}%> style="border-right: thick solid; border-left: thin"><img src="vtext?<%=response.encodeURL("Number of studies")%>" title="Number of studies"/></th>
                <%--<th><img src="tmp/<%=VerticalTextRenderer.drawString("Total up", application.getRealPath("tmp"))%>" title="Total up"/></th>--%>
                <%--<th style="border-right: thick solid"><img src="tmp/<%=VerticalTextRenderer.drawString("Total down", application.getRealPath("tmp"))%>" title="Total down"/></th>--%>
                <%
                    List<HashMap> genes = atlasResultSet.getAtlasResultGenes();
                    for(HashMap<String,String> gene : genes ) {
                            String gid = gene.get("gene_identifier");
                            String gn = gene.get("gene_name");
                        %>
                            <th <%if(q_updn.equals("updn")){ %>colspan="2"<%}%> align="center"><a style="color: #404040;" href="gene?gid=<%=gid%>" title="Show Atlas Gene Info"><img border="0" src="vtext?<%=response.encodeURL(gn.equals("") ? gid : gn)%>"/></a></th>
                        <%
                    }
                %>
            </tr>
            <%if(q_updn.equals("updn")){ %>
            <tr>
            	
            <%
            for(int i=0; i<genes.size();i++){
            %>
            	<th <%if(i==0){ %>style="border-left: thick solid"<%} %>>UP</th>
				<th>DN</th>
			<%
    		}
            %>
            </tr>
            <%
            }
			%>

            <%
                HashMap<String,HashMap<String,String>> gars = atlasResultSet.getAtlasResultAllGenesByEfv();
                for (HashMap<String,String> ar : atlasResultSet.getAtlasEfvCounts() ) {
                	if (!ar.get("efv").startsWith("V1")) {
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
                                	Long r_dn = 255L;
									Long b_dn = 255L;
									Long g_dn = 255L;

									Long r_up = 255L;
									Long b_up = 255L;
									Long g_up = 255L;

                                    String mpvup = gar.get("mpvup");
                                    String mpvdn = gar.get("mpvdn");

                                    String countup = gar.get("countup").equals("0") ? " ": gar.get("countup");
									String countdn = gar.get("countdn").equals("0") ? " ": gar.get("countdn");

                                    String display = "";
                                    String display_up = "";
									String display_dn = "";
                                    String title   = "Probes for " + gene.get("gene_identifier") + " found in experiment(s) " + gar.get("experiment_count") +
                                                     ", observed up "   + (countup == null ? 0 : countup) + " times (mean p=" + (mpvup == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvup))) + ")" +
                                                     ", observed down " + (countdn == null ? 0 : countdn) + " times (mean p=" + (mpvdn == null ? "N/A" : String.format("%.3g", Double.valueOf(mpvdn))) + ")";

									if (mpvup == null && mpvdn == null) {
										r_up = g_up = b_up = g_dn = r_dn = b_dn = 255L;
									}
									if (mpvdn != null) {
										b_dn = 255L;
										g_dn = 255 - Math.round(Double.valueOf(mpvdn)
												* (-255D / 0.05D) + 255);
										r_dn = 255 - Math.round(Double.valueOf(mpvdn)
												* (-255D / 0.05D) + 255);
										display_up = "0";
										display_dn = countdn;
									}
									if (mpvup != null) {
										r_up = 255L;
										g_up = 255 - Math.round(Double.valueOf(mpvup)
												* (-255D / 0.05D) + 255);
										b_up = 255 - Math.round(Double.valueOf(mpvup)
												* (-255D / 0.05D) + 255);
										display_up = countup;
										display_dn = "0";
									} 
                                    %>
									<%if(q_updn.contains("up")){ %>
									<td align="center"
									style="background-color: rgb(<%=     r_up %>, <%=     g_up %>, <%=     b_up %>)"><span
									title="<%=title%>"
									style="text-decoration: none; font-weight: bold; color: lightgray"><%=countup == "" ? " " : countup%></span>
									</td>
									<%} if(q_updn.contains("down") || q_updn.contains("dn")){ %>
									<td align="center"
										style="background-color: rgb(<%=     r_dn %>, <%=     g_dn %>, <%=     b_dn %>) <%if(q_updn.contains("dn")) %>;border-right: thin solid">
										<span title="<%=title%>" style="text-decoration: none; font-weight: bold; color: lightgray"><%=countdn == "" ? " " : countdn%></span>
									</td>
									<%} 
									} else if(q_updn.equals("updn")){%>
										<td>&nbsp;</td>
										<td style="border-right: thin solid" >&nbsp;</td>
						              <%
									}else{%>
									<td>&nbsp;</td>
										<%
									}
                            }
                        %>
                    </tr>
                    <%
                }
                }
            %>
        </table>
            <%
    } else if(recs > 0 && viewParam.equals("table")) {
        List<HashMap> atlasResults = atlasResultSet.getAllAtlasResults(request.getParameter("sortby"));
        if (recs>1000) atlasResults = atlasResults.subList(0,1000);

        %>
        <input type="button" style="font:small" value="View as heatmap" onclick="$('#view_heatmap').click();atlasform.submit()"/>
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
                        <td><a href="gene?gid=<%=ar.get("gene_identifier")%>" title="Show Atlas Gene Info"><%=ar.get("gene_name")%></a></td>
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

<jsp:include page="end_body.jsp"></jsp:include>