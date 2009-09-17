/******************************************************************************

******************************************************************************/

  CREATE OR REPLACE TRIGGER "A2_ASSAY_INSERT" 

before insert on A2_Assay
for each row
begin
if(:new.AssayID is null) then
select A2_Assay_seq.nextval into :new.AssayID from dual;
end if;
end;
/
ALTER TRIGGER "A2_ASSAY_INSERT" ENABLE;
 
  CREATE OR REPLACE TRIGGER "A2_ASSAYONTOLOGY_INSERT" 

before insert on A2_AssayOntology
for each row
begin
if(:new.AssayOntologyID is null) then
select A2_AssayOntology_seq.nextval into :new.AssayOntologyID from dual;
end if;
end;
/
ALTER TRIGGER "A2_ASSAYONTOLOGY_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_ASSAYPROPERTYVALUE_INSERT" 

before insert on A2_AssayPropertyValue
for each row
begin
if(:new.AssayPropertyValueID is null) then
select A2_AssayPropertyValue_seq.nextval into :new.AssayPropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "A2_ASSAYPROPERTYVALUE_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_ASSAYSAMPLE_INSERT" 

before insert on A2_AssaySample
for each row
begin
if(:new.AssaySampleID is null) then
select A2_AssaySample_seq.nextval into :new.AssaySampleID from dual;
end if;
end;
/
ALTER TRIGGER "A2_ASSAYSAMPLE_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_EXPERIMENT_INSERT" 

before insert on A2_Experiment
for each row
begin
if(:new.ExperimentID is null) then
  select A2_Experiment_seq.nextval into :new.ExperimentID from dual;
end if;
end;

/
ALTER TRIGGER "A2_EXPERIMENT_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_EXPRESSIONVALUE_INSERT" 

before insert on A2_ExpressionValue
for each row
begin
if(:new.ExpressionValueID is null) then
select A2_ExpressionValue_seq.nextval into :new.ExpressionValueID from dual;
end if
end;
/
ALTER TRIGGER "A2_EXPRESSIONVALUE_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_GENE_INSERT" 

before insert on A2_GENE
for each row
begin
if(:new.GENEID is null) then
select A2_GENE_seq.nextval into :new.GENEID from dual;
end if;
end;
/
ALTER TRIGGER "A2_GENE_INSERT" ENABLE;

  CREATE OR REPLACE TRIGGER "A2_GENEProperty_INSERT" 

before insert on A2_GENEProperty
for each row
begin
if(:new.GENEPropertyID is null) then
select A2_GENEProperty_seq.nextval into :new.GENEPropertyID from dual;
end if;
end;
/
ALTER TRIGGER "A2_GENEProperty_INSERT" ENABLE;

  CREATE OR REPLACE TRIGGER "A2_GENEPropertyValue_INSERT" 

before insert on A2_GENEPropertyValue
for each row
begin
if(:new.GENEPropertyValueID is null) then
select A2_GENEPropertyValue_seq.nextval into :new.GENEPropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "A2_GENEPropertyValue_INSERT" ENABLE;


  CREATE OR REPLACE TRIGGER "A2_ONTOLOGYTERM_INSERT" 

before insert on A2_OntologyTerm
for each row
begin
if( :new.OntologyTermID is null) then
select A2_OntologyTerm_seq.nextval into :new.OntologyTermID from dual;
end if;
end;
/
ALTER TRIGGER "A2_ONTOLOGYTERM_INSERT" ENABLE;


   CREATE OR REPLACE TRIGGER "A2_PROPERTY_INSERT" 

before insert on A2_Property
for each row
begin
if(:new.PropertyID is null) then
  select A2_Property_seq.nextval into :new.PropertyID from dual;
end if;
end;
/
ALTER TRIGGER "A2_PROPERTY_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_PROPERTYVALUE_INSERT" 

before insert on A2_PropertyValue
for each row
begin
if ( :new.PropertyValueID is null) then 
  select A2_PropertyValue_seq.nextval into :new.PropertyValueID from dual;
end if;
end;
/
ALTER TRIGGER "A2_PROPERTYVALUE_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_SAMPLE_INSERT" 

before insert on A2_Sample
for each row
begin
if(:new.SampleID is null) then
select A2_Sample_seq.nextval into :new.SampleID from dual;
end if;
end;
/
ALTER TRIGGER "A2_SAMPLE_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_SAMPLEONTOLOGY_INSERT" 

before insert on A2_SampleOntology
for each row
begin
if(:new.SampleOntologyID is null) then
select A2_SampleOntology_seq.nextval into :new.SampleOntologyID from dual;
end if;
end;

/
ALTER TRIGGER "A2_SAMPLEONTOLOGY_INSERT" ENABLE;
 

  CREATE OR REPLACE TRIGGER "A2_SAMPLEPROPERTYVALUE_INSERT" 

before insert on A2_SamplePropertyValue
for each row
begin
if(:new.SamplePropertyValueID is null) then
select A2_SamplePropertyValue_seq.nextval into :new.SamplePropertyValueID from dual;
end if;
end;

/
ALTER TRIGGER "A2_SAMPLEPROPERTYVALUE_INSERT" ENABLE;

  CREATE OR REPLACE TRIGGER "A2_SPEC_INSERT" 

before insert on A2_SPEC
for each row
begin
if(:new.SPECID is null) then
select A2_SPEC_seq.nextval into :new.SPECID from dual;
end if;
end;

/
ALTER TRIGGER "A2_SPEC_INSERT" ENABLE;

CREATE OR REPLACE TRIGGER "A2_ArrayDesign_Insert" 

before insert on A2_ArrayDesign
for each row
begin
if( :new.ArrayDesignID is null) then
select A2_ArrayDesign_seq.nextval into :new.ArrayDesignID from dual;
end if;
end;

/
ALTER TRIGGER "A2_ArrayDesign_Insert" ENABLE;

CREATE OR REPLACE TRIGGER "A2_DesignElement_Insert" 

before insert on A2_DesignElement
for each row
begin
if(:new.DesignElementID is null) then
select A2_DesignElement_seq.nextval into :new.DesignElementID from dual;
end if;
end;

/
ALTER TRIGGER "A2_DesignElement_Insert" ENABLE;

CREATE OR REPLACE TRIGGER "A2_ExpressionAnalytics_Insert" 

before insert on A2_ExpressionAnalytics
for each row
begin
if(:new.ExpressionID is null) then
select A2_ExpressionAnalytics_seq.nextval into :new.ExpressionID from dual;
end if;
end;

/
ALTER TRIGGER "A2_ExpressionAnalytics_Insert" ENABLE;










 
