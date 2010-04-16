/*******************************************************************************
reassigning all property association to new property, and delete old one
*******************************************************************************/
create or replace PROCEDURE CUR_MergePropertyValue(
   mPropertyValueID_old int
  ,mPropertyValueID_new int)
AS
BEGIN
  if(mPropertyValueID_old = mPropertyValueID_new) then
    return;
  end if;  

  update a2_samplepv pv
          set pv.PropertyValueID = mPropertyValueID_new
          where pv.PropertyValueID = mPropertyValueID_old
          and not exists(select 1 from a2_samplepv pv1 
                       where pv1.sampleid = pv.sampleid
                       and pv1.propertyvalueid = mPropertyValueID_new);
       
          update a2_assaypv pv
          set pv.PropertyValueID = mPropertyValueID_new
          where pv.PropertyValueID = mPropertyValueID_old
          and not exists(select 1 from a2_assaypv pv1 
                       where pv1.assayid = pv.assayid
                       and pv1.propertyvalueid = mPropertyValueID_new);
        
          delete from a2_propertyvalue 
          where propertyvalueid = mPropertyValueID_old;
END;