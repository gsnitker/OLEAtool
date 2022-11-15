import java.text.DecimalFormat; 
import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;
import ij.text.*;
import java.text.DecimalFormat; 
import ij.plugin.PlugIn;
import java.awt.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;


public class Ratio_ROI_Manager implements PlugInFilter, Measurements  {

	RoiManager roiManager;
	ImagePlus imp;
	StringBuffer sb = new StringBuffer();
	StringBuffer header = new StringBuffer();
	String timePoint="";
	DecimalFormat df2 = new DecimalFormat("##0.00");
	DecimalFormat df0 = new DecimalFormat("##0");
	boolean isAltKey =IJ.altKeyDown();
	int roiCount=0;
	RoiManager rm;

public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	return DOES_ALL+NO_CHANGES;
	}

public void run(ImageProcessor ip) 
	{
		ImagePlus imp= WindowManager.getCurrentImage();
		Calibration cal = imp.getCalibration();
		rm = RoiManager.getInstance();
		if (rm==null)
			{IJ.error("Roi Manager is not open"); return;}
		rm.select(0);	
		Roi roi = imp.getRoi();

		Hashtable table = rm.getROIs();
		java.awt.List list = rm.getList();
		int roiCount = list.getItemCount();
		Roi[] rois = new Roi[roiCount];
		for (int i=0; i<roiCount; i++) {
			String label = list.getItem(i);
			Roi roi2 = (Roi)table.get(label);
			if (roi2==null) continue;
			rois[i] = roi2;
		}
	
		String units=cal.getTimeUnit();
		header.append(units+"\t");
		ImageStack stack = imp.getStack();
		int size = stack.getSize()/2;
		float ratioValue=0;
		float ratioValue2=0;
		
		ImageProcessor mask = imp.getMask();
		//int[] mask = imp.getMask();
		Rectangle r = imp.getRoi().getBoundingRect();

	
		Analyzer analyzer = new Analyzer(imp);

		float [] ch1Prev = new float [roiCount];
		float [] ch1Prev2 = new float [roiCount];

		int measurements = analyzer.getMeasurements();
		boolean showResults = measurements!=0 && measurements!=LIMIT;
		measurements |= MEAN;
		String roiName = "";
		measurements |= MEDIAN;
			int sel=0;
		while(sel<roiCount) {	
			roiName =rois[sel].getName();
			roiName  = (sel+1)+" " + roiName;
			header.append(roiName+" ch1 \t"+roiName+" ch2\t"+roiName+" ratio\t");
			sel++;
		}
		header.append("\n");
		sb.append(header);

		if (showResults) 
			{
			if (!analyzer.resetCounter())
			return;
			}
		
		for (int i=1; i<=(size*2); i++) 
			{
			//IJ.showStatus("getting ip2");
			ImageProcessor ip2 = stack.getProcessor(i);	
			for(sel=0;sel<roiCount;sel++)
				{
				IJ.showStatus("Processing: "+i + " of "+ size*2);				IJ.showProgress(i, size*2);			
				//rm.select(sel);	
				//r = imp.getRoi().getBoundingRect();
				//mask = imp.getMask();

				ip2.setRoi(rois[sel]);
				//ip2.setMask(mask);
				ImageStatistics stats = ImageStatistics.getStatistics(ip2, measurements, cal);
				analyzer.saveResults(stats, roi);		
				timePoint=(df2.format(((i-1)/2)*cal.frameInterval));
				if (cal.frameInterval==0) timePoint=df0.format(i/2);
				if((sel==0)&&(i%2 == 0)) sb.append(timePoint);
				if (i%2!= 0)
				 	{
					ch1Prev[sel] = (float)stats.mean;
					
					}
				if (i%2 == 0)
					{
					if (isAltKey) 
						{ratioValue = (float)stats.mean/ch1Prev[sel];
						
						}
					if (!isAltKey) 
						{ratioValue = ch1Prev[sel]/(float)stats.mean;
						
						}				
					sb.append("\t"+ch1Prev[sel]+"\t"+(float)stats.mean+"\t"+ratioValue);
					}
				}
				if (i%2 == 0) sb.append("\n");
			}

		new TextWindow( "Ratio Analysis: "+imp.getTitle(), header.toString(), sb.toString(), 300, 400);


		}
}


