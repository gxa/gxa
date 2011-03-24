/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
/*  Views in Atlas2 schema     
/*
/* select * from A2_ONTOLOGYMAPPING where Accession = 'E-MEXP-171' and PropertyValue ='ectoderm'   
/*
/*
/*******************************************************************************/
 CREATE OR REPLACE VIEW "A2_ONTOLOGYMAPPING" 
 AS SELECT distinct  e.accession, 
         p.name AS Property, 
         pv.name AS PropertyValue, 
         ot.accession AS OntologyTerm, 
         ot.term AS OntologyTermName, 
         ot.OntologyTermID AS OntologyTermID, 
         o.Name OntologyName, 
         1 IsSampleProperty, 
         0 IsAssayProperty, 
         pv.PropertyValueID,
         e.ExperimentID 
 FROM a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
 JOIN a2_assay ass ON ass.ExperimentID = e.ExperimentID 
 JOIN a2_assaysample asss ON asss.AssayID = ass.AssayID 
 JOIN a2_sample s ON s.SampleID = asss.SampleID 
 JOIN a2_samplePV spv ON spv.SampleID = s.SampleID
 JOIN a2_samplePVontology so ON so.SamplePVID = spv.SamplePVID 
 JOIN a2_propertyvalue pv ON pv.PropertyValueID = spv.PropertyValueID 
 JOIN a2_property p ON p.PropertyID = pv.PropertyID 
 JOIN a2_ontologyterm ot ON ot.OntologyTermID = so.OntologyTermID 
 JOIN a2_ontology o ON o.OntologyID = ot.OntologyID 
 UNION ALL 
SELECT distinct e.accession, 
         p.name AS Property, 
         pv.name AS PropertyValue, 
         ot.accession AS OntologyTerm, 
         ot.term AS OntologyTermName, 
         ot.OntologyTermID AS OntologyTermID, 
         o.Name OntologyName, 
         0 IsSampleProperty, 
         1 IsAssayProperty, 
         pv.PropertyValueID,
         e.ExperimentID 
  FROM a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
  JOIN a2_assay ass ON ass.ExperimentID = e.ExperimentID 
  JOIN a2_assayPV apv ON apv.assayID = ass.AssayID
  JOIN a2_assayPVontology ao ON ao.AssayPVID = apv.assayPVID
  JOIN a2_propertyvalue pv ON pv.PropertyValueID = apv.PropertyValueID 
  JOIN a2_property p ON p.PropertyID = pv.PropertyID 
  JOIN a2_ontologyterm ot ON ot.OntologyTermID = ao.OntologyTermID 
  JOIN a2_ontology o ON o.OntologyID = ot.OntologyID
/
--------------------------------------------------------
--  DDL for View VWARRAYDESIGN
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWARRAYDESIGN" ("ARRAYDESIGNID", "NAME", "TYPE", "PROVIDER") AS SELECT ArrayDesignID
          , Name
          , Type
          , Provider
FROM a2_ArrayDesign
/
--------------------------------------------------------
--  DDL for View VWARRAYDESIGNELEMENT
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWARRAYDESIGNELEMENT" ("ARRAYDESIGNID", "DESIGNELEMENTID", "GENEIDENTIFIER") AS select de.ArrayDesignID
     , de.DesignElementID
     , g.Identifier GeneIdentifier
from a2_DesignElement de
join a2_Gene g on g.GeneID = de.GeneID
/
--------------------------------------------------------
--  DDL for View VWASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWASSAY" ("ASSAYID", "ACCESSION", "EXPERIMENTACCESSION") AS select a.AssayID
       ,a.Accession
       ,e.Accession ExperimentAccession
from a2_Assay a
join a2_Experiment e on a.ExperimentID = e.ExperimentID
/
--------------------------------------------------------
--  DDL for View VWASSAYPROPERTY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWASSAYPROPERTY" ("ASSAYID", "PROPERTY", "PROPERTYVALUE") AS select a.AssayID
    ,p.Name Property
    ,pv.Name PropertyValue
from a2_Assay a
join a2_AssayPV ap on ap.AssayID = a.AssayID
join a2_PropertyValue pv on pv.PropertyValueID = ap.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID
/
--------------------------------------------------------
--  DDL for View VWASSAYSAMPLE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWASSAYSAMPLE" ("ASSAYID", "SAMPLEACCESSION") AS select a.AssayID
      ,s.Accession SampleAccession
from a2_Assay a
join a2_AssaySample as1 on as1.AssayID = a.AssayID
join a2_Sample s on s.SampleID = as1.SampleID
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENT
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWEXPERIMENT" ("EXPERIMENTID", "ACCESSION", "DESCRIPTION", "PERFORMER", "LAB", "LOADDATE") AS select "EXPERIMENTID","ACCESSION","DESCRIPTION","PERFORMER","LAB","LOADDATE" from a2_Experiment
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWEXPERIMENTASSAY" ("EXPERIMENTID", "ASSAYID", "ACCESSION") AS select e.ExperimentID
          ,a.AssayID
          ,a.Accession  
from a2_Experiment e
join a2_Assay a on a.ExperimentID = e.ExperimentID
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTFACTORS
--------------------------------------------------------

