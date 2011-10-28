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

<c:set var="timeStart" value="${u:currentTime()}"/>
<jsp:useBean id="gene" type="uk.ac.ebi.gxa.web.controller.GenePageGene" scope="request"/>
<jsp:useBean id="differentiallyExpressedFactors" type="java.util.List<ae3.model.ExperimentalFactor>" scope="request"/>
<jsp:useBean id="anatomogramMap" type="java.util.Collection<uk.ac.ebi.gxa.anatomogram.AnatomogramArea>" scope="request"/>
<jsp:useBean id="hasAnatomogram" type="java.lang.Boolean" scope="request" />
<jsp:useBean id="ef" class="java.lang.String" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>

    <tmpl:stringTemplate name="genePageHead">
        <tmpl:param name="gene" value="${gene}"/>
    </tmpl:stringTemplate>

    <meta name="Description"
          content="${gene.geneName} (${gene.geneSpecies}) - Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <!--[if IE]>
    <script language="javascript" type="text/javascript" src="${contextPath}/scripts/excanvas.min.js"></script>
    <![endif]-->

    <c:import url="/WEB-INF/jsp/includes/global-inc-head.jsp"/>
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-jquery-flot-old"/>
    <wro4j:all name="bundle-gxa"/>
    <wro4j:all name="bundle-gxa-grid-support"/>
    <wro4j:all name="bundle-gxa-page-gene"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<script type="text/javascript">
$(document).ready(function() {
    $("#heatmap_tbl").tablesorter({headers: {2: {sorter: false}}});

    function markRow(el) {
        if (el) {
            old = $(".heatmap_over");
            old.removeClass("heatmap_over");
            old.addClass("heatmap_row");
            el.className = "heatmap_over";
        } else {
            $(".heatmap_over").removeClass("heatmap_over");
        }
    }

    var expList = atlas.geneExperimentList({
        gene: ${gene.geneId},
        listTarget: "experimentList",
        listTemplate: "experimentListTemplate",
        pageTarget: "experimentListPage",
        pageTemplate: "experimentListPageTemplate",
        pageSize: 5
    });

    window.FilterExps = function(el, efv, ef) {
        expList.load({ef:ef, efv:efv});
        markRow(el);
    };

    window.FilterExpsEfo = function(ef, efo) {
        expList.load({ef:ef, efo:efo});
    };

    expList.load({ef:"${ef}"});
});
</script>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
<div class="ae_pagecontainer">

<jsp:include page="../includes/atlas-header.jsp"/>

<table cellspacing="0" cellpadding="0" border="0" width="100%">
    <tr>
        <td style="vertical-align:top;">
            <table class="gene-properties">
                <tr>
                    <td align="left" class="page-title">${gene.geneName}
                        <div style="font:normal">
                            <c:import url="../includes/apilinks.jsp">
                                <c:param name="apiUrl" value="geneIs=${gene.geneIdentifier}"/>
                            </c:import>
                        </div>
                    </td>
                    <td style="vertical-align: text-bottom">${gene.geneSpecies}</td>
                </tr>

                <tr>
                    <td colspan="2" align="left" style="padding-bottom:1em;padding-top:1em">
                        ${f:escapeXml(gene.geneDescription)}
                    </td>
                </tr>

                <c:if test="${!empty gene.synonyms}">
                    <tr>
                        <td class="propname">Synonyms</td>
                        <td align="left">${u:join(gene.synonyms, ", ")}</td>
                    </tr>
                </c:if>

                <c:if test="${!empty gene.orthologs}">
                    <tr>
                        <td class="propname">Orthologs</td>

                        <td align="left">
                            <c:set var="orthoIds" value=""/>
                            <c:forEach var="ortholog" items="${gene.orthologs}">
                                <a href="${contextPath}/gene/${ortholog.geneIdentifier}"
                                   target="_self"
                                   title="Gene Atlas Data For ${ortholog.geneName} (${ortholog.geneSpecies})">${ortholog.geneName}
                                    (${ortholog.geneSpecies})
                                </a>&nbsp;
                                <c:set var="orthoIds" value="${orthoIds}${ortholog.geneIdentifier}+"/>
                            </c:forEach>
                            (<a href="${contextPath}/qrs?gprop_0=&gval_0=${orthoIds}${gene.geneIdentifier}&fexp_0=UP_DOWN&fact_0=&specie_0=&fval_0=(all+conditions)&view=hm"
                                target="_self">Compare orthologs</a>)
                        </td>
                    </tr>
                </c:if>

                <c:forEach items="${gene.geneFields}" var="field">
                        <tr class="${field.default ? '' : 'expandable'}"
                            style="${field.default ? '' : 'display:none'}">
                            <td class="propname">${f:escapeXml(field.name)}</td>
                            <td align="left">
                                <c:choose>
                                    <c:when test="${!empty field.urlAware}">
                                        <c:forEach items="${field.valuesWithUrl}" var="value" varStatus="s">
                                            <a href="${value.url}" target="_blank">${f:escapeXml(value.value)}</a><c:if test="${!s.last}">, </c:if>
                                        </c:forEach>
                                    </c:when>
                                    <c:when test="${!empty atlasProperties.geneIdentifierLinks[prop]}">
                                        <a href="${f:replace(atlasProperties.geneIdentifierLinks[prop], '$$', atlasGene.geneIdentifier)}"
                                           target="_blank">${f:escapeXml(atlasGene.geneIdentifier)}
                                    </c:when>
                                    <c:otherwise>
                                        ${u:join(field.values, ", ")}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                </c:forEach>

                <tr>
                    <td class="propname">Search EB-eye</td>
                    <td align="left">
                        <a title="Show gene annotation" target="_blank"
                           href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=allebi&query=&quot;${gene.geneIdentifier}&quot;&requestFrom=ebi_index&submit=+FIND+">
                            ${gene.geneIdentifier}
                        </a>
                    </td>
                </tr>

                <tr>
                    <td colspan="2" style="padding-top:5px">
                        <a href="#"
                           onclick="$(this).parents('table:first').find('.expandable').show();$(this).remove();return false;"><img
                                src="${pageContext.request.contextPath}/images/expp.gif" alt="" border="none"> Show more
                            properties</a>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>

