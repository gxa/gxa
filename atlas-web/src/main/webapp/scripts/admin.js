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
jQuery.each(['backgroundColor'], function(i,attr){
    jQuery.fx.step[attr] = function(fx){
        if (!fx.inited) {
            var r = /rgb\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*\)/.exec(jQuery.curCSS(fx.elem, attr));
            fx.start = r ? [parseInt(r[1]), parseInt(r[2]), parseInt(r[3])] : [255,255,0];
            r = /rgb\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*\)/.exec(fx.end);
            fx.end = r ? [parseInt(r[1]), parseInt(r[2]), parseInt(r[3])] : [255,255,255];
            fx.inited = true;            
        }

        fx.elem.style[attr] = "rgb(" + [
            Math.max(Math.min( parseInt((fx.pos * (fx.end[0] - fx.start[0])) + fx.start[0]), 255), 0),
            Math.max(Math.min( parseInt((fx.pos * (fx.end[1] - fx.start[1])) + fx.start[1]), 255), 0),
            Math.max(Math.min( parseInt((fx.pos * (fx.end[2] - fx.start[2])) + fx.start[2]), 255), 0)
        ].join(",") + ")";
    };
});

$.fn.vale = function() {
    var v = this.val();
    return v == undefined ? "" : v;
};

var currentState = {};
var atlas = { homeUrl: '' };
var selectedExperiments = {};
var selectAll = false;
var $time = {};
var $tpl = {};
var $tab = {};
var lastLogPages;
var lastLogTimestamp;
var $options = {
    queueRefreshRate: 5000,
    queuePageSize: 10,
    experimentPageSize: 20,
    arraydesignPageSize: 20,
    searchDelay: 500,
    tasklogPageSize: 20
};

var $msg = {
    taskType: {
        analytics: 'Compute analytics',
        loadexperiment: 'Load experiment',
        loadarraydesign: 'Load array design',
        index: 'Build index',
        unloadexperiment: 'Unload experiment',
        updateexperiment: 'Update NetCDF',
        indexexperiment: 'Index experiment',
        repairexperiment: 'Repair experiment',
        datarelease: 'Release data'
    },
    runMode: {
        RESTART: '[Restart]',
        CONTINUE: '[Continue]'
    },
    event: {
        SCHEDULED: 'Scheduled',
        CANCELLED: 'Cancelled',
        STARTED: 'Started',
        FINISHED: 'Finished',
        WARNING: 'Warning',
        SKIPPED: 'Skipped',
        FAILED: 'Failed'
    },
    taskState: {
        WORKING: 'Working',
        PENDING: 'Pending'
    }
};

function msgMapper(field, dict) {
    return function (r) { return r.item[field] ? $msg[dict][r.item[field]] : ''; };
}

function msgMapperSelf(dict) {
    return function (r) { return r.item ? $msg[dict][r.item] : ''; };
}

var currentStateHash;

function storeState() {
    var urlParts = window.location.href.split('#');
    var stateString = '';
    for(var key in currentState) {
        var value = escape(currentState[key]);
        if(value != '') {
            if(stateString.length > 0)
                stateString += '&';
            stateString += key + '=' + value;
        }
    }
    if(stateString != urlParts[1]) {
        window.location.href = urlParts[0] + '#' + stateString;
        currentStateHash = stateString;
    }
}

function restoreState() {
    var urlParts = window.location.href.split('#');
    var newState = {};
    if(urlParts.length > 1) {
        var states = urlParts[1].split('&');
        for(var i = 0; i < states.length; ++i) {
            var keyValue = states[i].split('=');
            if(keyValue.length == 2) {
                var key = keyValue[0].replace(/[^a-zA-Z0-9-]/g, '');
                newState[key] = unescape(keyValue[1]);
            }
        }
    }

    currentStateHash = urlParts[1];
    currentState = newState;
    redrawCurrentState();
}

function requireLogin(op, params, func) {
    $('#loginMessage').text('');
    $('#loginForm').show();
    $('#tabs').hide();
    $('#logout').hide();

    $('#loginForm form').unbind('submit').submit(function () {
        adminCall('login', { userName: $('#loginUser').val(), password: $('#loginPassword').val() }, function (resp) {
            if(resp.success) {
                $('#loginForm').hide();
                $('#tabs').show();
                $('#logout').show();
                $('#userName').text(resp.userName);
                redrawCurrentState();
            } else {
                $('#loginMessage').text('Invalid username or password');
            }
        });
        return false;
    });
}

