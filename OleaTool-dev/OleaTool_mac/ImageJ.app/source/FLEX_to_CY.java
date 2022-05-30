import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.measure.Calibration;

public class FLEX_to_CY implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack flexStack;
       	static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	double scale = 0.5;
	String title;
	int yellowSlice = 1;
	int cyanSlice = 0;
	int fredSlice =2;

//double rollingBall = Prefs.get("EI_rollingBall.double", 50);

	int yellMax=(int)Prefs.get("F2C_yellMax.int",500);
	int cyanMax=(int)Prefs.get("F2C_cyanMax.int",200);
	int fredmax=(int)Prefs.get("F2C_fredmax.int",300);
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
		fredSlice =2;
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
		gd.addNumericField("yellow_slice ", 7,0);
		gd.addNumericField("cyan_slice", 1,0 );
		gd.addNumericField("nCh", 7 , 0);
		gd.addNumericField("yellow_Max", yellMax,0);
		gd.addNumericField("cyan_Max", cyanMax,0);
	//	gd.addNumericField("fred_Max", fredmax, 0);
		gd.addCheckbox("Auto_Contrast", true);
		gd.showDialog();
        			if (gd.wasCanceled())
	            		return ;

		yellowSlice = (int)gd.getNextNumber();
		cyanSlice = (int)gd.getNextNumber();
		nCh= (int)gd.getNextChoiceIndex();
		yellMax = (int)gd.getNextNumber();
		cyanMax = (int)gd.getNextNumber();
		//fredmax = (int)gd.getNextNumber();
		boolean autoContrast = gd.getNextBoolean();
		Prefs.set("F2C_yellMax.int",(int)yellMax );
		Prefs.set("F2C_cyanMax.int",(int)cyanMax );
	//Prefs.set("F2C_fredmax.int",(int)fredmax );
	//IJ.showMessage(""+yellowSlice);
		ImageProcessor ipBlank;

		//count channels
	//	if(yellowSlice==4) nCh=nCh-1;
	//	if(cyanSlice==4) nCh=nCh-1;
	//	if(fredSlice==4) nCh=nCh-1;

		//check for slice error
		//if(nCh<2)
		//	{if(yellowSlice!=0&&cyanSlice!=0){
		//          IJ.error("Please select correct slice numbers. You need a slice 1.");
		//	return;}
		//	}
		if(bitDepth==8)
		ipBlank = new ByteProcessor(w,h);
		else
		ipBlank = new ShortProcessor(w,h);

		int inc = d/10; /* for showing progress */
		int numpels = w*h;
		if (inc<1) inc = 1;
		ColorProcessor cp;
		ImageProcessor ip, ipYel, ipCy ;
		String sliceLabel= flexStack.getSliceLabel(1);
		byte[] GS,rS,gS,bS; /* source pointers */
		byte[] yPels=new byte[w*h];
		byte[] cPels=new byte[w*h];
	//	byte[] fPels=new byte[w*h];
		int [] rgb  = new int [3];
		int greyPix;
		int n;
		scale = 0.5;

		ip = new ByteProcessor(w,h);
		n=0;

		if(yellowSlice<1)
			ipYel=ipBlank;
		else
			ipYel = flexStack.getProcessor(yellowSlice) ;
		if(cyanSlice<1)
			ipCy=ipBlank;
		else
			ipCy = flexStack.getProcessor(cyanSlice) ;

		for (int s=0; s<(flexStack.getSize()/nCh); s++)
			{cp = new ColorProcessor(w, h);

			ipYel = flexStack.getProcessor((nCh*s) - (nCh - yellowSlice)) ;

			ipCy = flexStack.getProcessor((nCh*s) - (nCh-cyanSlice)) ;

		//	if(fredSlice>2)
		//		ipFr=ipBlank;
		//	else
		//		ipFr = flexStack.getProcessor((nCh*s) - (nCh-fredSlice)+1);

			if(autoContrast)
				{
				if(bitDepth==16){
				yellMax = getMaxValue16(ipYel);
				cyanMax = getMaxValue16(ipCy);
		//		fredmax = getMaxValue16(ipFr);
				}

				else{yellMax = getMaxValue8(ipYel);
				cyanMax = getMaxValue8(ipCy);
		//		fredmax = getMaxValue8(ipFr);
				}

				if(yellMax<255) yellMax=255;
				if(cyanMax<255) cyanMax=255;
		//		if(fredmax<255) fredmax=255;

				if(yellMax==0) yellMax=1;
				if(cyanMax==0) cyanMax=1;
		//		if(fredmax==0) fredmax=1;

				}
			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					rgb[0]=(int)((ipYel.getPixelValue(x,y)/yellMax)*255);
					rgb[1]=(int)(((ipCy.getPixelValue(x,y)/cyanMax)+(ipYel.getPixelValue(x,y)/yellMax))*255);
					rgb[2]=(int)((ipCy.getPixelValue(x,y)/cyanMax)*255);
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



