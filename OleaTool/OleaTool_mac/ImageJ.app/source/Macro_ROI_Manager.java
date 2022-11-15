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
import java.awt.event.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.plugin.filter.*;
import ij.io.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;



public class Macro_ROI_Manager implements PlugInFilter, Measurements  {

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
	{
	int[] wList = WindowManager.getIDList();	
	String[] titles = new String[wList.length];
	int currentID=0;
int sel=0;
	int id = imp!=null?imp.getID():0;
        	for (int i=0; i<wList.length; i++) 
		{
	           	ImagePlus imp = WindowManager.getImage(wList[i]);
	           	 if (imp!=null)
               		titles[i] = imp.getTitle();
			if(wList[i]== id) currentID=i;
            	else
			titles[i] = "";
       		}
	ImagePlus imp= WindowManager.getCurrentImage();

	String path = null;
	OpenDialog od = new OpenDialog("Run Macro...", path);
            String directory = od.getDirectory();
            String name = od.getFileName();

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
		ip.setRoi(rois[sel]);
		imp.updateAndDraw();
	
	           runMacroFile(directory+name, null);
    	
	}
}
 public String runMacroFile(String name, String arg) {
        if (name.startsWith("ij.jar:"))
            return runMacroFromIJJar(name, arg);
        if (name.indexOf(".")==-1) name = name + ".txt";
        String name2 = name;
        boolean fullPath = name.startsWith("/") || name.startsWith("\\") || name.indexOf(":\\")==1;
        if (!fullPath) {
            String macrosDir = Menus.getMacrosPath();
            if (macrosDir!=null)
                name2 = Menus.getMacrosPath() + name;
        }
        File file = new File(name2);
        int size = (int)file.length();
        if (size<=0 && !fullPath && name2.endsWith(".txt")) {
            String name3 = name2.substring(0, name2.length()-4)+".ijm";
            file = new File(name3);
            size = (int)file.length();
            if (size>0) name2 = name3;
        }
        if (size<=0 && !fullPath) {
            file = new File(System.getProperty("user.dir") + File.separator + name);
            size = (int)file.length();
            //IJ.log("runMacroFile: "+file.getAbsolutePath()+"  "+name+"  "+size);
        }
        if (size<=0) {
            IJ.error("RunMacro", "Macro file not found:\n \n"+name2);
            return null;
        }
        try {
            byte[] buffer = new byte[size];
            FileInputStream in = new FileInputStream(file);
            in.read(buffer, 0, size);
            String macro = new String(buffer, 0, size, "ISO8859_1");
            in.close();
            return runMacro(macro, arg);
        }
        catch (Exception e) {
            IJ.error(e.getMessage());
            return null;
        }
    }

    /** Opens and runs the specified macro on the current thread. Macros can
        retrieve the optional string argument by calling the getArgument() macro function. 
        Returns the String value returned by the macro or null if the macro does not
        return a value. */
    public String runMacro(String macro, String arg) {
        try {
            Interpreter interp = new Interpreter();
            return interp.run(macro, arg);
        } catch(Throwable e) {
            Interpreter.abort();
            IJ.showStatus("");
            IJ.showProgress(1.0);
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp!=null) imp.unlock();
            String msg = e.getMessage();
            if (e instanceof RuntimeException && msg!=null && e.getMessage().equals(Macro.MACRO_CANCELED))
                return null;
            CharArrayWriter caw = new CharArrayWriter();
            PrintWriter pw = new PrintWriter(caw);
            e.printStackTrace(pw);
            String s = caw.toString();
            if (IJ.isMacintosh())
                s = Tools.fixNewLines(s);
            //Don't show exceptions resulting from window being closed
            if (!(s.indexOf("NullPointerException")>=0 && s.indexOf("ij.process")>=0)) {
                if (IJ.getInstance()!=null)
                    new ij.text.TextWindow("Exception", s, 350, 250);
                else
                    IJ.log(s);
            }
        }
        return null;
    }
    
    public String runMacroFromIJJar(String name, String arg) {
        ImageJ ij = IJ.getInstance();
        if (ij==null) return null;
        name = name.substring(7);
        String macro = null;
        try {
            InputStream is = ij.getClass().getResourceAsStream("/macros/"+name+".txt");
            //IJ.log(is+"  "+("/macros/"+name+".txt"));
            if (is==null)
                return runMacroFile(name, arg);
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer sb = new StringBuffer();
            char [] b = new char [8192];
            int n;
            while ((n = isr.read(b)) > 0)
                sb.append(b,0, n);
            macro = sb.toString();
        }
        catch (IOException e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = "" + e;   
            IJ.showMessage("Macro Runner", msg);
        }
        if (macro!=null)
            return runMacro(macro, arg);
        else
            return null;
    }

}
