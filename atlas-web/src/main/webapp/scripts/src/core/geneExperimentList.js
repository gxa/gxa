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
     * Pagination for experiment list on gene page.
     *
     * Note: requires jQuery pagination plugin
     * @param opts {
     *      pgeSize          - number of items per page
     *      paginationTarget - an id of DOM element where to render paging control
     *      onPageClick      - page click handler (optional)
     * }
     */
    A.geneExperimentListPagination = function(opts) {
        opts = $.extend({
            pageSize : 10,
            paginationTarget : "pagination",
            onPageClick : null
        }, opts);

        var paginationTarget = A.hsh(opts.paginationTarget),
            pageClickHandler = A.safeFunctionCall(opts.onPageClick);

        return {
            clear: function() {
                A.clearContent(paginationTarget);
            },

            render: function(total, currentPage) {
                this.clear();
                var el = $(paginationTarget);
                if (total > opts.pageSize && el.length) {
                    el.pagination(total, {
                        num_edge_entries: 2,
                        num_display_entries: 5,
                        items_per_page: opts.pageSize,
                        callback: pageClickHandler,
                        current_page: currentPage
                    });
                }
            }
        }
    };

    /**
     * @param opts {
     *       listTemplate - jQuery template name for list (header and pagination of the list)
     *       listTarget   - an id of DOM element where to insert the list content
     *       pageTemplate - jQuery template name for one page of the list (experiment details)
     *       pageTarget   - an id of DOM element (could be inside of list template) where to insert
     *                      a page content
     * }
     */
    A.geneExperimentListView = function(opts) {
        opts = $.extend({
            listTarget : "experimentList",
            pageTarget : "experimentListPage",
            listTemplate : "experimentListTemplate",
            pageTemplate : "experimentListPageTemplate",
            pageState: null,
            pageStatePrefix: ""
        }, opts);

        prepareTemplate(opts.listTemplate, opts.pageTemplate);

        function clear(t) {
            A.clearContent(t);
        }

        function prepareTemplate() {
            for (var i = 0, len = arguments.length; i < len; i++) {
                var template = arguments[i];
                var el = A.$(template);
                if (el) {
                    $.template(template, el);
                } else {
                    A.logError("Template '" + template + "' not found.");
                }
            }
        }

        function renderWithTemplate(template, templTarget, data) {
            var target = A.$(templTarget);
            if (target) {
                $.tmpl(template, data).appendTo(target);
                $("a[atlas-uri]", target).atlasRelativeHref();
            }
        }

        function renderExperimentDetails(data) {
            var exps = data.exps || [];
            var geneId = data.gene.geneId;
            var ef = data.ef || null;
            var efv = data.ef ? (data.efv || null) : null;

            for (var i = 0, len = exps.length; i < len; i++) {
                var exp = exps[i];
                var expEf = ef || exp.highestRankAttribute.ef;
                var el = A.$(exp.accession + "_" + geneId + "_efPagination");
                if (!el) {
                    continue;
                }

                el.efPagination({
                    factors: exp.experimentFactors || [],
                    pageState: opts.pageState,
                    pageStatePrefix: opts.pageStatePrefix + (opts.pageStatePrefix ? "." : "") + "exp_" + exp.accession + "_" + geneId
                }).efChanged(
                    function(expAccession, geneId, ef, efv) {
                        return function(event, selectedEf) {
                            A.barPlot({
                                target: expAccession + "_" + geneId + "_plot",
                                geneId: geneId,
                                expAccession: expAccession,
                                ef: selectedEf,
                                efv: ef && selectedEf === ef ? efv : null
                            }).load();
                        };
                    }(exp.accession, geneId, expEf, efv)
                ).setDefault(expEf);
            }
        }

        return {
            render: function(data) {
                clear(opts.listTarget);
                renderWithTemplate(opts.listTemplate, opts.listTarget, data);
                this.renderPage(data);
            },

            renderPage: function(data) {
                clear(opts.pageTarget);
                renderWithTemplate(opts.pageTemplate, opts.pageTarget, data);
                renderExperimentDetails(data);
            },

            clear: function() {
                clear(opts.listTarget);
            }
        }
    };

    /**
     * Paginated list of experiments filtered by geneId and experimental factor (optional)
     * @param opts {
     *     gene         - gene Id to filter the list by
     *     pageSize     - number of experiments per page
     *     listTarget   - an id of DOM element where the list should be inserted
     *     listTemplate - an id of jQuery template to render the list
     *     pageTarget   - an id of DOM element (could be inside of 'listTemplate') where the current page is rendered
     *     pageTemplate - an id of jQuery template to render the current page
     * }
     */
    A.geneExperimentList = function(opts) {
        opts = $.extend({
            pageSize : 10,
            pageState : null,
            pageStatePrefix : "",
            gene : null
        }, opts);

        var defaults = {pagenum:0},
            currPageIndex = 0,
            cachedParams = {},
            _this = {};

        var listRenderer = A.geneExperimentListView({
            listTarget: opts.listTarget,
            pageTarget: opts.pageTarget,
            listTemplate: opts.listTemplate,
            pageTemplate: opts.pageTemplate,
            pageState: opts.pageState,
            pageStatePrefix: opts.pageStatePrefix + (opts.pageStatePrefix ? ".page" : "")
        });

        var listPagination = A.geneExperimentListPagination({
            pageSize: opts.pageSize,
            paginationTarget: opts.paginationTarget,
            onPageClick: function(pageIndex) {
                loadPage(pageIndex);
                triggerStateChanged({pagenum: pageIndex});
            }
        });

        var listLoader = A.ajaxLoader({
            url: "/geneExpList",
            defaultParams: {gid: opts.gene},
            onSuccess: function(data) {
                if (!data) {
                    listRenderer.clear();
                    listPagination.clear();
                } else if (data.expTotal >= 0) {
                    listRenderer.render(data);
                    listPagination.render(data.expTotal, currPageIndex);
                } else {
                    listRenderer.renderPage(data);
                }
            },
            onFailure: function() {
                listRenderer.clear();
                listPagination.clear();
            }
        });

        if (opts.pageState) {
            opts.pageState.register(_this, opts.pageStatePrefix);
        }

        function pageState() {
            var state = opts.pageState ? opts.pageState.stateFor(opts.pageStatePrefix) : {};
            return  A.extendIfUndefined(state, defaults);
        }
        function triggerStateChanged(state) {
            $(_this).trigger("stateChanged", [A.difference(state || {}, defaults)]);
        }

        function loadAll(params) {
            cachedParams = params || {};
            load($.extend(true, {}, cachedParams, {needPaging: true}), pageState().pagenum);
        }

        function loadPage(pageIndex) {
            load(cachedParams, pageIndex);
        }

        function load(params, pageIndex) {
            currPageIndex = pageIndex;
            var from = pageIndex * opts.pageSize;
            var to = (pageIndex + 1) * opts.pageSize;
            listLoader.load($.extend(true, {
                from: from,
                to: to
            }, params));
        }

        return $.extend(true, _this, {
            /**
             * Loads experiments for the first page + counts the total number of experiments
             * @param params {
             *     ef  - experiment factor
             *     efv - experiment factor value
             *     efo - experiment factor ontology term mapping
             * }
             */
            load: function(params) {
                loadAll(params);
            }
        });
    }

}(atlas || {}, jQuery));
