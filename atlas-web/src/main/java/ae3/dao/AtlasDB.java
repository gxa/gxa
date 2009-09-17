package ae3.dao;

import ae3.model.AtlasGene;
import ae3.service.structuredquery.*;
import ae3.service.ArrayExpressSearchService;

import java.util.*;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Connection;

import ds.utils.DS_DBconnection;
import uk.ac.manchester.cs.bhig.util.Tree;
import org.apache.lucene.index.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
//import org.apache.commons.dbcp.;


/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 3, 2009
 * Time: 4:55:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDB {

     public static class Gene{
         public String Id;
         public String Name;

         public String getId(){
             return Id;
         }

         public String getName(){
             return Name;
         }
     }

     final private Logger log = LoggerFactory.getLogger(getClass());

     public static List<Gene> getGenes(String StartLetter, int FirstRow, int PageSize) throws Exception {

         List<Gene> result = new ArrayList<Gene>();

         Connection conn = DS_DBconnection.instance().getConnection();

         CallableStatement sql = conn.prepareCall("{call ATLAS_PKG.Gene_GetList(?,?,?,?,?,?)}");

         sql.setString(1, StartLetter);
         sql.setInt(2, FirstRow);
         sql.setInt(3, PageSize);
         sql.setString(4, "GeneID");
         sql.setInt(5, 10);
                 //
         sql.registerOutParameter(6, oracle.jdbc.OracleTypes.CURSOR);

         sql.execute();

         ResultSet expRS = (ResultSet) sql.getObject(6);

        while(expRS.next()){
            Gene g = new Gene();

            g.Id    = expRS.getString("GeneID");
            g.Name  = expRS.getString("GeneName");

            result.add(g);
       }

       expRS.close();
       sql.close();
       conn.close();

       return result;                                                                                
     }

     public AtlasStructuredQueryResult doStructuredAtlasQuery(final AtlasStructuredQuery query, AtlasStructuredQueryResult result)
     {
         try{

         //AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(query.getStart(), query.getRowsPerPage(), query.getExpsPerGene());
        // AtlasGene g =  List<AtlasGene> getGenes()

         HashMap<String,Integer> Payloads = new HashMap<String,Integer>();

         //String EfvIDs = "";
         ArrayList<String> EfvIDs = new ArrayList<String>();

         for( EfvTree.EfEfv<Integer> ie : result.getResultEfvs().getNameSortedList())
         {
             int Payload = ie.getPayload();
             String EfEfvId = ie.getEf()+'|'+ie.getEfv();

             Payloads.put(EfEfvId,Payload);
            //int ic= 
            //EfvIDs += ( ie.getEfEfvId() + ",");
             EfvIDs.add( ie.getEfEfvId() );
         }

         String GeneIDs = "";

         for(StructuredResultRow r : result.getResults())
         {
            GeneIDs += (r.getGene().getGeneId() + ",");
         }

         Connection conn = DS_DBconnection.instance().getConnection();

         CallableStatement sql = conn.prepareCall("{call a2_ExpressionGet(?,?,?)}");


         /*This is where we map the Java Object Array to a PL/SQL Array */

        Connection actualCon = ((org.apache.commons.dbcp.DelegatingConnection) conn).getInnermostDelegate();


         ArrayDescriptor desc = ArrayDescriptor.createDescriptor("TBLVARCHAR",sql.getConnection());

         ARRAY jdbc_arry = new ARRAY (desc, sql.getConnection(), EfvIDs);

         sql.setString(1, GeneIDs);
         sql.setArray(2, jdbc_arry);

         sql.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR);

         sql.execute();

         ResultSet expRS = (ResultSet) sql.getObject(3);

         AtlasGene CurrentGene = null;
         List<UpdownCounter> CurrentUpDown = null;

        while(expRS.next()){
            String GeneID = expRS.getString("GeneID");

            if((CurrentGene==null)||(!GeneID.equals(CurrentGene.getGeneId())))
            {
                if(CurrentGene!=null)
                {
                    StructuredResultRow r = new  StructuredResultRow(CurrentGene , CurrentUpDown );
                    result.addResult(r);
                }

                String geneId = expRS.getString("GeneID");

                AtlasDao.AtlasGeneResult r1 = ArrayExpressSearchService.instance().getAtlasDao().getGeneByIdentifier(geneId);

                CurrentGene = r1.getGene();

                CurrentUpDown = new ArrayList<UpdownCounter>();

                for(int j=0; j!=2000; j++){ //set_size(1000);
                    CurrentUpDown.add(null);
                }

                //CurrentGene.setGeneId( );
                //CurrentGene.setGeneName( expRS.getString("GeneName") );
                //CurrentGene.setGeneIdentifier( expRS.getString("GeneIdentifier") );

                CurrentGene.setGeneHighlights(new HashMap<String, List<String>>());
            }

            String EvfID = expRS.getString("EfvStringID");

            if(Payloads.containsKey(EvfID))
            {
                int Payload = Payloads.get(EvfID);

                int Up = expRS.getInt("Up");
                int Dn = expRS.getInt("Dn");

                UpdownCounter up1 = new UpdownCounter(Up, Dn, 0.01, 0.02);
                CurrentUpDown.set(Payload, up1);
            }
       }

         if(CurrentGene!=null)
         {
             StructuredResultRow r = new  StructuredResultRow(CurrentGene , CurrentUpDown );
             result.addResult(r);
         }

         return result;
         }
         catch(Exception ex)
         {

             log.error("Error in structured query plus!", ex);
             return null;
         }
     }
}
