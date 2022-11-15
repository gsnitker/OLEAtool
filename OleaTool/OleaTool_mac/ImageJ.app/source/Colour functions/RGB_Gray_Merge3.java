import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

public class RGB_Gray_Merge3 implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack grey;
        static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	double scale = 0.2;
	/* Merges one, two or three 8-bit stacks into an RGB stack. */
	public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (GetStacks() && CheckStacks()) {
	                CombineStacks(delete);
		 	DisplayResult();
			CloseUsedStacks(delete);
		}
	}

	/** Combines four grayscale stacks into one RGB stack. */
	public boolean GetStacks() {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return false;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Gray-RGB Stack Merge");
		gd.addChoice("Gray Stack:", titles, titles[0]);
		gd.addChoice("Red Stack:", titles, titles[1]);
		String title3 = titles.length>2?titles[2]:none;
		gd.addChoice("Green Stack:", titles, title3);
		String title4 = titles.length>3?titles[3]:none;
		gd.addChoice("Blue Stack:", titles, title4);

		gd.addNumericField("Subtract scaled Fl from Grey", scale,3);
		gd.addCheckbox("Keep source stacks", true);

		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;
		for(int ii=0;ii<4;ii++) {
			int FILE=gd.getNextChoiceIndex();
			image[ii] = (FILE<wList.length)
					? WindowManager.getImage(wList[FILE])
					: null;
		}
		scale=gd.getNextNumber();
		delete = !gd.getNextBoolean();
		return true;
	}
	public boolean CheckStacks(){
		int stackSize, width, height, type, img=0;
		while (image[img]==null) img++;
		if(img>=4) IJ.error("an image must exist");
		d=stackSize = image[img].getStackSize();
		h=height = image[img].getHeight();
		w=width = image[img].getWidth();
		type = image[img].getType();
		if (stackSize <1 ) {
			IJ.error("require stackSize>0");
			return false;
		}
		if (type != ImagePlus.GRAY8) {
			IJ.error("require 8-bit grayscale");
			return false;
		}
		if (width <1 ) {
			IJ.error("require width>0");
			return false;
		}
		if (height <1 ) {
			IJ.error("require height>0");
			return false;
		}
		for (int ii=0; ii<4; ii++){
			if(image[ii]!=null){
				if(stackSize!=image[ii].getStackSize()) {
					IJ.error("stackSize mismatch");
					return false;
				}
				if(height!=image[ii].getHeight()){
					IJ.error("height mismatch");
					return false;
				}
				if(width!=image[ii].getWidth()) {
					IJ.error("width mismatch");
					return false;
				}
				if(type!=image[ii].getType()) {
					IJ.error("type mismatch");
					return false;
				}
			}
		}
		return true;
	}
	public void CombineStacks(boolean remove){
		rgb = new ImageStack(w, h);
		grey = new ImageStack(w, h);
		ImageProcessor ipBlank = new ByteProcessor(w,h);
		int inc = d/10; /* for showing progress */
		int numpels = w*h;
		if (inc<1) inc = 1;
		ColorProcessor cp;
		ImageProcessor ip, ipGr, ipR, ipG, ipB ;
		byte[] GS,rS,gS,bS; /* source pointers */
		byte[] rPels=new byte[w*h];	
		byte[] gPels=new byte[w*h];
		byte[] bPels=new byte[w*h];
		byte[] blank=new byte[w*h];
		byte[] grPels=new byte[w*h];
		int [] rgbG  = new int [3]; 
		int greyPix;
		for (int ss=1; ss<=d; ss++)
			 { /* i is the image slice among d slices per stack */
			cp = new ColorProcessor(w, h); /* MAY NEED TO DO THIS INSIDE STACK BUILDING LOOP */		
			ip = new ByteProcessor(w,h);
			
			
			ipGr = (image[G].getStack().getProcessor(ss)) ;


			if (image[r]!=null)
				ipR = (image[r].getStack().getProcessor(ss)) ;
			else 
				ipR=ip;
	
			if(image[g]!=null)
				ipG = (image[g].getStack().getProcessor(ss)) ;
			else 	
				ipG =ip;
			if(image[b]!=null)
				ipB = (image[b].getStack().getProcessor(ss)) ;
			else
				ipB = ip;
			


			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					ip.putPixel(x,y,0);
					greyPix = (int)(ipGr.getPixelValue(x,y)-(scale*(ipR.getPixelValue(x,y)+ipG.getPixelValue(x,y)+ipB.getPixelValue(x,y))));
					rgbG[0]=(int)(ipR.getPixelValue(x,y)+(double)greyPix);

					
					rgbG[1]=(int)(ipG.getPixelValue(x,y)+(double)greyPix);

					rgbG[2]=(int)(ipB.getPixelValue(x,y)+(double)greyPix);
					for (int i=0;i<3;i++)
						{
						if ((rgbG[i]>255)) rgbG[i]=254;
						if(rgbG[i]<0) rgbG[i]=0;
						}				
					cp.putPixel(x,y, rgbG);		
					}	
				}
	    				
			
				rgb.addSlice(null, cp);
				grey.addSlice(null, ip);

				if(remove)
				for(int ii=0; ii<4;ii++){
					if(image[ii]!=null)
						image[ii].getStack().deleteSlice(1);
				}
				if ((ss%inc) == 0) IJ.showProgress((double)ss/d);
			}
			IJ.showProgress(1.0);
		
	}

	public void CloseUsedStacks(boolean close){
	if (close)
		for (int i=0; i<4; i++) {
			if (image[i]!=null) {
				image[i].changes = false;
				ImageWindow win = image[i].getWindow();
				if (win!=null)
					win.close();
			}
		}
	}
	
	public void DisplayResult(){
		new ImagePlus("Color", rgb).show();
		//new ImagePlus("grey", grey).show();
	}
}



