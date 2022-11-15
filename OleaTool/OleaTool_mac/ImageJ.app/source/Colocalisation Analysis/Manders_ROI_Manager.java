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


public class Manders_ROI_Manager implements PlugInFilter, Measurements  {

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
	private int dualChannelIndex = (int)Prefs.get("ICP_channels.int",0);

public int setup(String arg, ImagePlus imp) 
	{
	this.imp = imp;
	return DOES_ALL+NO_CHANGES;
	}

public void run(ImageProcessor ip) 
	{int[] wList = WindowManager.getIDList();
	ImagePlus cimg = WindowManager.getCurrentImage();
    	int id = cimg!=null?cimg.getID():0;
	String[] titles = new String[wList.length];
	int currentID=0;
        	for (int i=0; i<wList.length; i++) 
		{
	           	ImagePlus imp = WindowManager.getImage(wList[i]);
	           	 if (imp!=null)
               		titles[i] = imp.getTitle();
			if(wList[i]== id) currentID=i;
            	else
			titles[i] = "";
       		}
	int index1 = currentID;
	int index2 = index1;
	int sel=0;
	String[] dualChannels=  { "Red : Green","Red : Blue", "Green : Blue"};
	GenericDialog gd = new GenericDialog("Mander's ROI Calculator");
       	gd.addChoice("Ch1", titles, titles[index1]);
	gd.addChoice("Ch2 ", titles, titles[index2]);
	gd.addChoice("Channel Combination", dualChannels, dualChannels[dualChannelIndex]);

	gd.showDialog();
        	if (gd.wasCanceled())
	            return ;
       	index1 = gd.getNextChoiceIndex();
	index2 = gd.getNextChoiceIndex();
	dualChannelIndex = gd.getNextChoiceIndex();

	ImagePlus imp1 = WindowManager.getImage(wList[index1]);
	ImagePlus imp2 = WindowManager.getImage(wList[index2]);
	String ch1title=imp1.getTitle();
	String ch2title=imp2.getTitle();

	ImageProcessor ip1 = imp1.getProcessor();

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
	
	for(sel=0;sel<roiCount;sel++)
		{
		rm.select(sel);
		ip1.setRoi(rois[sel]);
		//IJ.showMessage("pausing "+sel );
		imp1.updateAndDraw();
		IJ.run("Manders Coefficients", "Ch1=["+ ch1title+"] Ch2=["+ch2title+"] channel=["+ dualChannels[dualChannelIndex] +"] use=[Channel 1] use use exclude");
		}
	}
}
