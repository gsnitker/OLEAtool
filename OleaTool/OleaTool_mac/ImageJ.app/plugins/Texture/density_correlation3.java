import ij.*;

import ij.process.*;

import ij.gui.*;

import java.awt.*;

import ij.plugin.*;



public class density_correlation3 implements PlugIn {





public void run(String arg) 

	{

	//check there's an image open

	int[] wList = WindowManager.getIDList();

        	if (wList==null)

		{

	            IJ.error("No images are open.");

            	return;

        		}



	//set up analysis objects

	ImagePlus imp = WindowManager.getCurrentImage();

	ImageProcessor ip = imp.getProcessor();



//accessing the number of pixels in the x direction
int Ni= imp.getWidth();

IJ.write(""+Ni);

//accessing the number of pixels in the y direction
int Nj= imp.getHeight();  

IJ.write(""+Nj);

//calculating the average intensity of the image

double intensity=0; 
for (int i=1; i<=Ni; i++)  // start at i=0 or 1? <Ni or <=Ni?
{	
	for (int j=1; j<=Nj; j++)
	{
		
		intensity=intensity+ip.getPixelValue(i,j); //
		
	}
}

double avgintensity=intensity/(Ni*Nj);
double avgintensity_squared=Math.pow(avgintensity,2);

//part 1 of calculating the density density correlation function
int c=(Ni*Nj);

//*(Ni*Nj);

// this is the maximum number of radii values that can be calculated

IJ.write(""+c);


int counter=0;// this counts how many radius values have been calculated-1
double intensity_product[]= new double[c]; // this array will contain the multiplied intensities  
double r[]= new double[c]; // this array will contain the radius values
double N[]=new double[c]; // this array will contain the number of points at which the same radius occurs for each radius value 

for ( int i=0; i<c; i++) 
{IJ.write("reached here");
	intensity_product[i]=0; //fill array intensity_product with zeros
	r[i]=-1; //fill array r with -1s
	N[i]=1; //fill array N with 1s
	IJ.write(" "+N[i]);
}

for (int i=0; i<Ni; i++)
{
	for (int j=0; j<Nj; j++)
	{	
		for (int i2=1; i2<=Ni; i2++)
		{	
			for (int j2=1; j2<=Nj; j2++)

			{
				double densityfunc= ip.getPixelValue(i,j)* ip.getPixelValue (i2,j2);
				double radius= Math.sqrt((i-i2)^2 + (j-j2)^2);
				int k=-1;
				{
					do // this is necessary to group all of the values that have the same radius value 
					{
						k++;
					
					IJ.write(" "+r[k]);
					if (radius==r[k])
						{	
							intensity_product[k]= intensity_product[k]+densityfunc;
							N[k]=N[k]+1;
						}
					}
					while ((radius!=r[k]) && (k<counter));
					
					if (radius!=r[k])
					{
							intensity_product[k] = densityfunc;
							r[k]=radius;
							counter=counter+1;
					}
				}
			}	
		}
	}	

}
// part 2 of calculating the correlation function
double correlation[]= new double[counter];// this array contains the density density correlation function
for (int i=0; i<counter; i++)
{
	
	correlation[i]= ((intensity_product[i]/N[i])/avgintensity_squared)-1; 	
}



//print data
IJ.write("Correlation   " + "r   " + "N   ");

for (int i=0; i<counter; i++)
{
	IJ.write(correlation[i] + "   " + r[i]+ "   " + N[i]); //write in column 1 correlation[]; write in column 2 r[]; write N[] in column 3

}
}
}
