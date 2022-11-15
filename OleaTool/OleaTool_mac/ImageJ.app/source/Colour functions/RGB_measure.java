import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class RGB_measure implements PlugIn {

	public void run(String arg) {
		if (IJ.getImage().getBitDepth()!=24)
			{IJ.showMessage("RGB Measure", "RGB Image required");}
	
		
		ImagePlus imp = WindowManager.getCurrentImage();	
		ImageProcessor ip = imp.getProcessor();
		int w = imp.getWidth();
		int h = imp.getHeight();
		ColorProcessor cp = new ColorProcessor(w, h);
		ImageProcessor mask = imp.getMask();
		Rectangle r = imp.getRoi().getBoundingRect();
		
		cp.setRoi(r);
		cp.setMask(mask);
		
		


	}

}
