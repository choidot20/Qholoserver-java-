
// JAILoader.java
// Andrew Davison, May 2014, ad@fivedots.coe.psu.ac.th

/* Simple use of JAI to test if JAI is installed correctly.
   Use JAI to load and display an image.
*/


import javax.swing.*;
import java.awt.*;
 
import javax.media.jai.*;
import java.awt.image.renderable.ParameterBlock;
import com.sun.media.jai.widget.DisplayJAI;


public class JAILoader extends JFrame 
{

  public JAILoader(String fnm)
  {
    super("JAI Loader: " + fnm);

    // load the image
    ParameterBlock pb = new ParameterBlock();
    pb.add(fnm);
    PlanarImage plim = JAI.create("fileload", pb);
    // PlanarImage plim = JAI.create("fileload", fnm);

    // print information about the image
    System.out.println("Dimensions: " + plim.getWidth()+ "x" + plim.getHeight() +
                     "; Bands:" + plim.getNumBands());

	  Container c = getContentPane();
	  c.add( new DisplayJAI(plim) );    // display in a panel

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setResizable(false);
    pack(); 
    setVisible(true);
  }  // end of JAILoader()



  public static void main(String[] args)
  {  
    if (args.length != 1)
      System.out.println("Usage: run JAILoader <image fnm>");
    else
      new JAILoader(args[0]);
  }  // end of main()

}  // end of JAILoader class

 