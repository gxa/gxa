<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>

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

<c:set value="http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.servletContext.contextPath}/api?${param.apiUrl}" var="apiUrl" />
<a style="font-size: 12px;font-weight: normal;" title="Get API URL for this result set in XML format" href="apilinks.jsp#" onclick="atlas.showApiLinks('${u:escapeJS(apiUrl)}', ${empty param.callback ? 'null' : param.callback});return false;">REST API</a>
<div id="apilinks"><div class="abs">
    <div class="closebox">close</div>
    <p>Please copy/paste those URLs into your code to get same results in machine-readable formats:</p>
    <p><form action="#" onsubmit="return false;">
        <table>
        <tr><td>for <a href="http://www.json.org">JSON</a>&nbsp;</td><td style="width:90%"><input class="jsonapilink value" type="text" value="${apiUrl}&format=json" style="width:100%" onclick="atlas.copyText(this);"></td></tr>
        <tr><td>for <a href="http://www.w3.org/XML/">XML</a>&nbsp;</td><td style="width:90%"><input class="xmlapilink value" type="text" value="${apiUrl}&format=xml" style="width:100%" onclick="atlas.copyText(this);"></td></tr>
        </table>
    </form></p>
    <p>Check our <a href="javascript:alert('Sorry, no tutorials available, just read the output');">tutorials</a> on how to handle this output from your code.</p>
</div></div>
