/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

(function(A, $){

    A.barPlotTooltip = function() {
        function showTooltip(x, y, contents) {
            if (contents != 'Mean') {
                $('<div id="tooltip" />').text(contents).css({
                    position: 'absolute',
                    display: 'none',
                    top: (y - 10) + 'px',
                    left: (x + 10) + 'px',
                    border: '1px solid #005555',
                    margin: '0px',
                    backgroundColor: '#EEF5F5',
                    zIndex: 2000
                }).appendTo("body").fadeIn('fast');
            }
        }
        var cachedPoint = null;
        return {
            show: function(dataItem, pos) {
                if (cachedPoint != dataItem.datapoint) {
                    cachedPoint = dataItem.datapoint;
                    $("#tooltip").remove();
                    showTooltip(pos.pageX, pos.pageY, dataItem.series.label);
                }
            },
            hide: function() {
                $("#tooltip").remove();
                cachedPoint = null;
            }
        }
    };

    /**
     *
     * @param opts {
     *     * plotTarget        - an id of DOM element where to put the plot
     *     * legendTarget      - an id of DOM element where to put legend of the plot
     *     * arrayDesignTarget - an id of DOM element where to put array design reference of the experiment
     *     * efv               - an experiment factor value to mark (optional)
     * }
     */
    A.barPlotRenderer = function(opts) {
        opts = opts || {};
        var tooltip = A.barPlotTooltip();
        var plotTarget = A.hsh(opts.plotTarget);
        var efv = opts.efv || null;

        function prepareData(data) {
            if (data) {
                $.extend(true, data.options, {
                    legend: {container: '#' + opts.legendTarget},
                    arrayDesignContainer: '#' + opts.arrayDesignTarget,
                    grid: {
                        backgroundColor: '#fafafa',
                        autoHighlight: false,
                        hoverable: true,
                        borderWidth: 1
                    }
                })
            }
            return data;
        }

        function drawPlot(data) {
            var targetEl = A.$(plotTarget);
            if (!targetEl) {
                A.logError("Plot target element '" + plotTarget + "' not found.");
                return;
            }

            if (!data.series) {
                A.logError({
                    msg: "Can't draw bar plot: the series data is empty (data.series=" + data.series + ")",
                    cause: data.error});
                return;
            }

            var plot = $.plot(targetEl, data.series, data.options);
            markFactorValue(plot, data);
            targetEl.bind("plothover", function (event, pos, item) {
                if (item) {
                    tooltip.show(item, pos);
                } else {
                    tooltip.hide();
                }
            });

            targetEl.bind('mouseleave', function () {
                tooltip.hide();
            });
        }

        function getRangeX(allSeries) {
            allSeries = Array.prototype.concat.call([], allSeries);
            var first = allSeries[0].data;
            var last = allSeries[allSeries.length - 1].data;

            return {
                min: first[0][0],
                max: last[last.length - 1][0],
                length: function() {
                    return Math.abs(this.max - this.min) + 1;
                },
                median: function() {
                    return this.min + Math.round(this.length() * 0.5);
                }
            };
        }

        function markFactorValue(plot, data) {
            if (!efv) {
                return;
            }

            var allSeries = plot.getData();
            var markedSeries;

            for (var i = 0, len = allSeries.length; i < len; ++i) {
                if (allSeries[i].label) {
                    if (allSeries[i].label.toLowerCase() == efv.toLowerCase()) {
                        markedSeries = allSeries[i];
                        break;
                    }
                }
            }

            if (markedSeries == null) {
                return;
            }

            var markedRange = getRangeX(markedSeries);
            var xRange = getRangeX(allSeries);
            var xMin = xRange.min, xMax = xRange.max;
            if (markedRange.length() / xRange.length() < 0.04) {
                var d = markedRange.length() * 25;
                xMin = Math.max(xMin, Math.round(markedRange.median() - d*0.5));
                xMax = Math.min(xMin + d, xMax);
            }

            $.plot($(plotTarget), data.series, $.extend(true, {}, data.options, {
                grid:{
                    markings: [
                        {xaxis: {from: markedRange.min - 0.5, to: markedRange.max + 0.5}, color: '#FFFFCC'}
                    ]},
                xaxis: {min: xMin, max: xMax}
            }));
        }

        return {
            render: function(data) {
                 drawPlot(prepareData(data));
            }
        }
    };

    /**
     *
     * @param opts {
     *     * target       - an id of DOM element where insert the plot
     *     * expAccession - an experiment accession to create plot for
     *     * geneId       - a gene id to create plot for
     *     * ef           - an experiment factor
     *     * efv          - an experiment factor value (optional)
     * }
     */
    A.barPlot = function(opts) {
        opts = opts || {};

        var renderer = A.barPlotRenderer({
            plotTarget: opts.plotTarget || opts.expAccession + "_" + opts.geneId + "_plot",
            legendTarget: opts.legendTarget || opts.expAccession + "_" + opts.geneId + "_legend",
            arrayDesignTarget: opts.arrayDesignTarget || opts.expAccession + "_" + opts.geneId + "_arrayDesign",
            efv: opts.efv
        });

        var loader = A.ajaxLoader({
            url: "/plot",
            onSuccess: function(data) {
                renderer.render(data);
            }
        });
        return {
           load: function() {
               loader.load({
                   plot: "bar",
                   eacc: opts.expAccession,
                   gid: opts.geneId,
                   ef: opts.ef || "",
                   efv: opts.efv || ""
               });
           }
        }
    };
})(atlas || {}, jQuery);

