package ds.R;

import java.io.File;
import java.util.Vector;

import org.kchine.r.server.RServices;
import org.kchine.r.server.graphics.GDDevice;
import org.kchine.r.RChar;
import org.kchine.r.RLogical;
import org.kchine.r.RInteger;

import ds.server.ExpressionDataSet;

public class RPlotBoxPlot implements RPlotPackage {
	
	Vector<String> plotPaths = null;
	
	public RPlotBoxPlot(Vector<String> plotPaths){
		
		this.plotPaths = plotPaths;
		
	}

	public Vector<String> drawLargePlot(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath, String factor) throws Exception {

		String thbStr = "_thb";

		String bdcIds = new String();
		String bdcIdsAnn = new String();
		RChar study_id = (RChar) r.getObject("attributes(experimentData(eset))$other$aew.exptid;");

		for (int a = 0; a < eds.getAr_DE().length; a++) {
			// R command that test to see if Design Element Ids exist in bdc matrix
			
			RLogical rl = (RLogical) r.getObject("any(row.names(exprs(eset))=='"+ eds.getAr_DE()[a] + "')");
			boolean brl = rl.getValue()[0];
			// if Design Element Ids dis in bdc matrix then we add ids to the bdcIds variable
			if (brl) {
				bdcIds += "'" + eds.getAr_DE()[a] + "',";

				if (eds.getDeAnn().containsKey(eds.getAr_DE()[a])) {
					bdcIdsAnn += "'"
							+ eds.getDeAnn().get(eds.getAr_DE()[a]) + "',";
				} else
					bdcIdsAnn += "'" + eds.getAr_DE()[a] + "',";

			}
		}

		// if no Design Elements Ids are in matrix bdc then we return from teh method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")"; // paste all deids into a basic R c() command
		bdcIdsAnn = "c(" + bdcIdsAnn.substring(0, bdcIdsAnn.length() - 1)+ ")"; // paste all de annotations into a basic R c() command

		GDDevice device = null;

		device = (GDDevice) r.newDevice(900, 450);

		RInteger dimEf = (RInteger) r.getObject("length(varLabels(phenoData(eset)));");
		int i_dimEf = dimEf.getValue()[0];

		r.evaluate("genedata4=melt(data.frame(eset["+ bdcIds+ ",], bsid=sampleNames(phenoData(eset))),variable_name='gene');");

		// for each factor we draw the plots where each factor are ordered  
		for (int a = 1; a <= i_dimEf; a++) {

			RChar currentFactor = (RChar) r.getObject("varLabels(phenoData(eset))["+ a + "]");
			String currF = currentFactor.getValue()[0];

			r.evaluate("h1<-qplot("+ currF+ ", value, data=genedata4, colour=gene, geom=c('jitter', 'boxplot'));");
			r.evaluate("print(h1, newpage=FALSE, pretty=TRUE);");
			r.evaluate("upViewport();"); // request viewport in order to be able to retrieve and save image later on

//			JGDPanelPop panel = new JGDPanelPop(device, false, false, null);
//			panel.popNow();
			
			String imageName = study_id.getValue()[0]+"_"
			+ currentFactor.getValue()[0] + "_"
			+ System.currentTimeMillis() + thbStr + ".png";
			
			String pngFullPath = thumbnails_filepath + imageName;

			File outfile = new File(pngFullPath);
//			ImageIO.write(panel.getImage(), "png", outfile); // Write thumbnails to file

			System.out.println(pngFullPath);
			
			plotPaths.add(imageName);

		}
		
		return plotPaths;
	}

	public Vector<String> drawThumbnail(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath , String factor) throws Exception {
		
		String thbStr = "_thb";

		String bdcIds = new String();
		String bdcIdsAnn = new String();
		RChar study_id = (RChar) r.getObject("attributes(experimentData(eset))$other$aew.exptid;");

		for (int a = 0; a < eds.getAr_DE().length; a++) {
			// R command that test to see if Design Element Ids exist in bdc matrix
			RLogical rl = (RLogical) r.getObject("any(row.names(exprs(eset))=='"+ eds.getAr_DE()[a] + "')");
			boolean brl = rl.getValue()[0];
			// if Design Element Ids dis in bdc matrix then we add ids to the bdcIds variable
			if (brl) {
				bdcIds += "'" + eds.getAr_DE()[a] + "',";

				if (eds.getDeAnn().containsKey(eds.getAr_DE()[a])) {
					bdcIdsAnn += "'"
							+ eds.getDeAnn().get(eds.getAr_DE()[a]) + "',";
				} else
					bdcIdsAnn += "'" + eds.getAr_DE()[a] + "',";

			}
		}

		// if no Design Elements Ids are in matrix bdc then we return from teh method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")"; // paste all deids into a basic R c() command
		bdcIdsAnn = "c(" + bdcIdsAnn.substring(0, bdcIdsAnn.length() - 1)+ ")"; // paste all de annotations into a basic R c() command

		GDDevice device = null;

		device = (GDDevice) r.newDevice(225, 110);
		
		RInteger dimEf = (RInteger) r.getObject("length(varLabels(phenoData(eset)));");
		int i_dimEf = dimEf.getValue()[0];

		r.evaluate("genedata4=melt(data.frame(eset["+ bdcIds+ ",], bsid=sampleNames(phenoData(eset))),variable_name='gene');");

		// for each factor we draw the plots where each factor are ordered  
		for (int a = 1; a <= i_dimEf; a++) {

			RChar currentFactor = (RChar) r.getObject("varLabels(phenoData(eset))["+ a + "]");
			String currF = currentFactor.getValue()[0];

			r.evaluate("h1<-qplot("+ currF+ ", value, data=genedata4, colour=gene, geom=c('boxplot'));");
			r.evaluate("pushViewport(viewport());");
			r.evaluate("print(h1, newpage=FALSE, pretty=FALSE);");
			r.evaluate("grid.gedit(gPath('xaxis','label'), gp=gpar(fontsize=1));");
			r.evaluate("grid.gedit(gPath('yaxis','label'), gp=gpar(fontsize=1));");
			r.evaluate("grid.gedit(gPath('yaxis','ticks'), gp=gpar(lty=0));");
			r.evaluate("grid.gedit(gPath('xaxis','ticks'), gp=gpar(lty=0));");
			
			r.evaluate("upViewport();"); // request viewport in order to be able to retrieve and save image later on

//			JGDPanelPop panel = new JGDPanelPop(device, false, false, null);
//			panel.popNow();
			
			String imageName = study_id.getValue()[0]+"_"
			+ currentFactor.getValue()[0] + "_"
			+ System.currentTimeMillis() + thbStr + ".png";
			
			String pngFullPath = thumbnails_filepath + imageName;

			File outfile = new File(pngFullPath);
//			ImageIO.write(panel.getImage(), "png", outfile); // Write thumbnails to file

			System.out.println(pngFullPath);
			
			plotPaths.add(imageName);

		}
		
		return plotPaths;
	}

}
