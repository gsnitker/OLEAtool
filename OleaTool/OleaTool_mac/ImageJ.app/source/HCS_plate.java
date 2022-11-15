import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;

public class HCS_plate implements PlugIn {
	
	int rows=16;
	int columns = 24;
	int firstCol=3;
	int scale = 1;

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
	int rowCount=1;
	int colCount=1;
	double pSum=0; int pCount=0; double pSum2=0; double stdDev=0; double z=0;

	for (int well=0; well<(columns*rows); well++)
		{
		//create two arrays of row and column info
		rowID[well]=rowCount;
		colID[well]=colCount;	
		colCount++;
		if(colCount>columns) 
			{colCount=1;
			rowCount++;
			if(rowCount>rows) rowCount=1;
			}				
		}
	double zMax, pMax;
	int wellID=0;
	for (int r = firstCol; r<=w; r++)
		{ImageProcessor ip2=new FloatProcessor(columns,rows);	
		pMax=0;
		for (int c1=0; c1<h; c1++)
				{p=ip.getPixelValue(r-1, c1);
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

		for (int c=0; c<h; c++)
			{
			p=ip.getPixelValue(r-1, c-1);
			z = ((p-(pSum/pCount))/stdDev)/zMax;
		
			ip2.putPixelValue(colID[c]-1,rowID[c]-1,z);
			}
		ip2.setInterpolate(false);
		ip2= ip2.resize(columns*scale,rows*scale);
		hcs.addSlice("row"+r, ip2);
		}
	new ImagePlus("HCS",hcs).show();
	}

}
