/*
drop procedure A2_ARRAYDESIGNSET;
drop procedure A2_EXPERIMENTSET;
drop procedure A2_ASSAYSET;
drop procedure A2_SAMPLESET;
drop procedure LOAD_PROGRESS;
*/


CREATE OR REPLACE PACKAGE ATLASLDR IS

PROCEDURE A2_ARRAYDESIGNSET(
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,DesignElements DesignElementTable 
); 

PROCEDURE A2_EXPERIMENTSET(
  TheAccession varchar2
 ,TheDescription varchar2 
 ,ThePerformer varchar2 
 ,TheLab varchar2
);

PROCEDURE A2_ASSAYSET(
   TheAccession varchar2
  ,TheExperimentAccession  varchar2
  ,TheArrayDesignAccession varchar2
  ,TheProperties PropertyTable
  ,TheExpressionValues ExpressionValueTable
  --,UnknownAccessionThreshold int default 75  
);

PROCEDURE A2_SAMPLESET(
    p_Accession varchar2
  , p_Assays AccessionTable
  , p_Properties PropertyTable
  , p_Species varchar2
  , p_Channel varchar2
); 

PROCEDURE A2_AnalyticsSet(
    ExperimentAccession      IN   varchar2  
   ,Property                 IN   varchar2
   ,PropertyValue            IN   varchar2
   ,ExpressionAnalytics ExpressionAnalyticsTable
);

PROCEDURE A2_AnalyticsDelete(
    ExperimentAccession      IN   varchar2  
);

PROCEDURE LOAD_PROGRESS(
 experiment_accession varchar
 ,stage varchar --load, netcdf, similarity, ranking, searchindex
 ,status varchar --done, pending 
 ,load_type VARCHAR2 := 'experiment'
);

PROCEDURE A2_EXPERIMENTDELETE(
  Accession varchar2
);

END;
/

/*******************************************************************************
BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY
*******************************************************************************/
CREATE OR REPLACE PACKAGE BODY ATLASLDR AS

