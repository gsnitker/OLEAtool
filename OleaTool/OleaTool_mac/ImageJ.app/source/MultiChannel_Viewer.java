/*    MultiChannelViewer (C) Harald Hutter 
*
*      generates a dynamic display of a multi-channel 3D-stack 
*      with individual control of channel visibility and variable extended focus
*      
*      ImageJ plugin
*      Version : 1.0
*      created : February 12, 2004 
*
*      Author:  Harald Hutter
*      email:    hutter@sfu.ca
*
*     uses multi-slider window functions from 'HyperVolume_Browser.java' version 1.0 by Patrick Pirrotte
*
*     MultiChannel_Viewer should be called with an 8-bit image stack in the active window.
*     The stack should be a multi-channel (2-5 channels) 3D-stack with the following order of images:
*     all slices of the first channel, then all slices of the second channel...all slices of the last channel.
*     MultiChannel_Viewer opens a new window for the display of an RGB overlay of all channels.
*     The window contains several sliders for the control of the following parameters:
*        1. slider: navigate through the slices of the stack
*        2. slider: control extended focus (uses zProjector with its different projection methods)
*        3. -7. slider: adjust visibility of the individual channels (from 0% to 100%)
*     optionally xz and yz cross sections are displayed as separate images.  
*     In this case channel visibility can only be turned on and off via checkboxes.
*     3 sliders are used here to navigate through the stack in  z, x, y directions 
*     Colors for the individual channels and the method of choice for the z-projection can be selected 
*     in a dialog box after start of the plugin
*/

import ij.*;
import ij.plugin.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;



/* MultiChannelViewer class begin*/

public class MultiChannel_Viewer implements PlugIn {
    
    static int GRAY = 0;
    static int RED = 1;
    static int GREEN = 2;
    static int BLUE = 3;
    static int CYAN= 4;
    static int YELLOW = 5;
    static int PURPLE = 6;
    static int MAX_STACKS = 7;
    static int VIS_MAX = 100;                              // value corresponding to 100% visibility of a channel
                                                           // note: drawinfo expects this to be 100 (simply adds '%' after the value)
    int textgap = 40;                                      // extra space on the right side of a scrollbar to display text
    private int[] chColor = new int[MAX_STACKS];           // channel color, see static ints above
    private int[] chVis = new int[MAX_STACKS];             // channel visibility, ranges from 0 (invisible) to 100 (fully visible)
    private int[] colorStart = new int[MAX_STACKS];        // stores the number of the first frame for each color
    private int w, h, z;                                   // width,height and no of slices per channel
    private int stackSize, imgtype;                        // total number of images in the stack, image type
    private ImageStack stack2, stackHor, stackVert;        // local copy of the image stack, xz and yz stacks
    private ImagePlus dispImg;                             // image used in the display window
    private int channels= 0;                               // number of channels used, slices per channel
    private int projSlices = 1;                            // no of slices for zprojection
    private int ScrollbarLimit;                            // limit for scrollbars controlling channel visibility
    private int defaultProjMethod;                         // default projection method used by zProjector
    private int startW, startH, startZ, endW, endH, endZ;  // begin and end for zProjections for xy, xz and yz stacks
    private int currentW, currentH, currentZ;              // current position in stack according to slider position

     static public String[] projmethods = { "Average Intensity", "Max Intensity", "Min Intensity", "Sum Slices", "Standard deviation"};      
     static public String[] pmShort = { "AvgProj", "MaxProj", "MinProj", "SumProj", "StdProj"};      
     static public String[] colors = {"gray", "red", "green", "blue", "cyan", "yellow", "purple" };
 
     private boolean showCrossSectionWindows;              // true, if cross section windows are displayed (set in the start dialog)

    ImagePlus imp1;            // original image
    ImageStack stack1;


/* entry point of plugin, check for args, if not present show dialog*/
 
