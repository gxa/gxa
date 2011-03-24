/* PREPARE A2_SAMPLE TABLE*/
alter table A2_SAMPLE
   add (ORGANISMID  NUMBER(22,0));

alter table A2_SAMPLE
   add constraint "FK_SAMPLE_ORGANISM"
   foreign key ("ORGANISMID")
   references "A2_ORGANISM" ("ORGANISMID");

update A2_SAMPLE
   set ORGANISMID = to_number(SPECIES);

/* PREPARE A2_ORGANISM TABLE */
create table A2_ORGANISM_TMP
as (
  select distinct lower(A2_SampleOrganism(s.sampleid)) as OrganismName
  from A2_SAMPLE s
  where s.species is null
);

delete from A2_ORGANISM_TMP
  where exists(
     select org.name from A2_ORGANISM org
     where lower(org.name) = OrganismName
  );

insert into A2_ORGANISM(ORGANISMID, NAME) (
   select A2_ORGANISM_SEQ.nextval, OrganismName
   from A2_ORGANISM_TMP
);

drop table A2_ORGANISM_TMP;

/* REPLACE NULL ORGANISMID VALUES IN A2_SAMPLE */
update A2_SAMPLE
   set ORGANISMID = (
       select ORGANISMID from A2_ORGANISM org
       where lower(org.name) = lower(A2_SampleOrganism(SAMPLEID))
   )
where SPECIES is NULL;

-- apply CUR_ExperimentProperty.sql

-- apply PKG_ATLASLDR.sql

/* REMOVE UNUSED */
alter table A2_SAMPLE
   drop column SPECIES;

drop function A2_SampleOrganism;

CREATE OR REPLACE VIEW "VWSAMPLE" ("SAMPLEID", "ACCESSION", "SPECIES", "CHANNEL") AS select s.SAMPLEID,s.ACCESSION,o.NAME,s.CHANNEL from a2_sample s, a2_organism o where s.organismid=o.organismid;

CREATE OR REPLACE VIEW vwCheck as
 select 'Experiments w/o assay' as Name
        , Accession 
            from a2_Experiment 
           where not exists (select 1 
                             from a2_Assay 
                             where a2_Assay.ExperimentID = a2_Experiment.ExperimentID 
                             and 1=1)
 UNION ALL
 select 'Assays w/o sample' as Name
        , Accession 
            from a2_Assay
            where not exists (select 1 
                              from a2_AssaySample
                              where a2_AssaySample.AssayID = a2_Assay.AssayID 
                              and 1=1)
 UNION ALL
 select 'Samples w/o assay' as Name
        ,Accession
            from a2_Sample
            where not exists (select 1 
                              from a2_AssaySample
                              where a2_AssaySample.SampleID = a2_Sample.SampleID 
                              and 1=1)
 UNION ALL                             
 select 'Experiments w/o factor' as Name
        , Accession
            from a2_Experiment
            where not exists (select 1 
                              from vwExperimentFactors
                              where vwExperimentFactors.ExperimentID = a2_Experiment.ExperimentID 
                              and 1=1)
 UNION ALL                             
 select  'Assay w/o properties' as Name
         , Accession
            from a2_Assay
            where not exists (select 1 
                              from a2_AssayPV
                              where a2_AssayPV.AssayID = a2_Assay.AssayID 
                              and 1=1)
 UNION ALL
 select 'Sample w/o properties' as Name
        , Accession
            from a2_Sample
            where not exists (select 1 
                              from a2_SamplePV
                              where a2_SamplePV.SampleID = a2_Sample.SampleID 
                              and 1=1)
 UNION ALL                             
 select 'Sample w/o properties' as Name
        , Accession
            from a2_Assay
;
/


