package ds.R;

import java.io.File;
import java.util.Vector;

import org.kchine.r.server.RServices;
import org.kchine.r.server.graphics.GDDevice;
import org.kchine.r.RChar;
import org.kchine.r.RLogical;
import org.kchine.r.RInteger;

import ds.server.ExpressionDataSet;


/**
 * 
 * This class integrates all of the necessary R commands needed to draw Atlas-like plots
 * 
 * @author hugo
 *
 */

public class RPlotAtlas implements RPlotPackage {

	Vector<String> plotPaths = null;
	
	public RPlotAtlas(Vector<String> plotPaths){
		
		this.plotPaths = plotPaths;
		
	}
	
	public Vector<String> drawLargePlot(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath, String factor) throws Exception {
		
		boolean onlyDrawForFactor = false; // whether or not plot is draw for one or all factors
		
		if (factor != null)
			onlyDrawForFactor= true;
		
		String bdcIds = new String();
		String bdcIdsAnn = new String();

		r.evaluate("de<-row.names(exprs(eset));"); // retrieve de ids
		
		// retrieve study_id needed for image name
		RChar study_id = (RChar) r.getObject("attributes(experimentData(eset))$other$aew.exptid;");

		// for all the selected de ids
		for (int a = 0; a < eds.getAr_DE().length; a++) {
			RLogical rl = (RLogical) r.getObject("any(de=='"+ eds.getAr_DE()[a] + "');"); // test to see if any selected de ids exists in this netcdf
			boolean brl = rl.getValue()[0]; // boolean: is the de found in the file ?

			// if Design Element Ids is in bdc matrix then we add ids to the bdcIds variable
			if (brl) {
				bdcIds += "'" + eds.getAr_DE()[a] + "',"; // add de id to the list of of ids to be drawn

				if (eds.getDeAnn().containsKey(eds.getAr_DE()[a])) { // if annotation exists for this ids then replace name with annotation
					bdcIdsAnn += "'"+ eds.getDeAnn().get(eds.getAr_DE()[a]) + "',";
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
		RInteger dimEf = (RInteger) r.getObject("length(varLabels(phenoData(eset)));"); // get number of factors
		int i_dimEf = dimEf.getValue()[0];

		r.evaluate("genedata4=melt(data.frame(eset["+ bdcIds+ ",], bsid=sampleNames(phenoData(eset))),variable_name='gene');");  // retrieve data frame for selected de ids
		
		for (int a = 1; a <= i_dimEf; a++) { // iterate over factors
			
			RChar currentFactor = (RChar) r.getObject("varLabels(phenoData(eset))["+ a + "];"); // get factor name
			String currF = currentFactor.getValue()[0];
			
			if (onlyDrawForFactor){ // if we draw only one factor then jump to next iteration if its not the right factor
				if (!factor.equalsIgnoreCase(currF))
					continue;
			}
			
			RInteger efvSize = (RInteger) r.getObject("length(levels(factor(phenoData(eset)@data[,"+a+"])));"); // get number of factor values

			if (efvSize.getValue()[0] < 30) // if there are more than 30 factor values then the image needs to increase according to the number
				device = (GDDevice) r.newDevice(1000, 450);
			else
				device = (GDDevice) r.newDevice(1000, efvSize.getValue()[0]*12);

			r.evaluate("h1<-drawDiffExGenes(eset,'"+currF+"',"+bdcIds+", "+bdcIdsAnn+",FALSE);"); // draw image
			r.evaluate("pushViewport(viewport(width=0.95));"); // request image to viewport
			r.evaluate("print(h1, newpage=FALSE, pretty=TRUE);"); // print image
			r.evaluate("upViewport();"); // request viewport in order to be able to retrieve and save image later on

//			JGDPanelPop panel = new JGDPanelPop(device, false, false, null);
//			panel.popNow(); // request image now
			
			String imageName = study_id.getValue()[0]+ "_" + currentFactor.getValue()[0] + "_" + System.currentTimeMillis() + ".png"; // image name
			String pngFullPath = thumbnails_filepath + imageName; // full path to image

			File outfile = new File(pngFullPath);
//			ImageIO.write(panel.getImage(), "png", outfile); // Write thumbnails to file

			System.out.println(pngFullPath);
			this.plotPaths.add(imageName); // add image to returned images full path

		}
		
		return plotPaths;
	}

	public Vector<String> drawThumbnail(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath, String factor) throws Exception {
		
		boolean onlyDrawForFactor = false;
		
		if (factor != null)
			onlyDrawForFactor= true;
		
		String thbStr = "_thb";

		String bdcIds = new String();
		String bdcIdsAnn = new String();

		r.evaluate("de<-row.names(exprs(eset));"); // retrieve de ids
		RChar study_id = (RChar) r.getObject("attributes(experimentData(eset))$other$aew.exptid;"); // retrieve study_id needed for image name

		// for all the selected de ids
		for (int a = 0; a < eds.getAr_DE().length; a++) {
			RLogical rl = (RLogical) r.getObject("any(de=='"+ eds.getAr_DE()[a] + "');");
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

		GDDevice device = (GDDevice) r.newDevice(225, 110); // image size
		RInteger dimEf = (RInteger) r.getObject("length(varLabels(phenoData(eset)));"); // get number of factors
		int i_dimEf = dimEf.getValue()[0];

		r.evaluate("genedata4=melt(data.frame(eset["+ bdcIds+ ",], bsid=sampleNames(phenoData(eset))),variable_name='gene');");

		// go over all factor
		for (int a = 1; a <= i_dimEf; a++) {
			
			RChar currentFactor = (RChar) r.getObject("varLabels(phenoData(eset))["+ a + "];"); // get current factor from R environment
			String currF = currentFactor.getValue()[0];
			
			if (onlyDrawForFactor){ // for one-factor-drawing, only draw the selected factor
				
				if (!factor.equalsIgnoreCase(currF))
					continue;
				
			}

			r.evaluate("h1<-drawDiffExGenes(eset,'"+currF+"',"+bdcIds+", FALSE);"); //draw image
			r.evaluate("pushViewport(viewport());"); // push image to viewport
			r.evaluate("print(h1, newpage=FALSE, pretty=FALSE);"); 
			r.evaluate("upViewport();"); // request viewport in order to be able to retrieve and save image later on

//			JGDPanelPop panel = new JGDPanelPop(device, false, false, null);
//			panel.popNow(); // request image now
			
			String imageName = study_id.getValue()[0]+"_"+currentFactor.getValue()[0] + "_"	+ System.currentTimeMillis() + thbStr + ".png";
			
			String pngFullPath = thumbnails_filepath + imageName;

			File outfile = new File(pngFullPath);
//			ImageIO.write(panel.getImage(), "png", outfile); // Write thumbnails to file

			System.out.println(pngFullPath);
			plotPaths.add(imageName);

		}
		
		return plotPaths;
	}

}
