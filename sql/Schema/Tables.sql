 /*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa/
 */

 /*******************************************************************************/
 /*  Tables, Sequences and Triggers in Atlas schema
 /*
 /*  Please note that all constraints and indexes should go into Indexes.sql instead
 /*
 /*
 /*******************************************************************************/

--------------------------------------------------------
--  ORGANISM
--------------------------------------------------------
  CREATE SEQUENCE  "A2_ORGANISM_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;
    

  CREATE TABLE "A2_ORGANISM" (
    "ORGANISMID" NUMBER(22,0) CONSTRAINT NN_ORGANISM_ORGANISMID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_ORGANISM_NAME NOT NULL);

CREATE OR REPLACE TRIGGER "A2_ORGANISM_INSERT"  
before insert on A2_ORGANISM
for each row
begin
if(:new.ORGANISMID is null) then
select A2_ORGANISM_seq.nextval into :new.ORGANISMID from dual;
end if;
end;
/

ALTER TRIGGER "A2_ORGANISM_INSERT" ENABLE;


Insert into A2_Organism(OrganismID,Name)
select a2_organism_seq.nextval,'Unknown' from dual
where not exists(select 1 from a2_Organism where name = 'Unknown');


--------------------------------------------------------
-- GENE PROPERTY
-- ALTER TABLE A2_GENEPROPERTY ADD "IDENTIFIERPRIORITY" NUMBER(22,0)
--------------------------------------------------------
 CREATE SEQUENCE  "A2_GENEPROPERTY_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_GENEPROPERTY" (
    "GENEPROPERTYID" NUMBER(22,0)  CONSTRAINT NN_GENEPROPERTY_ID NOT NULL 
  , "NAME" VARCHAR2(255) CONSTRAINT NN_GENEPROPERTY_NAME NOT NULL
  , "AE2TABLENAME" VARCHAR2(255) 
  , "IDENTIFIERPRIORITY" NUMBER(22,0));

CREATE OR REPLACE TRIGGER A2_GENEPROPERTY_INSERT  
before insert on A2_GENEProperty
for each row
begin
if(:new.GENEPropertyID is null) then
select A2_GENEProperty_seq.nextval into :new.GENEPropertyID from dual;
end if;
end;
/

ALTER TRIGGER A2_GENEProperty_INSERT ENABLE;      


Insert into A2_GENEPROPERTY (GenePropertyID,Name,IDENTIFIERPRIORITY)
Select 1,'ensembl',1 from dual
where not exists(select 1 from a2_geneproperty where Name = 'ensembl');

Insert into A2_GENEPROPERTY (GenePropertyID,Name,IDENTIFIERPRIORITY)
Select 2,'ensgene',2 from dual
where not exists(select 1 from a2_geneproperty where Name = 'ensgene');

Insert into A2_GENEPROPERTY (GenePropertyID,Name,IDENTIFIERPRIORITY)
Select 3,'uniprot',3 from dual
where not exists(select 1 from a2_geneproperty where Name = 'uniprot');

Insert into a2_GeneProperty (GenePropertyID, Name, AE2TABLENAME, IDENTIFIERPRIORITY)
select 33, 'mirna', null, 4 from dual
where not exists(select 1 from a2_geneproperty where Name = 'mirna');

commit;

--------------------------------------------------------
-- GENE PROPERTYVALUE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_GENEPROPERTYVALUE_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_GENEPROPERTYVALUE" (
    "GENEPROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_GPV_ID NOT NULL
  , "GENEPROPERTYID" NUMBER(22,0) CONSTRAINT NN_GPV_GENEPROPERTYID NOT NULL
  , "VALUE" VARCHAR2(255) CONSTRAINT NN_GPV_VALUE NOT NULL);

CREATE OR REPLACE TRIGGER A2_GENEPROPERTYVALUE_INSERT  
before insert on A2_GENEPropertyValue
for each row
begin
if(:new.GENEPropertyValueID is null) then
select A2_GENEPropertyValue_seq.nextval into :new.GENEPropertyValueID from dual;
end if;
end;
/

ALTER TRIGGER A2_GENEPropertyValue_INSERT ENABLE;

