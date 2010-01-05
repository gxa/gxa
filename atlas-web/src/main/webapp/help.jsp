<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="start_head.jsp"></jsp:include>
<c:out value="${f:substringAfter(pageContext.request.requestURI, '/help/')}"/> - Gene Expression Atlas Help
<jsp:include page="end_head.jsp"></jsp:include>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css" media="print, projection, screen" />
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>


<style type="text/css">
    /* TODO: display contents appropriately */

    #ae_pagecontainer  .toc {
        position:absolute;
        top:0;
        right: 0;
        width: 30%;
        background-color:white;
    }


    .toc ul {
        margin-left: 0;
        padding-left: 1.5em;
        text-indent: -0.5em;
    }

    #ae_pagecontainer .toc td {
        vertical-align:top;
        padding:0 0 0 20px;
        margin:0;
    }

    #toctitle h2 { margin-top:0; }

</style>

<script type="text/javascript">
    $(document).ready(function () {
        var name = '.toc';
        $('.toc ~ hr').hide();

        var menuYloc = 0;
        $(window).scroll(function () {
            var offset = menuYloc+$(document).scrollTop()+"px";
            $(name).animate({top:offset},{duration:500,queue:false});
        });
    });
</script>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<!-- location bar -->
<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right:10px;">
            <a href="${pageContext.request.contextPath}/" title="Gene Expression Atlas Homepage"><img border="0" width="55" src="${pageContext.request.contextPath}/images/atlas-logo.png" alt="Gene Expression Atlas"/></a>
        </td>
        <td align="right" valign="bottom">
            <a href="${pageContext.request.contextPath}">home</a> |
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>
<div style="position:relative;width:100%;">
<div style="padding-bottom:50px;width:70%">
    <u:renderWiki/>
</div>
</div>

</div><!-- /id="ae_pagecontainer" -->
</div><!-- /id="contents" -->

<jsp:include page="end_body.jsp"></jsp:include>

