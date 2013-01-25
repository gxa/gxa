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

<jsp:include page="/WEB-INF/jsp/includes/global-inc-head.jsp"/>

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="helpPageHead"/>
    <wro4j:all name="bundle-jquery"/>
    <wro4j:all name="bundle-common-libs"/>
    <wro4j:all name="bundle-gxa"/>

    <style type="text/css">
            /* TODO: display contents appropriately */

        .ae_pagecontainer  .toc {
            position: absolute;
            top: 0;
            right: 0;
            width: 30%;
            background-color: white;
        }

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

            var toc = $('.toc');
            $('.toc ~ hr').hide();

            var parentY = toc.offset().top;
            $(window).scroll(function () {
                var scrolltop = $(window).scrollTop();
                var offset = scrolltop - parentY + 10;
                if (offset < 0)
                    offset = 0;
                toc.animate({top:offset + "px"}, {duration:500, queue:false});
            });
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

    <div class="contents" id="contents">
        <div class="ae_pagecontainer">

            <div style="position:relative;width:100%;">
                <div style="padding-bottom:50px;width:70%">
                    <u:renderWiki/>
                </div>
            </div>

        </div>
        <!-- ae_pagecontainer -->
    </div>
    <!-- /id="contents" -->

</tmpl:stringTemplateWrap>
</html>
