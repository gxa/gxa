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

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="exp" type="ae3.model.AtlasExperiment" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expPageHead">
        <tmpl:param name="experiment" value="${exp}"/>
    </tmpl:stringTemplate>

    <c:import url="/WEB-INF/jsp/includes/global-inc-head.jsp"/>
    <wro4j:all name="bundle-jquery" />
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-gxa" />
    <wro4j:all name="bundle-gxa-grid-support" />
    <wro4j:all name="bundle-gxa-page-experiment-design"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

<script type="text/javascript">
    $(document).ready(function () {
        atlas.ajaxBatchLoader({
            url:"/experimentDesignTable",
            limit:300,
            onNextBatch:renderNextBatch,
            onComplete:applySorting,
            totalCount: "experimentDesign.total"
        }).load({eacc:"${exp.accession}"}, "#loadingIndicator");

        function renderNextBatch(batchOffset, data) {
            data = data || {};
            data = data.experimentDesign || {};

            if (batchOffset == 0) {
                $("#expDesignTable").html($("#expDesignTableTmpl").tmpl(data));
            }
            $("#expDesignTable tbody").append($("#expDesignTableRowsTmpl").tmpl(data));
        }

        function applySorting() {
            $("#expDesignTable > table").tablesorter({
                widgets:['zebra'],
                cssHeader:"sortable",
                cssAsc:"order1",
                cssDesc:"order2"
            });
        }

        $(".inlineDownloadLink").inlineDownloadLink();
    });
</script>

<script id="expDesignTableTmpl" type="text/x-jquery-tmpl">
    <table class="atlas-grid sortable experiment-design">
        <thead>
        <tr>
            <th>Assay</th>
            <th>Array</th>
            {{each propertyNames}}
                <th>\${this}</th>
            {{/each}}
        </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
</script>

<script id="expDesignTableRowsTmpl" type="text/x-jquery-tmpl">
    {{each propertyValues}}
    <tr>
        <td class="padded assayName" style="border-left:none">
            \${this.assayAcc}
        </td>
        <td><nobr>\${this.arrayDesignAcc}</nobr></td>
        {{each this.propertyValues}}
        <td class="padded wrapok">
            \${this}
        </td>
        {{/each}}
    </tr>
    {{/each}}
</script>

<div class="contents" id="contents">
    <div class="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <div class="column-container">
            <div class="left-column">

                <span class="section-header-1" style="vertical-align:baseline">${exp.description}</span>

                <p>
                    ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a class="external" href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                        target="_blank" class="external">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>
            </div>

            <div class="right-column">
                <div style="float:right">
                    <jsp:include page="experiment-header.jsp"/>
                    <div>
                        <a href="${pageContext.request.contextPath}/experiment/${exp.accession}"
                           style="font-size:12px;font-weight:bold;">View experiment analytics&nbsp;&rsaquo;&rsaquo;</a>
                    </div>
                    <jsp:include page="experiment-header-assets.jsp"/>
                </div>
            </div>

            <div style="clear:both; width:100%; height:1px; padding:0; margin:0;" ></div>
        </div>

        <div id="loadingIndicator">&nbsp;</div>
        <div style="margin: 5px;">
            <a class="export2TsvLink" target="_blank" rel="nofollow"
               href="${pageContext.request.contextPath}/experimentDesignTable?format=tsv&eacc=${exp.accession}">Export Experiment Design as Tab-Delimited file</a>
        </div>
        <div id="expDesignTable" style="overflow: auto;position: relative;"></div>
    </div>
</div>

</tmpl:stringTemplateWrap>
</html>