--------------------------------------------------------
-- GENE 
--------------------------------------------------------
 CREATE SEQUENCE  "A2_GENE_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_GENE" (
    "GENEID" NUMBER(22,0) CONSTRAINT NN_GENE_ID NOT NULL
  , "ORGANISMID" NUMBER(22,0) CONSTRAINT NN_GENE_ORGANISMID NOT NULL
  , "IDENTIFIER" VARCHAR2(255) CONSTRAINT NN_GENE_IDENTIFIER NOT NULL
  , "NAME" VARCHAR2(255) /*CONSTRAINT NN_GENE_NAME NOT NULL*/);

CREATE OR REPLACE TRIGGER A2_GENE_INSERT  
before insert on A2_GENE
for each row
begin
if(:new.GENEID is null) then
select A2_GENE_seq.nextval into :new.GENEID from dual;
end if;

if(:new.OrganismID is null) then
select OrganismID into :new.OrganismID from a2_Organism where Name = 'unknown';
end if;

end;
/

ALTER TRIGGER A2_GENE_INSERT ENABLE;


--------------------------------------------------------
-- GENE GENEPROPERTYVALUE
--------------------------------------------------------
  CREATE SEQUENCE  "A2_GENEGPV_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_GENEGPV" (
    "GENEGPVID" NUMBER(22,0) CONSTRAINT NN_GENEGPV_ID NOT NULL
  , "GENEID" NUMBER(22,0)  CONSTRAINT NN_GENEGPV_GENEID NOT NULL
  , "GENEPROPERTYVALUEID" NUMBER(22,0)  CONSTRAINT NN_GENEGPV_GPVID NOT NULL);

CREATE OR REPLACE TRIGGER A2_GENEGPV_INSERT  
before insert on A2_GENEGPV
for each row
begin
if(:new.GENEGPVID is null) then
select A2_GENEGPV_seq.nextval into :new.GENEGPVID from dual;
end if;
end;
/

ALTER TRIGGER A2_GENEGPV_INSERT ENABLE;

--------------------------------------------------------
--  MAPPING SOURCE
--------------------------------------------------------
  CREATE SEQUENCE  "A2_SOFTWARE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_SOFTWARE" (
    "SOFTWAREID" NUMBER(22,0) CONSTRAINT NN_SOFTWARE_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_SOFTWARE_NAME NOT NULL
  , "ISACTIVE" VARCHAR2(1) DEFAULT 'F'
  , "VERSION" VARCHAR2(255));

CREATE OR REPLACE TRIGGER "A2_SOFTWARE_INSERT"
before insert on A2_SOFTWARE
for each row
begin
if(:new.SOFTWAREID is null) then
select A2_SOFTWARE_seq.nextval into :new.SOFTWAREID from dual;
end if;
end;
/

ALTER TRIGGER "A2_SOFTWARE_INSERT" ENABLE;

    
--------------------------------------------------------
-- ARRAY DESIGN
--------------------------------------------------------
 CREATE SEQUENCE  "A2_ARRAYDESIGN_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_ARRAYDESIGN" (
    "ARRAYDESIGNID" NUMBER(22,0) CONSTRAINT NN_ARRAYDESIGN_ID NOT NULL
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_ARRAYDESIGN_ACCESSION NOT NULL
  , "TYPE" VARCHAR2(255) 
  , "NAME" VARCHAR2(255) CONSTRAINT NN_ARRAYDESIGN_NAME NOT NULL
  , "PROVIDER" VARCHAR2(255)
  , "LOADDATE" DATE)
 ;

CREATE OR REPLACE TRIGGER A2_ArrayDesign_Insert  
before insert on A2_ArrayDesign
for each row
begin
if( :new.ArrayDesignID is null) then
select A2_ArrayDesign_seq.nextval into :new.ArrayDesignID from dual;
end if;

if(( :new.LOADDATE is null)and(:old.LOADDATE is null)) then
select SYSDATE into :new.LOADDATE from dual;
end if;
end;
/

ALTER TRIGGER A2_ArrayDesign_Insert ENABLE;

