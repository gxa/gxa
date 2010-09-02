<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/plain;charset=UTF-8" language="java" %>
<jsp:useBean id="atlasStatistics" class="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>
Atlas Data Release <c:out value="${atlasStatistics.dataRelease}"/>: <c:out value="${atlasStatistics.experimentCount}"/> experiments, <c:out value="${atlasStatistics.assayCount}"/> assays, <c:out value="${atlasStatistics.factorValueCount}"/> conditions
