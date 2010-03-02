<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<div style="height:57px;">&nbsp;</div>
<table class="footerpane" id="footerpane" summary="The main footer pane of the page" style="position:fixed; bottom: 0px; z-index:10;">
    <tr>
      <td class="footerrow" width="90%">
            
            <iframe src="http://www.ebi.ac.uk/inc/foot.html" name="foot" frameborder="0" marginwidth="0px" marginheight="0px" scrolling="no"  height="22px" width="100%"  style="z-index:2;"></iframe>
      </td>
      <td style="text-align:right;width:10%;white-space:nowrap;padding-right:10px;" id="divFooter">

      </td>
        <script type="text/javascript">
           document.getElementById("divFooter").innerHTML = "Gene Expression Atlas ${u:getProp('atlas.software.version')} Build <c:out value="${u:getProp('atlas.buildNumber')}"/>";
        </script>
    </tr>
</table>

<script src="http://www.ebi.ac.uk/inc/js/footer.js" type="text/javascript"></script>

${atlas.googleanalytics.script}
</body>
</html>