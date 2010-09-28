/*******************************************************************************
PropertyValueIDs associated with assay or sample
******************************************************************************/
create or replace view CUR_AllPropertyID as
select apv.AssayID
      ,NULL SampleID
      ,apv.PropertyValueID PropertyValueID
      ,NVL(apv.IsFactorValue,0) IsFactorValue
      ,1 IsAssay
from a2_AssayPV apv 
UNION ALL
select ass.AssayID
       ,spv.SampleID 
       ,spv.PropertyValueID PropertyValueID
       ,NVL(spv.IsFactorValue,0) IsFactorValue
       ,0 IsAssay
from a2_SamplePV spv
join a2_assaysample ass on ass.sampleid = spv.sampleid;
/
exit;
/