    public void run(String arg) {
            
    imp1 = WindowManager.getCurrentImage();
    if ((imp1==null) || (imp1.getStackSize()==0))  {
         IJ.error("No stack selected!");
        return;
    }
    stack1 = imp1.getStack();
    stackSize = stack1.getSize();
    imgtype = imp1.getType();
    if (imgtype != ImagePlus.GRAY8) {
         IJ.error("Can only work with 8 bit grayscale stack");
        return;
    }
    if (!arg.equals("")) {
         IJ.error("calling with parameters not implemented");
        return;
    }
    else {        // get Parameters (no. of channels, colors, projection method)

        GenericDialog gd = new GenericDialog("Parameter definition");
                    gd.addMessage("Stack consists of "+imp1.getStackSize()+" slices");
         gd.addNumericField("How many channels?   ", channels, 0);
        gd.addChoice("z projection method:   ", projmethods, projmethods[1]);
        gd.addCheckbox("show xz and yz cross section windows", true);
        gd.addMessage("");
        gd.addChoice("channel 1   ", colors, colors[0]);
        gd.addChoice("channel 2   ", colors, colors[1]);
        gd.addChoice("channel 3   ", colors, colors[2]);
        gd.addChoice("channel 4   ", colors, colors[3]);
        gd.addChoice("channel 5   ", colors, colors[4]);
                    gd.showDialog();
                    if (gd.wasCanceled())
            return;
                   channels = (int) gd.getNextNumber();
        if (channels < 2 || channels>5) {
            IJ.error("Currently only 2-5 channels are supported.");
            return;
        }
               z = stackSize / channels;
        if ((stackSize % channels) != 0){
             IJ.error("Stacksize - channel number mismatch");
            return;
        }
        defaultProjMethod = gd.getNextChoiceIndex();    // get method for zProjector
        showCrossSectionWindows = gd.getNextBoolean();
        for(int i=0;i<7;i++) {
            colorStart[i] = 0;     // colorStart[0] is the first frame of the channel displayed  (0 if corresponding color is not used)
            chVis[i] = 0;
        }
        for(int i=0;i<channels;i++) {
            chColor[i] = gd.getNextChoiceIndex();
            if (colorStart[chColor[i]] != 0) {         // color was already used for another channel
                       IJ.error("Can't use the same color for different channels");
                            return;
            }
            colorStart[chColor[i]] = z * i + 1;        // find out where first slice of this channel is
            chVis[chColor[i]] = VIS_MAX ;              // set channel visibility to 100%
        }
    }
    
    if (imp1 instanceof ImagePlus && imp1.getStackSize() > 1)
        initFrame();
    IJ.register(MultiChannel_Viewer.class);
    }
 

/* Frame initialisation method*/
    
    void initFrame(){
       
    w = imp1.getWidth();
    h = imp1.getHeight();
    stack2 = new ImageStack(w,h);        

    for (int i=1; i<=stackSize; i++) {            // copy original stack
                IJ.showStatus(i+"/"+stackSize);
        stack2.addSlice(null,stack1.getProcessor(i));
         IJ.showProgress((double)i/stackSize);
    }
    startW = endW = currentW = w / 2;
    startH = endH = currentH = h / 2;
    startZ = endZ = currentZ = z / 2;
    projSlices = 1;
    dispImg = new ImagePlus(imp1.getTitle()+" dynamic overlay", stack2.getProcessor(1));
    dispImg.setProcessor(null, getMergedImage(stack2,startZ, endZ));        // get the overlay of the first slice and
    CrossSectionCanvas cc = new CrossSectionCanvas(dispImg);
    new CrossSectionWindow(dispImg, cc);                                    // display in new custom window
    }


 /* CustomLayout class begin */
   
    class CustomLayout extends ImageLayout implements LayoutManager {
        
        int hgap, vgap;
         ImageCanvas ic;

        CustomLayout(ImageCanvas ic) {
            super(ic);
            this.hgap = this.vgap = 5;
            this.ic = ic;
        }
        

