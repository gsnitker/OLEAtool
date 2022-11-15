import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.Calibration;

public class RGB_Gray_Merge implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];
	ImageStack rgb; int w,h,d; boolean delete;
	ImageStack grey;
    static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	double scale = 0.5;
	String title;
	Calibration oc;
	double redMax, redMin, greenMax, greenMin,blueMax, blueMin, greyMax, greyMin=1;
	int nCh =0;
	static boolean blend= false;

	/* Merges one, two or three stacks into an RGB stack. */
	public void run(String arg) {
		if (arg.equals("about"))
			{showAbout(); return;}
		else{	
			imp = WindowManager.getCurrentImage();
			if (GetStacks() && CheckStacks()) {
	               CombineStacks(delete);
				DisplayResult();
				CloseUsedStacks(delete);
			}
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
		gd.addCheckbox("Blend?", blend);

		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;
		for(int ii=0;ii<4;ii++) {
			int FILE=gd.getNextChoiceIndex();
			image[ii] = (FILE<wList.length)
					? WindowManager.getImage(wList[FILE])
					: null;
		}
		oc = WindowManager.getImage(wList[0]).getCalibration().copy();
		scale=gd.getNextNumber();
		delete = !gd.getNextBoolean();
		blend= gd.getNextBoolean();
		return true;
	}
	public boolean CheckStacks(){
		int stackSize, width, height, type, img=0;
		while (image[img]==null) img++;
		if(img>=4) IJ.error("an image must exist");
		d=stackSize = image[img].getStackSize();
		title = image[0].getTitle();
		h=height = image[img].getHeight();
		w=width = image[img].getWidth();
		type = image[img].getType();
		if (stackSize <1 ) {
			IJ.error("require stackSize>0");
			return false;
		}
	//if (type != ImagePlus.GRAY8) {
			if(image[0]!=null)
				{
				greyMax = image[0].getProcessor().getMax();
				greyMin =  image[0].getProcessor().getMin();}

			if(image[1]!=null)
				{nCh++;
				redMax = image[1].getProcessor().getMax();
				redMin =  image[1].getProcessor().getMin();}
			if(image[2]!=null)
				{nCh++;
				greenMax = image[2].getProcessor().getMax();
				greenMin =  image[2].getProcessor().getMin();}
			if(image[3]!=null)
				{nCh++;
			blueMax = image[3].getProcessor().getMax();
			blueMin =  image[3].getProcessor().getMin();}
//			return false;
//}
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
		int greyPix, redPix, greenPix,bluePix, scaledPix;
		int n;
		int jScale = 1;

		if (blend) jScale = 2;
		
		for (int ss=1; ss<=d; ss++)
			 { /* i is the image slice among d slices per stack */
			cp = new ColorProcessor(w, h); /* MAY NEED TO DO THIS INSIDE STACK BUILDING LOOP */		
			ip = new ByteProcessor(w,h);
			n=0;
			ipGr = (image[G].getStack().getProcessor(ss)) ;
			if (image[r]!=null)
				{ipR = (image[r].getStack().getProcessor(ss)) ; n++;}
			else 
				ipR=ip;
	
			if(image[g]!=null)
				{ipG = (image[g].getStack().getProcessor(ss)) ; n++;}
			else 	
				ipG =ip;
			if(image[b]!=null)
				{ipB = (image[b].getStack().getProcessor(ss)) ; n++;}
			else
				ipB = ip;
			//scale=scale/nCh;
			for (int x=0; x<w; x++)
				{
				for (int y=0; y<h; y++)
					{
					//ip.putPixel(x,y,0);
					
					redPix = (int)((255*(ipR.getPixelValue(x,y)-redMin)/(redMax-redMin))/jScale);
					greenPix = (int)((255*(ipG.getPixelValue(x,y)-greenMin)/(greenMax-greenMin))/jScale);
					bluePix = (int)((255*(ipB.getPixelValue(x,y)-blueMin)/(blueMax-blueMin))/jScale);
					//scaledPix = 
					greyPix =  (int)((255*(ipGr.getPixelValue(x,y)-greyMin)/(greyMax-greyMin))/jScale);
					
					if(redPix<(int)0) redPix=0;
					if(greenPix<(int)0) greenPix=0;
					if(bluePix<(int)0) bluePix=0;
					
					if (!blend) greyPix=(int)(greyPix-(scale*(redPix+bluePix+greenPix)));
					
					
					
					rgbG[0]=(int)(redPix+(double)greyPix);
					rgbG[1]=(int)(greenPix+(double)greyPix);
					rgbG[2]=(int)(bluePix+(double)greyPix);
					for (int i=0;i<3;i++)
						{
						if ((rgbG[i]>255)) rgbG[i]=255;
						if(rgbG[i]<0) rgbG[i]=0;
						}				
					cp.putPixel(x,y, rgbG);		
					}	
				}
	    				
			
				rgb.addSlice(null, cp);

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
		new ImagePlus(title +" rgbG", rgb).show();
		//new ImagePlus("grey", grey).show();
	    WindowManager.getCurrentImage().setCalibration(oc);
        WindowManager.getCurrentImage().getWindow().repaint();
	}
	
	public void showAbout() {
		IJ.showMessage("RGB Gray merge Plugin", "Plugin merges fluorescence and transmitted light (grey) images.\n"
			+ "If 'blend' is selected then each new pixel has the intensity: \n"
			+ "   ((r+g+b)/2)+(grey/2)\n"
			+ "If 'blend' is not selected then the new pixel intensity is:\n"
			+ "   (grey-scale*(r+g+b))+(r+g+b)\n"
			+ "Where 'scale' is the user defined scaling factor."

			);
	}


}



