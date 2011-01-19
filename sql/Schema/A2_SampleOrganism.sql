/*******************************************************************************
select A2_SampleOrganism(sampleid) from a2_sample 
*******************************************************************************/
create or replace function A2_SampleOrganism(SampleID integer) 
RETURN varchar2
AS 
 result varchar2(255);
BEGIN
 select max(pv.name) into result
  from a2_samplepv spv
  join a2_propertyvalue pv on pv.propertyvalueid = spv.propertyvalueid
  join a2_property p on p.propertyid = pv.propertyid
  where p.name = 'organism'
  and spv.sampleid = A2_SampleOrganism.SampleID;
  
 return result; 
END;
/
exit;
/
