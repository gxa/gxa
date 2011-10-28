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

(function(A, $) {
    /**
     *
     * @param opts {
     *     factors - an array of experiment factors
     *     target - an id of DOM element where to insert pagination
     *     onSelect - handler for select event
     * }
     */
    A.efPagination = function(opts) {
        opts = opts || {};
        var targetEl = A.$(opts.target),
            withTarget = function(func) {
                return (!targetEl) ?
                    function() {
                        A.logError("EF Pagination: element '" + opts.target + "' doesn't exist.");
                    } : func;
            },
            factors = opts.factors || [],
            onSelectHandler = opts.onSelect || null;

        var fillIn =
            withTarget(function() {
                var html = [];
                for (var i = 0, len = factors.length; i < len; i++) {
                    var item = factors[i];
                    html.push("<div data-ef=\"" + item + "\">" + item + "</div>");
                }
                if (!targetEl.hasClass("pagination_ef")) {
                    targetEl.addClass("pagination_ef");
                }
                targetEl.html(html.join(""));
            });


        var bindSelectEvent =
            withTarget(function() {
                targetEl.children().each(function() {
                    $(this).click(function() {
                        var elem = $(this);
                        if (elem.hasClass("current")) {
                            return;
                        }
                        var ef = elem.data("ef");
                        mark(ef);
                        notifySelectEvent(ef);
                    });
                });
            });

        var mark =
            withTarget(function(ef) {
            targetEl.children().each(function() {
                var elem = $(this);
                var data = elem.data("ef");
                if (ef === data) {
                    elem.addClass("current");
                } else {
                    elem.removeClass("current");
                }
            });
        });

        var notifySelectEvent =
            withTarget(function(ef) {
                if (onSelectHandler) {
                    try {
                        onSelectHandler(ef);
                    } catch(e) {
                        A.logError(e);
                    }
                }
            });

        fillIn();
        bindSelectEvent();

        return {
            select: function(ef) {
                mark(ef);
                notifySelectEvent(ef);
                return this;
            }
        }
    };
})(atlas || {}, jQuery);