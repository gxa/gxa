delete from A2_propertyvalue pv
where not exists
(
select 1
from A2_assaypv apv
where apv.propertyvalueid = pv.propertyvalueid
    union all
select 1
from A2_samplepv spv
where spv.propertyvalueid = pv.propertyvalueid
);