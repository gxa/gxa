/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
 * http://ostolop.github.com/gxa/
 */

 /*******************************************************************************
drop package ATLASUPG;
*******************************************************************************/

CREATE OR REPLACE PACKAGE ATLASUPG IS
  
  PROCEDURE CreateLink(ID varchar2);
  
  PROCEDURE LoadSpecies;
  PROCEDURE LoadGenes;
  PROCEDURE LoadGeneProperties;
  PROCEDURE LoadGenePropertyValues;
  PROCEDURE LoadArrayDesign;
  PROCEDURE LoadDesignElement;
  PROCEDURE LoadExperiment;
  PROCEDURE LoadAssay;
  PROCEDURE LoadSample;
  PROCEDURE LoadAssaySample;
  PROCEDURE LoadProperty;
  PROCEDURE LoadPropertyValue;
  PROCEDURE LoadAssayPropertyValue;
  PROCEDURE LoadSamplePropertyValue;
  PROCEDURE LoadOntology;
  PROCEDURE LoadOntologyTerm;
  PROCEDURE LoadSampleOntology;
  PROCEDURE LoadAssayOntology;
  PROCEDURE LoadExpressionAnalytics;
  
  PROCEDURE FixSourceMapping;
  PROCEDURE FixFactorValue;
  
  PROCEDURE Load;
  PROCEDURE Clean;
END ATLASUPG;
/

/*******************************************************************************

CREATE PUBLIC DATABASE LINK DWPRD
   CONNECT TO AEMART IDENTIFIED BY ***
   USING '(DESCRIPTION =
   (ADDRESS_LIST =
     (ADDRESS = (PROTOCOL = TCP)
                (HOST = MOE.EBI.AC.UK)
                (PORT = 1521)
     )
   )
   (CONNECT_DATA = (SID = AEDWDEV)))' 

call ATLASMGR.Load();
call ATLASMGR.LoadSpecies();
call ATLASMGR.LoadGenes();
call ATLASMGR.LoadGeneProperties();
call ATLASMGR.LoadGenePropertyValues();

delete from a2_spec
select * from a2_spec
ROLLBACK
select * from a2_GenePropertyValue
COMMIT 

insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value)  select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 1, VALUE from AE2__GENE_CAS__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;

delete from a2_GeneProperty
select count(1) from a2_GenePropertyValue

select * from a2_gene
delete from a2_Gene;
delete from a2_GenePropertyValue;
delete from a2_GeneProperty;
delete from a2_Spec;


/*******************************************************************************/
CREATE OR REPLACE PACKAGE BODY ATLASUPG AS

  
PROCEDURE CreateLink(ID varchar2)
AS
BEGIN
  update a2_Spec set name = name where 1=0;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadSpecies
AS
BEGIN
  Insert into a2_Spec (Name) 
  select distinct Value from ae2__Gene_Species__dm@dwprd
  where value is not null;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadGenes
AS
BEGIN
  Insert into a2_Gene(GeneID, SpecID, Identifier, Name) 
  select a.GENE_ID_KEY, s.SpecID, a.GENE_IDENTIFIER, a.GENE_NAME
  from AE2__GENE__MAIN@dwprd a
  left outer join AE2__GENE_SPECIES__DM@dwprd gs on gs.Gene_ID_KEY = a.GENE_ID_KEY
  join a2_Spec s on s.name = gs.Value;
