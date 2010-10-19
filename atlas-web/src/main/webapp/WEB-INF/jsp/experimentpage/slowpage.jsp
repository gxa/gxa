<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>


<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>


<%!
    public static class SearchResultItem{
        private String name;
        public void setName(String name){
            this.name = name;
        }
        public String getName(){
            return name;
        }
    }

    public static class SearchResult{
        private List<SearchResultItem> items;
        private String page;
        public List<SearchResultItem> getItems(){
            return items;
        }
        public void setItems(List<SearchResultItem> items){
            this.items = items;
        }
        public String getPage(){
            return this.page;
        }
        public void setPage(String page){
            this.page = page;
        }
        public String getRowsPerPage(){
            return "10";
        }
        public String getTotal(){
            return "100";
        }

    }

    class Dao{
    public SearchResult getSearchResult(String searchQuery,String pageNumber){
        SearchResult result = new SearchResult();

        List<SearchResultItem> items = new ArrayList<SearchResultItem>();

        Integer start = 0;
        try{
        start = Integer.parseInt(pageNumber);
        }catch(NumberFormatException ex){
            //noop 
        }

        for(int i=start;i!=start+10;i++){
            SearchResultItem ii = new SearchResultItem();

            ii.setName(String.format("%s- %d",searchQuery,i));

            items.add(ii);
        }

        result.setPage(pageNumber);
        result.setItems(items);

        return result;
    }
    }
%>

<%
    Thread.currentThread().sleep(1000);

    request.setAttribute("callbackFormat",request.getParameter("callbackFormat"));
    request.setAttribute("clearAllFormat",request.getParameter("clearAllFormat"));

    //can be many search parameters, specified by this form
    String searchQuery = request.getParameter("searchQuery");
    String strPage = request.getParameter("p");

    //-----------------------------------------------------------//

    request.setAttribute("searchResult",(new Dao()).getSearchResult(searchQuery,strPage));
%>

<script type="text/javascript">

    $(document).ready(function(){
        showPaging();

        $("#expts").tablesorter({});
    });
    

    function showPaging(){
        var opts = {
            current_page: ${searchResult.page},
            num_edge_entries: 2,
            items_per_page: ${searchResult.rowsPerPage},
            link_to: '#', //...&p=__id__
            next_text: '>>',
            prev_text: '<<',
            callback: function(page) {
                ${callbackFormat}(page);
                return false; }
        };
        opts.num_display_entries = 5;
        $(".pagination_div").pagination(${searchResult.total}, opts);
    }
</script>


<div class="pagination_div"></div>

<table id="expts">
<thead>
  <th>
      gene
  </th>
  <th>
      expression
  </th>
</thead>
<tbody>
<c:forEach var="i" items="${searchResult.items}">
  <tr>
    <td>${i.name}</td>
    <td>hello</td>
  </tr>
</c:forEach>
</tbody>
</table>

<script type="text/javascript">
    //document.ready does not triggered when loaded via div.load
    //showPaging();
</script>