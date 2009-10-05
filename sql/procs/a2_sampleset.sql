
  CREATE OR REPLACE PROCEDURE "A2_SAMPLESET" (
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

  Insert into a2_AssaySample(AssayID, SampleID)
  Select AssayID, SampleID
  from a2_Assay a
  where a.Accession in (select * from table(CAST(p_Assays as AccessionTable)) t);
   
  
  dbms_output.put_line('not kidding');

end;

/
 