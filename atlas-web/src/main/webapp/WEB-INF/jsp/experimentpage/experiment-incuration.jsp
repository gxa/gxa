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
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-gxa"/>
    <wro4j:all name="bundle-gxa-page-experiment"/>
</head>

<tmpl:stringTemplateWrap name="page">

    <div class="contents" id="contents">

        <div class="ae_pagecontainer">

            <jsp:include page="../includes/atlas-header.jsp"/>

            <div class="column-container">
                <div class="left-column">

                    <span class="section-header-1" style="vertical-align: baseline">${exp.description}</span>

                    <p>
                        ${exp.abstract}
                        <c:if test="${exp.pubmedId!=null}">(<a class="external" href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                                                               target="_blank">PubMed ${exp.pubmedId}</a>)</c:if>
                    </p>
                </div>

                <div class="right-column">
                    <jsp:include page="experiment-header.jsp"/>
                </div>

                <div class="clean">&nbsp;</div>
            </div>

            <h3>We are sorry, this experiment is currently in curation. Please come back soon.</h3>
        </div>
    </div>
</tmpl:stringTemplateWrap>
</html>
