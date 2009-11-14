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