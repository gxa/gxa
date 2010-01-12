/*******************************************************************************/
/*  Views in Atlas2 schema     
/*
/*                                              
/*******************************************************************************/

 CREATE OR REPLACE VIEW "ATLAS2"."A2_ONTOLOGYMAPPING" ("ACCESSION", "PROPERTY", "PROPERTYVALUE", "ONTOLOGYTERM", "ONTOLOGYTERMNAME", "ONTOLOGYTERMID", "ONTOLOGYNAME", "ISSAMPLEPROPERTY", "ISASSAYPROPERTY", "ISFACTORVALUE") AS SELECT   e.accession, 
         p.name AS Property, 
         pv.name AS PropertyValue, 
         ot.accession AS OntologyTerm, 
         ot.term AS OntologyTermName, 
         ot.OntologyTermID AS OntologyTermID, 
         o.Name OntologyName, 
         1 IsSampleProperty, 
         0 IsAssayProperty, 
         0 IsFactorValue 
 FROM                          a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
                                 JOIN 
                                    a2_assay ass 
                                 ON ass.ExperimentID = e.ExperimentID 
                              JOIN 
                                 a2_assaysample asss 
                              ON asss.AssayID = ass.AssayID 
                           JOIN 
                              a2_sample s 
                           ON s.SampleID = asss.SampleID 
                        JOIN 
                           a2_sampleontology so 
                        ON so.SampleID = s.SampleID 
                     JOIN 
                        a2_samplepropertyvalue spv 
                     ON spv.SamplePropertyValueID = so.Sourcemapping 
                  JOIN 
                     a2_propertyvalue pv 
                  ON pv.PropertyValueID = spv.PropertyValueID 
               JOIN 
                  a2_property p 
               ON p.PropertyID = pv.PropertyID 
            JOIN 
               a2_ontologyterm ot 
            ON ot.OntologyTermID = so.OntologyTermID 
         JOIN 
            a2_ontology o 
         ON o.OntologyID = ot.OntologyID 
UNION ALL 
SELECT   e.accession, 
         p.name AS Property, 
         pv.name AS PropertyValue, 
         ot.accession AS OntologyTerm, 
         ot.term AS OntologyTermName, 
         ot.OntologyTermID AS OntologyTermID, 
         o.Name OntologyName, 
         0 IsSampleProperty, 
         1 IsAssayProperty, 
         1 IsFactorValue 
  FROM                        a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
                           JOIN 
                              a2_assay ass 
                           ON ass.ExperimentID = e.ExperimentID 
                        JOIN 
                           a2_assayontology so 
                        ON so.assayID = ass.assayID 
                     JOIN 
                        a2_assaypropertyvalue spv 
                     ON spv.assayPropertyValueID = so.Sourcemapping 
                  JOIN 
                     a2_propertyvalue pv 
                  ON pv.PropertyValueID = spv.PropertyValueID 
               JOIN 
                  a2_property p 
               ON p.PropertyID = pv.PropertyID 
            JOIN 
               a2_ontologyterm ot 
            ON ot.OntologyTermID = so.OntologyTermID 
         JOIN 
            a2_ontology o 
         ON o.OntologyID = ot.OntologyID
/
--------------------------------------------------------
--  DDL for View VWARRAYDESIGN
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWARRAYDESIGN" ("ARRAYDESIGNID", "NAME", "TYPE", "PROVIDER") AS SELECT ArrayDesignID
          , Name
          , Type
          , Provider
FROM a2_ArrayDesign
/
--------------------------------------------------------
--  DDL for View VWARRAYDESIGNELEMENT
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWARRAYDESIGNELEMENT" ("ARRAYDESIGNID", "DESIGNELEMENTID", "GENEIDENTIFIER") AS select de.ArrayDesignID
     , de.DesignElementID
     , g.Identifier GeneIdentifier
from a2_DesignElement de
join a2_Gene g on g.GeneID = de.GeneID
/
--------------------------------------------------------
--  DDL for View VWASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWASSAY" ("ASSAYID", "ACCESSION", "EXPERIMENTACCESSION") AS select a.AssayID
       ,a.Accession
       ,e.Accession ExperimentAccession
from a2_Assay a
join a2_Experiment e on a.ExperimentID = e.ExperimentID
/
--------------------------------------------------------
--  DDL for View VWASSAYPROPERTY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWASSAYPROPERTY" ("ASSAYID", "PROPERTY", "PROPERTYVALUE") AS select a.AssayID
    ,p.Name Property
    ,pv.Name PropertyValue
from a2_Assay a
join a2_AssayPropertyValue ap on ap.AssayID = a.AssayID
join a2_PropertyValue pv on pv.PropertyValueID = ap.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID
/
--------------------------------------------------------
--  DDL for View VWASSAYSAMPLE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWASSAYSAMPLE" ("ASSAYID", "SAMPLEACCESSION") AS select a.AssayID
      ,s.Accession SampleAccession
