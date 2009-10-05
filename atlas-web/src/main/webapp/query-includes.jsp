<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery.cookie.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery-impromptu.1.5.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery.tooltip.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery.dimensions.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/jquery.token.autocomplete.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/scripts/feedback.js"></script>

<link rel="stylesheet" href="<%= request.getContextPath()%>/atlas.css" type="text/css" />
<link rel="stylesheet" href="<%= request.getContextPath()%>/blue/style.css" type="text/css" media="print, projection, screen" />

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