--------------------------------------------------------
-- DESIGN ELEMENT
--------------------------------------------------------
 CREATE SEQUENCE  "A2_DESIGNELEMENT_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_DESIGNELEMENT" (
    "DESIGNELEMENTID" NUMBER(22,0) CONSTRAINT NN_DESIGNELEMENT_ID NOT NULL
  , "ARRAYDESIGNID" NUMBER(22,0) CONSTRAINT NN_DESIGNELEMENT_ARRAYDESIGNID NOT NULL
  , "GENEID" NUMBER(22,0) 
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_DESIGNELEMENT_ACCESSION NOT NULL
  , "NAME" VARCHAR2(255)
  , "TYPE" VARCHAR2(255)
  , "ISCONTROL" NUMBER(22,0));

CREATE OR REPLACE TRIGGER A2_DesignElement_Insert  
before insert on A2_DesignElement
for each row
begin
if(:new.DesignElementID is null) then
select A2_DesignElement_seq.nextval into :new.DesignElementID from dual;
end if;
end;
/

ALTER TRIGGER A2_DesignElement_Insert ENABLE;


--------------------------------------------------------
-- PROPERTY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_PROPERTY_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_PROPERTY" (
    "PROPERTYID" NUMBER(22,0) CONSTRAINT NN_PROPERTY_ID NOT NULL,
    "NAME" VARCHAR2(255) CONSTRAINT NN_PROPERTY_NAME NOT NULL,
    "DISPLAYNAME" VARCHAR2(512));

CREATE OR REPLACE TRIGGER A2_PROPERTY_INSERT  
before insert on A2_Property
for each row
begin
if(:new.PropertyID is null) then
  select A2_Property_seq.nextval into :new.PropertyID from dual;
end if;
end;
/

ALTER TRIGGER A2_PROPERTY_INSERT ENABLE;


--------------------------------------------------------
-- PROPERTY VALUE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_PROPERTYVALUE_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_PROPERTYVALUE" (
    "PROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_PROPERTYVALUE_ID NOT NULL
  , "PROPERTYID" NUMBER(22,0) CONSTRAINT NN_PROPERTYVALUE_PROPERTYID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_PROPERTYVALUE_NAME NOT NULL
  , "DISPLAYNAME" VARCHAR2(512));

CREATE OR REPLACE TRIGGER A2_PROPERTYVALUE_INSERT  
before insert on A2_PropertyValue
for each row
begin
if ( :new.PropertyValueID is null) then 
  select A2_PropertyValue_seq.nextval into :new.PropertyValueID from dual;
end if;
end;
/

ALTER TRIGGER A2_PROPERTYVALUE_INSERT ENABLE;


--------------------------------------------------------
-- EXPERIMENT
--------------------------------------------------------
 CREATE SEQUENCE  "A2_EXPERIMENT_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_EXPERIMENT" (
    "EXPERIMENTID" NUMBER(22,0) CONSTRAINT NN_EXPERIMENT_ID NOT NULL
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_EXPERIMENT_ACCESSION NOT NULL
  , "DESCRIPTION" VARCHAR2(2000) 
  , "PERFORMER" VARCHAR2(2000)
  , "LAB" VARCHAR2(2000)
  , "PMID" VARCHAR2(255)
  , "ABSTRACT" VARCHAR2(2000)
  , "LOADDATE" DATE
  , "RELEASEDATE" DATE
  , "PRIVATE" NUMBER(1)
  );

CREATE OR REPLACE TRIGGER A2_EXPERIMENT_INSERT  
before insert on A2_Experiment
for each row
begin
if(:new.ExperimentID is null) then
  select A2_Experiment_seq.nextval into :new.ExperimentID from dual;
end if;
end;
/

ALTER TRIGGER A2_EXPERIMENT_INSERT ENABLE;

--------------------------------------------------------
-- ASSET - file,figure,picture,etc. COMMIT
--------------------------------------------------------
 CREATE SEQUENCE  "A2_EXPERIMENTASSET_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;

 CREATE TABLE "A2_EXPERIMENTASSET" (
    "EXPERIMENTASSETID" NUMBER CONSTRAINT NN_EASSET_ID NOT NULL
  , "EXPERIMENTID" NUMBER CONSTRAINT NN_EASSET_EXPERIMENTID NOT NULL
  , "NAME" VARCHAR2(255) 
  , "FILENAME" VARCHAR2(255)
  , "DESCRIPTION" VARCHAR2(2000));
    
  CREATE OR REPLACE TRIGGER A2_EXPERIMENTASSET_INSERT  
