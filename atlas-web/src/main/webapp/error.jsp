<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("service", ArrayExpressSearchService.instance());
%>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="atlas.css" type="text/css" />
    <link rel="stylesheet" href="structured-query.css" type="text/css" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="scripts/jquery.min.js"></script>
    <script type="text/javascript" src="scripts/jquery.cookie.js"></script>
    <script type="text/javascript" src="scripts/jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="scripts/jquery.autocomplete.js"></script>
    <script type="text/javascript" src="scripts/jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="scripts/common-query.js"></script>
    <script type="text/javascript" src="scripts/feedback.js"></script>

    <script type="text/javascript">
        function toggleAtlasHelp(e) {
            if($("div.atlasHelp").is(":hidden")) {
                showAtlasHelp();
            } else {
                hideAtlasHelp();
            }
            if(e && typeof(e.stopPropagation) == 'function')
                e.stopPropagation();
            return false;
        }

        function showAtlasHelp() {
            if($("div.atlasHelp").is(":hidden")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("hide help");
            }
            $.cookie('atlas_help_state','shown');
        }

        function hideAtlasHelp() {
            if($("div.atlasHelp").is(":visible")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("show help");
            }
            $.cookie('atlas_help_state','hidden');
        }

        $(document).ready(function()
            {
                atlas.initSimpleForm();

                $("#atlasHelpToggle").click(toggleAtlasHelp);


                if (($.cookie('atlas_help_state') == "shown") && ($("div.atlasHelp").is(":hidden"))) {
                   showAtlasHelp();
                } else if (($.cookie('atlas_help_state') == "hidden") && ($("div.atlasHelp").is(":visible"))) {
                   hideAtlasHelp();
                }
            }
        );
    </script>

    <style type="text/css">
        .label {
            font-size: 10px;
        }

        .atlasHelp {
            display: none;
            text-align:center;
        }

        .atlasHelp .div1 {
            font-size: 0px; line-height: 0%; width: 0px;
            border-bottom: 20px solid #EEF5F5;
            border-left: 10px solid white;border-right: 10px solid white;
        }

        .atlasHelp .div2 {
            background-color: #EEF5F5; text-align:left; height:100%; width: 140px;padding:5px;
        }

        #newexplist {
            font-size: 10px;
            text-align:left;margin-top:70px;margin-right:auto;margin-left:auto;width:740px;
        }

        #newexplist table {
            border: none;
            padding: 0;
            margin: 10px 0 0 0;
        }

        #newexplist td {
            padding: 5px 10px 0 0;
            vertical-align:top;
            font-size: 10px;
        }
    </style>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals=" />
<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>

<div id="ae_pagecontainer">
    <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;" >
        <jsp:include page="simpleform.jsp"></jsp:include>
    </div>
</div>

<div align="center" style="color:red;font-weight:bold;margin-top:150px">
    <c:choose>
        <c:when test="${!empty errorMessage}">
            <c:out value="${errorMessage}" />
        </c:when>
        <c:otherwise>
            We're sorry an error has occurred! We will try to remedy this as soon as possible. Responsible parties have been notified and heads will roll.
        </c:otherwise>
    </c:choose>
    <br/><br/><br/>Please try another search.
</div>


        <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
            <div style="position: absolute; bottom:80px; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center">
                            For news and updates, subscribe to the <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
                            <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
                            <input type="submit" name="email-button" value="Subscribe" />
            </div>
        </form>

<jsp:include page="end_body.jsp"></jsp:include>
