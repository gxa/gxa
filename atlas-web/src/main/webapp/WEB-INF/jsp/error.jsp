<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>

<c:import url="/WEB-INF/jsp/includes/start_head.jsp"/>
Gene Expression Atlas
<c:import url="/WEB-INF/jsp/includes/end_head.jsp"/>


<c:import url="/WEB-INF/jsp/includes/query-includes.jsp"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>

<c:import url="/WEB-INF/jsp/includes/start_body_no_menus.jsp"/>

<c:import url="/WEB-INF/jsp/includes/end_menu.jsp"/>

<div id="ae_pagecontainer">
    <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;">
        <c:import url="/WEB-INF/jsp/includes/simpleform.jsp">
            <c:param name="logolink" value="true"/>
        </c:import>
    </div>
</div>

<div align="center" style="color:red;font-weight:bold;margin-top:150px">
    <c:choose>
        <c:when test="${!empty errorMessage}">
            <c:out value="${errorMessage}"/>
        </c:when>
        <c:otherwise>
            We're sorry an error has occurred! We will try to remedy this as soon as possible. Responsible parties have been notified and heads will roll.
        </c:otherwise>
    </c:choose>
    <br/><br/><br/>Please try another search.
</div>


<form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
    <div style="position: absolute; bottom:80px; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center">
        For news and updates, subscribe to the
        <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
        <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
        <input type="submit" name="email-button" value="Subscribe"/>
    </div>
</form>

<c:import url="/WEB-INF/jsp/includes/end_body.jsp"/>