    public void layoutContainer(Container target) {
        
        Insets insets = target.getInsets();
        Dimension d = target.getSize();
        int preferredImageWidth = d.width - (insets.left + insets.right + hgap*2);
        int preferredImageHeight = d.height - (insets.top + insets.bottom + vgap*2);
        ic.setSize(preferredImageWidth, preferredImageHeight);
        int maxwidth = d.width - (insets.left + insets.right + hgap*2);
        int maxheight = d.height - (insets.top + insets.bottom + vgap*2);
        int nmembers = target.getComponentCount();
        Dimension psize = preferredLayoutSize(target);
        int x = insets.left + hgap + (d.width - psize.width)/2;
        int y = 0;
        int colw = 0;
        int cboxCounter = 0;
        boolean isCheckbox = false;

        for (int i=0; i<nmembers; i++) {
            Component m = target.getComponent(i);
            d = m.getPreferredSize();
            if (m instanceof Checkbox) {
                cboxCounter += 1;
                isCheckbox = true;
            }
            else
                isCheckbox = false;
            if (m instanceof Scrollbar) {
                int scrollbarWidth = target.getComponent(0).getPreferredSize().width - textgap;
                Dimension minSize = m.getMinimumSize();
            if (scrollbarWidth<minSize.width-textgap) scrollbarWidth = minSize.width-textgap;
            m.setSize(scrollbarWidth, d.height);
        } else
            m.setSize(d.width, d.height);
        if (cboxCounter < 2 || isCheckbox == false) {                // assumes that all checkboxes fit into one line
            if (y > 0) y += vgap;
             y += d.height;
        }
        colw = Math.max(colw, d.width);
        }
        moveComponents(target, x, insets.top + vgap, colw, maxheight - y, nmembers);

    } // end of layoutContainer()


    private void moveComponents(Container target, int x, int y, int width, int height, int nmembers) {

    int x1 = 0;
    int  x2 = 0;
    int  y1;

    y += height / 2;
    y1 = y;
    for (int i=0; i<nmembers; i++) {
        Component m = target.getComponent(i);
         Dimension d = m.getSize();
        if (i== 0)
            x2 = x1 = x + (width - d.width)/2;
        m.setLocation(x2, y);
        if (i < nmembers - 1) {
            Component m1 = target.getComponent(i+1);
            if (m1 instanceof Checkbox && m instanceof Checkbox) {
                x2 += hgap + d.width;
                y = y1;
            }
            else {
                y += vgap + d.height;
                y1 = y;
                x2 = x1;
            }
        }
    }
    }  // end of moveComponents()


    public Dimension preferredLayoutSize(Container target) {

    Dimension dim = new Dimension(0,0);
    int nmembers = target.getComponentCount();
    int cboxCounter = 0;
    boolean isCheckbox = false;

    for (int i=0; i<nmembers; i++) {
        Component m = target.getComponent(i);
        if (m instanceof Checkbox) {
            cboxCounter += 1;
            isCheckbox = true;
        }
        else
            isCheckbox = false;
        Dimension d = m.getPreferredSize();
        dim.width = Math.max(dim.width, d.width);
        if (cboxCounter < 2 || isCheckbox == false) {        // assumes that all checkboxes fit into one line
            if (i>0) dim.height += vgap;
            dim.height += d.height;
        }
    }
    Insets insets = target.getInsets();
    dim.width += insets.left + insets.right + hgap*2;
    dim.height += insets.top + insets.bottom + vgap*2;
    return dim;
    }
    }  // end of preferredLayoutSize()


/* CustomLayout class end*/

 
 
 /* CrossSectionCanvas class begin*/
   
    class CrossSectionCanvas extends ImageCanvas {
        
        CrossSectionCanvas(ImagePlus imp) {
             super(imp);
        }
        
    } 
/* CrossSectionCanvas class end*/
    

/* CrossSectionWindow class begin*/

        class CrossSectionWindow extends ImageWindow implements AdjustmentListener, ItemListener {
        
