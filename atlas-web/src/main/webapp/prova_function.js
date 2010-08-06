

        // Adjust this for your installation
        //
        // var atlasHomeUrl = 'http://localhost:8080/atlas-web/';
        var atlasHomeUrl = ' http://www.ebi.ac.uk/gxa';

        var colors = [ 'red', 'green', 'blue', 'magenta', 'darkgray', 'maroon', 'cyan', 'brown', 'blueviolet', 'darkgreen', 'coral', 'khaki', 'lawngreen' ];

        function processData(data) {
            if(data.error) {
                alert(data.error);
                return;
            }

            if(data.numberOfResults < 1) {
                alert("experiment not found");
                return;
            }

            data = data.results[0];

            document.getElementById("accession").innerText = data.experimentInfo.accession;
            document.getElementById("desc").innerText = data.experimentInfo.description;

            var numOfAssays = data.experimentDesign.assays.length;

            var canvas = document.getElementById("graphs");
            canvas.width = Math.max(Math.min(numOfAssays * 20, 5000), 500);

            canvas.height = 200;

            var context = canvas.getContext("2d");
            context.clearRect(0,0,canvas.width,canvas.height);
            context.fillStyle = "rgba(180, 180, 180, 0.5)";

            context.strokeStyle = 'black';
            context.fillRect(0, canvas.height / 2, canvas.width, canvas.height / 2);

            var legendDiv = document.getElementById("legend");
            legendDiv.innerHTML = '';

            var numbersDiv = document.getElementById("numbers");
            numbersDiv.innerHTML = '';
            numbersDiv.width = canvas.width;
            numbersDiv.height = canvas.height;
            numbersDiv.style.position = 'relative';
            numbersDiv.style.top = (canvas.height / 2) + 'px';

            var assayGraphWidth = canvas.width / numOfAssays;

            for(var j = 0; j < numOfAssays; ++j) {
                var numberDiv = document.createElement("div");
                numberDiv.style.position = 'absolute';
                numberDiv.style.width = assayGraphWidth + 'px';
                numberDiv.innerText = j + 1;
                numberDiv.style.fontSize = '10px';
                numberDiv.style.left = (j * assayGraphWidth) + 'px';
                numberDiv.style.textAlign = 'center';
                numbersDiv.appendChild(numberDiv);

                context.fillStyle = "rgba(200, 200, 200, 0.2)";
                if(j % 2)
                    context.fillRect(assayGraphWidth * j, 0, assayGraphWidth, canvas.height);
            }

            var colorIndex = 0;

            for(var arrayDesignId in data.geneExpressions) {
                var arrayDesign = data.geneExpressions[arrayDesignId];
                for(var geneId in arrayDesign.genes) {
                    var designElementsText = '';
                    for(var designElementId in arrayDesign.genes[geneId]) {
                        var dataPoints = arrayDesign.genes[geneId][designElementId];

                        var min = Math.min.apply( Math, dataPoints );
                        var max = Math.max.apply( Math, dataPoints );

                        if(min < 0 && -min > max)
                            max = -min;

                        context.strokeStyle = colors[colorIndex];
                        context.beginPath();
                        for (j = 0; j < dataPoints.length; j++)
                        {
                            var dataX = assayGraphWidth / 2 + assayGraphWidth * arrayDesign.assays[j];
                            var dataY = canvas.height / 2 * (1 - dataPoints[j] / max * 0.98);

                            if (j == 0)
                                context.moveTo(dataX, dataY);
                            else
                                context.lineTo(dataX, dataY);

                            context.fillStyle = colors[colorIndex];
                            context.fillRect(dataX - 1, dataY - 1, 3, 3);

                        }
                        context.stroke();

                        designElementsText += ' ' + designElementId;
                    }

                    var geneLegendDiv = document.createElement("div");
                    geneLegendDiv.style.color = colors[colorIndex];
                    geneLegendDiv.innerText = geneId + designElementsText;
                    legendDiv.appendChild(geneLegendDiv);

                    if(++colorIndex >= colors.length)
                        colorIndex = 0;
                }
            }

            var assaysDiv = document.getElementById("assays");
            assaysDiv.innerHTML = '';

            var experimentalFactors = data.experimentDesign.experimentalFactors;

            var tr = document.createElement("tr");
            var td = document.createElement("td");
            td.innerText = 'No';
            tr.appendChild(td);
            for(j = 0; j < experimentalFactors.length; ++j) {
                td = document.createElement("td");
                td.innerText = experimentalFactors[j];
                tr.appendChild(td);
            }
            td = document.createElement("td");
            td.innerText = 'array design';
            tr.appendChild(td);
            assaysDiv.appendChild(tr);

            for(j = 0; j < numOfAssays; ++j) {
                tr = document.createElement("tr");
                td = document.createElement("td");
                td.innerText = j + 1;
                tr.appendChild(td);

                var factorValues = data.experimentDesign.assays[j].factorValues;
                for(var k = 0; k < experimentalFactors.length; ++k) {
                    td = document.createElement("td");
                    td.innerText = factorValues[experimentalFactors[k]] == null ? 'n/a' : factorValues[experimentalFactors[k]];
                    tr.appendChild(td);
                }
                td = document.createElement("td");
                td.innerText = data.experimentDesign.assays[j].arrayDesign;
                tr.appendChild(td);
                assaysDiv.appendChild(tr);
            }
        }

        function queryExperiment(experimentId, geneId) {
            var head = document.getElementsByTagName('head');
            var script = document.createElement('script');
            script.type = "text/javascript";
            var src = atlasHomeUrl + "/api?experiment="+encodeURIComponent(experimentId)+"&format=json&callback=processData";
            var geneIds = geneId.split(" ");
            for(var g in geneIds)
                src += "&gene="+encodeURIComponent(geneIds[g]);
            src += '&' + Math.random();
            script.src = src;
            head[0].appendChild(script);
        }

        if(location.href.indexOf('?') >= 0) {
            var experimentId = '';
            var geneIds = '';
            var params = location.href.substr(location.href.indexOf('?') + 1).split('&');
            for(var p in params) {
                var kv = params[p].split('=');
                if(kv[0] == 'experiment')
                    experimentId = decodeURIComponent(kv[1]);
                else if(kv[0] == 'gene')
                    geneIds += decodeURIComponent(kv[1]);
            }
            if(experimentId != '')
                queryExperiment(experimentId, geneIds);
        } else {
            queryExperiment('E-MEXP-748', 'ENSMUSG00000025867 ENSMUSG00000070385');
        }