function adminCall(op, params, func) {
    $('.loadIndicator').css('visibility', 'visible');
    return $.ajax({
        type: "POST",
        url: atlas.homeUrl + "admin",
        dataType: "json",
        data: $.extend(params, { op : op }),
        success: function (json) {
            $('.loadIndicator').css('visibility', 'hidden');
            if(json.notAuthenticated) {
                requireLogin(op, params, func);
                return;
            } else if(op != 'login' && !$('#logout').is(':visible')) {
                adminCall('getuser', {}, function (r) {
                    $('#userName').text(r.userName);
                    $('#logout').show();
                });
            }

            if(json.error)
                alert(json.error);
            else
                func(json);
        },
        error: function() {
            $('.loadIndicator').css('visibility', 'hidden');
            alert('AJAX error for ' + op + ' ' + params);
        }});
}

function switchToQueue() {
    $('#tabs').tabs('select', $tab.que);
}

function bindHistoryExpands(root, type, items) {
    root.find('a.history').each(function (i,e) {
        var showHistory = function() {
            adminCall('tasklogtag', {
                type: type.toUpperCase(),
                accession: items[i].accession
            }, function(logresult) {
                var newc = $('<td/>').addClass('expLog').attr('colspan', $(e).parents('tr:first').find('td').length);
                var tr = $('<tr/>').append(newc);
                $(e).parents('tr:first').after(tr);
                newc.appendClassTpl(logresult, 'expTaskLog');
                $(e).parents('td:first').css('background', '#cdcdcd');
                $(e).text('Hide history').unbind('click').click(function () {
                    tr.remove();
                    $(e).text('Show history').click(showHistory);
                    $(e).parents('td:first').css('background', 'inherit');
                    return false;
                });
            });
            return false;
        };

        $(e).unbind('click').click(showHistory);
    });
}

function updateBrowseExperiments() {
    adminCall('searchexp', {

        p: currentState['exp-p'] || 0,
        n: $options.experimentPageSize,
        search: $('#experimentSearch').val(),
        fromDate: $('#dateFrom').val(),
        toDate: $('#dateTo').val(),
        pendingOnly: $('#incompleteOnly').val()

    }, function (result) {

        function updateExperimentButtons() {
            var cando = selectAll;
            for(var k in selectedExperiments) {
                cando = true;
                break;
            }
            if(cando)
                $('#experimentList .opbuttons input').removeAttr('disabled');
            else
                $('#experimentList .opbuttons input').attr('disabled', 'disabled');
        }

        if(result.page * $options.experimentPageSize > result.numTotal && result.numTotal > 0) {
            currentState['exp-p'] = Math.floor((result.numTotal - 1) / $options.experimentPageSize);
            storeState();
            updateBrowseExperiments();
            return;
        }

        renderTpl('experimentList', result);

        $('#expPages').pagination(result.numTotal, {
            current_page: result.page,
            num_edge_entries: 2,
            num_display_entries: 5,
            items_per_page: $options.experimentPageSize,
            prev_text: "Prev",
            next_text: "Next",
            callback: function(page) {
                currentState['exp-p'] = page;
                storeState();
                updateBrowseExperiments();
                return false;
            }});

        bindHistoryExpands($('#experimentList'), 'experiment', result.experiments);

        $('#experimentList tr input.selector').click(function () {
            if($(this).is(':checked'))
                selectedExperiments[this.value] = 1;
            else
                delete selectedExperiments[this.value];
            updateExperimentButtons();
        });

        var newAccessions = {};
        for(var i = 0; i < result.experiments.length; ++i)
            newAccessions[result.experiments[i].accession] = 1;
        for(i in selectedExperiments)
            if(!newAccessions[i])
                delete selectedExperiments[i];
        updateExperimentButtons();

        $('#selectAll').click(function () {
            if($(this).is(':checked')) {
                $('#experimentList tr input.selector').attr('disabled', 'disabled').attr('checked','checked');
                selectAll = true;
                $('#selectCollapsed').css('visibility', 'visible');
            } else {
                $('#experimentList tr input.selector').removeAttr('disabled').removeAttr('checked');
                selectedExperiments = {};
                selectAll = false;
                $('#selectCollapsed').css('visibility', 'hidden');
            }
            updateExperimentButtons();
        });

        if(selectAll)
            $('#selectAll').attr('checked', 'checked');

        function startSelectedTasks(type, mode, title, noAutodep) {
            var accessions = [];
            for(var accession in selectedExperiments)
                accessions.push(accession);

            if(accessions.length == 0 && !selectAll)
                return;

            var autoDep = noAutodep ? false : $('#experimentAutodep').is(':checked');
            if(window.confirm('Do you really want to ' + title + ' '
                    + (selectAll ? result.numTotal : accessions.length)
                    + ' experiment(s)' + (autoDep ? ' and rebuild index afterwards' : '') +'?')) {
                if(selectAll) {
                    adminCall('schedulesearchexp', {
                        runMode: mode,
                        type: type,
                        search: $('#experimentSearch').val(),
                        fromDate: $('#dateFrom').val(),
                        toDate: $('#dateTo').val(),
                        pendingOnly: $('#incompleteOnly').val(),
                        autoDepends: autoDep
                    }, switchToQueue);
                } else {
                    adminCall('schedule', {
                        runMode: mode,
                        accession: accessions,
                        type: type,
                        autoDepends: autoDep
                    }, switchToQueue);
                }
                
                selectedExperiments = {};
                selectAll = false;
            }
        }

        $('#experimentList input.analytics').click(function () {
            startSelectedTasks('analytics', 'RESTART', 'restart analytics of', false);
        });

        $('#experimentList input.unload').click(function () {
            startSelectedTasks('unloadexperiment', 'RESTART', 'unload', false);
        });

        $('#experimentList input.updatenetcdf').click(function () {
            startSelectedTasks('updateexperiment', 'RESTART', 'update NetCDF of', false);
        });

        $('#experimentList input.updateindex').click(function () {
            startSelectedTasks('indexexperiment', 'RESTART', 'update index of', true);
        });

        $('#experimentList input.datarelease').click(function () {
            startSelectedTasks('datarelease', 'RESTART', 'release packaging of', true);
        });

        $('#experimentList input.repair').click(function () {
            startSelectedTasks('repairexperiment', 'RESTART', 'repair', true);
        });

        $('#experimentList .rebuildIndex input').click(function () {
            if(window.confirm('Do you really want to rebuild complete index?')) {
                adminCall('schedule', {
                    runMode: 'RESTART',
                    accession: '',
                    type: 'index',
                    autoDepends: 'true'
                }, switchToQueue);
            }
        });
    });
}

