import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;

public class ZROIAxisProfiler_ implements PlugInFilter, Measurements  {

	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL+NO_CHANGES+ROI_REQUIRED;
	}

	public void run(ImageProcessor ip) {
		if (imp.getStackSize()<2) {
			IJ.showMessage("ZAxisProfiler", "This command requires a stack.");
			return;
		}
		Roi roi = imp.getRoi();
		if (roi.getType()>=Roi.LINE) {
			IJ.showMessage("ZAxisProfiler", "This command does not work with line selections.");
			return;
		}
		 double minThreshold = ip.getMinThreshold();
		double maxThreshold = ip.getMaxThreshold();
		float[] y = getZAxisProfile(roi, minThreshold, maxThreshold);
		Calibration cal = imp.getCalibration();
		String timeUnit = cal.getTimeUnit();
		if (y!=null) {
			float[] x = new float[y.length];

			for (int i=0; i<4; i++) x[i] = 0;

			for (int i=4; i<x.length; i++) 
				{
			
				if (cal.frameInterval==0)
					{x[i] = ((i-4));
					timeUnit="slice";
					}
				else { x[i] = ((i-4)*(float)cal.frameInterval);
					}
				}

			Rectangle r = imp.getRoi().getBoundingRect();
			PlotWindow pw = new PlotWindow("T-series: "+ imp.getTitle()+"-x"+r.x+".y"+r.y+".w"+r.width+".h"+r.height,timeUnit, "Mean", x, y);
			double [] a = Tools.getMinMax(x);
           			double xmin=a[0], xmax=a[1];
		            float [] values2 = new float [y.length-4];
		            int valsize = values2.length;
			for (int j=0; j<valsize; j++) values2[j] = y[j+4];
            		a = Tools.getMinMax(values2);
		            double ymin=a[0], ymax=a[1];
		            pw.setLimits(xmin,xmax,ymin,ymax);
			pw.draw();
			//IJ.write("x="+r.x+", y="+r.y+", width="+r.width+", height="+r.height);
		}
	}

	float[] getZAxisProfile(Roi roi, double minThreshold, double maxThreshold) {
		ImageStack stack = imp.getStack();
		int size = stack.getSize();
		float[] values = new float[size+4];
		float[] x = new float[size+4];
		
		ImageProcessor mask = imp.getMask();
		//int[] mask = imp.getMask();
		Rectangle r = imp.getRoi().getBoundingRect();
		Calibration cal = imp.getCalibration();
		Analyzer analyzer = new Analyzer(imp);
values[0] = r.x;
values[1] = r.y;
values[2] = r.width;
values[3] = r.height;
		int measurements = analyzer.getMeasurements();
		boolean showResults = measurements!=0 && measurements!=LIMIT;
       		boolean showingLabels = (measurements&LABELS)!=0 || (measurements&SLICE)!=0;

		measurements |= MEAN;
	//	if (showResults) {
	//	if (!analyzer.resetCounter())
	//			return null;
	//	}


		for (int i=1; i<=size; i++) {
			ImageProcessor ip = stack.getProcessor(i);
			// if (showingLabels) imp.setSlice(i);
			

			if (minThreshold!=ImageProcessor.NO_THRESHOLD)
			ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
       
			ip.setRoi(r);
			ip.setMask(mask);
			ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
			analyzer.saveResults(stats, roi);
			//if (showResults)
			//analyzer.displayResults();
			values[i+3] = (float)stats.mean;
				}
		return values;
			}

}