<table cellspacing="0" cellpadding="0" border="0" style="width:100%;margin-top:40px">
<tr>
<td valign="top" width="50%">
<table>
<tr>
    <td class="section-header-1" style="padding-right:20px;">
        <c:choose>
            <c:when test="${not empty ef}">
                ${f:escapeXml(atlasProperties.curatedEfs[ef])}
                <div style="font-size:10px; font-weight:normal;"><a
                        href="${pageContext.request.contextPath}/gene/${gene.geneIdentifier}">&lt;&lt;view all
                    experimental factors</a></div>
            </c:when>
            <c:otherwise>
                Experimental Factors
            </c:otherwise>
        </c:choose>
    </td>
</tr>

<tr>
<td style="padding-top: 3px; width:60%;">

<table cellpadding="0" style="width:100%;" border="0">
    <tr>

        <c:forEach var="experimentalFactor" items="${differentiallyExpressedFactors}" varStatus="i" end="5">
        <c:if test="${(i.index mod 2)==0}">
    </tr>
    <tr>
        </c:if>

        <td style="vertical-align:top; padding-right:20px;">
            <c:if test="${empty ef}">
                <div class="section-header-2 nowrap"
                     style="width:200px;">${f:escapeXml(atlasProperties.curatedEfs[experimentalFactor.name])}</div>
            </c:if>
            <p style="margin-top:3px; margin-bottom:5px">studied in
                <c:forEach var="experiment" items="${experimentalFactor.experiments}" varStatus="i_e">
                    <c:if test="${(i_e.index<5)||(not empty ef)}">
                        <a href="${pageContext.request.contextPath}/experiment/${experiment}/${gene.geneIdentifier}"
                           title="${experiment}">${experiment}</a><c:if test="${!i_e.last}">, </c:if>
                    </c:if>
                    <c:if test="${i_e.last}">
                        <c:if test="${(i_e.count>=5)&&(empty ef)}">
                            ... (${i_e.count} experiments)
                        </c:if>
                    </c:if>
                </c:forEach>
            </p>

             <!-- Output legend for the first experimental factor only -->
             <c:if test="${i.index==0}">
                   <table cellspacing="2" cellpadding="0" border="0" width="100%">
                        <tr>
                            <td style="vertical-align:middle;">
                                <div class="sq">
                                    <div class="udduo"></div>
                                    <div class="uddnval">3</div>
                                    <div class="udupval">1</div>
                                </div>
                            </td>
                            <td>
                                <div style="padding-left:0; font-size:12px;">
                                    Number of published studies where the gene is <span
                                        style="color:red">over</span>/<span
                                        style="color:blue">under</span> expressed compared to the gene's overall mean
                                    expression level in the study.
                                </div>
                            </td>
                        </tr>
                    </table>
            </c:if>

            <c:if test='${experimentalFactor.name=="organism_part" && hasAnatomogram}'>
                <br/>

                <map name="anatomogram" class="anatomogram">
                    <c:forEach var="area" items="${anatomogramMap}">
                        <area shape="poly" onclick="FilterExpsEfo('organism_part', '${area.efo}');return false;" coords="${u:join(area.coordinates, ",")}" href="#"/>
                    </c:forEach>
                </map>

                <img src="${pageContext.request.contextPath}/webanatomogram/${gene.geneIdentifier}.png"
                         alt="anatomogram" border="none" usemap="#anatomogram"/>

                <c:if test="${empty ef}">
                    <div style="padding-left:0;font-size:10px;">
                       <c:choose>
                           <c:when test="${experimentalFactor.name != 'organism_part'}">
                                <a href="${pageContext.request.contextPath}/gene/${gene.geneIdentifier}?ef=${experimentalFactor.name}">show
                            this factor only&gt;&gt;</a>
                            </c:when>
                            <c:otherwise>
                                 <a href="${pageContext.request.contextPath}/gene/${gene.geneIdentifier}?ef=${experimentalFactor.name}">show
                            expression data for <b>all</b> values of this factor&gt;&gt;</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>

            </c:if>
            <c:if test='${experimentalFactor.name!="organism_part" || not hasAnatomogram || not empty ef}'>
                <%--
                generic ef - the above clause imposes the following rules:
                1. in multi-experimental factor experiment view:
                a. ef != 'organism_part' => always show a table
                b. ef == 'organism_part'
                - anatomogram exists => show anatomogram
                - anatomogram doesn't exist => show table
                2. in single-experimental factor experiment view: (ef != null):
                a. as 1a
                b ef == 'organism_part' => show anatomogram (if exists) AND the table
                --%>

                <table class="atlas-grid hoverable heatmap" cellpadding="0" cellspacing="0" border="0"
                       style="margin-top:5px; margin-left:0px; width:100%;">
                    <thead>
                    <tr>
                        <th >Factor Value</th>
                        <th style="width:20px;">
                            <span style="font-size:8px">U/D</span>
                        </th>
                        <th>Experiments</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${not empty ef}">
                            <c:set var="values" value="${experimentalFactor.values}"/>
                        </c:when>
                        <c:otherwise>
                            <c:set var="values" value="${experimentalFactor.topValues}"/>
                        </c:otherwise>
                    </c:choose>
                    <c:forEach var="e" items="${values}" varStatus="i">
                        <c:if test='${e.efv!="(empty)"}'>
                            <tr class="heatmap_row"
                                onclick="FilterExps(this,'${u:escapeJS(e.efv)}','${u:escapeJS(e.ef)}'); return false;"
                                title="${e.efv}">
                                <td style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD; min-width: 100px;border-left:1px solid #CDCDCD;">
                                                                    <span style="font-weight: bold">
                                                                            ${e.efv}
                                                                    </span>
                                </td>

                                <c:set var="ud" value="${e.payload}"/>
                                <c:choose>
                                    <c:when test="${empty ud || ud.ups + ud.downs + ud.nones == 0}">
                                        <td class="counter"><c:choose><c:when test="${j.first}">
                                            <div class="osq"></div>
                                        </c:when></c:choose></td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs == 0 && ud.nones > 0}">
                                        <td class="acounter" style="color:black;"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is non-differentially in ${ud.nones} experiment(s)."
                                                >
                                            <div class="osq">${ud.nones}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones == 0}">
                                        <td class="acounter upback"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is overexpressed in ${ud.ups} experiment(s)."
                                                >
                                            <div class="osq">${ud.ups}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones == 0}">
                                        <td class="acounter downback"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) is underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="osq">${ud.downs}</div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs == 0 && ud.nones > 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and not differentially expressed in ${ud.nones} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="nuduo"></div>
                                                <div class="nunoval">${ud.nones}</div>
                                                <div class="nuupval">${ud.ups}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups == 0 && ud.downs > 0 && ud.nones > 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) underexpressed in ${ud.downs} and not differentially expressed in ${ud.nones} experiment(s).">
                                            <div class="sq">
                                                <div class="ndduo"></div>
                                                <div class="ndnoval">${ud.nones}</div>
                                                <div class="nddnval">${ud.downs}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:when test="${ud.ups > 0 && ud.downs > 0 && ud.nones == 0}">
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="udduo"></div>
                                                <div class="uddnval">${ud.downs}</div>
                                                <div class="udupval">${ud.ups}</div>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:otherwise>
                                        <td class="acounter"
                                            title="in ${f:escapeXml(e.efv)} (${f:escapeXml(e.ef)}) overexpressed in ${ud.ups} and underexpressed in ${ud.downs} experiment(s)."
                                                >
                                            <div class="sq">
                                                <div class="tri"></div>
                                                <div class="tdnval">${ud.downs}</div>
                                                <div class="tupval">${ud.ups}</div>
                                                <div class="tnoval">${ud.nones}</div>
                                            </div>
                                        </td>
                                    </c:otherwise>
                                </c:choose>

                                <td style="border:1px solid #CDCDCD; padding-left:5px;">
                                                            <span style="font-size:smaller">
                                                            <c:forEach var="experimentID" items="${ud.experiments}"
                                                                       varStatus="i_e">
                                                                <c:if test="${(i_e.index<5)}">
                                                                    <a href="${pageContext.request.contextPath}/experiment/${experimentalFactor.experimentAccessions[experimentID]}/${gene.geneIdentifier}"
                                                                       onclick="window.location=this.href;event.stopPropagation();return false;"
                                                                       title="${experimentalFactor.experimentAccessions[experimentID]}">${experimentalFactor.experimentAccessions[experimentID]}</a><c:if
                                                                        test="${!i_e.last}">, </c:if>
                                                                </c:if>
                                                                <c:if test="${i_e.last}">
                                                                    <c:if test="${(i_e.count>=5)}">
                                                                        ... (${i_e.count} experiments)
                                                                    </c:if>
                                                                </c:if>
                                                            </c:forEach>
                                                            </span>
                                </td>

                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>


                <div style="padding-left:0px">
                    <c:if test="${(experimentalFactor.moreValuesCount>0)&&(empty ef)}">
                        ${experimentalFactor.moreValuesCount} more value(s).
                    </c:if>
                    <c:if test="${empty ef}">
                        <div style="font-size:10px;">
                            <a href="${pageContext.request.contextPath}/gene/${gene.geneIdentifier}?ef=${experimentalFactor.name}">show
                                this factor only&gt;&gt;</a>
                        </div>
                    </c:if>
                </div>
                <!--generic ef -->
            </c:if>

            <br/><br/>

        </td>
        </c:forEach>

        <c:forEach var="k" begin="${4 - i.index mod 2}" end="2">
            <td></td>
        </c:forEach>
    </tr>
