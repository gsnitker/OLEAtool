import ij.plugin.*;
import ij.*;
import ij.io.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
//import ij.util.StringSorter;
//import ij.plugin.frame.Recorder;
//import ij.plugin.FileInfoVirtualStack;
//import ij.measure.Calibration;


public class Biostation_TIF_Collator_2 extends ImagePlus implements PlugIn {

	ImageProcessor ip;
	int width, height;
	int count=0;
	int countTIF=0;
	int count2=0;
	int count3=0;
	int countFolder=0;
	String [] listTIF;
	String [] listFolder;
	int countSlices=0;
	String dir = Prefs.get("BSt_dir.string","");
	public void run(String arg) {

		String dir= IJ.getDirectory("Select source folder...");

        		if (dir==null)	return;

		Prefs.set("BSt_dir.string",dir);

		//getDifectories then for each directory...
		getFolderCount(dir);
		listFolder = new String [countFolder];
		getFolderPaths(dir);

		for (int f=0; f<listFolder.length; f++)
			{

			String folder = listFolder[f].substring(listFolder[f].lastIndexOf("\\")+1);

				count=0;
				countTIF=0;
				count2=0;
				getFileCount(listFolder[f]);
				listTIF = new String [countTIF];
				if(countTIF==0)
					{IJ.showMessage("No TIFs found");
					return;
					}
				getTIFpaths(listFolder[f]);
				ImageStack stack = new ImageStack(1000, 1000);
				Opener opener = new Opener();

				for (int i=0; i<listTIF.length; i++)
					{
					if(IJ.escapePressed() ) return;
					if(listTIF[i].endsWith(".tif"))
						{
						 IJ.showProgress((float)i/(float)countTIF);
						int fileIndex=listTIF[i].lastIndexOf("\\");
						String curPath = listTIF[i].substring(0, fileIndex);

						String curPathtmp =listTIF[i].substring(0, fileIndex-1);
						int folderIndex = curPathtmp.lastIndexOf("\\");

						String curDir = curPath.substring(folderIndex+1, fileIndex);


						String motherFolder = curPath.substring(folderIndex-2, folderIndex);

						//IJ.write(curDir+motherFolder);

					//if(curDir.equals("Ch2"))	IJ.write("found CH2 folder"+ curDir);

						if(!curDir.equals("Macro"))
							{
							String curFile = listTIF[i].substring(fileIndex+1, listTIF[i].length());

							if(!(curDir+motherFolder).equals("PhFL"))
							{


								IJ.showStatus("Reading: "+folder +" - "+ i +" of "+ listTIF.length);


								ImagePlus img = opener.openImage(curPath, curFile);
								//convert to 8bit
								ImageConverter ic = new ImageConverter(img);
								ImageProcessor ip2 = new ByteProcessor(1000,1000);
								ic.convertToGray8();
								width = img.getWidth();
								height  =img.getHeight();
								img.setRoi((width-1000)/2, 0, 1000, 1000);
								ip = img.getProcessor().crop();
								ip2.copyBits(ip, 0,0, Blitter.COPY);
								stack.addSlice(curFile, ip2);}
							}
						}
					}

				String fileName = listFolder[f];
				String row, column, zeropad, rowNumStr ;
				char rowNumChar;
				int rowNumInt;
				zeropad="000";
				row=folder.substring(0,1);
				rowNumInt= row.charAt(0)-64;
				if(rowNumInt>9) zeropad="00";
				rowNumStr = zeropad + (int)rowNumInt;
				zeropad="000";
				column=folder.substring(1);
				if(column.length()==2) zeropad="00";
				column=zeropad+column;
				fileName=rowNumStr+column;
				if(countTIF>0)
					{new ImagePlus(fileName, stack).show();
					ImagePlus impNew=WindowManager.getCurrentImage();
					//IJ.write(""+ listFolder[f]+".tif");
					IJ.saveAs("Tiff", dir+"\\"+fileName+".tif");
					impNew.close();
					//IJ.showMessage("Number of fields of view = "+(float)countTIF/(float)countSlices);
					//IJ.run("DeInterleave ");
				}


			else	IJ.log(dir+": No TIF files found");


	}

}




	void getFileCount(String dir)
	{
	if(IJ.escapePressed() ) return;
	IJ.showStatus("Counting files: " + dir);
	String[] list = new File(dir).list();
	for(int f=0; f<list.length; f++)
		{
		if(list[f].indexOf(".")<0)
			{
			//IJ.write("" + dir + list[f]+ " is dir");
			getFileCount(""+dir+"\\"+list[f]);
			}
		else count++;

		if(list[f].endsWith(".tif")) countTIF++;
		}
	}


	void getFolderCount(String dir)
		{if(IJ.escapePressed() ) return;
		IJ.showStatus("Counting Folders: " + dir);
		String[] list = new File(dir).list();
		for(int f=0; f<list.length; f++)
			{
			if(list[f].indexOf(".")<0)
				countFolder++;
			}

		}



	String [] getTIFpaths(String dir)
		{

		String cfolder;

		String[] list = new File(dir).list();

		for(int f=0; f<list.length; f++)
			{
			if(list[f].indexOf(".")<0)
				{
				getTIFpaths(""+dir+"\\"+list[f]);
				}
			else
				{
				if(list[f].endsWith(".tif"))
					{
					listTIF[count2] = ""+dir+"\\"+list[f];
					count2++;
					}
				}
			}

		return(listTIF);
		}




		String [] getFolderPaths(String dir)
				{
				String[] list = new File(dir).list();
				for(int f=0; f<list.length; f++)
					{
					if(list[f].indexOf(".")<0)
						{
							//IJ.write("" + dir + list[f]+ " is dir");
							listFolder[count3] = ""+dir+"\\"+list[f];
							count3++;
						}

					//if(list[f].endsWith(".TIF")) countTIF++;
					}
				return(listFolder);
				}



}

