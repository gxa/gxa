--------------------------------------------------------
--  DDL for Procedure A2_ARRAYDESIGNSET
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_ARRAYDESIGNSET" (
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,DesignElements DesignElementTable 
) 
as
  TheArrayDesignID int := 0;
  TheAccession varchar2(255) := Accession;
  TheType varchar2(255) := Type;
  TheProvider varchar2(255) := Provider;
  TheName varchar2(255) := Name;
begin
 
 Select ArrayDesignID into TheArrayDesignID
 from a2_ArrayDesign
 where Accession = TheAccession;

 Update a2_ArrayDesign 
 set Type = TheType
     ,Name = TheName
     ,Provider = TheProvider
 where ArrayDesignID = ArrayDesignID;   

 EXCEPTION
 WHEN NO_DATA_FOUND THEN
 begin
  Insert into a2_ArrayDesign(ArrayDesignID, Accession,Type,Name,Provider)
  select a2_ArrayDesign_Seq.nextval, TheAccession,TheType, TheName, TheProvider
  from dual;
 
  Select a2_ArrayDesign_Seq.CURRVAL into TheArrayDesignID from dual;
 end;
 
 Insert into a2_ArrayDesign (ArrayDesignID,Accession,Type,Name,Provider)
 select a2_ArrayDesign_Seq.nextval, TheAccession, TheType, TheName, TheProvider 
 from dual;
 
 Delete from a2_DesignElement where ArrayDesignID = TheArrayDesignID;
 
 Insert into a2_DesignElement (DesignElementID, ArrayDesignID, GeneID, Accession, Name, Type, IsControl)
 Select a2_DesignElement_seq.NEXTVAL, TheArrayDesignID, g.GeneID, de.Accession, de.Accession, 'unknown', 0
 from table(CAST(DesignElements as DesignElementTable)) de
 join a2_Gene g on g.Identifier = de.GeneAccession;
 
 commit;
end;
/
--------------------------------------------------------
--  DDL for Procedure A2_ASSAYSET
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_ASSAYSET" (
   TheAccession varchar2
  ,TheExperimentAccession  varchar2
  ,TheArrayDesignAccession varchar2
  ,TheProperties PropertyTable
  ,TheExpressionValues ExpressionValueTable
)
as
  TheExperimentID int := 0;
  TheArrayDesignID int := 0;
  TheAssayID int :=0;
  UnknownDesignElementAccession varchar2(255) := NULL;
