function drawPlot(jsonObj, plot_id){
	   	if(jsonObj.series){
   			//alert('start of draw plot');
			var plot = $.plot($('#'+plot_id), jsonObj.series,jsonObj.options); 
			var overview;
			var allSeries = plot.getData();
			if(allSeries.length >10){
				var divElt = $('#'+plot_id+'_thm');
				divElt.width(300);divElt.height(50);
				overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']})); 
				//overview.setSelection({ xaxis: { from: 0, to: allSeries.length } });
				
			}
				
			
			
					
			$('#'+plot_id).bind("plotselected", function (event, ranges) {
        		// do the zooming
        		plot = $.plot($('#'+plot_id), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
           					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
                    	 	}));

				// don't fire event on the overview to prevent eternal loop
				//overview.setSelection(ranges, true);
    		});
    
				    $('#'+plot_id+'_thm').bind("plotselected", function (event, ranges) {
				        plot.setSelection(ranges);
				    });
				    
				    
			//Tooltip
			var previousPoint = null;
			$('#'+plot_id).bind("plothover", function (event, pos, item) {

        		if (item) {
          			if (previousPoint != item.datapoint) {
          			
				          previousPoint = item.datapoint;
				                $("#tooltip").remove();
				                 var x = item.datapoint[0].toFixed(2),
				                     y = item.datapoint[1].toFixed(2);
				                     
				                 showTooltip(item.pageX, item.pageY,item.series.label,plot_id);
				                    
				                }
        		}else {
				          $("#tooltip").remove();
				           previousPoint = null;            
				       }
			});
        				
        			/*	
        			//Legend Colors
        			var allSeries = plot.getData();
        			var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			for (var i = 0; i < allSeries.length; ++i){
        				var color = allSeries[i].color;
        				var label = allSeries[i].label.toLowerCase().replace(/ /g,'');
        				
        				$("#"+eid+'_'+gid+'_'+label+'_td').css('background-color',color);
        			}*/	
   					 

				return plot;	
				}// if(o.series)
	}
 

	function bindMarkings(jsonObj,plot, plot_id){
	
					//Markings 
					var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			
        			//unbind from previous events bound to previous plots (different EFs) otherwise they keep adding up.
        			$("#"+eid+'_'+gid+'_tbl').find("input").unbind();
        			
					$("#"+eid+'_'+gid+'_tbl').find("input").click(function(){
						var tokens = this.value.split('_');
						var EFV=tokens[0];
						var EF=tokens[1];
						if(this.checked){
							markClicked(eid,gid,EF,EFV,plot,jsonObj);
						}
						else{
						
						plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1}
                      					}));
                      	overview.clearSelection();
                      	
						}
					});
	
	}

	function markClicked(eid,gid,ef,efv,plot,jsonObj){
								
		var plot_id = eid+'_'+gid+'_plot';
		var allSeries = plot.getData();
		var series;
		var markColor;
      	for (var i = 0; i < allSeries.length; ++i){
      		if(allSeries[i].label){
       		 	if(allSeries[i].label.toLowerCase()==efv.toLowerCase()){
       		 		series = allSeries[i];
       		 		markColor = series.color
       		 		break;
       	 		}
       	 	}
		}
						if(series==null){
							redrawPlotForFactor(eid+'_'+gid+'_'+ef,true,efv);
							return null;
						}
						var data = series.data;
						var xMin= data[0][0]
						var xMax= data[data.length-1][0]
						
						//var seriesoptions = $.extend(true,{},series.lines,{show:true,lineWidth:1, fill:true})
						
						//var testSeries=[];
						
						//testSeries.push(series);
						////allSeries.push(testSeries);
						//var modSeries = $.extend(true,{},series,{lines:{show:true,lineWidth:1, fill:true}})
						//alert(testSeries);
						
						//plot = $.plot($('#'+plot_id), allSeries,o.options);
						var overviewDiv = $('#'+plot_id+'_thm');
						if(allSeries.length>10 && data.length<5){
						
						
							
                      		//showThumbnail(eid+'_'+gid);
                      		
                      		
                      		if(overviewDiv.height()!=0){
                      		overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,jsonObj.options);
				
                      		overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
                      		}
                      		plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#e8cfac' }]},
                          				xaxis: { min: xMin-10, max: xMax+10 }
                      					}));
						}
						else{
						
						plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#e8cfac' }]}
                      					}));
                      					if(overviewDiv.height()!=0){
                      		overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']})); 
				
                      		overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
                      		}
                      	}
	}

	function drawPlots(){

			$(".plot").each(function(){
        	var plot_id = this.id;
        	var tokens = plot_id.split('_');
        	var eid = tokens[0];
        	var gid = tokens[1]; 
        	$.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid,
   			
   			dataType:"json",
   			
   			success: function(o){
   				var plot = drawPlot(o,plot_id);//success
   				bindMarkings(o,plot,plot_id);
   				}
 			});//ajax
        	
        }); //each



	}//drawPlots