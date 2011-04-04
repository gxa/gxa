<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="display" uri="http://displaytag.sf.net" %>
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
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>
<jsp:useBean id="experiments" type="java.util.Collection" scope="request"/>
<jsp:useBean id="count" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="total" type="java.lang.Integer" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <script type="text/javascript" src='<c:url value="/scripts/jquery-1.4.3.min.js"/>'></script>

    <script type="text/javascript" src='<c:url value="/scripts/feedback.js"/>'></script>

    <link rel="stylesheet" href='<c:url value="/atlas.css"/>' type="text/css"/>
    <link rel="stylesheet" href='<c:url value="/geneView.css"/>' type="text/css"/>

    <link rel="stylesheet" href='<c:url value="/blue/style.css"/>' type="text/css" media="print, projection, screen"/>
    <link rel="stylesheet" href='<c:url value="/structured-query.css"/>' type="text/css"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

    <div class="contents" id="contents">
        <div class="ae_pagecontainer">

            <jsp:include page="../includes/atlas-header.jsp"/>

            <div style="margin:40px; font-weight:bold; font-size:larger; text-align:center;">
                Complete list of experiments curated and loaded in the Gene Expression Atlas
            </div>

            <display:table name="${experiments}" sort="external" requestURI="./index.html"
                           requestURIcontext="false" id="experiment" class="heatmap"
                           size="${total}" partialList="true" pagesize="${count}">
                <display:column property="accession" sortable="true" sortName="accession"
                                url="/experiment/${experiment.accession}"/>
                <display:column property="description" sortable="false"/>
                <display:column title="Experiment Factors">
                    <dl>
                        <dt style="white-space:nowrap;">${f:length(experiment.experimentFactors)}&nbsp;EFs</dt>
                        <dd>
                            <c:forEach var="factor" items="${experiment.experimentFactors}">
                                ${f:escapeXml(atlasProperties.curatedGeneProperties[factor])}
                                [${f:length(experiment.factorValuesForEF[factor])}&nbsp;FVs]<br/>
                            </c:forEach>
                        </dd>
                    </dl>
                </display:column>
            </display:table>
        </div>
    </div>

</tmpl:stringTemplateWrap>
</html>