function updatePauseButton(isRunning) {
    function unpauseTaskman() {
        adminCall('restart', {}, function () {
            updatePauseButton(true);
            updateQueue();
        });
    }

    function pauseTaskman() {
        adminCall('pause', {}, function () {
            updatePauseButton(true);
            updateQueue();
        });
    }
    $('#pauseButton').unbind('click').click(isRunning ? pauseTaskman : unpauseTaskman).val(isRunning ? 'Pause task execution' : 'Restart task execution');
    $('.taskmanPaused').css('display', isRunning ? 'none' : 'inherit');
}

function updateQueueAndLog() {
    updateQueue();
    updateTaskLog();
}

function updateQueue() {
    clearTimeout($time.queue);
    $time.queue = null;
    adminCall('tasklist', {
        p: currentState['que-p'] || 0,
        n: $options.queuePageSize
    }, function (result) {
        renderTpl('taskList', result);

        $('#numPending').text(result.numPending);
        $('#numWorking').text(result.numWorking);

        if(result.page != currentState['que-p']) {
            currentState['que-p'] = result.page;
            storeState();
        }

        if(result.numTotal > $options.queuePageSize)
            $('#taskListPages').pagination(result.numTotal, {
                current_page: result.page,
                num_edge_entries: 2,
                num_display_entries: 5,
                items_per_page: $options.queuePageSize,
                prev_text: "Prev",
                next_text: "Next",
                callback: function(page) {
                    currentState['que-p'] = page;
                    storeState();
                    updateQueue();
                    return false;
                }});
        else
            $('#taskListPages').empty();

        for(var i in result.tasks) {
            (function (task) {
                $('#taskList .cancelButton' + task.id).click(function () {
                    if(confirm('Do you really want to cancel task ' + task.type + ' ' + task.accession + '?')) {
                        adminCall('cancel', { id: task.id }, updateQueueAndLog);
                    }
                });
            })(result.tasks[i]);
        }

        $('#cancelAllButton').unbind('click').attr('disabled', result.tasks.length ? '' : 'disabled').click(function () {
            if(confirm('Do you really want to cancel all tasks?')) {
                adminCall('cancelall', {}, updateQueueAndLog);
            }
        });

        updatePauseButton(result.isRunning);

        $time.queue = setTimeout(updateQueue, $options.queueRefreshRate);
    });
}

