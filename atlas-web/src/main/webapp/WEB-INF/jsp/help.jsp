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
    <jsp:include page="/WEB-INF/jsp/includes/global-inc-head.jsp"/>

    <tmpl:stringTemplate name="helpPageHead"/>
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-gxa"/>

    <style type="text/css">
            /* TODO: display contents appropriately */
/*
        .ae_pagecontainer  .toc {
            position: absolute;
            top: 0;
            right: 0;
            width: 30%;
            background-color: white;
            z-index: 100;
        }
*/
        .toc ul {
            margin-left: 0;
            padding-left: 1.5em;
            text-indent: -0.5em;
        }

        .ae_pagecontainer .toc td {
            vertical-align: top;
            padding: 0 0 0 20px;
            margin: 0;
        }

        #toctitle h2 {
            margin-top: 0;
        }

        .content{
            width: 660px;
        }

        /* jQuery Stickem - side menu - inline css start */
        .aside{
            float:right;
            width:300px;
            margin-right:20px;
        }

        .stickem-container {
            position: relative;
        }

        .stickit {
            margin-left:680px;
            position: fixed;
            top: 0;
        }

        .stickit-end {
            bottom: 40px;
            position: absolute;
            right: 0;
        }
        /* jQuery Stickem - inline side menu - end */

    </style>

    <script type="text/javascript">
        $(document).ready(function () {
            clearLocalNav();
            if (document.URL.indexOf("ReleaseNotes") > -1) {
                $("#local-nav-notes").addClass("active");
            }
            if (document.URL.indexOf("AtlasDasSource") > -1) {
                $("#local-nav-das").addClass("active");
            }
            if (document.URL.indexOf("AtlasApis") > -1) {
                $("#local-nav-api").addClass("active");
            }
            if (document.URL.indexOf("HelpHome") > -1) {
                $("#local-nav-help").addClass("active");
            }
            if (document.URL.indexOf("AtlasFaq") > -1) {
                $("#local-nav-faq").addClass("active");
            }
            if (document.URL.indexOf("AboutAtlas") > -1) {
                $("#local-nav-about").addClass("active");
            }

            $('.toc ~ hr').hide();

            /* jQuery Stickem - side menu - inline javascript start */

            $('.toc').wrap("<div class='aside stickem'/>");

            $('.stickem-container').children().not('.stickem').wrapAll("<div class='content'/>");

            $('.container').stickem();

            $("pre").css("overflow-x","auto");

            /* jQuery Stickem - side menu - inline javascript end */

        });
    </script>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

    <div class="ae_pagecontainer" style="overflow:hidden;">

        <div style="position:relative;width:1000px;">
            <div class="container">
                <div style="padding-bottom:50px;width:100%" class="row stickem-container" >
                        <u:renderWiki/>
                </div>
            </div>
        </div>

    </div>
    <!-- ae_pagecontainer -->

</tmpl:stringTemplateWrap>
</html>
