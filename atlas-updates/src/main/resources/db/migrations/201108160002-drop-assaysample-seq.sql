DROP TRIGGER "A2_ASSAYSAMPLE_INSERT";

DROP SEQUENCE "A2_ASSAYSAMPLE_SEQ";

ALTER TABLE "A2_ASSAYSAMPLE" DROP CONSTRAINT PK_ASSAYSAMPLE;

ALTER TABLE "A2_ASSAYSAMPLE" DROP CONSTRAINT UQ_ASSAYSAMPLE;

ALTER TABLE "A2_ASSAYSAMPLE" DROP COLUMN ASSAYSAMPLEID;

ALTER TABLE "A2_ASSAYSAMPLE" 
  ADD CONSTRAINT "PK_ASSAYSAMPLE" 
    PRIMARY KEY (ASSAYID,SAMPLEID)
    /*PK_TABLESPACE*/
    ENABLE;  