function updateTaskLog() {
    clearTimeout($time.tasklog);
    $time.tasklog = null;
    var askPage = (currentState['tlog-p'] != null && currentState['tlog-p'] != lastLogPages - 1) ? currentState['tlog-p'] : -1;
    adminCall('tasklog', {
        event: currentState['tlog-ef'] || "",
        user: currentState['tlog-uf'] || "",
        type: currentState['tlog-tf'] || "",
        accession: currentState['tlog-af'] || "",
        p: askPage,
        n: $options.tasklogPageSize
    }, function (result) {
        if(currentState['tlog-p'] != result.page) {
            currentState['tlog-p'] = result.page;
            storeState();
        }

        lastLogPages = Math.ceil(result.numTotal/$options.tasklogPageSize);

        renderTpl('taskLogItems', result);

        function updateFilter(id, list, dict, stvar) {
            var filter = $(id);
            $.each(list, function (i, e) {
                var opt = $('<option/>').addClass('option').attr('value', e).text(dict ? dict[e] : e);
                if(e == currentState[stvar])
                    opt.attr('selected', 'selected');
                var found = false;
                $('option.option', filter).each(function () {
                    var v = $(this).attr('value');
                    if(e == v) {
                        found = true;
                    } else if(e < v && !found) {
                        $(this).before(opt);
                    }
                });
                if(!found)
                    $(filter).append(opt);
            });
        }

        updateFilter('#taskLogEventFilter', result.eventFacet, $msg.event, 'tlog-ef');
        updateFilter('#taskLogUserFilter', result.userFacet, null, 'tlog-uf');
        updateFilter('#taskLogTypeFilter', result.typeFacet, $msg.taskType, 'tlog-tf');

        if(result.items.length)
            $('#taskLog .none').hide();
        else
            $('#taskLog .none').show();

        if(result.items.length)
            if(!lastLogTimestamp || result.items[0].timestamp > lastLogTimestamp)
                lastLogTimestamp = result.items[0].timestamp;

        if(result.numTotal > $options.tasklogPageSize) {
            $('#taskLog .pager').show();
            $('#taskLogPages').pagination(result.numTotal, {
                current_page: result.page,
                num_edge_entries: 2,
                num_display_entries: 5,
                items_per_page: $options.tasklogPageSize,
                prev_text: "Prev",
                next_text: "Next",
                callback: function(page) {
                    lastLogTimestamp = null;
                    currentState['tlog-p'] = page;
                    storeState();
                    updateTaskLog();
                    return false;
                }});
        } else {
            $('#taskLogPages').empty();
            $('#taskLog .pager').hide();
        }

        $('#taskLog .retry input').each(function (i,e) {
            var li = result.items[i];
            if(li.event == 'FAILED')
                $(e).click(function() {
                    if(window.confirm('Are you sure you want to re-try to '
                            + $msg.taskType[li.type].toLowerCase() + (li.accession != '' ? ' ' + li.accession : '') +'?'))
                    {
                        var autoDep = window.confirm('Do you want to run further processing tasks automatically afterwards?');
                        adminCall('schedule', {
                            runMode: 'RESTART',
                            accession: li.accession,
                            type: li.type,
                            autoDepends: autoDep
                        }, switchToQueue);
                    }
                });
            else
                $(e).remove();
        });

        $('#taskLog .newitem').css('backgroundColor', 'rgb(255,255,0)')
                .animate({backgroundColor:'rgb(250,250,250)'}, Math.min($options.queueRefreshRate / 2, 500));

        if($('#taskLogRefresh').is(':checked'))
            $time.tasklog = setTimeout(function () {
                updateTaskLog();
            }, $options.queueRefreshRate);
    });
}

function updateProperties() {
    adminCall('proplist', {}, function (result) {
        renderTpl('propList', result);
        $('#propList tr.property td.value').each(function (i, o) {
            var pr = result.properties[i];
            $(o).click(function () {
                var i = $('<input type="text"/>').attr('name', pr.name).addClass('value').val("" + pr.value);
                $(this).empty().append(i).unbind('click');
                i.focus();
                $('#savePropsButton,#cancelPropsButton').removeAttr('disabled');
            });
        });
        $('#propList form').submit(function () {
            saveProperties();
            return false;
        });
        $('#savePropsButton,#cancelPropsButton').attr('disabled', 'disabled');
    });
}

function saveProperties() {
    var newValues = {};
    $('#propList table input.value').each(function (i, o) {
        newValues[$(o).attr('name')] = $(o).val();
    });
    adminCall('propset', newValues, updateProperties);
}

