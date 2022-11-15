import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;

//modfied from Z-axis profiler
//T.Collins Jan 07
//www.macbiophotonics.ca

public class BG_Subtraction_from_ROI implements PlugInFilter, Measurements  {

	


		

ImagePlus imp;
	float nStDev = 2;

	public int setup(String arg, ImagePlus imp) 

	{this.imp = imp;

        
	return DOES_ALL+NO_CHANGES+ROI_REQUIRED;


	}

	public void run(ImageProcessor ip) {
		Roi roi = imp.getRoi();
		   GenericDialog gd = new GenericDialog("BG subtraction");
	gd.addNumericField("Number of stdev from mean",2,1);

	gd.showDialog();

       	nStDev = (float)gd.getNextNumber();

		if (roi.getType()>=Roi.LINE)
					{
					IJ.showMessage("BG_Subtraction_from _ROI", "This command does not work with line selections.");
					return;
					}

			float[] bg = getZAxisProfile(roi);
		//do BG subtraction
				ImageStack img = imp.getStack();
				ImageProcessor ip2 = img.getProcessor(1);

				for(int s=1; s<=imp.getStackSize(); s++)
					{ip2 = img.getProcessor(s);
					for (int x=0; x<=imp.getWidth(); x++)
						{for(int y=0; y<=imp.getHeight(); y++)
							ip2.putPixelValue(x,y, (int)((float)ip2.getPixelValue(x,y)-(float)bg[s]));

						}
					}
		ip2.resetMinAndMax();
		imp.updateAndDraw();
	}

	float[] getZAxisProfile(Roi roi) {
		ImageStack stack = imp.getStack();
		int size = stack.getSize();
		float[] values = new float[size+4];
		float[] values_std = new float[size+4];

		ImageProcessor mask = imp.getMask();
	//	int[] mask = imp.getMask();
		
		Rectangle r = imp.getRoi().getBoundingRect();
		Calibration cal = imp.getCalibration();


			
		for (int i=1; i<=size; i++) {
			ImageProcessor ip = stack.getProcessor(i);
			ip.setRoi(r);
			ip.setMask(mask);
			ImageStatistics stats = ImageStatistics.getStatistics(ip, MEAN, cal);

			//analyzer.saveResults(stats, roi);
			//if (showResults)
			//analyzer.displayResults();
			values[i] = (float)stats.mean + (nStDev *(float)stats.STD_DEV);
			

				}
		return values;
			}

}


