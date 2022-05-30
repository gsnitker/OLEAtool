import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.io.*;
import java.io.*;
import ij.util.Tools;


public class HCS_plate3 implements PlugIn {
	int wordsPerLine=0, wordsInPreviousLine=0;
	int words = 0, chars = 0;
	int lines = 0;
	int width=1;
	int rows=16;
	int columns = 24;
	int firstCol=2;
	int scale = 1;
	boolean calcZ=true;
	String directory, name, path;
	public void run(String arg) {
	int words = 0, chars = 0;
	int lines = 0;
	int width=1;
	ImageProcessor ip;
	Boolean hideErrorMessages = true;
	String[] values;
	float [] pixels ;
	OpenDialog od = new OpenDialog("Open Text Image...", null);
	directory = od.getDirectory();
	name = od.getFileName();
        	if (name!=null)
		{path = directory + name;}
	else
		{return;}

	try{
            	words = chars = lines = 0;
	            Reader r = new BufferedReader(new FileReader(path));
 		countColumns(r);
		values = new String [rows*columns*wordsPerLine];
		pixels =  new float[rows*columns*wordsPerLine];

	           // ip = new FloatProcessor(width, lines, pixels, null);
            	read(r, columns*rows, values);
	            r.close();
            	//ip.resetMinAndMax();
		}
	catch (IOException e) {
	            String msg = e.getMessage();
            	if (msg==null || msg.equals(""))
	                msg = ""+e;
            	if (!hideErrorMessages) 
	                IJ.error("TextReader", msg);
            	ip = null;
        		}
	
	int a1=0;


	//while(values[a1]!="WellIndex")
	//	{a1++;}
	
	


	ImagePlus imp = WindowManager.getCurrentImage();
	ip=imp.getProcessor();
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



void countColumns(Reader r) throws IOException {
        StreamTokenizer tok = new StreamTokenizer(r);
  
        tok.resetSyntax();
        tok.wordChars(33, 127);
        tok.whitespaceChars(0, ' ');
        tok.whitespaceChars(128, 255);
        tok.eolIsSignificant(true);

        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            switch (tok.ttype) {
                case StreamTokenizer.TT_EOL:
                    lines++;
                    if (wordsPerLine==0)
                        lines--;  // ignore empty lines
                    if (lines==1)
                        width = wordsPerLine;
                    else if (wordsPerLine!=0 && wordsPerLine!=wordsInPreviousLine)
                         throw new IOException("Line "+lines+ " is not the same length as the first line.");
                    if (wordsPerLine!=0)
                        wordsInPreviousLine = wordsPerLine;
                    wordsPerLine = 0;
                    if (lines%20==0 && width>1 && lines<=width)
                        IJ.showProgress(((double)lines/width)/2.0);
                    break;
                case StreamTokenizer.TT_WORD:
                    words++;
                    wordsPerLine++;
                    break;
            }
        }
        if (wordsPerLine==width) 
            lines++; // last line does not end with EOL
   }
	

    void read(Reader r, int size, String[] values) throws IOException {
        StreamTokenizer tok = new StreamTokenizer(r);
        tok.resetSyntax();
        tok.wordChars(33, 127);
        tok.whitespaceChars(0, ' ');
        tok.whitespaceChars(128, 255);
        //tok.parseNumbers();

        int i = 0;
        int inc = size/20;
        if (inc<1)
            inc = 1;
        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            if (tok.ttype==StreamTokenizer.TT_WORD) {
                values[i++] = tok.sval;
	IJ.write(""+tok.sval);
                 if (i==size)
                     break;
                 if (i%inc==0)
                     IJ.showProgress(0.5+((double)i/size)/2.0);
            }
        }
        IJ.showProgress(1.0);
    }

}

