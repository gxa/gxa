SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 500
set wrap off
SET FEEDBACK OFF
SET VERIFY OFF

select s.sample_id_key      || chr(9) ||
       s.sample_identifier  || chr(9) ||
       o.organismid
from ae1__sample__main s
join
(
select rownum organismid, species from (
  select distinct lower(
    case when value = 'UNKOWN' then 'UNKNOWN'
      when value is null       then 'UNKNOWN'
      else value
    end) species from ae2__gene_species__dm
  union
  select distinct lower(sample_species) species
  from ae1__sample__main)
) o
on o.species=lower(s.sample_species)
where assay_id_key is not null
AND EXISTS
  (select 1 from AE1__ASSAY__MAIN
    where AE1__ASSAY__MAIN.assay_id_key = s.assay_id_key
      AND AE1__ASSAY__MAIN.ARRAYDESIGN_ID is not null);

quit;
