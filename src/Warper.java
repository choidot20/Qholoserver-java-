
// Warper.java
// Andrew Davison, May 2014, ad@fivedots.coe.psu.ac.th

/* Allow the user to draw a quadrilateral over an image, which is used
   as the source quadrilateral in a perspective transformation. The destination
   quadrilateral is a rectangle of a specified width and height. The cropped
   image is shown in the right hand panel, and is also
   saved to WARP_FNM.
*/

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

import javax.media.jai.*;
import java.awt.image.renderable.ParameterBlock;



public class Warper extends JFrame implements ActionListener
{
  private static final String IM_DIR = "images/";    // examples directory

  private static final String BOUNDED_FNM = "bounded.png"; 
                   // name if the bounded sourse quad image file

  private static final String WARP_FNM = "warpOut.png"; 
                   // name of warped, cropped image file


  private static final int NUM_CORNERS = 4;

  private static final int WARP_WIDTH = 480;
  private static final int WARP_HEIGHT = 360;


  private BufferedImage sourceIm;

  // GUI elements
  private QuadPanel quadPanel; 
  private ImagePanel imPanel;
  private JTextField widthTF, heightTF;
  private JButton warpButton;



  public Warper(String fnm)
  {
    super("Warper");

    String imFnm = IM_DIR + fnm;
    System.out.println("Processing the image in \"" + imFnm + "\"");
    sourceIm = loadImage(imFnm);

	  Container c = getContentPane();
    c.setLayout( new BorderLayout() );

    quadPanel = new QuadPanel(sourceIm);
	  c.add(quadPanel, BorderLayout.CENTER);

    imPanel = new ImagePanel();
	  c.add(imPanel, BorderLayout.EAST);


    // add controls 
    JPanel ctrls = new JPanel();
    // ctrls.setLayout( new BoxLayout(ctrls, BoxLayout.X_AXIS));

    ctrls.add( new JLabel("Width: "));
    widthTF = new JTextField("" + WARP_WIDTH, 5);
    ctrls.add(widthTF);

    ctrls.add( new JLabel("Height: "));
    heightTF = new JTextField(""+WARP_HEIGHT, 5);
    ctrls.add(heightTF);

    warpButton = new JButton("Warp");
    warpButton.addActionListener(this);
    ctrls.add(warpButton);
    c.add(ctrls, BorderLayout.SOUTH);


    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setResizable(false);
    pack();  
    setLocationRelativeTo(null);  // center the window 
    setVisible(true);
  }  // end of Warper()




  private BufferedImage loadImage(String fn)
  // load the image stored in fn
  {
    BufferedImage im = null;
    try {
      im = ImageIO.read( getClass().getResource(fn));
    }
    catch(Exception e) {
      System.out.println("Could not load " + fn);
      System.exit(0);
    }
    return im;
  }  // end of loadImage()



