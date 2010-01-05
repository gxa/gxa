<%@ page import="ae3.util.URLUtil" %>

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