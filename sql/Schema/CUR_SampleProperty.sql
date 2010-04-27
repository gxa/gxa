/*******************************************************************************
select * from CUR_SampleProperty
*******************************************************************************/
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
,NVL(apv.IsFactorValue,0) IsFactorValue
from a2_Sample s
join a2_SamplePV apv on apv.SampleID = s.SampleID
join a2_PropertyValue pv on pv.PropertyValueID = apv.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID;
/
exit;
/