import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.measure.Calibration;

public class Opened_FLEX_to_CY implements PlugIn {

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
	int yellowSlice = 1;
	int cyanSlice = 0;
	int blueSlice =2;

//double rollingBall = Prefs.get("EI_rollingBall.double", 50);

	int yellowMax=(int)Prefs.get("F2C_yellowMax.int",500);
	int cyanMax=(int)Prefs.get("F2C_cyanMax.int",200);
	int blueMax=(int)Prefs.get("F2C_blueMax.int",300);
	boolean autoContrast=Prefs.get("F2C_auto.boolean",false);
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
		yellowSlice =0;
		cyanSlice =1;
		blueSlice =2;
		w=imp.getWidth();
		h=imp.getHeight();
		title = imp.getTitle();
		int nCh=7;
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
		gd.addNumericField("yellow_slice ", 7,0);
		gd.addNumericField("cyan_slice", 1,0);
	//	gd.addNumericField("blue_slice", 0,0);
		gd.addNumericField("Number of channels", nCh, 0);
		gd.addNumericField("yellow_Max", yellowMax, 0);
		gd.addNumericField("cyan_Max", cyanMax, 0);
	//	gd.addNumericField("blue_Max", blueMax, 0);
		gd.addCheckbox("Auto_Contrast", autoContrast);
		gd.showDialog();
        			if (gd.wasCanceled())
	            		return ;

     	  	yellowSlice = (int)gd.getNextNumber();
       		cyanSlice = (int)gd.getNextNumber();
	 //      	blueSlice = (int)gd.getNextNumber();
		nCh= (int)gd.getNextNumber();
		yellowMax = (int)gd.getNextNumber();
		cyanMax = (int)gd.getNextNumber();
	//	blueMax = (int)gd.getNextNumber();
		boolean autoContrast = gd.getNextBoolean();
		Prefs.set("F2C_yellowMax.int",(int)yellowMax );
		Prefs.set("F2C_cyanMax.int",(int)cyanMax );
	//	Prefs.set("F2C_blueMax.int",(int)blueMax );
		Prefs.set("F2C_auto.boolean",autoContrast);
	//IJ.showMessage(""+yellowSlice);
		ImageProcessor ipBlank;

		//count channels
	//	if(yellowSlice==4) nCh=nCh-1;
	//	if(cyanSlice==4) nCh=nCh-1;
	//	if(blueSlice==4) nCh=nCh-1;

		//check for slice error
		if(nCh<3)
			{if(yellowSlice!=0&&cyanSlice!=0){
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
		ImageProcessor ip, ipYl, ipCy, ipYel ;
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

		if(yellowSlice==0)
			ipYl=ipBlank;
		else
			ipYl = flexStack.getProcessor(yellowSlice) ;

		if(cyanSlice==0)
			ipCy=ipBlank;
		else
			ipCy = flexStack.getProcessor(cyanSlice) ;

	//	if(blueSlice==0)
	//		ipYel=ipBlank;
	//	else
	//		ipYel = flexStack.getProcessor(blueSlice) ;


		for (int s=1; s<=(flexStack.getSize()/nCh); s++)
			{cp = new ColorProcessor(w, h);

			if(yellowSlice==0)
				ipYl=ipBlank;
			else
				ipYl = flexStack.getProcessor((nCh*s) - (nCh - yellowSlice)) ;

			if(cyanSlice==0)
				ipCy=ipBlank;
			else
				ipCy = flexStack.getProcessor((nCh*s) - (nCh-cyanSlice)) ;

			//if(blueSlice==0)
		//		ipYel=ipBlank;
		//	else
		//		ipYel = flexStack.getProcessor((nCh*s) - (nCh-blueSlice));

			if(autoContrast)
				{
				if(bitDepth==16)
					{
					yellowMax = getMaxValue16(ipYl);
					//IJ.write("yellow max "+ yellowMax);
					cyanMax = getMaxValue16(ipCy);
			//		blueMax = getMaxValue16(ipYel);
					}

				else
					{yellowMax = getMaxValue8(ipYl);
					cyanMax = getMaxValue8(ipCy);
		//		blueMax = getMaxValue8(ipYel);
					}

				if(yellowMax<255) yellowMax=255;
				if(cyanMax<255) cyanMax=255;
		//		if(blueMax<255) blueMax=255;

				if(yellowMax==0) yellowMax=1;
				if(cyanMax==0) cyanMax=1;
			//	if(blueMax==0) blueMax=1;
				}

			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					rgb[2]=(int)(float)((ipCy.getPixelValue(x,y)/(float)cyanMax)*255);
					rgb[1]=(int)(float)((ipCy.getPixelValue(x,y)/(float)cyanMax)*255)+(int)((float)(ipYl.getPixelValue(x,y)/(float)yellowMax)*255);
					//rgb[1]=(int)(((float)ipCy.getPixelValue(x,y)/(float)cyanMax)+((float)ipYel.getPixelValue(x,y)/(float)yellowMax)*255);
					rgb[0]=(int)((float)(ipYl.getPixelValue(x,y)/(float)yellowMax)*255);
					for(int p=0; p<3; p++)
						{if (rgb[p]>255) rgb[p]=255;}
						cp.putPixel(x,y, rgb);
						}
				}
			rgbStack.addSlice(null, cp);
    		}

	new ImagePlus("CY_"+fileName, rgbStack).show();
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



