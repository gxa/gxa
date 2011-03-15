/*******************************************************************************
CUR_AssayProperty
*******************************************************************************/
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
/
exit;
/