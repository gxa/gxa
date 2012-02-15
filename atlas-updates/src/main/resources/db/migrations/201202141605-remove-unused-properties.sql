delete from A2_property p
where not exists
(
select 1
from A2_assaypv apv, A2_Assay ass, A2_propertyvalue pv
where ass.assayid = apv.assayid and apv.propertyvalueid = pv.propertyvalueid and pv.propertyid = p.propertyid
    union all
select 1
from A2_samplepv apv, A2_Sample ass, A2_propertyvalue pv
where ass.sampleid = apv.sampleid and apv.propertyvalueid = pv.propertyvalueid and pv.propertyid = p.propertyid
);
