import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;

public class FLIM_stack implements PlugInFilter, Measurements  {
static boolean headingsSet;
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
			
				if (cal.frameInterval<1)
					{x[i] = ((i-4));
					timeUnit="slice";
					}
				else { x[i] = ((i-4)*(float)cal.frameInterval);
					}
				}

			Rectangle r = imp.getRoi().getBoundingRect();
			
	String Headings = "Image\tx\ty\tw\th\tAxAm\tDxDm\tDxAm\tphotons\ttau\n";

		if ((!headingsSet))
			{ 
			IJ.setColumnHeadings(Headings);
			headingsSet = true;
//	      		IJ.write(Headings);
	  		}				

			IJ.write(imp.getTitle()+"\t"+r.x+"\t"+r.y+"\t"+r.width+"\t"+r.height+"\t"+y[4]+"\t"+y[5]+"\t"+y[6]+"\t"+y[9]+"\t"+y[10]);
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


