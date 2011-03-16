alter table A2_ASSAYPV drop column ISFACTORVALUE;
alter table A2_SAMPLEPV drop column ISFACTORVALUE;

CREATE OR REPLACE VIEW "VWEXPERIMENTFACTORS" ("EXPERIMENTID", "PROPERTYID", "PROPERTYVALUEID") AS
select distinct a. ExperimentID, pv.PropertyID, pv.PropertyValueID
from a2_assayPV apv
join a2_propertyvalue pv on apv.propertyvalueid = pv.propertyvalueid
join a2_assay a on a.assayid = apv.assayid;

create or replace view CUR_AllPropertyID as
select apv.AssayID
      ,NULL SampleID
      ,apv.PropertyValueID PropertyValueID
      ,1 IsAssay
from a2_AssayPV apv 
UNION ALL
select ass.AssayID
       ,spv.SampleID 
       ,spv.PropertyValueID PropertyValueID
       ,0 IsAssay
from a2_SamplePV spv
join a2_assaysample ass on ass.sampleid = spv.sampleid;

create or replace view CUR_AssayProperty as 
select
e.Accession Experiment 
,a.Accession 
,p.Name property
,pv.Name value
from a2_Assay a
join a2_AssayPV apv on apv.AssayID = a.AssayID
join a2_PropertyValue pv on pv.PropertyValueID = apv.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID
join a2_Experiment e on e.ExperimentID = a.ExperimentID;

create or replace view CUR_SampleProperty as 
select
 (select MIN(e.Accession) 
  from a2_Experiment e
  join a2_Assay a on a.ExperimentID = e.ExperimentID
  join a2_AssaySample asa on asa.AssayID = a.AssayID
  where asa.SampleID = s.SampleID) Experiment
,s.Accession 
,p.Name property
,pv.Name value
from a2_Sample s
join a2_SamplePV apv on apv.SampleID = s.SampleID
join a2_PropertyValue pv on pv.PropertyValueID = apv.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID;