</table>

<c:forEach var="experimentalFactor" items="${differentiallyExpressedFactors}" varStatus="i" begin="6">
    <c:if test="${empty ef}">
        <div class="section-header-2 nowrap">${f:escapeXml(atlasProperties.curatedEfs[experimentalFactor.name])}</div>
    </c:if>
    studied in
    <c:forEach var="experiment" items="${experimentalFactor.experiments}" varStatus="i_e">
        <c:if test="${(i_e.index<5)||(not empty ef)}">
            <a href="${pageContext.request.contextPath}/experiment/${experiment}/${gene.geneIdentifier}">${experiment}</a><c:if
                test="${!i_e.last}">, </c:if>
        </c:if>
        <c:if test="${i_e.last}">
            <c:if test="${(i_e.count>=5)&&(empty ef)}">
                ... (${i_e.count} experiments)
            </c:if>
        </c:if>
    </c:forEach>
    <c:if test="${empty ef}">
        <div style="font-size:10px;">
            <a href="${pageContext.request.contextPath}/gene/${gene.geneIdentifier}?ef=${experimentalFactor.name}">show
                this factor only&gt;&gt;</a>
        </div>
    </c:if>
    <br/><br/>
</c:forEach>
</td>
</tr>
</table>

</td>
<td valign="top" align="left" width="50%" id="experimentList">
    <jsp:include page="experiment-list-template.jsp" />
</td>
</tr>
</table>

<c:set var="timeFinish" value="${u:currentTime()}"/>
<div align="center">Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}"/> secs.</div>

</div>
<!-- ae_pagecontainer -->
</div>
<!-- /id="contents" -->

</tmpl:stringTemplateWrap>
</html>
