<%@page contentType="text/plain;encoding=UTF-8" %><%
if(!"".equals(request.getParameter("factor")))
{                                                       
 java.util.List<String> ac = ae3.service.ArrayExpressSearchService.instance().autoCompleteFactorValues(request.getParameter("factor"), request.getParameter("q"), request.getParameter("limit"));
 if (ac != null) {
    for(String s : ac) {
        response.getWriter().println(s);
    }
 } else {
    System.err.println("Null");
 }
}                                                      
%>