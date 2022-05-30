import java.text.DecimalFormat;
import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import java.awt.Rectangle;
import ij.text.*;
import java.text.DecimalFormat;
import ij.plugin.PlugIn;
import java.awt.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;


public class DNA_Cell_Cycle implements PlugInFilter, Measurements  {

        RoiManager roiManager;
        static boolean headingsSetCC;
        ImagePlus imp;
        StringBuffer sb = new StringBuffer();
        StringBuffer header = new StringBuffer();
       // String timePoint="";
      //  DecimalFormat df2 = new DecimalFormat("##0.00");
     //   DecimalFormat df0 = new DecimalFormat("##0");
      //  boolean isAltKey =IJ.altKeyDown();
        //int roiCount=0;
        RoiManager rm;
        String [] ccStr = {"G0-G1", "S", "G2", "early M", "late M"};

public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL+NO_CHANGES;
        }

public void run(ImageProcessor ip)
        {
                ImagePlus imp= WindowManager.getCurrentImage();
                String title = imp.getTitle();
                Calibration cal = imp.getCalibration();
                rm = RoiManager.getInstance();
                if (rm==null)
                        {IJ.error("Roi Manager is not open"); return;}
                rm.select(0); 
                Roi roi = imp.getRoi();

              

                Hashtable table = rm.getROIs();
                java.awt.List list = rm.getList();
                int roiCount = list.getItemCount();
                Roi[] rois = new Roi[roiCount];
                for (int i=0; i<roiCount; i++) {
                        String label = list.getItem(i);
                        Roi roi2 = (Roi)table.get(label);
                        if (roi2==null) continue;
                        rois[i] = roi2;
                }
      
                String units=cal.getTimeUnit();
                //header.append(units+"\t");
                ImageStack stack = imp.getStack();
           //     int firstSlice=imp.getCurrentSlice()-2;
                //int size = stack.getSize();
              //  float ratioValue=0;
              //  float ratioValue2=0;
              
                ImageProcessor mask = imp.getMask();
                //int[] mask = imp.getMask();
                Rectangle r = imp.getRoi().getBoundingRect();

      
                Analyzer analyzer = new Analyzer(imp);

                float [] ch1Prev = new float [roiCount];
               // float [] ch1Prev2 = new float [roiCount];

                int measurements = analyzer.getMeasurements();
                boolean showResults = measurements!=0 && measurements!=LIMIT;
                measurements |= MEAN;
                String roiName = "";
                measurements |= MEDIAN;
                int sel=0;

                header.append("File\tCell\tIntensity\tArea\tNorm intensity\tNorm Area\tTotal intensity\tCellCycle");

                header.append("\n");
              
                //sb.append(header);

                int s =1;

                ImageProcessor ip2= stack.getProcessor(s);  

        float []  meanArray= new float [roiCount];
        float []  stddevArray= new float [roiCount];
	float []  areaArray= new float [roiCount];

        float maxMean=0;
        float maxStddev =0;
        float minMean =9999999;
        float minStddev =999999999;
	float maxArea =0;
	float minArea =9999999;

         for(sel=0;sel<roiCount;sel++)
                        {
                           
                        ip2.setRoi(rois[sel]);
                        ImageStatistics stats = ImageStatistics.getStatistics(ip2, measurements, cal);
                //add stats to hist bin
                meanArray[sel]=(float)stats.mean;
                stddevArray[sel]=(float)stats.stdDev;
		areaArray[sel] = (float)stats.area;

		if (minArea>areaArray[sel]) minArea=areaArray[sel];
		if (maxArea<areaArray[sel]) maxArea=areaArray[sel];

                if (maxMean<meanArray[sel]) maxMean=meanArray[sel];
                if (minMean>meanArray[sel]) minMean=meanArray[sel];
                if (maxStddev<stddevArray[sel]) maxStddev=stddevArray[sel];
                if (minStddev>stddevArray[sel]) minStddev=stddevArray[sel];
                        }
               

                //set bins
        int nBins=12;

        float [] meanHist = new float [nBins+1];
        float [] stddevHist = new float [nBins+1];
	float [] areaHist = new float [nBins+1];

        float [] meanHistBin = new float [roiCount];
	float [] areaHistBin = new float [roiCount];
	float binWidthArea = (maxArea-minArea)/(float)nBins;

        float binWidthMean = (maxMean-minMean)/(float)nBins;
        float binWidthStddev = (maxStddev-minStddev)/(float)nBins;
        int currBin=0; 
int currBinArea=0;	

        //make histogram
        for (int i=0; i<roiCount; i++)
                {
                currBin=(int)(nBins*(meanArray[i]-minMean)/(maxMean-minMean));
                meanHist[currBin]=meanHist[currBin]+1;
                meanHistBin[i]=currBin;
                currBin=(int)(nBins*(stddevArray[i]-minStddev)/(maxStddev-minStddev));
                stddevHist[currBin]=stddevHist[currBin]+1;

	currBinArea=(int)(nBins*(areaArray[i]-minArea)/(maxArea-minArea));
		areaHist[currBinArea]=areaHist[currBinArea]+1;
		areaHistBin[i]=currBinArea;

                }      
       

        //identify max bin
        int maxBin=0;
        float maxBinValue=0;

	int maxBinArea=0;
	float maxBinAreaValue=0;

        for (int k=0; k<nBins; k++)
                {
                if(maxBinValue<meanHist[k])
                        {maxBinValue= meanHist[k];
                        maxBin=k;

		if(maxBinAreaValue<areaHist[k]) 
			{maxBinAreaValue= areaHist[k];
			maxBinArea=k;
			}
                        }
                }      
       
        //get Average of cells in this G1 bin
       
        float meanG1Int=0;
	float areaG1=0;

        for (int j=0;j<roiCount; j++)
                {
                if (meanHistBin[j]==maxBin) meanG1Int= meanG1Int+meanArray[j]/maxBinValue;             
if (areaHistBin[j]==maxBinArea) areaG1= areaG1+areaArray[j]/maxBinAreaValue;	

                }      
       
        //normalise to  G1 bin
        float [] meanArrayNorm  = new float [roiCount];
       float [] areaArrayNorm  = new float [roiCount];

        //set up cell cycle class aray 0= g1, 1=s, 2 = G2, 3 = eM' 4 = lM

       

        String [] classArray = new String [roiCount];
        int curCC=0;

        ImageProcessor ipR= imp.getProcessor().duplicate();
        ImageProcessor ipG= imp.getProcessor().duplicate();
        ImageProcessor ipB= imp.getProcessor().duplicate();

        for (int l=0;l<roiCount; l++)
                {curCC=0;
                meanArrayNorm[l]=meanArray[l]/meanG1Int;    
		areaArrayNorm[l]=areaArray[l]/areaG1;
                           
            //    if((meanArrayNorm[l]>1.1)&&(areaArrayNorm[l]>1.0) ) curCC=1;
             //   if((meanArrayNorm[l]>1.2)&&(areaArrayNorm[l]>1.3) )  curCC=2;
              //  if((meanArrayNorm[l]>1.5)&&(areaArrayNorm[l]>1.4) )  curCC=3;
               // if((meanArrayNorm[l]>1.3)&&(areaArrayNorm[l]<0.9) ) curCC=4;      
            

//close toMM output
	 if((meanArrayNorm[l]*areaArrayNorm[l]>1.25) ) curCC=1;
           if((meanArrayNorm[l]*areaArrayNorm[l]>2.0) )  curCC=2;
           if((meanArrayNorm[l]*areaArrayNorm[l]>2.5) )  curCC=3;
        if((meanArrayNorm[l]>1.4)&&(areaArrayNorm[l]<0.95) ) curCC=4;    

//	 if((meanArrayNorm[l]*areaArrayNorm[l]>1.1) ) curCC=1;
 //           if((meanArrayNorm[l]*areaArrayNorm[l]>1.5) )  curCC=2;
   //         if((meanArrayNorm[l]*areaArrayNorm[l]>2) )  curCC=3;
   //     if((meanArrayNorm[l]>1.4)&&(areaArrayNorm[l]<0.95) ) curCC=4;    


    classArray[l]=ccStr[curCC];
               

	boolean draw=true;
		if(draw)
			{	
			ipR.setMask(rois[l].getMask());
			ipR.setRoi(rois[l].getBoundingRect());
			
			ipG.setMask(rois[l].getMask());
			ipG.setRoi(rois[l].getBoundingRect());

			ipB.setMask(rois[l].getMask());
			ipB.setRoi(rois[l].getBoundingRect());
			
			ipR.setColor(255) ;
			ipG.setColor(255) ;
			ipB.setColor(255) ;			

			if(curCC==0)
				{ipB.fill(ipB.getMask());			
				}
			
			if(curCC==1)
				{
				ipG.fill(ipG.getMask());
				ipB.fill(ipB.getMask());
				}

			if(curCC==2)
				{ipG.fill(ipG.getMask());				
				}


			if(curCC==3)
				{ipR.fill(ipR.getMask());
				ipG.fill(ipG.getMask());
				}

			if(curCC==4)
				{ipR.fill(ipB.getMask());
				
				}

			}
		
		}	

                for(sel=0;sel<roiCount;sel++)
                        {
                           
                       sb.append(imp.getTitle());
                        sb.append("\t"+(sel+1)+"\t"+meanArray[sel]+"\t"+areaArray[sel]+"\t"+meanArrayNorm[sel]+"\t"+areaArrayNorm[sel]+"\t"+meanArrayNorm[sel]*areaArrayNorm[sel]+"\t"+classArray[sel]);
                    sb.append("\n");
                //add stats to hist bin
                        }

                ResultsTable rt=Analyzer.getResultsTable();
        ImageProcessor cp= new ColorProcessor(imp.getWidth(), imp.getHeight());
        int [] rgb  = new int [3];
        for(int x = 0; x<imp.getWidth(); x++)
                {
                for(int y = 0; y<imp.getHeight(); y++)
                        {rgb[0]=(int)(ipR.getPixelValue(x,y));
                        rgb[1]=(int)(ipG.getPixelValue(x,y));
                        rgb[2]=(int)(ipB.getPixelValue(x,y));
                        cp.putPixel(x,y, rgb);
                        }              
                }
       

       
       
        new ImagePlus("Color", cp).show();
        imp.updateAndDraw();
//                if(Analyzer.getResultsTable().getColumnHeadings()!=header.toString())
  //                      {
                      IJ.setColumnHeadings(header.toString());
                      
    //                    }

                IJ.write(sb.toString());
        //rt.updateResults() ;
        }
}


 

