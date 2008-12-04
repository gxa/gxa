package ds.R;

//import graphics.rmi.JGDPanelPop;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.bioconductor.packages.rservices.RArray;
import org.bioconductor.packages.rservices.RChar;
import org.bioconductor.packages.rservices.RInteger;
import org.bioconductor.packages.rservices.RLogical;

import ds.server.ExpressionDataSet;

import remoting.RServices;

/**
 * 
 * Class that takes care of plotting the TimeSeries R plot
 * 
 * @author hugo
 *
 */

public class RPlotTimeSeries implements RPlotPackage {

	Vector<String> plotPaths = null; // paths of the images

	public RPlotTimeSeries(Vector<String> plotPaths) {

		this.plotPaths = plotPaths;

	}
	
	
	public Vector<String> drawLargePlot(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath, String factor) throws Exception {

		boolean onlyDrawForFactor = false;
		
		if (factor != null)
			onlyDrawForFactor= true;
		
//		JGDPanelPop panel = null;
		
		int maxGenesForHLegend = 7;

		String bdcIds = new String();
		String bdcIdsAnn = new String();

		for (int a = 0; a < eds.getAr_DE().length; a++) {
			// R command that test to see if Design Element Ids exist in bdc matrix

			RLogical rl = (RLogical) r.getObject("any(de=='"+ eds.getAr_DE()[a] + "');");

			boolean brl = rl.getValue()[0];
			// if Design Element Ids dis in bdc matrix then we add ids to the
			// bdcIds variable
			if (brl) {
				bdcIds += "'" + eds.getAr_DE()[a] + "',";

				if (eds.getDeAnn().containsKey(eds.getAr_DE()[a])) {
					bdcIdsAnn += "'" + eds.getDeAnn().get(eds.getAr_DE()[a])
							+ "',";
				} else
					bdcIdsAnn += "'" + eds.getAr_DE()[a] + "',";

			}
		}

		// if no Design Elements Ids are in matrix bdc then we return from teh
		// method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")"; // paste all deids into a basic R c() command
		bdcIdsAnn = "c(" + bdcIdsAnn.substring(0, bdcIdsAnn.length() - 1) + ")";

		r.evaluate("bdcIds<-" + bdcIds);
		r.evaluate("bdcIdsAnn<-" + bdcIdsAnn);
		r.evaluate("legendX<-(max(length(bs)))+0.5;");
		r.evaluate("legendY<-max(bdc[" + bdcIds + ",]);");
		
		RChar exp_R = (RChar) r.getObject("expt_id");
		RChar array_id_R = (RChar) r.getObject("arraydesign_id");
		String exp = exp_R.getValue()[0];
		String array_id = array_id_R.getValue()[0];
		
		
		
		// System.out.println("A.1.2");
		RInteger dimEf = (RInteger) r.getObject("length(ef);"); 
		RInteger dimBS = (RInteger) r.getObject("length(bs);");	
		RInteger geneSize = (RInteger) r.getObject("length(bdcIds);"); 
		
		
		
		int bsSize = dimBS.getValue()[0];
		String xAxisFontSize = "1";
		
//		if (bsSize < 7)
//			panel = new JGDPanelPop(r.newDevice(bsSize*75, 400));
//		else if (bsSize < 30){
//			panel = new JGDPanelPop(r.newDevice(bsSize*50, 400));
//			xAxisFontSize = "0.8";
//		}
//		else {
//			panel = new JGDPanelPop(r.newDevice(bsSize*30, 400));
//			xAxisFontSize = "0.7";
//		}
		
		// System.out.println("A.1.3");
		int i_dimEf = dimEf.getValue()[0];
		// System.out.println("A.2");
		// for each factor we draw the plots where each factor are ordered
		
		boolean factorFound =false;
		
		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();
		
		for (int a = 1; a <= i_dimEf; a++) {
			
			if (onlyDrawForFactor){
				
				if (factor.equalsIgnoreCase(ar_EF[a-1])){
					factorFound = true;
				}
				else
					continue;
				
			}
			
			r.evaluate("currFactorIt<-" + a + ";");
			r.evaluate("efvd<-levels(factor(efv[,currFactorIt]));");
			r.evaluate("efvj<-efv[,currFactorIt];"); // retrieve matrix of
														// factor values
			r.evaluate("mj<-matrix(c(efvj)); "); // create matrix from one
													// column of factor value
			r.evaluate("oj<-order(mj); "); // get order of sorted factor values

			RChar currentFactor = (RChar) r.getObject("ef[currFactorIt]");
			
			
			r.evaluate("bdcIdsMeans<-getMean(currFactorIt);");
			r.evaluate("newM<-matrix(0,length(bdcIds)*2,length(bs));");
			r.evaluate("newM[1:length(bdcIds),]<-bdc[bdcIds,];");
			r.evaluate("newM[(length(bdcIds)+1):(length(bdcIds)*2),]<-bdcIdsMeans;");
			

			
			
			//if (bdcIds.split(",").length > 1) {
				//r.evaluate("matplot(t(bdc[bdcIds , oj]), type='l',  xaxt = 'n', xlab='Samples', ylab='Expression Value', col=1:20, lty=1, xlim=c(1,(max(length(bs))*1.15)),  ylim=c(min(bdc[bdcIds , ]),max(bdc[bdcIds,]) + (max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.1))");
				//r.evaluate("matplot(t(bdc[bdcIds , oj]), type='l',  xaxt = 'n', xlab='Samples', ylab='Expression Value', col=1:(length(bdcIds)), lty=1, xlim=c(1,(length(bs)+2.5)),  ylim=c(min(bdc[bdcIds , ]),max(bdc[bdcIds,]) + (max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.1))");
				//
			if((bsSize > 25 && geneSize.getValue()[0] < maxGenesForHLegend)  || geneSize.getValue()[0] < 5)
				r.evaluate("matplot(t(newM[ , oj]), type='l',  xaxt = 'n', xlab='Samples', ylab='Expression Value', col=1:(length(bdcIds)), lty=c(rep(1,length(bdcIds)),rep(3,length(bdcIds))),pch=c(rep(18,length(bdcIds)),rep(1,length(bdcIds))), xlim=c(1,length(bs)),  ylim=c((min(newM)-(max(newM)-min(newM))*0.125),max(newM) + (max(newM)-min(newM))*0.15), xaxs='i');");
			else
				r.evaluate("matplot(t(newM[ , oj]), type='l',  xaxt = 'n', xlab='Samples', ylab='Expression Value', col=1:(length(bdcIds)), lty=c(rep(1,length(bdcIds)),rep(3,length(bdcIds))),pch=c(rep(18,length(bdcIds)),rep(1,length(bdcIds))), xlim=c(1,length(bs)+3),  ylim=c(min(newM),max(newM) + (max(newM)-min(newM))*0.15), xaxs='i');");
			
			
			

			r.evaluate("axis(1, 1:length(bs), oj, cex.axis="+xAxisFontSize+");");
			r.evaluate("title(ef[currFactorIt], sub = '',cex.main = 2,   font.main= 4, col.main= 'blue', cex.sub = 0.75, font.sub = 3, col.sub = 'red');");

			// Creating line between factor values and also labelling each
			// factor value on the top left section
			r.evaluate("efvd<-levels(factor(efv[,currFactorIt][oj])); ");

			RInteger efv = (RInteger) r.getObject("length(efvd);");
			for (int b = 1; b <= efv.getValue()[0]; b++) {
				r.evaluate("b<-" + b + ";");
				// get lower sample id for current factor value
				r.evaluate("rectxmin<-min(c(1:length(efv[,currFactorIt]))[efv[,currFactorIt][oj]==efvd[b]])-0.5;"); 
				// get higher sample id for current factor value
				r.evaluate("rectxmax<-max(c(1:length(efv[,currFactorIt]))[efv[,currFactorIt][oj]==efvd[b]]);"); 
				//if (b %2 ==0)
				//	r.evaluate("rectBorder<-(max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.075;"); //calculate a border size that will
				//else
				//	r.evaluate("rectBorder<-(max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.15;"); //calculate a border size that will
				if (b % 2 == 0)
					r.evaluate("rectBorder<-(max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.1;");
				else
					r.evaluate("rectBorder<-(max(bdc[bdcIds,])-min(bdc[bdcIds,]))*0.19;");
				
				if (b != 1)
					r.evaluate("legend(rectxmin+0.2, max(bdc[bdcIds,])+rectBorder,efvd[b] ,box.lty=0, box.lwd=0, cex=0.9);"); 
				else
					r.evaluate("legend(rectxmin+0.7, max(bdc[bdcIds,])+rectBorder,efvd[b] ,box.lty=0, box.lwd=0, cex=0.9);"); 

				if (b != 1)
					r.evaluate("abline(v=rectxmin, col='darkgrey', lwd=1, lty=2);");

			}
			
			if(onlyDrawForFactor && !factorFound)
				return plotPaths;
				
			if((bsSize > 25 && geneSize.getValue()[0] < maxGenesForHLegend)  || geneSize.getValue()[0] < 5)
				r.evaluate("legend('bottom', bdcIdsAnn, col=1:(length(bdcIds)),  lty=1, box.lty=1, box.lwd=1, horiz=TRUE, inset = .02);");

			else
				r.evaluate("legend('right', bdcIdsAnn, col=1:(length(bdcIds)),  lty=1, inset = .005);");
	
			
			String imageName = exp+"_"+array_id+"_"+currentFactor.getValue()[0] + "_"
			+ System.currentTimeMillis() + ".png";
			
			String pngFullPath = thumbnails_filepath + imageName;

			File outfile = new File(pngFullPath);
//			panel.popNow(); // retrieve image from r Ressource since r is a
//							// thread and we must force the image pop now
//			BufferedImage im = panel.getImage();
			plotPaths.add(imageName);
//			ImageIO.write(im, "png", outfile); // Write thumbnails to file
			System.out.println(pngFullPath + " written !");
			// System.out.println("A.8");

		}

		return plotPaths;

	}

