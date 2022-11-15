import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Composite_RGB_Merge implements PlugIn {
	int nCh=4;
	ImagePlus[] imp= new ImagePlus[nCh];
	int w, h;
	String title;
	public void run(String arg) {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return ;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Merge RGB-Composite");
		
		gd.addChoice("Red Stack:", titles, titles[0]);
		String title2 = titles.length>2?titles[1]:none;
		gd.addChoice("Green Stack:", titles, title2);
		String title3 = titles.length>3?titles[2]:none;
		gd.addChoice("Blue Stack:", titles, title3);
		String title4 = titles.length>4?titles[3]:none;
		gd.addChoice("GreyStack:", titles, title4);

		gd.showDialog();
		
		if (gd.wasCanceled()) 
			return ;
		for(int ii=0;ii<nCh;ii++) {
			int FILE=gd.getNextChoiceIndex();
			if(FILE<wList.length)
				{imp[ii] =WindowManager.getImage(wList[FILE]);
				w = imp[ii].getWidth();
				h = imp[ii].getHeight();
				title = imp[ii].getTitle();
				}
			}	
		ImageStack img = new ImageStack (w,h);
		ImageProcessor iptemp;
		ImageProcessor ipBlank = new ByteProcessor (w,h);
		for (int s=0; s<nCh; s++)
			{
			iptemp = (imp[s]!=null) ?  imp[s].getProcessor() : ipBlank;
			img.addSlice(null, iptemp);
			}		
		ImagePlus impComp = new ImagePlus(title + " composite", img);
		new CompositeImage(impComp,1).show();
		}

}
