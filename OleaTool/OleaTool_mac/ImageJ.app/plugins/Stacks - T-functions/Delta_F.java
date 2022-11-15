import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.ImageStack.*;

public class Delta_F implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
   		if (imp==null)
			{IJ.noImage(); return;}
		String fileName = imp.getTitle();
		ImageStack stack1 = imp.getStack();

		ImageStack stack2 = new ImageStack(imp.getWidth(), imp.getHeight());

		
		ImageProcessor ip1, ip2, ip3;
		

		float a, b, c, d;
		FloatProcessor ip4 = new FloatProcessor(imp.getWidth(), imp.getHeight());


		for(int s=2; s<stack1.getSize(); s++)
		{
		ip1=stack1.getProcessor(s-1);		
		ip2=stack1.getProcessor(s);		
		ip3=stack1.getProcessor(s+1);		
		ip4 = new FloatProcessor(imp.getWidth(), imp.getHeight());
		
		for(int x=0; x<imp.getWidth(); x++)
			{for (int y=0; y<imp.getHeight(); y++)
				{
				a=(float)ip1.getPixel(x,y);
				b=(float)ip2.getPixel(x,y);
				c=(float)ip3.getPixel(x,y);

				c=0;		
				d= b-(a/2)-(c/2);
				
				ip4.putPixelValue(x,y,d);
				}
			}
		
		stack2.addSlice(""+s, ip4);
		}
	new ImagePlus("Delta F", stack2).show();
	}

}
