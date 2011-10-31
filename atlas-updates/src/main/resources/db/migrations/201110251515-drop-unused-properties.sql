delete from a2_propertyvalue pv where not exists (
select apv.propertyvalueid from a2_assaypv apv where apv.propertyvalueid = pv.propertyvalueid
union
select spv.propertyvalueid from a2_samplepv spv where spv.propertyvalueid = pv.propertyvalueid
);

delete from a2_property p
where not exists (
select * from a2_propertyvalue pv where p.propertyid = pv.propertyid
);
