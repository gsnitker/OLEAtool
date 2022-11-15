import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class My_Plugin implements PlugIn {

	public void run(String arg) {
		String a= "B";
		int b = a.charAt(0);
		IJ.showMessage("test", a + "- "+ b);

	}

}
