package ds.server;

import java.util.Vector;

/**
 * 
 * Interface that gives a minimal access to the DataServer class. It only allows monitoring of 
 * the current process status and the retrieval of the image paths without having access to other methods.
 * 
 * This class is returned by the DataServerAPI if the plotting method chosen does not wait on the plotting processes. 
 * 
 * @author hugo
 *
 */
public interface DataServerMonitor {
	
	public boolean isProcessFinished();
	
	public Vector<String> retrieveImagePath();
	
	public void waitOnProcessToFinish();

}
