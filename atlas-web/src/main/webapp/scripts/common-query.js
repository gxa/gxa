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

(function($){
    if(!atlas.homeUrl)
        atlas.homeUrl = '/';

    $.extend($.fn, {
        efoTree: function(o, handler) {
            if(!o.slideSpeed) o.slideSpeed = 500;
            if(!o.root) o.root = '';

            $(this).each( function() {
                var treeel = $(this);
                var hl;
                function showTree(c, t, downTo) {
                    c.addClass('wait');
                    $.getJSON(atlas.homeUrl + 'efo',
                        o.root == t ? {
                            downTo: downTo,
                            hl: o.highlight
                        } : {
                            childrenOf: t
                        },
                            function(result) {
                            c.removeClass('wait');
                            var ul = $('<ul/>').hide();
                            c.append(ul);

                            var currentDepth = 0;
                            var n;
                            var maxDepth = 0;
                            var scrollToElem = c;

                            if(!hl && result.hl) {
                                hl = [];
                                for(var i = 0; i < result.hl.length; ++i)
                                    hl[result.hl[i].id] = 1;
                            }
                            var data = result.tree;

                            for(var i = 0; i < data.length; ++i) {
                                var dc = data[i];
                                if(dc.depth > currentDepth) {
                                    if(dc.depth > maxDepth) {
                                        maxDepth = dc.depth;
                                        scrollToElem = n;
                                    }
                                    ul = $('<ul/>');
                                    n.addClass('expanded').removeClass('collapsed').append(ul);
                                } else while(dc.depth < currentDepth) {
                                    ul = ul.parent().parent();
                                    currentDepth--;
                                }
                                currentDepth = dc.depth;
                                var a;
                                if(dc.id == 'Other')
                                    a = $('<a>more...</a>');
                                else if(dc.alternativeTerms.length)
                                    a = $('<a />').attr('title', 'Alternative terms: ' + dc.alternativeTerms.join(', ')).text(dc.term).append(' <em>(' + dc.count + ') ' + dc.id + '</em>');
                                else
                                    a = $('<a />').text(dc.term).append(' <em>(' + dc.count + ') ' + dc.id + '</em>');
                                if(hl && hl[dc.id])
                                    a.addClass("hl");
                                n = $('<li />')
                                    .append($('<i>&nbsp;</i>'))
                                    .append(a);
                                $.data(n.get(0), "efotree", dc);
                                if(dc.expandable) {
                                    n.addClass('expandable').addClass('collapsed');
                                }
                                ul.append(n);
                            }

                            while(currentDepth > 0) {
                                ul = ul.parent().parent();
                                --currentDepth;
                            }

                            bindTree(c);

                            var scrollIt = function() {
                                var e = scrollToElem.get(0);
                                var t = treeel.get(0);
                                var y = e.offsetTop + e.clientHeight;
                                if(e.clientHeight < t.clientHeight && y > t.scrollTop + t.clientHeight)
                                    treeel.get(0).scrollTop = y - t.clientHeight;
                                else if(e.clientHeight > t.clientHeight && e.offsetTop > t.scrollTop)
                                    treeel.get(0).scrollTop = e.offsetTop;
                            };
                    
                            if(t == o.root) {
                                ul.show();
                                scrollIt();
                            } else {
                                ul.show().parent().removeClass('collapsed').addClass('expanded');
                                scrollIt();
                            }

                        });
                }

                function generateId(nodeid) {
                    return treeel.get(0).id + '_' + nodeid;
                }

                function bindTree(t) {
                    t.find('LI.expandable > i').click(function(event) {
                        var diz = $(this).parent();
                        if( diz.hasClass('collapsed') ) {
                            // Expand
                            if( !o.multiFolder ) {
                                diz.siblings('li.expanded').children('UL').parent().removeClass('expanded').addClass('collapsed').end().remove();
                                var dc = $.data(diz.get(0), "efotree");
                                showTree( diz, dc.id );
                            }
                        } else {
                            // Collapse
                            diz.find('UL').parent().removeClass('expanded').addClass('collapsed').end().remove();
                        }
                        event.stopPropagation();
                        return false;
                    });
                    t.find('a').click(function (event) {
                        if(handler) {
                            var dc = $.data($(this).parent().get(0), "efotree");
                            if(dc.id != 'Other') {
                                $(this).parent().andSelf().addClass('selected');
                                handler.call(treeel, dc);
                            }
                        } else {
                            if(event.ctrlKey || event.metaKey) {
                                $(this).parent().andSelf().toggleClass('selected');
                            } else {
                                treeel.find('LI.selected,a.selected').removeClass('selected');
                                $(this).parent().andSelf().addClass('selected');
                            }
                        }
                        event.stopPropagation();
                        return false;
                    });
                    t.find('LI').filter(':not(LI.expandable)').click(function (event) {
                        event.stopPropagation();
                        return false;
                    });
                }

                function killevent(e) { 
                    e.stopPropagation();
                    return false;
                }
                treeel.bind('startselect', killevent).bind('dblclick', killevent);
                treeel.bind('mousedown', function (e) {
                    if(e.shiftKey) e.stopPropagation();
                    return true;
                });

                showTree( treeel, o.root, o.downTo );
            });
            return $(this);
        },
        getSelection: function() {
            if ( this.length ) {
                var elem = $(this[0]);
                return jQuery.map(elem.find('LI.selected'), function (e) { return e.id.substr(elem.get(0).id.length + 1); });
            }
            return undefined;
        }
    });
    
    atlas.showApiLinks = function (url, callback) {
        if(callback) {
            url = callback(url);
            $('#jsonapilink').val(url + '&format=json');
            $('#xmlapilink').val(url + '&format=xml');
        }
        var p = $('#apilinks').show();
        p.find('.closebox').click(function () { p.hide(); });
    };

    atlas.copyText = function(e) {
        e.focus();
        e.select();
        if(e && e.createTextRange) {
            var t = e.createTextRange();
            t.execCommand("Copy");
        }
    };

    atlas.ajaxCall = function (url, data, successFunc, errorFunc) {
        return $.ajax({
            type: "GET",
            url: atlas.homeUrl + url,
            dataType: "json",
            data: data,
            success: function(resp) {
                $('#waiter,.waiter').remove();
                if(resp.error) {
                    if(console && typeof(console.log) == 'function')
                       console.log('AJAX Execution Error url=' + url + ': ' + resp.error);
                    if(errorFunc)
                        errorFunc(resp.error);
                    return;
                }
                if(successFunc)
                    successFunc(resp);
            },
            error: function() {
                if(console && typeof(console.log) == 'function')
                   console.log('AJAX Error url=' + url);

            }
        });
    };


})(jQuery);

