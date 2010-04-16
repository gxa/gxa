/*******************************************************************************
insert/update trigger to manage property values
*******************************************************************************/
create or replace
trigger TRU_CUR_PropertyValue instead of UPDATE OR INSERT OR DELETE on CUR_PropertyValue
--referencing old as old1 new as new1
for each row 
declare 
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
  
  if(mPropertyValueID_new is not null) then
    if(mPropertyValueID_old is not null) then
        dbms_output.put_line('merge property value with existing property value');
        CUR_MergePropertyValue(mPropertyValueID_old,mPropertyValueID_new);
    else
        dbms_output.put_line('inserted value exists, do nothing');
    end if; 
  else --PV_new is null
    if(mPropertyID_new is not null) then
      if(mPropertyValueID_old is not null) then
        dbms_output.put_line('rename value');
      
       Update a2_PropertyValue 
       set name = :new.Value
       where propertyvalueid = mPropertyValueID_old;
      else
        dbms_output.put_line('insert property value');
  
        Insert into a2_PropertyValue (PropertyValueID,PropertyID,Name) 
        select a2_PropertyValue_SEQ.nextval,mPropertyID_new,:new.Value from dual;
      end if;
    else -- mPropertyID_new is null
      if((:new.Property is null)or(:new.Value = '')) then
        if(mPropertyValueID_old is not null) then
            dbms_output.put_line('delete property value');
            
            delete from a2_propertyvalue where propertyvalueid = mPropertyValueID_old;
        else
          dbms_output.put_line('empty value inserted, do nothing');
        end if;
      else --not empty
        dbms_output.put_line('create new property and value, and migrate old if old exists');
      
        select a2_Property_Seq.nextval into mPropertyID_new from dual;
        Insert into a2_Property (PropertyID, name) 
                       values (mPropertyID_new, :new.Property);    
      
        Select a2_PropertyValue_seq.nextval into mPropertyValueID_new from dual;   
        
        Insert into a2_PropertyValue (PropertyValueID,PropertyID,Name) 
                            values (mPropertyValueID_new,mPropertyID_new,:new.Value);
        
        if(mPropertyValueID_old is not null) then
          CUR_MergePropertyValue(mPropertyValueID_old,mPropertyValueID_new);
        end if;
      end if;
    end if;
  end if;

END;