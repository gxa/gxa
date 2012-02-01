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

(function($) {
    var pluginName = "efPagination";

    /**
     * @param opts {
     *     factors - an array of experiment factors (e.g., {name: "factor_name", displayName: "Factor name"})
     *     pageState - page state object to register for stateChange event (optional)
     *     pageStatePrefix - a hierarchical prefix for page state; required if pageState is given
     * }
     */
    function EfPagination(elem, opts) {
        opts = $.extend({
            factors: [],
            pageState: null,
            pageStatePrefix: ""
        }, opts);

        var _this = this,
            defaultEf = null,
            currEf = null;

        $.extend(true, _this, {
            select: function(ef) {
                if (ef && ef != currEf) {
                    mark(ef);
                }
                return _this;
            },
            setDefault: function(ef) {
                defaultEf = ef;
                mark(currEf || defaultEf);
                return _this;
            },
            efChanged: function(handler) {
                $(_this).bind("efChanged", handler);
                return _this;
            }
        });

        init();

        function init() {
            draw();
            bindEvents();

            if (opts.pageState) {
                opts.pageState.register(_this, opts.pageStatePrefix);
                $(elem).bind("destroyed", function() {
                    opts.pageState.unregister(_this, opts.pageStatePrefix);
                });

                currEf = opts.pageState.stateFor(opts.pageStatePrefix).ef;
            }
        }

        function draw() {
            var html = [], factors = opts.factors;
            for (var i = 0, len = factors.length; i < len; i++) {
                var factor = factors[i];
                html.push("<div data-ef=\"" + factor.name + "\">" + factor.displayName + "</div>");
            }
            var el = $(elem);
            if (!el.hasClass("pagination_ef")) {
                el.addClass("pagination_ef");
            }
            el.html(html.join(""));
        }

        function bindEvents() {
            $(elem).children().each(function() {
                $(this).click(function() {
                    var el = $(this);
                    if (el.hasClass("current")) {
                        return;
                    }
                    var ef = el.data("ef");
                    mark(ef);
                    notifyEfClicked(ef);
                });
            });
        }

        function mark(ef) {
            $(elem).children().each(function() {
                var elem = $(this);
                var data = elem.data("ef");
                if (ef === data) {
                    elem.addClass("current");
                } else {
                    elem.removeClass("current");
                }
            });
            currEf = ef;
            notifyEfChanged(ef);
        }

        function notifyEfClicked(ef) {
            $(_this).trigger("efClicked", [ef]);
            $(_this).trigger("stateChanged", [{ef: ef === defaultEf ? null : ef}]);
        }

        function notifyEfChanged(ef) {
            $(_this).trigger("efChanged", [ef]);
        }
    }

    $.extend($.fn, {
        efPagination: function(opts) {
            if (this.length > 1) {
                this.each(function() {
                    $(this).efPagination(opts);
                });
                return this;
            }

            var p = $(this).data(pluginName);
            if (!p) {
                p = new EfPagination(this, opts);
                $(this).data(pluginName, p);
            }
            return p;
        }
    });
})(jQuery);