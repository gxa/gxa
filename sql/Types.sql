/*******************************************************************************/
/*  Types in Atlas2 schema     
/*
/*                                              
/*******************************************************************************/


--------------------------------------------------------
--  DDL for Type INTRECORD
--------------------------------------------------------
  CREATE OR REPLACE TYPE "ATLAS2"."INTRECORD" AS OBJECT(ID INTEGER);
/
--------------------------------------------------------
--  DDL for Type TBLINT
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."TBLINT" AS TABLE OF ATLAS2.INTRECORD;
/
--------------------------------------------------------
--  DDL for Type ACCESSIONQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."ACCESSIONQUERY" as Object(
  Id int
  ,Accession varchar2(255)
) FINAL;
/
--------------------------------------------------------
--  DDL for Type ACCESSIONTABLE
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."ACCESSIONTABLE" as table of varchar2(255);
/
--------------------------------------------------------
--  DDL for Type ARRAYDESIGNQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."ARRAYDESIGNQUERY" as Object(
  AccessionQuery ATLAS2.AccessionQuery
  ,Name varchar2(255)
  ,Type varchar2(255)
  ,Provider varchar2(255)  
);
/
--------------------------------------------------------
--  DDL for Type ASSAYQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."ASSAYQUERY" as Object(
   accessionQuery ATLAS2.AccessionQuery
  ,properties TBLINT
);
/
--------------------------------------------------------
--  DDL for Type DESIGNELEMENT
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."DESIGNELEMENT" is object (Accession varchar2(255),  GeneAccession varchar2(255));
/
--------------------------------------------------------
--  DDL for Type DESIGNELEMENTTABLE
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."DESIGNELEMENTTABLE" is table of DESIGNELEMENT;
/  
--------------------------------------------------------
--  DDL for Type EXPERIMENTQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."EXPERIMENTQUERY" as Object(
   accessionQuery ATLAS2.AccessionQuery
  ,lab varchar2(255)
  ,performer varchar2(255)
  ,genes TBLINT
  ,properties TBLINT
);
/
--------------------------------------------------------
--  DDL for Type EXPRESSIONVALUE
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."EXPRESSIONVALUE" is object (DesignElementAccession varchar2(255), Value float);
/  
--------------------------------------------------------
--  DDL for Type EXPRESSIONVALUETABLE
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."EXPRESSIONVALUETABLE" is TABLE of ExpressionValue;
/  
--------------------------------------------------------
--  DDL for Type GENEPROPERTYQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."GENEPROPERTYQUERY" as Object(
 PropertyID int
,PropertyValue varchar2(255) 
,FullTextQuery varchar2(255)  
);
/
--------------------------------------------------------
--  DDL for Type GENEQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."GENEQUERY" as Object(
   accessionQuery ATLAS2.AccessionQuery
  ,properties TBLINT
  ,species varchar2(255)
);
/
--------------------------------------------------------
--  DDL for Type PAGESORTPARAMS
--------------------------------------------------------
  CREATE OR REPLACE TYPE "ATLAS2"."PAGESORTPARAMS" as Object(
   StartRow int
  ,NumRows int
  ,OrderBy varchar2(255)
);
/
--------------------------------------------------------
--  DDL for Type PROPERTY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."PROPERTY" as object(
  Accession varchar2(255)
  ,Name varchar2(255)
  ,Value varchar2(255)
  ,IsFactorValue int
);
/
--------------------------------------------------------
--  DDL for Type PROPERTYQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."PROPERTYQUERY" as Object(
 PropertyID int
,PropertyValue varchar2(255) 
,FullTextQuery varchar2(255)  
,samples TBLINT
,assays TBLINT
,experiments TBLINT
,isAssay int --return only properties associated with assay
,isSample int   --return only properties associated with assay
);
/
--------------------------------------------------------
--  DDL for Type PROPERTYTABLE
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."PROPERTYTABLE" as table of Property
/
--------------------------------------------------------
--  DDL for Type SAMPLEQUERY
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."SAMPLEQUERY" as Object(
   accessionQuery ATLAS2.AccessionQuery
  ,properties TBLINT
);
/
--------------------------------------------------------
--  DDL for Type TBLINTEGER
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."TBLINTEGER" 
  as table of integer;
/
--------------------------------------------------------
--  DDL for Type TBLVARCHAR
--------------------------------------------------------

  CREATE OR REPLACE TYPE "ATLAS2"."TBLVARCHAR" 
  as table of varchar(2000);
/

create or replace
TYPE INTARRAY is VARRAY(100000000) OF INTEGER;
/
