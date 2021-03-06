import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.measure.Calibration;

public class Opened_FLEX_to_RGB implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack flexStack;
       	static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	static int nCh=3;
	double scale = 0.5;
	String title;
	int redSlice = 1;
	int greenSlice = 0;
	int blueSlice =2;

//double rollingBall = Prefs.get("EI_rollingBall.double", 50);

	int redMax=(int)Prefs.get("F2C_redMax.int",500);
	int greenMax=(int)Prefs.get("F2C_greenMax.int",200);
	int blueMax=(int)Prefs.get("F2C_blueMax.int",300);
	boolean auto=Prefs.get("F2C_auto.boolean",false);
	int bitDepth;

	public void run(String arg) 
		{
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
		          IJ.error("No images are open.");
			return;}
		imp = WindowManager.getCurrentImage();
		bitDepth = imp.getBitDepth() ;
		String fileName = imp.getTitle();
		redSlice =0;
		greenSlice =1;
		blueSlice =2;
		w=imp.getWidth();
		h=imp.getHeight();
		title = imp.getTitle();
		int nCh=3;
		Calibration cal = imp.getCalibration();
		ImageStack rgbStack = new ImageStack(w, h);
		flexStack =imp.getStack();
		int slices = flexStack.getSize();
		String [] sliceList  = new String[nCh+2];
		
	for (int s=0; s<nCh+2; s++)
			{sliceList[s] = ""+ (s+1);
			if (s==nCh) sliceList [s] = "ignore";
			if (s==nCh+1) sliceList [s] = "none";}
		


		GenericDialog gd = new GenericDialog("FLEX RGB merge");
		gd.addNumericField("red_slice ", 1,0);
		gd.addNumericField("green_slice", 2,0);
		gd.addNumericField("blue_slice", 3,0);
		gd.addNumericField("Number of channels", nCh, 0);
		gd.addNumericField("red_Max", redMax, 0);
		gd.addNumericField("green_Max", greenMax, 0);
		gd.addNumericField("blue_Max", blueMax, 0);
		gd.addCheckbox("Auto_Contrast", false);
		gd.showDialog();
        			if (gd.wasCanceled())
	            		return ;

     	  	redSlice = (int)gd.getNextNumber();
       		greenSlice = (int)gd.getNextNumber();
	       	blueSlice = (int)gd.getNextNumber();
		nCh= (int)gd.getNextNumber();
		redMax = (int)gd.getNextNumber();
		greenMax = (int)gd.getNextNumber();
		blueMax = (int)gd.getNextNumber();
		boolean autoContrast = gd.getNextBoolean();
		Prefs.set("F2C_redMax.int",(int)redMax );
		Prefs.set("F2C_greenMax.int",(int)greenMax );
		Prefs.set("F2C_blueMax.int",(int)blueMax );
		Prefs.set("F2C_auto.boolean",auto);	
	//IJ.showMessage(""+redSlice);
		ImageProcessor ipBlank;

		//count channels
	//	if(redSlice==4) nCh=nCh-1;
	//	if(greenSlice==4) nCh=nCh-1;
	//	if(blueSlice==4) nCh=nCh-1;

		//check for slice error
		if(nCh<3)
			{if(blueSlice!=0&&redSlice!=0&&greenSlice!=0){
		          IJ.error("Please select correct slice numbers. You need a slice 1.");
			return;}
			}
		if(bitDepth==8)
		ipBlank = new ByteProcessor(w,h);
		else
		ipBlank = new ShortProcessor(w,h);

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

		if(redSlice==0)
			ipRd=ipBlank;
		else	
			ipRd = flexStack.getProcessor(redSlice) ;
		if(greenSlice==0)
			ipGrn=ipBlank;
		else	
			ipGrn = flexStack.getProcessor(greenSlice) ;
		if(blueSlice==0)
			ipBlu=ipBlank;
		else	
			ipBlu = flexStack.getProcessor(blueSlice) ;


		for (int s=1; s<=(flexStack.getSize()/nCh); s++)
			{cp = new ColorProcessor(w, h);
			
			if(redSlice==0)
				ipRd=ipBlank;
			else
				ipRd = flexStack.getProcessor((nCh*s) - (nCh - redSlice)) ;
					
			if(greenSlice==0)
				ipGrn=ipBlank;
			else
				ipGrn = flexStack.getProcessor((nCh*s) - (nCh-greenSlice)) ;
					
			if(blueSlice==0)
				ipBlu=ipBlank;
			else
				ipBlu = flexStack.getProcessor((nCh*s) - (nCh-blueSlice));	

			if(autoContrast)
				{
				if(bitDepth==16){
				redMax = getMaxValue16(ipRd);
				//IJ.write("red max "+ redMax);
				greenMax = getMaxValue16(ipGrn);	
				blueMax = getMaxValue16(ipBlu);}

				else{redMax = getMaxValue8(ipRd);
				greenMax = getMaxValue8(ipGrn);	
				blueMax = getMaxValue8(ipBlu);}
		
				if(redMax<255) redMax=255;
				if(greenMax<255) greenMax=255;
				if(blueMax<255) blueMax=255;
	
				if(redMax==0) redMax=1;
				if(greenMax==0) greenMax=1;
				if(blueMax==0) blueMax=1;

				}
			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					rgb[0]=(int)(((float)(ipRd.getPixelValue(x,y))/(float)redMax)*255);
					rgb[1]=(int)((ipGrn.getPixelValue(x,y)/greenMax)*255);
					rgb[2]=(int)((ipBlu.getPixelValue(x,y)/blueMax)*255);	
					for(int p=0; p<3; p++)
						{if (rgb[p]>255) rgb[p]=255;}
					
					cp.putPixel(x,y, rgb);		
					}	
				}
			rgbStack.addSlice(null, cp);
    			}		

	new ImagePlus("RGB"+fileName, rgbStack).show();
	WindowManager.getCurrentImage().setCalibration(cal);
	WindowManager.getCurrentImage().updateAndRepaintWindow();
	}


	int getMaxValue16(ImageProcessor ip)
		{
		short [] pixels =(short[]) ip.getPixels();
		int max=0;
		for(int i=0; i<pixels.length; i++)
			{if((int)pixels[i]>max) max=(int)pixels[i];
			}
		return max;
		}

	int getMaxValue8(ImageProcessor ip)
		{
		byte [] pixels =(byte[]) ip.getPixels();
		int max=0;
		for(int i=0; i<pixels.length; i++)
			{if((int)pixels[i]>max) max=(int)pixels[i];
			}
		return max;
		}
}



