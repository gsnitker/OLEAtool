import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;
import ij.measure.CurveFitter;
import java.awt.*;
import java.text.DecimalFormat; 
import java.awt.image.*;
import ij.plugin.frame.RoiManager;
import java.util.*;


public class FLIM_Profiler implements PlugInFilter, Measurements  {
	static boolean headingsSet;
	ImagePlus imp;
	DecimalFormat df3 = new DecimalFormat("##0.000");
	DecimalFormat df2 = new DecimalFormat("##0.00");
	DecimalFormat df1 = new DecimalFormat("##0.0");
	RoiManager rm;
	boolean showPlot;
	//float frameInterval= (float)0.0390625;


	public int setup(String arg, ImagePlus imp) {
		
		this.imp = imp;
		return DOES_ALL+NO_CHANGES+ROI_REQUIRED;
		
	}

	public void run(ImageProcessor ip) {
		
		if (imp.getStackSize()<2) {
			IJ.showMessage("FLIM Profiler", "This command requires a stack.");
			return;
		}
		showPlot= IJ.altKeyDown();
		Roi roi = imp.getRoi();
		Rectangle r = roi.getBoundingRect();
		
		if (roi.getType()>=Roi.LINE) {
			IJ.showMessage("ZAxisProfiler", "This command does not work with line selections.");
			return;
		}
		double minThreshold = ip.getMinThreshold();
	        	double maxThreshold = ip.getMaxThreshold();


//get profile
		float[] yFulltot = getZAxisProfile(roi, minThreshold, maxThreshold);
		float[] yFull =new float [yFulltot.length-2];
		for (int n = 0; n<yFull.length; n++)
			{
			yFull[n]=yFulltot[n];
			}

		float  frameInterval = 80/imp.getStackSize();
		
		Calibration cal = imp.getCalibration();
		String timeUnit = cal.getTimeUnit();
		cal.frameInterval= frameInterval;
		imp.setCalibration(cal);
		imp.updateAndDraw();
		
//get max	
		float yFmax=0;
		float yMax=0;
		float yFmin=0;
		float yMin=0;
		boolean foundPeak=false;
		float lastY=0;
		int peakSlice=0;
		
		

//find max and peakslice
		for (int v = 0; v<yFull.length; v++)
			{
			if(yFmax<yFull[v])
				{yFmax=yFull[v];
				peakSlice=v;}
			lastY=yFull[v];
			}

		float[]yDecay = new float[(yFull.length-peakSlice)]; 	


		for (int v2 = 0; v2<yDecay.length; v2++)
			{yDecay[v2]=yFull[v2+peakSlice];
			}
		
		
		float timescale = 1;
		
		float[] x = new float[yFull.length];
		for (int i=0; i<x.length; i++) 
			{
			if (cal.frameInterval==0)
				{x[i] = ((i));}
			else 
				{ x[i] = ((i)*(float)frameInterval); 
				timescale = (float)frameInterval;
				}
			}

//generate new arrays for fitting
		double[] yDecayD =  new double[yFull.length-peakSlice];
		double [] x2D = new double[yFull.length-peakSlice];
		float[] x2 = new float[yFull.length-peakSlice];

		for (int i=0; i<x2.length; i++) 
			{
			x2D[i]=(double)x[i];
			x2[i]=x[i];
			yDecayD[i]=yDecay[i];
			}
//fit curve
		CurveFitter cf = new CurveFitter(x2D, yDecayD) ;
		cf.doFit(CurveFitter.EXP_WITH_OFFSET);
	//	cf.doFit(CurveFitter.EXPONENTIAL);
		double[] p = cf.getParams();	
		double tmp=0;

String Headings = "Image\tmean peak\ttau\toffset\tphotons\tarea\trChi^2\n";


if ((!headingsSet))
		{ 
		IJ.setColumnHeadings(Headings);
		headingsSet = true;
  		}	


//generate fitted data
		float chi2=0;

		float[] fittedDecay = new float [x2.length];
		float[] residuals= new float [x2.length];
		for (int w=0; w<x2.length; w++) 
			{
			fittedDecay[w] = (float)(p[0]*Math.exp(p[1]*x2[w]*-1)+p[2]);

			x2[w] = x[w+peakSlice];
			
			chi2=chi2+(((yDecay[w]-fittedDecay[w])*(yDecay[w]-fittedDecay[w]))/fittedDecay[w]);
			//IJ.log("Obs:" + yDecay[w] +"   Expt:" + fittedDecay[w] +"  ch2: "+(((yDecay[w]-fittedDecay[w])*(yDecay[w]-fittedDecay[w]))/fittedDecay[w]));
			residuals[w]=fittedDecay[w]-yDecay[w];
			}

//p[0]*Math.exp(p[1]*x*-1)+p[2];
	IJ.write(imp.getTitle() + "\t"+ df3.format(p[0]) +"\t" + df3.format(1/p[1])+"\t" + df3.format(p[2])+"\t" +yFulltot[yFulltot.length-2]+"\t" +yFulltot[yFulltot.length-1] +"\t" +df2.format(chi2/(float)(x2.length-1)));
	//IJ.write(imp.getTitle() + "\t"+ df3.format(p[0]) +"\t" + df3.format(-1/p[1])+"\t" + "na"+"\t" +yFulltot[yFulltot.length-2]+"\t" +yFulltot[yFulltot.length-1]);



		
		
		if(showPlot)
			{
			PlotWindow pwF = new PlotWindow("Tau "+ imp.getTitle()+"-x"+r.x+".y"+r.y+".w"+r.width+".h"+r.height, timeUnit, "Mean", x, yFull);
			double [] a = Tools.getMinMax(x);
	            	double 	xmin=a[0], xmax=a[1];
			pwF.setLimits(xmin,xmax,yFmin,yFmax);
			pwF.setColor(Color.red);
			pwF.addPoints(x2, fittedDecay, PlotWindow.LINE);
			pwF.setColor(Color.black);
			pwF.draw();
			PlotWindow pwR = new PlotWindow("Residuals"+ imp.getTitle()+"-x"+r.x+".y"+r.y+".w"+r.width+".h"+r.height, timeUnit, "Mean", x2, residuals);
			pwR.draw();
			}
			
		}

	float[] getZAxisProfile(Roi roi, double minThreshold, double maxThreshold) {

		ImageStack stack = imp.getStack();
		int size = stack.getSize();
		float[] values = new float[size+2];
		values[values.length-1]=0;
		imp.setRoi(roi);
		ImageProcessor mask = imp.getMask();
		Rectangle r = roi.getBoundingRect();
		Calibration cal = imp.getCalibration();
		Analyzer analyzer = new Analyzer(imp);
		
		int measurements = analyzer.getMeasurements();
		measurements |= MEAN;
		for (int i=1; i<=size; i++) {
			ImageProcessor ip = stack.getProcessor(i);
			if (minThreshold!=ImageProcessor.NO_THRESHOLD)
		            ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
			ip.setRoi(r);
			ip.setMask(mask);
			ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
			analyzer.saveResults(stats, roi);
			values[i-1] = (float)stats.mean;
			values[values.length-2]=values[values.length-2]+(float)(stats.mean);
			values[values.length-1]=(float)stats.area;

			}
		return values;
			}

}


