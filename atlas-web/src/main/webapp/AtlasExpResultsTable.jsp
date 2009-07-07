<%
    String GeneId = request.getParameter("GeneId");
%>


<table align="left">
                            <tr>
                                <td id="expHeader_td" class="sectionHeader" style="vertical-align: top">Expression Profiles</td>
                                <td align="right">
                                    <div id="Pagination" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; "></div>
                                    <div id="Pagination1" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; ">
                                    </div>
                                </td>
                            </tr>

                            <tr>
                                <td align="left"  valign="top" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                                    <div id="pagingSummary" class="header"></div>
                                </td>
                                <td align="right" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                                    <div id="expSelect"></div>
                                </td>

                            </tr>
                            <tr>
                                <td colspan="2">
                                    <div id="ExperimentResult">
                                    <jsp:include page="AtlasExpResults.jsp">
                                        <jsp:param name="gid" value="<%=GeneId %>" />
                                        <jsp:param name="from" value="1" />
                                        <jsp:param name="to" value="5" />
                                    </jsp:include>
                                    </div>
                                </td>
                            </tr>
                        </table>