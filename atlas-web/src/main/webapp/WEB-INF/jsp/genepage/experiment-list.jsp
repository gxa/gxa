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

<jsp:useBean id="noAtlasExps" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="target" type="java.lang.String" scope="request"/>

<script type="text/javascript">
    if (ExperimentList) {
        ExperimentList.drawPagination(${noAtlasExps}, '${target}');
    }
</script>

<table align="left">
    <tr>
        <td id="expHeader_td" class="sectionHeader" style="vertical-align: top">Expression Profiles</td>
        <td align="right">
            <span id="allStudiesLink" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; "></span>
            <span id="pagination" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; "></span>
        </td>
    </tr>
    <tr>
        <td>
            <table width="100%">
                <tr>
                    <td align="left" valign="top" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                        <div id="pagingSummary" class="header">
                            ${noAtlasExps} experiment${noAtlasExps > 1 ? "s" : ""} showing differential expression <c:if
                                test="${! empty target}">in "${target}"</c:if>
                        </div>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td colspan="2">
            <div id="experimentListPage">
                <jsp:include page="/WEB-INF/jsp/genepage/experiment-list-page.jsp"/>
            </div>
        </td>
    </tr>
</table>



