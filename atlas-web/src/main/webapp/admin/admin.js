// javascript required by admin page

function handleKeyPress(e, form) {
  var key = e.keyCode || e.which;
  if (key == 13) {
    form.submit();
  }
}