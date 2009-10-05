
  CREATE OR REPLACE PROCEDURE "A2_EXPERIMENTSET" (
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
 