function updateArrayDesigns() {
    adminCall('searchad', {
        search: currentState['ad-s'] || "",
        p: currentState['ad-p'] || 0,
        n: $options.arraydesignPageSize
    }, function (result) {
        if(result.page * $options.arraydesignPageSize > result.numTotal && result.numTotal > 0) {
            currentState['ad-p'] = Math.floor((result.numTotal - 1) / $options.arraydesignPageSize);
            storeState();
            updateArrayDesigns();
            return;
        }

        renderTpl('adList', result);

        if(result.numTotal > $options.arraydesignPageSize)
            $('#adPages').pagination(result.numTotal, {
                current_page: result.page,
                num_edge_entries: 2,
                num_display_entries: 5,
                items_per_page: $options.arraydesignPageSize,
                prev_text: "Prev",
                next_text: "Next",
                callback: function(page) {
                    currentState['ad-p'] = page;
                    storeState();
                    updateArrayDesigns();
                    return false;
                }});
        else
            $('#adPages').empty();

        bindHistoryExpands($('#adList'), 'arraydesign', result.arraydesigns);
    });
}

var masterAtlasURL = "http://www.ebi.ac.uk/gxa";
//show downloadable updates
function updateAvailableUpdates(){
    //pull masterAtlasUrl
    adminCall('proplist', {}, function (result) {
        var a = "";
        for(var prop in result.properties){
            if(result.properties[prop].name == "atlas.masteratlas"){
                masterAtlasURL = result.properties[prop].value;
            }
        }
        updateAvailableUpdates2();
    });
}

function updateAvailableUpdates2(){
    var p = currentState['expupd-p'] || 0;
    var n = $options.experimentPageSize;
    var search = $('#txtUpdateSearch').val();
    var experimentKeyword = search == "" ? "listAll" : search;

    var head = document.getElementsByTagName('head');
    var script = document.createElement('script');
    script.type = "text/javascript";
    var start = p  * $options.experimentPageSize;
    script.src = masterAtlasURL + "/api/v0"
            +"?experiment="+experimentKeyword
            +"&experimentInfoOnly"
            +"&dateReleaseFrom=" + $('#dateReleaseFrom').val()
            +"&dateReleaseTo=" + $('#dateReleaseTo').val()
            +"&dateLoadFrom=" + $('#dateLoadFrom').val()
            +"&dateLoadTo=" + $('#dateLoadTo').val()
            +"&format=json&callback=processUpdatesData&start="+ start +"&rows=" + $options.experimentPageSize;
    head[0].appendChild(script);
}

//ajax calback, set all handlers here
function processUpdatesData(data){
    renderTpl('updatesList', data);

    $('#updatePages').pagination(data.totalResults, {
        current_page: data.startingFrom / $options.experimentPageSize,
        num_edge_entries: 2,
        num_display_entries: 5,
        items_per_page: $options.experimentPageSize,
        prev_text: "Prev",
        next_text: "Next",
        callback: function(page) {
            currentState['expupd-p'] = page;
            storeState();
            updateAvailableUpdates();
            return false;
        }});

    $('#updatesList tr input.selector').click(function () {
            if($(this).is(':checked'))
                selectedExperiments[this.value] = 1;
            else
                delete selectedExperiments[this.value];
        });

    $('#updatesList input.download').click(function () {
          var urls = [];

          if(selectAll){
              downloadAllUpdates();
              return;
          }

          for(var url in selectedExperiments)
                urls.push(url);

          adminCall('schedule', {
                    runMode: 'RESTART',
                    accession: urls,
                    type: 'loadexperiment',
                    autoDepends: false,
                    useRawData: false
                }, switchToQueue);
          selectedExperiments = {};
    });

    $('#selectAllUpdates').click(function () {
        if($(this).is(':checked')) {
            $('#updatesList tr input.selector').attr('disabled', 'disabled').attr('checked','checked');
            $('#selectAllUpdates').removeAttr('disabled');
            selectAll = true;
        } else {
            $('#updatesList tr input.selector').removeAttr('disabled').removeAttr('checked');
            selectedExperiments = {};
            selectAll = false;
        }
    });
}

function downloadAllUpdates(){
    var search = $('#txtUpdateSearch').val();
    var experimentKeyword = search == "" ? "listAll" : search;

    var head = document.getElementsByTagName('head');
    var script = document.createElement('script');
    script.type = "text/javascript";
    script.src = masterAtlasURL + "/api"
            +"?experiment="+experimentKeyword
            +"&experimentInfoOnly"
            +"&dateReleaseFrom=" + $('#dateReleaseFrom').val()
            +"&dateReleaseTo=" + $('#dateReleaseTo').val()
            +"&dateLoadFrom=" + $('#dateLoadFrom').val()
            +"&dateLoadTo=" + $('#dateLoadTo').val()
            +"&format=json&callback=downloadAllUpdatesCallback";
    head[0].appendChild(script);
}

function downloadAllUpdatesCallback(data){
    switchToQueue();
}

