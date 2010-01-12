/*******************************************************************************/
/*  Tables, Sequences, Indexes and Triggers in Atlas2 schema     
/*
/*                                              
/*******************************************************************************/

--------------------------------------------------------
--  DDL for Sequence A2_ARRAYDESIGN_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ARRAYDESIGN_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 161 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_ASSAYONTOLOGY_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ASSAYONTOLOGY_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 26721 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_ASSAYPROPERTYVALUE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ASSAYPROPERTYVALUE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 159661 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_ASSAYSAMPLE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ASSAYSAMPLE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 37441 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_ASSAY_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ASSAY_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 95221 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_DESIGNELEMENT_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_DESIGNELEMENT_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 1438121 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_EXPERIMENT_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_EXPERIMENT_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 1241 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_EXPRESSIONANALYTICS_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_EXPRESSIONANALYTICS_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 247741001 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_EXPRESSIONVALUE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_EXPRESSIONVALUE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 22329881 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_GENEPROPERTYVALUE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_GENEPROPERTYVALUE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 43181881 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_GENEPROPERTY_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_GENEPROPERTY_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 61 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_GENE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_GENE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 412061 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_ONTOLOGYTERM_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_ONTOLOGYTERM_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 3841 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_PROPERTYVALUE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_PROPERTYVALUE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 1287481 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_PROPERTY_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_PROPERTY_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 166 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_SAMPLEONTOLOGY_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_SAMPLEONTOLOGY_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 61641 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_SAMPLEPROPERTYVALUE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_SAMPLEPROPERTYVALUE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 331261 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_SAMPLE_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_SAMPLE_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 841 CACHE 20 NOORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Sequence A2_SPEC_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."A2_SPEC_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 41 CACHE 20 NOORDER  NOCYCLE;
--------------------------------------------------------
--  DDL for Sequence LOAD_MONITOR_SEQ
--------------------------------------------------------

   CREATE SEQUENCE  "ATLAS2"."LOAD_MONITOR_SEQ"  MINVALUE 1 MAXVALUE 1.00000000000000E+27 INCREMENT BY 1 START WITH 7701 CACHE 20 ORDER  NOCYCLE;

--------------------------------------------------------
--  DDL for Table A2_ARRAYDESIGN
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ARRAYDESIGN" ("ARRAYDESIGNID" NUMBER(22,0), "ACCESSION" VARCHAR2(255), "TYPE" VARCHAR2(255), "NAME" VARCHAR2(255), "PROVIDER" VARCHAR2(255)) ;
 
