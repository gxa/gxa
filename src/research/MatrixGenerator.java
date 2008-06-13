package research;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;

//import oracle.jdbc.OraclePreparedStatement;
//import oracle.jdbc.OracleResultSet;

public class MatrixGenerator {

	static String baseURLstr;
	static Connection connection;
//	static LinkedHashMap<String, Integer> heading = new LinkedHashMap<String, Integer>();
	static ArrayList<String[]> headings = new ArrayList<String[]>();
	static LinkedHashMap deMap = new LinkedHashMap<Integer, HashMap<String, String>>();
//	static BufferedWriter out;

	public static void init (String propPath) {
		try {

			Properties props = new Properties ();
			FileInputStream in = new FileInputStream (propPath);
			props.load (in);
			in.close ();

			String dbConnStr  = props.getProperty("jdbcString");
			String userName    = props.getProperty("userName");
			String password    = props.getProperty("password");


			DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver ());
			connection = DriverManager.getConnection (dbConnStr, userName, password);
			connection.setAutoCommit (false);

		} catch (Exception e) { e.printStackTrace (System.out);}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		init(args[0]);
		queryColHeaders();
		writeHeadings();
		queryDEs();
//		writeIt();
	}

	private static void queryDEs(){

		HashMap<String, String> data = new HashMap<String, String>();
		Integer deKey=0, oldkey=0;
		String colHeader;
		Integer updn;
		System.out.println("querying atlas data");
		try {
			Statement  stmt =  connection.createStatement();
			ResultSet rset = stmt.executeQuery("select designelement_id_key, experiment_id_key, EF, EFV, UPDN " +
					"from atlas_full " +
			"order by designelement_id_key");
//			OracleResultSet orset = (OracleResultSet) rset;
			System.out.println("Retrieved records, starting to put into de map");
			int c=0;int s=0;
			while(rset.next()){
				deKey = rset.getInt("designelement_id_key");
				if(c==0)
					oldkey = deKey;
				if(!deKey.equals(oldkey)){
					writeIt(data,deKey.toString());
					data.clear();
					oldkey=deKey;
					if(c%1000==0){
						System.out.println(c+" design elements writtent to file");
					}
				}

				c++;	
				colHeader = rset.getObject("experiment_id_key")+"_"+rset.getObject("EF")+"_"+rset.getObject("EFV");
				updn = rset.getInt("UPDN");
//				if(deMap.containsKey(deKey)){
//				data = (HashMap<String, String>)deMap.get(deKey);

//				}
//				else{
//				data = new HashMap<String, String>();
//				deMap.put(deKey, data);
//				s++;
//				}
				data.put(colHeader, updn.toString());
//				if(c%1000==0){
//				System.out.println(c+" records processed, size of map now is: "+deMap.size());
//				}

//				int index = heading.get(colHeader);
//				data[index] = updn.toString();

			}
			rset.close();
			if (stmt != null)
				stmt.close();
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void writeIt(HashMap<String, String> data, String deID){

		try {
			System.out.println("writing de to file");
			BufferedWriter out = new BufferedWriter(new FileWriter("atlas_matrix.dat",true));
//			for(int k=0; k<=2; k++){
//			Iterator iter = headings.iterator(); //.entrySet().iterator();
//			out.write("\t\t");
//			while(iter.hasNext()){
//			String[] heading = (String[])iter.next();
//			out.write(heading[k]+"\t");
//			}
//			out.newLine();
//			}

//			Iterator iter2 = deMap.entrySet().iterator();
//			int count=0;
//			while(iter2.hasNext()){
//			Entry<Integer, HashMap> entry = (Entry<Integer, HashMap>)iter2.next();
			System.out.println(deID);
			out.newLine();
			out.write(deID+"\t"); // de_id
//			HashMap data = entry.getValue();
			for(int i=0; i<headings.size(); i++){
				String[] heading = headings.get(i); //exp_id,EF,EFV triplet
				String key = heading[0]+"_"+heading[1]+"_"+heading[2];
				if(data.containsKey(key)){
					out.write(data.get(key)+"\t"); //updn value
//					System.out.print(data.get(key)+"\t");
				}
				else{
					out.write("\t");
//					System.out.print("\t");
				}
			}
			out.newLine();
//			System.out.println();
//			count++;
//			if(count%10000==0)
//			System.out.println(count+" lines written.");
//			}
			out.close();
		} catch (IOException e) {
		}



	}

	private static void writeHeadings(){
		try {
			System.out.println("writing ehadings to file");
			BufferedWriter out = new BufferedWriter(new FileWriter("atlas_matrix.dat",true));
			for(int k=0; k<=2; k++){
				Iterator iter = headings.iterator(); //.entrySet().iterator();
				out.write("\t");
				while(iter.hasNext()){
					String[] heading = (String[])iter.next();
					out.write(heading[k]+"\t");
				}
				out.newLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void queryColHeaders(){
		System.out.println("querying DB for col headers");
		try {
			Statement stmt = connection.createStatement(); 



			ResultSet rset = stmt.executeQuery("select distinct experiment_id_key, EF, EFV " +
					"from atlas_full " +
			"order by EF,EFV");
//			OracleResultSet orset = (OracleResultSet) rset;
			String[] heading;
//			Vector clobs = new Vector();
			int index = 0;
			System.out.println("Retrieved records, starting to put into map");
			while(rset.next()){
				heading = new String[3];
				int exp = rset.getInt("experiment_id_key");
				String EF = rset.getString("EF");
				String EFV = rset.getString("EFV");
				heading[0] = Integer.toString(exp);
				heading[1] = EF;
				heading[2] = EFV;
				headings.add(heading);//.put(exp+"_"+EF+"_"+EFV, index);
//				index++;
			}
			rset.close();
			if (stmt != null)
				stmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}