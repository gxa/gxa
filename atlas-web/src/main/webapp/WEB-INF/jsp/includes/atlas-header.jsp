<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<c:set var="isHomePage"><c:out value="${param.isHomePage}" default="false"/></c:set>

<table style=";width:100%;border-bottom:1px solid #DEDEDE;">
    <tr>
        <td align="left" valign="bottom">
            <a href='${pageContext.request.contextPath}/'>
                <img src="${pageContext.request.contextPath}/images/atlas-logo.png"
                     alt="Gene Expression Atlas"
                     title="Atlas Data Release ${atlasStatistics.dataRelease}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.factorValueCount} conditions"
                     <c:if test="${! isHomePage}">width="55"</c:if>
                     border="0"/>
            </a>
        </td>
        <td align="right" valign="bottom">
            <c:if test="${! isHomePage}">
                <a href="${pageContext.request.contextPath}/">home</a> |
            </c:if>
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks"
                                                                                         style="font-weight: bold; display: none">thanks!</span>
            |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>