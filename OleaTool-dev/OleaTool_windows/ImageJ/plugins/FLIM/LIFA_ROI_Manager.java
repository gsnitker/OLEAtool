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


public class LIFA_ROI_Manager implements PlugInFilter, Measurements  {

	RoiManager roiManager;
	static boolean headingsSetLIFA;
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
		String title = imp.getTitle();
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
		//header.append(units+"\t");
		ImageStack stack = imp.getStack();
		int firstSlice=imp.getCurrentSlice()-2;
		int size = stack.getSize();
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

		header.append("File\tSample\tPhase \tMod\tDC Int");

		header.append("\n");
		
		//sb.append(header);

		ImageProcessor ip2 = stack.getProcessor(firstSlice);	
		ImageProcessor ip3 = stack.getProcessor(firstSlice+1);	
		ImageProcessor ip4 = stack.getProcessor(firstSlice+2);	
		for(sel=0;sel<roiCount;sel++)
			{
			//IJ.showProgress(i, size*2);			
			ip2.setRoi(rois[sel]);
			ip3.setRoi(rois[sel]);
			ip4.setRoi(rois[sel]);
			ImageStatistics stats1 = ImageStatistics.getStatistics(ip2, measurements, cal);
			ImageStatistics stats2 = ImageStatistics.getStatistics(ip3, measurements, cal);
			ImageStatistics stats3 = ImageStatistics.getStatistics(ip4, measurements, cal);
			
			//rt.incrementCounter();
			//rt.addLabel("file", title);
			//rt.addValue("Phase", (float)stats1.mean);	
			//rt.addValue("Mod", (float)stats2.mean);
			//rt.addValue("int", (float)stats3.mean);
	
			sb.append(imp.getTitle());
			sb.append("\t"+firstSlice+"\t"+(float)stats1.mean+"\t"+(float)stats2.mean+"\t"+(float)stats3.mean);
			sb.append("\n");
			}

		ResultsTable rt=Analyzer.getResultsTable();

		IJ.log(""+Analyzer.getResultsTable().getColumnHeadings());
		IJ.log(""+header.toString());
		

		if(Analyzer.getResultsTable().getColumnHeadings()!=header.toString())
			{
			IJ.setColumnHeadings(header.toString());
			
			}

		IJ.write(sb.toString());

	}
}


