<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<table style="width:100%;border-bottom:1px solid #dedede">
    <tr>
        <td align="left" valign="bottom">
            <img src="images/atlas-logo.png" alt="Atlas of Gene Expression" title="Atlas Data Release <c:out value="${service.stats.dataRelease}"/>: <c:out value="${service.stats.numExperiments}"/> experiments, <c:out value="${service.stats.numAssays}"/> assays, <c:out value="${service.stats.numEfvs}"/> conditions"/>
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
            <td>
               <label class="label" for="view">View</label>
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
