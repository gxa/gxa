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

// javascript required by admin page

function handleKeyPress(e, form) {
    var key = e.keyCode || e.which;
    if (key == 13) {
        form.submit();
    }
}

/**
 * Build the index for experiments and/or genes, depending on how the IndexBuilder service is configured in the backend.
 * When invoking this method, pendingOnly flags whether to include all or pending objects only.
 *
 * @param pendingOnly true if only pending experiments/genes should be included, false otherwise
 */
function buildIndex(pendingOnly, form) {
    form.pending.value = pendingOnly;
    form.submit();
}

function updateComputeTable(pageNumber, experimentsPerPage, maxExperiments) {
    Fluxion.doAjax('experimentAccessionsExporter', 'getExperimentAccessions', {'pageNumber':pageNumber, 'experimentsPerPage':experimentsPerPage}, {'doOnSuccess':renderComputeTable});

    renderPageSwitcher(pageNumber, experimentsPerPage, maxExperiments);
}

var renderComputeTable = function(json) {
    var content =
            "<h2>Experiment Recompute Tasks</h2>" +
            "<div id=\"experiments\">" +
            "<table width=\"100%\">" +
            "<tr id=\"headers\">" +
            "<td width=\"20%\">Experiment Accession</td>" +
            "<td>Has NetCDF</td>" +
            "<td>Has Analytics</td>" +
            "<td>In Index</td>" +
            "</tr>";

    var exptArray = eval(json.accessions);
    for (var i = 0; i < exptArray.length; i++) {
        // render content
        if (i % 2 == 0) {
            content = content +
                      "<tr bgcolor=\"#EEF5F5\" id=\"experiment_" + exptArray[i] + "\">";
        }
        else {
            content = content +
                      "<tr bgcolor=\"white\" id=\"experiment_" + exptArray[i] + "\">";
        }
        content = content +
                  "<td id=\"experiment_" + exptArray[i] + "_accession\">" + exptArray[i] + "" +
                  "</td>" +
                  "<td id=\"experiment_" + exptArray[i] + "_netcdf\" align=\"left\">" +
                  "<img src=\"../images/ajax-loader.gif\" alt=\"?\"/>" +
                  "</td>" +
                  "<td id=\"experiment_" + exptArray[i] + "_analytics\" align=\"left\">" +
                  "<img src=\"../images/ajax-loader.gif\" alt=\"?\"/>" +
                  "</td>" +
                  "<td id=\"experiment_" + exptArray[i] + "_index\" align=\"left\">" +
                  "<img src=\"../images/ajax-loader.gif\" alt=\"?\"/>" +
                  "</td>" +
                  "</tr>";
    }

    // render content
    content = content + "</table>" + "</div>";
    $('compute.table').innerHTML = content;

    // finally, trigger load details checking to try and update each table element
    for (var j = 0; j < exptArray.length; j++) {
        checkLoadDetails(exptArray[j], 'experiment');
    }
};

var renderPageSwitcher = function(pageNumber, experimentsPerPage, maxExperiments) {
    var content = "<table><tr>";
    if ((Number(pageNumber) - 3) > 0) {
        content = content +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('1','" + Number(experimentsPerPage) + "','" +
                  Number(maxExperiments) +
                  "');\">" +
                  "&nbsp;|&lt;&nbsp;" +
                  "</a>" +
                  "</td>" +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) - 1) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;&lt;&nbsp;" +
                  "</a>" +
                  "</td>" +
                  "<td>" +
                  "&nbsp;...&nbsp;" +
                  "</td>";
    }

    if ((Number(pageNumber) - 2) > 0) {
        content = content +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) - 2) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;" + (Number(pageNumber) - 2) + "&nbsp;" +
                  "</a>" +
                  "</td>";
    }

    if ((Number(pageNumber) - 1) > 0) {
        content = content +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) - 1) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;" + (Number(pageNumber) - 1) + "&nbsp;" +
                  "</a>" +
                  "</td>";
    }

    content = content + "<td><b>&nbsp;" + Number(pageNumber) + "&nbsp;</b></td>";

    if ((Number(pageNumber) + 1) <= Math.round(Number(maxExperiments) / Number(experimentsPerPage))) {
        content = content +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) + 1) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;" + (Number(pageNumber) + 1) + "&nbsp;" +
                  "</a>" +
                  "</td>";
    }

    if ((Number(pageNumber) + 2) <= Math.round(Number(maxExperiments) / Number(experimentsPerPage))) {
        content = content +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) + 2) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;" + (Number(pageNumber) + 2) + "&nbsp;" +
                  "</a>" +
                  "</td>";
    }

    if ((Number(pageNumber) + 3) <= Math.round(Number(maxExperiments) / Number(experimentsPerPage))) {
        content = content +
                  "<td>" +
                  "&nbsp;...&nbsp;" +
                  "</td>" +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" + (Number(pageNumber) + 1) + "','" +
                  Number(experimentsPerPage) +
                  "','" + Number(maxExperiments) + "');\">" +
                  "&nbsp;&gt;&nbsp;" +
                  "</a>" +
                  "</td>" +
                  "<td>" +
                  "<a href=\"#\" onclick=\"updateComputeTable('" +
                  Math.round(Number(maxExperiments) / Number(experimentsPerPage)) + "','" +
                  Number(experimentsPerPage) + "', '" + Number(maxExperiments) + "');\">" +
                  "&nbsp;&gt;|&nbsp;" +
                  "</a>" +
                  "</td>";
    }

    // update the page.switcher div
    content = content + "</tr></table>";
    $('page.switcher').innerHTML = content;
}

