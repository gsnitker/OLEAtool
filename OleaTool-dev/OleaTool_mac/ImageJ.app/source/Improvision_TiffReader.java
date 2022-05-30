//
// Improvision_TiffReader.java
//

import ij.*;
import ij.io.*;
import ij.plugin.*;
import java.util.*;
import ij.measure.*;

/** Imports a Z series (image stack) from an Improvision TIFF file. */
public class Improvision_TiffReader extends ImagePlus implements PlugIn {

  public void run(String arg) {
    OpenDialog od = new OpenDialog("Open Improvision TIFF...", arg);
    String directory = od.getDirectory();
    String fileName = od.getFileName();
    if (fileName == null) return;

    // open the image normally
    IJ.showStatus("Opening: " + directory + fileName);
    Opener opener = new Opener();
    ImagePlus imp = opener.openImage(directory, fileName);
    if (imp == null) return;
    setStack(fileName, imp.getStack());

    // grab comment
    FileInfo fi = imp.getOriginalFileInfo();
    String comment = fi.description;

    // sanitize line feeds
    comment = comment.replaceAll("\r\n", "\n");
    comment = comment.replaceAll("\r", "\n");

    // extract calibration information
    StringTokenizer st = new StringTokenizer(comment, "\n");
    double xcal = Double.NaN, ycal = Double.NaN, zcal = Double.NaN;
    while (st.hasMoreTokens()) {
      String line = st.nextToken();
      int equals = line.indexOf("=");
      if (equals < 0) continue;
      String key = line.substring(0, equals);
      String value = line.substring(equals + 1);
      if (key.equals("XCalibrationMicrons")) {
        xcal = Double.parseDouble(value);
      }
      else if (key.equals("YCalibrationMicrons")) {
        ycal = Double.parseDouble(value);
      }
      else if (key.equals("ZCalibrationMicrons")) {
        zcal = Double.parseDouble(value);
      }
    }

    // assign calibration
    if (!Double.isNaN(xcal) && !Double.isNaN(ycal) && !Double.isNaN(zcal)) {
      try {
        Calibration c = new Calibration();
        c.pixelWidth = xcal;
        c.setUnit("micron");
        c.pixelHeight = ycal;
        c.setUnit("micron");
        c.pixelDepth = zcal;
        c.setUnit("micron");
        setCalibration(c);
      }
      catch (Exception e) {
        IJ.showStatus("");
        IJ.showMessage("Improvision_TiffReader", ""+e);
        return;
      }
    }

    if (arg.equals("")) show();
  }

}
