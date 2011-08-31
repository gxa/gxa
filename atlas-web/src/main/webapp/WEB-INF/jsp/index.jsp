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
<%@include file="includes/global-inc.jsp" %>

<jsp:useBean id="atlasQueryService" class="ae3.service.structuredquery.AtlasStructuredQueryService"
             scope="application"/>
<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="atlasStatistics" type="uk.ac.ebi.microarray.atlas.model.AtlasStatistics" scope="application"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="indexPageHead"/>

    <meta name="Description"
          content="Gene Expression Atlas is a semantically enriched database of meta-analysis statistics for condition-specific gene expression.">
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/atlas-searchform.css" type="text/css"/>


    <style type="text/css">

        .alertNotice {
            padding: 50px 10px 10px 10px;
            text-align: center;
            font-weight: bold;
        }

        .alertNotice > p {
            margin: 10px;
        }

        .alertHeader {
            color: red;
        }

        #centeredMain {
            width: 740px;
            margin: 0 auto;
            padding: 50px 0;
            height: 100%;
        }

        .roundCorner {
            background-color: #EEF5F5;
        }

    </style>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>

    <jsp:include page="/WEB-INF/jsp/includes/query-includes.jsp"/>

    <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/atlas-searchform.js"></script>

</head>

<tmpl:stringTemplateWrap name="page">

    <div id="contents" class="contents">
        <div id="centeredMain">

            <jsp:include page="/WEB-INF/jsp/includes/atlas-header.jsp">
                <jsp:param name="isHomePage" value="true"/>
            </jsp:include>
            <jsp:include page="/WEB-INF/jsp/includes/atlas-searchform.jsp">
                <jsp:param name="isAdvanced" value="false"/>
            </jsp:include>

            <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.corner.js"></script>
            <script type="text/javascript">
                $(document).ready(function () {
                    $("div.roundCorner").corner();
                });
            </script>

            <c:if test="${atlasProperties.alertNotice != ''}">
                <div class="alertNotice">
                    <p class="alertHeader">Downtime Notice!</p>

                    <p>${atlasProperties.alertNotice}</p>
                </div>
            </c:if>

            <div style="margin-top:50px">
                <c:if test="${atlasProperties.theMOTD != ''}">
                    <div class="roundCorner" style="margin-bottom:10px;padding:10px">${atlasProperties.theMOTD}</div>
                </c:if>
                <div style="float:left; width:200px;" class="roundCorner">
                    <div style="padding:10px">
                        <div style="font-weight:bold;margin-bottom:5px">Atlas Data Release <c:out
                                value="${atlasStatistics.dataRelease}"/>:
                        </div>
                        <table cellpadding="0" cellspacing="0" width="100%">
                            <c:if test="${atlasStatistics.newExperimentCount > 0}">
                                <tr>
                                    <td class="atlastable" align="left">
                                        <fmt:parseDate var="releaseDate" pattern="MM-yyyy"
                                                       value="${atlasProperties.lastReleaseDate}" parseLocale="en_US"/>
                                        <fmt:formatDate var="isoDate" pattern="yyyy-MM-dd'T'HH:mm:ss"
                                                        value="${releaseDate}"/>
                                        <a href="${pageContext.request.contextPath}/experiment/index.htm?q=loaddate:[${isoDate}Z TO *]">new</a>
                                        experiments
                                    </td>
                                    <td class="atlastable" align="right"><c:out
                                            value="${atlasStatistics.newExperimentCount}"/></td>
                                </tr>
                            </c:if>
                            <tr>
                                <td class="atlastable" align="left">total <a
                                        href="${pageContext.request.contextPath}/experiment/index.htm"
                                        title="Atlas Experiment Index">experiments</a></td>
                                <td class="atlastable" align="right"><c:out
                                        value="${atlasStatistics.experimentCount}"/></td>
                            </tr>

                            <tr>
                                <td class="atlastable" align="left">total <a
                                        href="${pageContext.request.contextPath}/gene/index.htm"
                                        title="Atlas Gene Index">genes</a></td>
                                <td class="atlastable" align="right"><c:out value="${atlasStatistics.geneCount}"/></td>
                            </tr>

                            <tr>
                                <td class="atlastable" align="left">assays</td>
                                <td class="atlastable" align="right"><c:out value="${atlasStatistics.assayCount}"/></td>
                            </tr>
                            <tr>
                                <td class="atlastable" align="left">conditions</td>
                                <td class="atlastable" align="right"><c:out
                                        value="${atlasStatistics.factorValueCount}"/></td>
                            </tr>
                            <tr>
                                <td class="atlastable" colspan="2">&nbsp;</td>
                            </tr>
                            <tr>
                                <td class="atlastable" align="left"><a href="http://www.ebi.ac.uk/efo">EFO</a> version
                                </td>
                                <td class="atlastable" align="right"><c:out
                                        value="${atlasQueryService.efo.version}"/></td>
                            </tr>
                        </table>
                    </div>
                </div>

                <div style="float:left;width:530px;margin-left:10px;" class="roundCorner">
                    <div style="padding:10px">
                        <div style="font-weight:bold;margin-bottom:5px">Gene Expression Atlas</div>

                        The Gene Expression Atlas is a semantically enriched database of
                        meta-analysis based summary statistics over a curated subset of
                        ArrayExpress Archive, servicing queries for condition-specific gene
                        expression patterns as well as broader exploratory searches for
                        biologically interesting genes/samples.
                    </div>
                </div>

                <div style="clear:both;"></div>
            </div>
            <div style="font-family: Verdana, helvetica, arial, sans-serif; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center; margin-top:50px">
                <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
                    For news and updates, subscribe to the <a
                        href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
                    <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
                    <input type="submit" name="email-button" value="Subscribe"/>
                </form>
            </div>
        </div>
    </div>

</tmpl:stringTemplateWrap>
</html>
