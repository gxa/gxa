<%@ page import="ae3.util.URLUtil" %>

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

<?xml version='1.0' encoding='UTF-8' ?>
<SOURCES>
    <SOURCE uri="<%= URLUtil.getDasUrl(request) %>"
         title="Gene Expression Atlas"
         doc_href="http://www.ebi.ac.uk/gxa"
         description="Gene Expression Atlas DAS Server">
    <MAINTAINER email="ostolop@ebi.ac.uk" />
    <VERSION uri="gxa" created="2009-07-13">
      <COORDINATES uri="http://www.dasregistry.org/coordsys/CS_DS95" source="Gene_ID" authority="Ensembl" test_range="ID">Gene_ID</COORDINATES>
      <CAPABILITY type="das1:features" query_uri="<%= URLUtil.getDasUrl(request) %>/features" />
      <CAPABILITY type="das1:types" query_uri="<%= URLUtil.getDasUrl(request) %>/types" />
      <CAPABILITY type="das1:unknown-segment" query_uri="<%= URLUtil.getDasUrl(request) %>/features" />
     </VERSION>
   </SOURCE>
</SOURCES>