/*******************************************************************************
data migration script, as described in

http://bar.ebi.ac.uk:8080/trac/wiki/ArrayExpressAtlas/DbMigration
********************************************************************************/

CREATE PUBLIC DATABASE LINK DWPRD
   CONNECT TO AEMART IDENTIFIED BY XXXXX
   USING '(DESCRIPTION =
   (ADDRESS_LIST =
     (ADDRESS = (PROTOCOL = TCP)
                (HOST = MOE.EBI.AC.UK)
                (PORT = 1521)
     )
   )
   (CONNECT_DATA = (SID = AEDWDEV)))';


--select * from a2_Spec
--10 sec
Insert into a2_Spec (Name) 
select distinct Value from ae2__Gene_Species__dm@dwprd
where value is not null;

--select * from a2_Gene
--14 sec
Insert into a2_Gene(GeneID, SpecID, Identifier, Name)
select a.GENE_ID_KEY, s.SpecID, a.GENE_IDENTIFIER, a.GENE_NAME
from AE2__GENE__MAIN@dwprd a
left outer join AE2__GENE_SPECIES__DM@dwprd gs on gs.Gene_ID_KEY = a.GENE_ID_KEY
join a2_Spec s on s.name = gs.Value;


/********************************************************************************
EXECUTE a2_GeneProperty_data.sql 
1 sec
********************************************************************************/
---a2_GeneProperty_data

declare 
  cursor c1 is select GenePropertyID, Name, Ae2TableName from a2_GeneProperty;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Name);
    
    q := 'insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, ' || rec.GenePropertyID || ', VALUE from ' || rec.Ae2TableName || '@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    --EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end;');
end;

/********************************************************************************
Copy output to a2_GenePropertyValue.sql and execute it
1800 sec - coffee break
********************************************************************************/

--delete from a2_ArrayDesign
--a2_ArrayDesign
--2 sec
Insert into A2_ArrayDesign ( ArrayDesignID,Accession,Type,Name,Provider) 
select ARRAYDESIGN_ID_KEY,ARRAYDESIGN_IDENTIFIER,ARRAYDESIGN_TYPE,ARRAYDESIGN_NAME,ARRAYDESIGN_PROVIDER
from AE2__ARRAYDESIGN@dwprd

--delete from a2_DesignElement
--a2_DesignElement
--106 sec
Insert into A2_DesignElement(DesignElementID, ArrayDesignID, GeneID, Accession,Type,Name,IsControl)
select DESIGNELEMENT_ID_KEY,ARRAYDESIGN_ID,GENE_ID_KEY,DESIGNELEMENT_IDENTIFIER,DESIGNELEMENT_TYPE,DESIGNELEMENT_NAME,DESIGNELEMENT_ISCONTROL
from AE2__DESIGNELEMENT__MAIN@dwprd
where GENE_ID_KEY is not null and GENE_ID_KEY <> 0;

--1 sec
Insert into a2_Experiment (ExperimentID,Accession,Description,Performer,Lab )
select e.Experiment_ID_Key, e.Experiment_ACCESSION, e.experiment_description, e.experiment_performer, e.experiment_lab
from AE1__EXPERIMENT__MAIN@dwprd e;

--TODO: there ARE some assays without ArrayDesignID, this information is lost
--6 sec
Insert into A2_Assay(AssayID
  , Accession 
  , ExperimentID
  , ArrayDesignID) 
select ASSAY_ID_KEY,ASSAY_IDENTIFIER,EXPERIMENT_ID_KEY,ARRAYDESIGN_ID
from AE1__ASSAY__MAIN@dwprd
where ArrayDesign_ID is not null

--10 sec
Insert into a2_Sample(SampleID,Accession,Species) 
select SAMPLE_ID_KEY,SAMPLE_IDENTIFIER,SAMPLE_SPECIES 
from AE1__SAMPLE__MAIN@dwprd;

--TODO: there ARE some assays without ArrayDesignID, this information is lost
--3 sec
Insert into a2_AssaySample(SampleID,AssayID) 
select SAMPLE_ID_KEY,Assay_ID_key
from AE1__SAMPLE__MAIN@dwprd
where Assay_ID_key is not null and Assay_ID_key <> 0
and exists(select 1 from a2_Assay where AssayID = Assay_ID_key);

/********************************************************************************
execute a2_Property.sql
1 sec
********************************************************************************/

/** does not work
Insert into a2_PropertyValue(PropertyID,Name) 
select distinct p.PropertyID, a.EFV
from a2_Property p
join ATLAS@dwprd a on a.Ef = p.Name
**/

declare 
  cursor c1 is select PropertyID, Ae1TableName_Assay from a2_Property where Ae1TableName_Assay is not null;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    
    q := 'insert into a2_PropertyValue(PropertyID,Name) select distinct ' || rec.PropertyID || ', VALUE from ' || rec.Ae1TableName_Assay || '@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=' || rec.PropertyID || ' and pv.Name = v.Value );' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    --EXECUTE immediate q;
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
    q := 'insert into a2_PropertyValue(PropertyID,Name) select distinct ' || rec.PropertyID || ', VALUE from ' || rec.Ae1TableName_Sample || '@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=' || rec.PropertyID || ' and pv.Name = v.Value );' ;
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    --EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;

/*******************************************************************************
EXECUTE PropertyValue.sql
20 sec
********************************************************************************/

--100 sec
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

--244 sec
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

--1 sec
Insert into a2_ontology(OntologyID,Name, Source_URI, Description,Version)
select Ontology_ID_Key,Name, Source_URI, Description,Version
from ontology_sources@dwprd; 


--1 sec
Insert into a2_ontologyterm(OntologyTermID, OntologyID, Term , Accession, Description, ORIG_VALUE, orig_value_src ) 
select a2_ontologyterm_seq.nextval, Ontology_id_key, Term, Accession, Description, ORIG_VALUE, ORIG_VALUE_SRC
from (select distinct Ontology_id_key, Term, Accession, Description, ORIG_VALUE, ORIG_VALUE_SRC from ontology_annotation@dwprd);
   
--2 sec   
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
      where oa.sample_id_key is not null  )                      

--1 sec
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
      where oa.assay_id_key is not null  )  

---

/*******************************************************************************
disable trigger, constraints, indexes
*******************************************************************************/

--- 1400 sec
insert into a2_ExpressionAnalytics(ExpressionID, ExperimentID,DesignElementID,PropertyValueID,TSTAT,FPVAL,FPVALADJ, pvaladj)
select rownum, a.Experiment_ID_Key, a.DESIGNELEMENT_ID_KEY, pv.PropertyValueID, a.UPDN_TSTAT,a.FPVAL,a.FPVALADJ,a.UPDN_pvaladj
from Atlas@dwprd a
join a2_Property p on p.Name = a.EF
join a2_PropertyValue pv on pv.Name = a.EFV and pv.PropertyID = p.PropertyID
join a2_Experiment e on e.ExperimentID = a.Experiment_ID_Key --- <-- todo: some data missed
join a2_DesignElement de on de.DesignElementID = a.DESIGNELEMENT_ID_KEY

/*******************************************************************************
enaable trigger, constraints, indexes
*******************************************************************************/

commit work

