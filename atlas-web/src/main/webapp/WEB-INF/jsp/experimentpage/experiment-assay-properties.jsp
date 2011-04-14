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
<%@include file="../includes/global-inc.jsp" %>

<jsp:useBean id="arrayDesign" type="java.lang.String" scope="request"/>
<jsp:useBean id="efs" type="java.util.Collection<ae3.model.ExperimentalFactorsCompactData>" scope="request"/>
<jsp:useBean id="scs" type="java.util.Collection<ae3.model.SampleCharacteristicsCompactData>" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expPageHead">
       <tmpl:param name="experiment" value="${exp}"/>
    </tmpl:stringTemplate>

    <jsp:include page="../includes/query-includes.jsp"/>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-ui-1.7.2.atlas.min.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-ui-1.7.2.atlas.css" type="text/css"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>

    <script type="text/javascript">
        $(document).ready(function() {
            $("#squery").tablesorter({});
        });
    </script>
</head>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
    <div class="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <div class="column-container">
            <div class="left-column">

                <span class="sectionHeader" style="vertical-align: baseline">${exp.description}</span>

                <p>
                    ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                        target="_blank" class="external">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>
            </div>

            <div class="right-column">
                <jsp:include page="experiment-header.jsp"/>
            </div>

            <div class="clean">&nbsp;</div>
        </div>

        <h3>Data shown for array design: ${arrayDesign}</h3>

        <div class="hrClear" style="margin-top:20px;width:100%;">
            <hr/>

            <table id="squery" class="tablesorter">
                <thead>
                <tr class="header">
                        <th width="60px">Assay</th>
                    <c:forEach var="ef" items="${efs}">
                        <th>${f:escapeXml(atlasProperties.curatedEfs[ef.name])}</th>
                    </c:forEach>
                    <c:forEach var="sc" items="${scs}">
                        <th>${f:escapeXml(atlasProperties.curatedEfs[sc.name])} (sc)</th>
                    </c:forEach>
                </tr>
                </thead>

                <tbody>
                <c:set var="numberOfAssays" value="${f:length(efs[0].assayEfvs)}" />
                <c:forEach var="i" begin="0" end="${numberOfAssays - 1}">
                    <tr>
                        <td>${i + 1}</td>
                        <c:forEach var="ef" items="${efs}">
                            <td>${ef.efvs[ef.assayEfvs[i]]}</td>
                        </c:forEach>
                         <c:forEach var="sc" items="${scs}">
                            <td>${sc.scvs[sc.assayScvs[i]]}</td>
                        </c:forEach>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

    </div>
</div>

</tmpl:stringTemplateWrap>
</html>