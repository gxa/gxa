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

function checkLoadDetails(accession, objectType) {
    // first create the row to make sure we've got the correct dom structure, and show it's updating
    createUpdatingRow(accession, objectType);

    // now trigger the update to write back the correct details
    progressupdater =
    Fluxion.doAjax('loadDetailsExporter', 'getLoadDetails', {'accession':accession}, {'ajaxType':'periodical', 'doOnSuccess':updateLoadDetails});
}

function changePage(newPage) {
  var target;
  var url;

  target = document.getElementById("compute.table");
  if (target == null) {
    target = parent.document.getElementById("compute.table");
  }

  url = "computer.jsp";

  ajax(url, target);
}

// sends a get ajax request for the contents of the target element
function ajax(url, target) {
  var req;
  if (window.XMLHttpRequest) {
    req = new XMLHttpRequest();
    req.open("GET", url);
    req.onreadystatechange = function() {
      ajaxDone(req, url, target);
    };
    req.send(null);
  }
  else if (window.ActiveXObject) {
    req = new ActiveXObject();
    if (req) {
      req.open("GET", url);
      req.onreadystatechange = function() {
        ajaxDone(req, url, target);
      };
      req.send(null);
    }
  }
}

var updateLoadDetails = function(json) {
    // this is the doOnSuccess method for AJAX responses for LoadDetails
    var done = false;

    alert("Updating load details for " + json.accession);
    if (json.failedLoad = "true") {
        // load failed, write appropriate element
        done = true;
        writeFailedLoadRow(json.accession, json.loadType);
    }
    else {
        // check netcdf, analytics, index to see if any are still working
        var netcdf = json.netcdf;
        if (netcdf == "working") {
            writeWorkingElement(json.accession, json.loadType, "netcdf");
        }
        else {
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

    if (done) {
        progressupdater.stop();
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

    $(elementID).innerHTML =
    "<img src=\"../images/ajax-loader.gif\" alt=\"checking status with database...\"/>";
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

    $(elementID).innerHTML =
    "<img src=\"../images/green-tick.png\" alt=\"done\" align=\"left\">" +
    "<form id=\"" + formID + "\"" +
    "action=\"" + formAction + "\"" +
    "method=\"post\"" +
    "enctype=\"application/x-www-form-urlencoded\">" +
    "<input type=\"hidden\" name=\"type\" value=\"<%=details.getLoadType()%>\"/>" +
    "<input type=\"hidden\" name=\"accession\" value=\"" + accession + "\"/>" +
    "<input type=\"button\" value=\"" + buttonName + "\" onclick=\"this.form.submit();\"/>" +
    "</form>";
};

var writePendingElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var formID = elementID + "_form";
    var formAction;
    var buttonName;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        $(elementID).innerHTML =
        "<img src=\"../images/red-cross.png\" alt=\"pending\" align=\"left\" />"
    }
    else if (elementType == "netcdf" || elementType == "analytics") {
        formAction = "do" + elementType + ".web";
        formID = elementID + "_form";
        buttonName = "generate";

        $(elementID).innerHTML =
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
};

var writeFailedElement = function(accession, objectType, elementType) {
    var elementID = extractTableColumnID(accession, objectType, elementType);
    var formID = elementID + "_form";
    var formAction;
    var buttonName;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        $(elementID).innerHTML =
        "<img src=\"../images/red-cross.png\" alt=\"pending\" align=\"left\" />"
    }
    else if (elementType == "netcdf" || elementType == "analytics") {
        formAction = "do" + elementType + ".web";
        formID = elementID + "_form";
        buttonName = "retry";

        $(elementID).innerHTML =
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
}

var writeFailedLoadRow = function(accession, objectType) {
    // check the row first of all - might need to insert new <td> elements
    var rowID = extractTableRowID(accession, objectType);

    $(rowID).innerHTML =
    "<td id=\"" + objectType + "_" + accession + "_accession\">" + accession + "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_netcdf\" align=\"left\">" +
    "<img src=\"../images/dialog-warning.png\" alt=\"Failed load!\"/>This load failed, please reload!" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_analytics\" align=\"left\">" +
    "</td>" +
    "<td id=\"" + objectType + "_" + accession + "_index\" align=\"left\">" +
    "</td>";
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