END;
--------------------------------------------------------------------------------
PROCEDURE LoadGeneProperties
AS
BEGIN
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (1,'CAS','AE2__GENE_CAS__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (2,'CHEBI','AE2__GENE_CHEBI__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (3,'CHEMFORMULA','AE2__GENE_CHEMFORMULA__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (4,'DBXREF','AE2__GENE_DBXREF__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (5,'DESC','AE2__GENE_DESC__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (6,'DISEASE','AE2__GENE_DISEASE__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (7,'EMBL','AE2__GENE_EMBL__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (8,'ENSFAMILY','AE2__GENE_ENSFAMILY__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (9,'ENSGENE','AE2__GENE_ENSGENE__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (10,'ENSPROTEIN','AE2__GENE_ENSPROTEIN__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (11,'ENSTRANSCRIPT','AE2__GENE_ENSTRANSCRIPT__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (12,'GOTERM','AE2__GENE_GOTERM__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (13,'GO','AE2__GENE_GO__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (14,'HMDB','AE2__GENE_HMDB__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (15,'IDS','AE2__GENE_IDS__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (16,'IMAGE','AE2__GENE_IMAGE__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (17,'INTERPROTERM','AE2__GENE_INTERPROTERM__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (18,'INTERPRO','AE2__GENE_INTERPRO__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (19,'KEYWORD','AE2__GENE_KEYWORD__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (20,'LOCUSLINK','AE2__GENE_LOCUSLINK__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (21,'OMIM','AE2__GENE_OMIM__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (22,'ORF','AE2__GENE_ORF__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (23,'ORTHOLOG','AE2__GENE_ORTHOLOG__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (24,'PATHWAY','AE2__GENE_PATHWAY__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (25,'PROTEINNAME','AE2__GENE_PROTEINNAME__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (26,'REFSEQ','AE2__GENE_REFSEQ__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (27,'SYNONYM','AE2__GENE_SYNONYM__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (28,'UNIGENE','AE2__GENE_UNIGENE__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (29,'UNIPROTMETENZ','AE2__GENE_UNIPROTMETENZ__DM');
  Insert into A2_GENEPROPERTY (GENEPROPERTYID,NAME,AE2TABLENAME) values (30,'UNIPROT','AE2__GENE_UNIPROT__DM');
END;
--------------------------------------------------------------------------------

PROCEDURE LoadGenePropertyValues
AS
BEGIN
declare 
  cursor c1 is select GenePropertyID, Name, Ae2TableName from a2_GeneProperty;
  q varchar(8000);
begin
 --dbms_output.put_line('begin');
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Name);
    
    q := 'insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value)  '
      || 'select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, ' || rec.GenePropertyID || ', VALUE '
      || 'from ' || rec.Ae2TableName || '@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    EXECUTE immediate q;
 end loop;
 --dbms_output.put_line('end;');
end;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadArrayDesign
AS
BEGIN
 Insert into A2_ArrayDesign ( ArrayDesignID,Accession,Type,Name,Provider) 
  select ARRAYDESIGN_ID_KEY,ARRAYDESIGN_IDENTIFIER,ARRAYDESIGN_TYPE,ARRAYDESIGN_NAME,ARRAYDESIGN_PROVIDER
  from AE2__ARRAYDESIGN@dwprd;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadDesignElement
AS
BEGIN
  Insert into A2_DesignElement(DesignElementID, ArrayDesignID, GeneID, Accession,Type,Name,IsControl)
  select DESIGNELEMENT_ID_KEY,ARRAYDESIGN_ID,GENE_ID_KEY,DESIGNELEMENT_IDENTIFIER,DESIGNELEMENT_TYPE,DESIGNELEMENT_NAME,DESIGNELEMENT_ISCONTROL
  from AE2__DESIGNELEMENT__MAIN@dwprd
  where GENE_ID_KEY is not null and GENE_ID_KEY <> 0;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadExperiment
AS
BEGIN
  Insert into a2_Experiment (ExperimentID,Accession,Description,Performer,Lab )
  select e.Experiment_ID_Key, e.Experiment_ACCESSION, e.experiment_description, e.experiment_performer, e.experiment_lab
  from AE1__EXPERIMENT__MAIN@dwprd e;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadAssay
AS
BEGIN
  Insert into A2_Assay(AssayID
  , Accession 
  , ExperimentID
  , ArrayDesignID) 
select ASSAY_ID_KEY,ASSAY_IDENTIFIER,EXPERIMENT_ID_KEY,ARRAYDESIGN_ID
from AE1__ASSAY__MAIN@dwprd
where ArrayDesign_ID is not null;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadSample
AS
BEGIN
 Insert into a2_Sample(SampleID,Accession,Species) 
 select SAMPLE_ID_KEY,SAMPLE_IDENTIFIER,SAMPLE_SPECIES 
 from AE1__SAMPLE__MAIN@dwprd;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadAssaySample
AS
BEGIN
 Insert into a2_AssaySample(SampleID,AssayID) 
 select SAMPLE_ID_KEY,Assay_ID_key
 from AE1__SAMPLE__MAIN@dwprd
 where Assay_ID_key is not null and Assay_ID_key <> 0
 and exists(select 1 from a2_Assay where AssayID = Assay_ID_key);
END;
--------------------------------------------------------------------------------

PROCEDURE LoadProperty
AS
BEGIN
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 1,'RNAi','AE1__ASSAY_RNAI__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 2,'age','AE1__ASSAY_AGE__DM','AE1__SAMPLE_AGE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 3,'cellline','AE1__ASSAY_CELLLINE__DM','AE1__SAMPLE_CELLLINE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 4,'celltype','AE1__ASSAY_CELLTYPE__DM','AE1__SAMPLE_CELLTYPE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 5,'clinhistory','AE1__ASSAY_CLINHISTORY__DM','AE1__SAMPLE_CLINHISTORY__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 6,'clininfo','AE1__ASSAY_CLININFO__DM','AE1__SAMPLE_CLININFO__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 7,'clintreatment','AE1__ASSAY_CLINTREATMENT__DM','AE1__SAMPLE_CLINTREATMENT__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 8,'compound','AE1__ASSAY_COMPOUND__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 9,'devstage','AE1__ASSAY_DEVSTAGE__DM','AE1__SAMPLE_DEVSTAGE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 10,'diseaselocation','','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 11,'diseasestaging','AE1__ASSAY_DISEASESTAGING__DM','AE1__SAMPLE_DISEASESTAGING__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 12,'diseasestate','AE1__ASSAY_DISEASESTATE__DM','AE1__SAMPLE_DISEASESTATE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 13,'dose','AE1__ASSAY_DOSE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 14,'ecotype','AE1__ASSAY_ECOTYPE__DM','AE1__SAMPLE_ECOTYPE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 15,'genmodif','AE1__ASSAY_GENMODIF__DM','AE1__SAMPLE_GENMODIF__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 16,'genotype','AE1__ASSAY_GENOTYPE__DM','AE1__SAMPLE_GENOTYPE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 17,'growthcondition','AE1__ASSAY_GROWTHCONDITION__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 18,'histology','AE1__ASSAY_HISTOLOGY__DM','AE1__SAMPLE_HISTOLOGY__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 19,'indgeneticchar','AE1__ASSAY_INDGENETICCHAR__DM','AE1__SAMPLE_INDGENETICCHAR__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 20,'individual','AE1__ASSAY_INDIVIDUAL__DM','AE1__SAMPLE_INDIVIDUAL__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 21,'infect','AE1__ASSAY_INFECT__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 22,'injury','AE1__ASSAY_INJURY__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 23,'light','AE1__ASSAY_LIGHT__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 24,'materialType','AE1__ASSAY_MATERIALTYPE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 25,'media','AE1__ASSAY_MEDIA__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 26,'observation','AE1__ASSAY_OBSERVATION__DM','AE1__SAMPLE_OBSERVATION__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 27,'organism','AE1__ASSAY_ORGANISM__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 28,'organismpart','AE1__ASSAY_ORGANISMPART__DM','AE1__SAMPLE_ORGANISMPART__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 29,'phenotype','AE1__ASSAY_PHENOTYPE__DM','AE1__SAMPLE_PHENOTYPE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 30,'protocoltype','AE1__ASSAY_PROTOCOLTYPE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 31,'replicate','AE1__ASSAY_REPLICATE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 32,'sex','AE1__ASSAY_SEX__DM','AE1__SAMPLE_SEX__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 33,'strainorline','AE1__ASSAY_STRAINORLINE__DM','AE1__SAMPLE_STRAINORLINE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 34,'targetcelltype','AE1__ASSAY_TARGETCELLTYPE__DM','AE1__SAMPLE_TARGETCELLTYPE__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 35,'temperature','AE1__ASSAY_TEMPERATURE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 36,'testresult','AE1__ASSAY_TESTRESULT__DM','AE1__SAMPLE_TESTRESULT__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 37,'time','AE1__ASSAY_TIME__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 38,'tumorgrading','AE1__ASSAY_TUMORGRADING__DM','AE1__SAMPLE_TUMORGRADING__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 39,'vehicle','AE1__ASSAY_VEHICLE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 104,'BIOMETRIC','AE1__ASSAY_BIOMETRIC__DM','AE1__SAMPLE_BIOMETRIC__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 105,'CULTIVAR','AE1__ASSAY_CULTIVAR__DM','AE1__SAMPLE_CULTIVAR__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 106,'DISEASELOC','AE1__ASSAY_DISEASELOC__DM','AE1__SAMPLE_DISEASELOC__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 107,'ENVHISTORY','AE1__ASSAY_ENVHISTORY__DM','AE1__SAMPLE_ENVHISTORY__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 108,'FAMILYHISTORY','AE1__ASSAY_FAMILYHISTORY__DM','AE1__SAMPLE_FAMILYHISTORY__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 109,'GENERATION','AE1__ASSAY_GENERATION__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 110,'INITIALTIME','AE1__ASSAY_INITIALTIME__DM','AE1__SAMPLE_INITIALTIME__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 111,'ORGANISMSTATUS','AE1__ASSAY_ORGANISMSTATUS__DM','AE1__SAMPLE_ORGANISMSTATUS__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 112,'PERFORMER','AE1__ASSAY_PERFORMER__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 113,'POPULATION','AE1__ASSAY_POPULATION__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 114,'QCDESCRTYPE','AE1__ASSAY_QCDESCRTYPE__DM','' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 115,'SOURCEPROVIDER','AE1__ASSAY_SOURCEPROVIDER__DM','AE1__SAMPLE_SOURCEPROVIDER__DM' from dual; 
Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 116,'TESTTYPE','AE1__ASSAY_TESTTYPE__DM','AE1__SAMPLE_TESTTYPE__DM' from dual; 
--Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 117,'TEST','AE1__ASSAY_TEST__DM','' from dual; 
--Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 118,'ALL','','AE1__SAMPLE_ALL__DM' from dual; 
--Insert into a2_property (PropertyID,Name,AE1TABLENAME_ASSAY,AE1TABLENAME_SAMPLE) select 119,'TEST_','','AE1__SAMPLE_TEST__DM' from dual; 
END;
--------------------------------------------------------------------------------
PROCEDURE LoadPropertyValue
AS
BEGIN
declare
  cursor c1 is select PropertyID, Ae1TableName_Assay from a2_Property where Ae1TableName_Assay is not null;
  q varchar(8000);
begin
 --dbms_output.put_line('begin');
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    
    q := 'insert into a2_PropertyValue(PropertyID,Name) select distinct ' || rec.PropertyID || ', VALUE from ' || rec.Ae1TableName_Assay || '@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=' || rec.PropertyID || ' and pv.Name = v.Value )' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;

declare 
  cursor c1 is select PropertyID, Ae1TableName_Sample from a2_Property where Ae1TableName_Sample is not null;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Ae1TableName_Sample);
    q := 'insert into a2_PropertyValue(PropertyID,Name) select distinct ' || rec.PropertyID || ', VALUE from ' || rec.Ae1TableName_Sample || '@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=' || rec.PropertyID || ' and pv.Name = v.Value )' ;
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    EXECUTE immediate q;
 end loop;
 --dbms_output.put_line('end');
end;
END;

--------------------------------------------------------------------------------
PROCEDURE LoadAssayPropertyValue
AS
BEGIN
  declare 
  cursor c1 is select PropertyValueID, Ae1TableName_Assay, pv.Name 
  from a2_Property p
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where Ae1TableName_Assay is not null;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    --dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    q := 'insert into a2_AssayPropertyValue(AssayID,PropertyValueID) select v.Assay_id_key, ' || rec.PropertyValueID || ' from ' || rec.Ae1TableName_Assay || '@dwprd v where v.Value = ''' || REPLACE(rec.Name,'''','''''') || '''' ;
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    --dbms_output.put_line(q);
    EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;

END;
--------------------------------------------------------------------------------
PROCEDURE LoadSamplePropertyValue
AS
BEGIN
declare 
  cursor c1 is select PropertyValueID, Ae1TableName_Sample, pv.Name 
  from a2_Property p
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where Ae1TableName_Sample is not null 
  order by PropertyValueID;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    --dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    q := 'insert into a2_SamplePropertyValue(SampleID,PropertyValueID) select v.sample_id_key, ' || rec.PropertyValueID || ' from ' || rec.Ae1TableName_Sample || '@dwprd v where v.Value = ''' || REPLACE(REPLACE(rec.Name,'''',''''''),'&',' ') || ''' and v.sample_id_key in (select SampleID from a2_Sample)';
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    --dbms_output.put_line(q);
    --insert into t(sql) select q from dual;
    EXECUTE IMMEDIATE q;
 end loop;
 dbms_output.put_line('end');
end;
END;
--------------------------------------------------------------------------------
PROCEDURE LoadOntology
AS
BEGIN
Insert into a2_ontology(OntologyID,Name, Source_URI, Description,Version)
select Ontology_ID_Key,Name, Source_URI, Description,Version
from ontology_sources@dwprd; 
END;
--------------------------------------------------------------------------------
PROCEDURE LoadOntologyTerm
AS
BEGIN
Insert into a2_ontologyterm(OntologyTermID, OntologyID, Term , Accession, Description, ORIG_VALUE, orig_value_src ) 
select a2_ontologyterm_seq.nextval, Ontology_id_key, Term, Accession, Description, ORIG_VALUE, ORIG_VALUE_SRC
from (select distinct Ontology_id_key, Term, Accession, Description, ORIG_VALUE, ORIG_VALUE_SRC from ontology_annotation@dwprd);
END;
--------------------------------------------------------------------------------
PROCEDURE LoadSampleOntology
AS
BEGIN
Insert into a2_SampleOntology(SampleOntologyID, SampleID, OntologyTermID, SourceMapping)   
select a2_SampleOntology_seq.nextval, SampleID, OntologyTermID, SourceMapping 
FROM (select oa.sample_id_key as SampleID
           , ot.OntologyTermID as OntologyTermID
           , null as SourceMapping
      from ontology_annotation@dwprd oa
      join a2_ontologyterm ot on ot.ontologyid = oa.Ontology_id_key
                              and ot.term = oa.Term
                              and ot.Accession = oa.Accession
                              --and ot.Description = oa.Description
                              and ot.Orig_Value = oa.Orig_Value
                              and ot.Orig_Value_Src = oa.Orig_Value_Src
      where oa.sample_id_key is not null  );                      
END;
--------------------------------------------------------------------------------
PROCEDURE LoadAssayOntology
AS
BEGIN
Insert into a2_AssayOntology(AssayOntologyID, AssayID, OntologyTermID, SourceMapping)   
select a2_AssayOntology_seq.nextval, AssayID, OntologyTermID, SourceMapping 
FROM (select oa.assay_id_key as AssayID
           , ot.OntologyTermID as OntologyTermID
           , null as SourceMapping
      from ontology_annotation@dwprd oa
      join a2_ontologyterm ot on ot.ontologyid = oa.Ontology_id_key
                              and ot.term = oa.Term
                              and ot.Accession = oa.Accession
                              --and ot.Description = oa.Description
                              and ot.Orig_Value = oa.Orig_Value
                              and ot.Orig_Value_Src = oa.Orig_Value_Src
      where oa.assay_id_key is not null  );  
END;
--------------------------------------------------------------------------------
PROCEDURE LoadExpressionAnalytics
AS
BEGIN
insert into a2_ExpressionAnalytics(ExpressionID, ExperimentID,DesignElementID,PropertyValueID,TSTAT,FPVAL,FPVALADJ, pvaladj)
select rownum, a.Experiment_ID_Key, a.DESIGNELEMENT_ID_KEY, pv.PropertyValueID, a.UPDN_TSTAT,a.FPVAL,a.FPVALADJ,a.UPDN_pvaladj
from Atlas@dwprd a
join a2_Property p on p.Name = a.EF
join a2_PropertyValue pv on pv.Name = a.EFV and pv.PropertyID = p.PropertyID
join a2_Experiment e on e.ExperimentID = a.Experiment_ID_Key --- <-- todo: some data missed
join a2_DesignElement de on de.DesignElementID = a.DESIGNELEMENT_ID_KEY;
END;
--------------------------------------------------------------------------------
FUNCTION CountRows(TableName varchar2)
  RETURN integer
AS
BEGIN
 return 0;
END;
--------------------------------------------------------------------------------
Function ElapsedSec
  return int
As
BEGIN
  return 33;
END;
--------------------------------------------------------------------------------

PROCEDURE Load
AS

BEGIN
  LoadSpecies();
  dbms_output.put_line('species: ' || CountRows('a2_Spec') || ' in ' || ElapsedSec());
  
  LoadGenes();
  dbms_output.put_line('genes: ' || CountRows('a2_Gene') || ' in ' || ElapsedSec());  
  
  LoadGeneProperties();
  dbms_output.put_line('gene properties: ' || CountRows('a2_GeneProperties') || ' in ' || ElapsedSec());  
  
  LoadGenePropertyValues();
  dbms_output.put_line('gene property values: ' || CountRows('a2_GenePropertyData') || ' in ' || ElapsedSec());  
  
  LoadArrayDesign();
  dbms_output.put_line('array design: ' || CountRows('a2_ArrayDesign') || ' in ' || ElapsedSec());  
  
  LoadDesignElement();
  dbms_output.put_line('design element: ' || CountRows('a2_DesignElement') || ' in ' || ElapsedSec());  
  
  LoadExperiment();
  dbms_output.put_line('experiment: ' || CountRows('a2_Experiment') || ' in ' || ElapsedSec());  
  
  LoadAssay();
  dbms_output.put_line('assay: ' || CountRows('a2_Assay') || ' in ' || ElapsedSec());  
  
  LoadSample();
  dbms_output.put_line('sample: ' || CountRows('a2_Sample') || ' in ' || ElapsedSec());  
  
  LoadAssaySample();
  dbms_output.put_line('assay to sample: ' || CountRows('a2_AssaySample') || ' in ' || ElapsedSec());  

  LoadProperty();
  dbms_output.put_line('property: ' || CountRows('a2_Property') || ' in ' || ElapsedSec());  

  LoadPropertyValue();
  dbms_output.put_line('property value: ' || CountRows('a2_PropertyValue') || ' in ' || ElapsedSec());  

  LoadAssayPropertyValue();
  dbms_output.put_line('assay property: ' || CountRows('a2_AssayPropertyValue') || ' in ' || ElapsedSec());  

  LoadSamplePropertyValue();
  dbms_output.put_line('sample property: ' || CountRows('a2_SamplePropertyValue') || ' in ' || ElapsedSec());  

  LoadOntology();
  dbms_output.put_line('ontology: ' || CountRows('a2_Ontology') || ' in ' || ElapsedSec());  

  LoadIOntologyTerm();
  dbms_output.put_line('ontology term: ' || CountRows('a2_OntologyTerm') || ' in ' || ElapsedSec());  

  LoadSampleOntology();
  dbms_output.put_line('sample ontology: ' || CountRows('a2_SampleOntology') || ' in ' || ElapsedSec());  

  LoadAssayOntology();
  dbms_output.put_line('assay ontology: ' || CountRows('a2_AssayOntology') || ' in ' || ElapsedSec());  

  LoadExpressionAnalytics();
  dbms_output.put_line('expression analytics: ' || CountRows('a2_ExpressionAnalytics') || ' in ' || ElapsedSec());  
  
  FixSourceMapping();
  FixFactorValue();

  commit;
END;
--------------------------------------------------------------------------------
PROCEDURE Clean
AS
BEGIN
  dbms_output.put_line('delete property ALL - assay');
  Delete from a2_AssayPropertyValue
  where PropertyValueID in (select PropertyValueID from a2_PropertyValue 
                            where PropertyID = (select PropertyID from a2_Property where Name = 'ALL'));
  
  dbms_output.put_line('delete property ALL - sample');                          
  Delete from a2_SamplePropertyValue
  where PropertyValueID in (select PropertyValueID from a2_PropertyValue 
                            where PropertyID = (select PropertyID from a2_Property where Name = 'ALL'));                            

  dbms_output.put_line('delete property ALL - expression analytics');
  Delete from a2_ExpressionAnalytics
  where PropertyValueID in (select PropertyValueID from a2_PropertyValue 
                            where PropertyID = (select PropertyID from a2_Property where Name = 'ALL'));   

  dbms_output.put_line('delete property ALL - property value');
  Delete from a2_PropertyValue
  where PropertyValueID in (select PropertyValueID from a2_PropertyValue 
                            where PropertyID = (select PropertyID from a2_Property where Name = 'ALL'));   
  
  dbms_output.put_line('delete property ALL - property');                          
  Delete from a2_Property                            
  where PropertyID = (select PropertyID from a2_Property where Name = 'ALL');

  dbms_output.put_line('update name to lowercase');                          
  Update a2_Property                            
  set name = LOWER(name);
  
  commit;

END;

PROCEDURE FixSourceMapping
AS
BEGIN

EXECUTE IMMEDIATE 'create table tmp_SourceMapping(SampleOntologyID int, SamplePropertyValueID int)'; 
EXECUTE IMMEDIATE 'create table tmp_SourceMappingAssay(AssayOntologyID int, AssayPropertyValueID int)'; 

EXECUTE IMMEDIATE 'Insert into tmp_SourceMapping(SampleOntologyID, SamplePropertyValueID)
Select sa.SampleOntologyID, spv.SamplePropertyValueID
from a2_SampleOntology sa
join a2_OntologyTerm ot on ot.OntologyTermID = sa.OntologyTermID
join ontology_annotation@dwprd oa on oa.Sample_ID_key = sa.SampleID and oa.Accession = ot.accession
join a2_PropertyValue pv on pv.name = ot.orig_value
join a2_property p on p.propertyid = pv.PropertyID and p.ae1tablename_sample = oa.ORIG_VALUE_SRC
join a2_samplepropertyvalue spv on spv.SampleID = sa.SampleID and spv.propertyvalueid = pv.propertyvalueid';

EXECUTE IMMEDIATE 'Insert into tmp_SourceMappingAssay(AssayOntologyID, AssayPropertyValueID)
Select sa.AssayOntologyID, MIN(spv.AssayPropertyValueID)
from a2_AssayOntology sa
join a2_OntologyTerm ot on ot.OntologyTermID = sa.OntologyTermID
join ontology_annotation@dwprd oa on oa.Assay_ID_key = sa.AssayID and oa.Accession = ot.accession
join a2_PropertyValue pv on pv.name = ot.orig_value
join a2_property p on p.propertyid = pv.PropertyID and p.ae1tablename_assay = oa.ORIG_VALUE_SRC
join a2_Assaypropertyvalue spv on spv.AssayID = sa.AssayID and spv.propertyvalueid = pv.propertyvalueid
group by sa.AssayOntologyID';

EXECUTE IMMEDIATE 'create index IDX_tmp_AssayOntologyID on tmp_SourceMappingAssay(AssayOntologyID)'; 
EXECUTE IMMEDIATE 'create index IDX_tmp_SampleOntologyID on tmp_SourceMapping(SampleOntologyID)'; 

EXECUTE IMMEDIATE 'update a2_SampleOntology set SourceMapping = (select MAX(SamplePropertyValueID) 
from tmp_SourceMapping t where t.sampleontologyid = a2_SampleOntology.sampleontologyid)';

EXECUTE IMMEDIATE 'update a2_AssayOntology set SourceMapping = (select MAX(AssayPropertyValueID) 
from tmp_SourceMappingAssay t where t.assayontologyid = a2_AssayOntology.Assayontologyid)';

EXECUTE IMMEDIATE 'drop table tmp_SourceMapping';
EXECUTE IMMEDIATE 'drop table tmp_SourceMappingAssay';

END;

PROCEDURE FixFactorValue
AS
BEGIN
  UPDATE a2_AssayPropertyValue set IsFactorValue = 1 where 1=1;
  COMMIT;
END;
  
END;
/
exit;
/