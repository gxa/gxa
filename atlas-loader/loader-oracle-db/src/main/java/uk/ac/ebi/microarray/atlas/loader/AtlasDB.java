package uk.ac.ebi.microarray.atlas.loader;

import oracle.sql.ArrayDescriptor;
import oracle.sql.StructDescriptor;
import oracle.sql.STRUCT;

import java.sql.Connection;
import java.sql.CallableStatement;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 26, 2009
 * Time: 5:14:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDB {

    public static Connection Connection;

    public static java.sql.Array ToSqlArray(String TypeName , Object[] Value) throws Exception{
        ArrayDescriptor  adExpressionValueTable  = ArrayDescriptor.createDescriptor(TypeName, Connection);

        return new oracle.sql.ARRAY(adExpressionValueTable, Connection, Value);
    }

    public static oracle.sql.STRUCT ToSqlStruct(String TypeName , Object[] Value) throws Exception{
        StructDescriptor sdExpressionValue       = StructDescriptor.createDescriptor(TypeName,Connection);

        return new STRUCT(sdExpressionValue,Connection,Value);
    }


    public static void ExperimentSet(Experiment Value) throws Exception{
            //TheAccession varchar2
            //,TheDescription varchar2
            //,ThePerformer varchar2
            //,TheLab varchar2
        CallableStatement sql = Connection.prepareCall("{call a2_ExperimentSet(?,?,?,?)}");

        sql.setString(1, Value.Accession);
        sql.setString(2, Value.Description);
        sql.setString(3, Value.Performer);
        sql.setString(4, Value.Lab);  //properties

        sql.execute();

    }

    public static void AssaySet(Assay Value) throws Exception{
        //1  Accession varchar2
        //2 ,ExperimentAccession  varchar2
        //3 ,ArrayDesignAccession varchar2
        //4 ,Properties PropertyTable
        //5 ,ExpressionValues ExpressionValueTable
       CallableStatement sql = Connection.prepareCall("{call a2_AssaySet(?,?,?,?,?)}");

       Object[] expressionValues = new Object[null==Value.ExpressionValues? 0 : Value.ExpressionValues.size()];
       Object[] members = new Object[2]; //placeholders for all properties of ExpressionValue structure

       int i=0;
       for(ExpressionValue v : Value.ExpressionValues)
       {
           members[0] = v.DesignElementAccession;
           members[1] = v.Value;

           expressionValues[i++] = ToSqlStruct("EXPRESSIONVALUE",members);
       }

        Object[] Properties = new Object[null==Value.Properties? 0 : Value.Properties.size()];
        Object[] members1 = new Object[4]; //placeholders for all properties of ExpressionValue structure

        int i1=0;
        for(Property v : Value.Properties)
        {
            members1[0] = v.Accession; //accession
            members1[1] = v.Name;
            members1[2] = v.Value;
            members1[3] = v.IsFactorValue;

            Properties[i1++] = ToSqlStruct("PROPERTY",members1);
        }

       sql.setString(1,Value.Accession);
       sql.setString(2,Value.ExperimentAccession);
       sql.setString(3,Value.ArrayDesignAcession);
       sql.setArray(4, ToSqlArray("PROPERTYTABLE", Properties));  //properties
       sql.setArray(5, ToSqlArray("EXPRESSIONVALUETABLE",expressionValues));

       sql.execute();
    }

    public static void SampleSet(Sample Value) throws Exception{
       //1 Accession varchar2
       //2 Assays AccessionTable
       //3 Properties PropertyTable
       //4 Species varchar2
       //5 Channel varchar2
       CallableStatement sql = Connection.prepareCall("{call a2_SampleSet(?,?,?,?,?)}");


       Object[] Properties = new Object[null==Value.Properties? 0 : Value.Properties.size()];
       Object[] members = new Object[4]; //placeholders for all properties of ExpressionValue structure

       int i=0;
       for(Property v : Value.Properties)
       {
           members[0] = v.Accession; //accession
           members[1] = v.Name;
           members[2] = v.Value;
           members[3] = v.IsFactorValue;

           Properties[i++] = ToSqlStruct("PROPERTY",members);
       }

        Object[] AssayAccessions = new Object[null==Value.AssayAccessions ? 0 : Value.AssayAccessions.size()];
        int i1=0;
        for(String v : Value.AssayAccessions)
        {
            AssayAccessions[i1++] = v;
        }

       sql.setString(1,Value.Accession);
       sql.setArray(2, ToSqlArray("ACCESSIONTABLE",AssayAccessions));
       sql.setArray(3, ToSqlArray("PROPERTYTABLE",Properties));
       sql.setString(4, Value.Species);  //properties
       sql.setString(5, Value.Channel);

       sql.execute();
    }

    public static void ExperimentDel(String Accession){

    }
}
