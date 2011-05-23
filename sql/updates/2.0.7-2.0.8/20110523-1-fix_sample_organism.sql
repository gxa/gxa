/* CREATE TEMPORARY SAMPLE - ORGANISM ASSOCIATION */
create table A2_SAMPLE_ORGANISM_TMP
as (
   select spv.sampleid, o.name, o.organismid from a2_samplepv spv
   join a2_propertyvalue pv on pv.propertyvalueid = spv.propertyvalueid
   join a2_property p on p.propertyid = pv.propertyid
   join a2_organism o on lower(o.name) = lower(pv.name)
   where p.name = 'organism'
);

alter table A2_SAMPLE_ORGANISM_TMP
  add constraint pk_a2_smplorg_tmp_sampleid
  primary key (sampleid);

update ( select s1.organismid, s2.organismid as orgid
         from A2_SAMPLE s1
         inner join A2_SAMPLE_ORGANISM_TMP s2 ON s1.sampleid = s2.sampleid
) o set o.organismid = o.orgid;

drop table A2_SAMPLE_ORGANISM_TMP;

-- apply fix_sample_organism.sql

-- apply PKG_ATLASLDR.sql