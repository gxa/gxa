<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<jsp:useBean id="atlasQueryService" class="ae3.service.structuredquery.AtlasStructuredQueryService" scope="application"/>
<option value="" selected="">Any species</option>
<c:forEach var="s" items="${atlasQueryService.speciesOptions}">
<option value="${f:escapeXml(s)}">${u:upcaseFirst(f:escapeXml(s))}</option>
</c:forEach>
