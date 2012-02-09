-- First trim all property values that don't already exist in A2_propertyvalue in trimmed form
update A2_propertyvalue pv set pv.name = trim(pv.name) where length(trim(pv.name)) <> length(pv.name)
and not exists (select 1 from A2_propertyvalue where name = trim(pv.name) and propertyid = pv.propertyid);

-- Now re-point all assays and samples from all the remaining non-trimmed propertyvalues
-- to their (already existing) trimmed equivalents
update A2_assaypv apv
set apv.propertyvalueid = (
  select pv1.propertyvalueid from A2_propertyvalue pv1, A2_propertyvalue pv2
  where pv1.name = trim(pv2.name) and pv2.propertyvalueid = apv.propertyvalueid and pv2.propertyid = pv1.propertyid)
where apv.propertyvalueid in (select propertyvalueid from A2_propertyvalue where length(trim(name)) <> length(name));

update A2_samplepv apv
set apv.propertyvalueid = (
  select pv1.propertyvalueid from A2_propertyvalue pv1, A2_propertyvalue pv2
  where pv1.name = trim(pv2.name) and pv2.propertyvalueid = apv.propertyvalueid and pv2.propertyid = pv1.propertyid)
where apv.propertyvalueid in (select propertyvalueid from A2_propertyvalue where length(trim(name)) <> length(name));

-- Finally remove now obsolete non-trimmed property values (from A2_propertyvalue and via FK constraints from A2_assaypv and A2_samplepv)
delete from A2_propertyvalue pv where length(trim(name)) <> length(name) and exists (select name from A2_propertyvalue where name = trim(pv.name));