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

(function($) {

$.fn.tokenInput = function (url, options) {
    var settings = $.extend({
        url: url,
        noResultsText: "No results",
        searchDelay: 300
    }, options);

    settings.classes = $.extend({
        tokenList: "token-input-list",
        token: "token-input-token",
        selectedToken: "token-input-selected-token",
        dropdown: "token-input-dropdown",
        dropdownItem: "token-input-dropdown-item",
        dropdownItem2: "token-input-dropdown-item2",
        selectedDropdownItem: "token-input-selected-dropdown-item",
        inputToken: "token-input-input-token",
        browseIcon: "toke-input-browse"
    }, options.classes);

    return this.each(function () {
        var list = new $.TokenList(this, settings);
    });
};

$.fn.hideResults = function () { this.trigger('hideResults');return this; };
$.fn.flushCache = function () { this.trigger('flushCache');return this; };
$.fn.setOptions = function (options) { this.trigger('setOptions', [options]);return this; };
$.fn.fullVal = function () { var r = ['']; this.trigger('fullVal', [r]);return r[0]; };

    // Highlight the query part of the search term
$.highlightTerm = function (value, term, tag) {
    if(!tag) tag = 'b';
    return value.replace(new RegExp("(?![^&;]+;)(?!<[^<>]*)(" + term + ")(?![^<>]*>)(?![^&;]+;)", "gi"), "<" + tag +">$1</" + tag +">");
};

$.TokenList = function (input, settings) {
    //
    // Variables
    //

    // Input box position "enum"
    var POSITION = {
        BEFORE: 0,
        AFTER: 1,
        END: 2
    };

    // Keys "enum"
    var KEY = {
        BACKSPACE: 8,
        RETURN: 13,
        LEFT: 37,
        UP: 38,
        RIGHT: 39,
        DOWN: 40,
        ESC: 27
    };

    var prevent_blur = false;

    // Basic cache to save on db hits
    var cache = new $.TokenList.Cache();

    // Keep track of the timeout
    var timeout;

    var xhr;

    var previousValue;

    // Create a new text input an attach keyup events
    var input_box = $("<input type='text'>")
        .attr('autocomplete', 'off')
        .val(previousValue = (settings.defaultValue ? settings.defaultValue : ''))
        .focus(function () {
            if(settings.defaultValue && $(this).val() == settings.defaultValue)
                $(this).val('');
            if(settings.hintText)
                show_dropdown_hint();
        })
        .blur(function () {
            if(prevent_blur) {
                $(this).focus();
                return false;
            }        

            if(settings.defaultValue && $(this).val() == '' && token_list.find('.' + settings.classes.token).length == 0)
                $(this).val(settings.defaultValue);
            hide_dropdown(true);
        })
        .keydown(function (event) {
            var previous_token;
            var next_token;

            switch(event.keyCode) {
                case KEY.ESC:
                    if(resultsVisible())
                        hide_dropdown(false);
                    break;
            
                case KEY.LEFT:
                case KEY.RIGHT:
                    if(!$(this).val()) {

                        previous_token = input_token.prev();
                        next_token = input_token.next();

                        if(next_token.is('.' + settings.classes.browseIcon))
                            next_token = [];

                        if((previous_token.length && previous_token.get(0) === selected_token) || (next_token.length && next_token.get(0) === selected_token)) {
                            // Check if there is a previous/next token and it is selected
                            if(event.keyCode == KEY.LEFT) {
                                deselect_token($(selected_token), POSITION.BEFORE);
                            } else {
                                deselect_token($(selected_token), POSITION.AFTER);
                            }
                        } else if((event.keyCode == KEY.LEFT) && previous_token.length) {
                            // We are moving left, select the previous token if it exists
                            select_token($(previous_token.get(0)));
                        } else if((event.keyCode == KEY.RIGHT) && next_token.length) {
                            // We are moving right, select the next token if it exists
                            select_token($(next_token.get(0)));
                        }
                        return false;
                    }
                    break;

                case KEY.UP:
                case KEY.DOWN:
                    if(resultsVisible()) {
                        var dropdown_item = null;

                        if(event.keyCode == KEY.DOWN) {
                            dropdown_item = selected_dropdown_item == null ? dropdown_ul.children(':first') : $(selected_dropdown_item).next();
                        } else {
                            dropdown_item = selected_dropdown_item == null ? dropdown_ul.children(':last') : $(selected_dropdown_item).prev();
                        }

                        if(dropdown_item.length) {
                            select_dropdown_item(dropdown_item);
                        }
                    } else if(event.keyCode == KEY.DOWN) {
                        show_dropdown_searching();
                        do_search(true);
                    }
                    return false;

                case KEY.BACKSPACE:
                    previous_token = input_token.prev();

                    if(!$(this).val().length) {
                        if(selected_token) {
                            delete_token($(selected_token));
                        } else if(previous_token.length) {
                            select_token($(previous_token.get(0)));
                        }

                        return false;
                    } else if($(this).val().length == 1) {
                        hide_dropdown(false);
                    } else {
                        show_dropdown_searching();
                        clearTimeout(timeout);
    				    timeout = setTimeout(on_change, settings.searchDelay);
                    }
                    break;

                case KEY.RETURN:
                    if(selected_dropdown_item) {
                        var li_data = $.data($(selected_dropdown_item).get(0), "tokeninput");
                        add_token(li_data);
                        return false;
                    } else {
                        hide_dropdown(true);
                    }
                    break;

                default:
                    if(input_box.val() == settings.defaultValue)
                    {
                        input_box.val('');
                    }
                    if(is_printable_character(event.keyCode)) {
                        show_dropdown_searching();
                        clearTimeout(timeout);
    				    timeout = setTimeout(on_change, settings.searchDelay);
				    }

                    break;
            }
        });

    // Keep a reference to the original input box
    var hidden_input = $(input)
            .hide()
            .focus(function () {
        input_box.focus();
    }).blur(function () {
        input_box.blur();
    }).bind('flushCache', function () {
        cache.clear();
    }).bind('hideResults', function () {
        stop_results();
    }).bind('preSubmit', function () {
        stop_results();
        hidden_input.val(full_value());
    }).bind('setOptions', function () {
        $.extend(settings, arguments[1]);
    }).bind('restore', function () {
        token_list.remove();
        hidden_input.unbind().show();
        dropdown.remove();
    }).bind('fullVal', function () {
        arguments[1][0] = full_value();
    });

    // Keep a reference to the selected token and dropdown item
    var selected_token = null;
    var selected_dropdown_item = null;
    var dropdown_ul = null;

    // The list to store the token items in
    var token_list = $("<ul />")
        .addClass(settings.classes.tokenList)
        .insertAfter(hidden_input)
        .click(function (event) {
            var li = get_element_from_event(event, "li");
            if(li && li.is('.' + settings.classes.browseIcon)) {
                input_box.focus();
                if(selected_token) {
                    deselect_token($(selected_token), POSITION.END);
                }
                
                open_browser();
                return false;
            } else if(li && li.get(0) != input_token.get(0)) {
//                toggle_select_token(li);
                return false;
            } else {
                input_box.focus();

                if(selected_token) {
                    deselect_token($(selected_token), POSITION.END);
                }
            }
        })
        .mousedown(function (event) {
            // Stop user selecting text on tokens
            var li = get_element_from_event(event, "li");
            if(li && !li.is('.' + settings.classes.inputToken)) {
                return false;
            }
        });

    if(settings.plugins)
        token_list.append(settings.plugins);


    // The token holding the input box
    var input_token = $("<li />")
        .addClass(settings.classes.inputToken)
        .appendTo(token_list)
        .append($('<span/>').append(input_box));

    var browser_icon = $([]);
    if(settings.browser) {
        browser_icon = $("<li />")
                .addClass(settings.classes.browseIcon)
                .appendTo(token_list);
    }

    // The list to store the dropdown items in
    var dropdown = $("<div>")
        .addClass(settings.classes.dropdown)
        .appendTo(document.body)
        .hide();

    if(hidden_input.val() != '') {
        input_box.val('');
        var vals = splitQuotes(hidden_input.val());
        hidden_input.val('');
        var others = [];
        $.get(settings.url, $.extend({"q": vals, limit: vals.length }, settings.extraParams), function (results) {
            for(var qi in vals) {
                var res = settings.getItemList(results, vals[qi]);
                var b = false;
                for(var ri in res)
                    if(settings.formatId(res[ri]).toLowerCase() == vals[qi].toLowerCase()) {
                        add_token(res[ri], true);
                        b = true;
                        break;
                    }
                if(!b) {
                    others.push(optionalQuote(vals[qi]));
                }
                input_box.val(others.join(' '));
            }
        }, "json");

    }


    //
    // Functions
    //

    function full_value() {
        var val = hidden_input.val();
        if(val != '')
            val += ' ';
        if(input_box.val() != '' && input_box.val() != settings.defaultValue)
            val += input_box.val();
        return val;
    }

    function stop_results() {
        if(xhr && typeof(xhr.abort) == 'function') {
            xhr.abort();
            xhr = null;
        }
        hide_dropdown(false);
    }

    function resultsVisible() {
        return dropdown && dropdown.is(":visible");        
    }

    function dropdown_add_hidetext() {
        dropdown.append($('<p/>').addClass(settings.classes.hideText).html(settings.hideText).mousedown(function (e) {
            prevent_blur = true;
        }).click(function () {
            hide_dropdown(false);
            prevent_blur = false;
        }));
    }

    function show_dropdown() {
        var temp = $('<div/>').insertAfter(token_list);
        var offset = temp.offset();
        temp.remove();
        dropdown.css({ top: offset.top + 'px', left: offset.left + 'px'})
        dropdown.show();
    }

    function open_browser() {
        if(!settings.browser)
            return;

        browser_icon.hide();

        var lastId;
        var prev = input_token.prev();
        if(prev.length)
            lastId = settings.formatId($.data(prev.get(0), "tokeninput"));
        else
            lastId = input_box.val();

        var browse_el = settings.browser(function (data) {
            add_token(data);
            hide_dropdown(false);
        }, lastId, hidden_input.val());

        dropdown.empty().append(browse_el);

        dropdown_add_hidetext();
        dropdown.mousedown(function () { prevent_blur = true; }).mouseup(function () { /*prevent_blur = false;*/ });

        show_dropdown();
    }

    function is_printable_character(keycode) {
        if((keycode >= 48 && keycode <= 90) ||      // 0-1a-z
           (keycode >= 96 && keycode <= 111) ||     // numpad 0-9 + - / * .
           (keycode >= 186 && keycode <= 192) ||    // ; = , - . / ^
           (keycode >= 219 && keycode <= 222)       // ( \ ) '
          ) {
              return true;
          } else {
              return false;
          }
    }

    // Get an element of a particular type from an event (click/mouseover etc)
    function get_element_from_event (event, element_type) {
        var target = $(event.target);
        var element = null;

        if(target.is(element_type)) {
            element = target;
        } else if(target.parents(element_type).length) {
            element = target.parents(element_type+":first");
        }

        return element;
    }

    // Add a token to the token list
    function add_token (li_data, init) {

        // Clear input box and make sure it keeps focus
        input_box
            .val("")
            .focus();

        // Don't show the help dropdown, they've got the idea
        hide_dropdown(false);

        var newid = settings.formatId(li_data);

        var vals = splitQuotes(hidden_input.val());
        for(var i = 0; i < vals.length; ++i)
            if(vals[i] == newid)
                return;
            else
                vals[i] = optionalQuote(vals[i]);


        var this_token = $("<li><p>"+ settings.formatToken(li_data) +"</p> </li>")
            .addClass(settings.classes.token)
            .insertBefore(input_token);

        if(settings.formatTokenTooltip)
            this_token.attr('title', settings.formatTokenTooltip(li_data));

        $("<span>x</span>")
            .appendTo(this_token)
            .click(function () {
                delete_token($(this).parent());
                return false;
            });

        $.data(this_token.get(0), "tokeninput", li_data);

        // Save this token id

        vals.push(optionalQuote(newid));
        hidden_input.val(vals.join(' '));

        if(!init)
            hidden_input.trigger('addResult', li_data);
    }

    // Select a token in the token list
    function select_token (token) {
        token.addClass(settings.classes.selectedToken);
        selected_token = token.get(0);

        // Hide input box
        input_box.val("");

        // Hide dropdown if it is visible (eg if we clicked to select token)
        hide_dropdown(false);
    }

    // Deselect a token in the token list
    function deselect_token (token, position) {
        token.removeClass(settings.classes.selectedToken);
        selected_token = null;

        if(position == POSITION.BEFORE) {
            input_token.insertBefore(token);
        } else if(position == POSITION.AFTER) {
            input_token.insertAfter(token);
        } else if(browser_icon) {
            input_token.insertBefore(browser_icon);
        } else {
            input_token.appendTo(token_list);
        }

        // Show the input box and give it focus again
        input_box.focus();
    }

    // Toggle selection of a token in the token list
    function toggle_select_token (token) {
        if(selected_token == token.get(0)) {
            deselect_token(token, POSITION.END);
        } else {
            if(selected_token) {
                deselect_token($(selected_token), POSITION.END);
            }
            select_token(token);
        }
    }

    function optionalQuote(s) {
        return (s.indexOf(' ') >= 0 || s.indexOf('"') >=0) ? '"' + s.replace(/["]/g, '\\"') + '"' : s;
    }

    function splitQuotes(value) {
        var sep = ' ';
        var result = [];
        var curVal = '';
        var p = 0;
        var inQuotes = false;
        while(p < value.length) {
            var c = value.charAt(p++);
            if(inQuotes)
            {
                if(c == '\\') {
                    if(p >= value.length)
                        break;

                    curVal += value[p++];
                } else if(c == '"') {
                    inQuotes = false;
                } else {
                    curVal += c;
                }
            } else {
                if(c == sep)
                {
                    if(curVal.length > 0) {
                        result[result.length] = curVal;
                        curVal = '';
                    }
                } else if(c == '"') {
                    inQuotes = true;
                } else {
                    curVal += c;
                }
            }
        }
        if(curVal.length > 0)
            result[result.length] = curVal;
        return result;
    }


    // Delete a token from the token list
    function delete_token (token) {
        // Remove the id from the saved list
        var token_data = $.data(token.get(0), "tokeninput");

        // Delete the token
        token.remove();
        selected_token = null;

        // Show the input box and give it focus again
        input_box.focus();

        // Delete this token's id from hidden input
        var vals = splitQuotes(hidden_input.val());
        var toremove = settings.formatId(token_data);

        for(var i in vals)
            if(toremove == vals[i]) {
                vals.splice(i, 1);
                break;
            }
        for(i = 0; i < vals.length; ++i)
            vals[i] = optionalQuote(vals[i]);
        hidden_input.val(vals.join(' '));
    }

    // Hide and clear the results dropdown
    function hide_dropdown (nofocus) {
        dropdown.hide().empty();
        selected_dropdown_item = null;
        browser_icon.show();
        if(!nofocus)
            input_box.focus();
    }

    function show_dropdown_searching () {
        browser_icon.show();
        if(settings.searchingText) {
            dropdown.html("<p>"+settings.searchingText+"</p>");
            show_dropdown();
        }
    }

    function show_dropdown_hint () {
        dropdown.html("<p>"+settings.hintText+"</p>");
        show_dropdown();
    }

    // Populate the results dropdown with some results
    function populate_dropdown (query, results, select_first) {
        results = settings.getItemList(results, query);
        if(results.length) {
            dropdown.empty();
            dropdown_ul = $("<ul>")
                .appendTo(dropdown)
                .mouseover(function (event) {
                    select_dropdown_item(get_element_from_event(event, "li"));
                })
                .click(function (event) {
                    var li_data = $.data(get_element_from_event(event, "li").get(0), "tokeninput");
                    add_token(li_data);
                })
                .mousedown(function (event) {
                    prevent_blur = true;
                    // Stop user selecting text on tokens
                    return false;
                })
                .hide();

            for(var i in results) {
                if (results.hasOwnProperty(i)) {
                    var this_li = $("<li>"+ settings.formatListItem(results[i], query, i) + "</li>")
                                      .appendTo(dropdown_ul);

                    if(i%2) {
                        this_li.addClass(settings.classes.dropdownItem);
                    } else {
                        this_li.addClass(settings.classes.dropdownItem2);
                    }

                    $.data(this_li.get(0), "tokeninput", results[i]);
                }
            }

            if(select_first)
                select_dropdown_item(dropdown_ul.children(':first'));
            else
                selected_dropdown_item = null;

            dropdown_add_hidetext();

            show_dropdown();
            dropdown_ul.show();

        } else {

            dropdown.html("<p>"+settings.noResultsText+"</p>");
            show_dropdown();
        }
    }

    // Highlight an item in the results dropdown
    function select_dropdown_item (item) {
        if(item) {
            if(selected_dropdown_item) {
                deselect_dropdown_item($(selected_dropdown_item));
            }

            item.addClass(settings.classes.selectedDropdownItem);
            selected_dropdown_item = item.get(0);
        }
    }

    // Remove highlighting from an item in the results dropdown
    function deselect_dropdown_item (item) {
        item.removeClass(settings.classes.selectedDropdownItem);
        selected_dropdown_item = null;
    }

    function on_change() {
        var newval = input_box.val();
        if(previousValue != newval && newval.length > 0)
            do_search(false);
        else if(newval.length == 0)
            hide_dropdown(false);
        
        previousValue = newval;
    }

    // Do a search
    function do_search(select_first) {
        var query = input_box.val().toLowerCase();

        if(query && query.length) {
            if(selected_token) {
                deselect_token($(selected_token), POSITION.AFTER);
            }

            var cached_results = cache.get(query);
            if(cached_results) {
                populate_dropdown(query, cached_results, select_first);
            } else {
                token_list.addClass(settings.classes.searching);
                if(xhr && typeof(xhr.abort) == 'function')
                    xhr.abort();
                xhr = $.get(settings.url, $.extend({"q": query }, settings.extraParams), function (results) {
                    token_list.removeClass(settings.classes.searching);
                    cache.add(query, results);
                    populate_dropdown(query, results, select_first);
                }, "json");
            }
        }
    }
};

// Really basic cache for the results
$.TokenList.Cache = function (options) {

    var settings = $.extend({
        max_size: 10
    }, options);

    var data = {};
    var order = [];

    this.clear = function () {
        data = {};
        order = [];
    };

    this.add = function (query, results) {
        data[query] = results;
        order.push(query);

        while(order.length > settings.max_size) {
            delete data[order[0]];
            order.splice(0, 1);
        }
    };

    this.get = function (query) {
        return data[query];
    };
};

})(jQuery);

