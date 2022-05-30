import ij.*;
import ij.plugin.PlugIn;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.text.TextWindow;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.text.DecimalFormat;

/** This plugin is customizable version of the Analyze/Histogram command. */
public class FLIM_Histogram implements PlugIn {

	public void run(String arg) {
		new HistogramWindow(IJ.getImage());
	}

	/** This inner class is an extended ImageWindow that displays histograms. */
	class HistogramWindow extends ImageWindow implements Measurements, ActionListener, ClipboardOwner {
		
	
		

		static final int WIN_WIDTH = 256	;
		static final int WIN_HEIGHT = 256;
		static final int BAR_HEIGHT = 20;


		static final int HIST_WIDTH = WIN_WIDTH-50;
		static final int	HIST_HEIGHT = WIN_HEIGHT-BAR_HEIGHT-50;
 		

		
		static final int XMARGIN = 25;
		static final int YMARGIN = 10;
		
		protected ImageStatistics stats;
		protected int[] histogram;
		protected LookUpTable lut;
		protected Rectangle frame = null;
		protected Button list, save, copy,log;
		protected Label value, count;
		protected String defaultDirectory = null;
		protected int decimalPlaces;
		protected int digits;
		protected int newMaxCount;
		protected int plotScale = 1;
		protected boolean logScale;
		protected Calibration cal;
		protected int yMax;
		public int nBins = 64;
		
		/** Displays a histogram using the title "Histogram of ImageName". */
		public HistogramWindow(ImagePlus imp) {

		super(NewImage.createByteImage("Histogram of "+imp.getShortTitle(), WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));

		ImageProcessor ipp= imp.getProcessor();		
		double curMin = ipp.getMin();
		double curMax = ipp.getMax();

		GenericDialog gd = new GenericDialog("Set Hist range");
		gd.addNumericField("Min_Tau", curMin, 0);
		gd.addNumericField("Max_Tau", curMax, 0);
		gd.addNumericField("Number_Bins", nBins, 0);

		gd.showDialog();
		if (gd.wasCanceled()) 
			return;

		curMin= gd.getNextNumber();
		curMax= gd.getNextNumber();
		nBins= (int)gd.getNextNumber();		
	

			showHistogram(imp, nBins, curMin, curMax);
		}
	
