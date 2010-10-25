/*******************************************************************************
select * from CUR_OntologyMapping
*******************************************************************************/
create or replace
trigger TR_CUR_OntologyMapping instead of UPDATE OR INSERT OR DELETE on CUR_OntologyMapping
for each row 
declare 
 mPropertyID_old int; 
 mPropertyValueID_old int;
 mPropertyID_new int;
 mPropertyValueID_new int;
 mOntologyTermID_new int;
 mOntlogyTerm_new varchar2(255);
 mOntologyTermID_old int;
begin
 
  Select MIN(PropertyID) into mPropertyID_old
  from a2_Property 
  where name = :old.Property;

  Select MIN(PropertyValueID) into mPropertyValueID_old
  from a2_propertyvalue
  where PropertyID = mPropertyID_old
  and name = :old.value;

  Select MIN(PropertyID) into mPropertyID_new
  from a2_Property 
  where name = :new.Property;

  Select MIN(PropertyValueID) into mPropertyValueID_new
  from a2_PropertyValue 
  where name = :new.Value
  and PropertyID = mPropertyID_new;

  if(:new.OntologyTerm is null) then
   dbms_output.put_line('delete assay and sample mapping'); 
   
   delete a2_samplepvontology t
   where exists (select 1 
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
   where e.Accession = :old.Experiment
   and p.Name = :old.Property
   and pv.Name = :old.Value
   and t.SamplePVOntologyID = so.SamplePVOntologyID);
 
   delete a2_assaypvontology t
   where exists(select 1 
   FROM a2_experiment e -- on e.ExperimentID = ev.ExperimentID 
   JOIN a2_assay ass ON ass.ExperimentID = e.ExperimentID 
   JOIN a2_assayPV apv ON apv.assayID = ass.AssayID
   JOIN a2_assayPVontology ao ON ao.AssayPVID = apv.assayPVID
   JOIN a2_propertyvalue pv ON pv.PropertyValueID = apv.PropertyValueID 
   JOIN a2_property p ON p.PropertyID = pv.PropertyID 
   JOIN a2_ontologyterm ot ON ot.OntologyTermID = ao.OntologyTermID 
   JOIN a2_ontology o ON o.OntologyID = ot.OntologyID
   where e.Accession = :old.Experiment
   and p.Name = :old.Property
   and pv.Name = :old.Value
   and t.AssayPVONtologyID = ao.AssayPVOntologyID);
  
  else --new ontology term is not 
   dbms_output.put_line('update assay and sample mapping'); 
   
   --create new ontology terms automatically
   --Insert into CUR_Ontology (Accession) Select :new.OntologyTerm from dual; 
   
   BEGIN
    select OntologyTermID into mOntologyTermID_new 
    from a2_OntologyTerm
    where accession = :new.OntologyTerm;
   EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20010,'ontology term not found');
   END;

   if(not(:old.OntologyTerm is null)) then
   BEGIN
    select OntologyTermID into mOntologyTermID_old
    from a2_OntologyTerm
    where accession = :old.OntologyTerm;
   EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20010,'ontology term not found');
   END;
   end if;
   
   --if(not exists(property) raise error)
   --http://asktom.oracle.com/pls/asktom/f?p=100:11:0::::P11_QUESTION_ID:3069487275935
   for x in (select count(*) cnt 
             from dual 
             where exists (select 1 
                           from a2_PropertyValue pv 
                           join a2_Property p on p.PropertyID = pv.PropertyID 
                           where p.name = :new.Property 
                           and pv.name = :new.Value))  
   loop
    if( x.cnt = 1) then
      null;
    else
      RAISE_APPLICATION_ERROR(-20010,'property not exists');        
    end if;
   end loop; 
   
   --if(not exists(assay/sample_property) raise error)
   --now this is cyclo-programming !
   for x in (select count(*) cnt 
             from dual 
             where exists(select 1 
                    from a2_assay a
                    left outer join a2_assaypv apv on a.assayid = apv.assayid
                    left outer join a2_assaysample asp on asp.assayid = a.assayid
                    left outer join a2_sample s on s.SampleID = asp.SampleID
                    left outer join a2_samplepv spv on spv.SampleID = s.SampleID
                    join a2_experiment e on e.experimentid = a.experimentid
                    join a2_propertyvalue pv on pv.propertyvalueid IN (apv.propertyvalueid ,spv.propertyvalueid)
                    join a2_property p on p.propertyid = pv.propertyid 
                    where e.Accession = :new.Experiment
                    and p.name = :new.Property
                    and pv.name = :new.Value))
   loop                  
   if( x.cnt = 1) then
      null;
   else
      RAISE_APPLICATION_ERROR(-20010,'property is not associated with either assay of sample');
   end if;
   end loop;
    
   BEGIN
   Update a2_assaypvontology set OntologyTermID = mOntologyTermID_new
   where OntologyTermID = mOntologyTermID_old
   and AssayPVID in (select apv.AssayPVID 
                                             from a2_assaypv apv
                                             join a2_assay a on a.AssayID = apv.AssayID   
                                             join a2_experiment e on e.ExperimentID = a.ExperimentID
                                             where e.accession = :new.Experiment
                                             and apv.PropertyValueID = mPropertyValueID_new);
   EXCEPTION
   WHEN NO_DATA_FOUND THEN
    NULL;
   END;
   
   
   --dbms_output.put_line(:new.Experiment); 
   --dbms_output.put_line(mPropertyValueID_new); 

   BEGIN                                             
   MERGE INTO a2_assaypvontology apvo USING (select apv.AssayPVID 
                                             from a2_assaypv apv
                                             join a2_assay a on a.AssayID = apv.AssayID   
                                             join a2_experiment e on e.ExperimentID = a.ExperimentID
                                             where e.accession = :new.Experiment
                                             and apv.PropertyValueID = mPropertyValueID_new) t
   ON (apvo.AssayPVID = t.AssayPVID and apvo.OntologyTermID = mOntologyTermID_new)
   WHEN NOT MATCHED THEN INSERT (AssayPVOntologyID,OntologyTermID, AssayPVID) 
                         VALUES (A2_AssayPVOntology_Seq.nextval,mOntologyTermID_new, t.AssayPVID);
   EXCEPTION
   WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20010,'omg o!o!');
    NULL;
   END;

   BEGIN
   Update a2_samplepvontology set OntologyTermID = mOntologyTermID_new
   where OntologyTermID = mOntologyTermID_old
   and SamplePVID in (select apv.SamplePVID 
                                              from a2_samplepv apv
                                              join a2_sample s on s.SampleID = apv.SampleID 
                                              join a2_assaysample asa on asa.sampleid = s.sampleid
                                              join a2_assay a on a.assayid = asa.assayid 
                                              join a2_experiment e on e.ExperimentID = a.ExperimentID
                                              where e.accession = :new.Experiment
                                              and apv.PropertyValueID = mPropertyValueID_new);
   EXCEPTION
   WHEN NO_DATA_FOUND THEN
    NULL;
   END;


   BEGIN
   MERGE INTO a2_samplepvontology apvo USING (select apv.SamplePVID 
                                              from a2_samplepv apv
                                              join a2_sample s on s.SampleID = apv.SampleID 
                                              join a2_assaysample asa on asa.sampleid = s.sampleid
                                              join a2_assay a on a.assayid = asa.assayid 
                                              join a2_experiment e on e.ExperimentID = a.ExperimentID
                                              where e.accession = :new.Experiment
                                              and apv.PropertyValueID = mPropertyValueID_new) t
   ON (apvo.SamplePVID = t.SamplePVID and apvo.OntologyTermID = mOntologyTermID_new)
   WHEN NOT MATCHED THEN INSERT (SamplePVOntologyID,OntologyTermID, SamplePVID) 
                         VALUES (A2_SamplePVOntology_Seq.nextval,mOntologyTermID_new, t.SamplePVID);
   EXCEPTION
   WHEN NO_DATA_FOUND THEN
    NULL;
   END;

  end if;
  
  MERGE INTO A2_TASKMAN_STATUS ts USING (select distinct (e.Accession) Accession from a2_Experiment e
                                         where e.Accession in (:new.Experiment,:old.Experiment)) t
  ON (ts.type = 'updateexperiment' and ts.accession = t.Accession)
  WHEN MATCHED THEN UPDATE SET status = 'INCOMPLETE'
  WHEN NOT MATCHED THEN INSERT (type,accession,status) values ('updateexperiment', t.Accession, 'INCOMPLETE');
  
end;
/
exit;
/
