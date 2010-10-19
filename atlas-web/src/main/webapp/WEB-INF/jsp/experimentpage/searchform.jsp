<%--
  Created by IntelliJ IDEA.
  User: Andrey
  Date: Oct 7, 2010
  Time: 4:45:46 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head><title>Simple jsp page</title>
      <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>

      <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
  </head>
  <body>

  <script type="text/javascript">
      function initSearchForm(){
            var searchfield = $('input[name=searchQuery]');
            var form = searchfield.parents('form:first');
            form.unbind('submit');

            form.bind('submit', function () {
                var q =  $("#searchForm").serialize();

                disableSearchControls();

                startSearch(q,0);

                return false;
            });
      }

      $(document).ready(function() {
           initSearchForm();
      });

      function startSearch(q,p){
          $("#qryHeader").html("<img src='/images/indicator.gif' />&nbsp;Loading...");
          $("#qryHeader").show();
          $("#qryResult").load("SlowPageServlet?" + q +"&callbackFormat=pagerClick&clearAllFormat=&p=" + p, function() {
              resetSearchControls();
          });
      };

      function resetSearchControls(){
          var searchfield = $('input[name=searchQuery]');

          searchfield.removeAttr("disabled");
          var form = searchfield.parents('form:first');
          var v = $(form).find('input[type=submit]');
          v.val('Submit Query');

          $("#qryHeader").hide();

          initSearchForm();
      };

      function disableSearchControls(){
          var searchfield = $('input[name=searchQuery]');
          var form = searchfield.parents('form:first');

          searchfield.attr("disabled",true);

          var v = $(form).find('input[type=submit]');
          v.val('Cancel ...');

          form.unbind('submit');
          form.bind('submit',function(){
              cancelSearch();
              resetSearchControls()
          });
      }

      function cancelSearch(){
          alert("cancel search");
      };

      function stopSearch(){

      }

      function pagerClick(p){
          var q =  $("#searchForm").serialize();
          disableSearchControls();
          startSearch(q,p);
      }


  </script>

  <H1>SEARCH !</H1>

  <a href="" onclick="javascript:resetSearchControls(); return false;">init</a>

  <form id="searchForm" action="javascript:void()">

  <input type="text" id="searchQuery" name="searchQuery"/>
  <input type="submit">

  </form>

  <div id="qryHeader" style="padding-top: 10px;"></div>
  <div id="qryResult" style="padding-top: 10px;"></div>

  </body>
</html>