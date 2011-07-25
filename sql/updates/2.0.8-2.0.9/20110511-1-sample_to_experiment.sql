alter table a2_sample add "EXPERIMENTID" NUMBER;

update a2_sample s set s.experimentid = (
  select distinct a.experimentid from a2_assay a, a2_assaysample ass
  where a.assayid=ass.assayid
  and ass.sampleid = s.sampleid
);

ALTER TABLE "A2_SAMPLE"
ADD CONSTRAINT "FK_SAMPLE_EXPERIMENT"
    FOREIGN KEY ("EXPERIMENTID")
    REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE a2_sample MODIFY EXPERIMENTID NOT NULL;
