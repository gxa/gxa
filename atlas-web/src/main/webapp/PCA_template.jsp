<jsp:include page= 'template.jsp' />
<head>
    <style type="text/css"  media="screen">
        div#PCA_plot{ padding:5px;
         height:500px;
         overflow-y:scroll;
        }
    </style>
    <style type="text/css"  media="print">
        div#PCA_plot{
            padding:5px;
        }
    </style>

</head>



<span class='Breadcrumb' style="margin-left:270px;">You are here:
    <SPAN class='Breadcrumbon'>
        <a href="http://localhost:9000/home_hge_atlas.jsp">Home</a> >
        <a href="http://localhost:9000/PCA.jsp">PCA</a>   
    </SPAN>
</span>

<div id="content" style="padding-top:3px;" >
<div class="ui-state-default ui-corner-all" style="padding:10px ; background:white;">
 
    <div>
     <b style="padding:5px">Classifications</b>
     <a href="" title="Print results"><img align="right" src="/silk_print.gif" alt="Print results"hspace="10px;"onClick="window.print()"/></a> 
    <a href="/PCA_Fig/PCA.pdf" title="Download pdf">
         <img align="right" src="/PCA_Fig/put-pdf-document-website-200X200.jpg" width="18" alt="Download Pdf"/></a>
    </div>

    <p class="normal_text" style="padding:5px;">
        <b>Principal component analysis.</b><br/><br/>
        We applied principal component analysis (PCA) to the expression matrix,
        and produced visualizations in which each sample is represented by a point in the plane formed by two principal axes,
        and colors were assigned to each point according to the biological class.
        We found that the first three principal components have biological interpretations;
        we named them the hematopoietic, malignancy and neurological axes. 
        Each dot represents one of the 5,372 samples in a
        multidimensional gene expression space projected on the principal plane formed by the first
        (hematopoietic) and second (malignancy) principal axes. The dots are colored semitransparently
        according to the biological group the sample belongs to.
    </p>
    <br/>
    <!-- PCA and legend-->
    <div id="PCA_plot">
            <p class="normal_text" align="right">
            <a href="/PCA_Fig/SupplementaryFigure_3a.png" target="_blank">
                <img class="ui-state-default ui-corner-all"align="left" border="0" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3a.png" alt="PCA-Blood/non Blood"/></a>
            <br/><br/>The first principal component separates hematopoietic system-derived samples from the rest of the samples, with connective tissues and
            incompletely differentiated cell–based samples forming a relatively compact group on the right.
            The cyan dots among the blood samples on the right side represent samples from bronchoalveolar lavage cells
            (a possible sample contamination with blood) and kidney. The dark green dots at the center include embryonic stem cells.</p>
        <br/><br/>

        <p class="normal_text">
        <a href="/PCA_Fig/SupplementaryFigure_3b.png" target="_blank">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3b.png" alt="PCA-Blood/non Blood"/></a>
        <br/><br/>The second principal axis predominantly arranges cell line samples at the bottom,
            neoplasm samples in the middle and a mixture of nonneoplastic disease and normal samples at the top.
        </p>
        <br/><br/>

        <p class="normal_text">
            <a href="/PCA_Fig/SupplementaryFigure_3e.png" target="_blank">
        <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3e.png" alt="PCA-Blood/non Blood"/></a>
            <br/><br/>Samples colored by 6 clusters (see legend) identified on self-vs-self heat-map of 96 biological groups.
            The samples are visualized on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>

        <p class="normal_text">
            <a href="/PCA_Fig/SupplementaryFigure_3c.png" target="_blank">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3c.png" alt="PCA-Blood/non Blood"/></a>
            <br/><br/>Samples colored by 15 meta-groups (see legend) on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>

        <p class="normal_text">
            <a href="/PCA_Fig/SupplementaryFigure_3d.png" target="_blank">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3d.png" alt="PCA-Blood/non Blood"/></a>
            <br/><br/>Samples colored by 15 groups of tissues of origin (see legend) on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>

    <br/>


</div>
<br/>
</div>
<br/><br/><br/>
</div>