	/**
	 * 
	 * Draw thumbnails
	 * 
	 */

	public Vector<String> drawThumbnail(ExpressionDataSet eds, RServices r,
			String thumbnails_filepath, String factor) throws Exception {

		// Panel from which the image will be taken
		boolean onlyDrawForFactor = false;
		
		if (factor != null)
			onlyDrawForFactor= true;

		String bdcIds = new String(); // ids of the genes to be drawn
		
		//r.evaluate("de<-row.names(exprs(eset));"); // store expression matrix row names in R variable 'de'

		for (int a = 0; a < eds.getAr_DE().length; a++) {
			// look if ids exist in expression matrix row names
			RLogical rl = (RLogical) r.getObject("any(de=='"+ eds.getAr_DE()[a] + "');");

			boolean brl = rl.getValue()[0];
			// if Design Element Ids dis in bdc matrix then we add ids to the bdcIds variable
			if (brl)
				bdcIds += "'" + eds.getAr_DE()[a] + "',";
		}

		// if no Design Elements Ids are in matrix bdc then we return from the method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		// paste all deids into a basic R c() command
		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")"; 
		
		/*
		r.evaluate("bs<-sampleNames(eset);"); 		// retrieve biosample id from the dataframe
		r.evaluate("bdc<-exprs(eset);");			// retrieve bdc matrix
		r.evaluate("ef<-varLabels(eset);");			// retrieve factor name
		r.evaluate("efv<-phenoData(eset)@data;");	// retrieve factor values matrix
		*/
		r.evaluate("bdcIds<-" + bdcIds);			// pass bdcIds to R environement
		//RChar study_id = (RChar) r.getObject("attributes(experimentData(eset))$other$aew.exptid;");
		
//		JGDPanelPop panel = new JGDPanelPop(r.newDevice(450, 200));
		
		RInteger dimEf = (RInteger) r.getObject("length(ef);"); //get number of factor
		RChar exp_R = (RChar) r.getObject("expt_id");
		RChar array_id_R = (RChar) r.getObject("arraydesign_id");
		String exp = exp_R.getValue()[0];
		String array_id = array_id_R.getValue()[0];
		
		
		int i_dimEf = dimEf.getValue()[0];

		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();
		
		for (int a = 1; a <= i_dimEf; a++) {
			
			if (onlyDrawForFactor){
				
				if (!factor.equalsIgnoreCase(ar_EF[a-1]))
					continue;
				
			}
			
			
			
			
			//JGDPanelPop panel = new JGDPanelPop(r.newDevice(600, 300));
			
			r.evaluate("efvj<-efv[," + a + "];"); // retrieve matrix of factor values
			r.evaluate("mj<-matrix(c(efvj)); "); // create matrix from one column of factor value
			r.evaluate("oj<-order(mj); "); // get order of sorted factor values

			RChar currentFactor = (RChar) r.getObject("ef[" + a + "]");

			if (bdcIds.split(",").length > 1) {
				r.evaluate("matplot(t(bdc["+ bdcIds+ " , oj]), type='l', xaxt = 'n', main=ef["+ a+ "], xlab='Samples', ylab='Expr. Value', col=1:20, lty=1);");

			} else {
				r.evaluate("matplot(bdc["+ bdcIds+ " , oj], type='l', xaxt = 'n', main=ef["+ a+ "], xlab='Samples', ylab='Expr. Value', col=1:20, lty=1);");
			}

			String imageName = exp+"_"+array_id+"_"+currentFactor.getValue()[0] + "_"
					+ System.currentTimeMillis() + "_thb" + ".png";
			String pngFullPath = thumbnails_filepath + imageName;

			File outfile = new File(pngFullPath);
//			panel.popNow(); // retrieve image from r Ressource since r is a
							// thread and we must force the image pop now
//			BufferedImage im = panel.getImage();
//			ImageIO.write(im, "png", outfile); // Write thumbnails to file

			System.out.println(thumbnails_filepath + currentFactor.getValue()[0] + "_"
					+ System.currentTimeMillis() + "_thb" + ".png written !");
			plotPaths.add(imageName);
			
		}
		
		
		
		return plotPaths;

	}
	
}
