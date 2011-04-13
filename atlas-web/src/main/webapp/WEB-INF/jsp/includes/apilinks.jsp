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

<c:set value="http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/api/vx?${param.apiUrl}" var="apiUrl" />
<a style="font-size: 12px;font-weight: normal;" title="Get API URL for this result set in XML format" href="#" onclick="atlas.showApiLinks('${u:escapeJS(apiUrl)}', ${empty param.callback ? 'null' : param.callback});return false;">
    <img src="${pageContext.request.contextPath}/images/JSON.png" alt="REST API" border="none"/>
    <img src="${pageContext.request.contextPath}/images/XML.png" alt="REST API" border="none"/>
</a>

<div id="apilinks">
    <div class="abs">
        <div class="closebox">close</div>
        <p>Please copy/paste those URLs into your code to get same results in machine-readable formats:</p>

        <form action="#" onsubmit="return false;">
            <table>
                <tr>
                    <td width="60px"><label for="jsonapilink">for&nbsp;<a href="http://www.json.org">JSON</a></label>&nbsp;</td>
                    <td><input id="jsonapilink" class="value" type="text" value="${apiUrl}&format=json"
                                                 style="width:98%" onclick="atlas.copyText(this);"></td>
                </tr>
                <tr>
                    <td width="60px"><label for="xmlapilink">for&nbsp;<a href="http://www.w3.org/XML/">XML</a></label>&nbsp;</td>
                    <td><input id="xmlapilink" class="value" type="text" value="${apiUrl}&format=xml"
                                                 style="width:98%" onclick="atlas.copyText(this);"></td>
                </tr>
            </table>
        </form>
    </div>
</div>
