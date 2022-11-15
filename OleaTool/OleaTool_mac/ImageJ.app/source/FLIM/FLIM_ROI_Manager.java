import java.text.DecimalFormat; 
import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;
import ij.plugin.PlugIn;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;

public class FLIM_ROI_Manager implements PlugIn, Measurements  {
             ImagePlus imp;
             RoiManager rm;

     public void run(String arg) {
             ImagePlus imp = IJ.getImage();
             ImageStack img = imp.getStack();
             if(img.getSize()<2) {
                  IJ.showMessage("Command requires a stack");
                  return;
             }
             RoiManager rm = RoiManager.getInstance();
             if (rm==null)
                 {IJ.error("ROI Manager is not open"); return;}
             Roi[] rois = rm.getSelectedRoisAsArray();
             for (int i=0; i<rois.length; i++) {
                 String name = rois[i].getName();
                 IJ.log(i+"  "+name+" "+rm.getSliceNumber(name));
             }
     }

}