function redrawCurrentState() {
    for(var timeout in $time) {
        clearTimeout($time[timeout]);
        delete $time[timeout];
    }
    
    if(currentState['exp-s'] != null)
        $('#experimentSearch').val(currentState['exp-s']);
    if(currentState['exp-df'] != null)
        $('#dateFrom').val(currentState['exp-df']);
    if(currentState['exp-dt'] != null)
        $('#dateTo').val(currentState['exp-dt']);
    if(currentState['exp-io'] != null)
        $('#incompleteOnly').val(currentState['exp-io']);
    if(currentState['ad-s'] != null)
        $('#adSearch').val(currentState['ad-s']);
    if(currentState['tlog-ar'] != null) {
        if(currentState['tlog-ar'] > 0)
            $('#taskLogRefresh').attr('checked','checked');
        else
            $('#taskLogRefresh').removeAttr('checked');
    }
    
    if(currentState['tab'] == $tab.exp) {
        $('#tabs').tabs('select', $tab.exp);
        updateBrowseExperiments();
    } else if(currentState['tab'] == $tab.que) {
        $('#tabs').tabs('select', $tab.que);
        updateQueueAndLog();
    } else if(currentState['tab'] == $tab.prop) {
        updateProperties();
        $('#tabs').tabs('select', $tab.prop);
    } else if(currentState['tab'] == $tab.ad) {
        updateArrayDesigns();
        $('#tabs').tabs('select', $tab.ad);
    } else if(currentState['tab'] == $tab.asys) {
        adminCall('aboutsys',{}, function (r) {
            $('#aboutSystem').autoRender(r);
        });
        $('#tabs').tabs('select', $tab.asys);
    } else if(currentState['tab'] == $tab.up) {
        updateAvailableUpdates();
        $('#tabs').tabs('select', $tab.up);
    } else {
        $('#tabs').tabs('select', $tab.que);
        updateQueue();
        updateTaskLog();
    }
}

function storeExperimentsFormState() {
    currentState['exp-s'] = $('#experimentSearch').val();
    currentState['exp-df'] = $('#dateFrom').val();
    currentState['exp-dt'] = $('#dateTo').val();
    currentState['exp-io'] = $('#incompleteOnly').val();
    storeState();
}

function renderTpl(name, data) {
    $('#'+name).render(data, $tpl[name]);
}

$.fn.appendClassTpl = function (data, name) {
    var n = $('<div/>');
    this.append(n);
    n.render(data, $tpl[name]);
    n.replaceWith(n.children());
    return this;
};

