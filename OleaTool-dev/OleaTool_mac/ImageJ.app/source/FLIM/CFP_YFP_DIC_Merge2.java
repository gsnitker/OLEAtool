import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

public class CFP_YFP_DIC_Merge2 implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack greyStack;
       	static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	double scale = 0.5;
	String title;
	int greySlice = (int)Prefs.get("CYG_greySlice.int",1);
	int cyanSlice = (int)Prefs.get("CYG_cyanSlice.int",1);
	int yellowSlice = (int)Prefs.get("CYG_yellowSlice.int",2);

	/* Merges one, two or three 8-bit stacks into an RGB stack. */
	public void run(String arg) 
		{
		imp = WindowManager.getCurrentImage();
		
		

		w=imp.getWidth();
		h=imp.getHeight();
		title = imp.getTitle();
		Calibration cal = imp.getCalibration();
		ImageStack rgb = new ImageStack(w, h);
		greyStack =imp.getStack();
		int slices = greyStack.getSize();


//greySlice=1;

	
	String [] sliceList  = new String[slices];
	
	for (int s=0; s<slices; s++)
		sliceList[s] =""+ (s+1);

 //error check for slices

	
	GenericDialog gd = new GenericDialog("CYG merge");
	gd.addChoice("Grey", sliceList  , sliceList [greySlice]);
	gd.addChoice("Cyan", sliceList  , sliceList [cyanSlice]);
	gd.addChoice("Yellow", sliceList  , sliceList [yellowSlice]);
	gd.showDialog();
        	if (gd.wasCanceled())
	            return ;

       	greySlice = gd.getNextChoiceIndex();
       	cyanSlice = gd.getNextChoiceIndex();
       	yellowSlice = gd.getNextChoiceIndex();

	Prefs.set("CYG_greySlice.int",greySlice);
	Prefs.set("CYG_cyanSlice.int",cyanSlice);
	Prefs.set("CYG_yellowSlice.int",yellowSlice);

		ImageProcessor ipBlank = new ByteProcessor(w,h);
		int inc = d/10; /* for showing progress */
		int numpels = w*h;
		if (inc<1) inc = 1;
		ColorProcessor cp;
		ImageProcessor ip, ipGr, ipCyan, ipYellow ;
		String sliceLabel= greyStack.getSliceLabel(1);
		byte[] GS,rS,gS,bS; /* source pointers */
		byte[] rPels=new byte[w*h];	
		byte[] gPels=new byte[w*h];
		byte[] bPels=new byte[w*h];
		byte[] blank=new byte[w*h];
		byte[] grPels=new byte[w*h];
		int [] rgbG  = new int [3]; 
		int greyPix;
		int n;
		scale = 0.5;
		cp = new ColorProcessor(w, h); /* MAY NEED TO DO THIS INSIDE STACK BUILDING LOOP */		
		ip = new ByteProcessor(w,h);
		n=0;

		ipGr = greyStack.getProcessor(greySlice+1) ;
		ipCyan = greyStack.getProcessor(cyanSlice+1) ;
		ipYellow = greyStack.getProcessor(yellowSlice+1);


	int greyMin=255;
	int greyMax=0;	
		for (int x=0; x<w; x++)
			{for (int y=0; y<h; y++)
				{greyPix = (int)(ipGr.getPixelValue(x,y));
				if (greyPix<greyMin) greyMin=greyPix;
				if (greyPix>greyMax) greyMax=greyPix;

				}
			}

int picMin=0;
int picMax=255;
		for (int x=0; x<w; x++)
			{
			for (int y=0; y<h; y++)
				{
				//ip.putPixel(x,y,0);
				greyPix=(int) ipGr.getPixelValue(x,y);
			//	greyPix = (int)(((((double)greyPix-(double)greyMin)/((double)greyMax-(double)greyMin))*(double)picMax)+(double)picMin);

				greyPix = (int)(greyPix-(scale*(ipCyan.getPixelValue(x,y)+ipYellow.getPixelValue(x,y))));
								
//				greyPix = (int)(greyPix+(picMin-greyMin)/(greyMax/picMax))));
				if(greyPix<0) greyPix=28;
				rgbG[0]=(int)(ipYellow.getPixelValue(x,y)+(double)greyPix);
				rgbG[1]=(int)(ipCyan.getPixelValue(x,y)+ipYellow.getPixelValue(x,y)+(double)greyPix);
				rgbG[2]=(int)(ipCyan.getPixelValue(x,y)+(double)greyPix);

				for (int i=0;i<3;i++)
					{
					if ((rgbG[i]>255)) rgbG[i]=254;
					if(rgbG[i]<0) rgbG[i]=0;
					}				
				cp.putPixel(x,y, rgbG);		
				}	
			}
    				
		
	rgb.addSlice(null, cp);
	rgb.setSliceLabel(sliceLabel,1);				
		
	IJ.showProgress(1.0);
	new ImagePlus(sliceLabel, rgb).show();
WindowManager.getCurrentImage().setCalibration(cal);
	WindowManager.getCurrentImage().updateAndRepaintWindow();
	}

}



