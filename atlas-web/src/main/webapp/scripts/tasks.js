var currentState = {};
var atlas = { homeUrl: '' };
var searchTimeout;
var selectedExperiments = {};
var $t = {};

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
    if(stateString != urlParts[1])
        window.location.href = urlParts[0] + '#' + stateString;
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

    currentState = newState;
    redrawCurrentState();
}

function taskmanCall(op, params, func) {
    $('.loadIndicator').show();
    return $.ajax({
        type: "GET",
        url: atlas.homeUrl + "tasks",
        dataType: "json",
        data: $.extend(params, { op : op }),
        success: function (json) {
            $('.loadIndicator').hide();
            if(json.error)
                alert(json.error);
            else
                func(json);
        },
        error: function() {
            $('.loadIndicator').hide();
            alert('AJAX error for ' + op + ' ' + params);
        }});
}

function updateBrowseExperiments() {
    taskmanCall('searchexp', {
        search: $('#experimentSearch').val(),
        fromDate: $('#dateFrom').val(),
        toDate: $('#dateTo').val(),
        pendingOnly: $('#incompleteOnly').is(':checked') ? 1 : 0
    }, function (result) {
        $('#experimentList').render(result, $t.experimentList);
        $('#experimentList tr input.selector').click(function () {
            if($(this).is(':checked'))
                selectedExperiments[this.value] = 1;
            else
                delete selectedExperiments[this.value];
        });
        var newAccessions = {};
        for(var i = 0; i < result.experiments.length; ++i)
            newAccessions[result.experiments[i].accession] = 1;
        for(i in selectedExperiments)
            if(!newAccessions[i])
                delete selectedExperiments[i];
        $('#selectAll').click(function () {
            if($(this).is(':checked')) {
                $('#experimentList tr input.selector').attr('disabled', 'disabled').attr('checked','checked');
                for(var i = 0; i < result.experiments.length; ++i)
                    selectedExperiments[result.experiments[i].accession] = 1;
            } else {
                $('#experimentList tr input.selector').removeAttr('disabled').removeAttr('checked');
                selectedExperiments.length = 0;
            }
        });
    });
}

function updateQueue() {
    taskmanCall('tasklist', {}, function (result) {
        $('#taskList').render(result, $t.taskList);
    });
}

function cancelTask(id) {
    alert('cancel task ' + id);
}

function redrawCurrentState() {
    if(currentState['exp-s'] != null)
        $('#experimentSearch').val(currentState['exp-s']);
    if(currentState['exp-df'] != null)
        $('#dateFrom').val(currentState['exp-df']);
    if(currentState['exp-dt'] != null)
        $('#dateTo').val(currentState['exp-dt']);
    if(currentState['exp-io'] != null)
        $('#incompleteOnly').attr('checked', currentState['exp-io'] == 1);


    if(currentState['tab'] == 0) {
        $('#tabs').tabs('select', 0);
        updateBrowseExperiments();
    } else if(currentState['tab'] == 1) {
        $('#tabs').tabs('select', 1);
        updateQueue();
    } else if(currentState['tab'] == 2) {
        $('#tabs').tabs('select', 2);

    } else {
        $('#tabs').tabs('select', 0);
        $('#experimentSearch').val('');
        updateBrowseExperiments();
    }
}

function storeExperimentsFormState() {
    currentState['exp-s'] = $('#experimentSearch').val(); 
    currentState['exp-df'] = $('#dateFrom').val();
    currentState['exp-dt'] = $('#dateTo').val();
    currentState['exp-io'] = $('#incompleteOnly').is(':checked') ? 1 : 0;
    storeState();
}

function compileTemplates() {
    $t.experimentList = $('#experimentList').compile({
        '.exprow': {
            'experiment <- experiments' : {
                '.accession': 'experiment.accession',
                '.stage': 'experiment.stage',
                '.selector@checked': function (r) { return selectedExperiments[r.item.accession] ? 'checked' : ''; },
                '.selector@value': 'experiment.accession'
            }
        },
        '.expall@style': function (r) { return r.context.experiments.length ? '' : 'display:none'; },
        '.expnone@style': function (r) { return r.context.experiments.length ? 'display:none' : ''; }
    });

    $t.taskList = $('#taskList').compile({
        'tr': {
            'task <- tasks': {
                '.state': 'task.state',
                '.type': 'task.type',
                '.accession': 'task.accession',
                '.stage': 'task.stage',
                '.runMode': 'task.runMode',
                'input@onclick': 'cancelTask(#{task.id})' 
            }
        }
    });
}

$(document).ready(function () {

    compileTemplates();

    $('#tabs').tabs({show: function(event, ui) {
        if(currentState['tab'] != ui.index) {
            currentState['tab'] = ui.index;
            storeState();
            redrawCurrentState();
        }
    }, selected: '-1'});

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

    $('#incompleteOnly').bind('click', function () {
        storeExperimentsFormState();
        updateBrowseExperiments();
    });

    $('#experimentSearch').bind('keydown', function (event) {
        var keycode = event.keyCode;
        if(keycode == 13) {
            clearTimeout(searchTimeout);
            storeExperimentsFormState();
            updateBrowseExperiments();
        } else if(keycode == 8 || keycode == 46 ||
                  (keycode >= 48 && keycode <= 90) ||      // 0-1a-z
                  (keycode >= 96 && keycode <= 111) ||     // numpad 0-9 + - / * .
                  (keycode >= 186 && keycode <= 192) ||    // ; = , - . / ^
                  (keycode >= 219 && keycode <= 222)) {

            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(function () {
                storeExperimentsFormState();
                updateBrowseExperiments();
            }, 500);
        }
    });
    
    restoreState();
});
