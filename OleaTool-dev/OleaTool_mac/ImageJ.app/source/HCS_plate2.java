import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;

public class HCS_plate2 implements PlugIn {
	
	int rows=16;
	int columns = 24;
	int firstCol=2;
	int scale = 1;
	boolean calcZ=false;
	boolean normalise2Max = false;

	public void run(String arg) {
		
	ImagePlus imp = WindowManager.getCurrentImage();
	ImageProcessor ip=imp.getProcessor();
	int w=imp.getWidth();
	int h=imp.getHeight();
	
	if(h!=rows*columns)
		{
		IJ.error("ERROR: image Height should equal the number of wells in plate");
		return;
		}

	String title = imp.getTitle();
	
	ImageStack hcs= new ImageStack(columns*scale,rows*scale);
	double p=0;
	int [] rowID = new int[columns*rows];
	int [] colID = new int[columns*rows];
	int rowCount=0;
	int colCount=0;
	double pSum=0; int pCount=0; double pSum2=0; double stdDev=0; double z=0;

	for (int well=0; well<(columns*rows); well++)
		{
		//create two arrays of row and column info
		rowID[well]=rowCount;
		colID[well]=colCount;	
		colCount++;
		if(colCount>=columns) 
			{colCount=0;
			rowCount++;
			if(rowCount>rows) rowCount=0;
			}				
		}
	double zMax, pMax;
	int wellID=0;
	for (int c = firstCol; c<w; c++)
		{ImageProcessor ip2=new FloatProcessor(columns,rows);	
		pMax=0;
		for (int r1=0; r1<h; r1++)
				{p=ip.getPixelValue(c, r1);
				pSum=+p;
				pCount++;
				if (p>pMax) pMax=p;
				}
		pSum2=pSum*pSum;
		if (pCount>0) {
			stdDev = (pCount*pSum2-pSum*pSum)/pCount;
			if (stdDev>0.0)
				stdDev = Math.sqrt(stdDev/(pCount-1.0));
			else
				stdDev = 0.0;
		}
		
		zMax=	(pMax-(pSum/pCount))/stdDev;		
		if (!normalise2Max) pMax=1;
		for (int r=0; r<h; r++)
			{
			p=ip.getPixelValue(c, r);
			
			if(calcZ)
				{z = ((p-(pSum/pCount))/stdDev)/zMax;}
			else{z = p/pMax;
				}
			//IJ.write(""+colID[r]+" X "+ rowID[r] +" = " + z);
			ip2.putPixelValue(colID[r],rowID[r],z);
			}
		ip2.setInterpolate(false);
		ip2= ip2.resize(columns*scale,rows*scale);
		hcs.addSlice("Column"+(c+1), ip2);
		}
	new ImagePlus("HCS",hcs).show();
	ImageCanvas ic = WindowManager.getCurrentImage().getCanvas();
	 Point loc = ic.getCursorLoc();
	int x = ic.screenX(loc.x);
	int y = ic.screenY(loc.y);
	for (int zm=1; zm<10; zm++)   ic.zoomIn(x, y);
	WindowManager.getCurrentImage().repaintWindow();
	}

}
