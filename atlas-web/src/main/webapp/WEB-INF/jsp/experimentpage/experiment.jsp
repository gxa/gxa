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
<jsp:useBean id="expSpecies" type="java.util.List<java.lang.String>" scope="request"/>
<jsp:useBean id="jsMap" type="java.util.Map<java.lang.String, java.lang.Object>" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="expPageHead">
        <tmpl:param name="experiment" value="${exp}"/>
    </tmpl:stringTemplate>

    <!--[if IE]>
    <script type="text/javascript" src="${contextPath}/scripts/excanvas.min.js"></script>
    <![endif]-->

    <c:import url="/WEB-INF/jsp/includes/global-inc-head.jsp"/>
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-jquery-flot"/>
    <wro4j:all name="bundle-gxa"/>
    <wro4j:all name="bundle-gxa-grid-support"/>
    <wro4j:all name="bundle-gxa-page-experiment"/>

<script type="text/javascript">
    $(function() {
        $("a.lightbox").lightbox({
            fileLoadingImage: "${contextPath}/scripts/jquery-lightbox/images/loading.gif",
		    fileBottomNavCloseImage: "${contextPath}/scripts/jquery-lightbox/images/closelabel.gif"
        });
    });
</script>

<script id="source" type="text/javascript">
    <c:forEach var="ef" varStatus="s" items="${exp.experimentFactors}">
    curatedEFs['${ef.name}'] = '${u:escapeJS(ef.displayName)}';
    </c:forEach>

    $(document).ready(function() {

        initPlotTabs({
            selected: "box",
            onchange: function(tabId) {
                expPage.changePlotType(tabId);
            }
        });

        function initPlotTabs(opts) {
            var tabs = {
                curr: null,

                select: function(tabId) {
                    if (!tabId) {
                        var sel = $(".btabs .sel")[0];
                        if (sel) {
                            tabId = sel.id.split("_")[1];
                        }
                    }

                    if (this.curr == tabId) {
                        return;
                    }

                    if (this.curr) {
                        this.tabEl(this.curr).removeClass("sel");
                        this.tabContentEl(this.curr).hide();
                    }

                    this.tabEl(tabId).addClass("sel");
                    this.tabContentEl(tabId).show();

                    if (this.curr && opts.onchange) {
                        opts.onchange.call(this, tabId);
                    }

                    this.curr = tabId;
                },

                tabEl: function(tabId) {
                    return $(".btabs #tab_" + tabId);
                },

                tabContentEl: function(tabId) {
                    return $("#tab_content_" + tabId);
                }

            };

            tabs.select(opts.selected);

            $(".btabs li").each(function() {
                $(this).bind("click", function() {
                    tabs.select(this.id.split("_")[1]);
                });
            });

        }

        window.expPage = new ExperimentPage(${u:toJson(jsMap)});
    });
</script>


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

        <div class="column-container exp-page">
            <div class="left-column">

                <span class="section-header-1" style="vertical-align:baseline">${exp.description}</span>

                <p>
                    <c:import url="../includes/apilinks.jsp">
                        <c:param name="apiUrl" value="experiment=${exp.accession}"/>
                        <c:param name="callback" value="expPage.getApiLink"/>
                    </c:import>
                </p>

                <p>
                        ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                        target="_blank" class="external">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>

                <c:choose>
                    <c:when test="${isRNASeq}">
                        <h3>High-throughput sequencing experiment, analyzed with the <a
                                href="http://bioinformatics.oxfordjournals.org/content/early/2011/01/13/bioinformatics.btr012.short">ArrayExpressHTS</a>
                            pipeline.</h3>
                    </c:when>
                    <c:otherwise>
                        <h3>Data shown for array design: <span id="arrayDesign"></span></h3>
                    </c:otherwise>
                </c:choose>

                <div id="result_cont" style="margin-top:20px; margin-bottom:10px;">

                    <div>
                        <div id="EFpagination" class="pagination_ef"></div>
                        <div class="clean"></div>
                    </div>

                    <div style="position:relative;width:100%; margin-top:10px;">

                        <div id="tab_content_large" style="display:none">
                            <table cellpadding="0" cellspacing="0" style="padding:0;">
                                <tr>
                                    <td>

                                        <div id="plot_large" class="bigplot"
                                             style="width:700px;height:150px;padding:0;"></div>
                                        <div id="plot_overview_large"
                                             style="width:700px;height:60px;padding:0;"></div>
                                        <div id="legend_large"></div>
                                    </td>
                                    <td valign="bottom">
                                        <div id="zoomControls_large"></div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div id="tab_content_box" style="display:none">
                            <table cellpadding="0" cellspacing="0" style="padding:0;">
                                <tr>
                                    <td>
                                        <div id="plot_box" class="bigplot"
                                             style="width:700px;height:150px;padding:0;"></div>
                                        <div id="plot_overview_box"
                                             style="width:700px;height:60px;padding:0;"></div>
                                        <div id="legend_box"></div>
                                    </td>
                                    <td valign="bottom">
                                        <div id="zoomControls_box"></div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div class="btabs" style="width:100%;margin-top:10px">
                            <ul>
                                <li id="tab_box">box plot</li>
                                <li id="tab_large">line plot</li>
                            </ul>
                        </div>

                    </div>
                </div>

            </div>


            <div class="right-column">
                <jsp:include page="experiment-header.jsp"/>
            </div>

            <div class="clean">&nbsp;</div>

        </div>

