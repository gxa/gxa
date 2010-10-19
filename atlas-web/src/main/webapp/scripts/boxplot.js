/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

(function() {

    function generateColors(n) {
        function parse(str) {
            var res;
            if (res = /#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})/.exec(str)) {
                return [parseInt(res[1], 16), parseInt(res[2], 16), parseInt(res[3], 16)];
            }
            return [0, 0, 0];
        }

        function clamp(min, value, max) {
            if (value < min)
                return min;
            else if (value > max)
                return max;
            else
                return value;
        }

        function scale(arr, rf, gf, bf) {
            var x = 3;
            while (-1 < --x) {
                if (arguments[x + 1] != null) {
                    arr[x] *= arguments[x + 1];
                    arr[x] = clamp(0, parseInt(arr[x]), 255);
                }
            }
        }

        var colors = [];
        var def = ["#59BB14", "#39A5FE", "#DC5F13", "#EDC240", "#AFD8F8", "#660000", "#333333"];

        for (var i = 0, variation = -def.length; i < n; i++,variation++) {
            var color = parse(def[i % def.length]);

            if (variation >= 0) {
                var sign = variation % 2 == 1 ? -1 : 1;
                var factor = 1 + sign * Math.ceil(variation / 2) * 0.2;
                scale(color, factor, factor, factor);
            }

            colors.push("rgb(" + color.join(",") + ")");
        }

        return colors;
    }

    var BoxPlot = window.BoxPlot = function(data, domElId, colors) {
        if (! this instanceof arguments.callee) {
            return new BoxPlot(data, domElId);
        }

        for (var i = 0; i < data.series.length; i++) {
            data.series[i].i = i;
            for (var j = 0; j < data.series[i].data.length; j++) {
                data.series[i].data[j].i = j;
            }
        }

        this.data = data;
        this.domId = domElId;
        this.colors = colors;

        return this;
    };

    BoxPlot.prototype = {

        render: function() {

            var data = this.data;
            var colors = this.colors;
            var n = data.series.length > 0 ? data.series[0].data.length : 0;

            if (!colors) {
                colors = generateColors(n);
                this.colors = colors;
            }

            var w = 600,
                    h = 300,
                    kx = w / h,
                    ky = 1,
                    x1 = 0, x2 = data.series.length * n > w / 50 ? Math.floor(w / 50) : data.series.length * n,
                    y1 = pv.min(data.series,
                    function(d) {
                        return pv.min(d.data, function(b) {
                            return b.min;
                        });
                    }) - 1,

                    y2 = pv.max(data.series,
                    function(d) {
                        return pv.max(d.data, function(b) {
                            return b.max;
                        });
                    }) + 1,

                    x = pv.Scale.linear(x1, x2).range(0, w),
                    y = pv.Scale.linear(y1, y2).range(0, h),
                    bw = 0.8;


            var vis = new pv.Panel().canvas(this.domId)
                    .width(w)
                    .height(h);


            var factorValues = vis.add(pv.Panel)
                    .data(data.series)
                    .top(0)
                    .left(
                    function(d) {
                        return x(d.i * n);
                    })
                    .width(function() {
                return (x(1) - x(0)) * n;
            })
                    .lineWidth(1)
                    .strokeStyle("#ccc")
                    .fillStyle(
                    function(d) {
                        return d.i % 2 == 0 ? "#eee" : "white";
                    });

            factorValues.anchor("bottom").add(pv.Label)
                    .textBaseline("bottom")
                    .textStyle("#000")
                    .text(
                    function(d) {
                        return d.name;
                    });

            factorValues.add(pv.Rule)
                    .data(y.ticks())
                    .bottom(y)
                    .strokeStyle(
                    function(d) {
                        return (d == 0 || d == 10) ? "#999" : "#ccc";
                    })
                    .anchor("left").add(pv.Label)
                    .text(y.tickFormat);


            var points = factorValues.add(pv.Panel)
                    .data(
                    function(d) {
                        return d.data;
                    })
                    .left(
                    function(d) {
                        return x(d.i) - x(0);
                    })
                    .width(
                    function() {
                        return (x(1) - x(0)) * bw;
                    });

            points.add(pv.Rule)
                    .left(
                    function() {
                        return (x(1) - x(0)) * bw * 0.5;
                    })
                    .bottom(
                    function(d) {
                        return y(d.min);
                    })
                    .height(
                    function(d) {
                        return y(d.max) - y(d.min);
                    });

            points.add(pv.Bar).bottom(
                    function(d) {
                        return y(d.lq);
                    }).height(
                    function(d) {
                        return y(d.uq) - y(d.lq);
                    })
                    .fillStyle(function(d) {
                return colors[d.i];
            })//("rgba(0,0,255,0.5)")
                    .strokeStyle("black")
                    .lineWidth(1)
                    .antialias(false)
                    .title(function(d) {
                return [d.lq, d.uq, d.min, d.max, d.median].join("\n");
            });

            points.add(pv.Rule).data(
                    function(d) {
                        return [d.min, d.max];
                    })
                    .bottom(y)
                    .left(0)
                    .width(function() {
                return (x(1) - x(0)) * bw;
            });


            points.add(pv.Rule)
                    .bottom(
                    function(d) {
                        return y(d.median);
                    });


            if (data.series.length > 0) {

                var legend = [];
                var items = data.series[0].data;
                for(var d=0; d<items.length; d++) {
                    if(items[d])
                        legend.push(items[d].id.split(":",1));
                }

                vis.add(pv.Dot)
                        .data(legend)
                        .left(10)
                        .bottom(
                        function() {
                            return this.index * 12 + 10;
                        })
                        .strokeStyle(null)
                        .fillStyle(
                        function() {
                            return colors[this.index];
                        })
                        .anchor("right").add(pv.Label);
            }

            /* Use an invisible panel to capture pan & zoom events. */
            vis.add(pv.Panel)
                    .events("all")
                    .event("mousedown", pv.Behavior.pan())
                    .event("mousewheel", pv.Behavior.zoom())
                    .event("pan", transform)
                    .event("zoom", transform);

            /** Update the x- and y-scale domains per the new transform. */
            function transform() {
                var t = this.transform().invert();
                x.domain(t.x / w * 2 * kx + x1, (t.k + t.x / w) * 2 * kx + x2 - 2 * kx);
                y.domain(y1, y2);
                vis.render();
            }


            vis.render();
        }
    }
})();