  public void actionPerformed(ActionEvent e) 
  /* Triggered when the user presses the "Warp" button.
     Apply the perspective transformation using the selected
     quadrilateral defined by the coordinates set in the QuadPanel.

     The warped, cropped image is displayed in the ImagePanel, and also
     saved to WARP_FNM
  */
  {
    // get source and destination quad info from the GUI
    int warpWidth = toInt(widthTF.getText(), WARP_WIDTH);
    int warpHeight = toInt(heightTF.getText(), WARP_HEIGHT);
    System.out.println("\nWarping and cropping to " + warpWidth + " x " + warpHeight);

    if (!quadPanel.hasQuad()) {
      System.out.println("No quadrilateral defined");
      return;
    }

      //         ___________________________________
	  //         |                                 |
	  //         | *                             * |
	  //         | 0                             1 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |        Point[]                  |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         | *                            *  |
      //  	     | 3                            2  |
	  //         -----------------------------------
    
    Point[] coords = quadPanel.getCoords();

    // crop the image so it tightly bounds the quadrilateral
    Rectangle bounds = rectangleBounds(coords);
    System.out.println("Bounds rectangle: " + bounds);

    BufferedImage boundedIm = cropImage(sourceIm, bounds.x, bounds.y, 
                                                  bounds.width, bounds.height);
    saveImage("bounded", boundedIm, BOUNDED_FNM); //KYT removes
     /* then read in again to avoid JAI bug at end of warping
        a cropped image; as discussed in
        https://community.oracle.com/thread/1272280
     */
    boundedIm = loadImage(BOUNDED_FNM); //KYT removes

    // adjust coordinates to apply to bounded image
    for (int i=0; i < NUM_CORNERS; i++) {
      coords[i].x -= bounds.x;
      coords[i].y -= bounds.y;
      System.out.println("Bounded coord: " + coords[i]);
    }

    // convert the quadrilteral into a perspective transform
    PerspectiveTransform persTF = makeWarpTransform(coords, warpWidth, warpHeight);

    // apply perspective tranformation to image
    BufferedImage warpIm = warpImage(boundedIm, persTF);
    if (warpIm == null)
      return;

    //System.out.println("Size of warped image: " + warpIm.getWidth() + " x " + 
    //                                              warpIm.getHeight() );

    // find top-left coordinate of warped image
    Point topLeft = findWarpTopLeft(boundedIm, persTF);
    if (topLeft == null)
      return;

    // saveImage("warped",  warpIm, "warpUncropped.png");
    BufferedImage cropIm = cropWarp(warpIm, topLeft, warpWidth, warpHeight);

    //System.out.println("Size of cropped warped image: " + 
    //                        cropIm.getWidth() + " x " + cropIm.getHeight() );

    imPanel.setImage(cropIm);
    saveImage("warped and cropped", cropIm, WARP_FNM);
  }  // end of actionPerformed()



  private int toInt(String txt, int val)
  // extract an integer from txt, or return val otherwise
  { 
    int i = val;
    try {
      i = Integer.parseInt(txt);
    }
    catch (NumberFormatException x) 
    {  System.out.println("Error : \"" + txt + "\" is not an integer; using " + val);  }
    return i;
  }  // end of toInt()



  private Rectangle rectangleBounds(Point[] coords)
  /* calculate the rectangle bound around the quadrilateral
     defined by the coords[] array
  */
  {
    int xMin = coords[0].x;
    int xMax = coords[0].x;

    int yMin = coords[0].y;
    int yMax = coords[0].y;

    for (int i=1; i < NUM_CORNERS; i++) {
      if (coords[i].x < xMin)
        xMin = coords[i].x;
      if (coords[i].y < yMin)
        yMin = coords[i].y;

      if (coords[i].x > xMax)
        xMax = coords[i].x;
      if (coords[i].y > yMax)
        yMax = coords[i].y;
    }

    return new Rectangle(xMin, yMin, (xMax-xMin), (yMax-yMin));
  }  // end of rectangleBounds



  private PerspectiveTransform makeWarpTransform(Point[] coords, int w, int h)
  /* create a perspective transformation that maps the quadrilateral into
     a w by h rectangle */
  { return PerspectiveTransform.getQuadToQuad(
                     coords[0].x, coords[0].y,   coords[1].x, coords[1].y,        
                     coords[2].x, coords[2].y,   coords[3].x, coords[3].y,        
                     0,0,     w,0,        w,h,      0,h  );
  }



  private BufferedImage warpImage(BufferedImage im, PerspectiveTransform persTF)
  /* Apply the persepctive transformation to the specified image, returned 
     the warped result as a BufferedImage. JAI deals with PlanarImage objects,
     not BufferedImages, so some extra conversion steps are needed.

     The returned image will contain the warped area, and extra details which
     will need to be cropped.
  */
  {
    WarpPerspective warpOp = null;
    try {
      warpOp = new WarpPerspective( persTF.createInverse() );   // invert the transform
    }
    catch(Exception e)
    {  System.out.println("Unable to create warp operation");  }

    if (warpOp == null)
      return null;

    ParameterBlock pb = new ParameterBlock();
    pb.addSource( PlanarImage.wrapRenderedImage(im) );    // BufferedImage --> PlanarImage
    
    pb.add( warpOp ); 
    pb.add( Interpolation.getInstance(Interpolation.INTERP_BILINEAR ));
    // pb.add( Interpolation.getInstance(Interpolation.INTERP_BICUBIC) );    // slower, but smoother
    PlanarImage warpedImage = JAI.create("warp", pb);
    
    BufferedImage warpIm = null;
    try {
      warpIm = warpedImage.getAsBufferedImage();     // PlanarImage --> BufferedImage
    }
    catch (Exception e) 
    { System.out.println("Unable to create warped image: " + e); }
    catch(OutOfMemoryError e)    // quite likely if source image is large
    { System.out.println("Warped image is too large: " + e); }

    return warpIm;
  }  // end of warpImage()



