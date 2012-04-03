<%--
  ~ Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

<jsp:useBean id="exp" type="ae3.model.AtlasExperiment" scope="request"/>

<c:if test="${not empty exp.experiment.assets}">
    <p>Figures:</p>
    <ul>
        <c:forEach var="a" items="${exp.experiment.assets}" varStatus="status">
            <li>
                <a href="${pageContext.request.contextPath}/assets?eid=${exp.accession}&amp;asset=${a.fileName}" rel="lightbox"
                   class="lightbox" title="${a.description}" alt="${a.description}">
                    ${a.name}
                </a>
            </li>
        </c:forEach>
    </ul>
</c:if>
