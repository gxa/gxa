/*******************************************************************************
insert/update trigger to assay properties
*******************************************************************************/
create or replace
trigger TRU_CUR_AssayProperty instead of UPDATE OR INSERT OR DELETE on CUR_AssayProperty
--referencing old as old1 new as new1
for each row 
declare 
  mAssayID_old int;
  mAssayID_new int;
  mPropertyValueID_old int;
  mPropertyID_old int;
  mPropertyValueID_new int;
  mPropertyID_new int;
begin
  if updating('assays') then
    raise_application_error(-20010,'can not update assays');
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
  
  Select MIN(AssayID) into mAssayID_new
  from a2_Assay 
  where Accession = :new.Accession; 
  
  Select MIN(AssayID) into mAssayID_old
  from a2_Assay 
  where Accession = :old.Accession;
  
  if ((mPropertyValueID_new is null) and (:new.value is not null)) then
    raise_application_error(-20010,'property value not found');
  end if;  
  
  if(mPropertyValueID_new is not null) then
    if(mPropertyValueID_old is not null) then
        dbms_output.put_line('update assaypv');
        Update a2_AssayPV 
        set AssayID = mAssayID_new
           ,PropertyValueID = mPropertyValueID_new
           ,IsFactorValue = :new.IsFactorValue
        where AssayID = mAssayID_old
        and PropertyValueID = mPropertyValueID_old;
    else
        dbms_output.put_line('insert assaypv');
        
        Insert into a2_AssayPV (AssayPVID,AssayID,PropertyValueID,IsFactorValue)
        values (a2_AssayPV_SEQ.nextval,mAssayID_new,mPropertyValueID_new,:new.IsFactorValue);
    end if; 
  else
    dbms_output.put_line('delete assaypv');
    
    delete from a2_AssayPV
    where AssayID = mAssayID_old
    and PropertyValueID = mPropertyValueID_old;
  end if;
END;
