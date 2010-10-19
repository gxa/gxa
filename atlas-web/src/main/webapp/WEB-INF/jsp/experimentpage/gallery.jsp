<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <c:forEach var="a" items="${exp.assets}" varStatus="status">
            <li>
            <a href="${pageContext.request.contextPath}/assets/${exp.accession}/${a.fileName}" rel="lightbox" class="lightbox" title="${a.description}" alt="${a.description}">
                ${a.name}
            </a>
            </li>
        </c:forEach>
