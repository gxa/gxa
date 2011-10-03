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

var atlas = atlas || {};

(function($) {

    var commonTokenOptions = {
        hideText: "hide suggestions",
        noResultsText: "no results found",

        getItemList:  function (json, query) {
            return json.completions[query];
        },

        formatToken: function(row) {
            return row.value.length > 20 ? row.value.substr(0, 20) + '...' : row.value;
        },

        formatTokenTooltip: function(row) {
            return row.value;
        }
    };

    function ellipsis(val, n) {
        return val.length > n ? val.substr(0, n) + '...' : val
    }

    atlas.tokenizeConditionInput = function (fvalfield, factor, defaultvalue) {
        var actype = 'efv';
        if (factor == '') {
            actype = 'efoefv';
        } else if (factor == 'efo') {
            actype = 'efo';
        }

        fvalfield.tokenInput(atlas.pathFor("fval"), $.extend(true, {}, commonTokenOptions,
        {
            extraParams: {
                factor: factor,
                type: actype,
                limit: 15
            },

            treeMode: true,

            defaultValue: defaultvalue,

            formatListItem: function(item, q, i) {
                var text = [$.highlightTerm(ellipsis(item.value, 50), q, 'b')];
                var title = item.value;
                var id = item.property === "efo" ? item.id : (item.factorName || "");

                if (item.alternativeTerms && item.alternativeTerms.length > 0) {
                    for (var i = 0; i < item.alternativeTerms.length; i++) {
                        var at = item.alternativeTerms[i];
                        var hat = $.highlightTerm(at, q, 'b');
                        if (hat != at) {
                            text.push(" [" + $.highlightTerm(ellipsis(at, 50), q, 'b') + "]");
                            title += " [" + at + "]";
                            break;
                        }
                    }
                }

                title += " [" + id + "]";

                var span = $("<span>");
                span.attr("title", title);
                span.html(text.join(" "));
                span.append($("<em>").text(" (" + item.count + " genes) " + id));
                return $("<div>").append(span).html();
            },

            formatToken: function(row) {
                if (row.property == 'efo' && row.id == 'Other')
                    return 'more...';
                return row.value.length > 20 ? row.value.substr(0, 20) + '...' : row.value;
            },

            formatId: function(res) {
                if (res.property == 'efo')
                    return res.id;
                else
                    return res.value;
            },

            browser: factor == "" || factor == "efo" ? function (onResult, lastId, values) {
                return $('<div/>').addClass('tree').efoTree({ slideSpeed: 200, downTo: lastId, highlight: values }, function(dc) {
                    onResult({ id: dc.id,  count: dc.count, property: 'efo', value: dc.term, depth: dc.depth });
                });
            } : null
        }));
    };

    atlas.tokenizeGeneInput = function (fvalfield, property, defaultvalue) {
        fvalfield.tokenInput(atlas.pathFor("fval"), $.extend(true, {}, commonTokenOptions,
        {
            extraParams: {
                factor: property,
                type: "gene",
                limit: 15
            },

            defaultValue: defaultvalue,

            formatListItem: function(row, q, i) {
                var text = $.highlightTerm(ellipsis(row.value, 50), q, 'b');
                var title = row.value;
                var prop = property ? "" : (row.property || "").toLowerCase();
                var ext = "";
                var count = row.count || 0;

                if (prop == "gene") {
                    var otherNames = row.otherNames || [];
                    ext = otherNames.length > 0 ? "(" + otherNames.join(",") + ") " : "";
                    ext += row.species;
                    title += " " + ext;
                }

                var span = $("<span>");
                span.attr("title", title);
                if (prop) {
                    span.append($("<em>").html(prop + ":"));
                }
                span.append("&nbsp;" + text);
                if (ext) {
                    span.append($("<em>").html("&nbsp;" + ext));
                }
                if (count > 0) {
                    span.append($("<em>").html("&nbsp;(" + count + ")"));
                }
                return $("<div>").append(span).html();
            },

            formatToken: function(row) {
                var text = row.property == "gene" && row.value == row.id && row.otherNames.length > 0 ? row.otherNames[0] : row.value;
                return text.length > 20 ? text.substr(0, 20) + '...' : text;
            },

            formatTokenTooltip: function(row) {
                return row.property == "gene" && row.value == row.id && row.otherNames.length > 0 ? row.otherNames[0] : row.value;
            },

            formatId: function(res) {
                return res.property == "gene" ? res.id : res.value;
            }

        }));
    };

    /**
     *  Simple form ////////////////////////////////////////////////////////////////////////////////////////////////////
     */

    var simpleForm = (function() {

        function geneConditionsField(form) {
            return $('input[name=genes]', form);
        }

        function expConditionField(form) {
            return $('input[name=fval_0]', form);
        }

        function expressionField(form) {
            return $("select[name=fexp_0]", form);
        }

        function speciesField(form) {
            return $("select[name=specie_0]", form);
        }

        function initGeneConditions(form, query) {
            var conditionsField = geneConditionsField(form);

            var geneConditions = query.geneConditions || [];
            if (geneConditions.length > 0) {
                conditionsField.val(geneConditions[0].jointFactorValues);
            }

            conditionsField.get(0).geneProperties = [];

            atlas.tokenizeGeneInput(conditionsField, '', '(all genes)');

            conditionsField.bind("addResult", function(event, geneToken) {
                var props = event.target.geneProperties;

                if (!props) {
                    return;
                }

                props.push({
                    value: geneToken.id,
                    property: geneToken.property
                });
            });

            conditionsField.bind("removeResult", function(event, geneToken) {
                var props = event.target.geneProperties;

                if (!props) {
                    return;
                }

                for (var i = 0; i < props.length; i++) {
                    var gene = props[i];
                    if (gene.value == geneToken.id) {
                        props.splice(i, 1);
                        break;
                    }
                }
            });
        }

        function initExpConditions(form, query) {
            var conditionsField = expConditionField(form);

            val = [];
            var conditions = query.conditions || [];
            if (conditions.length > 0) {
                expressionField(form).val(conditions[0].expression);
                // Enable autocomplete for @EFO_... values
                if (conditions[0].jointFactorValues.charAt(0) === "@") {
                    conditions[0].jointFactorValues = conditions[0].jointFactorValues.substr(1);
                }
                conditionsField.val(conditions[0].jointFactorValues);
            }

            atlas.tokenizeConditionInput(conditionsField, '', '(all conditions)');
        }

        function initSpecies(form, query) {
            var species = query.species || [];
            if (species.length > 0) {
                speciesField(form).val(species[0]);
            }
        }

        function asQuery(form) {
            var query = { geneConditions:[], conditions:[], species:[] };

            var field = geneConditionsField(form);
            var value0 = field.val();
            var prop0 = ""; // prop0 <= any

            var props = field.get(0).geneProperties;
            if (props) {    // find best fit
                for (var i = 0; i < props.length; i++) {
                    if (prop0.length == 0) {
                        prop0 = props[i].property;
                    } else if (prop0 != props[i].property) {
                        prop0 = "";
                        break;
                    }
                }
            }

            query.geneConditions.push({value:value0, property:prop0});

            query.conditions.push({
                expression: expressionField(form).val(),
                value: expConditionField(form).val()
            });

            query.species.push(speciesField(form).val());

            query.view = $('input[name=view]:checked', form).val();

            return query;
        }

        return {
            init: function(query) {
                query = query || {};

                var form = $('#simpleform');

                initGeneConditions(form, query);
                initExpConditions(form, query);
                initSpecies(form, query);

                form.bind('submit', function () {
                    try {
                        atlas.startSearching(form);
                        atlas.submitForm(asQuery(form), form);
                    } catch(e) {
                        if (window.console) {
                            window.console.log(e);
                        }
                    }
                    return false;
                });
            }
        }
    })();

    /**
     * Advanced form ///////////////////////////////////////////////////////////////////////////////////////////////////
     */

    var advancedForm = (function() {

        var sequence = (function() {
            var seq = 0;
            return {
                nextVal: function() {
                    return ++seq;
                },

                currVal: function() {
                    return seq;
                }
            }
        })();

        function createRemoveButton(callback) {
            return $('<td class="rm" />').append($('<input type="button" value=" - " />').click(callback));
        }

        function createSelect(name, options, optional, value) {
            var e = document.createElement("select");
            if (name) {
                e.name = name;
                e.id = name;
            }
            if (optional) e.options[0] = new Option("(any)", "");
            if (options) {
                var selected = 0;
                for (var i = 0; i < options.length; i++) {
                    var option;
                    if (typeof(options[i]) == "object") {
                        option = new Option(options[i][1], options[i][0]);
                    } else {
                        option = new Option(options[i], options[i]);
                    }
                    if (value == option.value) {
                        selected = e.options.length;
                    }
                    e.options[e.options.length] = option;
                }
                e.selectedIndex = selected;
            }
            return e;
        }

        function queryConditionsChanged() {
            var isQueryEmpty = $("#conditions tr").length <= 1;

            var emptyQueryRow = $("#helprow");
            var submitButton = $('#structsubmit');
            var newQueryButton = $('#structclear');

            if (isQueryEmpty) {
                emptyQueryRow.show();
                submitButton.attr('disabled', 'disabled');
                newQueryButton.attr('disabled', 'disabled');
            } else {
                emptyQueryRow.hide();
                submitButton.removeAttr('disabled');
                newQueryButton.removeAttr('disabled');
            }
        }

        function addSpecie(specie) {
            sequence.nextVal();

            var sel = $('#species').get(0);
            var found = false;
            for (var i = 0; i < sel.options.length; ++i)
                if (sel.options[i].value.toLowerCase() == specie.toLowerCase()) {
                    specie = sel.options[i].value;
                    sel.options[i] = null;
                    found = true;
                    break;
                }

            if (!found)
                return;


            var or = '';
            if ($('tr.speccond:last', tbody).length > 0) {
                or = 'or ';
            }
            var value = $('<td class="specval value">' + or + specie + '<input class="specval" type="hidden" name="specie_' + sequence.currVal() + '" value="' + specie + '"></td>');
            var remove = createRemoveButton(
                                           function () {
                                               var tr = $(this).parents('tr:first');
                                               var rmvalue = $('input.specval', tr).val();
                                               var tbody = tr.parents('tbody:first');
                                               var specs = $('tr.speccond', tbody);
                                               if (specs.length > 1 && specs.index(tr.get(0)) == 0) {
                                                   var nextr = tr.next('tr');
                                                   $('td.specval', tr).replaceWith($('td.specval', nextr));
                                                   nextr.remove();
                                               } else {
                                                   tr.remove();
                                                   queryConditionsChanged();
                                               }
                                               var i;
                                               var sel = $('#species').get(0);
                                               for (i = 0; i < sel.options.length; ++i)
                                                   if (sel.options[i].value >= rmvalue)
                                                       break;
                                               sel.options.add(new Option(rmvalue, rmvalue), i);
                                           });

            var tbody = $('#conditions');
            var tr = $('tr.speccond:last', tbody);
            if (tr.length > 0) {
                tr.after($('<tr class="speccond"><td class="left"></td></tr>').append(value).append(remove));
            } else {
                tbody.prepend($('<tr class="speccond"><td class="left">organism</td></tr>')
                        .append(value).append(remove));
            }
            queryConditionsChanged();
        }

        var minexpoptions = [
            [1, '1 exp.' ],
            [2, '2 exps.' ],
            [5, '5 exps.' ],
            [10, '10 exps.' ],
            [20, '20 exps.' ]
        ];

        function addExpFactor(factor, expression, minexps, values) {
            var selopt = $('#factors').get(0).options;
            var factorLabel = factor;
            for (var i = 0; i < selopt.length; ++i) {
                if (selopt[i].value == factor) {
                    factorLabel = selopt[i].text.toLowerCase();
                }
            }

            if (factor == "") {
                factorLabel = "any condition";
            }

            sequence.nextVal();

            // Enable autocomplete for @EFO_... values
            if (values && values.charAt(0) === "@") {
                values = values.substr(1);
            }

            var input = $('<input type="text" class="value"/>')
                    .attr('name', "fval_" + sequence.currVal())
                    .val(values != null ? values : "");

            var tr = $('<tr class="efvcond" />')
                    .append($('<td class="left" />')
                    .append(' in at least ')
                    .append(createSelect("fmex_" + sequence.currVal(), minexpoptions, false, minexps != null ? minexps : 1))
                    .append(' is ')
                    .append(createSelect("fexp_" + sequence.currVal(),
                    (factor == "" || factor == "efo") ? options['expressions'] : options['onlyexpressions'], false, expression))
                    .append(' in ')
                    .append('&nbsp;&nbsp;&nbsp;')
                    .append(factorLabel)
                    .append($('<input type="hidden" name="fact_' + sequence.currVal() + '" value="' + factor + '">')))
                    .append($('<td class="value" />').append(input))
                    .append(createRemoveButton(
                                              function () {
                                                  var tr = $(this).parents('tr:first');
                                                  var tbody = tr.parents('tbody:first').get(0);
                                                  tr.remove();
                                                  queryConditionsChanged();
                                              }));

            atlas.tokenizeConditionInput(input, factor, '(all ' + (factor != '' ? factorLabel : 'condition') + 's)');

            var t = $('tr.efvcond:last,tr.speccond:last', $('#conditions'));
            if (t.length) {
                t.eq(0).after(tr);
            } else {
                $('#conditions').append(tr);
            }
            queryConditionsChanged();
        }

        function getPropLabel(property) {
            var selopt = $('#geneprops').get(0).options;
            var propertyLabel = property;
            for (var i = 0; i < selopt.length; ++i)
                if (selopt[i].value == property) {
                    propertyLabel = selopt[i].text.toLowerCase();
                }

            if (property == "")
                propertyLabel = "any property";

            return propertyLabel;
        }

        function addGeneQuery(property, values, not) {
            sequence.nextVal();

            var label = getPropLabel(property);
            var input = $('<input type="text" class="value"/>')
                    .attr('name', "gval_" + sequence.currVal())
                    .val(values != null ? values : "");

            var tr = $('<tr class="genecond" />')
                    .append($('<td class="left" />')
                    .append($('<select  name="' + ('gnot_' + sequence.currVal()) + '"><option ' + (not ? '' : 'selected="selected"') + 'value="">has</option><option'
                    + (not ? ' selected="selected"' : '') + ' value="1">hasn&#39;t</option></select>'))
                    .append('&nbsp;&nbsp;&nbsp;')
                    .append($('<span class="gprop" />').text(label))
                    .append($('<input type="hidden" name="gprop_' + sequence.currVal() + '" value="' + property + '">')))
                    .append($('<td class="value" />').append(input))
                    .append(createRemoveButton(
                                              function () {
                                                  var tr = $(this).parents('tr:first');
                                                  tr.remove();
                                                  queryConditionsChanged();
                                              }));

            $('#conditions').append(tr);

            atlas.tokenizeGeneInput(input, property, '(all ' + (property != "" ? label.toLowerCase() : 'gene') + 's)');

            queryConditionsChanged();
        }

        function initSpecies(query) {
            if (query.species && query.species.length) {
                for (var i = 0; i < query.species.length; ++i) {
                    addSpecie(query.species[i]);
                }
            }
        }

        function initExpConditions(query) {
            if (query.conditions && query.conditions.length) {
                for (var i = 0; i < query.conditions.length; ++i) {
                    addExpFactor(query.conditions[i].factor,
                            query.conditions[i].expression,
                            query.conditions[i].minExperiments,
                            query.conditions[i].jointFactorValues);
                }
            }
        }

        function initGeneConditions(query) {
            if (query && query.geneConditions.length) {
                for (var i = 0; i < query.geneConditions.length; ++i) {
                    addGeneQuery(query.geneConditions[i].factor,
                            query.geneConditions[i].jointFactorValues,
                            query.geneConditions[i].negated);
                }
            }
        }

        function asQuery(form) {
            var query = { conditions:[], geneConditions:[], species:[] };

            $('input.specval').each(function() {
                query.species.push(this.value);
            });

            function contains(string, substr) {
                return string.indexOf(substr) >= 0;
            }

            $('#conditions tr.efvcond').each(function() {
                var condition = {};
                $('input,select', this).each(function() {
                    if (contains(this.name, "fact_")) {
                        condition.factor = this.value;
                    } else if (contains(this.name, "fval_")) {
                        condition.value = this.value;
                    } else if (contains(this.name, "fmex_")) {
                        condition.minExperiments = this.value;
                    } else if (contains(this.name, "fexp_")) {
                        condition.expression = this.value;
                    }
                });
                query.conditions.push(condition);
            });

            $('#conditions tr.genecond').each(function() {
                var condition = {};
                $('input,select', this).each(function() {
                     if (contains(this.name, "gprop_")) {
                         condition.property = this.value;
                     } else if (contains(this.name, "gnot_")) {
                         condition.not = this.value;
                     } else if (contains(this.name, "gval_")) {
                         condition.value = this.value;
                     }
                });
                query.geneConditions.push(condition);
            });

            query.view = $('input[name=view]:checked', form).val();
            return query;
        }

        return {
            init: function(query) {
                query = query || {};

                $('#geneprops').change(
                                      function () {
                                          if (this.selectedIndex >= 1) {
                                              var property = this.options[this.selectedIndex].value;
                                              addGeneQuery(property);
                                          }
                                          this.selectedIndex = 0;
                                      });

                $('#species').change(
                                    function () {
                                        if (this.selectedIndex >= 1) {
                                            var specie = this.options[this.selectedIndex].value;
                                            addSpecie(specie);
                                        }
                                        this.selectedIndex = 0;
                                    });

                $('#factors').change(
                                    function () {
                                        if (this.selectedIndex >= 1) {
                                            var factor = this.options[this.selectedIndex].value;
                                            addExpFactor(factor);
                                        }
                                        this.selectedIndex = 0;
                                    });

                initSpecies(query);
                initExpConditions(query);
                initGeneConditions(query);

                var form = $('#structform');
                form.bind('submit', function () {
                    try {
                        atlas.startSearching(form);
                        atlas.submitForm(asQuery(form), form);
                    } catch(e) {
                        if (window.console) {
                            window.console.log(e);
                        }
                    }
                    return false;
                });
            },

            clear: function() {
                $('#conditions td.rm input').click();
                $('#conditions td.rm input').click();
                $('#gene0,#fval0,#grop0').val('');
                $('#species0,#expr0').each(function () {
                    this.selectedIndex = 0;
                });
            }
       }
    })();

    /**
     *  SearchForm Help  ///////////////////////////////////////////////////////////////////////////////////////////////
     */
    var searchFormHelp = (function() {
        function toggleAtlasHelp() {
            if ($("div.atlasHelp").is(":hidden")) {
                showAtlasHelp();
            } else {
                hideAtlasHelp();
            }
            return false;
        }

        function showAtlasHelp() {
            if ($("div.atlasHelp").is(":hidden")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("hide help");
            }
            $.cookie('atlas_help_state', 'shown');
        }

        function hideAtlasHelp() {
            if ($("div.atlasHelp").is(":visible")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("show help");
            }
            $.cookie('atlas_help_state', 'hidden');
        }

        return {
            init: function() {
                var helpToggle = $("#atlasHelpToggle");
                if (helpToggle.length > 0) {
                    helpToggle.click(toggleAtlasHelp);

                    if (($.cookie('atlas_help_state') == "shown") && ($("div.atlasHelp").is(":hidden"))) {
                        showAtlasHelp();
                    } else if (($.cookie('atlas_help_state') == "hidden") && ($("div.atlasHelp").is(":visible"))) {
                        hideAtlasHelp();
                    }
                }
            }
        }
    })();

    /**
     * /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
     */

    atlas.initSearchForm = function(query) {
        atlas.latestSearchQuery = query;

        // disable Firefox's bfcache
        $(window).unload(function() {
            return true;
        });

        simpleForm.init(query);
        advancedForm.init(query);
        searchFormHelp.init();
    };

    atlas.startSearching = function(form) {
        var v = $(form).find('input[type=submit]');
        v.val('Searching...');
        $(form).find('input:hidden').trigger('preSubmit');
    };

    atlas.submitForm = function(query, form) {
        form = form || $('#simpleform:visible, #structform:visible');

        if ($('#temporaryform')) {
            $("#temporaryform").remove();
        }

        var tmpl = '<input type="hidden" name="${name}" value="${value}"/>';

        var i;
        var formEl = $('<form id="temporaryform"></form>');
        for (i = 0; i < query.species.length; i++) {
            formEl.append($.tmpl(tmpl, {name: "specie_" + i, value: query.species[i]}));
        }

        var j = 0, cond, val;
        for (i = 0; i < query.geneConditions.length; i++,j++) {
            cond = query.geneConditions[i];
            val = cond.value || "";
            val += cond.jointFactorValues || "";

            formEl.append($.tmpl(tmpl, {name: "gprop_" + j, value: cond.property || ""}));
            formEl.append($.tmpl(tmpl, {name: "gnot_" + j, value: cond.not || ""}));
            formEl.append($.tmpl(tmpl, {name: "gval_" + j, value: val}));
        }

        for (i = 0; i < query.conditions.length; i++,j++) {
            cond = query.conditions[i];
            val = cond.value || "";
            val += cond.jointFactorValues || "";

            formEl.append($.tmpl(tmpl, {name: "fact_" + j, value: cond.factor || ""}));
            formEl.append($.tmpl(tmpl, {name: "fexp_" + j, value: cond.expression || ""}));
            formEl.append($.tmpl(tmpl, {name: "fmex_" + j, value: cond.minExperiments || ""}));
            formEl.append($.tmpl(tmpl, {name: "fval_" + j, value: val}));
        }

        formEl.append($.tmpl(tmpl, {name: "view", value: query.view || ""}));

        $(document.body).append(formEl);

        var tform = $("#temporaryform");
        tform.attr("action", form.attr("action"));
        tform.submit();
    };

    atlas.clearQuery = function() {
        advancedForm.clear();
        atlas.simpleMode();
    };

    atlas.structMode = function() {
        if ($('#structform:visible').length)
            return;
        $('#simpleform').hide();
        $('#structform').show();
    };

    atlas.simpleMode = function() {
        if ($('#simpleform:visible').length)
            return;
        $('#simpleform').show();
        $('#structform').hide();
    };
})(jQuery);