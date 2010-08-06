    <!--Load the AJAX API-->
 
        // Load the Visualization API and the piechart package.
        google.load('visualization', '1', {'packages':['piechart']});
        google.load("visualization", "1", {packages:["table","linechart"]});

        
        //Set a callback to run when the Google Visualization API is loaded.
        google.setOnLoadCallback(drawChart);
        google.setOnLoadCallback(drawHeatMap);
        // Callback that creates and populates a data table,
        // instantiates the pie chart, passes in the data and
        // draws it.
        function drawChart() {
            // Create our data table.
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Task');
            data.addColumn('number', 'Hours per Day');
            data.addRows([
                ['Work', 11],
                ['Eat', 2],
                ['Commute', 2],
                ['Watch TV', 2],
                ['Sleep', 7]
            ]);

            // Instantiate and draw our chart, passing in some options.
            var chart = new google.visualization.PieChart(document.getElementById('chart_div'));
            chart.draw(data, {width: 400, height: 240, is3D: true, title: 'Chart'});
        }

google.setOnLoadCallback(drawHeatMap);
    function drawHeatMap() {

          // Create our data table.
          var data = BioHeatMapExampleData.example1();
          var table;
          var heatmap;

          var PlotUtils = {
              getDataSelectionAsPlotTable: function(dataTable1,selectionObj) {
                  var dataTable2 = new google.visualization.DataTable();
                  dataTable2.addColumn('string', 'Time');
                  for (var i = 0; i < selectionObj.length; i++) {
                      var selectedRow = selectionObj[i].row;
                      var geneName = dataTable1.getValue(selectedRow, 0);
                      dataTable2.addColumn('number', geneName);
                  }

                  var time=0;
                  // transform row to column
                  for (var col = 1; col < dataTable1.getNumberOfColumns(); col++) {
                      var newRowIndex = dataTable2.addRow();
                      dataTable2.setCell(newRowIndex, 0, time + "", time + "");
                      for (var i = 0; i < selectionObj.length; i++) {
                          var selectedRow = selectionObj[i].row;
                          var value = dataTable1.getValue(selectedRow, col);
                          var formattedValue = dataTable1.getFormattedValue(selectedRow, col);
                          if (!formattedValue) formattedValue = value;
                          dataTable2.setCell(newRowIndex, i+1, value, formattedValue);
                      }
                      time++;
                  }
                  return dataTable2;
              }
          }

         // Add more data rows and cells here
          heatmap = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('heatmap'));
          heatmap.draw(data, {drawBorder: false});

          // draw table
          table = new google.visualization.Table(document.getElementById("tableDiv"));
          table.draw(data,{showRowNumber:false});

          // draw line chart
          var chart = new google.visualization.LineChart(document.getElementById('plotDiv'));
          chart.draw(PlotUtils.getDataSelectionAsPlotTable(data,[{row:0}]), {width: 400, height: 200, legend: 'bottom', title: 'Expression Profile'});

          google.visualization.events.addListener(table, 'select', function() {
              heatmap.setSelection(table.getSelection());
              chart.draw(PlotUtils.getDataSelectionAsPlotTable(data,table.getSelection()), {width: 400, height: 200, legend: 'bottom', title: 'Expression Profile'});
          });
          google.visualization.events.addListener(heatmap, 'select', function() {
              table.setSelection(heatmap.getSelection());
              chart.draw(PlotUtils.getDataSelectionAsPlotTable(data,heatmap.getSelection()), {width: 400, height: 200, legend: 'bottom', title: 'Expression Profile'});
          });


      }

      var BioHeatMapExampleData = {
          example1 : function() {
              var data = new google.visualization.DataTable();

              data.addColumn('string', 'Gene Name');
              data.addColumn('number', 'chip_XXX_XXX_600');
              data.addColumn('number', 'chip2');
              data.addColumn('number', 'chip3');
              data.addColumn('number', 'chip4');
              data.addColumn('number', 'chip5');
              data.addColumn('number', 'chip6');
              data.addRows(4);
              data.setCell(0, 0, 'ATF3');
              data.setCell(0, 1, 0);
              data.setCell(0, 2, 0.5);
              data.setCell(0, 3, 1);
              data.setCell(0, 4, 1.5);
              data.setCell(0, 5, 2);
              data.setCell(0, 6, 2.5);
              data.setCell(1, 0, 'INS');
              data.setCell(1, 1, 3);
              data.setCell(1, 2, 3.5);
              data.setCell(1, 3, 4);
              data.setCell(1, 4, 4.5);
              data.setCell(1, 5, 5);
              data.setCell(1, 6, 5.5);
              data.setCell(2, 0, 'TAP1');
              data.setCell(2, 1, 0);
              data.setCell(2, 2, null);
              data.setCell(2, 3, -1);
              data.setCell(2, 4, -1.5);
              data.setCell(2, 5, -2);
              data.setCell(2, 6, -2.5);
              data.setCell(3, 0, 'IL6');
              data.setCell(3, 1, -3);
              data.setCell(3, 2, -3.5);
              data.setCell(3, 3, -4);
              data.setCell(3, 4, -4.5);
              data.setCell(3, 5, -5);
              data.setCell(3, 6, -5.5);

              heatmap = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('heatmapContainer'));
          heatmap.draw(data, {});
          }
      }


  var data = response.getDataTable();
  var chart = new google.visualization.PieChart(document.getElementById('chart_div'));
  chart.draw(data, {width: 400, height: 240, is3D: true});

}
    