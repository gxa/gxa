<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.gxa.web.AtlasSearchService" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    AtlasSearchService searchService = (AtlasSearchService)application.getAttribute(Atlas.SEARCH_SERVICE.key());
    request.setAttribute("service", searchService);
%>
<option value="" selected="">Any species</option>
<c:forEach var="s" items="${service.allAvailableAtlasSpecies}">
<option value="${f:escapeXml(s)}">${f:escapeXml(s)}</option>
</c:forEach>