CREATE OR REPLACE VIEW "VWEXPERIMENTFACTORS" ("EXPERIMENTID", "PROPERTYID", "PROPERTYVALUEID") AS
select distinct a. ExperimentID, pv.PropertyID, pv.PropertyValueID
from a2_assayPV apv
join a2_propertyvalue pv on apv.propertyvalueid = pv.propertyvalueid
join a2_assay a on a.assayid = apv.assayid
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTSAMPLE
--------------------------------------------------------

CREATE OR REPLACE VIEW "VWEXPERIMENTSAMPLE" ("EXPERIMENTID", "SAMPLEID", "ACCESSION") AS
select e.ExperimentID, s.SampleID, s.Accession
from a2_Experiment e
join a2_Assay a on a.ExperimentID = e.ExperimentID
join a2_AssaySample sa on sa.AssayID = a.AssayID
join a2_Sample s on s.SampleID = sa.SampleID
/
--------------------------------------------------------
--  DDL for View VWGENE
--------------------------------------------------------

CREATE OR REPLACE VIEW "VWGENE" ("GENEID", "ORGANISMID", "IDENTIFIER", "NAME") AS select "GENEID","ORGANISMID","IDENTIFIER","NAME" from a2_Gene
/
--------------------------------------------------------
--  DDL for View VWGENEPROPERTYVALUE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWGENEPROPERTYVALUE" ("GENEPROPERTYVALUEID", "GENEID", "GENEPROPERTYID", "PROPERTYNAME", "VALUE") AS select pv.GenePropertyValueID
,gpv.GeneID
,pv.GenePropertyID
,p.Name PropertyName
,pv.Value Value
from a2_GenePropertyValue pv
join a2_GeneProperty p on p.GenePropertyID = pv.GenePropertyID
join a2_GeneGPV gpv on gpv.genepropertyvalueid = pv.genepropertyvalueid
/
--------------------------------------------------------
--  DDL for View VWPROPERTYVALUE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWPROPERTYVALUE" ("PROPERTYVALUEID", "PROPERTYID", "PROPERTYNAME", "VALUE") AS select pv.PropertyValueID
,p.PropertyID
,p.Name PropertyName
,pv.Name Value
from a2_PropertyValue pv
join a2_Property p on p.PropertyID = pv.PropertyID
/
--------------------------------------------------------
--  DDL for View VWSAMPLE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWSAMPLE" ("SAMPLEID", "ACCESSION", "SPECIES", "CHANNEL") AS select s.SAMPLEID,s.ACCESSION,o.NAME,s.CHANNEL from a2_sample s, a2_organism o where s.organismid=o.organismid;
/
--------------------------------------------------------
--  DDL for View VWSAMPLEASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWSAMPLEASSAY" ("ASSAYSAMPLEID", "ASSAYID", "SAMPLEID", "ASSAYACCESSION") AS select asa.ASSAYSAMPLEID, asa.ASSAYID, asa.SAMPLEID, a.Accession AssayAccession 
from a2_assaysample asa
join a2_assay a on a.AssayID = asa.AssayID
/
--------------------------------------------------------
--  DDL for View VWSAMPLEPROPERTY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "VWSAMPLEPROPERTY" ("SAMPLEID", "PROPERTY", "PROPERTYVALUE") AS select a.SampleID
    ,p.Name Property
    ,pv.Name PropertyValue
from a2_Sample a
join a2_SamplePV ap on ap.SampleID = a.SampleID
join a2_PropertyValue pv on pv.PropertyValueID = ap.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID
/

CREATE OR REPLACE VIEW vwExperimentSample as
  Select distinct s.SampleID, a.ExperimentID 
  from a2_Sample s
  join a2_AssaySample ass on ass.SampleID = s.SampleID
  join a2_Assay a on a.AssayID = ass.AssayID;

/  

/**OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE 
CREATE OR REPLACE VIEW vwGeneIDProperty as 
  Select Name, IdentifierPriority as Priority 
  from a2_GeneProperty
  where IdentifierPriority is not null; 

*/

/**OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE 
CREATE OR REPLACE VIEW vwGeneIDs as
 Select g.GeneID
       ,p.Name
       ,pv.Value
 from a2_Gene g
 join a2_GeneGPV gpv on gpv.GeneID = g.GeneID
 join a2_GenePropertyValue pv on pv.genepropertyvalueid = gpv.genepropertyvalueid
 join a2_geneproperty p on p.genepropertyid = pv.genepropertyid
 where p.name in (select Name from vwGeneIDProperty);

*/

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
    frombe.organismid       AS organismid,
  FROM a2_designelement de
  join a2_designeltbioentity debe on debe.designelementid = de.designelementid
  join a2_bioentity frombe on frombe.bioentityid = debe.bioentityid
  join a2_bioentitytype betype on betype.bioentitytypeid = frombe.bioentitytypeid
  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid
  where debe.softwareid = ad.mappingswid
  and betype.ID_FOR_INDEX = 1
 /

CREATE OR REPLACE VIEW VWGENEPROPERTIES
AS
 Select
   g.GeneID
  ,g.Identifier
  ,p.name
  ,pv.value
  from a2_Gene g
  left outer join a2_GeneGPV ggpv on ggpv.GeneID = g.GeneID
  join a2_GenePropertyValue pv on pv.GenePropertyValueID = ggpv.GenePropertyValueID
  join a2_GeneProperty p on p.GenePropertyID = pv.GenePropertyID;
/
exit;
/

