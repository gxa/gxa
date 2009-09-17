--select * from a2_Spec
Insert into a2_Spec (Name) 
select distinct Value from ae2__Gene_Species__dm@dwprd;

COMMIT WORK;

--delete from a2_Gene

--select * from a2_Gene
Insert into a2_Gene(GeneID, SpecID, Identifier, Name)
select a.GENE_ID_KEY, s.SpecID, a.GENE_IDENTIFIER, a.GENE_NAME
from AE2__GENE__MAIN@dwprd a
left outer join AE2__GENE_SPECIES__DM@dwprd gs on gs.Gene_ID_KEY = a.GENE_ID_KEY
join a2_Spec s on s.name = gs.Value;

---a2_GeneProperty_data

declare 
  cursor c1 is select GenePropertyID, Name, Ae2TableName from a2_GeneProperty;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Name);
    
    q := 'insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, ' || rec.GenePropertyID || ', VALUE from ' || rec.Ae2TableName || '@dwprd' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    --EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;

--delete from a2_ArrayDesign
--a2_ArrayDesign
Insert into A2_ArrayDesign ( ArrayDesignID,Accession,Type,Name,Provider) 
select ARRAYDESIGN_ID_KEY,ARRAYDESIGN_IDENTIFIER,ARRAYDESIGN_TYPE,ARRAYDESIGN_NAME,ARRAYDESIGN_PROVIDER
from AE2__ARRAYDESIGN@dwprd

--delete from a2_DesignElement
--a2_DesignElement
Insert into A2_DesignElement(DesignElementID, ArrayDesignID, GeneID, Accession,Type,Name,IsControl)
select DESIGNELEMENT_ID_KEY,ARRAYDESIGN_ID,GENE_ID_KEY,DESIGNELEMENT_IDENTIFIER,DESIGNELEMENT_TYPE,DESIGNELEMENT_NAME,DESIGNELEMENT_ISCONTROL
from AE2__DESIGNELEMENT__MAIN@dwprd
where GENE_ID_KEY is not null;

--select count(1) from a2_DesignElement;

--COMMIT WORK;
Insert into a2_Experiment (ExperimentID,Accession,Description,Performer,Lab )
select e.Experiment_ID_Key, e.Experiment_ACCESSION, e.experiment_description, e.experiment_performer, e.experiment_lab
from AE1__EXPERIMENT__MAIN@dwprd e;

Insert into A2_Assay(AssayID
  , Accession 
  , ExperimentID
  , ArrayDesignID) 
select ASSAY_ID_KEY,ASSAY_IDENTIFIER,EXPERIMENT_ID_KEY,ARRAYDESIGN_ID
from AE1__ASSAY__MAIN@dwprd
where ArrayDesign_ID is not null

Insert into a2_Sample(SampleID,Accession,Species) 
select SAMPLE_ID_KEY,SAMPLE_IDENTIFIER,SAMPLE_SPECIES 
from AE1__SAMPLE__MAIN@dwprd;

Insert into a2_AssaySample(SampleID,AssayID) 
select SAMPLE_ID_KEY,Assay_ID_key
from AE1__SAMPLE__MAIN@dwprd;

Insert into a2_PropertyValue(PropertyID,Name) 
select distinct p.PropertyID, a.EFV
from a2_Property p
join ATLAS@dwprd a on a.Ef = p.Name

select count(1) from a2_PropertyValue

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

--select count(1) from a2_PropertyValue
select count(1) from a2_AssayPropertyValue

