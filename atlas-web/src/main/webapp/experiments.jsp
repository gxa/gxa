<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.structuredquery.ExperimentList" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.model.AtlasGene" %>
<%@ page import="ae3.dao.AtlasDao" %>
<%@ page import="ae3.dao.AtlasObjectNotFoundException" %>
<%
    String geneIdKey = request.getParameter("gene");
    String factor = request.getParameter("ef");
    String factorValue = request.getParameter("efv");

    if(geneIdKey != null && factor != null && factorValue != null)
    {
        request.setAttribute("ef", factor);
        request.setAttribute("efv", factorValue);

        ExperimentList experiments = ArrayExpressSearchService.instance().getExperiments(geneIdKey, factor, factorValue);
        request.setAttribute("exps", experiments);
        request.setAttribute("expsi", experiments.iterator());

        try {
            AtlasGene gene = AtlasDao.getGene(geneIdKey);
            request.setAttribute("gene", gene);
        } catch(AtlasObjectNotFoundException e) {

        }
    }
%>
<c:if test="${!empty gene && !empty exps}">
    <div class="head">
        <a href="gene?gid=${f:escapeXml(gene.geneIdentifier)}"><b>${f:escapeXml(empty gene.geneName ? gene.geneIdentifier : gene.geneName)}</b></a> in <b>${f:escapeXml(efv)}</b> (<fmt:message key="head.ef.${ef}"/>)<br>
        overexpressed in <b>${f:length(exps.ups)}</b> and underexpressed in <b>${f:length(exps.downs)}</b> experiment(s)
    </div>
    <div class="exptable">
    <table>
        <c:forEach var="e" items="${expsi}" varStatus="s">
            <tr class="${s.last ? 'last' : 'notlast'}">
                <td class="explot">
                    <b><c:out value="${e.experimentAccession}"/></b>:
                    <c:out value="${e.experimentName}"/>
                    <div class="plot" id="explot_${e.experimentId}"></div>
                    <div class="legend" id="explot_${e.experimentId}_legend"></div>
                    <div style="margin-top:5px;font-size:10px;">
                        Show <a target="_blank" title="Show expression profile in ArrayExpress Warehouse" id="explot_${e.experimentId}_link" href="/microarray-as/aew/DW?queryFor=gene&gene_query=${u:escapeURL(gene.geneIdentifier)}&species=&displayInsitu=on&exp_query=${u:escapeURL(e.experimentAccession)}">expression profile</a>
			&nbsp;/&nbsp;
			<a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=${u:escapeURL(e.experimentAccession)}&detailedview=on">experiment details</a>
                    </div>
                </td>
                <c:choose>
                    <c:when test="${e.updn == 'UP'}">
                        <td class="expup"><nobr>&#8593;&nbsp;<fmt:formatNumber value="${e.pvalue}" pattern="#.###E0" /></nobr></td>
                    </c:when>
                    <c:otherwise>
                        <td class="expdn"><nobr>&#8595;&nbsp;<fmt:formatNumber value="${e.pvalue}" pattern="#.###E0" /></nobr></td>
                    </c:otherwise>
                </c:choose>
            </tr>
        </c:forEach>       
    </table>
    </div>
</c:if>

