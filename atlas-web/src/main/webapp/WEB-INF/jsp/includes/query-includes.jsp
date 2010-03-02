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

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.cookie.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tooltip.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.dimensions.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.token.autocomplete.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>

<script type="text/javascript" language="javascript">
    var atlas = {};
    atlas.homeUrl='${pageContext.request.contextPath}/';
</script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css" media="print, projection, screen" />


<!--[if IE]>
<style type="text/css">input { margin-top:-1px;margin-bottom:-1px; padding:1px; }</style>
<![endif]-->
<script type="text/javascript">
var fixpng = function () {};
</script>
<!--[if lt IE 7]>
<script type="text/javascript">
fixpng = function (i) {
    var width = i.width;
    var height = i.height;
    i.style.filter = 'progid:DXImageTransform.Microsoft.AlphaImageLoader(enabled=true, sizingMethod=scale, src="' + i.src + '")';
    i.onload = null;
    i.src = 'images/1.gif';
    i.width = width;
    i.height = height;
}
</script>
<![endif]-->

