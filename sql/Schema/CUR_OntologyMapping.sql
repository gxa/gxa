/*******************************************************************************

select * from CUR_OntologyMapping 

*******************************************************************************/
create or replace view CUR_OntologyMapping as 
SELECT distinct Experiment, Property, Value, OntologyTerm from 
( SELECT distinct e.accession Experiment 
         ,p.name AS Property 
         ,pv.name AS Value 
         ,ot.accession AS OntologyTerm 
         --,spv.SampleID AS SampleID
         --,NULL AS AssayID
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
SELECT distinct e.accession Experiment 
         ,p.name AS Property 
         ,pv.name AS Value 
         ,ot.accession AS OntologyTerm
         --,NULL as SampleID
         --,apv.AssayID as AssayID
  FROM a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
  JOIN a2_assay ass ON ass.ExperimentID = e.ExperimentID 
  JOIN a2_assayPV apv ON apv.assayID = ass.AssayID
  JOIN a2_assayPVontology ao ON ao.AssayPVID = apv.assayPVID
  JOIN a2_propertyvalue pv ON pv.PropertyValueID = apv.PropertyValueID 
  JOIN a2_property p ON p.PropertyID = pv.PropertyID 
  JOIN a2_ontologyterm ot ON ot.OntologyTermID = ao.OntologyTermID 
  JOIN a2_ontology o ON o.OntologyID = ot.OntologyID );
/
exit;
/