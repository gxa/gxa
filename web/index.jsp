<%String svnBuildString = "$Rev$ $Date$";%>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
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

    <script type="text/javascript">
        function toggleAtlasHelp() {
            if($("div.atlasHelp").is(":hidden")) {
                showAtlasHelp();
            } else {
                hideAtlasHelp();
            }            
        }

        function showAtlasHelp() {
            if($("div.atlasHelp").is(":hidden")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("hide help");
            }
            $.cookie('atlas_help_state','shown');
        }

        function hideAtlasHelp() {
            if($("div.atlasHelp").is(":visible")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("show help");
            }
            $.cookie('atlas_help_state','hidden');
        }

        $(document).ready(function()
            {
                $("#q_gene").defaultvalue("(all genes)");
                $("#q_expt").defaultvalue("(all conditions)");

                $("#atlasHelpToggle").click(toggleAtlasHelp);

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

                if (($.cookie('atlas_help_state') == "shown") && ($("div.atlasHelp").is(":hidden"))) {
                   showAtlasHelp();
                } else if (($.cookie('atlas_help_state') == "hidden") && ($("div.atlasHelp").is(":visible"))) {
                   hideAtlasHelp();
                }
            }
        );
    </script>

    <style type="text/css">
        .label {
            font-size: 10px;
        }

        .atlasHelp {
            display: none;
        }
    </style>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals=" />
<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>

<table width="100%" style="position:relative;top:-10px;border-bottom:thin solid lightgray">
    <tr>
        <td align="left">
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
<form name="atlasform" action="qr">
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

    <table style="margin:auto;height:200px">
        <tr valign="top">
            <td valign="top"><img border="0" src="atlasbeta.jpg"/></td>
            <td>
                <table>
                    <tr>
                        <td>
                            <label class="label" for="q_gene">Genes</label>
                        </td>
                        <td/>
                        <td>
                            <label class="label" for="q_orgn">Organism</label>
                        </td>
                        <td>
                            <label class="label" for="q_expt">Conditions</label>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <input type="text" name="q_gene" id="q_gene" style="width:280px" value="<%=StringEscapeUtils.escapeHtml(q_gene)%>"/>
                        </td>
                        <td>
                            <select name="q_updn">
                                <option value="updn" <%=q_updn.equals("updn") ? "selected" : ""%>>up/down in</option>
                                <option value="up"   <%=q_updn.equals("up") ? "selected" : ""%>>up in</option>
                                <option value="down" <%=q_updn.equals("down") ? "selected" : ""%>>down in</option>
                            </select>
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
                        <td>
                            <input type="text" name="q_expt" id="q_expt" style="width:280px" value="<%=StringEscapeUtils.escapeHtml(q_expt)%>"/>
                        </td>
                    </tr>
                    <tr>
                        <td valign="top" align="center" style="width:150px">
                            <div class="atlasHelp">
                                <div style="font-size: 0px; line-height: 0%; width: 0px; border-bottom: 20px solid pink; border-left: 10px solid white;border-right: 10px solid white;">&nbsp;</div>
                                <div style="background-color:pink; text-align:left; height:100%; width: 140px;padding:5px">
                                    Please enter a gene name, synonym, Ensembl or UniProt identifier, GO category, etc.
                                </div>
                            </div>
                        </td>
                        <td colspan="2" align="center" valign="top" width="200">
                            <div style="margin-top:10px">
                                <input type="submit" value="Search Atlas">
                            </div>
                            <div style="margin-top:10px">
                                <label>View results as:</label>
                                <input type="radio" name="view" id="view_table" value="table"
                                    <%=request.getParameter("view") == null || request.getParameter("view").equals("table") ? "checked" : ""%>>
                                <label for="view_table">table</label>

                                <input type="radio" name="view" id="view_heatmap" value="heatmap"
                                    <%=request.getParameter("view") != null && request.getParameter("view").equals("heatmap") ? "checked" : ""%>>
                                <label for="view_heatmap">heatmap</label>

                                <%--<br/>--%>
                                <%--<i><input type="checkbox" name="expand_efo" id="expand_efo" value="expand_efo"--%>
                                    <%--<%=null == request.getParameter("expand_efo") ? "checked" : ""%>--%>
                                    <%--<%=null != request.getParameter("expand_efo") && request.getParameter("expand_efo").equals("expand_efo") ? "checked" : ""%>>--%>
                                <%--<label for="expand_efo">expand search with <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=EFO" title="Experimental Factor Ontology">EFO</a> ontology</label>--%>
                                <%--</i>--%>
                                <input type="hidden" name="expand_efo" id="expand_efo" value="on"/>                            
                            </div>
                        </td>
                        <td valign="top" align="center" style="width:150px">
                            <div class="atlasHelp">
                                <div style="font-size: 0px; line-height: 0%; width: 0px; border-bottom: 20px solid pink; border-left: 10px solid white;border-right: 10px solid white;">&nbsp;</div>
                                <div style="background-color:pink; text-align:left; height:100%; width: 140px;padding:5px">
                                    Please enter an experimental condition or tissue, etc. Start typing and autosuggest will help you narrow down your choice.
                                </div>
                            </div>
                        </td>
                    </tr>
                </table>
            </td>
            <td valign="top">
                <div style="position:relative;padding-left:15px;top:10px">
                    <a class="smallgreen" href="decounts.jsp">gene counts</a><br/>
                    <a id="atlasHelpToggle" class="smallgreen" href="#">show help</a><br/>
                    <a class="smallgreen" href="qrs">advanced query</a>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3" align="center" valign="center">
                <b>Hint:</b> query for condition 'kidney' leaving blank the genes field for the most active genes in that tissue.
            </td>
        </tr>
    </table>

    <input type="hidden" name="view"/>
</form>

<form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
    <div style="position:relative;top:150px;text-align:center">
    for news and updates, subscribe to the <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:
        <table align="center">
            <tr valign="middle">
                <td>
                    <input style="border: thin solid lightgray;font-size:11px" type="text" name="email" size="10" value=""/>
                </td>
                <td>
                    <input style="font-size:11px" type="submit" name="email-button" value="subscribe"/>
                 </td>
            </tr>
        </table>
</div>
</form>

<span style="position:fixed;bottom:30px;right:0px;padding-right:10px">ArrayExpress Atlas Build <%=svnBuildString.replaceAll("\\$","")%></span>
<jsp:include page="end_body.jsp"></jsp:include>
