import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import ij.measure.Calibration;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;

public class Photon_weighted_FLIM_stack implements PlugIn 
	{

	private ImagePlus imp; 
	ImagePlus[] image = new ImagePlus[2];

	public void run(String arg) 
		{
		int[] wList = WindowManager.getIDList();
		if (wList==null) 
			{
			IJ.error("No images are open.");
			}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++)
			 {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
			}
		String none = "*None*";
		titles[wList.length] = none;
		GenericDialog gd = new GenericDialog("Photon-FLIM Merge");
		gd.addChoice("Photon image:", titles, titles[0]);
		gd.addChoice("Lifetime image:", titles, titles[1]);
		
		gd.showDialog();
		if (gd.wasCanceled()) 
			return;
		for(int ii=0;ii<2;ii++) 
			{
			int FILE=gd.getNextChoiceIndex();
			image[ii] = (FILE<wList.length)
					? WindowManager.getImage(wList[FILE])
				: null;
			}

	
		if(image[0].getType()!= ImagePlus.GRAY32) 
			{
			IJ.error("Photon imge must be 32-bit");
			}
		if(image[1].getType()!= ImagePlus.COLOR_RGB) 
			{
			IJ.error("FLIM imge must be RGB");
			}
		byte[] r,g,b;
		ColorProcessor cp, cp2;
		cp=(ColorProcessor)image[1].getProcessor();
		
		ImageProcessor ip = (ImageProcessor)image[0].getProcessor();
		float photons =0;

		int w=image[0].getWidth();
		int h= image[0].getHeight();
		
		cp2 = new ColorProcessor (w,h);
		ImageStack impStack = image[0].getStack();

 		ImageStack rgbStack = image[1].getStack();

	         	ImageStack redStack = new ImageStack(w,h);
         		ImageStack greenStack = new ImageStack(w,h);
		ImageStack blueStack = new ImageStack(w,h);
		ImageStack rgb = new ImageStack(w, h);

		r = new byte[w*h];
      	     	g = new byte[w*h];
		b = new byte[w*h];
	         int n = rgbStack.getSize();

	         for (int i=1; i<=n; i++) 
			{r = new byte[w*h];
      	     		g = new byte[w*h];
			b = new byte[w*h];
			cp = (ColorProcessor)rgbStack.getProcessor(i);
			cp.getRGB(r,g,b);
			redStack.addSlice(null,r);
  	          		greenStack.addSlice(null,g);
	   	         	blueStack.addSlice(null,b);
			}
		//new ImagePlus(" (green)",greenStack).show();
		
		int [] colour  = new int [3]; 
		ImageProcessor ipR, ipG, ipB;
	
		Analyzer analyzer = new Analyzer(image[0]);
		Calibration cal = image[0].getCalibration();
		int measurements = analyzer.getMeasurements();
		ImageStatistics ipStat = ImageStatistics.getStatistics(ip, measurements, cal);

	for(int s=1; s<=n; s++)
		{
		cp2 = new ColorProcessor (w,h);
		ip = impStack.getProcessor(s);
		 ip.resetMinAndMax();
		
		
		
		ipR = redStack.getProcessor(s);
		ipG = greenStack.getProcessor(s);
		ipB = blueStack.getProcessor(s);
		IJ.write(""+ip.getMax());
		for (int y=0; y<h; y++)
			{
			for (int x=0; x<w; x++)
				{
				photons = ip.getPixelValue(x,y);
				colour[0] = (int)(ipR.getPixel(x,y)*(photons/ip.getMax()));
				colour[1] = (int)(ipG.getPixel(x,y)*(photons/ip.getMax()));
				colour[2] =(int)(ipB.getPixel(x,y)*(photons/ip.getMax()));
			  	cp2.putPixel(x,y,colour);
				}
			}
		rgb.addSlice(null, cp2);
		}
		
		new ImagePlus( "FLIM-photon",rgb).show();
        WindowManager.getCurrentImage().getWindow().repaint();
System.gc();
	}
	
}
