<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right: 10px;">
            <a href='<c:url value="/"/>'>
                <img border="0" width="55" src="${pageContext.request.contextPath}/images/atlas-logo.png"
                     alt="Gene Expression Atlas"
                     title="Atlas Data Release ${atlasStatistics.dataRelease}: ${atlasStatistics.experimentCount} experiments, ${atlasStatistics.assayCount} assays, ${atlasStatistics.propertyValueCount} conditions"/>
            </a>
        </td>
        <td align="right" valign="bottom">
            <a href="${pageContext.request.contextPath}/">home</a> |
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a>
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks"
                                                                                         style="font-weight: bold; display: none">thanks!</span>
            |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>