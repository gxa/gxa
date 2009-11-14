<%@ taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core" %>


<head></head>
<body>
<div id="load_fail" class="generic_box">
  <table align="center" border="0" class="generic_table">
    <tr>
      <td align="center">
        Your load request failed, because
        <jstl:set var="message" value="${message}"/>${message}
      </td>
    </tr>
    <tr>
      <td align="center">
        <a href="admin.jsp">>> Return to admin page</a>
      </td>
    </tr>
  </table>
</div>
</body>