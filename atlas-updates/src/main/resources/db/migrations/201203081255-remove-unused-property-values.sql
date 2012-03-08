delete from A2_propertyvalue pv
where not exists
(
select 1
from A2_assaypv apv, A2_Assay ass
where ass.assayid = apv.assayid and apv.propertyvalueid = pv.propertyvalueid
    union all
select 1
from A2_samplepv apv, A2_Sample ass
where ass.sampleid = apv.sampleid and apv.propertyvalueid = pv.propertyvalueid
);