delete from t;

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
    q := 'insert into a2_SamplePropertyValue(SampleID,PropertyValueID) select v.sample_id_key, ' || rec.PropertyValueID || ' from ' || rec.Ae1TableName_Sample || '@dwprd v where v.Value = ''' || REPLACE(rec.Name,'''','''''') || ''' and v.sample_id_key in (select SampleID from a2_Sample);' ;
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    --dbms_output.put_line(q);
    insert into t(sql) select q from dual;
    --EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;

insert into a2_ExpressionAnalytics(ExperimentID,DesignElementID,PropertyValueID,TSTAT,FPVAL,FPVALADJ)
select a.EXPERIMENT_ID_KEY, a.DESIGNELEMENT_ID_KEY, pv.PropertyValueID, a.UPDN_TSTAT,a.FPVAL,a.FPVALADJ
from Atlas@dwprd a
join a2_Property p on p.Name = a.EF
join a2_PropertyValue pv on pv.Name = a.EFV

insert into a2_ExpressionAnalytics(ExperimentID,DesignElementID,PropertyValueID,TSTAT,FPVAL,FPVALADJ)
select a.ExperimentID, a.DESIGNELEMENTID, pv.PropertyValueID, a.TSTAT,a.FPVAL,a.FPVALADJ
from Atlas a
join a2_Property p on p.Name = a.FP
join a2_PropertyValue pv on pv.Name = a.FPV

create index idx_atlas on Atlas(FP,FPV)

select * from atlas


select count(1) from a2_ExpressionAnalytics

select 1 from dual

insert into a2_SamplePropertyValue(SampleID,PropertyValueID) select v.sample_id_key, 1853 from AE1__SAMPLE_GENOTYPE__DM@dwprd v where v.Value = 'ATF3(-/-)' and v.sample_id_key in (select SampleID from a2_Sample);

select * from a2_SamplePropertyValue

COMMIT WORK;

Select REPLACE('asd','s','ff') from DUAL

insert into a2_AssayPropertyValue(AssayID,PropertyValueID) select v.Assay_id_key, 1853 from AE1__ASSAY_GENOTYPE__DM@dwprd v where v.Value = 'ATF3(-/-)'

select * from a2_AssayPropertyValue
select count(1) from a2_AssayPropertyValue
delete from a2_AssayPropertyValue


select *
from ATLAS@dwprd;

select * from AE1__SAMPLE__MAIN@dwprd;


select distinct ARRAYDESIGN_ID
from AE1__ASSAY__MAIN@dwprd
where ArrayDesign_ID is not null
and ARRAYDESIGN_ID not in (select ArrayDesignID from a2_ArrayDesign)

select * from AE2__ARRAYDESIGN@dwprd where ArrayDesign_ID_Key = 130297520
select * from a2_ArrayDesign where ArrayDesignID = 130297520
select count(1) from a2_ArrayDesign
select count(1) from AE2__ARRAYDESIGN@dwprd
select * from a2_ArrayDesign
select * from AE2__ARRAYDESIGN@dwprd




COMMIT WORK;

select count(1) from a2_GenePropertyValue

select * from a2_Spec


declare 
  cursor c1 is select PropertyID, Ae1TableName_Assay from a2_Property where Ae1TableName_Assay is not null;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    
    q := 'insert into a2_PropertyValue(PropertyID,Name) select ' || rec.PropertyID || ', VALUE from ' || rec.Ae1TableName_Assay || '@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=' || rec.PropertyID || ' and pv.Name = v.Value )' ;
    
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    dbms_output.put_line(q);
    --EXECUTE immediate q;
 end loop;
 dbms_output.put_line('end');
end;;
;

select * from t

declare 
  cursor c1 is select PropertyValueID, Ae1TableName_Sample, pv.Name 
  from a2_Property p
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where Ae1TableName_Sample is not null /*and PropertyValueID between 0 and 30*/ /*and Ae1TableName_Sample not like '%AE1__SAMPLE_AGE__DM%'*/
  order by PropertyValueID;
  q varchar(8000);
begin
 dbms_output.put_line('begin');
 for rec in c1
 loop
    --dbms_output.put_line('--' || rec.Ae1TableName_Assay);
    q := 'insert into a2_SamplePropertyValue(SampleID,PropertyValueID) select v.sample_id_key, ' || rec.PropertyValueID || ' from ' || rec.Ae1TableName_Sample || '@dwprd v where v.Value = ''' || REPLACE(rec.Name,'''','''''') || ''' and v.sample_id_key in (select SampleID from a2_Sample);' ;
    --Insert into a2_GenePropertyValue(GenePropertyID,Name,Ae2TableName) 
    --dbms_output.put_line(q);
    --EXECUTE immediate q;
    Insert into t (sql) select q from dual;
 end loop;
 dbms_output.put_line('end');
end;

Select count(1) from a2_Gene
Select count(1) from a2_expressionvalue;
Select count(1) from a2_expressionanalytics;

ALTER INDEX SYS_C008033 REBUILD
A2_IDX_EXPRESSION_FV
SYS_C008033



