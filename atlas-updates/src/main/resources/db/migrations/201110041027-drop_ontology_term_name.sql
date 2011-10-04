 CREATE OR REPLACE VIEW "A2_ONTOLOGYMAPPING"
 AS SELECT distinct  e.accession,
         p.name AS Property,
         pv.name AS PropertyValue,
         ot.accession AS OntologyTerm,
         ot.OntologyTermID AS OntologyTermID,
         o.Name OntologyName,
         1 IsSampleProperty,
         0 IsAssayProperty,
         pv.PropertyValueID,
         e.ExperimentID
 FROM a2_experiment e
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
         ot.OntologyTermID AS OntologyTermID,
         o.Name OntologyName,
         0 IsSampleProperty,
         1 IsAssayProperty,
         pv.PropertyValueID,
         e.ExperimentID
  FROM a2_experiment e
  JOIN a2_assay ass ON ass.ExperimentID = e.ExperimentID
  JOIN a2_assayPV apv ON apv.assayID = ass.AssayID
  JOIN a2_assayPVontology ao ON ao.AssayPVID = apv.assayPVID
  JOIN a2_propertyvalue pv ON pv.PropertyValueID = apv.PropertyValueID
  JOIN a2_property p ON p.PropertyID = pv.PropertyID
  JOIN a2_ontologyterm ot ON ot.OntologyTermID = ao.OntologyTermID
  JOIN a2_ontology o ON o.OntologyID = ot.OntologyID;
