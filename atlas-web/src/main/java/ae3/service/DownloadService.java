package ae3.service;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import ae3.service.structuredquery.AtlasStructuredQuery;

/**
 * Manages Atlas download requests on list results.
 * @author iemam
 *
 */
public class DownloadService {
	private static DownloadService instance;
	private static HashMap<String, ArrayList<Download>>  downloads;
	
	public static DownloadService instance() {
		if(null == instance) {
	         instance = new DownloadService();
	    }
		return instance;
	}
	
	public void initialize(){
		downloads = new HashMap<String, ArrayList<Download>>();
	}
	
	public static ArrayList<Download> getDownloads(String sessionID){
		return downloads.get(sessionID);
	}
	
	public void addDownload(Download download){
//		downloads.put("",download);
	}
	
	public static void requestDownload(HttpSession session, AtlasStructuredQuery query){
		ArrayList<Download> downloadList;
		if(downloads.containsKey(session.getId())){
			downloadList =  downloads.get(session.getId());
		}
		else{
			downloadList = new ArrayList<Download>();
		}
		Download download = new Download(query);
		downloadList.add(download);
		downloads.put(session.getId(), downloadList);
		download.doDownload();
	}
	
	public static int getNumOfDownloads(String sessionID){
		if(downloads.containsKey(sessionID))
			return downloads.get(sessionID).size();
		else 
			return 0;
	}
	
	public static StringBuilder getFileContent(String sessionID, String qid){
		StringBuilder strBuf = new StringBuilder();
		
		if(downloads.containsKey(sessionID)){
			return downloads.get(sessionID).get(Integer.parseInt(qid)).getFileContents();
		}
		return strBuf;
			
	}
	
	public static String getDownloadFileName(String sessionID, String qid){
		String filename = "download";
		if(downloads.containsKey(sessionID)){
			Download dn = downloads.get(sessionID).get(Integer.parseInt(qid));
			if(dn != null){
				filename = dn.getFile();
			}
			
		}
		return filename;
	}
}
