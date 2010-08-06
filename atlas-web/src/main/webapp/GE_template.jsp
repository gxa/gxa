<jsp:include page= 'template.jsp' />


<head>
<script language="javascript" type="text/javascript">
    $(document).ready(function(){
        var ohlc = [['1', 138.7, 139.68, 135.18, 135.4],
            ['2', 143.46, 144.66, 139.79, 140.02],
            ['3', 128.24, 133.5, 126.26, 129.19],
            ['4', 122.9, 127.95, 122.66, 127.24],
            ['5', 121.73, 127.2, 118.6, 123.9],
            ['6', 136.47, 146.4, 136, 144.67],
            ['7', 124.76, 135.9, 124.55, 135.81],
            ['8', 123.73, 129.31, 121.57, 122.5],
            ['9', 127.37, 130.96, 119.38, 122.42],
            ['10', 140.67, 143.56, 132.88, 142.44],
            ['11', 136.01, 139.5, 134.53, 139.48],
            ['12', 143.82, 144.56, 136.04, 136.97],
            ['13', 120.01, 124.25, 115.76, 123.42],
            ['14', 114.94, 120, 113.28, 119.57],
            ['15', 104.51, 116.13, 102.61, 115.99]
        ];



        ticks=['Blood Neoplasm Cell Line', 'Blood Non Neoplastic Desease', 'Breast Cancer',
            'Germ Cell Neoplasm', 'Leukemia', 'Nervous System Neoplasm', 'Non Breast Carcinoma',
            'Non Leukemic Blood Neoplasm','Non Neoplastic Cell Line','Normal Blood',
            'Normal Solid Tissue','Other Neoplasm','Sarcoma','Solid Tissue Neoplasm Cell Line',
            'Solid Tissue Non Neoplastic Disease'];
        //  ticks=['1', '2', '3', '4'];
        plot2 = $.jqplot('boxplot',[ohlc],{
            title: 'Gene: AAK1',
            axesDefaults:{tickRenderer: $.jqplot.CanvasAxisTickRenderer },
            axes: {
                xaxis: {
                    //renderer:$.jqplot.DateAxisRenderer,
                    renderer:$.jqplot.CategoryAxisRenderer,
                    ticks:ticks,
                    tickOptions:{
                        formatString:'string',
                        angle:-30,
                        fontSize: '8pt'
                    }
                }
                /* yaxis: {
                tickOptions:{formatString:'string'}
                }  */
            },
            /*axes:{xaxis:{ticks:ticks, renderer:$.jqplot.CategoryAxisRenderer}},    */
            series: [{renderer:$.jqplot.OHLCRenderer, rendererOptions:{candleStick:true}}],
            
            highlighter: {
                showMarker:false,
                tooltipAxes: 'xy',
                yvalues: 5,
                formatString:'<table class="jqplot-highlighter"><tr><td>95th percentile:</td><td>%s</td></tr><tr><td>75th percentile:</td><td>%s</td></tr><tr><td>Median:</td><td>%s</td></tr><tr><td>25th percentile:</td><td>%s</td></tr><tr><td>5th percentile:</td><td>%s</td></tr></table>'
            }
        });
    });
</script>


</head>

 <span class='Breadcrumb' style="margin-left:270px;">You are here:
        <SPAN class='Breadcrumbon'>
            <a href="http://localhost:9000/home_hge_atlas.jsp">Home</a> >
            <a href="http://localhost:9000/PCA.jsp">GE</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
        </SPAN>
    </span>

<div id="content" style="padding-top:3px;" >
    <div class="ui-state-default ui-corner-all" style="padding:10px ;background:white;">
        <b>Gene Expression across condition</b>
        <a href="" title="Print results"><img align="right" src="/silk_print.gif" alt="Print results"hspace="10px;"onClick="window.print()"/></a> 
        
        <br/><br/>


        <div id="boxplot" style="margin:20px;margin-left:45px;height:260px; width:640px;"></div>
        <br/>
        <b>Ensembl Gene Annotation</b>
        <br/>
        <p style="font:bold">Probes: 205434_s_at, 205435_s_at, 211186_s_at</p>
        <br/>
        <p style="font:bold">Ensembl ID: ENSG00000115977 </p>
        <br/>
        <p style="font:bold">HGNC symbol: AAK1</p>
        <br/>
        <p style="font:bold">Description: AP2-associated protein kinase 1 (EC 2.7.11.1)(Adaptor-associated kinase 1) [Source:UniProtKB/Swiss-Prot;Acc:Q2M2I8]</p>
        <br/>
    </div>
</div>
<br/>
