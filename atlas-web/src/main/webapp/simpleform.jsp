<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<script type="text/javascript">
    function toggleAtlasHelp(e) {
        if ($("div.atlasHelp").is(":hidden")) {
            showAtlasHelp();
        } else {
            hideAtlasHelp();
        }
        return false;
    }

    function showAtlasHelp() {
        if ($("div.atlasHelp").is(":hidden")) {
            $("div.atlasHelp").slideToggle();
            $("#atlasHelpToggle").text("hide help");
        }
        $.cookie('atlas_help_state', 'shown');
    }

    function hideAtlasHelp() {
        if ($("div.atlasHelp").is(":visible")) {
            $("div.atlasHelp").slideToggle();
            $("#atlasHelpToggle").text("show help");
        }
        $.cookie('atlas_help_state', 'hidden');
    }

    $(document).ready(function() {
        $("div.roundCorner").corner();

        atlas.initSimpleForm();

        $("#atlasHelpToggle").click(toggleAtlasHelp);


        if (($.cookie('atlas_help_state') == "shown") && ($("div.atlasHelp").is(":hidden"))) {
            showAtlasHelp();
        } else if (($.cookie('atlas_help_state') == "hidden") && ($("div.atlasHelp").is(":visible"))) {
            hideAtlasHelp();
        }

    });
</script>
<style type="text/css">
    .label {
        font-size: 10px;
    }

    .atlasHelp {
        display: none;
        text-align: center;
    }

    .atlasHelp .div1 {
        font-size: 0px;
        line-height: 0%;
        width: 0px;
        border-bottom: 20px solid #EEF5F5;
        border-left: 10px solid white;
        border-right: 10px solid white;
    }

    .atlasHelp .div2 {
        background-color: #EEF5F5;
        text-align: left;
        height: 100%;
        width: 140px;
        padding: 5px;
    }

</style>
<table style="width:100%;border-bottom:1px solid #dedede">
    <tr>
        <td align="left" valign="bottom">
            <c:if test="${param.logolink}"><a href="<%=request.getContextPath()%>/"></c:if>
            <img src="<%= request.getContextPath()%>/images/atlas-logo.png" alt="Gene Expression Atlas" title="Atlas Data Release ${f:escapeXml(service.stats.dataRelease)}: ${service.stats.numExperiments} experiments, ${service.stats.numAssays} assays, ${service.stats.numEfvs} conditions" border="0">
            <c:if test="${param.logolink}"></a></c:if>
        </td>

        <td width="100%" valign="bottom" align="right">
            <a href="<%=request.getContextPath()%>/help/AboutAtlas">about the project</a> |
            <a href="<%=request.getContextPath()%>/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="<%=request.getContextPath()%>/help/AtlasDasSource">das</a> |
            <a href="<%=request.getContextPath()%>/help/AtlasApis">api</a> <b>new</b> |
            <a href="<%=request.getContextPath()%>/help">help</a>
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
                        <a id="atlasHelpToggle" class="smallgreen" style="font-size:12px" href="#">show help</a>
                        <!--<a class="smallgreen" href="decounts.jsp">gene counts</a><br/>-->
                        <a class="smallgreen" style="font-size:12px" href="qrs?struct"><nobr>advanced search</nobr></a>
                        
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

    <input type="hidden" name="view" value="hm"/>
</form>
