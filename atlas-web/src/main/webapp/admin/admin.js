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

function checkLoadDetails(accession) {
    // kick off periodical updates for these load details
    progressupdater = new Ajax.PeriodicalUpdater('trash', url, {
        asynchronous:true,
        onSuccess:function(request) {
            var json = eval("(" + request.responseText + ")");
            var done = false;

            if (json.cancelled)
            {
                done = true;
                log.info(json.message + " - Reasoner process cancelled");
                $('statusinfo').innerHTML += json.message + "<br/><b>Reasoner process cancelled</b>";
            }
            else
            {
                if (json.finished)
                {
                    done = true;
                    log.info(json.message + " - Reasoner process complete");
                    $('statusinfo').innerHTML += json.message + "<br/><b>Reasoner process complete</b>";
                }
                else
                {
                    done = false;

                    if (json.message == "")
                    {
                        $('statusinfo').innerHTML = "<i>Initialising...</i><br/>";
                    }
                    else
                    {
                        log.info(json.message);
                        $('statusinfo').innerHTML = json.message + "<br/>";
                    }
                }
            }

            if (done)
            {
                progressupdater.stop();
            }
        },
        frequency:1,
        method: 'post',
        parameters: {action: 'checkReasonerStatus'},
        onFailure: reportError
    });
}

function writeWorkingElement() {

}

function writeDoneElement(accession, elementType) {
    var elementID;
    var formID;
    var formAction;
    var buttonName;

    // element type one of index, netcdf, analytics
    if (elementType == "index") {
        elementID = "expt_" + accession + "_index";
        formAction = "dereschedule.web";
    }
    else if (elementType == "netcdf") {
        elementID = "expt_" + accession + "_index";
        formAction = "donetcdf.web";
    }
    else if (elementType == "analytics") {
            elementID = "expt_" + accession + "_index";
            formAction = "doanalytics.web";
        }
        else {
            alert("Unrecoginsed element type: " + elementType + " not known!");
            return;
        }

    formID = elementID + "_form";
    buttonName = "regenerate";

    $(elementID).innerHTML = "<td>" +
                             "<img src=\"../images/green-tick.png\" alt=\"done\" align=\"left\">" +
                             "</td>" +
                             "<td>" +
                             "<form id=\"" + formID + "\"" +
                             "action=\"" + formAction + "\"" +
                             "method=\"post\"" +
                             "enctype=\"application/x-www-form-urlencoded\">" +
                             "<input type=\"hidden\" name=\"type\" value=\"<%=details.getLoadType()%>\"/>" +
                             "<input type=\"hidden\" name=\"accession\" value=\"" + accession + "\"/>" +
                             "<input type=\"button\" value=\"" + buttonName + "\" onclick=\"this.form.submit();\"/>" +
                             "</form>" +
                             "</td>";
}

function writePendingElement() {

}

// table row html
//<tr id="expt_<%=expt.getAccession()%>">
//    <td id="expt_<%=expt.getAccession()%>_accession"><%=expt.getAccession()%></td>
//    <td id="expt_<%=expt.getAccession()%>_index" colspan="2"> - if working, whirly, else if done green tick form, else red cross form </td>
//    <td id="expt_<%=expt.getAccession()%>_netcdf" colspan="2"> - if working, whirly, else if done green tick form, else red cross form </td>
//    <td id="expt_<%=expt.getAccession()%>_analytics" colspan="2"> - if working, whirly, else if done green tick form, else red cross form </td>
//</tr>

// green tick columns
//    <td>
//        <img src="../images/green-tick.png" alt="done" align="left">
//    </td>
//    <td>
//        <!-- form to regenerate NetCDF -->
//        <form id="{my.form.id}"
//        name="{action.name}"
//        action="{post.request}"
//        method="post"
//        enctype="application/x-www-form-urlencoded">
//
//            <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
//            <input type="hidden" name="accession" value="<%=accession%>"/>
//            <input type="button" value="{button.name}" onclick="this.form.submit();"/>
//        </form>
//    </td>

// red cross columns
//    <td>
//        <img src="../images/red-cross.png" alt="pending" align="left">
//    </td>
//    <td>
//        Pending indexing
//    </td>

