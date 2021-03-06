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

        fvalfield.tokenInput(atlas.fullPathFor("fval"), $.extend(true, {}, commonTokenOptions,
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
                var id = item.property === "efo" ? item.id : (item.value || "");

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
                span.append($("<em>").text(" (" + item.count + " genes) "));
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
        fvalfield.tokenInput(atlas.fullPathFor("fval"), $.extend(true, {}, commonTokenOptions,
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
                var text;
                if (row.property) {
                    text = row.property + ":";
                }
                var text = text + (row.property == "gene" && row.value == row.id && row.otherNames.length > 0 ? row.otherNames[0] : row.value);
                return text;
            },

            formatId: function(res) {
                return res.property == "gene" ? res.id : res.value;
            }

        }));
    };

    /**
     *  Query Builder //////////////////////////////////////////////////////////////////////////////////////////////////
     */

    function QueryBuilder() {
        if (!(this instanceof arguments.callee)) {
            return new QueryBuilder();
        }

        var q = {
            geneConditions:[],
            conditions:[],
            species:[],
            view:"",
            searchMode:"advanced"
        };

        function trim(value) {
            return $.trim(value || "");
        }

        function isValid() {
            // For gene-only queries an empty (experiment) condition may be passed in order to add to the query
            // user's expression-type selection.
            return q.geneConditions.length > 0 || (q.conditions.length > 0 && q.conditions[0].value != '');
        }

        // condition => {
        //   value - required
        //   property  - required
        //   not - optional (condition negation)
        // }
        this.addGeneCondition = function (condition) {
            if ((condition.value = trim(condition.value)).length) {
                q.geneConditions.push(condition);
            }
        };

        // condition => {
        //   value - required
        //   expression - required
        //   factor - optional (default is 'Any')
        //   minExperiments - optional (default is 1)
        // }
        this.addCondition = function (condition) {
            q.conditions.push(condition);
        };

        this.addSpecies = function (value) {
            if ((value = trim(value)).length) {
                q.species.push(value);
            }
        };

        this.setView = function (value) {
            q.view = value;
        };

        this.setSearchMode = function (mode) {
            q.searchMode = mode;
        }

        this.query = function () {
            return isValid() ? q : null;
        };
    }

    /**
     *  Simple form ////////////////////////////////////////////////////////////////////////////////////////////////////
     */

    var simpleForm = (function() {

        var timeout;

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
            // geneProperties array contains tokenized property-values;
            conditionsField.get(0).geneProperties = []

            var nonTokenizedVals = "";
            var tokenizedVals = "";
            for (var i = 0; i < query.geneConditions.length; ++i) {
                var prop = query.geneConditions[i].factor;
                var val =  query.geneConditions[i].jointFactorValues;
                if (prop) {
                    // Note that due to some race conditions in jquery.token.autocomplete.js a delay (c.f. timeout below)
                    // is needed to prevent the same property-value being added again every time the user presses a search button.
                    // To test this, remove the delay below then
                    // 1. search for autocompleted go term: 'BRCA1-A complex', on heatmap page click on advanced interface and
                    //    observe one 'goterm:BRCA1-A complex' gene condition
                    // 2. Click on 'search' again and agaon on heatmap page click on advanced interface and now
                    //    observe not one but two 'goterm:BRCA1-A complex' gene conditions.
                    clearTimeout(timeout);
                    timeout = setTimeout(function() {
                        conditionsField.get(0).geneProperties.push({
                            value: val,
                            property: prop
                        })
                    }, 100);
                    tokenizedVals = tokenizedVals + " " + val;
                } else {
                    nonTokenizedVals = nonTokenizedVals + " " + val;
                }
            }
            // Property-values in conditionsField.get(0).geneProperties are used by atlas.tokenizeGeneInput() to generate autocomplete tokens, visible to the user in the genes input field
            conditionsField.val($.trim(tokenizedVals));
            atlas.tokenizeGeneInput(conditionsField, '', '(all genes)');

            // Now that the genes input field has been enabled for autocompletion, enter non-autocompleted gene conditions into the
            // special field created by autocomplete to enter such raw values (c.f.  var input_box in jquery.token.autocomplete.js)
            // Note that due to some race conditions within jquery.token.autocomplete.js populateNonTokenizedVals will not work unless
            // the delay is imposed (see timeout below)
            clearTimeout(timeout);
            nonTokenizedVals = $.trim(nonTokenizedVals);
            if (nonTokenizedVals) {
                timeout = setTimeout(function() {
                    $('input[id="gene"]').val(nonTokenizedVals)
                }, 300);
            }

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

        function initFormTips() {
            $("#simple_genes_tip").qMarkTip(
                "Please enter a gene name, synonym, Ensembl or UniProt identifier, GO category, etc. Start typing and autosuggest will help you narrow down your choice."
            );
            $("#simple_conditions_tip").qMarkTip(
                "Please enter an experimental condition or tissue, etc. Start typing and autosuggest will help you narrow down your choice."
            );
        }

        function optionalQuote(s) {
            return (s.indexOf(' ') >= 0 && !(s.charAt(0) == '"' && s.charAt(s.length - 1) == '"')) ? '"' + s.replace(/["]/g, '\\"') + '"' : s;
        }

        // if properties for all values in conditionsField.get(0).geneProperties are either the same or unspecified,
        // assume property for the first value applies to all values; otherwise assume 'any' property for all values.
        function findCommonGeneProperty(tokenizedPropsVals) {
            var commonProp = ""; // common <= any
            for (var i = 0; i < tokenizedPropsVals.length; i++) {
                if (commonProp.length == 0) {
                    commonProp = tokenizedPropsVals[i].property;
                } else if (commonProp != tokenizedPropsVals[i].property) {
                    commonProp = "";
                    break;
                }
            }
            return commonProp;
        }

        function asQuery(form) {

            var qBuilder = new QueryBuilder();

            var conditionsField = geneConditionsField(form); // field containing tokenized property-values;
            var inputBox = $('input[id="gene"]'); // field containing non-tokenized values

            // For 'BRCA1' and 'BRCA1-A complex' values chosen from autocomplete and 'aspm' value entered raw, the following holds:
            // conditionsField.get(0).geneProperties =
            //      [ Object { value="BRCA1", property="interproterm"}, Object { value="BRCA1-A complex", property="goterm"}]
            // conditionsField.val() =
            //      'BRCA1 "BRCA1-A complex" aspm'
            // inputBox.val() =
            //      "aspm"

            var tokenizedPropsVals = conditionsField.get(0).geneProperties;
            var nonTokenizedVals = $.trim(inputBox.val())
            var commonProp = findCommonGeneProperty(tokenizedPropsVals);
            var val = $.trim(conditionsField.val() + " " + nonTokenizedVals);
            // TODO When Atlas can handle OR logic for values of different properties, e.g. prop1:val1 OR prop2:val2, where prop1 != prop2 (c.f. Ticket #3508),
            // the code should:
            // 1. Loop through tokenizedPropsVals and add the following OR condition to qBuilder:
            //    {value: optionalQuote($.trim(tokenizedPropsVals[i].value, property: tokenizedPropsVals[i].property }
            // 2. Then loop through nonTokenizedVals and add the following OR condition to qBuilder:
            //    {value: nonTokenizedVals, property: "" }
            qBuilder.addGeneCondition({
                value: val,
                property: commonProp
            });

            inputBox = $('input[id="efoefv"]'); // field containing non-tokenized experimental condition values
            var expConditions = $.trim(expConditionField(form).val() + " " + inputBox.val());
            qBuilder.addCondition({
                expression: expressionField(form).val(),
                value: expConditions
            });

            qBuilder.addSpecies(speciesField(form).val());

            qBuilder.setView($('input[name=view]:checked', form).val());

            qBuilder.setSearchMode("simple");

            return qBuilder.query();
        }

        return {
            init: function(query) {
                query = query || {};

                var form = $('#simpleform');

                initGeneConditions(form, query);
                initExpConditions(form, query);
                initSpecies(form, query);
                initFormTips();

                form.bind('submit', function () {
                    try {
                        submitForm(asQuery(form), form, {
                            failureMsg: "Please specify at least one gene or condition"
                        });
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

        var timeout;

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
            var factorLabel = factor;

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
            var nonTokenizedVals = "";
            var input = $('<input type="text" class="value"/>')
                .attr('name', "gval_" + sequence.currVal());
            if (property) {
                input.val(values != null ? values : "");
            } else {
                nonTokenizedVals = $.trim(values);
            }

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

            // Now that the genes input field has been enabled for autocompletion, enter non-autocompleted gene conditions into the
            // special field created by autocomplete to enter such raw values (c.f.  var input_box in jquery.token.autocomplete.js)
            // Note that due to some race conditions within jquery.token.autocomplete.js populateNonTokenizedVals will not work unless
            // the delay is imposed (see timeout below)
            clearTimeout(timeout);
            if (nonTokenizedVals) {
                timeout = setTimeout(function() {
                    $('input[name="gval_' + sequence.currVal() + '"]').val(nonTokenizedVals)
                }, 300);
            }

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
            flushTokenizedValues(form);

            var qBuilder = new QueryBuilder();

            $('input.specval').each(function() {
                qBuilder.addSpecies(this.value);
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
                qBuilder.addCondition(condition);
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
                qBuilder.addGeneCondition(condition);
            });

            qBuilder.setView($('input[name=view]:checked', form).val());
            qBuilder.setSearchMode("advanced");
            return qBuilder.query();
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
                         submitForm(asQuery(form), form, {
                             failureMsg: "Please specify at least one gene property or experimental factor condition"
                         });
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
     * Private routine  ///////////////////////////////////////////////////////////////////////////////////////////////
     */

    /**
     * Triggers 'preSubmit' event on all tokenized input fields in the form
     * to flush tokenized values into corresponding hidden form fields
     */
    function flushTokenizedValues(form) {
        $(form).find('input:hidden').trigger('preSubmit');
    }

    function showSearchingIndicator(form) {
        var v = $(form).find('input[type=submit]');
        v.val('Searching...');
    }

    function submitForm(query, form, msg) {
        if (query) {
            showSearchingIndicator(form);
            submitQuery(query, form);
        } else if (msg){
            showFormNotice(form, msg.failureMsg || "Can't submit empty query");
        }
    }

    function showFormNotice(form, msg) {
        var notice = $('#formNotice');
            if (notice.length == 0) {
                $('body').prepend(
                '<div id="formNotice" style=" color: red; background-color:#fff; position:absolute; z-index: 1000"></div>');
            notice = $('#formNotice').html(msg);
            }

        var pos = $(form).offset();
            var height = $(form).height();
        notice.css({ "left":pos.left + "px", "top":(pos.top + height + 10) + "px" });
        notice.fadeIn().delay(6000).fadeOut('slow');
        }

    function submitQuery(query, form) {
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

        formEl.append($.tmpl(tmpl, {name: "searchMode", value: query.searchMode || "advanced"}));

        $(document.body).append(formEl);

        var tform = $("#temporaryform");
        tform.attr("action", form.attr("action"));
        tform.submit();
    }

    /**
     * Public
     */

    atlas.initSearchForm = function(query, viewMode) {
        atlas.latestSearchQuery = query;

        // disable Firefox's bfcache
        $(window).unload(function() {
            return true;
        });

        simpleForm.init(query);

        if (viewMode === "advanced") {
        advancedForm.init(query);
        }
    };

    atlas.submitQuery = function(query) {
        submitQuery(query);
    };

    atlas.clearQuery = function() {
        advancedForm.clear();
    };

    atlas.structMode = function() {
        $('#simpleform').hide();
        $('#structform').show();
    };

    atlas.simpleMode = function() {
        $('#simpleform').show();
        $('#structform').hide();
    };
})(jQuery);