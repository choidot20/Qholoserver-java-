
// ImagePanel.java
// Andrew Davison, May 2014, ad@fivedots.coe.psu.ac.th

/*  Draw the specified image in the center of the panel.
    If the image is too big for the panel, scale it so that it is
    all visible.
*/


import javax.swing.*;
import java.awt.*;
import java.awt.image.*;



public class ImagePanel extends JPanel
{
  private static final int PWIDTH = 400;   // size of panel
  private static final int PHEIGHT = 400; 


  private BufferedImage im;    // the image shown in this panel



  public ImagePanel()
  { // setBackground(Color.white);
    setPreferredSize(new Dimension(PWIDTH, PHEIGHT));
  }



  public void setImage(BufferedImage im)
  { this.im = im;  
    repaint();
  }


  public void paintComponent(Graphics g)
  { 
    super.paintComponent(g); 
    if (im == null)
      return;

    // compare panel and image dimensions
    int pWidth = this.getWidth();
    int pHeight = this.getHeight();

    int imWidth = im.getWidth();
    int imHeight = im.getHeight();

    double xScale = ((double)imWidth)/pWidth;
    double yScale = ((double)imHeight)/pHeight;

    if ((xScale < 1) && (yScale < 1))    // image is smaller than panel
      g.drawImage(im, (pWidth/2 - imWidth/2), 
                      (pHeight/2 - imHeight/2), null);    // center
    else {  
      // image must be scaled down to fit inside panel
      double scale = (xScale > yScale) ? xScale : yScale;   // get biggest scale
      int scWidth = (int) Math.round(imWidth / scale);
      int scHeight = (int) Math.round(imHeight / scale);
    
      g.drawImage(im, (pWidth/2 - scWidth/2), 
                      (pHeight/2 - scHeight/2), scWidth, scHeight, null);
    }
  }  // end of paintComponent()


} // end of ImagePanel class