begin

  begin
      Select e.ExperimentID into TheExperimentID 
      from a2_Experiment e
      where e.Accession = TheExperimentAccession;

      Select d.ArrayDesignID into TheArrayDesignID
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
    where not exists(select 1 from a2_designelement e where e.arraydesignid = Thearraydesignid and e.accession = t.DesignElementAccession)
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
      Select a.AssayID into TheAssayID
      from a2_Assay a
      where a.Accession = TheAccession;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Assay(Accession,ExperimentID,ArrayDesignID)
      values (TheAccession,TheExperimentID,TheArrayDesignID);
      
      Select a2_Assay_seq.currval into TheAssayID from dual;
     end;
    when others then 
      RAISE;    
  end;

  dbms_output.put_line('cleanup');
  delete a2_assaypropertyvalue pv where pv.assayid = TheAssayID;
  delete a2_expressionvalue ev where ev.assayid = TheAssayID;
  
  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionValue(DesignElementID,ExperimentID,AssayID,Value)
  select distinct d.DesignElementID, TheExperimentID, TheAssayID, t.Value
  from table(CAST(TheExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on d.Accession = t.DesignElementAccession;

  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(TheProperties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(TheProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_assaypropertyvalue(AssayID, PropertyValueID, IsFactorValue)
  select distinct TheAssayID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(TheProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where not exists(select 1 from a2_AssayPropertyValue pv1 where pv1.AssayID = Theassayid and pv1.PropertyValueID = pv.PropertyValueID);
 
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
  COMMIT WORK;

end;
/
--------------------------------------------------------
--  DDL for Procedure A2_EXPERIMENTSET
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_EXPERIMENTSET" (
  TheAccession varchar2
 ,TheDescription varchar2 
 ,ThePerformer varchar2 
 ,TheLab varchar2
)
AS
begin
  update a2_Experiment e
  set e.Description = Thedescription
  ,e.Performer = Theperformer
  ,e.Lab = Thelab
  where e.accession = TheAccession;
  
  dbms_output.put_line('updated ' || sql%rowcount || ' rows' );
  
  if ( sql%rowcount = 0 )
  then
     insert into a2_Experiment(Accession,Description,Performer,Lab)
     values (TheAccession,TheDescription,ThePerformer,TheLab);
     
     dbms_output.put_line('inserted');
  else
     dbms_output.put_line('update rowcount <> 0');
  end if;
  
  commit;
  
end;
/
--------------------------------------------------------
--  DDL for Procedure A2_EXPRESSIONGET
--------------------------------------------------------
/*
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_EXPRESSIONGET" (GeneIds varchar2,EfvIds tblVarchar, io IN OUT sys_refcursor) 
--return sys_refcursor
as
a1 sys_refcursor;
--efv table( EvfID integer); --tblInteger;
begin
  
      
  open a1 for 
  select g.GeneID GeneID
       , g.Identifier GeneIdentifier
       , g.Name GeneName
       , f.name ExperimentalFactor
       , fv.name ExperimentalFactorValue
       , fv.ExperimentFactorValueID ExperimentFactorValueID
       , SUM(CASE WHEN TSTAT > 0 THEN 1 ELSE 0 END)  Up
       , SUM(CASE WHEN TSTAT < 0 THEN 1 ELSE 0 END)  Dn
       , f.name || '|' || fv.name EfvStringID
  from a2_expressionanalytics e
  join a2_gene g on g.GeneID = e.GeneID
  join a2_experimentfactorvalue fv on fv.experimentfactorvalueid = e.experimentfactorvalueid
  join a2_experimentfactor f on f.experimentfactorid = fv.experimentfactorid
  join (
    select a.column_value val
    from the ( select cast( list_to_table(GeneIds) as tblInteger )
                             from dual ) a                     
                             where a.column_value is not null  
       ) g_i on g_i.val = e.geneid
  join (
   select e.ExperimentFactorValueID EfvID
    from a2_experimentfactorvalue e
    join a2_experimentfactor f on f.experimentfactorid = e.experimentfactorid
    join (select a.column_value val
        from the ( select EfvIds from dual ) a                     
                             where a.column_value is not null  ) a on a.val = f.Name || '|' || e.Name

  ) efv on efv.EfvID = e.experimentfactorvalueid
  group by g.GeneID
       , g.Identifier
       , g.Name 
       , f.name 
       , fv.name
       , fv.ExperimentFactorValueID
  order by GeneName, fv.ExperimentFactorValueID;
       
   io:=a1;    
   
   -- return io;
end;
*/
--------------------------------------------------------
--  DDL for Procedure A2_ORGANISMPARTGET
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_ORGANISMPARTGET" (
  GeneID int  := 0, io IN OUT sys_refcursor) 
 as
  a1 sys_refcursor;
 
 begin
 
  open a1 for
  select PropertyValueID
         ,Name
         ,null "X"
         ,null "Y"
         ,0 "UP"
         ,0 "DN"
   from a2_propertyvalue 
   where propertyid = 28
   and rownum < 10;
   
   io := a1;
   
  -- return a1;
 end;
 /
--------------------------------------------------------
--  DDL for Procedure A2_SAMPLESET
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."A2_SAMPLESET" (
    p_Accession varchar2
  , p_Assays AccessionTable
  , p_Properties PropertyTable
  , p_Species varchar2
  , p_Channel varchar2
) 
as
  SampleID int :=0;
  
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  UnknownDesignElementAccession varchar2(255) := NULL;
begin

 dbms_output.put_line('checking sample accession'); 
 begin
      Select s.SampleID into SampleID
      from a2_Sample s
      where s.Accession = p_Accession;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Sample(Accession,Species,Channel)
      values (p_Accession,p_Species,p_Channel);
      
      Select a2_Sample_seq.currval into SampleID from dual;
     end;
    when others then 
      RAISE;    
  end;

  dbms_output.put_line('linking sample and assay'); 
  Insert into a2_AssaySample(AssayID, SampleID)
  Select AssayID, SampleID
  from a2_Assay a
  where a.Accession in (select * from table(CAST(p_Assays as AccessionTable)) t);
  
  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(p_Properties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(p_Properties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_samplepropertyvalue(SampleID, PropertyValueID, IsFactorValue)
  select distinct SampleID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(p_Properties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
  where not exists(select 1 from a2_SamplePropertyValue pv1 where pv1.SampleID = Sampleid and pv1.PropertyValueID = pv.PropertyValueID);
  
  COMMIT WORK;
end;
/
--------------------------------------------------------
--  DDL for Procedure LOAD_PROGRESS
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "ATLAS2"."LOAD_PROGRESS" (
 experiment_accession varchar
 ,stage varchar --load, netcdf, similarity, ranking, searchindex
 ,status varchar --done, pending 
 ,load_type VARCHAR2 := 'experiment'
)
as
 thestatus varchar(255) := status;
begin
  if stage not in ('load', 'netcdf', 'similarity', 'ranking', 'searchindex') then
    RAISE_APPLICATION_ERROR(-20001, 'unknownn stage. possible values:load,netcdf,similarity,ranking,searchindex');
  end if;  
  
  if status not in ('pending', 'done', 'working', 'failed') then
    RAISE_APPLICATION_ERROR(-20001, 'unknownn status. possible values:pending,done');
  end if;

  if(stage = 'load') then
    update load_monitor set status = thestatus where accession = experiment_accession;
  elsif(stage = 'netcdf') then 
   update load_monitor set netcdf = thestatus where accession = experiment_accession;   
  elsif(stage = 'similarity') then 
   update load_monitor set similarity = thestatus where accession = experiment_accession;   
  elsif(stage = 'ranking')  then
   update load_monitor set ranking = thestatus where accession = experiment_accession;   
  elsif(stage = 'searchindex') then 
   update load_monitor set searchindex = thestatus where accession = experiment_accession;
  end if; 
   
   if ( sql%rowcount = 0 ) then 
      Insert into load_monitor(
          ID
          , Accession
          , Status
          , Netcdf
          , Similarity
          , Ranking
          , SearchIndex
          , Load_Type)
      select 
          load_monitor_seq.nextval
          ,experiment_accession
          , CASE stage WHEN 'load' then thestatus else 'pending' end
          , CASE stage WHEN 'netcdf' then thestatus else 'pending' end
          , CASE stage WHEN 'similarity' then thestatus else 'pending' end
          , CASE stage WHEN 'ranking' then thestatus else 'pending' end
          , CASE stage WHEN 'searchindex' then thestatus else 'pending' end
          ,load_type
     from dual;    
    end if;
  
commit work;  
  
end;
/
