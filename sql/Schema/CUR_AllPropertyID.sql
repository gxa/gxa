/*******************************************************************************
PropertyValueIDs associated with assay or sample
******************************************************************************/
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
/
exit;
/