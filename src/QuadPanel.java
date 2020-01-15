
// QuadPanel.java
// Andrew Davison, May 2014, ad@fivedots.coe.psu.ac.th

/* A panel that allows the user to draw a quadrilateral over 
   an image. The quad's coordinates will be used to create
   a perspective transformation for that part of the image.

   * Left mouse button click: add a quad point;
        -- add the points in clockwise-order starting from the top-left

   * Middle mouse button (wheel) click: remove all points;

   * Right mouse button drag: drags the nearest point.
*/


import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;

import java.awt.image.*;



public class QuadPanel extends JPanel implements MouseInputListener
{
  private static final int NUM_CORNERS = 4;
  private static final int POINT_SZ = 15;


  private BufferedImage im;

  // stores the quadrilateral's coordinates
  private Point[] coords = new Point[NUM_CORNERS];
  private int nPoints = 0;

  private Point cursorPt, dragPt;
  private Font msgFont;



  public QuadPanel(BufferedImage im)
  {
    this.im = im;

    setBackground(Color.DARK_GRAY);
    setPreferredSize( new Dimension(im.getWidth(), im.getHeight()));

    msgFont = new Font("SansSerif", Font.BOLD, 18);

    addMouseMotionListener(this);
    addMouseListener(this);
  } // end of QuadPanel()




  public boolean hasQuad()
  // have 4 points been defined?
  {  return (nPoints == NUM_CORNERS);  }



  public Point[] getCoords()
  // copy the coordinates array
  { 
    Point[] cds = new Point[NUM_CORNERS];
    for (int i = 0; i < nPoints; i++)
      cds[i] = new Point( coords[i].x, coords[i].y);
    return cds;  
  }


  public void paintComponent(Graphics g)
  /*  Draw the image, quadrilateral points, and lines (if there are 4 points)
  */
  { 
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D)g; 

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setStroke(new BasicStroke(3));
    g2d.setFont(msgFont);


    g2d.drawImage(im, 0, 0, null);    // draw the image

    // draw the quadrilateral points
    g2d.setColor(Color.YELLOW);
    for (int i = 0; i < nPoints; i++)
      g2d.fillOval(coords[i].x - POINT_SZ/2, coords[i].y - POINT_SZ/2, POINT_SZ, POINT_SZ);


    if (nPoints == NUM_CORNERS) {
      // draw lines connecting the points if all of the quadrilateral is defined
      for (int i = 0; i < nPoints-1; i++)
        g2d.drawLine(coords[i].x, coords[i].y, coords[i+1].x, coords[i+1].y);

      // the last line links the last coord back to the first
      g2d.drawLine(coords[nPoints-1].x, coords[nPoints-1].y, coords[0].x, coords[0].y);
    }

    // number the points
    g2d.setColor(Color.GREEN);
    for (int i = 0; i < nPoints; i++)
      g2d.drawString("" + i, coords[i].x - 5, coords[i].y - POINT_SZ);


    // draw instructions
    g2d.setColor(Color.WHITE);
    int height = this.getHeight();
    g2d.drawString("+ Left click: add a point", 10, height-55);
    g2d.drawString("+ Middle click: remove all points", 10, height-35);
    g2d.drawString("+ Right drag: drag the nearest point", 10, height-15);

  }  // end of paintComponent()



  // ------------ mouse listener methods ----------------


  public void mousePressed( MouseEvent e)
  /* add a coordinate, move a coordinate, or clear all coordinates
  */
  { 
    if (SwingUtilities.isLeftMouseButton(e)) {    // add a coordinate
      if (nPoints < NUM_CORNERS) {
        coords[nPoints] = e.getPoint();
        nPoints++;
        repaint();   // the repaint will call paintComponent()
      }
    }
    else if (SwingUtilities.isRightMouseButton(e)) {   // move a coordinate
      if (nPoints > 0) {
        cursorPt = e.getPoint();
        dragPt = findClosestPoint(coords, nPoints, cursorPt);
      }
      else 
        System.out.println("No points available for movement"); 
    }
    else if (SwingUtilities.isMiddleMouseButton(e)) {   // clear all coordinates
      if (nPoints > 0) {
        nPoints = 0;   // remove everything
        repaint();
      }
    }
  }  // end of mousePressed()



  private Point findClosestPoint(Point[] coords, int nPoints, Point pt)
  // return the coordinate which is closest to pt
  {
    double distSq = coords[0].distanceSq(pt);
    int idx = 0;
    for (int i=1; i < nPoints; i++) {
       double d2 = coords[i].distanceSq(pt);   // use squared distance
       if (d2 < distSq) {
         idx = i;
         distSq = d2;
       }
    }
    return coords[idx];
  }  // end of findClosestPoint()




  public void mouseReleased(MouseEvent e)
  { cursorPt = null;
    dragPt = null;
  }



  public void mouseDragged(MouseEvent e)
  /* offset movement of mouse cursor affects the selected quad coordinate
     referred to by dragPt
  */
  { int xOffset = 0; 
    int yOffset = 0;
    if (cursorPt != null) {
      xOffset = e.getX() - cursorPt.x;
      yOffset = e.getY() - cursorPt.y;
      cursorPt = e.getPoint();   // update for possible next drag event
    }

    if (dragPt != null) {
      if ((xOffset != 0) || (yOffset != 0)) {
        dragPt.translate(xOffset, yOffset);
        repaint();
      }
    }
  }  // end of mouseDragged()



  public void mouseClicked(MouseEvent e) {}  // not needed 
  public void mouseEntered(MouseEvent e) {}  // not needed 
  public void mouseExited(MouseEvent e) {}   // not needed 

  public void mouseMoved(MouseEvent e) {}   // not needed

} // end of QuadPanel class
