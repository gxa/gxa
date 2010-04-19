/*******************************************************************************
CUR_SampleProperty
*******************************************************************************/
create or replace view CUR_SampleProperty as 
select a.Accession 
,p.Name property
,pv.Name value
,NVL(apv.IsFactorValue,0) IsFactorValue
from a2_Sample a
join a2_SamplePV apv on apv.SampleID = a.SampleID
join a2_PropertyValue pv on pv.PropertyValueID = apv.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID