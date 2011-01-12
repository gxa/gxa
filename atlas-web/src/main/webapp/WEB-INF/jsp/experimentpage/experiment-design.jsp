<%@ page language="java" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>

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

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="exp" type="ae3.model.AtlasExperiment" scope="request"/>
<jsp:useBean id="arrayDesigns" type="java.lang.String[]" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>

    <tmpl:stringTemplate name="expPageHead"/>

    <jsp:include page="../includes/query-includes.jsp"/>

<script type="text/javascript">
$(function() {
	$("a.lightbox").lightBox(); // Select all links with lightbox class
});
</script>

<!--[if IE]><script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->
<script language="javascript"
        type="text/javascript"
        src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.selectboxes.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-ui-1.7.2.atlas.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/experiment.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/listview.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-ui-1.7.2.atlas.css" type="text/css"/>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-lightbox-0.5/js/jquery.lightbox-0.5.js"></script>

<style type="text/css">
    @media print {
        body, .contents, .header, .contentsarea, .head {
            position: relative;
        }
    }
    </style>
</head>

<body onLoad="if(navigator.userAgent.indexOf('MSIE') != -1) {document.getElementById('head').allowTransparency = true;}">
<tmpl:stringTemplateWrap name="page">


<script type="text/javascript">
    jQuery(document).ready(function()
    {
        $("#squery").tablesorter({});
    });
</script>

<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <div class="column-container">
            <div class="left-column">

                <span class="sectionHeader" style="vertical-align: baseline">${exp.description}</span>

                <p>
                    ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                        target="_blank">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>
            </div>

            <div class="right-column">
                <jsp:include page="experiment-header.jsp"/>
            </div>

            <div class="clean">&nbsp;</div>
        </div>

        <div class="hrClear" style="margin-top:20px;width:100%;">
        <hr/>

        <table id="squery" class="tablesorter">
        <thead>
            <tr class="header">
                <th style="border-left:none" class="padded">Assay</th>
                <th style="border-left:none" class="padded">Array</th>
                <c:forEach var="factor" items="${experimentDesign.factors}" varStatus="r">
		<th>${f:escapeXml(atlasProperties.curatedEfs[factor.name])}</th>
                </c:forEach>
            </tr>
        </thead>

        <tbody>

        <c:forEach var="assay" items="${experimentDesign.assays}" varStatus="r">
            <tr>
                <td class="padded genename" style="border-left:none">
                        ${assay.name}
                </td>
                <td>${assay.arrayDesignAccession}</td>
                <c:forEach var="factorValue" items="${assay.factorValues}" varStatus="r">
                <td class="padded wrapok">
                        ${factorValue}
                </td>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </div>

    </div>
</div>

</tmpl:stringTemplateWrap>

</body></html>