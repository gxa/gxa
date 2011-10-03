<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>
 <%@include file="includes/global-inc.jsp" %>

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="errorPageHead"/>

    <c:import url="/WEB-INF/jsp/includes/global-inc-head.jsp"/>
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs" />
    <wro4j:all name="bundle-gxa"/>
    <wro4j:all name="bundle-gxa-searchform-support"/>
    <wro4j:all name="bundle-gxa-page-index"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

    <div class="ae_pagecontainer">
        <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;">
             <jsp:include page="/WEB-INF/jsp/includes/atlas-header.jsp">
                <jsp:param name="isHomePage" value="true"/>
            </jsp:include>
            <jsp:include page="/WEB-INF/jsp/includes/atlas-searchform.jsp">
                <jsp:param name="isAdvanced" value="false"/>
            </jsp:include>
        </div>
    </div>

    <div align="center" style="color:red;font-weight:bold;margin-top:150px">
        <c:choose>
            <c:when test="${!empty errorMessage}">
                <c:out value="${errorMessage}"/>
            </c:when>
            <c:otherwise>
                We're sorry an error has occurred! We will try to remedy this as soon as possible. Responsible parties have been notified and heads will roll.
            </c:otherwise>
        </c:choose>
        <br/><br/><br/>Please try another search.
    </div>

    <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
        <div style="position: absolute; bottom:80px; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center">
            For news and updates, subscribe to the
            <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
            <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
            <input type="submit" name="email-button" value="Subscribe"/>
        </div>
    </form>

</tmpl:stringTemplateWrap>
</html>
