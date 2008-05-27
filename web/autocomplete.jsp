<%@page contentType="text/plain;encoding=UTF-8" %><%
java.util.List<String> ac = ae3.service.ArrayExpressSearchService.instance().autoComplete(request.getParameter("q"), request.getParameter("type"));
if (ac != null) {
    for(String s : ac) {
        response.getWriter().println(s);
    }
} else {
    System.err.println("Null");
}
%>