<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">

<%@ page import="java.util.*" %>

<link href="/css-common.css" media="screen, print" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen, print" rel="stylesheet" type="text/css">
<link href="/style.css" media="screen, print" rel="stylesheet" type="text/css">
<link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" />
<link type="text/css" href="/hge_includes/jquery-ui-1.8.2.custom/css/overcast/jquery-ui-1.8.2.custom.css" rel="Stylesheet" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.css" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/examples/examples.css" />

<%--><link rel="stylesheet" href="/themeroller/css/parseTheme.css.php?ctl=themeroller" type="text/css" media="all" /> <--%>

<head>

<!--script type="text/javascript" src="http://www.ebi.ac.uk/microarray-as/ae/assets/scripts/ae_browse-1.4.js"></script-->
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-ui-1.8.2.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.dialog.js"></script>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page= '/WEB-INF/jsp/includes/start_head.jsp' />
HGE
<jsp:include page= '/WEB-INF/jsp/includes/end_head.jsp' />

<!-- add header scripts here -->

<body>
<jsp:include page= '/WEB-INF/jsp/includes/start_body_no_menus_max.jsp' />
<jsp:include page= '/WEB-INF/jsp/includes/end_menu.jsp' />
    

<br/>
   <div style="padding:20px ; margin-left:20px; margin-right:10px;background:white;">
  <label>
           <input type="button" style="float:right;" value="Print this page" onClick="window.print()">
       </label>
   <div>
    <b style="padding:5px">Classifications</b>
    <a href="" title="Print results"><img align="right" src="/silk_print.gif" alt="Print results"/></a>
    <a href="/PCA_Fig/pca_2.pdf" title="Download pdf">
        <img align="right" src="/PCA_Fig/put-pdf-document-website-200X200.jpg" width="20" alt="Download Pdf"/></a>
   </div>

   <p class="normal_text" style="padding:5px;">
       <b>Principal component analysis.</b><br/><br/>
       Each dot represents one of the 5,372 samples in a
       multidimensional gene expression space projected on the principal plane formed by the first
       (hematopoietic) and second (malignancy) principal axes. The dots are colored semitransparently
       according to the biological group the sample belongs to.
   </p>
   <br/>
   <!-- PCA and legend-->
   <div style="padding:5px;">
           <p class="normal_text" align="right">
               <img class="ui-state-default ui-corner-all"align="left" border="0" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3a.png" alt="PCA-Blood/non Blood"/>
           <br/><br/>The first principal component separates hematopoietic system–derived samples from the rest of the samples, with connective tissues and
           incompletely differentiated cell–based samples forming a relatively compact group on the right.
           The cyan dots among the blood samples on the right side represent samples from bronchoalveolar lavage cells
           (a possible sample contamination with blood) and kidney. The dark green dots at the center include embryonic stem cells.</p>
       <br/><br/>

       <p class="normal_text">
           <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3b.png" alt="PCA-Blood/non Blood"/>
       <br/><br/>The second principal axis predominantly arranges cell line samples at the bottom,
           neoplasm samples in the middle and a mixture of nonneoplastic disease and normal samples at the top.
       </p>
       <br/><br/>

       <p class="normal_text">
       <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3e.png" alt="PCA-Blood/non Blood"/>
           <br/><br/>Samples colored by 6 clusters (see legend) identified on self-vs-self heat-map of 96 biological groups.
           The samples are visualized on the 1st and 2nd principal component plane.
       </p>
       <br/><br/>

       <p class="normal_text">
           <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3c.png" alt="PCA-Blood/non Blood"/>
           <br/><br/>Samples colored by 15 meta-groups (see legend) on the 1st and 2nd principal component plane.
       </p>
       <br/><br/>

       <p class="normal_text">
           <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3d.png" alt="PCA-Blood/non Blood"/>
           <br/><br/>Samples colored by 15 groups of tissues of origin (see legend) on the 1st and 2nd principal component plane.
       </p>
       <br/><br/>

   <br/>


   </div>
   <br/><br/>
   </div>
   <br/> <br/> <br/>

   
</body>

