/*******************************************************************************
print experiment
*******************************************************************************/
CREATE OR REPLACE PROCEDURE Util_PrintExperiment(Accession in varchar2)
as
BEGIN

print_table('select * from a2_Experiment where Accession = ''' || Util_PrintExperiment.Accession || '''');

FOR r IN (Select a.AssayID 
          from a2_Assay a
          join a2_Experiment e on e.experimentid = a.experimentid
          where e.Accession = Util_PrintExperiment.Accession
) LOOP
BEGIN
  dbms_output.put_line('>>>>>>>>>>>>A>S>S>A>Y>>>>>>>>>>>>>');

  print_table('select * from a2_Assay where AssayID = ' || r.AssayID);
  
  print_table('select p.name property, pv.name value 
  from a2_assaypv apv
  join a2_assay a on a.assayid = apv.assayid
  join a2_propertyvalue pv on pv.propertyvalueid = apv.propertyvalueid
  join a2_property p on p.propertyid = pv.propertyid
  where apv.AssayID = ' || r.AssayID);

  print_table('Select s.Accession sample
  ,p.Name property 
  ,pv.Name value
  from a2_Sample s
  join a2_AssaySample sa on sa.SampleID = s.SampleID
  left outer join a2_SamplePV spv on spv.SampleID = s.SampleID
  left outer join a2_PropertyValue pv on pv.PropertyValueID = spv.PropertyValueID
  left outer join a2_Property p on p.PropertyID = pv.PropertyID
  where sa.AssayID = ' || r.AssayID);
  
END;
END LOOP;

END;
/
exit;
/