--------------------------------------------------------
--  MAPPING SOURCE
--------------------------------------------------------
  CREATE SEQUENCE  "A2_SOFTWARE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_SOFTWARE" (
    "SOFTWAREID" NUMBER(22,0) CONSTRAINT NN_SOFTWARE_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_SOFTWARE_NAME NOT NULL
  , "VERSION" VARCHAR2(255));


  ALTER TABLE "A2_SOFTWARE"
  ADD CONSTRAINT "PK_SOFTWARE"
    PRIMARY KEY ("SOFTWAREID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
  /*INDEX_TABLESPACE*/
  ENABLE;


  ALTER TABLE "A2_SOFTWARE"
  ADD CONSTRAINT "UQ_SOFTWARE_NAME_VERSION"
    UNIQUE("NAME", "VERSION")
    ENABLE;


CREATE OR REPLACE TRIGGER "A2_SOFTWARE_INSERT"
before insert on A2_SOFTWARE
for each row
begin
if(:new.SOFTWAREID is null) then
select A2_SOFTWARE_seq.nextval into :new.SOFTWAREID from dual;
end if;
end;
/

ALTER TRIGGER "A2_SOFTWARE_INSERT" ENABLE;

-------------------------------------
-- ALTER A2_ARRAYDESIGN
------------------------------------
ALTER TABLE "A2_ARRAYDESIGN" add "MAPPINGSWID" NUMBER(22,0);

ALTER TABLE "A2_ARRAYDESIGN"
    ADD CONSTRAINT "FK_ARRAYDESIGN_mappingsw"
    FOREIGN KEY ("MAPPINGSWID")
    REFERENCES "A2_SOFTWARE" ("SOFTWAREID")
    ON DELETE CASCADE
    ENABLE;

-------------------------------------
-- ALTER A2_DESIGNELEMENT
------------------------------------
ALTER TABLE
   "A2_DESIGNELEMENT"
drop constraint "NN_DESIGNELEMENT_GENEID";

--------------------------------------------------------
-- BIOENTITY PROPERTY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYPROPERTY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYPROPERTY" (
    "BIOENTITYPROPERTYID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYPROPERTY_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BIOENTITYPROPERTY_NAME NOT NULL
  );


  ALTER TABLE "A2_BIOENTITYPROPERTY"
  ADD CONSTRAINT "PK_BIOENTITYPROPERTY"
    PRIMARY KEY ("BIOENTITYPROPERTYID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*INDEX_TABLESPACE*/
    ENABLE;


  ALTER TABLE "A2_BIOENTITYPROPERTY"
  ADD CONSTRAINT "UQ_BIOENTITYPROPERTY_NAME"
    UNIQUE("NAME")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BIOENTITYPROPERTY_INSERT
before insert on A2_BIOENTITYProperty
for each row
begin
if(:new.BIOENTITYPropertyID is null) then
select A2_BIOENTITYProperty_seq.nextval into :new.BIOENTITYPropertyID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYProperty_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY PROPERTYVALUE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYPROPERTYVALUE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYPROPERTYVALUE" (
    "BEPROPERTYVALUEID" NUMBER(22,0) CONSTRAINT NN_BEPV_ID NOT NULL
  , "BIOENTITYPROPERTYID" NUMBER(22,0) CONSTRAINT NN_GPV_BEPROPERTYID NOT NULL
  , "VALUE" VARCHAR2(1000) CONSTRAINT NN_BEPV_VALUE NOT NULL);


  ALTER TABLE "A2_BIOENTITYPROPERTYVALUE"
  ADD CONSTRAINT "PK_BEPROPERTYVALUE"
    PRIMARY KEY ("BEPROPERTYVALUEID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*INDEX_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_BEPROPERTYVALUEID_PROPERTY"
  ON "A2_BIOENTITYPROPERTYVALUE" ("BIOENTITYPROPERTYID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_BIOENTITYPROPERTYVALUE"
  ADD CONSTRAINT "FK_BEPROPERTYVALUE_PROPERTY"
    FOREIGN KEY ("BIOENTITYPROPERTYID")
    REFERENCES "A2_BIOENTITYPROPERTY" ("BIOENTITYPROPERTYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_BIOENTITYPROPERTYVALUE"
  ADD CONSTRAINT "UQ_BEPROPERTYVALUE_VALUE"
    UNIQUE("BIOENTITYPROPERTYID","VALUE")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BEPROPERTYVALUE_INSERT
before insert on A2_BIOENTITYPropertyValue
for each row
begin
if(:new.BEPropertyValueID is null) then
select A2_BIOENTITYPROPERTYVALUE_SEQ.nextval into :new.BEPropertyValueID from dual;
end if;
end;
/

ALTER TRIGGER A2_BEPROPERTYVALUE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITYTYPE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITYTYPE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BIOENTITYTYPE" (
    "BIOENTITYTYPEID" NUMBER(22,0) CONSTRAINT NN_BIOENTITYTYPEID_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BETYPE_NAME NOT NULL
  , "ID_FOR_INDEX" VARCHAR2(1) default '0'
  , "ID_FOR_ANALYTICS" VARCHAR2(1) default '0'
  , "PROP_FOR_INDEX" VARCHAR2(1) default '0');


  ALTER TABLE "A2_BIOENTITYTYPE"
  ADD CONSTRAINT "PK_BIOENTITYTYPE"
    PRIMARY KEY ("BIOENTITYTYPEID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*TABLESPACE "ATLAS2_DATA"*/
    ENABLE;

 ALTER TABLE "A2_BIOENTITYTYPE"
  ADD CONSTRAINT "UQ_BIOENTITYTYPE_NAME"
    UNIQUE("NAME")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BIOENTITYTYPE_INSERT
before insert on A2_BIOENTITYTYPE
for each row
begin
if(:new.BIOENTITYTYPEID is null) then
select A2_BIOENTITYTYPE_seq.nextval into :new.BIOENTITYTYPEID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYTYPE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BIOENTITY" (
    "BIOENTITYID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_ID NOT NULL
  , "IDENTIFIER" VARCHAR2(255) CONSTRAINT NN_BIOENTITY_IDENTIFIER NOT NULL
  , "NAME" VARCHAR2(255)
  , "ORGANISMID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_ORGANISMID NOT NULL
  , "BIOENTITYTYPEID" NUMBER(22,0) CONSTRAINT NN_BIOENTITY_BIOENTITYTYPEID NOT NULL);


  ALTER TABLE "A2_BIOENTITY"
  ADD CONSTRAINT "PK_BIOENTITY"
    PRIMARY KEY ("BIOENTITYID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*TABLESPACE "ATLAS2_DATA"*/
    ENABLE;


  CREATE INDEX "IDX_BIOENTITY_ORGANISM"
  ON "A2_BIOENTITY" ("ORGANISMID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_BIOENTITY"
  ADD CONSTRAINT "FK_BIOENTITY_ORGANISM"
    FOREIGN KEY ("ORGANISMID")
    REFERENCES "A2_ORGANISM" ("ORGANISMID")
    ON DELETE CASCADE
    ENABLE;

  CREATE INDEX "IDX_BIOENTITY_BIOENTITYTYPEID"
  ON "A2_BIOENTITY" ("BIOENTITYTYPEID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_BIOENTITY"
  ADD CONSTRAINT "FK_BIOENTITY_BIOENTITYTYPE"
    FOREIGN KEY ("BIOENTITYTYPEID")
    REFERENCES "A2_BIOENTITYTYPE" ("BIOENTITYTYPEID")
    ON DELETE CASCADE
    ENABLE;

 ALTER TABLE "A2_BIOENTITY"
  ADD CONSTRAINT "UQ_BIOENTITY_ID_TYPE"
    UNIQUE("IDENTIFIER", "BIOENTITYTYPEID")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BIOENTITY_INSERT
before insert on A2_BIOENTITY
for each row
begin
if(:new.BIOENTITYID is null) then
select A2_BIOENTITY_seq.nextval into :new.BIOENTITYID from dual;
end if;

if(:new.OrganismID is null) then
select OrganismID into :new.OrganismID from a2_Organism where Name = 'unknown';
end if;

end;
/

ALTER TRIGGER A2_BIOENTITY_INSERT ENABLE;

--------------------------------------------------------
-- BERELATIONTYPE
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BERELATIONTYPE_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BERELATIONTYPE" (
    "BERELATIONTYPEID" NUMBER(22,0) CONSTRAINT NN_BERELATIONTYPEID_ID NOT NULL
  , "NAME" VARCHAR2(255) CONSTRAINT NN_BERELATIONTYPE_NAME NOT NULL )
 ;

  ALTER TABLE "A2_BERELATIONTYPE"
  ADD CONSTRAINT "PK_BERELATIONTYPE"
    PRIMARY KEY ("BERELATIONTYPEID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*TABLESPACE "ATLAS2_DATA"*/
    ENABLE;

 ALTER TABLE "A2_BERELATIONTYPE"
  ADD CONSTRAINT "UQ_BERELATIONTYPE_NAME"
    UNIQUE("NAME")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BERELATIONTYPE_INSERT
before insert on A2_BERELATIONTYPE
for each row
begin
if(:new.BERELATIONTYPEID is null) then
select A2_BERELATIONTYPE_seq.nextval into :new.BERELATIONTYPEID from dual;
end if;
end;
/

ALTER TRIGGER A2_BERELATIONTYPE_INSERT ENABLE;

--------------------------------------------------------
-- BIOENTITY2BIOENTITY
--------------------------------------------------------
 CREATE SEQUENCE  "A2_BIOENTITY2BIOENTITY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


 CREATE TABLE "A2_BIOENTITY2BIOENTITY" (
    "BE2BEID" NUMBER(22,0) CONSTRAINT NN_BE2BE_ID NOT NULL
  , "BIOENTITYIDFROM"  NUMBER(22,0) CONSTRAINT NN_BE2BE_BEIDFROM NOT NULL
  , "BIOENTITYIDTO"  NUMBER(22,0) CONSTRAINT NN_BE2BE_BEIDTO NOT NULL
  , "BERELATIONTYPEID" NUMBER(22,0) /*CONSTRAINT NN_BE2BE_BERELTYPEID NOT NULL*/
  , "SOFTWAREID" NUMBER(22,0)  CONSTRAINT NN_BE2BE_SWID NOT NULL);

  ALTER TABLE "A2_BIOENTITY2BIOENTITY"
  ADD CONSTRAINT "PK_BE2BE"
    PRIMARY KEY ("BE2BEID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*TABLESPACE "ATLAS2_DATA"*/
    ENABLE;

 ALTER TABLE "A2_BIOENTITY2BIOENTITY"
  ADD CONSTRAINT "UQ_BE2BE_FROM_TO_RELATION"
    UNIQUE("BIOENTITYIDFROM", "BIOENTITYIDTO", "BERELATIONTYPEID", "SOFTWAREID")
    ENABLE;

 ALTER TABLE "A2_BIOENTITY2BIOENTITY"
  ADD CONSTRAINT "FK_BE2BE_BEFROM"
    FOREIGN KEY ("BIOENTITYIDFROM")
    REFERENCES "A2_BIOENTITY" ("BIOENTITYID")
    ON DELETE CASCADE
    ENABLE;

 ALTER TABLE "A2_BIOENTITY2BIOENTITY"
  ADD CONSTRAINT "FK_BE2BE_BETO"
    FOREIGN KEY ("BIOENTITYIDTO")
    REFERENCES "A2_BIOENTITY" ("BIOENTITYID")
    ON DELETE CASCADE
    ENABLE;

--   ALTER TABLE "A2_BIOENTITY2BIOENTITY"
--   ADD CONSTRAINT "FK_BE2BE_TYPE"
--     FOREIGN KEY ("BERELATIONTYPEID")
--     REFERENCES "A2_BERELATIONTYPE" ("BERELATIONTYPEID")
--     ON DELETE CASCADE
--     ENABLE;

 ALTER TABLE "A2_BIOENTITY2BIOENTITY"
  ADD CONSTRAINT "FK_BE2BE_SOFTWARE"
    FOREIGN KEY ("SOFTWAREID")
    REFERENCES "A2_SOFTWARE" ("SOFTWAREID")
    ON DELETE CASCADE
    ENABLE;

  CREATE INDEX "IDX_BE2BE_FROM"
  ON "A2_BIOENTITY2BIOENTITY" ("BIOENTITYIDFROM");

  CREATE INDEX "IDX_BE2BE_TO"
  ON "A2_BIOENTITY2BIOENTITY" ("BIOENTITYIDTO");

  CREATE INDEX "IDX_BE2BE_FROM_TO"
  ON "A2_BIOENTITY2BIOENTITY" ("BIOENTITYIDFROM", "BIOENTITYIDTO");

CREATE OR REPLACE TRIGGER A2_BIOENTITY2BIOENTITY_INSERT
before insert on A2_BIOENTITY2BIOENTITY
for each row
begin
if(:new.BE2BEID is null) then
select A2_BIOENTITY2BIOENTITY_SEQ.nextval into :new.BE2BEID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITY2BIOENTITY_INSERT ENABLE;


--------------------------------------------------------
-- BIOENTITY BIOENTITYPROPERTYVALUE
--------------------------------------------------------
  CREATE SEQUENCE  "A2_BIOENTITYBEPV_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_BIOENTITYBEPV" (
    "BIOENTITYBEPVID" NUMBER(22,0) CONSTRAINT NN_BIOENTITYBEPV_ID NOT NULL
  , "BIOENTITYID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_BIOENTITYID NOT NULL
  , "BEPROPERTYVALUEID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_BEPVID NOT NULL
  , "SOFTWAREID" NUMBER(22,0)  CONSTRAINT NN_BIOENTITYBEPV_SWID NOT NULL);


  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "PK_BIOENTITYBEPV"
    PRIMARY KEY ("BIOENTITYBEPVID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*INDEX_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_BIOENTITYBEPV_PROPERTY"
  ON "A2_BIOENTITYBEPV" ("BEPROPERTYVALUEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_BIOENTITYBEPV_BIOENTITY"
  ON "A2_BIOENTITYBEPV" ("BIOENTITYID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "FK_BIOENTITYBEPV_BIOENTITY"
    FOREIGN KEY ("BIOENTITYID")
    REFERENCES "A2_BIOENTITY" ("BIOENTITYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "FK_BIOENTITYBEPV_BIOENTITYPV"
    FOREIGN KEY ("BEPROPERTYVALUEID")
    REFERENCES "A2_BIOENTITYPROPERTYVALUE" ("BEPROPERTYVALUEID")
    ON DELETE CASCADE
    ENABLE;

  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "FK_BIOENTITYBEPV_SOFTWARE"
    FOREIGN KEY ("SOFTWAREID")
    REFERENCES "A2_SOFTWARE" ("SOFTWAREID")
    ON DELETE CASCADE
    ENABLE;

  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "UQ_BIOENTITY_BEPV_SW"
    UNIQUE("BIOENTITYID","BEPROPERTYVALUEID", "SOFTWAREID")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_BIOENTITYBEPV_INSERT
before insert on A2_BIOENTITYBEPV
for each row
begin
if(:new.BIOENTITYBEPVID is null) then
select A2_BIOENTITYBEPV_seq.nextval into :new.BIOENTITYBEPVID from dual;
end if;
end;
/

ALTER TRIGGER A2_BIOENTITYBEPV_INSERT ENABLE;

--------------------------------------------------------
-- DESIGNELEMENT BIOENTITY
--------------------------------------------------------
  CREATE SEQUENCE  "A2_DESIGNELTBIOENTITY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;


  CREATE TABLE "A2_DESIGNELTBIOENTITY" (
    "DEBEID" NUMBER(22,0) CONSTRAINT NN_DEBE_ID NOT NULL
  , "DESIGNELEMENTID" NUMBER(22,0) CONSTRAINT NN_DEBE_DESIGNELEMENTID NOT NULL
  , "BIOENTITYID" NUMBER(22,0)  CONSTRAINT NN_DEBE_BIOENTITYID NOT NULL
  , "SOFTWAREID" NUMBER(22,0)  CONSTRAINT NN_DEBE_SOFTWAREID NOT NULL);


  ALTER TABLE "A2_DESIGNELTBIOENTITY"
  ADD CONSTRAINT "PK_DESIGNELTBIOENTITY"
    PRIMARY KEY ("DEBEID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
    STORAGE(INITIAL 1048576
            NEXT 1048576
            MINEXTENTS 1
            MAXEXTENTS 2147483645
            PCTINCREASE 0
            FREELISTS 1
            FREELIST GROUPS 1
            BUFFER_POOL DEFAULT)
    /*INDEX_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_DEBE_BIOENTITY"
  ON "A2_DESIGNELTBIOENTITY" ("BIOENTITYID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_DESIGNELTBIOENTITY"
  ADD CONSTRAINT "FK_DEBE_BIOENTITY"
    FOREIGN KEY ("BIOENTITYID")
    REFERENCES "A2_BIOENTITY" ("BIOENTITYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_DESIGNELTBIOENTITY"
  ADD CONSTRAINT "FK_DEBE_DESIGNELEMENT"
    FOREIGN KEY ("DESIGNELEMENTID")
    REFERENCES "A2_DESIGNELEMENT" ("DESIGNELEMENTID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_DESIGNELTBIOENTITY"
  ADD CONSTRAINT "UQ_DESIGNELTBIOENTITY"
    UNIQUE("BIOENTITYID","DESIGNELEMENTID", "SOFTWAREID")
    ENABLE;


CREATE OR REPLACE TRIGGER A2_DESIGNELTBIOENTITY_INSERT
before insert on A2_DESIGNELTBIOENTITY
for each row
begin
if(:new.DEBEID is null) then
select A2_DESIGNELTBIOENTITY_seq.nextval into :new.DEBEID from dual;
end if;
end;
/

ALTER TRIGGER A2_DESIGNELTBIOENTITY_INSERT ENABLE;

CREATE OR REPLACE VIEW VWDESIGNELEMENTGENELINKED
AS
  SELECT
    de.designelementid    AS designelementid,
    de.accession          AS accession,
    de.name               AS name,
    de.arraydesignid      AS arraydesignid,
    tobe.bioentityid      AS bioentityid,
    tobe.identifier       AS identifier,
    tobe.organismid       AS organismid,
    tobe.bioentitytypeid  AS bioentitytypeid,
    be2be.softwareid      AS annotationswid
  FROM a2_designelement de
  join a2_designeltbioentity debe on debe.designelementid = de.designelementid
  join a2_bioentity frombe on frombe.bioentityid = debe.bioentityid
  join a2_bioentity2bioentity be2be on be2be.bioentityidfrom = frombe.bioentityid
  join a2_bioentity tobe on tobe.bioentityid = be2be.bioentityidto
  join a2_bioentitytype betype on betype.bioentitytypeid = tobe.bioentitytypeid
  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid
  where debe.softwareid = ad.mappingswid
  and betype.ID_FOR_INDEX = 1
/

CREATE OR REPLACE VIEW VWDESIGNELEMENTGENEDIRECT
AS
  SELECT
    de.designelementid    AS designelementid,
    de.accession          AS accession,
    de.name               AS name,
    de.arraydesignid      AS arraydesignid,
    frombe.bioentityid      AS bioentityid,
    frombe.identifier       AS identifier,
    frombe.organismid       AS organismid
  FROM a2_designelement de
  join a2_designeltbioentity debe on debe.designelementid = de.designelementid
  join a2_bioentity frombe on frombe.bioentityid = debe.bioentityid
  join a2_bioentitytype betype on betype.bioentitytypeid = frombe.bioentitytypeid
  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid
  where debe.softwareid = ad.mappingswid
  and betype.ID_FOR_INDEX = 1
 /


