/*******************************************************************************
select * from CUR_ExperimentProperty where accession = 'E-GEOD-5258'

select * from a2_experiment

*******************************************************************************/
create or replace view CUR_ExperimentProperty as 
select distinct
e.Accession
,e.Description
,(select count(1) from a2_assay where a2_assay.Experimentid = e.experimentid) NumberOfAssays
, s.species SampleOrganisms
, e.LoadDate ExperimentLoadDate
, p.Name ExperimentalFactor
, pv.Name  ExperimentalFactorValue
from a2_experiment e
join a2_assay a on a.experimentid = e.experimentid
join a2_assaysample ass on ass.assayid = a.assayid 
join a2_sample s on s.sampleid = ass.sampleid
join CUR_AllPropertyID a_p on a_p.assayid = a.assayid or a_p.sampleid = s.sampleid
join a2_propertyvalue pv on pv.propertyvalueid = a_p.propertyvalueid
join a2_property p on p.propertyid = pv.propertyid
/
exit;
/