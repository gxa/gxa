/*******************************************************************************
*                         MIGRATION PROCS                                      *
*******************************************************************************/

call ATLASMGR.DisableConstraints()
call ATLASMGR.EnableTriggers()

create or replace
function get_propertyvalueid (ef varchar2, efv varchar2)
  return integer
as
 r integer;  
 begin
  select PropertyValueID into r
  from a2_PropertyValue pv
  join a2_Property p on p.PropertyID = pv.PropertyID
  where p.name = ef
  and pv.name = efv;
  
  return r;
  
 end;
--
create or replace
function get_propertyvalueid_p (propertyid integer, efv varchar2)
  return integer
as
 r integer;  
 begin
  select PropertyValueID into r
  from a2_PropertyValue pv
  where pv.propertyid = get_propertyvalueid_p.propertyid
  and pv.name = efv;
  
  return r;
  
 end;
--
create or replace
function get_samplepvid (
  SAMPLEID varchar2
  ,VALUE varchar2
  ,ORIG_VALUE_SRC varchar2
) return INTEGER
as
  r integer;
begin
  
  select spv.SamplePVID into r
  from a2_SamplePV spv
  join a2_PropertyValue pv on pv.PropertyValueID = spv.PropertyValueID
  join a2_property p on p.PropertyID = pv.PropertyID
  where p.ae1tablename_sample = get_samplepvid.ORIG_VALUE_SRC
  and pv.name = get_samplepvid.value
  and spv.SampleID = get_samplepvid.SampleID;
  
  --Insert into loggy (msg) select 'SampleID=' || SAMPLEID || ' value=' || VALUE || ' ORIG_VALUE_SRC=' || ORIG_VALUE_SRC from dual;
  
  --select 22 into r from dual;
  
  return r;
end;
--
create or replace
function get_assaypvid (
  ASSAYID varchar2
  ,VALUE varchar2
  ,ORIG_VALUE_SRC varchar2
) return INTEGER
as
  r integer;
begin
  
  select MIN(spv.AssayPVID) into r
  from a2_AssayPV spv
  join a2_PropertyValue pv on pv.PropertyValueID = spv.PropertyValueID
  join a2_property p on p.PropertyID = pv.PropertyID
  where p.ae1tablename_assay = get_assaypvid.ORIG_VALUE_SRC
  and pv.name = get_assaypvid.value
  and spv.AssayID = get_assaypvid.AssayID;
  
  return r;
end;
--

create or replace
function get_ontologytermid (
  ONTOLOGYID varchar2
  ,ACCESSION varchar2
) return INTEGER
as
  r integer;
begin
  
  select ontologytermid into r 
  from a2_OntologyTerm 
  where accession = get_ontologytermid.accession 
  and ontologyid = get_ontologytermid.ontologyid; 
  
  
  --select 11 into r from dual; 

  return r;
end;

/**

**/

create or replace
function get_GenePropertyValueID(
  GenePropertyID integer
 ,Value varchar2
) return INTEGER
as
  r INTEGER;
begin
  select GenePropertyValueID into r
  from a2_GenePropertyValue
  where GenePropertyID = get_GenePropertyValueID.GenePropertyID
  and Value = get_GenePropertyValueID.Value;

  return r;
end;

select * from a2_genepropertyvalue

select * from loggy
truncate table loggy

delete from a2_expressionAnalytics where not exists(select 1 from a2_experiment where a2_experiment.ExperimentID = a2_expressionAnalytics.ExperimentID)

delete from a2_expressionanalytics a where exists(select 1 from a2_expressionanalytics a1 where a1.ExperimentID = a.ExperimentID 
and a1.DesignElementID = a.DesignElementID
and a1.PropertyValueID = a.PropertyValueID
and a1.expressionid < a.expressionid) 

create table a2 as select MIN(expressionid),  Designelementid,propertyvalueid,expressionid, MIN(TSTAT) ,

delete from a2_expressionanalytics NOLOGGING where PropertyValueID is null;
COMMIT


ALTER TABLE a2_expressionanalytics drop constraint UQ_ANALYTICS;

select Designelementid,propertyvalueid,expressionid,count(1)
from a2_expressionanalytics
group by Designelementid,propertyvalueid,expressionid
having count(1) > 1

select * from a2_expressionanalytics
where not exists(select 1 from a2_experiment where a2_experiment.ExperimentID = a2_expressionanalytics.ExperimentID )

select distinct experimentid from a2_expressionanalytics
where not exists(select 1 from a2_experiment where a2_experiment.ExperimentID = a2_expressionanalytics.ExperimentID )

delete from a2_expressionanalytics
where not exists(select 1 from a2_experiment where a2_experiment.ExperimentID = a2_expressionanalytics.ExperimentID )

select * from a2_Assayp

call ATLASMGR.EnableConstraints()

commit

  ALTER TABLE "A2_EXPRESSIONANALYTICS" 
  ADD CONSTRAINT "UQ_ANALYTICS" 
    UNIQUE(ExperimentID,DesignElementID,PropertyValueID) 
    ENABLE; 

create index idx_1 on a2_expressionanalytics(experimentID)

delete from a2_expressionanalytics
where not exists(select 1 from a2_designelement where a2_expressionanalytics.ExperimentID = a2_designelement.designelementID )

select 'DROP ' || OBJECT_TYPE || ' ' || OBJECT_NAME || ';' from user_objects; 


delete from a2_expressionanalytics where PropertyValueID is null

commit

Alter table a2_expressionanalytics drop constraint UQ_ANALYTICS
Drop index 

select count(1) from a2_expressionanalytics

call ATLASMGR.EnableConstraints()
delete from a2_expressi


select * from a2_SamplePV
select * from a2_AssayPV
Update a2_AssayPV set IsFactorValue = 1

Commit


ExperimentID,DesignElementID,PropertyValueID

ALTER TABLE a2_expressionAnalytics REBUILD INDEX UQ_ANALYTICS
ALTER INDEX UQ_ANALYTICS REBUILD

create table loggy(msg varchar(2555))

select * from a2_samplePVontology
delete from a2_samplePVontology 
where not exists (select 1 from a2_ontologyterm where a2_ontologyterm.ontologytermid =  a2_samplePVontology.ontologytermid)

select count(1) from a2_samplepvontology

call AtlasMGR.EnableConstraints()

select * from a2_property
select * from a2_samplepvontology

delete from a2_SamplePV pv where not exists (select 1 from a2_Sample s where s.SampleID = pv.SampleID)
delete from a2_SamplePV pv where not exists (select 1 from a2_PropertyValue s where s.PropertyValueID = pv.PropertyValueID)

select * from user_objects

DROP TABLE A2_EXPRESSIONVALUE;                                                                                                                             
DROP TABLE A2_EXPERIMENT;                                                                                                                                  
DROP INDEX PK_EXPERIMENT;                                                                                                                                  
DROP INDEX IDX_EXPERIMENT_ACCESSION;                                                                                                                       
DROP TABLE A2_ASSAY;                                                                                                                                       
DROP INDEX PK_ASSAY;                                                                                                                                       
DROP INDEX UQ_ASSAY_ACCESSION;                                                                                                                             
DROP TABLE A2_ORGANISM;                                                                                                                                    
DROP INDEX PK_ORGANISM;                                                                                                                                    
DROP INDEX UQ_ORGANISM_NAME;                                                                                                                               
DROP TABLE A2_GENE;                                                                                                                                        
DROP INDEX PK_GENE;                                                                                                                                        
DROP TABLE A2_ARRAYDESIGN;                                                                                                                                 
DROP INDEX PK_ARRAYDESIGN;                                                                                                                                 
DROP INDEX IDX_ARRAYDESIGN_ACCESSION;                                                                                                                      
DROP INDEX UQ_ARRAYDESIGN_NAME;                                                                                                                            
DROP TABLE A2_DESIGNELEMENT;                                                                                                                               
DROP INDEX PK_DESIGNELEMENT;                                                                                                                               
DROP INDEX UQ_DESIGNELEMENT_ACCESSION;   

DROP TABLE A2_ORGANISM;                                                                                                                                    
DROP INDEX PK_ORGANISM;                                                                                                                                    
DROP INDEX UQ_ORGANISM_NAME;                                                                                

call ATLASLDR.LOAD_PROGRESS('E-GEOD-2431','netcdf','done','experiment')

select * from LOAD_MONITOR

select * from a2_AssayPV
