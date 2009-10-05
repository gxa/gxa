package ds.R;

import java.util.Vector;

import ds.server.ExpressionDataSet;
import org.kchine.r.server.RServices;

public interface RPlotPackage {
	
	public Vector<String> drawLargePlot(ExpressionDataSet eds, RServices r, String thumbnails_filepath, String factor) throws Exception;
	
	public Vector<String> drawThumbnail(ExpressionDataSet eds, RServices r, String thumbnails_filepath, String factor) throws Exception;

}
