import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.PlugIn;

public class Whiten_background implements PlugIn {
	static double threshold = Prefs.get("WBG_threshold", 25);
	static double grey = Prefs.get("WBG_grey", 128);

	public void run(String arg) {
		
		double nwhite=0, BGred=0, BGgreen=0, BGblue=0;
		int p=0;
		double tred, tgreen, tblue;
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack img = imp.getStack();
		ImageProcessor ip = imp.getProcessor();	
		int nslices = imp.getStackSize();

		

		GenericDialog gd = new GenericDialog("Whiten Background");
		gd.addNumericField("Background cutoff", threshold,0);
		gd.addNumericField("New background shade", grey,0);

		gd.showDialog();
		if (gd.wasCanceled()) 
				return;

		threshold =gd.getNextNumber();
		grey= gd.getNextNumber();
	
		Prefs.set("WBG_threshold.double", threshold);
		Prefs.set("WBG_grey.double", grey);

		for(int s=1; s<=nslices; s++)
		{ip = img.getProcessor(s);
	
			for(int y=0;y<ip.getHeight();y++){
				for (int x=0;x<ip.getWidth();x++){
					IJ.showStatus("Whitening background. 'Esc' to abort."  );	
					if (IJ.escapePressed()) 
						{IJ.beep();  return;}
					p=ip.getPixel(x,y);
					tred=((p & 0xff0000)>> 16);
					tgreen=((p & 0x00ff00) >> 8);
					tblue=(p & 0x0000ff);
	
					
					if ((tred<=threshold)&&(tgreen<=threshold)&&(tblue<=threshold))
						{ tred=grey;
					  tgreen=grey;
					 tblue=grey;
						}
					

					
					ip.putPixel(x,y, (((int)tred & 0xff) <<16)+ (((int)tgreen & 0xff) <<8) + ((int)tblue & 0xff));
				}
			}
		}

	imp.updateAndDraw();
	IJ.showStatus("Done");
	}

}
