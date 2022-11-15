import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import java.io.*;
import ij.plugin.*;
import ij.text.TextWindow;




public class TXT_to_scatter implements PlugIn
{
	int count, colNum, rowNum = 0;
	String [] headers;
	String row;


    public void run(String arg)
    {
		float px, py, pxmin, pxmax, pymin, pymax;
		int spx, spy, ps;



  		OpenDialog od = new OpenDialog("List Opener", null);


  		String name = od.getFileName();
        if (name==null)
            return;
        String dir = od.getDirectory();

        String separator = System.getProperty("file.separator");
		String fileName = dir+name;
		IJ.run("Text Image... ", "open=["+fileName+"]");

		ImagePlus imp = WindowManager.getCurrentImage();
		int height= imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
		//

	//	getColumnCount(fileName);

	//	IJ.showMessage(""+colNum);
	//	headers = new String [colNum];


		getHeaderInfo(fileName);

		int xaxis= 1;
		int yaxis = 2;

		//IJ.write(headers.length+"");
		//for(int h=0; h<headers.length; h++) IJ.write(headers[h]);

		GenericDialog gd = new GenericDialog("Image to Scatterplot");
		gd.addChoice("X-axis", headers, headers[xaxis]);
		gd.addCheckbox("Autoscale x-axis", false);
		gd.addNumericField("x-axis min", 0, 0);
		gd.addNumericField("x-axis max", 512, 0);
		gd.addMessage("");

		gd.addChoice("Y-axis", headers, headers[yaxis]);
		gd.addCheckbox("Autoscale y-axis", false);
		gd.addNumericField("y-axis min", 1500, 0);
		gd.addNumericField("y-axis max", 3500, 0);

		gd.addMessage("");
		gd.addNumericField("Bins", 128, 0);
		gd.addNumericField("Image size", 256, 0);
		gd.showDialog();
       	if (gd.wasCanceled())
 			return ;

		xaxis = (int)gd.getNextChoiceIndex();
		boolean autoX = gd.getNextBoolean();
		pxmin = (float)gd.getNextNumber();
		pxmax = (float)gd.getNextNumber();

		yaxis = (int)gd.getNextChoiceIndex();
		boolean autoY =gd.getNextBoolean();
		pymin=(float)gd.getNextNumber();
		pymax = (float)gd.getNextNumber();


		int size = (int)gd.getNextNumber();
		int frame = (int)gd.getNextNumber();
		//create plot
		//IJ.write(""+xaxis+"   "+ yaxis);

		ImageProcessor ip2 = new ByteProcessor(size,size);


		//getmin and max of column

		float [] xpixels = new float [imp.getHeight()];
		float [] ypixels = new float [imp.getHeight()];


		//pxmin=0; pxmax=0; pymax=0; pymin=0;

		for (int r=0; r<height; r++)
			{
			px =ip.getPixelValue(xaxis, r);
			py =ip.getPixelValue(yaxis, r);

			//IJ.write(""+px+"  "+py);

			xpixels[r]=px;
			ypixels[r]=py;


			if(autoY)
				{
				if(r==1)
					{pymin=py; pymax=py;}
				if(pymin>py) pymin=py;
				if(pymax<py) pymax=py;
				}

			if(autoX)
				{
				if(r==1)
					{pxmin=px; pxmax=px;}
				if(pxmin>px) pxmin=px;
				if(pxmax<px) pxmax=px;
				}
			}
	//IJ.write(pxmin+"-"+ pxmax+ "    "+ pymin+"-"+ pymax);

		for (int r=0; r<imp.getHeight(); r++)
			{
			px =xpixels[r];
			py =ypixels[r];

			//scale ps
			spx = (int)((px-pxmin)/(pxmax-pxmin)*size);
			spy =size-(int)((py-pymin)/(pymax-pymin)*size);

		//IJ.write(""+spx+"  "+spy);

			ps = (int)ip2.getPixel(spx, spy)+1;
			ip2.putPixel(spx, spy, ps);

			}

		double sc =(double)((frame/size));
		//IJ.showMessage(sc+"");
		ImageProcessor ip3 = ip2.resize(frame,frame);
		int border=100;

		ImageProcessor ip4 = new ShortProcessor(frame+border, frame+border);
		ip4.insert(ip3, (int)(border/2),(int)(border/2));
		ip3.resetMinAndMax();
		new ImagePlus("plot x: "+headers[xaxis]+" vs y:"+headers[yaxis], ip3).show();

		imp.close();

		//add axes and scales
		ip4.resetMinAndMax();
		double plotMax = ip4.getMax();
		double plotMin = ip4.getMin();

		for(int x=(int)(border/2); x<(int)(frame+(border/2)); x++)
				{
				ip4.putPixelValue(x,(int)(frame+(border/2)),plotMax);
				}

		for(int y=(int)(border/2); y<(int)(frame+(border/2)); y++)
			{
			ip4.putPixelValue((int)(border/2),y,plotMax);
			}


		//areit axes
	ip4.setColor(0xffffff);

	ip4.drawString(headers[xaxis],(int)(frame/2),frame+(int)(border*0.8));
	ip4.drawString(""+pxmin,(int)(border*0.3),frame+(int)(border*0.75));
	ip4.drawString(""+pxmax,frame+(int)(border*0.3),frame+(int)(border*0.75));

	int r=90;
	ip4.rotate(r);

	ip4.drawString(headers[yaxis],(int)(frame/2),(int)(border*0.2));
	ip4.drawString(""+pymin,(int)(border*0.3),(int)(border*0.25));
	ip4.drawString(""+pymax,frame+(int)(border*0.3),(int)(border*0.25));


	r=270;
	//ip4.drawString(headers[xaxis],(int)(frame),frame+(int)(border*0.75));
	ip4.rotate(r);
	ip4.setMinAndMax(plotMin, plotMax);
	new ImagePlus("x: "+headers[xaxis]+" vs y:"+headers[yaxis], ip4).show();
	}







String [] getHeaderInfo(String fileName)
	{

	String comma = "\t";
	char [] commaAr = comma.toCharArray();
	char [] rowAr;
	int offset=0;
//get headers and count rows
        try
    		{
            BufferedReader r = new BufferedReader(new FileReader(fileName));
            while (true)
	         	{
				colNum=1;
        		row= r.readLine();
				if (row==null)
					break;
		        rowNum++;
				if (rowNum==2)
						{
							return headers;
							//break;

					}
		      //  IJ.write(row);
        	    rowAr =  row.toCharArray();
        		int [] commaLoc = new int [rowAr.length];
        		//IJ.showMessage(""+rowAr.length);

        		//count columns
		        for (int i=0; i<rowAr.length; i++)
        		    {
        		    if(rowAr[i]==commaAr[0])
        		        {
        		        commaLoc[colNum]=i;
        		        colNum++;
        		        }

        			}
        //create header line
        	if(rowNum==1)
           		{
            	headers = new String [colNum-1];
            	commaLoc[colNum] = rowAr.length;
            	for(int j=0; j<colNum-1; j++)
            	    {

            	   // IJ.write(""+commaLoc[j]+"  to " +commaLoc[j+1] );
            	    if(j>0) offset=1;
            	    headers[j]=row.substring(commaLoc[j]+offset, commaLoc[j+1]);
            	    }
				}
                if (row==null)
        	        break;
                else
            		{

                    }
                }

            r.close();
            return headers;
            }


catch (IOException e)
     	{
     	 IJ.error(""+e);
      	return headers;
      	}

	}





}

