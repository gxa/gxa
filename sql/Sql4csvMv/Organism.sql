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


select rownum || chr(9) ||
       species from (
  select distinct lower(
    case when value = 'UNKOWN' then 'UNKNOWN'
      when value is null       then 'UNKNOWN'
      else value
    end) species from ae2__gene_species__dm
  union
  select distinct lower(sample_species) species
  from ae1__sample__main);

quit;
