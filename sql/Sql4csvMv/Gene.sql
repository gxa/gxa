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

select g.gene_id_key     || chr(9) ||
       o.organismid      || chr(9) ||
       g.gene_identifier || chr(9) ||
       g.gene_name
from ae2__gene__main g
left outer join ae2__gene_species__dm gs
on g.gene_id_key=gs.gene_id_key
join (
select rownum organismid, species from (
  select distinct lower(
    case when value = 'UNKOWN' then 'UNKNOWN'
      when value is null       then 'UNKNOWN'
      else value
    end) species from ae2__gene_species__dm
  union
  select distinct lower(sample_species) species
  from ae1__sample__main)
) o on o.species=lower(
    case when gs.value = 'UNKOWN' then 'UNKNOWN'
      when gs.value is null       then 'UNKNOWN'
      else gs.value
    end)
where gs.value is not null or g.gene_id_key=0;

quit;
