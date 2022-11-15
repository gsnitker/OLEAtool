import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.*;

/** Splits an RGB image or stack into three 8-bit grayscale images or stacks. */
//Mofified to split single slice RGB to a 3 slice stack

public class RGB_to_Montage implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
        splitStack(imp);
    }

    public void splitStack(ImagePlus imp) {


         	int w = imp.getWidth();
         	int h = imp.getHeight();
            ImageStack rgbStack = imp.getStack();
	ColorProcessor cp = (ColorProcessor)rgbStack.getProcessor(1);
      	Calibration cal = imp.getCalibration();
	double scale= cal.pixelWidth*(w/10);
	ImageStack splitStack = new ImageStack(w,h);
	//processors for each channel
	ColorProcessor rcp= new ColorProcessor (w,h);
	ColorProcessor gcp= new ColorProcessor (w,h);
	ColorProcessor bcp= new ColorProcessor (w,h);
	



  	//byte arrays to pull each channel from original image
	byte[] r,g,b;
             	r = new byte[w*h];
             	g = new byte[w*h];
             	b = new byte[w*h];
             cp = (ColorProcessor)rgbStack.getProcessor(1);
	cp.getRGB(r,g,b);
	//create image processors from byte arrays
            ImageProcessor rip = new ByteProcessor(w, h, r, null);
        	ImageProcessor gip = new ByteProcessor(w, h, g, null);
        	ImageProcessor bip = new ByteProcessor(w, h, b, null);

	//create arrays for 'colour'
	int [] red= new int [3]; 
	int [] green= new int [3]; 
	int [] blue = new int [3]; 
	int [] colour = new int [3];


	int n = rgbStack.getSize();
	if (n>1)
		{IJ.showMessage("Does not work with stacks. Use Image/Colour/RGB split"); 
		return;}  

//	
	
	
	String [] 	mont3= {"square", "vertical", "horizontal"};	
	String [] 	color= {"Greys", "Pseudo", "Color_Blind"};	
	
	GenericDialog gd = new GenericDialog("Montage");
	int rows = 2, cols = 2;
	int border = 3;
	
	gd.addChoice("Rows × Columns", mont3,mont3[2]);
	gd.addNumericField("Border width", border, 0);
	gd.addNumericField("Scale bar size", scale, 1);
	//gd.addMessage("Click cancel to exit dialog \nand keep the split-stack");
	gd.addChoice("Colourisation", color,color[0]);

	gd.showDialog();
	if (gd.wasCanceled())
	            return;
	int montIndex  = gd.getNextChoiceIndex();
	border = (int)gd.getNextNumber();
	scale =  gd.getNextNumber();

	int colIndex = 	gd.getNextChoiceIndex();

	
	

	
  

	//create int to test for presence of each channel.
	int sumR=0, sumG =0, sumB =0;	

	for (int y=0; y<h;y++)
		{
		for (int x=0; x<w; x++)
			{
			if(colIndex==1)
				{
				red[0] = rip.getPixel(x,y);
				green[1]=gip.getPixel(x,y);
				blue[2]=bip.getPixel(x,y);
				}

			

			
			if(colIndex==0)	{
				for (int r2=0; r2<3; r2++)
					{red[r2] = rip.getPixel(x,y);
					green[r2]=gip.getPixel(x,y);
					blue[r2]=bip.getPixel(x,y);}	
				}
	
			if(colIndex ==2)
				{
				//red becomes R+G
				//green becomes G+B
				//blue R+B
				red[0] = rip.getPixel(x,y);
				red[2] = rip.getPixel(x,y);
				red[1]= gip.getPixel(x,y);
				
				green[0] = gip.getPixel(x,y);
				green[2] = gip.getPixel(x,y);
				green[1]= bip.getPixel(x,y);

				blue[0] = rip.getPixel(x,y);
				blue[2] = rip.getPixel(x,y);
				blue[1]= bip.getPixel(x,y);				


				}
			sumR +=rip.getPixel(x,y);
			sumG +=gip.getPixel(x,y);
			sumB +=bip.getPixel(x,y);
				
			rcp.putPixel(x,y,red);
			gcp.putPixel(x,y,green);
			bcp.putPixel(x,y,blue);
			
			}	
		}
             	cp = (ColorProcessor)rgbStack.getProcessor(1);

             	cp.getRGB(r,g,b);



	
		if (sumR!=0)	splitStack.addSlice("red",rcp);
		if(sumG!=0)    splitStack.addSlice("green",gcp);
		if(sumB!=0)     splitStack.addSlice("blue",bcp);
		

           splitStack.addSlice("merge",cp);

     	String title = imp.getTitle();
       	new ImagePlus(title+" (split)",splitStack).show();
	n = splitStack.getSize();
	
	ImagePlus imp2 = WindowManager.getCurrentImage();
	imp2.setCalibration(cal);
          	imp2.getWindow().repaint();


	
	if (montIndex==1&&n==4) {rows = 4; cols=1;}
	if (montIndex==2&&n==4) {rows = 1; cols=4;}
	if (montIndex==1&&n==3) {rows = 3; cols=1;}
	if (montIndex==2&&n==3) {rows = 1; cols=3;}
	IJ.run("Make Montage...", "columns="+cols +" rows="+rows+" scale=1 first=1 last="+n+" increment=1 border="+border);

	if(scale>0) IJ.run("Scale Bar...", "width="+scale+" height=5 font=18 color=White background=None location=[Lower Right] bold hide");

	WindowManager.getCurrentImage().setTitle(title+"  montage");
	
        WindowManager.getCurrentImage().getWindow().repaint();

	imp2.close();

    }
}



