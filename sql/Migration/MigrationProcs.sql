/*******************************************************************************
*                         MIGRATION PROCS                                      *
*******************************************************************************/
create or replace
function get_propertyvalueid (ef varchar2, efv varchar2)
  return integer
as
 r integer;  
begin
  select PropertyValueID into r
  from a2_PropertyValue pv
  join a2_Property p on p.PropertyID = pv.PropertyID
  where p.name = ef
  and pv.name = efv;
  
  return r;
exception
  when others then
    return 0;

end;
/
--
create or replace
function get_propertyvalueid_p (propertyid integer, efv varchar2)
  return integer
as
 r integer;  
begin
  select PropertyValueID into r
  from a2_PropertyValue pv
  where pv.propertyid = get_propertyvalueid_p.propertyid
  and pv.name = efv;
  
  return r;
exception
  when others then
    return 0;
end;
/
--
create or replace
function get_samplepvid (
  SAMPLEID varchar2
  ,VALUE varchar2
  ,ORIG_VALUE_SRC varchar2
) return INTEGER
as
  r integer;
begin
  
  select spv.SamplePVID into r
  from a2_SamplePV spv
  join a2_PropertyValue pv on pv.PropertyValueID = spv.PropertyValueID
  join a2_property p on p.PropertyID = pv.PropertyID
  where p.ae1tablename_sample = get_samplepvid.ORIG_VALUE_SRC
  and pv.name = get_samplepvid.value
  and spv.SampleID = get_samplepvid.SampleID;
  
  return r;
exception
  when others then
    return 0;
end;
/
--
create or replace
function get_assaypvid (
  ASSAYID varchar2
  ,VALUE varchar2
  ,ORIG_VALUE_SRC varchar2
) return INTEGER
as
  r integer;
begin
  
  select MIN(spv.AssayPVID) into r
  from a2_AssayPV spv
  join a2_PropertyValue pv on pv.PropertyValueID = spv.PropertyValueID
  join a2_property p on p.PropertyID = pv.PropertyID
  where p.ae1tablename_assay = get_assaypvid.ORIG_VALUE_SRC
  and pv.name = get_assaypvid.value
  and spv.AssayID = get_assaypvid.AssayID;

  if(r is null) then
    r := 0;
  end if;

  return r;
exception  
  when others then
    return 0;
end;
/
--
create or replace
function get_ontologytermid (
  ONTOLOGYID varchar2
  ,ACCESSION varchar2
) return INTEGER
as
  r integer;
begin
  
  select ontologytermid into r 
  from a2_OntologyTerm 
  where accession = get_ontologytermid.accession 
  and ontologyid = get_ontologytermid.ontologyid; 
  
  return r;
exception  
  when others then
    return 0;
end;
/
---
create or replace
function get_GenePropertyValueID (
  GenePropertyID integer
 ,Value varchar2
) return INTEGER
as
  r INTEGER;
begin
  select GenePropertyValueID into r
  from a2_GenePropertyValue
  where GenePropertyID = get_GenePropertyValueID.GenePropertyID
  and Value = get_GenePropertyValueID.Value;

  return r;
exception  
  when others then
    return 0;  
end;
/
