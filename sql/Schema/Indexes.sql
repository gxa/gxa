 /*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa/
 */

/*******************************************************************************/
/*  Indexes and constraints in Atlas schema
/*
/*  @author rpetry
/*******************************************************************************/


--------------------------------------------------------
--  ORGANISM
--------------------------------------------------------
  ALTER TABLE "A2_ORGANISM"
  ADD CONSTRAINT "PK_ORGANISM"
    PRIMARY KEY ("ORGANISMID")
  /*PK_TABLESPACE*/
  ENABLE;

  ALTER TABLE "A2_ORGANISM"
  ADD CONSTRAINT "UQ_ORGANISM_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- GENE PROPERTY
--------------------------------------------------------
  ALTER TABLE "A2_GENEPROPERTY"
  ADD CONSTRAINT "PK_GENEPROPERTY"
    PRIMARY KEY ("GENEPROPERTYID")
    /*PK_TABLESPACE*/
    ENABLE;


  ALTER TABLE "A2_GENEPROPERTY"
  ADD CONSTRAINT "UQ_GENEPROPERTY_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- GENE PROPERTYVALUE
--------------------------------------------------------

  ALTER TABLE "A2_GENEPROPERTYVALUE"
  ADD CONSTRAINT "PK_GENEPROPERTYVALUE"
    PRIMARY KEY ("GENEPROPERTYVALUEID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_GENEPROPERTYVALUE_PROPERTY"
  ON "A2_GENEPROPERTYVALUE" ("GENEPROPERTYID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_GENEPROPERTYVALUE"
  ADD CONSTRAINT "FK_GENEPROPERTYVALUE_PROPERTY"
    FOREIGN KEY ("GENEPROPERTYID")
    REFERENCES "A2_GENEPROPERTY" ("GENEPROPERTYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_GENEPROPERTYVALUE"
  ADD CONSTRAINT "UQ_GENEPROPERTYVALUE_VALUE"
    UNIQUE("GENEPROPERTYID","VALUE")
    ENABLE;

--------------------------------------------------------
-- GENE
--------------------------------------------------------
  ALTER TABLE "A2_GENE"
  ADD CONSTRAINT "PK_GENE"
    PRIMARY KEY ("GENEID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_GENE_ORGANISM"
  ON "A2_GENE" ("ORGANISMID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_GENE"
  ADD CONSTRAINT "FK_GENE_ORGANISM"
    FOREIGN KEY ("ORGANISMID")
    REFERENCES "A2_ORGANISM" ("ORGANISMID")
    ON DELETE CASCADE
    ENABLE;


 ALTER TABLE "A2_GENE"
  ADD CONSTRAINT "UQ_GENE_IDENTIFIER"
    UNIQUE("IDENTIFIER")
    ENABLE;

--------------------------------------------------------
-- GENE GENEPROPERTYVALUE
--------------------------------------------------------
  ALTER TABLE "A2_GENEGPV"
  ADD CONSTRAINT "PK_GENEGPV"
    PRIMARY KEY ("GENEGPVID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_GENEGPV_PROPERTY"
  ON "A2_GENEGPV" ("GENEPROPERTYVALUEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_GENEGPV_GENE"
  ON "A2_GENEGPV" ("GENEID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_GENEGPV"
  ADD CONSTRAINT "FK_GENEGPV_GENE"
    FOREIGN KEY ("GENEID")
    REFERENCES "A2_GENE" ("GENEID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_GENEGPV"
  ADD CONSTRAINT "FK_GENEGPV_GPV"
    FOREIGN KEY ("GENEPROPERTYVALUEID")
    REFERENCES "A2_GENEPROPERTYVALUE" ("GENEPROPERTYVALUEID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_GENEGPV"
  ADD CONSTRAINT "UQ_GENEGPV"
    UNIQUE("GENEID","GENEPROPERTYVALUEID")
    ENABLE;

--------------------------------------------------------
--  MAPPING SOURCE
--------------------------------------------------------

  ALTER TABLE "A2_SOFTWARE"
  ADD CONSTRAINT "PK_SOFTWARE"
    PRIMARY KEY ("SOFTWAREID")
  /*PK_TABLESPACE*/
  ENABLE;


  ALTER TABLE "A2_SOFTWARE"
  ADD CONSTRAINT "UQ_SOFTWARE_NAME_VERSION"
    UNIQUE("NAME", "VERSION")
    ENABLE;

--------------------------------------------------------
-- ARRAY DESIGN
--------------------------------------------------------
  ALTER TABLE "A2_ARRAYDESIGN"
  ADD CONSTRAINT "PK_ARRAYDESIGN"
    PRIMARY KEY ("ARRAYDESIGNID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_ARRAYDESIGN_ACCESSION"
  ON "A2_ARRAYDESIGN" ("ACCESSION")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_ARRAYDESIGN"
  ADD CONSTRAINT "UQ_ARRAYDESIGN_NAME"
    UNIQUE("NAME")
    ENABLE;


  ALTER TABLE "A2_ARRAYDESIGN"
  ADD CONSTRAINT "UQ_ARRAYDESIGN_ACCESSION"
    UNIQUE("ACCESSION")
    ENABLE;

--------------------------------------------------------
-- DESIGN ELEMENT
--------------------------------------------------------
  ALTER TABLE "A2_DESIGNELEMENT"
  ADD CONSTRAINT "PK_DESIGNELEMENT"
    PRIMARY KEY ("DESIGNELEMENTID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_DESIGNELEMENT_GENE"
  ON "A2_DESIGNELEMENT" ("GENEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_DESIGNELEMENT_ARRAYDESIGN"
  ON "A2_DESIGNELEMENT" ("ARRAYDESIGNID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_DESIGNELEMENT"
  ADD CONSTRAINT "FK_DESIGNELEMENT_ARRAYDESIGN"
    FOREIGN KEY ("ARRAYDESIGNID")
    REFERENCES "A2_ARRAYDESIGN" ("ARRAYDESIGNID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_DESIGNELEMENT"
    ADD CONSTRAINT "FK_DESIGNELEMENT_GENE"
    FOREIGN KEY ("GENEID")
    REFERENCES "A2_GENE" ("GENEID")
    ON DELETE CASCADE
    ENABLE;

  ALTER TABLE "A2_DESIGNELEMENT"
  ADD CONSTRAINT "UQ_DESIGNELEMENT_ACCESSION"
    UNIQUE("ARRAYDESIGNID","ACCESSION")
    ENABLE;

--------------------------------------------------------
-- PROPERTY
--------------------------------------------------------
  ALTER TABLE "A2_PROPERTY"
  ADD CONSTRAINT "PK_PROPERTY"
    PRIMARY KEY ("PROPERTYID")
    /*PK_TABLESPACE*/
    ENABLE;

  CREATE INDEX "IDX_PROPERTY_NAME"
  ON "A2_PROPERTY" ("NAME")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_PROPERTY"
  ADD CONSTRAINT "UQ_PROPERTY_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- PROPERTY VALUE
--------------------------------------------------------
  ALTER TABLE "A2_PROPERTYVALUE"
  ADD CONSTRAINT "PK_PROPERTYVALUE"
    PRIMARY KEY ("PROPERTYVALUEID")
  /*PK_TABLESPACE*/
  ENABLE;


  CREATE INDEX "IDX_PROPERTYVALUE_NAME"
  ON "A2_PROPERTYVALUE" ("NAME")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_PROPERTYVALUE_PROPERTY"
  ON "A2_PROPERTYVALUE" ("PROPERTYID")
  /*INDEX_TABLESPACE*/;

  ALTER TABLE "A2_PROPERTYVALUE"
  ADD CONSTRAINT "FK_PROPERTYVALUE_PROPERTY"
    FOREIGN KEY ("PROPERTYID")
    REFERENCES "A2_PROPERTY" ("PROPERTYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_PROPERTYVALUE"
  ADD CONSTRAINT "UQ_PROPERTYVALUE_NAME"
    UNIQUE("PROPERTYID","NAME")
    ENABLE;

--------------------------------------------------------
-- EXPERIMENT
--------------------------------------------------------
  ALTER TABLE "A2_EXPERIMENT"
  ADD CONSTRAINT "PK_EXPERIMENT"
    PRIMARY KEY ("EXPERIMENTID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_EXPERIMENT_ACCESSION"
  ON "A2_EXPERIMENT" ("ACCESSION")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_EXPERIMENT"
  ADD CONSTRAINT "UQ_EXPERIMENT_ACCESSION"
    UNIQUE("ACCESSION")
    ENABLE;

--------------------------------------------------------
-- ASSET - file,figure,picture,etc. COMMIT
--------------------------------------------------------
 ALTER TABLE "A2_EXPERIMENTASSET"
  ADD CONSTRAINT "PK_EXPERIMENTASSET"
    PRIMARY KEY ("EXPERIMENTASSETID")
    /*PK_TABLESPACE*/
    ENABLE;

  ALTER TABLE "A2_EXPERIMENTASSET"
  ADD CONSTRAINT "FK_EXPERIMENTASSET_EXPERIMENT"
    FOREIGN KEY ("EXPERIMENTID")
    REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
    ON DELETE CASCADE
    ENABLE;

  CREATE INDEX "IDX_EXPERIMENTASSET_EXPERIMENT"
  ON "A2_EXPERIMENTASSET" ("EXPERIMENTID")
  /*INDEX_TABLESPACE*/;

--------------------------------------------------------
-- ASSAY
--------------------------------------------------------

  ALTER TABLE "A2_ASSAY"
  ADD CONSTRAINT "PK_ASSAY"
    PRIMARY KEY ("ASSAYID")
    /*PK_TABLESPACE*/
    ENABLE;


  ALTER TABLE "A2_ASSAY"
  ADD CONSTRAINT "FK_ASSAY_ARRAYDESIGN"
    FOREIGN KEY ("ARRAYDESIGNID")
    REFERENCES "A2_ARRAYDESIGN" ("ARRAYDESIGNID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_ASSAY"
  ADD CONSTRAINT "FK_ASSAY_EXPERIMENT"
    FOREIGN KEY ("EXPERIMENTID")
    REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
    ON DELETE CASCADE
    ENABLE;


  CREATE INDEX "IDX_ASSAY_ARRAYDESIGN"
  ON "A2_ASSAY" ("ARRAYDESIGNID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_ASSAY_EXPERIMENT"
  ON "A2_ASSAY" ("EXPERIMENTID")
  /*INDEX_TABLESPACE*/;


 ALTER TABLE "A2_ASSAY"
  ADD CONSTRAINT "UQ_ASSAY_ACCESSION"
    UNIQUE("EXPERIMENTID","ACCESSION")
    ENABLE;

--------------------------------------------------------
-- SAMPLE
--------------------------------------------------------
  ALTER TABLE "A2_SAMPLE"
  ADD CONSTRAINT "PK_SAMPLE"
    PRIMARY KEY ("SAMPLEID")
  /*PK_TABLESPACE*/
  ENABLE;

  ALTER TABLE "A2_SAMPLE"
  ADD CONSTRAINT "FK_SAMPLE_EXPERIMENT"
    FOREIGN KEY ("EXPERIMENTID")
    REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
    ON DELETE CASCADE
    ENABLE;

CREATE INDEX "IDX_SAMPLE_EXPERIMENT"
  ON "A2_SAMPLE" (EXPERIMENTID)
  /*INDEX_TABLESPACE*/;

  ALTER TABLE "A2_SAMPLE"
  ADD CONSTRAINT "FK_SAMPLE_ORGANISM"
    FOREIGN KEY ("ORGANISMID")
    REFERENCES "A2_ORGANISM" ("ORGANISMID");

--------------------------------------------------------
-- ASSAYSAMPLE
--------------------------------------------------------
  ALTER TABLE "A2_ASSAYSAMPLE"
  ADD CONSTRAINT "PK_ASSAYSAMPLE"
    PRIMARY KEY ("ASSAYID", "SAMPLEID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_ASSAYSAMPLE_ASSAY"
  ON "A2_ASSAYSAMPLE" ("ASSAYID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_ASSAYSAMPLE_SAMPLE"
  ON "A2_ASSAYSAMPLE" ("SAMPLEID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_ASSAYSAMPLE"
  ADD CONSTRAINT "FK_ASSAYSAMPLE_ASSAY"
    FOREIGN KEY ("ASSAYID")
    REFERENCES "A2_ASSAY" ("ASSAYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_ASSAYSAMPLE"
  ADD CONSTRAINT "FK_ASSAYSAMPLE_SAMPLE"
    FOREIGN KEY ("SAMPLEID")
    REFERENCES "A2_SAMPLE" ("SAMPLEID")
    ON DELETE CASCADE
    ENABLE;

--------------------------------------------------------
-- ASSAYPROPERTYVALUE
--------------------------------------------------------
  ALTER TABLE "A2_ASSAYPV"
  ADD CONSTRAINT "PK_ASSAYPV"
    PRIMARY KEY ("ASSAYPVID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_ASSAYPV_PROPERTYVALUE"
  ON "A2_ASSAYPV" ("PROPERTYVALUEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_ASSAYPV_ASSAY"
  ON "A2_ASSAYPV" ("ASSAYID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_ASSAYPV_AP"
  ON "A2_ASSAYPV" ("ASSAYID", "PROPERTYVALUEID")
  /*INDEX_TABLESPACE*/; --composite index


  ALTER TABLE "A2_ASSAYPV"
  ADD CONSTRAINT "FK_ASSAYPV_PROPERTY"
    FOREIGN KEY ("PROPERTYVALUEID")
    REFERENCES "A2_PROPERTYVALUE" ("PROPERTYVALUEID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_ASSAYPV"
  ADD CONSTRAINT "FK_ASSAYPV_ASSAY"
    FOREIGN KEY ("ASSAYID")
    REFERENCES "A2_ASSAY" ("ASSAYID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_ASSAYPV"
  ADD CONSTRAINT "UQ_ASSAYPV"
    UNIQUE(ASSAYID, PropertyValueID)
    ENABLE;

--------------------------------------------------------
-- SAMPLEPROPERTYVALUE
--------------------------------------------------------
 ALTER TABLE "A2_SAMPLEPV"
  ADD CONSTRAINT "PK_SAMPLEPV"
    PRIMARY KEY ("SAMPLEPVID")
  /*PK_TABLESPACE*/
  ENABLE;


  CREATE INDEX "IDX_SAMPLEPROPERTY_PROPERTY"
  ON "A2_SAMPLEPV" ("PROPERTYVALUEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_SAMPLEPROPERTY_PS"
  ON "A2_SAMPLEPV" ("PROPERTYVALUEID", "SAMPLEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_SAMPLEPROPERTY_SAMPLE"
  ON "A2_SAMPLEPV" ("SAMPLEID")
  /*INDEX_TABLESPACE*/;


  CREATE INDEX "IDX_SAMPLEPROPERTY_SP"
  ON "A2_SAMPLEPV" ("SAMPLEID", "PROPERTYVALUEID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_SAMPLEPV"
  ADD CONSTRAINT "FK_SAMPLEPROPERTY_SAMPLE"
    FOREIGN KEY ("SAMPLEID")
    REFERENCES "A2_SAMPLE" ("SAMPLEID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_SAMPLEPV"
  ADD CONSTRAINT "FK_SAMPLEPROPERTY_PROPERTY"
    FOREIGN KEY ("PROPERTYVALUEID")
    REFERENCES "A2_PROPERTYVALUE" ("PROPERTYVALUEID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_SAMPLEPV"
  ADD CONSTRAINT "UQ_SAMPLEPV"
    UNIQUE(SAMPLEID, PropertyValueID)
    ENABLE;
--------------------------------------------------------
-- ONTOLOGY
--------------------------------------------------------
  ALTER TABLE "A2_ONTOLOGY"
  ADD CONSTRAINT "PK_ONTOLOGY"
    PRIMARY KEY ("ONTOLOGYID")
    /*PK_TABLESPACE*/
    ENABLE;

--------------------------------------------------------
-- ONTOLOGY TERM
--------------------------------------------------------
 ALTER TABLE "A2_ONTOLOGYTERM"
  ADD CONSTRAINT "PK_ONTOLOGYTERM"
    PRIMARY KEY ("ONTOLOGYTERMID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_ONTOLOGYTERM_ONTOLOGY"
  ON "A2_ONTOLOGYTERM" ("ONTOLOGYID")
  /*INDEX_TABLESPACE*/;


  ALTER TABLE "A2_ONTOLOGYTERM"
  ADD CONSTRAINT "FK_ONTOLOGYTERM_ONTOLOGY"
    FOREIGN KEY ("ONTOLOGYID")
    REFERENCES "A2_ONTOLOGY" ("ONTOLOGYID")
    ON DELETE CASCADE
    ENABLE;


 ALTER TABLE "A2_ONTOLOGYTERM"
  ADD CONSTRAINT "UQ_ONTOLOGYTERM_ACCESSION"
    UNIQUE(ONTOLOGYID, "ACCESSION")
    ENABLE;

--------------------------------------------------------
-- ASSAYPROPERTYVALUEONTOLOGY
--------------------------------------------------------
  ALTER TABLE "A2_ASSAYPVONTOLOGY"
  ADD CONSTRAINT "PK_ASSAYPVONTOLOGY"
    PRIMARY KEY ("ONTOLOGYTERMID", "ASSAYPVID")
    /*PK_TABLESPACE*/
    ENABLE;


  ALTER TABLE "A2_ASSAYPVONTOLOGY"
  ADD CONSTRAINT "FK_ASSAYPVONTOLOGY_ASSAYPV"
    FOREIGN KEY ("ASSAYPVID")
    REFERENCES "A2_ASSAYPV" ("ASSAYPVID")
    ON DELETE CASCADE
    ENABLE;


  ALTER TABLE "A2_ASSAYPVONTOLOGY"
  ADD CONSTRAINT "FK_ASSAYPVONTOLOGY_ONTOLOGY"
    FOREIGN KEY ("ONTOLOGYTERMID")
    REFERENCES "A2_ONTOLOGYTERM" ("ONTOLOGYTERMID")
    ON DELETE CASCADE
    ENABLE;

--------------------------------------------------------
-- SAMPLEPROPERTYVALUEONTOLOGY
--------------------------------------------------------
  ALTER TABLE "A2_SAMPLEPVONTOLOGY"
  ADD CONSTRAINT "PK_SAMPLEPVONTOLOGY"
    PRIMARY KEY ("ONTOLOGYTERMID", "SAMPLEPVID")
  /*PK_TABLESPACE*/
  ENABLE;


  ALTER TABLE "A2_SAMPLEPVONTOLOGY"
  ADD CONSTRAINT "FK_SAMPLEPVONTOLOGY_SAMPLEPV"
    FOREIGN KEY ("SAMPLEPVID")
    REFERENCES "A2_SAMPLEPV" ("SAMPLEPVID")
    ON DELETE CASCADE
    ENABLE;

  ALTER TABLE "A2_SAMPLEPVONTOLOGY"
  ADD CONSTRAINT "FK_SAMPLEPVONTOLOGY_ONTOLOGY"
    FOREIGN KEY ("ONTOLOGYTERMID")
    REFERENCES "A2_ONTOLOGYTERM" ("ONTOLOGYTERMID")
    ON DELETE CASCADE
    ENABLE;

--------------------------------------------------------
-- BIOENTITY PROPERTY
--------------------------------------------------------
  ALTER TABLE "A2_BIOENTITYPROPERTY"
  ADD CONSTRAINT "PK_BIOENTITYPROPERTY"
    PRIMARY KEY ("BIOENTITYPROPERTYID")
    /*PK_TABLESPACE*/
    ENABLE;


  ALTER TABLE "A2_BIOENTITYPROPERTY"
  ADD CONSTRAINT "UQ_BIOENTITYPROPERTY_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- BIOENTITY PROPERTYVALUE
--------------------------------------------------------
  ALTER TABLE "A2_BIOENTITYPROPERTYVALUE"
  ADD CONSTRAINT "PK_BEPROPERTYVALUE"
    PRIMARY KEY ("BEPROPERTYVALUEID")
    /*PK_TABLESPACE*/
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
--------------------------------------------------------
-- BIOENTITYTYPE
--------------------------------------------------------
  ALTER TABLE "A2_BIOENTITYTYPE"
  ADD CONSTRAINT "PK_BIOENTITYTYPE"
    PRIMARY KEY ("BIOENTITYTYPEID")
    /*PK_TABLESPACE*/
    ENABLE;

 ALTER TABLE "A2_BIOENTITYTYPE"
  ADD CONSTRAINT "UQ_BIOENTITYTYPE_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- BIOENTITY
--------------------------------------------------------
  ALTER TABLE "A2_BIOENTITY"
  ADD CONSTRAINT "PK_BIOENTITY"
    PRIMARY KEY ("BIOENTITYID")
    /*PK_TABLESPACE*/
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

--------------------------------------------------------
-- BERELATIONTYPE
--------------------------------------------------------
  ALTER TABLE "A2_BERELATIONTYPE"
  ADD CONSTRAINT "PK_BERELATIONTYPE"
    PRIMARY KEY ("BERELATIONTYPEID")
    /*PK_TABLESPACE*/
    ENABLE;

 ALTER TABLE "A2_BERELATIONTYPE"
  ADD CONSTRAINT "UQ_BERELATIONTYPE_NAME"
    UNIQUE("NAME")
    ENABLE;

--------------------------------------------------------
-- BIOENTITY BIOENTITYPROPERTYVALUE
--------------------------------------------------------
  ALTER TABLE "A2_BIOENTITYBEPV"
  ADD CONSTRAINT "PK_BIOENTITYBEPV"
    PRIMARY KEY ("BIOENTITYBEPVID")
    /*PK_TABLESPACE*/
    ENABLE;


  CREATE INDEX "IDX_BIOENTITYBEPV_ASSOCIATION"
  ON "A2_BIOENTITYBEPV" (BIOENTITYID, SOFTWAREID, bepropertyvalueid)
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

--------------------------------------------------------
-- DESIGNELEMENT BIOENTITY
--------------------------------------------------------
  ALTER TABLE "A2_DESIGNELTBIOENTITY"
  ADD CONSTRAINT "PK_DESIGNELTBIOENTITY"
    PRIMARY KEY ("DEBEID")
    /*PK_TABLESPACE*/
    ENABLE;

CREATE INDEX "IDX_DESIGNELTBIOENTITY_ASS"
  ON A2_DESIGNELTBIOENTITY (DESIGNELEMENTID, BIOENTITYID)
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

CREATE BITMAP INDEX "IDX_BIOENTITYBEPV_BEID_SWID"
ON "A2_BIOENTITYBEPV" (BIOENTITYID, SOFTWAREID)
/*INDEX_TABLESPACE*/;

--------------------------------------------------------
--  TASK MANAGER DATA STRUCTURES
--------------------------------------------------------
CREATE INDEX A2_TASKMAN_LOG_IDX1 ON A2_TASKMAN_LOG (TYPE, ACCESSION) NOPARALLEL /*INDEX_TABLESPACE*/;

CREATE INDEX A2_TASKMAN_LOG_IDX2 ON A2_TASKMAN_LOG (TASKID) NOPARALLEL /*INDEX_TABLESPACE*/;

CREATE INDEX A2_TASKMAN_LOG_IDX3 ON A2_TASKMAN_LOG (USERNAME) NOPARALLEL /*INDEX_TABLESPACE*/;

CREATE INDEX A2_TASKMAN_LOG_IDX4 ON A2_TASKMAN_LOG (TYPE) NOPARALLEL /*INDEX_TABLESPACE*/;

CREATE INDEX A2_TASKMAN_LOG_IDX5 ON A2_TASKMAN_LOG (EVENT) NOPARALLEL /*INDEX_TABLESPACE*/;

CREATE SEQUENCE A2_TASKMAN_LOG_SEQ ;


 CREATE INDEX A2_TASKMAN_TAG_IDX1 ON A2_TASKMAN_TAG (TAGTYPE, TAG) NOPARALLEL /*INDEX_TABLESPACE*/;

 CREATE INDEX A2_TASKMAN_TAG_IDX2 ON A2_TASKMAN_TAG (TAGTYPE) NOPARALLEL /*INDEX_TABLESPACE*/;

-------------------------------------------------------------------------------
-- Curators comments
-- drop table A2_EXPERIMENTLOG
-------------------------------------------------------------------------------
ALTER TABLE "A2_EXPERIMENTLOG"
ADD CONSTRAINT "FK_EXPERIMENTLOG_EXPERIMENT"
  FOREIGN KEY ("EXPERIMENTID")
  REFERENCES "A2_EXPERIMENT" ("EXPERIMENTID")
  ON DELETE CASCADE
  ENABLE;


CREATE INDEX A2_EXPERIMENTLOG_IDX1 ON A2_EXPERIMENTLOG (EXPERIMENTID,TYPE) /*INDEX_TABLESPACE*/;

ALTER TABLE A2_ANNOTATIONSRC
  ADD CONSTRAINT PK_ANNOTATIONSRC
    PRIMARY KEY (annotationsrcid)
  ENABLE;

ALTER TABLE A2_ANNOTATIONSRC
  ADD CONSTRAINT FK_ANNOTATIONSRC_ORGANISM
    FOREIGN KEY (ORGANISMID)
    REFERENCES A2_ORGANISM (ORGANISMID)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_ANNOTATIONSRC
  ADD CONSTRAINT UQ_ANNSRC_NAME_V_ORG_TYPE
    UNIQUE(SOFTWAREID, ANNSRCTYPE, ORGANISMID)
    ENABLE;

ALTER TABLE "A2_ANNOTATIONSRC"
  ADD CONSTRAINT "FK_ANNSRC_SOFTWARE"
    FOREIGN KEY ("SOFTWAREID")
    REFERENCES "A2_SOFTWARE" ("SOFTWAREID")
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_ANNSRC_BIOENTITYTYPE
  ADD CONSTRAINT UQ_ANNSRCBETYPE_AS_BET
    UNIQUE(annotationsrcid, BIOENTITYTYPEID)
    ENABLE;

ALTER TABLE A2_ANNSRC_BIOENTITYTYPE
  ADD CONSTRAINT FK_A2_ANNSRCBETYPE_BETYPE
    FOREIGN KEY (BIOENTITYTYPEID)
    REFERENCES A2_BIOENTITYTYPE (BIOENTITYTYPEID)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_ANNSRC_BIOENTITYTYPE
  ADD CONSTRAINT FK_ANNSRCBETYPE_ANNSRC
    FOREIGN KEY (annotationsrcid)
    REFERENCES A2_ANNOTATIONSRC (annotationsrcid)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_BIOMARTPROPERTY
  ADD CONSTRAINT PK_BMPROPERTY
    PRIMARY KEY (biomartpropertyId)
  ENABLE;

ALTER TABLE A2_BIOMARTPROPERTY
  ADD CONSTRAINT FK_BIOMARTPROPERTY_PROPERTY
    FOREIGN KEY (BIOENTITYPROPERTYID)
    REFERENCES A2_BIOENTITYPROPERTY (BIOENTITYPROPERTYID)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_BIOMARTPROPERTY
  ADD CONSTRAINT FK_BIOMARTPROPERTY_ANNSRC
    FOREIGN KEY (annotationsrcid)
    REFERENCES A2_ANNOTATIONSRC (annotationsrcid)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_BIOMARTPROPERTY
  ADD CONSTRAINT UQ_BMPROP_ANNSRCID_NAME_BEPROP
    UNIQUE(annotationsrcid, NAME, BIOENTITYPROPERTYID)
    ENABLE;

ALTER TABLE A2_BIOMARTARRAYDESIGN
  ADD CONSTRAINT PK_BIOMARTARRAYDESIGN
    PRIMARY KEY (BIOMARTARRAYDESIGNID)
  ENABLE;

ALTER TABLE A2_BIOMARTARRAYDESIGN
  ADD CONSTRAINT FK_BIOMARTARRAYDESIGN_AD
    FOREIGN KEY (ARRAYDESIGNID)
    REFERENCES A2_ARRAYDESIGN (ARRAYDESIGNID)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_BIOMARTARRAYDESIGN
  ADD CONSTRAINT FK_BMAD_ANNSRC
    FOREIGN KEY (annotationsrcid)
    REFERENCES A2_ANNOTATIONSRC (annotationsrcid)
    ON DELETE CASCADE
    ENABLE;

ALTER TABLE A2_BIOMARTARRAYDESIGN
  ADD CONSTRAINT UQ_BMAD_ANNSRCID_NAME
    UNIQUE(annotationsrcid, NAME)
    ENABLE;

exit;