function compileTemplates() {
    function compileTpl(name, func) {
        $tpl[name] = $('#' + name).compile(func);
        $('#' + name).empty();
    }

    function compileClassTpl(name, func) {
        $tpl[name] = $('.' + name).compile(func);
        $('.' + name).remove();
    }

    compileTpl('experimentList', {
        '.exprow': {
            'experiment <- experiments' : {
                'label.accession': 'experiment.accession',
                '.analytics': function (r) {
                    var t = '';
                    if(!r.item.curated)
                        t += ' NotCurated';
                    if(r.item.private)
                        t += ' Private';
                    if(!r.item.netcdf)
                        t += ' NoNetCDF';
                    if(!r.item.analytics)
                        t += ' NoAnalytics';
                    if(!r.item.index)
                        t += ' NoIndex';
                    if(t == '')
                        t = 'Complete';
                    return t;
                },
                '.description': 'experiment.description',
                '.numassays' : 'experiment.numassays',
                '.loaddate': 'experiment.loadDate', 
                '.selector@checked': function (r) { return selectedExperiments[r.item.accession] || selectAll ? 'checked' : ''; },
                '.selector@disabled': function () { return selectAll ? 'disabled' : ''; },
                '.selector@value': 'experiment.accession',
                '.selector@id+': 'experiment.accession',
                'label@for+': 'experiment.accession'
            }
        },
        '.expall@style': function (r) { return r.context.experiments.length ? '' : 'display:none'; },
        '.none@style': function (r) { return r.context.experiments.length ? 'display:none' : ''; },

        '.expcoll@style': function (r) { return r.context.numTotal > $options.experimentPageSize ? '' : 'display:none'; },
        '#selectCollapsed@style': function () { return selectAll ? '' : 'visibility:hidden'; },
        '.total': function (r) { return r.context.numTotal; },

        '.rebuildIndex .label': function (r) { return r.context.indexStatus ? 'Index build is complete' : 'Index build is incomplete'; },
        '.rebuildIndex .label@class+': function (r) { return ' ' + r.context.indexStatus; } 
    });

    compileTpl('taskList', {
        'thead@style': function(r) { return r.context.tasks.length ? '' : 'display:none'; },
        'tr.task': {
            'task <- tasks': {
                '.state': msgMapper('state', 'taskState'),
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'task.accession',
                '.user': 'task.user',
                '.runMode': msgMapper('runMode', 'runMode'),
                '.progress': 'task.progress',
                '.elapsed': 'task.elapsed',
                'input@class+': 'task.id',
                '.@class+': ' state#{task.state} mode#{task.runMode} type#{task.type}'
            }
        }
    });

    compileTpl('taskLogItems', {
        'tr' : {
            'litem <- items': {
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'litem.accession',
                '.event': msgMapper('event', 'event'),
                '.runMode': msgMapper('runMode', 'runMode'),
                '.runMode@style': function (r) { return r.item.event == 'SCHEDULED' || r.item.event == 'STARTED' ? '' : 'display:none'; },
                '.message': 'litem.message',
                '.user': 'litem.user',
                '.time': 'litem.time',
                '.@class+': function (r) {
                    return (lastLogTimestamp && r.item.timestamp > lastLogTimestamp ? ' newitem' : '')
                            + ' event' + r.item.event + ' type' + r.item.type; 
                }
            },
            sort:function(a, b){
                return a.timestamp < b.timestamp ? 1 : -1;
            }
        }
    });

    compileTpl('propList', {
        'tr.property' : {
            'property <- properties': {
                '.name': 'property.name',
                '.value': 'property.value'
            }
        }
    });

    compileTpl('adList', {
        '.none@style': function (r) { return r.context.arraydesigns.length ? 'display:none' : ''; },
        '.pager@style': function (r) { return r.context.numTotal > $options.arraydesignPageSize ? '' : 'display:none'; },
        'thead@style': function(r) { return r.context.arraydesigns.length ? '' : 'display:none'; },
        'tbody tr' : {
            'ad <- arraydesigns': {
                '.accession': 'ad.accession',
                '.provider': 'ad.provider',
                '.description': 'ad.description'
            }
        },
        '.total' : function (r) { return r.context.arraydesigns.length; }
    });

    compileClassTpl('expTaskLog', {
        '.none@style': function (r) { return r.context.items.length ? 'display:none' : ''; },
        'thead@style': function(r) { return r.context.items.length ? '' : 'display:none'; },
        'tbody tr' : {
            'litem <- items': {
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'litem.accession',
                '.event': msgMapper('event', 'event'),
                '.runMode@style': function (r) { return r.item.event == 'SCHEDULED' || r.item.event == 'STARTED' ? '' : 'display:none'; },
                '.runMode': msgMapper('runMode', 'runMode'),
                '.message': 'litem.message',
                '.user': 'litem.user',
                '.time': 'litem.time',
                '.@class+': ' event#{litem.event} type#{litem.type}'
            }
        }
    });

    compileTpl('updatesList', {
        'tr.experiment' : {
            'experiment <- results': {
                '.name': 'experiment.experimentInfo.accession',
                '.status': 'experiment.experimentInfo.status',
                '.value': function(r){
                    return masterAtlasURL + r.item.experimentInfo.archiveUrl;
                },
                '.sel .selector@value': function(r){
                    return masterAtlasURL + r.item.experimentInfo.archiveUrl;
                }
                ,'.loaddate': 'experiment.experimentInfo.loaddate'
                ,'.releasedate': 'experiment.experimentInfo.releasedate'
            }
        }
    });
}

function setReleaseDateFrom(){
    //set ReleaseDateFrom to max local date
    var lastKnownReleaseDate = null;
    adminCall('maxreleasedate', {}, function (result) {
        lastKnownReleaseDate = result;
    },true);

    if(null != lastKnownReleaseDate){
            $('#dateReleaseFrom').val(lastKnownReleaseDate);
    }
}

