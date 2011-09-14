-- Estimated running time: 40 seconds

alter table a2_sample add "EXPERIMENTID" NUMBER;

create table tmp_sample_exp as
  select distinct ass.sampleid as sid, a.experimentid as eid from a2_assay a
  join a2_assaysample ass on a.assayid = ass.assayid;

alter table tmp_sample_exp add constraint "tse_pk" primary key (sid);

update a2_sample s
  set s.experimentid = (select eid from tmp_sample_exp where sid = s.sampleid);

drop table tmp_sample_exp;

ALTER TABLE "A2_SAMPLE"
ADD CONSTRAINT "FK_SAMPLE_EXPERIMENT"
    FOREIGN KEY ("EXPERIMENTID")
    REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE a2_sample MODIFY EXPERIMENTID NOT NULL;
