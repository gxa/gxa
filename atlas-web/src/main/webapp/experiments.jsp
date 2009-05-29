<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<c:if test="${!empty gene && !empty exps}">
    <div class="head">
        <a href="gene?gid=${f:escapeXml(gene.geneIdentifier)}"><b>${f:escapeXml(empty gene.geneName ? gene.geneIdentifier : gene.geneName)}</b></a> in <b>${f:escapeXml(efv)}</b> (<fmt:message key="head.ef.${ef}"/>)<br>
        overexpressed in <b>${numup}</b> and underexpressed in <b>${numdn}</b> experiment(s)
    </div>
    <div class="exptable">
    <table>
        <c:set var="prevExpId" value="0"/>
        <c:set var="prevEf" value=""/>
        <c:forEach var="e" items="${expsi}" varStatus="s">
            <c:choose>
                <c:when test="${prevExpId != e.experimentId}">
                    <tr class="${s.last ? 'last' : 'notlast'}">
                        <td class="explot">
                            <b><c:out value="${e.experimentAccession}"/></b>:
                            <c:out value="${e.experimentName}"/>
                            <table border="0" cellpadding="0" cellspacing="0"><tr>
                                <td><div class="plot" id="explot_${s.index}"></div></td>
                                <td><div class="legend" id="legend_explot_${s.index}"></div></td>
                            </tr></table>
                            <div style="margin-top:5px;font-size:10px;">
                                Show <a target="_blank" title="Show expression profile in ArrayExpress Warehouse" id="link_explot_${s.index}" href="/microarray-as/aew/DW?queryFor=gene&gene_query=${u:escapeURL(gene.geneIdentifier)}&species=&displayInsitu=on&exp_query=${u:escapeURL(e.experimentAccession)}">expression profile</a>
                                &nbsp;/&nbsp;
                                <a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=${u:escapeURL(e.experimentAccession)}&detailedview=on">experiment details</a>
                            </div>
                            <span id="eid_explot_${s.index}">${f:escapeXml(e.experimentId)}</span>
                            <span id="ef_explot_${s.index}">${f:escapeXml(e.ef)}</span>
                            <span id="efv_explot_${s.index}">${f:escapeXml(e.efv)}</span>
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
                </c:when>
                <c:when test="${prevExpId == e.experimentId && prevEf != e.ef}">
                    <tr class="${s.last ? 'last' : 'notlast'}">
                        <td class="explot">
                            <table border="0" cellpadding="0" cellspacing="0"><tr>
                                <td><div class="plot" id="explot_${s.index}"></div></td>
                                <td><div class="legend" id="legend_explot_${s.index}"></div></td>
                            </tr></table>
                            <div style="margin-top:5px;font-size:10px;">
                                Show <a target="_blank" title="Show expression profile in ArrayExpress Warehouse" id="link_explot_${s.index}" href="/microarray-as/aew/DW?queryFor=gene&gene_query=${u:escapeURL(gene.geneIdentifier)}&species=&displayInsitu=on&exp_query=${u:escapeURL(e.experimentAccession)}">expression profile</a>
                                &nbsp;/&nbsp;
                                <a target="_blank" title="Show experiment details in ArrayExpress Archive" href="/microarray-as/ae/browse.html?keywords=${u:escapeURL(e.experimentAccession)}&detailedview=on">experiment details</a>
                            </div>
                            <span id="eid_explot_${s.index}">${f:escapeXml(e.experimentId)}</span>
                            <span id="ef_explot_${s.index}">${f:escapeXml(e.ef)}</span>
                            <span id="efv_explot_${s.index}">${f:escapeXml(e.efv)}</span>
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
                </c:when>
            </c:choose>
            <c:set var="prevEf" value="${e.ef}" />           
            <c:set var="prevExpId" value="${e.experimentId}" />
        </c:forEach>
    </table>
    </div>
</c:if>