<script id="expressionValueTableRowTemplate1" type="text/x-jquery-tmpl">
    <tr style="height:25px;">
        <td class="padded designElementRow" style="text-align:center;" id="results_\${deIndex}">
            <a onclick="expPage.addOrRemoveDesignElement(\${deIndex});return false;">
                <span title="Add to plot" class="chartIcon add-btn">&nbsp;</span></a>
        </td>
        <td class="padded genename">
            <a href="${pageContext.request.contextPath}/gene/\${geneIdentifier}" title="${geneName}">\${geneName}</a>
        </td>
        <td class="padded">\${deAcc}</td>
        <c:if test="${exp.typeString == 'RNA_SEQ'}">
          <c:choose>
              <c:when test="${expSpecies[0] == 'homo sapiens'}">
                  <c:set var="ensembl_organism" value="Homo_sapiens"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'mus musculus'}">
                  <c:set var="ensembl_organism" value="Mus_musculus"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'drosophila melanogaster'}">
                  <c:set var="ensembl_organism" value="Drosophila_melanogaster"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'saccharomyces cerevisiae'}">
                  <c:set var="ensembl_organism" value="Saccharomyces_cerevisiae"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'rattus norvegicus'}">
                  <c:set var="ensembl_organism" value="Rattus_norvegicus"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'gallus gallus'}">
                  <c:set var="ensembl_organism" value="Gallus_gallus"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'equus caballus'}">
                  <c:set var="ensembl_organism" value="Equus_caballus"/>
              </c:when>
              <c:when test="${expSpecies[0] == 'sus scrofa'}">
                  <c:set var="ensembl_organism" value="Sus_scrofa"/>
              </c:when>


              <c:otherwise>
                    <c:set var="ensembl_organism" value="Homo_sapiens"/>
            </c:otherwise>
          </c:choose>
            <td class="padded wiggle"><a target="_blank"
                                         href="http://www.ensembl.org/${ensembl_organism}/Location/View?g=\${geneIdentifier};contigviewbottom=url:http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/wiggle/\${geneIdentifier}_${exp.accession}_\${ef_enc}_\${efv_enc}.wig">Genome View</a></td>
        </c:if>
        <td class="padded">\${ef}</td>
        <td class="padded">\${efv}</td>
        <td class="padded">\${expr}</td>
        <td class="padded number">{{html tstat}}</td>
        <td class="padded number">{{html pvalue}}</td>
    </tr>
</script>

<script id="geneToolTipTemplate" type="text/x-jquery-tmpl">
    <div>
      <div class="genename">
        <b>\${name}</b> \${identifiers}
       </div>
        {{tmpl(properties) "#geneToolTipPropertyTemplate"}}
    </div>
</script>

<script id="geneToolTipPropertyTemplate" type="text/x-jquery-tmpl">
    <b>\${name}:</b> \${value}<br/>
</script>

        <div id="topPagination" class="pagination_ie alignRight"></div>

        <div class="hrClear">
            <form id="expressionListFilterForm" action="javascript:alert('error');">
                <table id="squery" class="atlas-grid experiment-stats">
                    <tr class="header">
                        <th align="left" width="20" class="padded" style="border-bottom:1px solid #CDCDCD">&nbsp;</th>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Gene</th>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Design Element</th>
                        <c:if test="${exp.typeString=='RNA_SEQ'}">
                            <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Genome View</th>
                        </c:if>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Experimental Factor</th>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Factor Value</th>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">UP/DOWN</th>

                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">T-Statistic</th>
                        <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">P-Value</th>
                    </tr>

                    <tr>
                        <td class="padded" width="20">&nbsp;</td>
                        <td class="padded" colspan="${exp.typeString == 'RNA_SEQ' ? 3 :  2}">
                            <input type="text" class="value" id="geneFilter" style="width:99%;"/>
                        </td>
                        <td class="padded" colspan="2">
                            <select id="efvFilter" style="width:100%;">
                                <option value="||">All factor values</option>
                                <c:forEach var="EF" items="${exp.experimentFactors}">
                                    <optgroup label="${f:escapeXml(EF.displayName)}">
                                        <c:forEach var="EFV" items="${exp.factorValuesForEF[EF]}">
                                            <option value='${EF.name}||${f:escapeXml(EFV)}'>${f:escapeXml(EFV)}</option>
                                        </c:forEach>
                                    </optgroup>
                                </c:forEach>
                            </select>
                        </td>
                        <td class="padded">
                            <select id="updownFilter" style="width:100%;">
                                <option value="CONDITION_ANY">All expressions</option>
                                <option value="CONDITION_UP_OR_DOWN">up or down</option>
                                <option value="CONDITION_UP">up</option>
                                <option value="CONDITION_DOWN">down</option>
                                <option value="CONDITION_NONDE">non d.e.</option>
                            </select>
                        </td>
                        <td class="padded" colspan="2">
                            <input type="submit" value="SEARCH" style="visibility:hidden"/>
                        </td>
                    </tr>

                    <tbody id="expressionTableBody">
                    </tbody>
                </table>

                <div class="errorMessage" id="divErrorMessage">No matching results found. See <a
                     onclick="expPage.clearQuery(); return false;" href="#">all</a> genes.
                </div>

            </form>
        </div>

    </div>
    <!-- ae_pagecontainer -->
</div>
<!-- /id="contents" -->

</tmpl:stringTemplateWrap>
</html>
