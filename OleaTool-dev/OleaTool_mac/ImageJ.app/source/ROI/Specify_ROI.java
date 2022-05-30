import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.measure.Calibration;
/**
 *      Enhancing the original program created by Jeffrey Kuhn. This one takes,
 *      in addition to width and height and the option to have an oval ROI from 
 *      the original program, x & y coordinates, slice number, and the option to have
 *      the x & y coordinates centered or in default top left corner of ROI.
 *      The original creator is Jeffrey Kuhn, The University of Texas at Austin,
 *	jkuhn@ccwf.cc.utexas.edu
 *
 *      @author Anthony Padua
 *      @author Duke University Medical Center, Department of Radiology
 *      @author padua001@mc.duke.edu
 *      
 */

public class Specify_ROI implements PlugInFilter {
    int             iX;
    int             iY;
    int             iXROI;
    int             iYROI;
    int             iSlice;
    int             iWidth;
    int             iHeight;
double xPixelSize;
double yPixelSize;
boolean iUnits = true;
    boolean         bAbort;
    ImagePlus       imp;
    static boolean  oval;
    static boolean  centered;

    /**
     *	Called by ImageJ when the filter is loaded
     */
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        if (arg.equals("about"))
                {showAbout(); return DONE;}
        return DOES_ALL+NO_CHANGES;
    }

    /**
     *	Called by ImageJ to process the image
     */
    public void run(ImageProcessor ip) {
        bAbort = false;
        Rectangle r = ip.getRoi();
        iWidth = r.width;
        iHeight = r.height;
        iXROI = r.x;
        iYROI = r.y;
        iSlice = imp.getCurrentSlice();
       Calibration oc = imp.getCalibration();
        getROI();
        if (bAbort)
            return;
xPixelSize = oc.pixelWidth;
yPixelSize = oc.pixelHeight;
           iX = iXROI;
            iY = iYROI;

if (iUnits==true){
          double dbleiX = ((double)iWidth/xPixelSize);
            double dbleiY = ((double)iHeight/yPixelSize);
	 iWidth= (int)dbleiX ;
            iHeight= (int)dbleiY ;
}

        
        
              imp.setRoi(iX, iY, iWidth, iHeight);

           imp.setSlice(iSlice);
           imp.repaintWindow();
        

        IJ.register(Specify_ROI.class);
    }

    /**
     *	Creates a dialog box, allowing the user to enter the requested
     *	width, height, x & y coordinates, slice number for a Region Of Interest,
     *  option for oval, and option for whether x & y coordinates to be centered.
     */
    void getROI() {

String[] dimensions = {"Pixels","Units"};

        GenericDialog gd = new GenericDialog("Specify ROI", IJ.getInstance());
gd.addCheckbox("ROI size in �m?", iUnits);
        gd.addNumericField("Width:", iWidth, 0);
        gd.addNumericField("Height:", iHeight, 0);

        gd.addNumericField("X Coordinate:", iXROI, 0);
        gd.addNumericField("Y Coordinate:", iYROI, 0);
        gd.addNumericField("Slice:", iSlice, 0);
        gd.addCheckbox("Oval", oval);
        gd.addCheckbox("Centered",centered);

        gd.showDialog();

        if (gd.wasCanceled()) {
            bAbort = true;
            return;
        }
iUnits=gd.getNextBoolean();
        iWidth = (int) gd.getNextNumber();
        iHeight = (int) gd.getNextNumber();

        iXROI = (int) gd.getNextNumber();	
        iYROI = (int) gd.getNextNumber();	
        iSlice = (int) gd.getNextNumber();  
        oval = gd.getNextBoolean();
        centered = gd.getNextBoolean();
    }

    /**
     *	Displays a short message describing the filter
     */
    void showAbout() {
        IJ.showMessage("About ROISelect_...",
                "This sample allows a ROI of a specific size to be specified.\n"
        );
    }
}