function checkLoadDetails(accession, objectType) {
    // first create the row to make sure we've got the correct dom structure, and show it's updating
    createUpdatingRow(accession, objectType);

    // now trigger the update to write back the correct details
    Fluxion.doAjax('loadDetailsExporter', 'getLoadDetails', {'accession':accession}, {'ajaxType':"periodical", 'doOnSuccess':updateLoadDetails});
}

var updateLoadDetails = function(json) {
    // this is the doOnSuccess method for AJAX responses for LoadDetails
    if (json.failedLoad == "true") {
        // load failed, write appropriate element
        writeFailedLoadRow(json.accession, json.loadType);
    }
    else {
        // check netcdf, analytics, index to see if any are still working
        var netcdf = json.netcdf;
        if (netcdf == "working") {
            writeWorkingElement(json.accession, json.loadType, "netcdf");
        }
        else {
            netcdfFinished = true;
            if (netcdf == "pending") {
                writePendingElement(json.accession, json.loadType, "netcdf");
            }
            else {
                // done or failed?
                if (netcdf == "failed") {
                    writeFailedElement(json.accession, json.loadType, "netcdf");
                }
                else {
                    writeDoneElement(json.accession, json.loadType, "netcdf");
                }
            }
        }

        var analytics = json.analytics;
        if (analytics == "working") {
            writeWorkingElement(json.accession, json.loadType, "analytics");
        }
        else {
            analyticsFinished = true;
            if (analytics == "pending") {
                writePendingElement(json.accession, json.loadType, "analytics");
            }
            else {
                // done or failed?
                if (analytics == "failed") {
                    writeFailedElement(json.accession, json.loadType, "analytics");
                }
                else {
                    writeDoneElement(json.accession, json.loadType, "analytics");
                }
            }
        }

        var index = json.index;
        if (index == "working") {
            writeWorkingElement(json.accession, json.loadType, "index");
        }
        else {
            if (index == "pending") {
                writePendingElement(json.accession, json.loadType, "index");
            }
            else {
                // done or failed?
                if (index == "failed") {
                    writeFailedElement(json.accession, json.loadType, "index");
                }
                else {
                    writeDoneElement(json.accession, json.loadType, "index");
                }
            }
        }
    }
};

var createUpdatingRow = function(accession, objectType) {
    // check the row first of all - might need to insert new <td> elements
    var rowID = extractTableRowID(accession, objectType);

    $(rowID).innerHTML =
    "<td id=\"" + objectType + "_" + accession + "_accession\">" + accession + "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_netcdf\" align=\"left\">" +
    "<img src=\"../images/ajax-loader.gif\" alt=\"checking status with database...\"/>" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_analytics\" align=\"left\">" +
    "<img src=\"../images/ajax-loader.gif\" alt=\"checking status with database...\"/>" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_index\" align=\"left\">" +
    "<img src=\"../images/ajax-loader.gif\" alt=\"checking status with database...\"/>" +
    "</td>";
};

var writeWorkingElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var newHTML = "<img src=\"../images/ajax-loader.gif\" alt=\"checking status with database...\"/>";
    if ($(elementID).innerHTML != newHTML) {
        $(elementID).innerHTML = newHTML;
    }
};

var writeDoneElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var formID = elementID + "_form";
    var formAction;
    var buttonName;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        formAction = "doreschedule.web";
    }
    else if (elementType == "netcdf" || elementType == "analytics") {
        formAction = "do" + elementType + ".web";
    }
    else {
        alert("Unrecognised element type: " + elementType + " not known!");
        return;
    }

    formID = elementID + "_form";
    buttonName = "regenerate";

    var newHTML =
    "<img src=\"../images/green-tick.png\" alt=\"done\" align=\"left\">" +
    "<form id=\"" + formID + "\"" +
    "action=\"" + formAction + "\"" +
    "method=\"post\"" +
    "enctype=\"application/x-www-form-urlencoded\">" +
    "<input type=\"hidden\" name=\"type\" value=\"" + objectType + "\"/>" +
    "<input type=\"hidden\" name=\"accession\" value=\"" + accession + "\"/>" +
    "<input type=\"button\" value=\"" + buttonName + "\" onclick=\"this.form.submit();\"/>" +
    "</form>";
    if ($(elementID).innerHTML != newHTML) {
        $(elementID).innerHTML = newHTML;
    }
};

var writePendingElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var formID = elementID + "_form";
    var formAction;
    var buttonName;

    var newHTML;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        newHTML =
        "<img src=\"../images/red-cross.png\" alt=\"pending\" align=\"left\" />"
    }
    else if (elementType == "netcdf" || elementType == "analytics") {
        formAction = "do" + elementType + ".web";
        formID = elementID + "_form";
        buttonName = "generate";

        newHTML =
        "<img src=\"../images/red-cross.png\" alt=\"pending\" align=\"left\">" +
        "<form id=\"" + formID + "\"" +
        "action=\"" + formAction + "\"" +
        "method=\"post\"" +
        "enctype=\"application/x-www-form-urlencoded\">" +
        "<input type=\"hidden\" name=\"type\" value=\"" + objectType + "\"/>" +
        "<input type=\"hidden\" name=\"accession\" value=\"" + accession + "\"/>" +
        "<input type=\"button\" value=\"" + buttonName +
        "\" onclick=\"this.form.submit();\"/>" +
        "</form>";
    }
    else {
        alert("Unrecognised element type: " + elementType + " not known!");
        return;
    }

    if ($(elementID).innerHTML != newHTML) {
        $(elementID).innerHTML = newHTML;
    }
};

var writeFailedElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var formID = elementID + "_form";
    var formAction;
    var buttonName;

    var newHTML;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        newHTML =
        "<img src=\"../images/red-cross.png\" alt=\"pending\" align=\"left\" />"
    }
    else if (elementType == "netcdf" || elementType == "analytics") {
        formAction = "do" + elementType + ".web";
        formID = elementID + "_form";
        buttonName = "retry";

        newHTML =
        "<img src=\"../images/red-cross.png\" alt=\"failed\" align=\"left\">" +
        "<form id=\"" + formID + "\"" +
        "action=\"" + formAction + "\"" +
        "method=\"post\"" +
        "enctype=\"application/x-www-form-urlencoded\">" +
        "<input type=\"hidden\" name=\"type\" value=\"" + objectType + "\"/>" +
        "<input type=\"hidden\" name=\"accession\" value=\"" + accession + "\"/>" +
        "<input type=\"button\" value=\"" + buttonName +
        "\" onclick=\"this.form.submit();\"/>" +
        "</form>";
    }
    else {
        alert("Unrecognised element type: " + elementType + " not known!");
        return;
    }

    if ($(elementID).innerHTML != newHTML) {
        $(elementID).innerHTML = newHTML;
    }
}

var writeFailedLoadRow = function(accession, objectType) {
    // check the row first of all - might need to insert new <td> elements
    var rowID = extractTableRowID(accession, objectType);

    var newHTML =
    "<td id=\"" + objectType + "_" + accession + "_accession\">" + accession + "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_netcdf\" align=\"left\">" +
    "<img src=\"../images/dialog-warning.png\" alt=\"Failed load!\"/>This load failed, please reload!" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_analytics\" align=\"left\">" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_index\" align=\"left\">" +
    "</td>";

    if ($(rowID).innerHTML != newHTML) {
        $(rowID).innerHTML = newHTML;
    }
};

var extractTableRowID = function(accession, objectType) {
    // element type can be gene or experiment
    if (objectType == "experiment") {
        return "experiment_" + accession;
    }
    else if (objectType == "gene") {
        return "gene_" + accession;
    }
    else {
        alert("Bad object type: " + objectType);
        return null;
    }
};

var extractTableColumnID = function(accession, objectType, elementType) {
    return extractTableRowID(accession, objectType) + "_" + elementType;
};