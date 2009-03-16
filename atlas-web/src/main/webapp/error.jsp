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
    <link rel="stylesheet" href="atlas.css" type="text/css" />
    <link rel="stylesheet" href="structured-query.css" type="text/css" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="scripts/jquery.min.js"></script>
    <script type="text/javascript" src="scripts/jquery.cookie.js"></script>
    <script type="text/javascript" src="scripts/jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
    <script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="scripts/common-query.js"></script>
    <script type="text/javascript" src="scripts/feedback.js"></script>

    <script type="text/javascript">
        function toggleAtlasHelp(e) {
            if($("div.atlasHelp").is(":hidden")) {
                showAtlasHelp();
            } else {
                hideAtlasHelp();
            }
            if(e && typeof(e.stopPropagation) == 'function')
                e.stopPropagation();
            return false;
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
                atlas.initSimpleForm();

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
            text-align:center;
        }

        .atlasHelp .div1 {
            font-size: 0px; line-height: 0%; width: 0px;
            border-bottom: 20px solid #EEF5F5;
            border-left: 10px solid white;border-right: 10px solid white;
        }

        .atlasHelp .div2 {
            background-color: #EEF5F5; text-align:left; height:100%; width: 140px;padding:5px;
        }

        #newexplist {
            font-size: 10px;
            text-align:left;margin-top:70px;margin-right:auto;margin-left:auto;width:740px;
        }

        #newexplist table {
            border: none;
            padding: 0;
            margin: 10px 0 0 0;
        }

        #newexplist td {
            padding: 5px 10px 0 0;
            vertical-align:top;
            font-size: 10px;
        }
    </style>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals=" />
<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>

<div id="ae_pagecontainer">
    <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;" >
        <table style="width:100%;border-bottom:1px solid #dedede">
            <tr>
                <td align="left" valign="bottom">
                    <a href="./"><img border="0" src="images/atlas-logo.png" alt="Atlas of Gene Expression" title="Atlas Data Release <c:out value="${service.stats.dataRelease}"/>: <c:out value="${service.stats.numExperiments}"/> experiments, <c:out value="${service.stats.numAssays}"/> assays, <c:out value="${service.stats.numEfvs}"/> conditions"/></a>
                </td>

                <td width="100%" valign="bottom" align="right">
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
                    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
                    <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> |
                    <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
                </td>
                <td align="right" valign="bottom">
                </td>
            </tr>
        </table>
<!--        <div style="width:100%;font-size:10px;text-align:right">-->
        <form name="atlasform" action="qrs" id="simpleform">
            <table style="width: 100%;border:none;margin:20px 0 0 0;padding:0">
                <tr>
                    <td><label class="label" for="gene0">Genes</label></td>
                    <td></td>
                    <td>
                        <label class="label" for="species0">Organism</label>
                    </td>
                    <td>
                        <label class="label" for="fval0">Conditions</label>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <td>
                        <input type="hidden" name="gprop_0" id="gprop0" value="${query.simple ? f:escapeXml(query.geneQueries[0].factor) : ''}">
                        <input type="text" class="value" name="gval_0" id="gene0" style="width:150px" value="${query.simple ? f:escapeXml(query.geneQueries[0].jointFactorValues) : ''}" /><br>
                    </td>
                    <td>
                        <select name="fexp_0" id="expr0">
                            <c:forEach var="s" items="${service.structQueryService.geneExpressionOptions}">
                                <option value="${f:escapeXml(s[0])}">${f:escapeXml(s[1])} in</option>
                            </c:forEach>
                        </select>
                        <input type="hidden" name="fact_0" value="">
                    </td>
                    <td>
                        <select name="specie_0" id="species0" style="width:180px">
                            <option value="">(any)</option>
                            <c:forEach var="s" items="${service.allAvailableAtlasSpecies}">
                                <option value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
                            </c:forEach>
                        </select>
                    </td>
                    <td>
                        <input type="text" class="value" name="fval_0" id="fval0" style="width:150px" value="${query.simple ? f:escapeXml(query.conditions[0].jointFactorValues) : ''}" />
                    </td>
                    <td align="right">
                        <input type="submit" value="Search Atlas" class="searchatlas">
                        <div style="position:relative;width:100%;">
                            <div style="position:absolute;right:0;overflow:visible;height:auto;text-align:right;top:10px;">
                                <a id="atlasHelpToggle" class="smallgreen" href="#">show help</a>
                                <!--<a class="smallgreen" href="decounts.jsp">gene counts</a><br/>-->
                                <a class="smallgreen" href="qrs?struct"><nobr>advanced search</nobr></a>
                            </div>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td class="label" colspan="3"><span style="font-style: italic" class="label">e.g. ASPM, "p53 binding"</span></td>
                    <td class="label" colspan="2"><span style="font-style: italic" class="label">e.g. liver, cancer, diabetes</span></td>
                </tr>
                <tr>
                    <td class="label" valign="top"><div class="atlasHelp">
                        <div class="div1">&nbsp;</div>
                        <div class="div2">
                            Please enter a gene name, synonym, Ensembl or UniProt identifier, GO category, etc.
                        </div>
                    </div></td>
                    <td colspan="2"></td>
                    <td class="label" valign="top"><div class="atlasHelp">
                        <div class="div1">&nbsp;</div>
                        <div class="div2">
                            Please enter an experimental condition or tissue, etc. Start typing and autosuggest will help you narrow down your choice.
                        </div>
                    </div></td>
                    <td></td>
                </tr>
                <tr>
                    <td class="label" colspan="4"></td>
                    <td class="label">        
                    </td>
                </tr>
            </table>

<!--
            <div id="newexplist">
                ArrayExpress Atlas Version <c:out value="${u:getProp('atlas.buildNumber')}"/> covers <c:out value="${service.stats.numExperiments}"/> experiments, <c:out value="${service.stats.numAssays}"/> assays, and <c:out value="${service.stats.numEfvs}"/> conditions. <c:out value="${f:length(service.stats.newExperiments)}"/> new experiments were loaded since the last data release:
                <table>
                    <c:forEach var="e" items="${service.stats.newExperiments}" varStatus="i">
                        <c:if test="${i.index < 5}">
                            <tr>
                                <td><nobr><b><a href="qrs?gprop_0=&gval_0=&fexp_0=UP_DOWN&fact_0=experiment&specie_0=&fval_0=${u:escapeURL(e.accession)}"><c:out value="${e.accession}"/></a></b></nobr></td>
                                <td><c:out value="${e.assayCount}"/></td>
                                <td><c:out value="${e.descr}" escapeXml="false"/></td>
                            </tr>
                        </c:if>
                    </c:forEach>
                </table>
            </div>
-->
            <input type="hidden" name="view" value="hm"/>
        </form>

    </div>
</div>

<div align="center" style="color:red;font-weight:bold;margin-top:150px">
    <c:choose>
        <c:when test="${!empty errorMessage}">
            <c:out value="${errorMessage}" />
        </c:when>
        <c:otherwise>
            We're sorry an error has occurred! We will try to remedy this as soon as possible. Responsible parties have been notified and heads will roll.
        </c:otherwise>
    </c:choose>
    <br/><br/><br/>Please try another search.
</div>


        <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
            <div style="position: absolute; bottom:80px; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center">
                            For news and updates, subscribe to the <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
                            <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
                            <input type="submit" name="email-button" value="Subscribe" />
            </div>
        </form>

<jsp:include page="end_body.jsp"></jsp:include>
