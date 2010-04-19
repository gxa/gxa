/*******************************************************************************
*******************************************************************************/
create or replace
trigger TRU_CUR_SampleProperty instead of UPDATE OR INSERT OR DELETE on CUR_SampleProperty
--referencing old as old1 new as new1
for each row 
declare 
  mSampleID_old int;
  mSampleID_new int;
  mPropertyValueID_old int;
  mPropertyID_old int;
  mPropertyValueID_new int;
  mPropertyID_new int;
begin
  if updating('Samples') then
    raise_application_error(-20010,'can not update Samples');
  end if;  

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

  if(mPropertyID_new is not null) then
    Select MIN(PropertyValueID) into mPropertyValueID_new
    from a2_PropertyValue 
    where name = :new.Value
    and PropertyID = mPropertyID_new;
  end if;
  
  Select MIN(SampleID) into mSampleID_new
  from a2_Sample 
  where Accession = :new.Accession; 
  
  Select MIN(SampleID) into mSampleID_old
  from a2_Sample 
  where Accession = :old.Accession;
  
  if ((mPropertyValueID_new is null) and (:new.value is not null)) then
    raise_application_error(-20010,'property value not found');
  end if;  
  
  if(mPropertyValueID_new is not null) then
    if(mPropertyValueID_old is not null) then
        dbms_output.put_line('update Samplepv');
        Update a2_SamplePV 
        set SampleID = mSampleID_new
           ,PropertyValueID = mPropertyValueID_new
           ,IsFactorValue = :new.IsFactorValue
        where SampleID = mSampleID_old
        and PropertyValueID = mPropertyValueID_old;
    else
        dbms_output.put_line('insert Samplepv');
        
        Insert into a2_SamplePV (SamplePVID,SampleID,PropertyValueID,IsFactorValue)
        values (a2_SamplePV_SEQ.nextval,mSampleID_new,mPropertyValueID_new,:new.IsFactorValue);
    end if; 
  else
    dbms_output.put_line('delete Samplepv');
    
    delete from a2_SamplePV
    where SampleID = mSampleID_old
    and PropertyValueID = mPropertyValueID_old;
  end if;
  
     MERGE INTO A2_TASKMAN_STATUS ts USING (select distinct (e.Accession) Accession from a2_Experiment e
                                        join a2_assay a on a.ExperimentID = e.ExperimentID
                                        join a2_assaysample assa on assa.AssayID = a.AssayID
                                        where assa.SampleID in (mSampleID_old,mSampleID_new)) t
   ON (ts.type = 'updateexperiment' and ts.accession = t.Accession)
   WHEN MATCHED THEN UPDATE SET status = 'INCOMPLETE' 
   WHEN NOT MATCHED THEN INSERT (type,accession,status) values ('updateexperiment', t.Accession, 'INCOMPLETE');
  
END;
