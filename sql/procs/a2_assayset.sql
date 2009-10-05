
  CREATE OR REPLACE PROCEDURE "A2_ASSAYSET" (
   TheAccession varchar2
  ,TheExperimentAccession  varchar2
  ,TheArrayDesignAccession varchar2
  ,TheProperties PropertyTable
  ,TheExpressionValues ExpressionValueTable
)
as
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  AssayID int :=0;
  UnknownDesignElementAccession varchar2(255) := NULL;
begin

  begin
      Select e.ExperimentID into ExperimentID 
      from a2_Experiment e
      where e.Accession = TheExperimentAccession;

      Select d.ArrayDesignID into ArrayDesignID
      from a2_ArrayDesign d
      where d.Accession = TheArrayDesignAccession;
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or array design not found');
    when others then 
      RAISE;
  end;

  dbms_output.put_line('check for invalid design elements');
  begin
    Select t.DesignElementAccession into UnknownDesignElementAccession
    from table(CAST(TheExpressionValues as ExpressionValueTable)) t
    where not exists(select 1 from a2_designelement e where e.arraydesignid = arraydesignid and e.accession = t.DesignElementAccession)
    and rownum < 2;
  exception
    WHEN NO_DATA_FOUND THEN
      null;
    when others then
      raise;
  end;

  if(not(UnknownDesignElementAccession is NULL)) then
    RAISE_APPLICATION_ERROR(-20001, 'design element accession not found:');
  end if;

  begin
      Select a.AssayID into AssayID
      from a2_Assay a
      where a.Accession = TheAccession;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Assay(Accession,ExperimentID,ArrayDesignID)
      values (TheAccession,ExperimentID,ArrayDesignID);
      
      Select a2_Assay_seq.currval into AssayID from dual;
     end;
    when others then 
      RAISE;    
  end;

  dbms_output.put_line('cleanup');
  delete a2_assaypropertyvalue pv where pv.assayid = assayid;
  delete a2_expressionvalue ev where ev.assayid = assayid;
  
  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionValue(DesignElementID,ExperimentID,AssayID,Value)
  select distinct d.DesignElementID, ExperimentID, AssayID, t.Value
  from table(CAST(TheExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on d.Accession = t.DesignElementAccession;

  dbms_output.put_line('insert property');
  Insert into a2_Property(Name, Accession)
  select distinct t.Accession, t.Name
  from table(CAST(TheProperties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Accession = t.Accession);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(TheProperties as PropertyTable)) t
  join a2_Property p on p.accession = t.Accession
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_assaypropertyvalue(AssayID, PropertyValueID, IsFactorValue)
  select distinct AssayID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(TheProperties as PropertyTable)) t
  join a2_Property p on p.accession = t.Accession
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where not exists(select 1 from a2_AssayPropertyValue pv1 where pv1.AssayID = assayid and pv1.PropertyValueID = pv.PropertyValueID);
 
  /*
  Insert into a2_AssayPropertyValue(DesignElementID,ExperimentID,AssayID,Value)
  select d.DesignElementID, ExperimentID, AssayID, t.Value
  from table(CAST(TheExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on d.Accession = t.DesignElementAccession;
  */

  /*
  if SQL%NOTFOUND THEN
    RAISE_APPLICATION_ERROR(-20001, 'no expression values inserted');
  end if; 

  Insert into a2_AssayPropertyValue(AssayID,PropertyValueID,IsFactorValue)
  select 
  from table (CAST(ThePrope) )
  */
 
  dbms_output.put_line('not kidding');

end;

/
 