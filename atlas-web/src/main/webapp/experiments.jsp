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
    <h1>
        <c:out value="${gene.geneName}"/> in <c:out value="${ef}"/>&nbsp;<c:out value="${efv}"/>:
        <c:out value="${f:length(exps.ups)}"/> up, <c:out value="${f:length(exps.downs)}"/> down
    </h1>
    <table>
        <c:forEach var="e" items="${expsi}">
            <tr>
                <th><a href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=${u:escapeURL(gene.geneIdentifier)}&species=&displayInsitu=on&exp_query=${u:escapeURL(e.experimentAccessment)}"><c:out value="${e.experimentAccessment}"/></a></th>
                <td><em><c:out value="${e.experimentName}"/></em><p><c:out value="${e.experimentDescription}"/></p></td>
                <c:choose>
                    <c:when test="${e.updn == 'UP'}">
                        <th class="expup">&#8593;</th>
                        <td class="expup"><fmt:formatNumber value="${e.pvalue}" maxFractionDigits="10"/></td>
                    </c:when>
                    <c:otherwise>
                        <th class="expdn">&#8595;</th>
                        <td class="expdn"><fmt:formatNumber value="${e.pvalue}" maxFractionDigits="10"/></td>
                    </c:otherwise>
                </c:choose>
            </tr>
        </c:forEach>       
    </table>
</c:if>