    private Scrollbar sliceSel1;                     // move in z-direction through the stack
    private Scrollbar sliceSel2;                     // move in x-direction through the stack (used  with xz and yz windows)
    private Scrollbar sliceSel3;                     // move in y-direction through the stack (used   with xz and yz windows)
    private Scrollbar sliceSel4;                     // number of slices used for zProjection
    private Scrollbar[] visSel = new Scrollbar[5];   // to control visibility of channels (used without with xz and yz windows)
 
     private  Checkbox[] cbox = new Checkbox[5];     // used instead of sliders for channel visibility 
                                                     // when xz and yz windows are displayed
   
    private int vis=1;                               // used to store values from visibility sliders
     private ImagePlus i;                            // the image to be displayed
  
      
        /* CrossSectionCanvas constructors, initialisation*/

    CrossSectionWindow(ImagePlus imp){
        super(imp,new CrossSectionCanvas(imp));
    }
        
    CrossSectionWindow(ImagePlus imp, ImageCanvas ic) {
        super(imp, ic);
        i = imp;        
        setLayout(new CustomLayout(ic));
                add(ic);
        addPanel();
    }        
 
       /* adds the Scrollbars and Checkboxes to the custom window, implements Listeners*/

    void addPanel() {
     ScrollbarLimit = 11;
  
     sliceSel1 = new Scrollbar(Scrollbar.HORIZONTAL, currentZ, 1, 1,z + 1);
     sliceSel1.addAdjustmentListener(this);
    int blockIncrement = z/10;
    if (blockIncrement<1)
         blockIncrement = 1;
     sliceSel1.setUnitIncrement(1);
    sliceSel1.setBlockIncrement(blockIncrement);
    add(sliceSel1);

    if (showCrossSectionWindows == true) {
         sliceSel2 = new Scrollbar(Scrollbar.HORIZONTAL, currentW, 1, 1,w + 1);
         sliceSel2.addAdjustmentListener(this);
        blockIncrement = w/10;
        if (blockIncrement<1)
             blockIncrement = 1;
         sliceSel2.setUnitIncrement(1);
        sliceSel2.setBlockIncrement(blockIncrement);
        add(sliceSel2);
        sliceSel3 = new Scrollbar(Scrollbar.VERTICAL, currentH, 1, 1,h + 1);
         sliceSel3.addAdjustmentListener(this);
        blockIncrement = h/10;
        if (blockIncrement<1)
             blockIncrement = 1;
         sliceSel3.setUnitIncrement(1);
        sliceSel3.setBlockIncrement(blockIncrement);
        add(sliceSel3, BorderLayout.WEST);

        for (int i = 0; i < channels; i++) {
            cbox[i] = new Checkbox(colors[chColor[i]],  true);
            cbox[i].addItemListener(this);
            add(cbox[i]);
        }
         sliceSel4 = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1,z + 1);
         sliceSel4.addAdjustmentListener(this);
         blockIncrement = z/10;
        if (blockIncrement<1)
             blockIncrement = 1;
        sliceSel4.setUnitIncrement(1);
        sliceSel4.setBlockIncrement(blockIncrement);
        add(sliceSel4);
    }
    else {
          sliceSel4 = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, z + 1);
         sliceSel4.addAdjustmentListener(this);
         sliceSel4.setUnitIncrement(1);
        sliceSel4.setBlockIncrement(blockIncrement);
        add(sliceSel4);
        for (int i = 0; i < channels; i++) {
            visSel[i] = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, ScrollbarLimit + 1);
                visSel[i].addAdjustmentListener(this);
            visSel[i].setUnitIncrement(1);
            visSel[i].setBlockIncrement(10);
            add(visSel[i]);
               }
    }
    pack();
    show(); 
    i.setSlice(1);
    WindowManager.addWindow(this);
 
        }  // end of addPanel()
  
    
        /* Scrollbar Listener*/

    public void adjustmentValueChanged(java.awt.event.AdjustmentEvent aE) {

    int offset1, offset2;
    
    if (aE.getSource()==sliceSel1)
        currentZ = sliceSel1.getValue();
    if (aE.getSource()==sliceSel2)
        currentW = sliceSel2.getValue();
    if (aE.getSource()==sliceSel3)
        currentH = sliceSel3.getValue();
    if (aE.getSource()==sliceSel4)
        projSlices = sliceSel4.getValue();
    for (int i = 0; i < channels; i++) 
        if (aE.getSource()==visSel[i]){
            vis = visSel[i].getValue();
            chVis[chColor[i]] = (VIS_MAX *  (ScrollbarLimit  - vis)) / (ScrollbarLimit - 1);
                }
     offset1 = offset2 = (projSlices - 1) / 2;
    if ((projSlices % 2) == 0)
        offset2++;
    startW = currentW - offset1;
    endW = currentW + offset2;
    if (currentW - offset1 < 1) {
        startW = 1;
        currentW = startW + offset1;
        endW = currentW + offset2;
    }
    if (endW > w) {
        endW = w;
        currentW = endW - offset2;
        startW = currentW - offset1;
    }
    startH = currentH - offset1;
    endH = currentH + offset2;
    if (startH < 1) {
        startH = 1;
        currentH = startH + offset1;
        endH = currentH + offset2;
    }
    if (endH > h) {
        endH = h;
        currentH = endH - offset2;
        startH = currentH - offset1;
    }
    startZ = currentZ - offset1;
    endZ = currentZ + offset2;
    if (startZ < 1) {
        startZ = 1;
        currentZ = startZ + offset1;
        endZ = currentZ + offset2;
    }
    if (endZ > z) {
        endZ = z;
        currentZ = endZ - offset2;
        startZ = currentZ - offset1;
    }
    i.setProcessor(null, getMergedImage(stack2, startZ, endZ));        
    i.updateAndDraw();
 
      } // end of adjustmentValueChanged()


    /* checkbox listener */
    
    public void itemStateChanged(ItemEvent e) {

        Object eItem = e.getItemSelectable();
        for (int i = 0; i < channels; i++)
            if (eItem == cbox[i])
                chVis[chColor[i]] = (e.getStateChange() == ItemEvent.SELECTED) ? VIS_MAX: 0;
        i.setProcessor(null, getMergedImage(stack2, startZ, endZ));        
        i.updateAndDraw();

    }

 
    /* drawinfo overrides method from ImageWindow to display parameters controlled with the additional scrollbars
    * original code from Wayne Rasband, modified by Patrick Pirrotte, further adapted by me (Harald Hutter) */
        
    public void drawInfo(Graphics g) {
    int TEXT_GAP = 0;

    String s="";
    Insets insets = super.getInsets();
  
    s +="z="+ Integer.toString(currentZ) + "/"+Integer.toString(z)+";  ";
    if (showCrossSectionWindows == true) {
        s +="x="+ Integer.toString(currentW) + "/"+Integer.toString(w)+";  ";
        s +="y="+ Integer.toString(currentH) + "/"+Integer.toString(h)+";  ";
    }
    s +="nProj= "+ Integer.toString(projSlices) +"  ("+pmShort[defaultProjMethod]+ ");  ";
    for (int i = 0; i < channels;  i++) 
        s += colors[chColor[i]]+":"+ Integer.toString(chVis[chColor[i]]) + "%; ";

    Calibration cal = imp.getCalibration();
    if (cal.pixelWidth!=1.0 || cal.pixelHeight!=1.0)
        s += IJ.d2s(imp.getWidth()*cal.pixelWidth,2) + "x" + IJ.d2s(imp.getHeight()*cal.pixelHeight,2)
        + " " + cal.getUnits() + " (" + imp.getWidth() + "x" + imp.getHeight() + "); ";
    else
        s += imp.getWidth() + "x" + imp.getHeight() + " pixels; ";
    int size = (imp.getWidth()*imp.getHeight()*imp.getStackSize())/256;      // image has to be RGB color
    s += "RGB; " + size + "K;";
      g.drawString(s, 5, insets.top+TEXT_GAP);

    addScrollbarLabel(g, sliceSel1, "z-axis");
    addScrollbarLabel(g, sliceSel4, "nProj.");
    if (showCrossSectionWindows == true) {
        addScrollbarLabel(g, sliceSel2, "x-axis");
        addScrollbarLabel(g, sliceSel3, "y-axis");
    }
    else 
        for (int i = 0; i < channels; i++)
            addScrollbarLabel(g, visSel[i], colors[chColor[i]]);

    }
        // drawinfo() ends
        

    private  void addScrollbarLabel(Graphics g, Scrollbar s, String label) {
        int LABEL_GAP = 5;
        Rectangle rs = s.getBounds();
        g.drawString(label, rs.x + rs.width + LABEL_GAP, rs.y + rs.height - LABEL_GAP);
    }
            
}
 /* CustomWindow class end*/


    /*  generate an overlay of different channels with different visibility */
    
    public ColorProcessor getMergedImage(ImageStack stack, int startSlice, int endSlice) {
    
        ColorProcessor cp, cpHor, cpVert;
        ImageStack zStack = new ImageStack(stack.getWidth(), stack.getHeight());
        ImageStack mergeStack = new  ImageStack(stack.getWidth(), stack.getHeight());
        int slices = endSlice - startSlice + 1;
        
        for(int ic = 0; ic < channels; ic++) {                            // for all the channels used
            for (int i = startSlice; i <= endSlice; i++) 
                       zStack.addSlice(null, stack.getProcessor(ic * z + i));
           }
        mergeStack = (slices > 1) ? doZProjection(zStack, channels, slices) : zStack;
         if (showCrossSectionWindows == false) 
            return MergeImage(mergeStack);
        else {                                // has to generate xz and yz cross sections
            cp = MergeImage(mergeStack);
            createCrossSectionStack(stack);
            ImageStack mHorStack = new ImageStack(stackHor.getWidth(), stackHor.getHeight());
            ImageStack mVertStack = new ImageStack(stackVert.getWidth(), stackVert.getHeight());
            mHorStack = (slices > 1) ? doZProjection(stackHor, channels, slices) : stackHor;
            cpHor = MergeImage(mHorStack);
            mVertStack = (slices > 1) ? doZProjection(stackVert, channels, slices) : stackVert;
            cpVert = MergeImage(mVertStack);
            return (getCrossSectionImage(cp, cpHor, cpVert));
        }
    }
    

    // merge up to 7 color channels into one RGB image, optionally with different visibilities 
    // values between 0 (invisible) and VIS_MAX (fully visible)

    public ColorProcessor MergeImage (ImageStack mergeStack){

        byte[] pB;                          // source pointer
        int w1 = mergeStack.getWidth();
        int h1 = mergeStack.getHeight();
        int size = w1 * h1;   
        byte[] red = new byte[size];              // for the red channel
        byte[] green = new byte[size];            // for the green channel
        byte[] blue = new byte[size];             // for the blue channel
        ColorProcessor rgb = new ColorProcessor(w1,h1);

        if (colorStart[GRAY] != 0 && chVis[GRAY] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[GRAY] - 1) / z + 1);
            mergeChannel(red, pB, size, GRAY);
            mergeChannel(green, pB, size, GRAY);
            mergeChannel(blue, pB, size, GRAY);
        }    
        if (colorStart[RED] != 0 && chVis[RED] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[RED] - 1) / z + 1);
            mergeChannel(red, pB, size, RED);
        }    
        if (colorStart[GREEN] != 0 && chVis[GREEN] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[GREEN] - 1) / z + 1);
            mergeChannel(green, pB, size, GREEN);
        }    
        if (colorStart[BLUE] != 0 && chVis[BLUE] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[BLUE] - 1) / z + 1);
            mergeChannel(blue, pB, size, BLUE);
        }    
        if (colorStart[CYAN] != 0 && chVis[CYAN] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[CYAN] - 1) / z + 1);
            mergeChannel(green, pB, size, CYAN);
            mergeChannel(blue, pB, size, CYAN);
        }    
        if (colorStart[YELLOW] != 0 && chVis[YELLOW] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[YELLOW] - 1) / z + 1);                
            mergeChannel(red, pB, size, YELLOW);
            mergeChannel(green, pB, size, YELLOW);
        }    
        if (colorStart[PURPLE] != 0 && chVis[PURPLE] > 0) {
            pB= (byte[])mergeStack.getPixels((colorStart[PURPLE] - 1) / z + 1);
            mergeChannel(red, pB, size, PURPLE);
            mergeChannel(blue, pB, size, PURPLE);
        }    
                
        rgb.setRGB(red, green, blue);
        return (rgb);
    }
        
    
    // merges one color channel
        
    private void mergeChannel(byte[] target, byte[] channel, int size, int color) {
        
        byte b;
        
        if (chVis[color] == VIS_MAX) {                // channel fully visible, no need for calculations (faster)
            for(int i = 0; i < size; i++) 
                    target[i] |=channel[i];                
        }
        else {
            for(int i = 0; i < size; i++){
                b = (byte) (chVis[color] * ((0xff &channel[i])) / VIS_MAX);     // convert to unsigned (0xff & ....) 
                target[i] |= b;                                                 // and calculate visibility                             
            }
        }
    } // end of mergeChannel()
    
    
    
    /* creates image stacks containing the xz and yz sections for zProjection
        takes parameters for stack and slices to be collected from global variables */
        
    private void createCrossSectionStack(ImageStack stack) {
    
    int[] data = new int[w+h];
    
    stackHor = new ImageStack(w, z, stack.getColorModel());
    ImageProcessor ip1 = stack.getProcessor(1);
     try {
         for (int ch = 0; ch < channels; ch++) {
            for (int i = startH; i<= endH; i++) {
                 ImageProcessor ipHor = ip1.createProcessor(w, z);
                for (int y = 0; y < z; y++) {
                    ImageProcessor ip = stack.getProcessor(ch * z + y + 1);
                    ip.getRow(0, i, data, w);
                    ipHor.putRow(0, y, data, w);
                }
                stackHor.addSlice(null, ipHor);         
            }
        }
    } catch(OutOfMemoryError o) {
        IJ.outOfMemory("createCrossSectionStack()");
        IJ.showProgress(1.0);
    }
    stackVert = new ImageStack(z, h, stack.getColorModel());
    ip1 = stack.getProcessor(1);
    try {
        for (int ch = 0; ch < channels; ch++) {
             for (int i = startW; i <= endW; i++) {
                 ImageProcessor ipVert = ip1.createProcessor(z, h);
                for (int x = 0; x < z; x++) {
                    ImageProcessor ip = stack.getProcessor(ch * z + x+1);
                    ip.getColumn(i, 0, data, h);
                    ipVert.putColumn(x, 0, data, h);
                }
                stackVert.addSlice(null, ipVert);
            }
        }
    } catch(OutOfMemoryError o) {
        IJ.outOfMemory("createCrossSectionStack()");
        IJ.showProgress(1.0);
    }
    } // end of createCrossSectionStack


    /*     generates zProjections expects a stack of images to be projceted
        the stack consists of 'ch' substacks with 'n' slices each, substacks are zProjected separately
        output is a stack containing the projected images */
    
    public ImageStack doZProjection(ImageStack stack, int ch, int n) {

        ImagePlus zimp, projection;
        ZProjector zproj;
    
        zimp = new ImagePlus(null, stack);
        zproj = new ZProjector(zimp);
        ImageStack projStack = zimp.createEmptyStack();
        zproj.setMethod(defaultProjMethod);     
          for (int i = 0; i < ch; i++) {
            zproj.setStartSlice(i * n + 1);
            zproj.setStopSlice((i+1) * n);
             zproj.doProjection();
                    projection =  zproj.getProjection();
             if (defaultProjMethod == zproj.SUM_METHOD || defaultProjMethod == zproj.SD_METHOD) {
                ImageConverter iCon = new ImageConverter(projection);
                iCon.convertToGray8();                // those projection methods produce 16bit gray images
            }
            projStack.addSlice(null, projection.getProcessor());
         }
         return projStack;

    } // end of doZProjection()


    // create a new image containing three windows for the xy, xz and yz cross-sections at position curW, curH, curZ
    
    public ColorProcessor getCrossSectionImage(ColorProcessor cp, ColorProcessor cpHor, ColorProcessor cpVert) {

    ColorProcessor ipNew;
    
    int SPACER = 20; 

    ipNew = new ColorProcessor(w + SPACER+3 + z, h + SPACER+3+ z);
     ipNew.setColor(Toolbar.getForegroundColor());
    ipNew.insert(cp, 1, 1);                        // insert xy image in upper left corner
    ipNew.insert(cpHor, 1, h + SPACER);            // insert xz image below main image
    ipNew.insert(cpVert, w + SPACER, 1);           // insert yz image to the right of main image

    ipNew.drawRect(0, 0, w + SPACER+z+2, h + SPACER+z+2);
    ipNew.drawRect(0, 0, w+2, h+2);
    ipNew.drawRect(0, h+SPACER, w+1, h + SPACER+z+1);
    ipNew.drawRect(w + SPACER, 0, w + SPACER+z+1, h+1);
    if (projSlices == 1) {                         // no zProjection
        ipNew.drawLine(0, currentH, SPACER, currentH);
        ipNew.drawLine(w-SPACER, currentH, w, currentH);
        ipNew.drawLine(currentW, 0, currentW, SPACER);
        ipNew.drawLine(currentW, h-SPACER, currentW, h);
        ipNew.drawLine(0, h + SPACER+currentZ, SPACER, h + SPACER + currentZ);
        ipNew.drawLine(w-SPACER, h + SPACER+currentZ, w , h +SPACER + currentZ);
        ipNew.drawLine(w+SPACER + currentZ, 0, w + SPACER + currentZ, SPACER); 
        ipNew.drawLine(w+SPACER + currentZ, h-SPACER, w + SPACER + currentZ, h ); 
    }
    else {
        ipNew.drawLine(0, startH, SPACER, startH);
        ipNew.drawLine(w-SPACER, startH, w, startH);
        ipNew.drawLine(startW, 0, startW, SPACER);
        ipNew.drawLine(startW, h-SPACER, startW, h);
        ipNew.drawLine(0, h + SPACER+startZ, SPACER, h + SPACER + startZ);
        ipNew.drawLine(w-SPACER, h + SPACER+startZ, w , h +SPACER + startZ);
        ipNew.drawLine(w+SPACER + startZ, 0, w + SPACER +startZ, SPACER); 
        ipNew.drawLine(w+SPACER + startZ, h-SPACER, w + SPACER + startZ, h ); 
        ipNew.drawLine(0, endH, SPACER, endH);
        ipNew.drawLine(w-SPACER, endH, w,endH);
        ipNew.drawLine(endW, 0, endW, SPACER);
        ipNew.drawLine(endW, h-SPACER, endW, h);
        ipNew.drawLine(0, h + SPACER+endZ, SPACER, h + SPACER + endZ);
        ipNew.drawLine(w-SPACER, h + SPACER+endZ, w , h +SPACER + endZ);
        ipNew.drawLine(w+SPACER + endZ, 0, w + SPACER + endZ, SPACER); 
        ipNew.drawLine(w+SPACER + endZ, h-SPACER, w + SPACER + endZ, h ); 
    }
    return ipNew;
    
    } // end of createCrossSectionImage

}
/* MultiChannel_Projector class ends*/