$(document).ready(function () {

    compileTemplates();
    
    $('#tabs li a').each(function (i, a) {
        $tab[$(a).attr('href').substr(5)] = i;
    });

    for(var k in $options)
        $('.option-'+k).text($options[k]);

    $('#tabs').tabs({
        show: function(event, ui) {
            if(currentState['tab'] != ui.index) {
                currentState['tab'] = ui.index;
                storeState();
                redrawCurrentState();
            }
        },
        selected: '-1'
    });

    $('#dateFrom,#dateTo').datepicker({
        dateFormat: 'dd/mm/yy',
        onSelect: function() {
            storeExperimentsFormState();
            updateBrowseExperiments();
        }
    });

    $('#experimentBrowseForm').bind('submit', function() {
        storeExperimentsFormState();
        updateBrowseExperiments();
        return false;
    });

    $('#incompleteOnly').bind('change', function () {
        storeExperimentsFormState();
        updateBrowseExperiments();
    });

    $('#adBrowseForm').submit(function () {
        currentState['ad-s'] = $('#adSearch').val();
        storeState();
        updateArrayDesigns();
        return false;
    });

    $('#dateReleaseFrom,#dateReleaseTo,#dateLoadFrom,#dateLoadTo').datepicker({
        dateFormat: 'dd/mm/yy',
        onSelect: function() {
            updateAvailableUpdates();
        }
    });

    $('#updatesBrowseForm').bind('submit', function() {
        //storeExperimentsFormState();
        updateAvailableUpdates();
        return false;
    });

    $('#loadButton').click(function () {
        var url = $('#loadUrl').val().replace(/^\s+/,'').replace(/\s+$/,'').split(/\s+/);
        var type = $('#loadType').val();
        var autoDep = $('#loadAutodep').is(':checked');
        var useRawData = $('#useRawData').is(':checked');
        var private = $('#private').is(':checked');
        var curated = $('#curated').is(':checked');
        if(url.length == 0 || (url.length == 1 && url[0] == ""))
            return;
        
        if(type == 'auto') {
            var experiments = [];
            var arraydesigns = [];
            for(var k in url) {
                (url[k].toLowerCase().replace(/^.*\//, '').indexOf(".adf") >= 0 ? arraydesigns : experiments).push(url[k]);
            }

            if(experiments.length)
                adminCall('schedule', {
                    runMode: 'RESTART',
                    accession: experiments,
                    type: 'loadexperiment',
                    autoDepends: autoDep,
                    useRawData: useRawData,
                    private: private,
                    curated: curated
                }, updateQueueAndLog);

            if(arraydesigns.length)
                adminCall('schedule', {
                    runMode: 'RESTART',
                    accession: arraydesigns,
                    type: 'loadarraydesign',
                    autoDepends: autoDep
                }, updateQueueAndLog);
        } else
            adminCall('schedule', {
                runMode: 'RESTART',
                accession: url,
                type: 'load' + type,
                autoDepends: autoDep
            }, updateQueueAndLog);
    });

    $('#loadExpand').click(function () {
        var loadf = $('#loadUrl');
        var loadv = loadf.val();
        if(loadf.is('textarea')) {
            $(this).text('more files');
            loadf.replaceWith($('<input type="text" id="loadUrl" class="value">').val(loadv.replace(/\s+(.|\n)*/, '')));
        } else {
            $(this).text('less files');
            loadf.replaceWith($('<textarea id="loadUrl" class="value">').val(loadv));
        }
        return false;
    });

    $('#loadArrayDesignButton').click(function () {
        var url = $('#loadArrayDesignUrl').val();
        var autoDep = $('#loadArrayDesignAutodep').is(':checked');
        adminCall('schedule', {
            runMode: 'RESTART',
            accession: url,
            type: 'loadarraydesign',
            autoDepends: autoDep
        }, updateQueueAndLog);
    });

    $('#cancelPropsButton').click(updateProperties);
    $('#savePropsButton').click(saveProperties);
        
    $('#logout a').click(function () {
        adminCall('logout', {}, function () {
            requireLogin(null, {}, null);
        });
        return false;
    });

    $('#taskLogRefresh').click(function () {
        if($(this).is(':checked')) {
            updateTaskLog();
        } else {
            if($time.tasklog) {
                clearTimeout($time.tasklog);
                $time.tasklog = null;
            }
        }
        currentState['tlog-ar'] = $(this).is(':checked') ? 1 : 0;
        storeState();
        return true;
    });

    $('#taskLogAccessionFilter').bind('keydown', function (e) {
        if(e.keyCode == 13) {
            currentState['tlog-af'] = $(this).val();
            lastLogTimestamp = null;
            storeState();
            updateTaskLog();
            return false;
        }
    });

    function bindFilter(id, stvar) {
        $(id).bind('change', function () {
            currentState[stvar] = $(this).val();
            lastLogTimestamp = null;
            storeState();
            updateTaskLog();
        });
    }

    bindFilter('#taskLogEventFilter', 'tlog-ef');
    bindFilter('#taskLogUserFilter', 'tlog-uf');
    bindFilter('#taskLogTypeFilter', 'tlog-tf');

    updatePauseButton(false);
    restoreState();

    setInterval(function () {
        if(document.location.href.indexOf('#') && currentStateHash && currentStateHash != document.location.href.substring(document.location.href.indexOf('#') + 1)) {
            restoreState();
        }
    }, 500);

    setReleaseDateFrom();
});