before insert on A2_EXPERIMENTASSET
for each row
begin
if(:new.EXPERIMENTASSETID is null) then
select A2_EXPERIMENTASSET_seq.nextval into :new.EXPERIMENTASSETID from dual;
end if;
end;
/

ALTER TRIGGER A2_EXPERIMENTASSET_INSERT ENABLE;  
    
--------------------------------------------------------
-- ASSAY
--------------------------------------------------------
  CREATE SEQUENCE  "A2_ASSAY_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_ASSAY" (
    "ASSAYID" NUMBER(22,0) CONSTRAINT NN_ASSAY_ASSAYID NOT NULL
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_ASSAY_ACCESSION NOT NULL
  , "EXPERIMENTID" NUMBER(22,0) CONSTRAINT NN_ASSAY_EXPERIMENTID NOT NULL
  , "ARRAYDESIGNID" NUMBER(22,0) CONSTRAINT NN_ASSAY_ARRAYDESIGNID NOT NULL);

CREATE OR REPLACE TRIGGER A2_ASSAY_INSERT  
before insert on A2_Assay
for each row
begin
if(:new.AssayID is null) then
select A2_Assay_seq.nextval into :new.AssayID from dual;
end if;
end;
/

ALTER TRIGGER A2_ASSAY_INSERT ENABLE;


--------------------------------------------------------
-- SAMPLE
--------------------------------------------------------
   CREATE SEQUENCE  "A2_SAMPLE_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_SAMPLE" (
    "SAMPLEID" NUMBER(22,0) CONSTRAINT NN_SAMPLE_SAMPLEID NOT NULL
  , "EXPERIMENTID" NUMBER(22,0) CONSTRAINT NN_SAMPLE_EXPERIMENTID NOT NULL
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_SAMPLE_ACCESSION NOT NULL
  , "ORGANISMID" NUMBER(22,0)
  , "CHANNEL" VARCHAR2(255));

CREATE OR REPLACE TRIGGER A2_SAMPLE_INSERT
before insert on A2_Sample
for each row
begin
if(:new.SampleID is null) then
select A2_Sample_seq.nextval into :new.SampleID from dual;
end if;
end;
/

ALTER TRIGGER A2_SAMPLE_INSERT ENABLE;


--------------------------------------------------------
-- ASSAYSAMPLE
--------------------------------------------------------
 CREATE TABLE "A2_ASSAYSAMPLE" (
   "ASSAYID" NUMBER(22,0) CONSTRAINT NN_ASSAYSAMPLE_ASSAYID NOT NULL
  , "SAMPLEID" NUMBER(22,0) CONSTRAINT NN_ASSAYSAMPLE_SAMPLEID NOT NULL) ;

