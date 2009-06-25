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
Gene Expression Atlas
<jsp:include page="end_head.jsp"></jsp:include>


<jsp:include page="query-includes.jsp" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<script type="text/javascript" src="scripts/common-query.js"></script>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals=" />
<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>

<div id="ae_pagecontainer">
    <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;" >
        <jsp:include page="simpleform.jsp">
            <jsp:param name="logolink" value="true"/>
        </jsp:include>
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
