-- This script serves to remove propertyAnnotated value white space duplicates - i.e. propertyAnnotated values that differ only by a white space.
-- We achieve this by replacing all double spaces with a single one and then removing this created propertyAnnotated value duplicates.
-- Note that before duplicates can be removed from A2_*propertyvalue table, in '%PV' tables that have FK constraints pointing to A2_*propertyvalue
-- the corresponding propertyvalueid's need to be re-pointed to the retained canonical entry in A2_*propertyvalue (c.f. step 5 below).
--
--  -- The tables that need this updated can be obtained by the following query:
--   select u.table_name, uc1.table_name as fk_table_name
--   from user_tables u, user_constraints uc, user_constraints uc1
--   where lower(u.table_name) like '%propertyvalue'
--   and u.table_name = uc.table_name
--   and uc.constraint_name like 'PK_%'
--   and uc1.constraint_name like 'FK_%'
--   and uc1.table_name like '%PV'
--   and uc1.r_constraint_name = uc.constraint_name;
-- TABLE_NAME                     FK_TABLE_NAME
-- ------------------------------ ------------------------------
-- A2_BIOENTITYPROPERTYVALUE      A2_BIOENTITYBEPV
-- A2_GENEPROPERTYVALUE           A2_GENEGPV
-- A2_PROPERTYVALUE               A2_ASSAYPV
-- A2_PROPERTYVALUE               A2_SAMPLEPV
--

-- ****** A2_GENEPROPERTYVALUE/A2_GENEGPV
-- 1. Find value duplicates (canonical_value contains value with all double spaces replaced with one space)
create table temp1 as 
select genepropertyid, REGEXP_REPLACE(value,'( ){2,}', ' ') as canonical_value, count(*) as count
from A2_genepropertyvalue group by genepropertyid, REGEXP_REPLACE(value,'( ){2,}', ' ') having count(*) > 1;
-- 2. Find propertyids and propertyAnnotated
create table temp2 as
select t1.genepropertyid, gpv.genepropertyvalueid, t1.canonical_value
from A2_genepropertyvalue gpv
join temp1 t1 on gpv.genepropertyid = t1.genepropertyid 
and t1.canonical_value = REGEXP_REPLACE(value,'( ){2,}', ' ');
--3. Find duplicates to be deleted (and references to them from A2_GENEGPV re-pointed to the first canonical entry)
create table temp3_dups as
select genepropertyvalueid from temp2 a
where rowid > (select min(rowid) from temp2 b where a.genepropertyid = b.genepropertyid and a.canonical_value = b.canonical_value);
--4. Find the first canonical entry (propertyvalueid corresponding to the min(rowid) corresponding to a given canonical value)
create table temp4_canon as
select genepropertyvalueid from temp2 a
where rowid = (select min(rowid) from temp2 b where a.genepropertyid = b.genepropertyid and a.canonical_value = b.canonical_value);
--5. Re-point A2_GENEGPV to the first canonical entries instead of duplicates
update A2_GENEGPV gpv
set genepropertyvalueid = (
  select t4.genepropertyvalueid 
  from temp4_canon t4, temp2 t2, temp2 t2a 
  where t4.genepropertyvalueid = t2.genepropertyvalueid and t2.canonical_value = t2a.canonical_value and t2a.genepropertyvalueid = gpv.genepropertyvalueid)
where genepropertyvalueid in (select genepropertyvalueid from temp3_dups); 
--6. Delete duplicates from A2_genepropertyvalue
delete from A2_genepropertyvalue where genepropertyvalueid in (select genepropertyvalueid from temp3_dups);
--7. Update canonical entries value in A2_genepropertyvalue
update A2_genepropertyvalue gpv
set value = (select t2.canonical_value from temp2 t2 where t2.genepropertyvalueid = gpv.genepropertyvalueid)
where genepropertyvalueid in (select genepropertyvalueid from temp4_canon);
--8. Drop session tables
drop table temp1;
drop table temp2;
drop table temp3_dups;
drop table temp4_canon;

-- ****** A2_BIOENTITYPROPERTYVALUE/A2_BIOENTITYBEPV
-- 1. Find value duplicates (canonical_value contains value with all double spaces replaced with one space)
create table temp1 as 
select bioentitypropertyid, REGEXP_REPLACE(value,'( ){2,}', ' ') as canonical_value, count(*) as count
from A2_bioentitypropertyvalue group by bioentitypropertyid, REGEXP_REPLACE(value,'( ){2,}', ' ') having count(*) > 1;
-- 2. Find propertyids and propertyAnnotated
create table temp2 as
select t1.bioentitypropertyid, gpv.bepropertyvalueid, t1.canonical_value
from A2_bioentitypropertyvalue gpv
join temp1 t1 on gpv.bioentitypropertyid = t1.bioentitypropertyid
and t1.canonical_value = REGEXP_REPLACE(value,'( ){2,}', ' ');
--3. Find duplicates to be deleted (and references to them from A2_BIOENTITYBEPV re-pointed to the first canonical entry)
create table temp3_dups as
select bepropertyvalueid from temp2 a
where rowid > (select min(rowid) from temp2 b where a.bioentitypropertyid = b.bioentitypropertyid and a.canonical_value = b.canonical_value);
--4. Find the first canonical entry (propertyvalueid corresponding to the min(rowid) corresponding to a given canonical value)
create table temp4_canon as
select bepropertyvalueid from temp2 a
where rowid = (select min(rowid) from temp2 b where a.bioentitypropertyid = b.bioentitypropertyid and a.canonical_value = b.canonical_value);
--5. Re-point A2_BIOENTITYBEPV to the first canonical entries instead of duplicates
update A2_BIOENTITYBEPV gpv
set bepropertyvalueid = (
  select t4.bepropertyvalueid
  from temp4_canon t4, temp2 t2, temp2 t2a 
  where t4.bepropertyvalueid = t2.bepropertyvalueid and t2.canonical_value = t2a.canonical_value and t2a.bepropertyvalueid = gpv.bepropertyvalueid)