  private Point findWarpTopLeft(BufferedImage boundedIm, PerspectiveTransform persTF)
  /* Apply the perspective transformation to the corner coordinates of the 
     bounded box image to calculate the top-left coordinate of the resulting 
     warped image. I'll use this to crop the warped image.
  */
  { int imWidth = boundedIm.getWidth();
    int imHeight = boundedIm.getHeight();

    Point[] bbCorners = new Point[NUM_CORNERS];
    bbCorners[0] = new Point(0,0);
    bbCorners[1] = new Point(imWidth,0);
    bbCorners[2] = new Point(imWidth, imHeight);
    bbCorners[3] = new Point(0, imHeight);

    // warp the bounded box corners using the perspective transform
    Point[] corners = new Point[NUM_CORNERS];
    for (int i=0; i < NUM_CORNERS; i++) {
      corners[i] = new Point();
      persTF.transform(bbCorners[i], corners[i]);
      // System.out.println("Warped image corner " + i + ": " + corners[i]);
    }

    // find the smallest (x,y) coordinate == top-left corner of warped image
    int xMin = corners[0].x;
    int yMin = corners[0].y;
    for (int i=1; i < NUM_CORNERS; i++) {
      if (corners[i].x < xMin)
        xMin = corners[i].x;
      if (corners[i].y < yMin)
        yMin = corners[i].y;
    }

    if (!((xMin <= 0) && (yMin <= 0))) {    // smallest should be -ve
      System.out.println("Perspective calculation error"); 
      return null;
    }

    // System.out.println("Warped top-left: (" + xMin + ", " + yMin + ")");

    return new Point(xMin, yMin);
  }  // end of findWarpTopLeft()




  private BufferedImage cropWarp(BufferedImage im, Point topLeft, 
                                                  int width, int height)
  /* Cropped this image, so only the reactangular area (width * height)
     starting at -topLeft, remains
  */
  {
    int x = -topLeft.x;
    if (x < 0)         // prevent -ve values
      x = 0;
    int y = -topLeft.y;
    if (y < 0)
      y = 0;

    // get the width and height of the crop
    int w, h;
    if (x + width > im.getWidth())
      w = im.getWidth() - x;    // cropped width cannot go beyound image width 
    else
      w = width;

    if (y + height > im.getHeight())
      h = im.getHeight() - y;    // cropped height cannot go beyound image height
    else
      h = height;

    return cropImage(im, x, y, w, h);
  }  // end of cropWarp()



  private BufferedImage cropImage(BufferedImage im, int x, int y, int w, int h)
  {
    BufferedImage cropIm = null;
    try {
      cropIm = im.getSubimage(x, y, w, h);
    }
    catch (RasterFormatException e)
    {  System.out.println("Crop dimensions are incorrect: " + e);  }
    return cropIm;
  }  // end of cropImage()




  private void saveImage(String imInfo, BufferedImage im, String fnm)
  // write to PNG file
  {
    if (im == null)
      System.out.println("No image created");
    else {  // save generated image
      try {
        ImageIO.write(im, "PNG", new File(fnm));
        System.out.println("Saved " + imInfo + " image to " + fnm);
      } 
      catch (IOException ie) 
      {  System.out.println("Could not save " + imInfo + " image to " + fnm); }
    }
  }  // end of saveImage()




  // ---------------------------------------------------

  public static void main(String args[])
  {  
    if (args.length != 1)
      System.out.println("Usage: run Warper <image fnm>");
    else
      new Warper(args[0]); 
  }

} // end of Warper