--------------------------------------------------------
--  DDL for Table A2_ASSAY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ASSAY" ("ASSAYID" NUMBER(22,0), "ACCESSION" VARCHAR2(255), "EXPERIMENTID" NUMBER(22,0), "ARRAYDESIGNID" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_ASSAYONTOLOGY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ASSAYONTOLOGY" ("ASSAYONTOLOGYID" NUMBER(22,0), "ASSAYID" NUMBER(22,0), "ONTOLOGYTERMID" NUMBER(22,0), "SOURCEMAPPING" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_ASSAYPROPERTYVALUE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("ASSAYPROPERTYVALUEID" NUMBER(22,0), "ASSAYID" NUMBER(22,0), "PROPERTYVALUEID" NUMBER(22,0), "ISFACTORVALUE" NUMBER(22,0)); 
 
--------------------------------------------------------
--  DDL for Table A2_ASSAYSAMPLE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ASSAYSAMPLE" ("ASSAYSAMPLEID" NUMBER(22,0), "ASSAYID" NUMBER(22,0), "SAMPLEID" NUMBER(22,0)) ;
 
--------------------------------------------------------
--  DDL for Table A2_DESIGNELEMENT
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_DESIGNELEMENT" ("DESIGNELEMENTID" NUMBER(22,0), "ARRAYDESIGNID" NUMBER(22,0), "GENEID" NUMBER(22,0), "ACCESSION" VARCHAR2(255), "NAME" VARCHAR2(255), "TYPE" VARCHAR2(255), "ISCONTROL" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_EXPERIMENT
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_EXPERIMENT" ("EXPERIMENTID" NUMBER(22,0), "ACCESSION" VARCHAR2(255), "DESCRIPTION" VARCHAR2(2000), "PERFORMER" VARCHAR2(2000), "LAB" VARCHAR2(2000), "LOADDATE" DATE);

--------------------------------------------------------
--  DDL for Table A2_EXPRESSIONANALYTICS
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_EXPRESSIONANALYTICS" ("EXPRESSIONID" NUMBER(22,0), "EXPERIMENTID" NUMBER(22,0), "PROPERTYVALUEID" NUMBER(22,0), "GENEID" NUMBER(22,0), "TSTAT" FLOAT(125), "PVALADJ" FLOAT(125), "FPVAL" FLOAT(125), "FPVALADJ" FLOAT(125), "DESIGNELEMENTID" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_EXPRESSIONVALUE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_EXPRESSIONVALUE" ("EXPRESSIONVALUEID" NUMBER(12,0), "VALUE" FLOAT(126), "DESIGNELEMENTID" NUMBER(12,0), "ASSAYID" NUMBER(12,0), "EXPERIMENTID" NUMBER(12,0));

--------------------------------------------------------
--  DDL for Table A2_GENE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_GENE" ("GENEID" NUMBER(22,0), "SPECID" NUMBER(22,0), "IDENTIFIER" VARCHAR2(255), "NAME" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_GENEPROPERTY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_GENEPROPERTY" ("GENEPROPERTYID" NUMBER(22,0), "NAME" VARCHAR2(255), "AE2TABLENAME" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_GENEPROPERTYVALUE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_GENEPROPERTYVALUE" ("GENEPROPERTYVALUEID" NUMBER(22,0), "GENEID" NUMBER(22,0), "GENEPROPERTYID" NUMBER(22,0), "VALUE" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_ONTOLOGY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ONTOLOGY" ("ONTOLOGYID" NUMBER(22,0), "NAME" VARCHAR2(255), "SOURCE_URI" VARCHAR2(500), "DESCRIPTION" VARCHAR2(4000), "VERSION" VARCHAR2(50));

--------------------------------------------------------
--  DDL for Table A2_ONTOLOGYTERM
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_ONTOLOGYTERM" ("ONTOLOGYTERMID" NUMBER(22,0), "ONTOLOGYID" NUMBER(22,0), "TERM" VARCHAR2(4000), "ACCESSION" VARCHAR2(255), "DESCRIPTION" VARCHAR2(4000), "ORIG_VALUE" VARCHAR2(255), "ORIG_VALUE_SRC" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_PROPERTY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_PROPERTY" ("PROPERTYID" NUMBER(22,0), "NAME" VARCHAR2(255), "AE1TABLENAME_ASSAY" VARCHAR2(255), "AE1TABLENAME_SAMPLE" VARCHAR2(255), "ASSAYPROPERTYID" NUMBER(22,0), "SAMPLEPROPERTYID" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_PROPERTYVALUE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYVALUEID" NUMBER(22,0), "PROPERTYID" NUMBER(22,0), "NAME" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_SAMPLE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_SAMPLE" ("SAMPLEID" NUMBER(22,0), "ACCESSION" VARCHAR2(255), "SPECIES" VARCHAR2(255), "CHANNEL" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table A2_SAMPLEONTOLOGY
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_SAMPLEONTOLOGY" ("SAMPLEONTOLOGYID" NUMBER(22,0), "SAMPLEID" NUMBER(22,0), "ONTOLOGYTERMID" NUMBER(22,0), "SOURCEMAPPING" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_SAMPLEPROPERTYVALUE
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("SAMPLEPROPERTYVALUEID" NUMBER(22,0), "SAMPLEID" NUMBER(22,0), "PROPERTYVALUEID" NUMBER(22,0), "ISFACTORVALUE" NUMBER(22,0));

--------------------------------------------------------
--  DDL for Table A2_SPEC
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."A2_SPEC" ("SPECID" NUMBER(22,0), "NAME" VARCHAR2(255));

--------------------------------------------------------
--  DDL for Table LOAD_MONITOR
--------------------------------------------------------

  CREATE TABLE "ATLAS2"."LOAD_MONITOR" ("ID" NUMBER, "ACCESSION" VARCHAR2(255), "STATUS" VARCHAR2(50), "NETCDF" VARCHAR2(50), "SIMILARITY" VARCHAR2(50), "RANKING" VARCHAR2(50), "SEARCHINDEX" VARCHAR2(50), "LOAD_TYPE" VARCHAR2(250));

--------------------------------------------------------
--  Constraints for Table A2_ARRAYDESIGN
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ARRAYDESIGN" ADD CONSTRAINT "PK_ARRAYDESIGN" PRIMARY KEY ("ARRAYDESIGNID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;
--------------------------------------------------------
--  Constraints for Table A2_ASSAY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAY" ADD CONSTRAINT "PK_ASSAY" PRIMARY KEY ("ASSAYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_ASSAYONTOLOGY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYONTOLOGY" ADD CONSTRAINT "PK_ASSAYONTOLOGY" PRIMARY KEY ("ASSAYONTOLOGYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_ASSAYPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYPROPERTYVALUE" ADD CONSTRAINT "PK_ASSAYPROPERTYVALUE" PRIMARY KEY ("ASSAYPROPERTYVALUEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_ASSAYSAMPLE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYSAMPLE" ADD CONSTRAINT "PK_ASSAYSAMPLE" PRIMARY KEY ("ASSAYSAMPLEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_DESIGNELEMENT
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_DESIGNELEMENT" ADD CONSTRAINT "PK_DESIGNELEMENT" PRIMARY KEY ("DESIGNELEMENTID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_EXPERIMENT
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_EXPERIMENT" ADD CONSTRAINT "PK_EXPERIMENT" PRIMARY KEY ("EXPERIMENTID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_EXPRESSIONANALYTICS
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_EXPRESSIONANALYTICS" ADD CONSTRAINT "PK_EXPRESSIONANALYTICS" PRIMARY KEY ("EXPRESSIONID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_EXPRESSIONVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_EXPRESSIONVALUE" ADD CONSTRAINT "PK_EXPRESSIONVALUE" PRIMARY KEY ("EXPRESSIONVALUEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_GENE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_GENE" ADD CONSTRAINT "PK_GENE" PRIMARY KEY ("GENEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_DATA"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_GENEPROPERTY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_GENEPROPERTY" ADD CONSTRAINT "PK_GENEPROPERTY" PRIMARY KEY ("GENEPROPERTYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_GENEPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_GENEPROPERTYVALUE" ADD CONSTRAINT "PK_GENEPROPERTYVALUE" PRIMARY KEY ("GENEPROPERTYVALUEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_ONTOLOGY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ONTOLOGY" ADD CONSTRAINT "PK_ONTOLOGY" PRIMARY KEY ("ONTOLOGYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_ONTOLOGYTERM
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ONTOLOGYTERM" ADD CONSTRAINT "PK_ONTOLOGYTERM" PRIMARY KEY ("ONTOLOGYTERMID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_PROPERTY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_PROPERTY" ADD CONSTRAINT "PK_PROPERTY" PRIMARY KEY ("PROPERTYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_PROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_PROPERTYVALUE" ADD CONSTRAINT "PK_PROPERTYVALUE" PRIMARY KEY ("PROPERTYVALUEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_SAMPLE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SAMPLE" ADD CONSTRAINT "PK_SAMPLE" PRIMARY KEY ("SAMPLEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_SAMPLEONTOLOGY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SAMPLEONTOLOGY" ADD CONSTRAINT "PK_SAMPLEONTOLOGY" PRIMARY KEY ("SAMPLEONTOLOGYID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_SAMPLEPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ADD CONSTRAINT "PK_SAMPLEPROPERTYVALUE" PRIMARY KEY ("SAMPLEPROPERTYVALUEID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table A2_SPEC
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SPEC" ADD CONSTRAINT "PK_SPEC" PRIMARY KEY ("SPECID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  Constraints for Table LOAD_MONITOR
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."LOAD_MONITOR" ADD CONSTRAINT "PK_LOAD_MONITOR" PRIMARY KEY ("ID") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255  STORAGE(INITIAL 131072 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "ATLAS2_INDX"  ENABLE;

--------------------------------------------------------
--  DDL for Index A2_IDX_EXPRESSION_FV
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."A2_IDX_EXPRESSION_FV" ON "ATLAS2"."A2_EXPRESSIONANALYTICS" ("PROPERTYVALUEID");

--------------------------------------------------------
--  DDL for Index A2_IDX_EXPRESSION_GENE
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."A2_IDX_EXPRESSION_GENE" ON "ATLAS2"."A2_EXPRESSIONANALYTICS" ("GENEID");

--------------------------------------------------------
--  DDL for Index IDX_ANALYTICS_DESIGNELEMENT
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ANALYTICS_DESIGNELEMENT" ON "ATLAS2"."A2_EXPRESSIONANALYTICS" ("DESIGNELEMENTID");
--------------------------------------------------------
--  DDL for Index IDX_ARRAYDESIGN_ACCESSION
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ARRAYDESIGN_ACCESSION" ON "ATLAS2"."A2_ARRAYDESIGN" ("ACCESSION");

--------------------------------------------------------
--  DDL for Index IDX_ASSAYPROPERTY_AP
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ASSAYPROPERTY_AP" ON "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("ASSAYID", "PROPERTYVALUEID");

--------------------------------------------------------
--  DDL for Index IDX_ASSAYPROPERTY_ASSAY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ASSAYPROPERTY_ASSAY" ON "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("ASSAYID");

--------------------------------------------------------
--  DDL for Index IDX_ASSAYPROPERTY_PEROPERTY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ASSAYPROPERTY_PEROPERTY" ON "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("PROPERTYVALUEID");

--------------------------------------------------------
--  DDL for Index IDX_ASSAY_ARRAYDESIGN
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ASSAY_ARRAYDESIGN" ON "ATLAS2"."A2_ASSAY" ("ARRAYDESIGNID");

--------------------------------------------------------
--  DDL for Index IDX_ASSAY_EXPERIMENT
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ASSAY_EXPERIMENT" ON "ATLAS2"."A2_ASSAY" ("EXPERIMENTID");

--------------------------------------------------------
--  DDL for Index IDX_DESIGNELEMENT_GENE
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_DESIGNELEMENT_GENE" ON "ATLAS2"."A2_DESIGNELEMENT" ("GENEID");

--------------------------------------------------------
--  DDL for Index IDX_EA_EXPERIMENT
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_EA_EXPERIMENT" ON "ATLAS2"."A2_EXPRESSIONANALYTICS" ("EXPERIMENTID");

--------------------------------------------------------
--  DDL for Index IDX_EV_ASSAYID
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_EV_ASSAYID" ON "ATLAS2"."A2_EXPRESSIONVALUE" ("ASSAYID");

--------------------------------------------------------
--  DDL for Index IDX_EV_DESIGNELEMENT
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_EV_DESIGNELEMENT" ON "ATLAS2"."A2_EXPRESSIONVALUE" ("DESIGNELEMENTID");

--------------------------------------------------------
--  DDL for Index IDX_EXPERIMENT_ACCESSION
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_EXPERIMENT_ACCESSION" ON "ATLAS2"."A2_EXPERIMENT" ("ACCESSION");

--------------------------------------------------------
--  DDL for Index IDX_GENEPROPERTYVALUE_PROPERTY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_GENEPROPERTYVALUE_PROPERTY" ON "ATLAS2"."A2_GENEPROPERTYVALUE" ("GENEPROPERTYID");

--------------------------------------------------------
--  DDL for Index IDX_GENE_SPEC
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_GENE_SPEC" ON "ATLAS2"."A2_GENE" ("SPECID");

--------------------------------------------------------
--  DDL for Index IDX_GPV_GENE
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_GPV_GENE" ON "ATLAS2"."A2_GENEPROPERTYVALUE" ("GENEID");

--------------------------------------------------------
--  DDL for Index IDX_LOAD_ACCESSION
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_LOAD_ACCESSION" ON "ATLAS2"."LOAD_MONITOR" ("ACCESSION");

--------------------------------------------------------
--  DDL for Index IDX_ONTOLOGYTERM_ONTOLOGY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_ONTOLOGYTERM_ONTOLOGY" ON "ATLAS2"."A2_ONTOLOGYTERM" ("ONTOLOGYID");

--------------------------------------------------------
--  DDL for Index IDX_PROPERTYVALUE_NAME
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_PROPERTYVALUE_NAME" ON "ATLAS2"."A2_PROPERTYVALUE" ("NAME");

--------------------------------------------------------
--  DDL for Index IDX_PROPERTYVALUE_PROPERTY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_PROPERTYVALUE_PROPERTY" ON "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYID");

--------------------------------------------------------
--  DDL for Index IDX_PROPERTY_NAME
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_PROPERTY_NAME" ON "ATLAS2"."A2_PROPERTY" ("NAME");

--------------------------------------------------------
--  DDL for Index IDX_SAMPLEPROPERTY_PROPERTY
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_SAMPLEPROPERTY_PROPERTY" ON "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("PROPERTYVALUEID");

--------------------------------------------------------
--  DDL for Index IDX_SAMPLEPROPERTY_PS
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_SAMPLEPROPERTY_PS" ON "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("PROPERTYVALUEID", "SAMPLEID");

--------------------------------------------------------
--  DDL for Index IDX_SAMPLEPROPERTY_SAMPLE
--------------------------------------------------------


  CREATE INDEX "ATLAS2"."IDX_SAMPLEPROPERTY_SAMPLE" ON "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("SAMPLEID");

--------------------------------------------------------
--  DDL for Index IDX_SAMPLEPROPERTY_SP
--------------------------------------------------------

  CREATE INDEX "ATLAS2"."IDX_SAMPLEPROPERTY_SP" ON "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("SAMPLEID", "PROPERTYVALUEID");

/*
--------------------------------------------------------
--  DDL for Index PK_ARRAYDESIGN
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ARRAYDESIGN" ON "ATLAS2"."A2_ARRAYDESIGN" ("ARRAYDESIGNID")

--------------------------------------------------------
--  DDL for Index PK_ASSAY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ASSAY" ON "ATLAS2"."A2_ASSAY" ("ASSAYID")
--------------------------------------------------------
--  DDL for Index PK_ASSAYONTOLOGY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ASSAYONTOLOGY" ON "ATLAS2"."A2_ASSAYONTOLOGY" ("ASSAYONTOLOGYID")
--------------------------------------------------------
--  DDL for Index PK_ASSAYPROPERTYVALUE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ASSAYPROPERTYVALUE" ON "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("ASSAYPROPERTYVALUEID")
--------------------------------------------------------
--  DDL for Index PK_ASSAYSAMPLE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ASSAYSAMPLE" ON "ATLAS2"."A2_ASSAYSAMPLE" ("ASSAYSAMPLEID")
--------------------------------------------------------
--  DDL for Index PK_DESIGNELEMENT
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_DESIGNELEMENT" ON "ATLAS2"."A2_DESIGNELEMENT" ("DESIGNELEMENTID")
--------------------------------------------------------
--  DDL for Index PK_EXPERIMENT
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_EXPERIMENT" ON "ATLAS2"."A2_EXPERIMENT" ("EXPERIMENTID")
--------------------------------------------------------
--  DDL for Index PK_EXPRESSIONANALYTICS
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_EXPRESSIONANALYTICS" ON "ATLAS2"."A2_EXPRESSIONANALYTICS" ("EXPRESSIONID")
--------------------------------------------------------
--  DDL for Index PK_GENE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_GENE" ON "ATLAS2"."A2_GENE" ("GENEID")
--------------------------------------------------------
--  DDL for Index PK_GENEPROPERTY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_GENEPROPERTY" ON "ATLAS2"."A2_GENEPROPERTY" ("GENEPROPERTYID")
--------------------------------------------------------
--  DDL for Index PK_GENEPROPERTYVALUE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_GENEPROPERTYVALUE" ON "ATLAS2"."A2_GENEPROPERTYVALUE" ("GENEPROPERTYVALUEID")
--------------------------------------------------------
--  DDL for Index PK_LOADMONITOR
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_LOADMONITOR" ON "ATLAS2"."LOAD_MONITOR" ("ID")
--------------------------------------------------------
--  DDL for Index PK_ONTOLOGY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ONTOLOGY" ON "ATLAS2"."A2_ONTOLOGY" ("ONTOLOGYID")
--------------------------------------------------------
--  DDL for Index PK_ONTOLOGYTERM
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_ONTOLOGYTERM" ON "ATLAS2"."A2_ONTOLOGYTERM" ("ONTOLOGYTERMID")
--------------------------------------------------------
--  DDL for Index PK_PROPERTY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_PROPERTY" ON "ATLAS2"."A2_PROPERTY" ("PROPERTYID")
--------------------------------------------------------
--  DDL for Index PK_PROPERTYVALUE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_PROPERTYVALUE" ON "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYVALUEID")
--------------------------------------------------------
--  DDL for Index PK_SAMPLE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_SAMPLE" ON "ATLAS2"."A2_SAMPLE" ("SAMPLEID")
--------------------------------------------------------
--  DDL for Index PK_SAMPLEONTOLOGY
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_SAMPLEONTOLOGY" ON "ATLAS2"."A2_SAMPLEONTOLOGY" ("SAMPLEONTOLOGYID")
--------------------------------------------------------
--  DDL for Index PK_SAMPLEPROPERTYVALUE
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_SAMPLEPROPERTYVALUE" ON "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("SAMPLEPROPERTYVALUEID")
--------------------------------------------------------
--  DDL for Index PK_SPEC
--------------------------------------------------------

  CREATE UNIQUE INDEX "ATLAS2"."PK_SPEC" ON "ATLAS2"."A2_SPEC" ("SPECID")
*/

--------------------------------------------------------
--  Ref Constraints for Table A2_ASSAY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAY" ADD CONSTRAINT "FK_ASSAY_ARRAYDESIGN" FOREIGN KEY ("ARRAYDESIGNID") REFERENCES "ATLAS2"."A2_ARRAYDESIGN" ("ARRAYDESIGNID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_ASSAY" ADD CONSTRAINT "FK_ASSAY_EXPERIMENT" FOREIGN KEY ("EXPERIMENTID") REFERENCES "ATLAS2"."A2_EXPERIMENT" ("EXPERIMENTID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table A2_ASSAYONTOLOGY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYONTOLOGY" ADD CONSTRAINT "FK_ASSAYONTOLOGY_SOURCE" FOREIGN KEY ("SOURCEMAPPING") REFERENCES "ATLAS2"."A2_ASSAYPROPERTYVALUE" ("ASSAYPROPERTYVALUEID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table A2_ASSAYPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYPROPERTYVALUE" ADD CONSTRAINT "FK_ASSAYPROPERTY_PROPERTY" FOREIGN KEY ("PROPERTYVALUEID") REFERENCES "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYVALUEID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table A2_ASSAYSAMPLE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ASSAYSAMPLE" ADD CONSTRAINT "FK_ASSAYSAMPLE_ASSAY" FOREIGN KEY ("ASSAYID") REFERENCES "ATLAS2"."A2_ASSAY" ("ASSAYID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_ASSAYSAMPLE" ADD CONSTRAINT "FK_ASSAYSAMPLE_SAMPLE" FOREIGN KEY ("SAMPLEID") REFERENCES "ATLAS2"."A2_SAMPLE" ("SAMPLEID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table A2_DESIGNELEMENT
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_DESIGNELEMENT" ADD CONSTRAINT "FK_DESIGNELEMENT_ARRAYDESIGN" FOREIGN KEY ("ARRAYDESIGNID") REFERENCES "ATLAS2"."A2_ARRAYDESIGN" ("ARRAYDESIGNID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_DESIGNELEMENT" ADD CONSTRAINT "FK_DESIGNELEMENT_GENE" FOREIGN KEY ("GENEID") REFERENCES "ATLAS2"."A2_GENE" ("GENEID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_EXPRESSIONANALYTICS
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_EXPRESSIONANALYTICS" ADD CONSTRAINT "FK_ANALYTICS_EXPERIMENT" FOREIGN KEY ("EXPERIMENTID") REFERENCES "ATLAS2"."A2_EXPERIMENT" ("EXPERIMENTID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_EXPRESSIONANALYTICS" ADD CONSTRAINT "FK_ANALYTICS_PROPERTYVALUE" FOREIGN KEY ("PROPERTYVALUEID") REFERENCES "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYVALUEID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_GENE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_GENE" ADD CONSTRAINT "FK_GENE_SPEC" FOREIGN KEY ("SPECID") REFERENCES "ATLAS2"."A2_SPEC" ("SPECID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_GENEPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_GENEPROPERTYVALUE" ADD CONSTRAINT "FK_GENEPROPERTY_GENE" FOREIGN KEY ("GENEID") REFERENCES "ATLAS2"."A2_GENE" ("GENEID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_GENEPROPERTYVALUE" ADD CONSTRAINT "FK_GENEPROPERTYVALUE_PROPERTY" FOREIGN KEY ("GENEPROPERTYID") REFERENCES "ATLAS2"."A2_GENEPROPERTY" ("GENEPROPERTYID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_ONTOLOGYTERM
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_ONTOLOGYTERM" ADD CONSTRAINT "FK_ONTOLOGYTERM_ONTOLOGY" FOREIGN KEY ("ONTOLOGYID") REFERENCES "ATLAS2"."A2_ONTOLOGY" ("ONTOLOGYID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_PROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_PROPERTYVALUE" ADD CONSTRAINT "FK_PROPERTY_PROPERTYVALUE" FOREIGN KEY ("PROPERTYID") REFERENCES "ATLAS2"."A2_PROPERTY" ("PROPERTYID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_SAMPLEONTOLOGY
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SAMPLEONTOLOGY" ADD CONSTRAINT "FK_SAMPLEONTOLOGY_SOURCE" FOREIGN KEY ("SOURCEMAPPING") REFERENCES "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ("SAMPLEPROPERTYVALUEID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_SAMPLEONTOLOGY" ADD CONSTRAINT "FK_SAMPLEONTOLOGY_ONTOLOGY" FOREIGN KEY ("ONTOLOGYTERMID") REFERENCES "ATLAS2"."A2_ONTOLOGYTERM" ("ONTOLOGYTERMID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table A2_SAMPLEPROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ADD CONSTRAINT "FK_SAMPLEPROPERTY_SAMPLE" FOREIGN KEY ("SAMPLEID") REFERENCES "ATLAS2"."A2_SAMPLE" ("SAMPLEID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_SAMPLEPROPERTYVALUE" ADD CONSTRAINT "FK_SAMPLEPROPERTY_PROPERTY" FOREIGN KEY ("PROPERTYVALUEID") REFERENCES "ATLAS2"."A2_PROPERTYVALUE" ("PROPERTYVALUEID") ENABLE;

--------------------------------------------------------
--  Ref Constraints for Table A2_EXPRESSIONVALUE
--------------------------------------------------------

  ALTER TABLE "ATLAS2"."A2_EXPRESSIONVALUE" ADD CONSTRAINT "FK_EXPRESSIONVALUE_DESIGNELEMENT" FOREIGN KEY ("DESIGNELEMENTID") REFERENCES "ATLAS2"."A2_DESIGNELEMENT" ("DESIGNELEMENTID") ENABLE;
 
  ALTER TABLE "ATLAS2"."A2_EXPRESSIONVALUE" ADD CONSTRAINT "FK_EXPRESSIONVALUE_ASSAY" FOREIGN KEY ("ASSAYID") REFERENCES "ATLAS2"."A2_ASSAY" ("ASSAYID") ENABLE;


--------------------------------------------------------
--  DDL for Trigger A2_ASSAYONTOLOGY_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ASSAYONTOLOGY_INSERT"  

before insert on A2_AssayOntology
for each row
begin
if(:new.AssayOntologyID is null) then
select A2_AssayOntology_seq.nextval into :new.AssayOntologyID from dual;
end if;
end;
/

ALTER TRIGGER "ATLAS2"."A2_ASSAYONTOLOGY_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ASSAYPROPERTYVALUE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ASSAYPROPERTYVALUE_INSERT"  

before insert on A2_AssayPropertyValue
for each row
begin
if(:new.AssayPropertyValueID is null) then
select A2_AssayPropertyValue_seq.nextval into :new.AssayPropertyValueID from dual;
end if;
end;
/

ALTER TRIGGER "ATLAS2"."A2_ASSAYPROPERTYVALUE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ASSAYSAMPLE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ASSAYSAMPLE_INSERT"  

before insert on A2_AssaySample
for each row
begin
if(:new.AssaySampleID is null) then
select A2_AssaySample_seq.nextval into :new.AssaySampleID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ASSAYSAMPLE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ASSAY_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ASSAY_INSERT"  

before insert on A2_Assay
for each row
begin
if(:new.AssayID is null) then
select A2_Assay_seq.nextval into :new.AssayID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ASSAY_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ArrayDesign_Insert
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ArrayDesign_Insert"  

before insert on A2_ArrayDesign
for each row
begin
if( :new.ArrayDesignID is null) then
select A2_ArrayDesign_seq.nextval into :new.ArrayDesignID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ArrayDesign_Insert" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_DesignElement_Insert
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_DesignElement_Insert"  

before insert on A2_DesignElement
for each row
begin
if(:new.DesignElementID is null) then
select A2_DesignElement_seq.nextval into :new.DesignElementID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_DesignElement_Insert" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_EXPERIMENT_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_EXPERIMENT_INSERT"  

before insert on A2_Experiment
for each row
begin
if(:new.ExperimentID is null) then
  select A2_Experiment_seq.nextval into :new.ExperimentID from dual;
end if;
end;
/

ALTER TRIGGER "ATLAS2"."A2_EXPERIMENT_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ExpressionAnalytics_Insert
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ExpressionAnalytics_Insert"  

before insert on A2_ExpressionAnalytics
for each row
begin
if(:new.ExpressionID is null) then
select A2_ExpressionAnalytics_seq.nextval into :new.ExpressionID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ExpressionAnalytics_Insert" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ExpressionValue_Insert
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ExpressionValue_Insert"  
 

before insert on A2_ExpressionValue
for each row
begin
if(:new.ExpressionValueID is null) then
select A2_ExpressionValue_seq.nextval into :new.ExpressionValueID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ExpressionValue_Insert" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_GENEPropertyValue_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_GENEPropertyValue_INSERT"  

before insert on A2_GENEPropertyValue
for each row
begin
if(:new.GENEPropertyValueID is null) then
select A2_GENEPropertyValue_seq.nextval into :new.GENEPropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_GENEPropertyValue_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_GENEProperty_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_GENEProperty_INSERT"  

before insert on A2_GENEProperty
for each row
begin
if(:new.GENEPropertyID is null) then
select A2_GENEProperty_seq.nextval into :new.GENEPropertyID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_GENEProperty_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_GENE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_GENE_INSERT"  

before insert on A2_GENE
for each row
begin
if(:new.GENEID is null) then
select A2_GENE_seq.nextval into :new.GENEID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_GENE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_ONTOLOGYTERM_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_ONTOLOGYTERM_INSERT"  

before insert on A2_OntologyTerm
for each row
begin
if( :new.OntologyTermID is null) then
select A2_OntologyTerm_seq.nextval into :new.OntologyTermID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_ONTOLOGYTERM_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_PROPERTYVALUE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_PROPERTYVALUE_INSERT"  

before insert on A2_PropertyValue
for each row
begin
if ( :new.PropertyValueID is null) then 
  select A2_PropertyValue_seq.nextval into :new.PropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_PROPERTYVALUE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_PROPERTY_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_PROPERTY_INSERT"  

before insert on A2_Property
for each row
begin
if(:new.PropertyID is null) then
  select A2_Property_seq.nextval into :new.PropertyID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_PROPERTY_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_SAMPLEONTOLOGY_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_SAMPLEONTOLOGY_INSERT"  

before insert on A2_SampleOntology
for each row
begin
if(:new.SampleOntologyID is null) then
select A2_SampleOntology_seq.nextval into :new.SampleOntologyID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_SAMPLEONTOLOGY_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_SAMPLEPROPERTYVALUE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_SAMPLEPROPERTYVALUE_INSERT"  

before insert on A2_SamplePropertyValue
for each row
begin
if(:new.SamplePropertyValueID is null) then
select A2_SamplePropertyValue_seq.nextval into :new.SamplePropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_SAMPLEPROPERTYVALUE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_SAMPLE_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_SAMPLE_INSERT"  

before insert on A2_Sample
for each row
begin
if(:new.SampleID is null) then
select A2_Sample_seq.nextval into :new.SampleID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_SAMPLE_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for Trigger A2_SPEC_INSERT
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "ATLAS2"."A2_SPEC_INSERT"  

before insert on A2_SPEC
for each row
begin
if(:new.SPECID is null) then
select A2_SPEC_seq.nextval into :new.SPECID from dual;
end if;
end;
/
ALTER TRIGGER "ATLAS2"."A2_SPEC_INSERT" ENABLE;
--------------------------------------------------------
--  DDL for View A2_ONTOLOGYMAPPING
--------------------------------------------------------

-- COMMIT WORK;
--ROLLBACK;