		/** Displays a histogram using the specified title and number of bins. 
			Currently, the number of bins must be 256 expect for 32 bit images. */
		public HistogramWindow(String title, ImagePlus imp, int bins) {
			super(NewImage.createByteImage(title, WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
			showHistogram(imp, bins, 0.0, 0.0);
		}
	
		/** Displays a histogram using the specified title, number of bins and histogram range.
			Currently, the number of bins must be 256 and the histogram range range must be the 
			same as the image range expect for 32 bit images. */
		public HistogramWindow(String title, ImagePlus imp, int bins, double histMin, double histMax) {
			super(NewImage.createByteImage(title, WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
			showHistogram(imp, bins, histMin, histMax);
		}
	
		/** Displays a histogram using the specified title, number of bins, histogram range and yMax. */
		public HistogramWindow(String title, ImagePlus imp, int bins, double histMin, double histMax, int yMax) {
			super(NewImage.createByteImage(title, WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
			this.yMax = yMax;
			showHistogram(imp, bins, histMin, histMax);
		}
	
		/** Displays a histogram using the specified title and ImageStatistics. */
		public HistogramWindow(String title, ImagePlus imp, ImageStatistics stats) {
			super(NewImage.createByteImage(title, WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
			//IJ.log("HistogramWindow: "+stats.histMin+"  "+stats.histMax+"  "+stats.nBins);
			this.yMax = stats.histYMax;
			showHistogram(imp, stats);
		}
	
		/** Draws the histogram using the specified title and number of bins.
			Currently, the number of bins must be 256 expect for 32 bit images. */
		public void showHistogram(ImagePlus imp, int bins) {
			showHistogram(imp, bins, 0.0, 0.0);
		}
	
		/** Draws the histogram using the specified title, number of bins and histogram range.
			Currently, the number of bins must be 256 and the histogram range range must be 
			the same as the image range expect for 32 bit images. */
		public void showHistogram(ImagePlus imp, int bins, double histMin, double histMax) {
			boolean limitToThreshold = (Analyzer.getMeasurements()&LIMIT)!=0;
			stats = imp.getStatistics(AREA+MEAN+MODE+MIN_MAX+(limitToThreshold?LIMIT:0), bins, histMin, histMax);
			showHistogram(imp, stats);
		}
	
		/** Draws the histogram using the specified title and ImageStatistics. */
		public void showHistogram(ImagePlus imp, ImageStatistics stats) {
			setup();
			this.stats = stats;
			cal = imp.getCalibration();
			boolean limitToThreshold = (Analyzer.getMeasurements()&LIMIT)!=0;
			imp.getMask();
			histogram = stats.histogram;
			if (limitToThreshold && histogram.length==256) {
				ImageProcessor ip = imp.getProcessor();
				if (ip.getMinThreshold()!=ImageProcessor.NO_THRESHOLD) {
					int lower = scaleDown(ip, ip.getMinThreshold());
					int upper = scaleDown(ip, ip.getMaxThreshold());
										for (int i=0; i<lower; i++)
						histogram[i] = 0;
					for (int i=upper+1; i<256; i++)
						histogram[i] = 0;
				}
			}
	
			
			lut = imp.createLut();
			int type = imp.getType();
			boolean fixedRange = type==ImagePlus.GRAY8 || type==ImagePlus.COLOR_256 || type==ImagePlus.COLOR_RGB;
			ImageProcessor ip = this.imp.getProcessor();
			boolean color = !(imp.getProcessor() instanceof ColorProcessor) && !lut.isGrayscale();
			if (color)
				ip = ip.convertToRGB();
			drawHistogram(ip, fixedRange);
			if (color)
				this.imp.setProcessor(null, ip);
			else
				this.imp.updateAndDraw();
		}
	
		public void setup() {
			Panel buttons = new Panel();
			buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
			list = new Button("List");
			list.addActionListener(this);
			buttons.add(list);
			copy = new Button("Copy");
			copy.addActionListener(this);
			buttons.add(copy);
			log = new Button("Log");
			log.addActionListener(this);
			buttons.add(log);
			Panel valueAndCount = new Panel();
			valueAndCount.setLayout(new GridLayout(2, 1));
			value = new Label("                  "); //21
			value.setFont(new Font("Monospaced", Font.PLAIN, 18));
			valueAndCount.add(value);
			count = new Label("                  ");
			count.setFont(new Font("Monospaced", Font.PLAIN, 18));
			valueAndCount.add(count);
			buttons.add(valueAndCount);
			add(buttons);
			pack();
		}
	
		public void mouseMoved(int x, int y) {
			if (value==null || count==null)
				return;
			if ((frame!=null)  && x>=frame.x && x<=(frame.x+frame.width)) {
				x = x - frame.x;
				if (x>255) x = 255;
				int index = (int)(x*((double)histogram.length)/HIST_WIDTH);
				value.setText("  Value: " + ResultsTable.d2s(cal.getCValue(stats.histMin+index*stats.binSize), digits));
				count.setText("  Count: " + histogram[index]);
			} else {
				value.setText("");
				count.setText("");
			}
		}
		
		protected void drawHistogram(ImageProcessor ip, boolean fixedRange) {
			int x, y;
			int maxCount2 = 0;
			int mode2 = 0;
			int saveModalCount;
					
			ip.setColor(Color.black);
			ip.setLineWidth(1);
			decimalPlaces = Analyzer.getPrecision();
			digits = cal.calibrated()||stats.binSize!=1.0?decimalPlaces:0;
			saveModalCount = histogram[stats.mode];
			for (int i = 0; i<histogram.length; i++)
			if ((histogram[i] > maxCount2) && (i != stats.mode)) {
				maxCount2 = histogram[i];
				mode2 = i;
			}
			newMaxCount = stats.maxCount;
			if ((newMaxCount>(maxCount2 * 2)) && (maxCount2 != 0)) {
				newMaxCount = (int)(maxCount2 * 1.5);
				//histogram[stats.mode] = newMaxCount;
			}
			if (IJ.shiftKeyDown()) {
				logScale = true;
				drawLogPlot(yMax>0?yMax:newMaxCount, ip);
			}
			drawPlot(yMax>0?yMax:newMaxCount, ip);
			histogram[stats.mode] = saveModalCount;
			x = XMARGIN + 1;
			y = YMARGIN + HIST_HEIGHT + 5;


		//	lut.drawUnscaledColorBar(ip, x-1, y,  HIST_WIDTH, BAR_HEIGHT);
			
			 byte[] rLUT,gLUT,bLUT;

			rLUT = new byte[256];
		            gLUT = new byte[256];
		            bLUT = new byte[256];


			rLUT = lut.getReds();
			gLUT= lut.getGreens();
			bLUT = lut.getBlues();

//lut.drawColorBar(x-1, y, 64, BAR_HEIGHT);
			
			int value = 0;
//			ip.setColor(rLUT[0], gLUT[0], bLUT[0]);
			ip.setColor(new Color(rLUT[0]&0xff, gLUT[0]&0xff, bLUT[0]&0xff));
			int x3=0;


			
			for (int x2=0; x2<HIST_WIDTH; x2++) 
				{
				 x3 = (int)Math.round((double)x2*(255/(double)HIST_WIDTH));
				ip.setColor(new Color(rLUT[x3]&0xff, gLUT[x3]&0xff, bLUT[x3]&0xff));
				ip.drawLine(x2+x-1,y+1, x2+x-1, y+BAR_HEIGHT-1);
				
				}

			ip.setColor(new Color(rLUT[0]&0xff, gLUT[0]&0xff, bLUT[0]&0xff));
			ip.drawLine(x-2, y , x+HIST_WIDTH-1, y);
			ip.drawLine(x-2, y+BAR_HEIGHT , x+HIST_WIDTH-1, y+BAR_HEIGHT );

ip.drawLine(x-2, y , x-2, y+BAR_HEIGHT);
ip.drawLine(x+HIST_WIDTH-1, y , x+HIST_WIDTH-1, y+BAR_HEIGHT);


		//add border
		
		//	for (int x1=0; x1<width; x1++)
		//		{ip.putPixelValue(x1+xLoc, yLoc, max/2);
		//		ip.putPixelValue(x1+xLoc, yLoc+height, max/2);
		//		}
		//	for (int y1=0; y1<height; y1++)
		//		{ip.putPixelValue(xLoc, yLoc+y1, max/2);
		//		ip.putPixelValue(xLoc+width-1, yLoc+y1, max/2);
		//		}
			


			y += BAR_HEIGHT+15;
			drawText(ip, x, y, fixedRange);
		}
	
		   
		/** Scales a threshold level to the range 0-255. */
		int scaleDown(ImageProcessor ip, double threshold) {
			double min = ip.getMin();
			double max = ip.getMax();
			if (max>min)
				return (int)(((threshold-min)/(max-min))*255.0);
			else
				return 0;
		}
	
		void drawPlot(int maxCount, ImageProcessor ip) {
			if (maxCount==0) maxCount = 1;
			frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
			ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
			int index, y;
			for (int i = 0; i<HIST_WIDTH; i++) {
				index = (int)(i*(double)histogram.length/HIST_WIDTH); 
				y = (int)((double)HIST_HEIGHT*histogram[index])/maxCount;
				if (y>HIST_HEIGHT)
					y = HIST_HEIGHT;
				ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
			}
		}
			
		void drawLogPlot (int maxCount, ImageProcessor ip) {
			frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
			ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
			double max = Math.log(maxCount);
			ip.setColor(Color.gray);
			int index, y;
			for (int i = 0; i<HIST_WIDTH; i++) {
				index = (int)(i*(double)histogram.length/HIST_WIDTH); 
				y = histogram[index]==0?0:(int)(HIST_HEIGHT*Math.log(histogram[index])/max);
				if (y>HIST_HEIGHT)
					y = HIST_HEIGHT;
				ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
			}
			ip.setColor(Color.black);
		}
			
		void drawText(ImageProcessor ip, int x, int y, boolean fixedRange) {
			ip.setFont(new Font("SansSerif",Font.BOLD,14));
			ip.setAntialiasedText(true);
			double hmin = cal.getCValue(stats.histMin);
			double hmax = cal.getCValue(stats.histMax);
			double range = hmax-hmin;
			String units = "ns";
			String axisMin = IJ.d2s(hmin,2);
			String axisMax = IJ.d2s(hmax,2);
			
			if (hmax>100)
				{
				//units = "ps";
				axisMin=IJ.d2s(hmin/1000,2);
				axisMax= IJ.d2s(hmax/1000,2);					
				}
			if (fixedRange&&!cal.calibrated()&&hmin==0&&hmax==255)
				range = 256;

			
			ip.drawString(axisMin , x - (ip.getStringWidth(axisMin)/2), y+6);

			ip.drawString(axisMax, x + HIST_WIDTH - (ip.getStringWidth(axisMax)/2), y+6);

			ip.setFont(new Font("SansSerif",Font.BOLD,18));
			String axisLabel = "Lifetime (" +units+")";
			ip.drawString(axisLabel , x +  HIST_WIDTH/2 - (ip.getStringWidth(axisLabel )/2), y+8);

			double binWidth = range/stats.nBins;
			binWidth = Math.abs(binWidth);
			boolean showBins = binWidth!=1.0 || !fixedRange;
			int col1 = XMARGIN + 5;
			int col2 = XMARGIN + HIST_WIDTH/2;
			int row1 = y+25;
			if (showBins) row1 -= 8;
			int row2 = row1 + 15;
			int row3 = row2 + 15;
			int row4 = row3 + 15;
		//	ip.drawString("Count: " + stats.pixelCount, col1, row1);
		//	ip.drawString("Mean: " + d2s(stats.mean), col1, row2);
		//	ip.drawString("StdDev: " + d2s(stats.stdDev), col1, row3);
		//	ip.drawString("Mode: " + d2s(stats.dmode) + " (" + stats.maxCount + ")", col2, row3);
		//	ip.drawString("Min: " + d2s(stats.min), col2, row1);
		//	ip.drawString("Max: " + d2s(stats.max), col2, row2);
			
			if (showBins) {
				ip.drawString("Bins: " + d2s(stats.nBins), col1, row4);
				ip.drawString("Bin Width: " + d2s(binWidth), col2, row4);
			}
		}
	
		String d2s(double d) {
			if (d==Double.MAX_VALUE||d==-Double.MAX_VALUE)
				return "0";
			else if (Double.isNaN(d))
				return("NaN");
			else if (Double.isInfinite(d))
				return("Infinity");
			else if ((int)d==d)
				return ResultsTable.d2s(d,0);
			else
				return ResultsTable.d2s(d,decimalPlaces);
		}
		
		int getWidth(double d, ImageProcessor ip) {
			return ip.getStringWidth(d2s(d));
		}
	
		protected void showList() {
			StringBuffer sb = new StringBuffer();
			String vheading = stats.binSize==1.0?"value":"bin start";
			if (cal.calibrated() && !cal.isSigned16Bit()) {
				for (int i=0; i<stats.nBins; i++)
					sb.append(i+"\t"+ResultsTable.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogram[i]+"\n");
				TextWindow tw = new TextWindow(getTitle(), "level\t"+vheading+"\tcount", sb.toString(), 200, 400);
			} else {
				for (int i=0; i<stats.nBins; i++)
					sb.append(ResultsTable.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogram[i]+"\n");
				TextWindow tw = new TextWindow(getTitle(), vheading+"\tcount", sb.toString(), 200, 400);
			}
		}
		
		protected void copyToClipboard() {
			Clipboard systemClipboard = null;
			try {systemClipboard = getToolkit().getSystemClipboard();}
			catch (Exception e) {systemClipboard = null; }
			if (systemClipboard==null)
				{IJ.error("Unable to copy to Clipboard."); return;}
			IJ.showStatus("Copying histogram values...");
			CharArrayWriter aw = new CharArrayWriter(stats.nBins*4);
			PrintWriter pw = new PrintWriter(aw);
			for (int i=0; i<stats.nBins; i++)
				pw.print(ResultsTable.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogram[i]+"\n");
			String text = aw.toString();
			pw.close();
			StringSelection contents = new StringSelection(text);
			systemClipboard.setContents(contents, this);
			IJ.showStatus(text.length() + " characters copied to Clipboard");
		}

		protected void copyImageToClipboard() {
			Clipboard systemClipboard = null;
			try {systemClipboard = getToolkit().getSystemClipboard();}
			catch (Exception e) {systemClipboard = null; }
			if (systemClipboard==null)
				{IJ.error("Unable to copy to Clipboard."); return;}
			IJ.showStatus("Copying histogram values...");
			CharArrayWriter aw = new CharArrayWriter(stats.nBins*4);
			PrintWriter pw = new PrintWriter(aw);
			for (int i=0; i<stats.nBins; i++)
				pw.print(ResultsTable.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogram[i]+"\n");
			String text = aw.toString();
			pw.close();
			StringSelection contents = new StringSelection(text);
			systemClipboard.setContents(contents, this);
			IJ.showStatus(text.length() + " characters copied to Clipboard");
		}
		
		void replot() {
			logScale = !logScale;
			ImageProcessor ip = this.imp.getProcessor();
			frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
			ip.setColor(Color.white);
			ip.setRoi(frame.x-1, frame.y, frame.width+2, frame.height);
			ip.fill();
			ip.resetRoi();
			ip.setColor(Color.black);
			if (logScale) {
				drawLogPlot(yMax>0?yMax:newMaxCount, ip);
				drawPlot(yMax>0?yMax:newMaxCount, ip);
			} else
				drawPlot(yMax>0?yMax:newMaxCount, ip);
			this.imp.updateAndDraw();
		}
		
		/*
		void rescale() {
			Graphics g = img.getGraphics();
			plotScale *= 2;
			if ((newMaxCount/plotScale)<50) {
				plotScale = 1;
				frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
				g.setColor(Color.white);
				g.fillRect(frame.x, frame.y, frame.width, frame.height);
				g.setColor(Color.black);
			}
			drawPlot(newMaxCount/plotScale, g);
			//ImageProcessor ip = new ColorProcessor(img);
			//this.imp.setProcessor(null, ip);
			this.imp.setImage(img);
		}
		*/
		
		public void actionPerformed(ActionEvent e) {
			Object b = e.getSource();
			if (b==list)
				showList();
			else if (b==copy)
				copyToClipboard();
			else if (b==log)
				replot();
		}
		
		public void lostOwnership(Clipboard clipboard, Transferable contents) {}
		
		public int[] getHistogram() {
			return histogram;
		}
	
		public double[] getXValues() {
			double[] values = new double[stats.nBins];
			for (int i=0; i<stats.nBins; i++)
				values[i] = cal.getCValue(stats.histMin+i*stats.binSize);
			return values;
		}
	
	} // inner class HistogramWindow




	
} // Histogram_Window class
	