--------------------------------------------------------
--  DDL for Procedure A2_ARRAYDESIGNSET
--------------------------------------------------------
PROCEDURE A2_ARRAYDESIGNSET (
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

--------------------------------------------------------
--  DDL for Procedure A2_ASSAYSET
--------------------------------------------------------
PROCEDURE A2_ASSAYSET (
   TheAccession varchar2
  ,TheExperimentAccession  varchar2
  ,TheArrayDesignAccession varchar2
  ,TheProperties PropertyTable
  ,TheExpressionValues ExpressionValueTable
  --,UnknownAccessionThreshold int default 75  
)
as
  TheExperimentID int := 0;
  TheArrayDesignID int := 0;
  TheAssayID int :=0;
  UnknownDesignElementAccession int :=0; --;varchar2(255) := NULL;
  LowerCaseProperties PropertyTable := TheProperties;
  MissedAccessionPercentage int := 0;
  UnknownAccessionThreshold int := 100;
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
    Select count(t.DesignElementAccession) into UnknownDesignElementAccession
    from table(CAST(TheExpressionValues as ExpressionValueTable)) t
    where not exists(select 1 from a2_designelement e 
                     where e.arraydesignid = Thearraydesignid 
                     and ((e.accession = t.DesignElementAccession) or (e.name = t.DesignElementAccession)));
    
    Select (100*UnknownDesignElementAccession)/count(t.DesignElementAccession) into MissedAccessionPercentage
    from table(CAST(TheExpressionValues as ExpressionValueTable)) t;
    
    if(MissedAccessionPercentage > UnknownDesignElementAccession) then
      RAISE_APPLICATION_ERROR(-20001, 'unknown accession threshold exceeded');
    end if;
  end;  

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
  
  --convert properties to lowercase 
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;

  dbms_output.put_line('cleanup');
  delete a2_assaypropertyvalue pv where pv.assayid = TheAssayID;
  delete a2_expressionvalue ev where ev.assayid = TheAssayID;
  
  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionValue(DesignElementID,ExperimentID,AssayID,Value)
  select distinct d.DesignElementID, TheExperimentID, TheAssayID, t.Value
  from table(CAST(TheExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on ((d.Accession = t.DesignElementAccession) or (d.name = t.DesignElementAccession));

  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_assaypropertyvalue(AssayID, PropertyValueID, IsFactorValue)
  select distinct TheAssayID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
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

--------------------------------------------------------
--  DDL for Procedure A2_EXPERIMENTSET
--------------------------------------------------------
PROCEDURE A2_EXPERIMENTSET (
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

--------------------------------------------------------
--  DDL for Procedure A2_SAMPLESET
--------------------------------------------------------
PROCEDURE A2_SAMPLESET (
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
  LowerCaseProperties PropertyTable := p_Properties;
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

 --convert properties to lowercase 
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;

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
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_samplepropertyvalue(SampleID, PropertyValueID, IsFactorValue)
  select distinct SampleID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
  where not exists(select 1 from a2_SamplePropertyValue pv1 where pv1.SampleID = A2_SAMPLESET.Sampleid and pv1.PropertyValueID = pv.PropertyValueID);
  
  COMMIT WORK;
end;


--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsSet
--------------------------------------------------------
PROCEDURE A2_AnalyticsSet(
    ExperimentAccession      IN   varchar2  
   ,Property                 IN   varchar2
   ,PropertyValue            IN   varchar2
   ,ExpressionAnalytics ExpressionAnalyticsTable
 )
 as
  ExperimentID int := 0;
  PropertyValueID int := 0;
  LowerCaseProperty varchar2(255) := LOWER(Property);
begin

  begin
      Select e.ExperimentID into ExperimentID 
      from a2_Experiment e
      where e.Accession = ExperimentAccession;
      
      Select pv.PropertyValueID into PropertyValueID
      from a2_Property p
      join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
      where p.Name = LowerCaseProperty
      and pv.Name = PropertyValue;
      
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or property not found');
    when others then 
      RAISE;
  end;

  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionAnalytics(DesignElementID,ExperimentID,PropertyValueID,TSTAT,PVALADJ,FPVAL,FPVALADJ)
  select t.DesignElementID, ExperimentID, PropertyValueID, t.Tstat, t.PVALADJ, null, null
  from table(CAST(ExpressionAnalytics as ExpressionAnalyticsTable)) t;

  COMMIT WORK;
END;

--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsDelete
--------------------------------------------------------
PROCEDURE A2_AnalyticsDelete(
    ExperimentAccession      IN   varchar2  
 )
 as
  ExperimentID int := 0;
begin

  begin
      Select e.ExperimentID into ExperimentID 
      from a2_Experiment e
      where e.Accession = ExperimentAccession;
      
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or property not found');
    when others then 
      RAISE;
  end;

  dbms_output.put_line('delete expression value');
  delete from a2_ExpressionAnalytics
  where ExperimentID = A2_AnalyticsDelete.ExperimentID;

  COMMIT WORK;
END;


--------------------------------------------------------
--  DDL for Procedure LOAD_PROGRESS
--------------------------------------------------------
PROCEDURE LOAD_PROGRESS (
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

PROCEDURE A2_EXPERIMENTDELETE(
  Accession varchar2
)
AS 
  ExperimentID integer := 0;
BEGIN
 
 begin
  Select ExperimentID into ExperimentID 
  from a2_Experiment where Accession = A2_EXPERIMENTDELETE.Accession;
 exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment not found');
    when others then 
      RAISE;
  end; 
  
  Delete from a2_ExpressionAnalytics where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
  Delete from a2_ExpressionValue where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);

  Delete from a2_SampleOntology where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_SamplePropertyValue where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_AssaySample where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_Sample where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);

  
  Delete from a2_AssayOntology where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_AssayPropertyValue where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
 
  Delete from a2_Experiment where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
  
  commit;
  
END;



END;
/
exit;
/