from a2_Assay a
join a2_AssaySample as1 on as1.AssayID = a.AssayID
join a2_Sample s on s.SampleID = as1.SampleID
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENT
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWEXPERIMENT" ("EXPERIMENTID", "ACCESSION", "DESCRIPTION", "PERFORMER", "LAB", "LOADDATE") AS select "EXPERIMENTID","ACCESSION","DESCRIPTION","PERFORMER","LAB","LOADDATE" from a2_Experiment
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWEXPERIMENTASSAY" ("EXPERIMENTID", "ASSAYID", "ACCESSION") AS select e.ExperimentID
          ,a.AssayID
          ,a.Accession  
from a2_Experiment e
join a2_Assay a on a.ExperimentID = e.ExperimentID
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTFACTORS
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWEXPERIMENTFACTORS" ("EXPERIMENTID", "PROPERTYID", "PROPERTYVALUEID") AS select distinct ExperimentID, PropertyID, PropertyValueID from (
select a. ExperimentID
      , pv.PropertyID
      , pv.PropertyValueID
from a2_assaypropertyvalue apv
join a2_propertyvalue pv on apv.propertyvalueid = pv.propertyvalueid
join a2_assay a on a.assayid = apv.assayid
where apv.isfactorvalue = 1
UNION ALL
select a. ExperimentID
      , pv.PropertyID
      , pv.PropertyValueID
from a2_samplepropertyvalue spv
join a2_propertyvalue pv on spv.propertyvalueid = pv.propertyvalueid
join a2_assaysample assa on assa.sampleid = spv.sampleid
join a2_assay a on a.assayid = assa.assayid
where spv.isfactorvalue = 1 ) t
/
--------------------------------------------------------
--  DDL for View VWEXPERIMENTSAMPLE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWEXPERIMENTSAMPLE" ("EXPERIMENTID", "SAMPLEID", "ACCESSION") AS select e.ExperimentID
          ,s.SampleID
          ,s.Accession  
from a2_Experiment e
join a2_Assay a on a.ExperimentID = e.ExperimentID
join a2_AssaySample sa on sa.AssayID = a.AssayID
join a2_Sample s on s.SampleID = sa.SampleID
/
--------------------------------------------------------
--  DDL for View VWGENE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWGENE" ("GENEID", "SPECID", "IDENTIFIER", "NAME") AS select "GENEID","SPECID","IDENTIFIER","NAME" from a2_Gene
/
--------------------------------------------------------
--  DDL for View VWGENEPROPERTYVALUE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWGENEPROPERTYVALUE" ("GENEPROPERTYVALUEID", "GENEID", "GENEPROPERTYID", "PROPERTYNAME", "VALUE") AS select pv.GenePropertyValueID
,pv.GeneID
,pv.GenePropertyID
,p.Name PropertyName
,pv.Value Value
from a2_GenePropertyValue pv
join a2_GeneProperty p on p.GenePropertyID = pv.GenePropertyID
/
--------------------------------------------------------
--  DDL for View VWPROPERTYVALUE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWPROPERTYVALUE" ("PROPERTYVALUEID", "PROPERTYID", "PROPERTYNAME", "VALUE") AS select pv.PropertyValueID
,p.PropertyID
,p.Name PropertyName
,pv.Name Value
from a2_PropertyValue pv
join a2_Property p on p.PropertyID = pv.PropertyID
/
--------------------------------------------------------
--  DDL for View VWSAMPLE
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWSAMPLE" ("SAMPLEID", "ACCESSION", "SPECIES", "CHANNEL") AS select "SAMPLEID","ACCESSION","SPECIES","CHANNEL" from a2_sample
/
--------------------------------------------------------
--  DDL for View VWSAMPLEASSAY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWSAMPLEASSAY" ("ASSAYSAMPLEID", "ASSAYID", "SAMPLEID", "ASSAYACCESSION") AS select asa.ASSAYSAMPLEID, asa.ASSAYID, asa.SAMPLEID, a.Accession AssayAccession 
from a2_assaysample asa
join a2_assay a on a.AssayID = asa.AssayID
/
--------------------------------------------------------
--  DDL for View VWSAMPLEPROPERTY
--------------------------------------------------------

  CREATE OR REPLACE VIEW "ATLAS2"."VWSAMPLEPROPERTY" ("SAMPLEID", "PROPERTY", "PROPERTYVALUE") AS select a.SampleID
    ,p.Name Property
    ,pv.Name PropertyValue
from a2_Sample a
join a2_SamplePropertyValue ap on ap.SampleID = a.SampleID
join a2_PropertyValue pv on pv.PropertyValueID = ap.PropertyValueID
join a2_Property p on p.PropertyID = pv.PropertyID
/