--------------------------------------------------------
-- ASSAYPROPERTYVALUE
--------------------------------------------------------
   CREATE SEQUENCE  "A2_ASSAYPV_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_ASSAYPV" (
    "ASSAYPVID" NUMBER(22,0) CONSTRAINT NN_ASSAYPV_ID NOT NULL
  , "ASSAYID" NUMBER(22,0) CONSTRAINT NN_ASSAYPV_ASSAYID NOT NULL
  , "PROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_ASSAYPV_PROPERTYVALUEID NOT NULL);

CREATE OR REPLACE TRIGGER A2_ASSAYPV_INSERT  
before insert on A2_ASSAYPV
for each row
begin
if(:new.ASSAYPVID is null) then
select A2_ASSAYPV_seq.nextval into :new.ASSAYPVID from dual;
end if;
end;
/

ALTER TRIGGER A2_ASSAYPV_INSERT ENABLE;

--------------------------------------------------------
-- SAMPLEPROPERTYVALUE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_SAMPLEPV_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_SAMPLEPV" (
    "SAMPLEPVID" NUMBER(22,0) CONSTRAINT NN_SAMPLEPV_ID NOT NULL
  , "SAMPLEID" NUMBER(22,0) CONSTRAINT NN_SAMPLEPV_SAMPLEID NOT NULL
  , "PROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_SAMPLEPV_PROPERTYVALUEID NOT NULL);

CREATE OR REPLACE TRIGGER A2_SAMPLEPV_INSERT  
before insert on A2_SAMPLEPV
for each row
begin
if(:new.SAMPLEPVID is null) then
select A2_SAMPLEPV_seq.nextval into :new.SAMPLEPVID from dual;
end if;
end;
/

ALTER TRIGGER A2_SAMPLEPV_INSERT ENABLE;   

--------------------------------------------------------
-- ONTOLOGY
--------------------------------------------------------

CREATE SEQUENCE "A2_ONTOLOGY_SEQ";

  CREATE TABLE "A2_ONTOLOGY" (
    "ONTOLOGYID" NUMBER(22,0) CONSTRAINT NN_ONTOLOGY_ID NOT NULL
  , "NAME" VARCHAR2(255) 
  , "SOURCE_URI" VARCHAR2(500)
  , "DESCRIPTION" VARCHAR2(4000)
  , "VERSION" VARCHAR2(50));

CREATE OR REPLACE TRIGGER A2_ONTOLOGY_INSERT
before insert on A2_Ontology
for each row
begin
if( :new.OntologyID is null) then
select A2_Ontology_seq.nextval into :new.OntologyID from dual;
end if;
end;
/

ALTER TRIGGER A2_ONTOLOGY_INSERT ENABLE;

--------------------------------------------------------
-- ONTOLOGY TERM
--------------------------------------------------------
CREATE SEQUENCE  "A2_ONTOLOGYTERM_SEQ"  
    MINVALUE 1 MAXVALUE 1.00000000000000E+27 
    INCREMENT BY 1 START WITH 1 CACHE 20 
    NOORDER  NOCYCLE;


CREATE TABLE "A2_ONTOLOGYTERM" (
    "ONTOLOGYTERMID" NUMBER(22,0) CONSTRAINT NN_ONTOLOGYTERM_ID NOT NULL
  , "ONTOLOGYID" NUMBER(22,0) CONSTRAINT NN_ONTOLOGYTERM_ONTOLOGYID NOT NULL
  , "TERM" VARCHAR2(4000)
  , "ACCESSION" VARCHAR2(255) CONSTRAINT NN_ONTOLOGYTERM_ACCESSION NOT NULL
  , "DESCRIPTION" VARCHAR2(4000));

CREATE OR REPLACE TRIGGER A2_ONTOLOGYTERM_INSERT  
before insert on A2_OntologyTerm
for each row
begin
if( :new.OntologyTermID is null) then
select A2_OntologyTerm_seq.nextval into :new.OntologyTermID from dual;
end if;
end;
/

ALTER TRIGGER A2_ONTOLOGYTERM_INSERT ENABLE; 


--------------------------------------------------------
-- ASSAYPROPERTYVALUEONTOLOGY 
--------------------------------------------------------
  CREATE TABLE "A2_ASSAYPVONTOLOGY" (
    "ONTOLOGYTERMID" NUMBER(22,0) CONSTRAINT NN_ASSAYPVONTOLOGY_OTID NOT NULL
  , "ASSAYPVID" NUMBER(22,0) CONSTRAINT NN_ASSAYPVONTOLOGY_ASSAYPVID NOT NULL);

--------------------------------------------------------
-- SAMPLEPROPERTYVALUEONTOLOGY 
--------------------------------------------------------

  CREATE TABLE "A2_SAMPLEPVONTOLOGY" (
    "ONTOLOGYTERMID" NUMBER(22,0) CONSTRAINT NN_SAMPLEPVONTOLOGY_OTID NOT NULL
  , "SAMPLEPVID" NUMBER(22,0) CONSTRAINT NN_SAMPLEPVONTOLOGY_SAMPLEPVID NOT NULL);    

--------------------------------------------------------
-- BIOENTITY PROPERTY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYPROPERTY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYPROPERTY" (
    "BIOENTITYPROPERTYID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYPROPERTY_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BIOENTITYPROPERTY_NAME NOT NULL
  );

