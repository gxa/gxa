<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>

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
