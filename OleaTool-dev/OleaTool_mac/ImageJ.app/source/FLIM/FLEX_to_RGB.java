import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

public class FLEX_to_RGB implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack flexStack;
       	static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	double scale = 0.5;
	String title;
	int redSlice = (int)Prefs.get("F2R_redSlice.int",1);
	int greenSlice = (int)Prefs.get("F2R_greenSlice.int",2);
	int blueSlice = (int)Prefs.get("F2R_blueSlice.int",2);

	/* Merges one, two or three 8-bit stacks into an RGB stack. */
	public void run(String arg) 
		{
		imp = WindowManager.getCurrentImage();
		redSlice =1;
		greenSlice = 0;
		blueSlice =2;
		w=imp.getWidth();
		h=imp.getHeight();
		title = imp.getTitle();
		int nCh=3;
		Calibration cal = imp.getCalibration();
		ImageStack rgbStack = new ImageStack(w, h);
		flexStack =imp.getStack();
		int slices = flexStack.getSize();
		String [] sliceList  = new String[slices];
		for (int s=0; s<nCh; s++)
			sliceList[s] = ""+ s;

		GenericDialog gd = new GenericDialog("FLEX RGB merge");
		gd.addChoice("red", sliceList  , sliceList [redSlice]);
		gd.addChoice("green", sliceList  , sliceList [greenSlice]);
		gd.addChoice("blue", sliceList  , sliceList [blueSlice]);
		gd.showDialog();
        			if (gd.wasCanceled())
	            		return ;

     	  	redSlice = gd.getNextChoiceIndex();
       		greenSlice = gd.getNextChoiceIndex();
	       	blueSlice = gd.getNextChoiceIndex();
		Prefs.set("F2R_redSlice.int",redSlice);
		Prefs.set("F2R_grenSlice.int",greenSlice);
		Prefs.set("F2R_blueSlice.int",blueSlice);

		ImageProcessor ipBlank = new ByteProcessor(w,h);
		int inc = d/10; /* for showing progress */
		int numpels = w*h;
		if (inc<1) inc = 1;
		ColorProcessor cp;
		ImageProcessor ip, ipRd, ipGrn, ipBlu ;
		String sliceLabel= flexStack.getSliceLabel(1);
		byte[] GS,rS,gS,bS; /* source pointers */
		byte[] rPels=new byte[w*h];	
		byte[] gPels=new byte[w*h];
		byte[] bPels=new byte[w*h];
		int [] rgb  = new int [3]; 
		int greyPix;
		int n;
		scale = 0.5;
			
		ip = new ByteProcessor(w,h);
		n=0;

		ipRd = flexStack.getProcessor(redSlice+1) ;
		ipGrn = flexStack.getProcessor(greenSlice+1) ;
		ipBlu = flexStack.getProcessor(blueSlice+1);
		

		for (int s=1; s<=(flexStack.getSize()/nCh); s++)
			{cp = new ColorProcessor(w, h);

			ipRd = flexStack.getProcessor((nCh*s) - (nCh - redSlice+1)) ;
			ipGrn = flexStack.getProcessor((nCh*s) - (nCh-greenSlice+1)) ;
			ipBlu = flexStack.getProcessor((nCh*s) - (nCh-blueSlice+1));	
//
//	set(ch2=_["image"&(nCh*f-2)])
//	set(ch1=_["image"&(nCh*f-1)])
//	set(ch3=_["image"&(nCh*f)])		

			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					rgb[0]=(int)(ipRd.getPixelValue(x,y));
					rgb[1]=(int)(ipGrn.getPixelValue(x,y));
					rgb[2]=(int)(ipBlu.getPixelValue(x,y));			
					cp.putPixel(x,y, rgb);		
					}	
				}
			rgbStack.addSlice(null, cp);
    			}		
		
	
	//rgbStack.setSliceLabel(sliceLabel,1);				
		
	//IJ.showProgress(1.0);
	new ImagePlus(sliceLabel, rgbStack).show();
	WindowManager.getCurrentImage().setCalibration(cal);
	WindowManager.getCurrentImage().updateAndRepaintWindow();
	}

}