CREATE OR REPLACE TRIGGER A2_BIOENTITYPROPERTY_INSERT
before insert on A2_BIOENTITYProperty
for each row
begin
if(:new.BIOENTITYPropertyID is null) then
select A2_BIOENTITYProperty_seq.nextval into :new.BIOENTITYPropertyID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYProperty_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY PROPERTYVALUE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYPROPERTYVALUE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYPROPERTYVALUE" (
    "BEPROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_BEPV_ID NOT NULL
  , "BIOENTITYPROPERTYID" NUMBER(22,0) CONSTRAINT NN_GPV_BEPROPERTYID NOT NULL
  , "VALUE" VARCHAR2(1000) CONSTRAINT NN_BEPV_VALUE NOT NULL);

CREATE OR REPLACE TRIGGER A2_BEPROPERTYVALUE_INSERT
before insert on A2_BIOENTITYPropertyValue
for each row
begin
if(:new.BEPropertyValueID is null) then
select A2_BIOENTITYPROPERTYVALUE_SEQ.nextval into :new.BEPropertyValueID from dual;
end if;
end;
/

ALTER TRIGGER A2_BEPROPERTYVALUE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITYTYPE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYTYPE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BIOENTITYTYPE" (
    "BIOENTITYTYPEID" NUMBER(22,0) CONSTRAINT NN_BIOENTITYTYPEID_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BETYPE_NAME NOT NULL
  , "ID_FOR_INDEX" VARCHAR2(1) default '0'
  , "ID_FOR_ANALYTICS" VARCHAR2(1) default '0'
  , "IDENTIFIERPROPERTYID" NUMBER(22,0)
  , "NAMEPROPERTYID" NUMBER(22,0)
  , "PROP_FOR_INDEX" VARCHAR2(1) default '0');

CREATE OR REPLACE TRIGGER A2_BIOENTITYTYPE_INSERT
before insert on A2_BIOENTITYTYPE
for each row
begin
if(:new.BIOENTITYTYPEID is null) then
select A2_BIOENTITYTYPE_seq.nextval into :new.BIOENTITYTYPEID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYTYPE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BIOENTITY" (
    "BIOENTITYID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_ID NOT NULL
  , "IDENTIFIER" VARCHAR2(255) CONSTRAINT NN_BIOENTITY_IDENTIFIER NOT NULL
  , "NAME" VARCHAR2(255)
  , "ORGANISMID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_ORGANISMID NOT NULL
  , "BIOENTITYTYPEID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_BIOENTITYTYPEID NOT NULL);

CREATE OR REPLACE TRIGGER A2_BIOENTITY_INSERT
before insert on A2_BIOENTITY
for each row
begin
if(:new.BIOENTITYID is null) then
select A2_BIOENTITY_seq.nextval into :new.BIOENTITYID from dual;
end if;

if(:new.OrganismID is null) then
select OrganismID into :new.OrganismID from a2_Organism where Name = 'unknown';
end if;

end;
/

ALTER TRIGGER A2_BIOENTITY_INSERT ENABLE;

--------------------------------------------------------
-- BERELATIONTYPE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BERELATIONTYPE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BERELATIONTYPE" (
    "BERELATIONTYPEID" NUMBER(22,0) CONSTRAINT NN_BERELATIONTYPEID_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BERELATIONTYPE_NAME NOT NULL )
 ;

CREATE OR REPLACE TRIGGER A2_BERELATIONTYPE_INSERT
before insert on A2_BERELATIONTYPE
for each row
begin
if(:new.BERELATIONTYPEID is null) then
select A2_BERELATIONTYPE_seq.nextval into :new.BERELATIONTYPEID from dual;
end if;
end;
/