where bepropertyvalueid in (select bepropertyvalueid from temp3_dups);
--6. Delete duplicates from A2_bioentitypropertyvalue
delete from A2_bioentitypropertyvalue where bepropertyvalueid in (select bepropertyvalueid from temp3_dups);
--7. Update canonical entries value in A2_bioentitypropertyvalue
update A2_bioentitypropertyvalue gpv
set value = (select t2.canonical_value from temp2 t2 where t2.bepropertyvalueid = gpv.bepropertyvalueid)
where bepropertyvalueid in (select bepropertyvalueid from temp4_canon);
--8. Drop session tables
drop table temp1;
drop table temp2;
drop table temp3_dups;
drop table temp4_canon;

-- ****** A2_PROPERTYVALUE/A2_ASSAYPV,A2_SAMPLEPV
-- 1. Find value duplicates (canonical_value contains value with all double spaces replaced with one space)
create table temp1 as 
select propertyid, REGEXP_REPLACE(name,'( ){2,}', ' ') as canonical_value, count(*) as count
from A2_propertyvalue group by propertyid, REGEXP_REPLACE(name,'( ){2,}', ' ') having count(*) > 1;
-- 2. Find propertyids and propertyAnnotated
create table temp2 as
select t1.propertyid, gpv.propertyvalueid, t1.canonical_value
from A2_propertyvalue gpv
join temp1 t1 on gpv.propertyid = t1.propertyid
and t1.canonical_value = REGEXP_REPLACE(name,'( ){2,}', ' ');
--3. Find duplicates to be deleted (and references to them from A2_ASSAYPV, A2_SAMPLEPV re-pointed to the first canonical entry)
create table temp3_dups as
select propertyvalueid from temp2 a
where rowid > (select min(rowid) from temp2 b where a.propertyid = b.propertyid and a.canonical_value = b.canonical_value);
--4. Find the first canonical entry (propertyvalueid corresponding to the min(rowid) corresponding to a given canonical value)
create table temp4_canon as
select propertyvalueid from temp2 a
where rowid = (select min(rowid) from temp2 b where a.propertyid = b.propertyid and a.canonical_value = b.canonical_value);
--5. Re-point A2_ASSAYPV to the first canonical entries instead of duplicates
update A2_ASSAYPV gpv
set propertyvalueid = (
  select t4.propertyvalueid
  from temp4_canon t4, temp2 t2, temp2 t2a 
  where t4.propertyvalueid = t2.propertyvalueid and t2.canonical_value = t2a.canonical_value and t2a.propertyvalueid = gpv.propertyvalueid)
where propertyvalueid in (select propertyvalueid from temp3_dups);
--5. Re-point A2_ASSAYPV to the first canonical entries instead of duplicates
update A2_SAMPLEPV gpv
set propertyvalueid = (
  select t4.propertyvalueid
  from temp4_canon t4, temp2 t2, temp2 t2a
  where t4.propertyvalueid = t2.propertyvalueid and t2.canonical_value = t2a.canonical_value and t2a.propertyvalueid = gpv.propertyvalueid)
where propertyvalueid in (select propertyvalueid from temp3_dups);
--6. Delete duplicates from A2_propertyvalue
delete from A2_propertyvalue where propertyvalueid in (select propertyvalueid from temp3_dups);
--7. Update canonical entries value in A2_propertyvalue
update A2_propertyvalue gpv
set name = (select t2.canonical_value from temp2 t2 where t2.propertyvalueid = gpv.propertyvalueid)
where propertyvalueid in (select propertyvalueid from temp4_canon);
--8. Drop session tables
drop table temp1;
drop table temp2;
drop table temp3_dups;
drop table temp4_canon;

-- ***** TESTS THAT THIS SCRIPT HAS DONE ITS JOB CORRECTLY (c.f. Ticket #2974) - all of the queries below should return 0 rows:
select 'Any rows returned in the following 4 selects means there was an error; if so, you need to investigate.' as script_quality_control from dual;

select propertyid, REGEXP_REPLACE(name,'( ){2,}', ' '), count(*)
from A2_propertyvalue group by propertyid, REGEXP_REPLACE(name,'( ){2,}', ' ') having count(*) > 1;

select genepropertyid, REGEXP_REPLACE(value,'( ){2,}', ' '), count(*)
from A2_genepropertyvalue group by genepropertyid, REGEXP_REPLACE(value,'( ){2,}', ' ') having count(*) > 1;

select BIOENTITYPROPERTYID, REGEXP_REPLACE(value,'( ){2,}', ' '), count(*)
from A2_BIOENTITYPROPERTYVALUE group by BIOENTITYPROPERTYID, REGEXP_REPLACE(value,'( ){2,}', ' ') having count(*) > 1;

-- Also, none of FK_GENEGPV_GPV, FK_BIOENTITYBEPV_BIOENTITYPV and FK_ASSAYPV_PROPERTY should be broken after this script is complete
select * from user_constraints where constraint_name in ('FK_GENEGPV_GPV', 'FK_BIOENTITYBEPV_BIOENTITYPV', 'FK_ASSAYPV_PROPERTY')
and (status != 'ENABLED' OR INVALID is not null);

exit;