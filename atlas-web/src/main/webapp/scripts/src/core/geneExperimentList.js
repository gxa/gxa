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
     *      * pgeSize              - number of items per page
     *      * paginationTarget     - an id of DOM element where to render paging control
     *      * allStudiesLinkTarget - an id of DOM element to render 'all studies' link
     *      * onPageClick          - page click handler (optional)
     *      * onAllStudiesClick    - 'all studies' click handler (optional)
     * }
     */
    A.geneExperimentListPagination = function(opts) {
        opts = opts || {};
        var pageSize = opts.pageSize || 10;
        var allStudiesLinkTarget = A.hsh(opts.allStudiesLinkTarget);
        var paginationTarget = A.hsh(opts.paginationTarget);
        var pageClickHandler = A.safeFunctionCall(opts.onPageClick);
        var allStudiesClickHandler = A.safeFunctionCall(opts.onAllStudiesClick);

        return {
            clear: function() {
                A.clearContent(allStudiesLinkTarget, paginationTarget);
            },

            render: function(total, showAllStudiesLink) {
                showAllStudiesLink = showAllStudiesLink || false;
                this.clear();

                var el = $(allStudiesLinkTarget);
                if (showAllStudiesLink && el.length) {
                    var lnk = $("<a>Show all studies</a>").bind("click", allStudiesClickHandler);
                    el.append(lnk);
                }

                el = $(paginationTarget);
                if (total > pageSize && el.length) {
                    el.pagination(total, {
                        num_edge_entries: 2,
                        num_display_entries: 5,
                        items_per_page: pageSize,
                        callback: pageClickHandler
                    });
                }
            }
        }
    };

    /**
     *
     * @param opts {
     *       * listTemplate - jQuery template name for list (header and pagination of the list)
     *       * listTarget   - an id of DOM element where to insert the list content
     *       * pageTemplate - jQuery template name for one page of the list (experiment details)
     *       * pageTarget   - an id of DOM element (could be inside of list template) where to insert
     *                        a page content
     * }
     */
    A.geneExperimentListView = function(opts) {
        opts = opts || {};
        var listTarget = A.hsh(opts.listTarget);
        var pageTarget = A.hsh(opts.pageTarget);
        var listTemplate = opts.listTemplate;
        var pageTemplate = opts.pageTemplate;

        prepareTemplate(listTemplate, pageTemplate);

        function clear(t) {
            A.clearContent(t);
        }

        function prepareTemplate() {
            for (var i = 0, len = arguments.length; i < len; i++) {
                var templName = arguments[i];
                var templ = $(A.hsh(templName));
                if (templ.length) {
                    $.template(templName, templ);
                }
            }
        }

        function renderWithTemplate(templName, templTarget, data) {
            var target = $(templTarget);
            if (target.length) {
                var res = $.tmpl(templName, data);
                if (res.length) {
                    res.appendTo(target);
                    $("a[atlas-uri]", target).atlasRelativeHref();
                }
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
                A.efPagination({
                    factors: exp.experimentFactors || [],
                    target: exp.accession + "_" + geneId + "_efPagination",
                    onSelect: function(expAccession, geneId, ef, efv) {
                        return function(selectedEf) {
                            A.barPlot({
                                target: expAccession + "_" + geneId + "_plot",
                                geneId: geneId,
                                expAccession: expAccession,
                                ef: selectedEf,
                                efv: ef && selectedEf === ef ? efv : null
                            }).load();
                        };
                    }(exp.accession, geneId, expEf, efv)
                }).select(expEf);
            }
        }

        return {
            render: function(data) {
                clear(listTarget);
                renderWithTemplate(listTemplate, listTarget, data);
                this.renderPage(data);
            },

            renderPage: function(data) {
                clear(pageTarget);
                renderWithTemplate(pageTemplate, pageTarget, data);
                renderExperimentDetails(data);
            },

            clear: function() {
                clear(listTarget);
            }
        }
    };

    /**
     * Paginated list of experiments filtered by geneId and experimental factor (optional)
     * @param opts {
     *     * gene         - gene Id to filter the list by
     *     * pageSize     - number of experiments per page
     *     * listTarget   - an id of DOM element where the list should be inserted
     *     * listTemplate - an id of jQuery template to render the list
     *     * pageTarget   - an id of DOM element (could be inside of 'listTemplate') where the current page is rendered
     *     * pageTemplate - an id of jQuery template to render the current page
     * }
     */
    A.geneExperimentList = function(opts) {
        opts = opts || {};
        var pageSize = opts.pageSize || 10;
        var cachedParams = {};

        var listRenderer = A.geneExperimentListView({
            listTarget: opts.listTarget || "experimentList",
            pageTarget: opts.pageTarget || "experimentListPage",
            listTemplate: opts.listTemplate || "experimentListTemplate",
            pageTemplate: opts.pageTemplate || "experimentListPageTemplate"
        });

        var listLoader = A.ajaxLoader({
            url: "/geneExpList",
            defaultParams: opts.gene ? {gid: opts.gene} : null,
            onSuccess: function(data) {
                if (!data) {
                    listRenderer.clear();
                    listPagination.clear();
                } else if (data.expTotal >= 0) {
                    listRenderer.render(data);
                    listPagination.render(data.expTotal, data.ef);
                } else {
                    listRenderer.renderPage(data);
                }
            },
            onFailure: function() {
                listRenderer.clear();
                listPagination.clear();
            }
        });

        function loadInitial(params) {
            cachedParams = params || {};
            listLoader.load($.extend(true, {
                from: 0,
                to: pageSize,
                needPaging: true
            }, params));
        }

        function loadPage(pageIndex) {
            var from = pageIndex * pageSize;
            var to = (pageIndex + 1) * pageSize;
            listLoader.load($.extend(true, {
                from: from,
                to: to
            }, cachedParams));
        }

        var listPagination = A.geneExperimentListPagination({
            pageSize: pageSize,
            paginationTarget: opts.paginationTarget || "pagination",
            allStudiesLinkTarget: opts.allStudiesLinkTarget || "allStudiesLink",
            onPageClick: function(pageIndex) {
                loadPage(pageIndex);
            },
            onAllStudiesClick: function() {
                loadInitial();
            }
        });

        return {
            /**
             * Loads experiments for the first page + counts the total number of experiments
             * @param params {
             *     * ef  - experiment factor
             *     * efv - experiment factor value
             *     * efo - experiment factor ontology term mapping
             * }
             */
            load: function(params) {
                loadInitial(params);
            }
        };
    }

}(atlas || {}, jQuery));