ALTER TRIGGER A2_BERELATIONTYPE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY BIOENTITYPROPERTYVALUE
--------------------------------------------------------
  CREATE SEQUENCE  "A2_BIOENTITYBEPV_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYBEPV" (
    "BIOENTITYBEPVID" NUMBER(22,0) CONSTRAINT NN_BIOENTITYBEPV_ID NOT NULL
  , "BIOENTITYID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_BIOENTITYID NOT NULL
  , "BEPROPERTYVALUEID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_BEPVID NOT NULL
  , "SOFTWAREID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_SWID NOT NULL);

CREATE OR REPLACE TRIGGER A2_BIOENTITYBEPV_INSERT
before insert on A2_BIOENTITYBEPV
for each row
begin
if(:new.BIOENTITYBEPVID is null) then
select A2_BIOENTITYBEPV_seq.nextval into :new.BIOENTITYBEPVID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYBEPV_INSERT ENABLE;

--------------------------------------------------------
-- DESIGNELEMENT BIOENTITY
--------------------------------------------------------
  CREATE SEQUENCE  "A2_DESIGNELTBIOENTITY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_DESIGNELTBIOENTITY" (
    "DEBEID" NUMBER(22,0) CONSTRAINT NN_DEBE_ID NOT NULL
  , "DESIGNELEMENTID" NUMBER(22,0) CONSTRAINT NN_DEBE_DESIGNELEMENTID NOT NULL
  , "BIOENTITYID" NUMBER(22,0)  CONSTRAINT NN_DEBE_BIOENTITYID NOT NULL
  , "SOFTWAREID" NUMBER(22,0)  CONSTRAINT NN_DEBE_SOFTWAREID NOT NULL);


CREATE OR REPLACE TRIGGER A2_DESIGNELTBIOENTITY_INSERT
before insert on A2_DESIGNELTBIOENTITY
for each row
begin
if(:new.DEBEID is null) then
select A2_DESIGNELTBIOENTITY_seq.nextval into :new.DEBEID from dual;
end if;
end;
/

ALTER TRIGGER A2_DESIGNELTBIOENTITY_INSERT ENABLE;

--------------------------------------------------------
--  DDL for TEMP TABLE
--------------------------------------------------------

create global temporary table tmp_DesignElementMap(
    DesignElementAccession varchar2(255) NOT NULL
    ,GeneID NUMBER(22,0) 
    ,GeneIdentifier varchar2(255)) ON COMMIT DELETE ROWS;

    
--------------------------------------------------------
--  TASK MANAGER DATA STRUCTURES
--------------------------------------------------------  

CREATE TABLE A2_TASKMAN_LOG
(
  ID NUMBER NOT NULL,
  TASKID NUMBER NOT NULL,
  TYPE VARCHAR2(32) NOT NULL,
  ACCESSION VARCHAR2(256 CHAR)  NOT NULL,
  RUNMODE VARCHAR2(32),
  USERNAME VARCHAR2(64) NOT NULL,
  EVENT VARCHAR2(32) NOT NULL,
  MESSAGE VARCHAR2(4000 CHAR),
  TIME TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL -- NOT NULL DEFAULT SYSTIMESTAMP does not work.
);

CREATE SEQUENCE A2_TASKMAN_TASKID_SEQ ;

CREATE SEQUENCE A2_TASKMAN_LOG_SEQ ;

CREATE
TRIGGER A2_TASKMAN_LOG_TRG
 BEFORE INSERT ON A2_TASKMAN_LOG
FOR EACH ROW 
BEGIN
  SELECT A2_TASKMAN_LOG_SEQ.NEXTVAL INTO :NEW.ID FROM DUAL;
END;
/

CREATE TABLE A2_TASKMAN_STATUS
(
  TYPE VARCHAR2(32)NOT NULL,
  ACCESSION VARCHAR2(256 CHAR) NOT NULL,
  STATUS VARCHAR2(32) NOT NULL
);

 CREATE TABLE A2_TASKMAN_TAGTASKS
 (
    TASKCLOUDID NUMBER NOT NULL,
    TASKID NUMBER NOT NULL
 );

 CREATE TABLE A2_TASKMAN_TAG
 (
    TASKCLOUDID NUMBER NOT NULL,
    TAG VARCHAR2(512 CHAR) NOT NULL,
    TAGTYPE VARCHAR2(32 CHAR) NOT NULL
 );

CREATE TABLE A2_CONFIG_PROPERTY
(
  NAME VARCHAR2(128 CHAR) CONSTRAINT NN_CONFIGPROP_NAME NOT NULL,
  VALUE VARCHAR2(4000 CHAR)
);

CREATE TABLE A2_SCHEMAVERSION (
  VERSION VARCHAR2(32 BYTE) NOT NULL
, APPLIED_ON TIMESTAMP(6) NOT NULL
, DURATION NUMBER(*, 0) NOT NULL
);

-- AnnotationSource table
CREATE SEQUENCE  A2_ANNOTATIONSRC_SEQ
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;

CREATE TABLE A2_ANNOTATIONSRC(
  annotationsrcid NUMBER(22,0)  CONSTRAINT NN_ANNOTATIONSRC_ID NOT NULL
  , SOFTWAREID NUMBER(22,0)  CONSTRAINT NN_ANNOTATIONSRC_SWID NOT NULL
  , ORGANISMID NUMBER(22,0) CONSTRAINT NN_ANNOTATIONSRC_ORGANISM NOT NULL
  , url VARCHAR2(512)
  , biomartorganismname VARCHAR2(255)
  , databaseName VARCHAR2(255)
  , mySqlDbName VARCHAR2(255)
  , mySqlDbUrl VARCHAR2(255)
  , annsrctype VARCHAR2(255) CONSTRAINT NN_ANNOTATIONSRC_ASTYPE NOT NULL
  , LOADDATE DATE
  , isApplied VARCHAR2(1) DEFAULT 'F'
);

CREATE OR REPLACE TRIGGER A2_ANNOTATIONSRC_INSERT
before insert on A2_ANNOTATIONSRC
for each row
begin
if(:new.annotationsrcid is null) then
select A2_ANNOTATIONSRC_SEQ.nextval into :new.annotationsrcid from dual;
end if;
END;
/

-- Link table AnnotationSource to BioEntity type
CREATE TABLE A2_ANNSRC_BIOENTITYTYPE(
  annotationsrcid NUMBER(22,0) CONSTRAINT NN_ANNSRCBET_ANNSRCID NOT NULL
  , BIOENTITYTYPEID NUMBER(22,0) CONSTRAINT NN_ANNSRCBET_BETID NOT NULL
  );

-- BioMart Property table
CREATE SEQUENCE  A2_BIOMARTPROPERTY_SEQ
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;

CREATE TABLE A2_BIOMARTPROPERTY (
 biomartpropertyId NUMBER(22,0) CONSTRAINT NN_BMPROPERTY_ID NOT NULL
, BIOENTITYPROPERTYID NUMBER(22,0) CONSTRAINT NN_BMPROPERTY_BEPROPERTYID NOT NULL
, NAME VARCHAR2(255) CONSTRAINT NN_BMPROPERTY_NAME NOT NULL
, annotationsrcid NUMBER(22,0) CONSTRAINT NN_BMPROPERTY_ANNSRCID NOT NULL
);

CREATE OR REPLACE TRIGGER A2_BIOMARTPROPERTY_INSERT
before insert on A2_BIOMARTPROPERTY
for each row
begin
if(:new.biomartpropertyId is null) then
select A2_BIOMARTPROPERTY_SEQ.nextval into :new.biomartpropertyId from dual;
end if;
END;
/
-- BioMart ArrayDesign table
CREATE SEQUENCE  A2_BIOMARTARRAYDESIGN_SEQ
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;

CREATE TABLE A2_BIOMARTARRAYDESIGN (
 BIOMARTARRAYDESIGNID NUMBER(22,0) CONSTRAINT NN_BMAD_ID NOT NULL
, ARRAYDESIGNID NUMBER(22,0) CONSTRAINT NN_BMAD_ARAYDESIGNID NOT NULL
, NAME VARCHAR2(255) CONSTRAINT NN_BMAD_NAME NOT NULL
, annotationsrcid NUMBER(22,0) CONSTRAINT NN_BMAD_ANNSRCID NOT NULL
);

CREATE OR REPLACE TRIGGER A2_BIOMARTARRAYDESIGN_INSERT
before insert on A2_BIOMARTARRAYDESIGN
for each row
begin
if(:new.biomartarraydesignId is null) then
select A2_BIOMARTARRAYDESIGN_SEQ.nextval into :new.biomartarraydesignId from dual;
end if;
END;
/
exit;
