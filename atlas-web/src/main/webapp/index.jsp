<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("service", ArrayExpressSearchService.instance());
%>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="structured-query.css" type="text/css" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.cookie.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="jquery.autocomplete.js"></script>
    <script type="text/javascript" src="jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="structured-query.js"></script>

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
                initSimpleForm();

                $("#atlasHelpToggle").click(toggleAtlasHelp);


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
<form name="atlasform" action="qrs">
    <table style="margin:auto;height:200px">
        <tr valign="top">
            <td valign="top"><img border="0" src="atlasbeta.jpg"/></td>
            <td>
                <table>
                    <tr>
                        <td>
                            <label class="label" for="gene0">Genes</label>
                        </td>
                        <td/>
                        <td>
                            <label class="label" for="species0">Organism</label>
                        </td>
                        <td>
                            <label class="label" for="fval0">Conditions</label>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <input type="hidden" name="gprop_0" id="gprop0" value="">
                            <input type="text" name="gval_0" id="gene0" style="width:150px" value="" />
                        </td>
                        <td>
                            <select name="fexp_0">
                                <c:forEach var="s" items="${service.structQueryService.geneExpressionOptions}">
                                    <option value="${f:escapeXml(s[0])}">${f:escapeXml(s[1])} in</option>
                                </c:forEach>
                            </select>
                        </td>
                        <td>
                            <select name="specie_0" id="species0">
                                <option value="">(any)</option>
                                <c:forEach var="s" items="${service.allAvailableAtlasSpecies}">
                                    <option value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
                                </c:forEach>
                            </select>
                        </td>
                        <td>
                            <input type="hidden" name="fact_0" value="">
                            <input type="text" name="fval_0" id="fval0" style="width:150px" value="" />
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
                    <a class="smallgreen" href="qrs?struct">advanced query</a>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3" align="center" valign="center">
                <b>Hint:</b> query for condition 'kidney' leaving blank the genes field for the most active genes in that tissue.
            </td>
        </tr>
    </table>

    <input type="hidden" name="view" value="hm"/>
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

<jsp:include page="end_body.jsp"></jsp:include>
