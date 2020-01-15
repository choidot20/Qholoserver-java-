
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import javax.media.jai.*;
import java.awt.image.renderable.ParameterBlock;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPointABC;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;

 
public class qholoserver{
	public final static String Deeplearningip = "1.245.48.126"; public final static int deeplearningport = 12345;
	public final static int myport = 20001;
    public static void main(String[] args) {
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        ServerSocket serverSocket = null;
        Socket socket = null;

 
        try {
            // ������ ���� ���� �� ���
            serverSocket = new ServerSocket(5005);
            System.out.println("������ ���۵Ǿ����ϴ�.");
 
            // ����Ǹ� ��ſ� ���� ����
          while(true){
        	
            socket = serverSocket.accept();
            System.out.println("Ŭ���̾�Ʈ�� ����Ǿ����ϴ�.");
          

            // ���� ���� �۾� ����
            FileReceiver fr = new FileReceiver(socket);
            fr.start();
          }
        } catch (IOException e) {
            e.printStackTrace();
        }
 
    }
    		
  }
 
//===========================================================================================


class FileReceiver extends Thread {
    Socket socket;
    DataInputStream dis;
    FileOutputStream fos;
    BufferedOutputStream bos;
    DataOutputStream dos;
    BufferedWriter bw=null;

    boolean sendOK = false;
    
    private static final int NUM_CORNERS = 4;
 
    private static final int WARP_WIDTH = 600;
    private static final int WARP_HEIGHT = 600;
    
    static double ssimg = 0;
	static double pixel = 1000000;
	static double count2 = 0;
	static double pixel_th = 150;
	private static double[] all = new double[4];
	
	double th1 = 38.7319 , th2 = 55.0602;					//�Է¹޴� th1,th2
  	double true_ssim_center = 0.9658, false_ssim_center = 0.9138;	//�Է¹޴� ssim.pixel�߾Ӱ�
  	double true_pixel_center = 0.2271, false_pixel_center = 0.7363;
  	double pixel_max = 43308;						//�Է¹޴� max_pixel��
  	double ssim_center_point = 0;
    double pixel_center_point = 0;
    double canny_check = 1000;			//ĳ�Ͽ��� NE
   	


   	static Mat str_red = new Mat();
    static Mat str_green = new Mat();
    static Mat str_blue = new Mat();
    static Mat edge = new Mat();
    
    
    Mat org_red = new Mat();
    Mat org_green = new Mat();
    Mat org_blue = new Mat();

    public FileReceiver(Socket socket) {
        this.socket = socket;
            
    }
    private Rectangle rectangleBounds(Point[] coords)						//bounds = �ּ�X��ǥ, �ּ�Y��ǥ, ��, ����
    /* calculate the rectangle bound around the quadrilateral
       defined by the coords[] array
    */
    {
      int xMin = coords[0].x;
      int xMax = coords[0].x;

      int yMin = coords[0].y;
      int yMax = coords[0].y;

      for (int i=1; i < NUM_CORNERS; i++) {
        if (coords[i].x < xMin)												//qr�ڵ���ǥ ���� x�ּڰ�
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



    private PerspectiveTransform makeWarpTransform(Point[] coords, int w, int h)								//�纯���� �����ϴ� PerspectiveTransform���� 
    { 
    	return PerspectiveTransform.getQuadToQuad(																//coords[0].x -> 0, coords[0].y -> 0										
                       coords[0].x, coords[0].y,   coords[1].x, coords[1].y,        							//coords[1].x -> w, coords[1].y -> 0
                       coords[2].x, coords[2].y,   coords[3].x, coords[3].y,        							//coords[2].x -> w, coords[2].y -> h
                       0,0,     w,0,        w,h,      0,h  );													//coords[3].x -> 0, coords[3].y -> h
    }



    private BufferedImage warpImage(BufferedImage im, PerspectiveTransform persTF)
    {
      WarpPerspective warpOp = null;
      try {
        warpOp = new WarpPerspective( persTF.createInverse() );   // invert the transform							//persTFũ���� warp����
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
    { 
    	int imWidth = boundedIm.getWidth();
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
      return new Point(xMin, yMin);
    }  // end of findWarpTopLeft()




    private BufferedImage cropWarp(BufferedImage im, Point topLeft, 
                                                    int width, int height)
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

    private void saveImage(String imInfo, BufferedImage im, String fnm)
    // write to PNG file
    {
      if (im == null)
        System.out.println("No image created");
      else {  // save generated image
        try {
          ImageIO.write(im, "PNG", new File(fnm));
        } 
        catch (IOException ie) 
        {  
           System.out.println("Could not save " + imInfo + " image to " + fnm); }
      }
    }  // end of saveImage()


    public void Netsendrecheck(){
       //*
       try {System.out.println("��ǥ �ν� ����. �ٽ���� ���Ӵ�� ��...");
       
         bw.write("RECHECK"); 
 //        bw.newLine();
         bw.flush(); //����
         System.out.println("���� �Ϸ�");
         bw.close();        
      
       } catch (IOException e) {
           System.err.println(e); // ������ �ִٸ� �޽��� ���
           //System.exit(1);
       }
       //*/
   }
    public void Netsendrecheckcount2(){
        //*
        try {System.out.println("���� ��Ī ����. �ٽ���� ���Ӵ�� ��...");
        
          bw.write("RECHECK2"); 
 //         bw.newLine();
          bw.flush(); //����
          System.out.println("���� �Ϸ�");
       
          bw.close();        
       
        } catch (IOException e) {
            System.err.println(e); // ������ �ִٸ� �޽��� ���
            //System.exit(1);
        }
        //*/
    }
    public void Netsendrecheckcount(){
       try {
    	   System.out.println("���� ���ν�");
       
         bw.write("RERECHECK"); 
//         bw.newLine();
         bw.flush(); //����
         System.out.println("���� �Ϸ�");
         bw.close();        
         //count2++;
       } catch (IOException e) {
           System.err.println(e); // ������ �ִٸ� �޽��� ���
           //System.exit(1);
       }
       
    }
    public void Netsendrecheckdist(){
          //*
          try {System.out.println("�Ÿ� ����� �ٽ���� ���Ӵ����..");
          
            bw.write("RECHECK"); 
            //bw.write("DIST");
//            bw.newLine();
            bw.flush(); //����
            System.out.println("���� �Ϸ�");
            bw.close();        
         
          } catch (IOException e) {
              System.err.println(e); // ������ �ִٸ� �޽��� ���
              //System.exit(1);
          }
          //*/
      }
    
    public void Netsendfake(){
        //*
        try {System.out.println("��¥ ��� �ѱ� ���Ӵ����..");
        
         bw.write("FAKE"); 
//         bw.newLine();
         bw.flush(); //����
         System.out.println("���� �Ϸ�");
         
         bw.close();        
      
       } catch (IOException e) {
           System.err.println(e); // ������ �ִٸ� �޽��� ���
           //System.exit(1);
       }finally {
         try {            
            socket.close();            
            System.out.println("socket close");
         } catch(IOException ie) {
            System.out.println(ie);
         }
       }
        //*/
    }
    
    public void NetsendOK(){
             
       try {
         System.out.println("��ǰ ��� �ѱ� ���� ��ٸ��� ��..");
       
         bw.write("OK"); 
//         bw.newLine();
         bw.flush(); //����
         System.out.println("���� �Ϸ�");
         bw.close();      
     
       } catch (IOException e) {
           System.err.println(e); // ������ �ִٸ� �޽��� ���
          
       }finally {
         try {            
            socket.close();            
            System.out.println("socket close");
         } catch(IOException ie) {
            System.out.println(ie);
         }
       }
      
   }
    
    
	public static BufferedImage mat2Img(Mat mat) {  
	     BufferedImage image = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);  
	     WritableRaster raster = image.getRaster();  
	     DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();  
	     byte[] data = dataBuffer.getData();  
	     mat.get(0, 0, data);  
	     return image;  
	  }
	
	public static Mat bufferedImageToMat(BufferedImage bi) {
		  Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		  byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		  mat.put(0, 0, data);
		  return mat;
		}
	

	
	public static int grayscale(Mat bi)
	{
		    Mat dstImg = new Mat(bi.rows(),bi.cols(),bi.type());
	        Imgproc.cvtColor(bi, dstImg, Imgproc.COLOR_RGB2GRAY);
	        //Imgcodecs.imwrite("D:/gray_6_crop.png",dstImg);
	        List<Integer> gray_img = new ArrayList<>();
	        
	        int width = dstImg.rows();
	        int height = dstImg.cols();
	        int vsum = 0;
	        int v = 0;
	        
	        double[] x  = new double[width * height];
	        
	        for (int i = 0; i < width; i++) {								
				for (int j = 0; j < height; j++) {
					gray_img.add((int) dstImg.get(i,j)[0]);				
					x[i * width + j] = gray_img.get(i * width +j);
					vsum += x[i * width + j];
					
				}

			}
	        
	        v = vsum / (width*height);
	//        System.out.println(v);
	        return v;
	}
	
	public void SplitRGB(Mat crop_Img) {

	      List<Mat> RGB = new ArrayList<Mat>(3);
	      Core.split(crop_Img, RGB);
	      org_red = RGB.get(2);
	      org_green = RGB.get(1);
	      org_blue = RGB.get(0);
	   }
	
	
    
    
    public static double CalculateValue(Mat input_red_600, Mat input_green_600, Mat input_blue_600, Mat org_red_600, Mat org_green_600, Mat org_blue_600, Mat org_red_300, Mat org_green_300, Mat org_blue_300, 
			Mat input_red_300, Mat input_green_300, Mat input_blue_300, Mat org_red_150, Mat org_green_150, Mat org_blue_150, Mat str_red_150, Mat str_green_150, Mat str_blue_150) {

		double V = 0, min = 1000000, Vmean = 0,Vsum = 0;
		int count = 0,count2 = 0;
		int x = 0, y = 0, x_300 = 0, y_300 = 0;
		int  x_f_600 = 0, y_f_600 = 0, x_f_300 = 0, y_f_300 = 0, x_150 = 0, y_150 = 0;
		
		double c1 = Math.pow(255*0.01, 2), c2 = Math.pow(255*0.03, 2);
		double new_og_sum = 0, new_ig_sum = 0, new_og_avg = 0, new_ig_avg = 0, new_og_var = 0, new_ig_var = 0, new_g_cov = 0, count_green = 0; 
		int org_width_600 = org_red_600.rows(), org_height_600 = org_red_600.cols(), input_width_600 = input_red_600.rows(), input_height_600 = input_red_600.cols(),
				width_300 = org_red_300.rows(), height_300 = org_red_300.cols(), width_150 = org_red_150.rows(), height_150 = org_red_150.cols(); 				//���� ���� ����

		List<Integer> o_red_600 = new ArrayList<>();							//1���� �迭�� mat������ ������ �ֱ����� LIST�� ���� �Ҵ�(o_red = ���� red����, i_red = �Է� red����)
		List<Integer> i_red_600 = new ArrayList<>();							//������ i�� o�� �Է°� �������� ������ red,green,blue�� ������ ������ 
		List<Integer> o_green_600 = new ArrayList<>();							//990,495,247�� ������ ũ�⸦ �����Ѵ�.
		List<Integer> i_green_600 = new ArrayList<>();
		List<Integer> o_blue_600 = new ArrayList<>();
		List<Integer> i_blue_600 = new ArrayList<>();
		
		List<Integer> o_red_300 = new ArrayList<>();
		List<Integer> i_red_300 = new ArrayList<>();
		List<Integer> o_green_300 = new ArrayList<>();
		List<Integer> i_green_300 = new ArrayList<>();
		List<Integer> o_blue_300 = new ArrayList<>();
		List<Integer> i_blue_300 = new ArrayList<>();
		
		List<Integer> o_red_150 = new ArrayList<>();
		List<Integer> i_red_150 = new ArrayList<>();
		List<Integer> o_green_150 = new ArrayList<>();
		List<Integer> i_green_150 = new ArrayList<>();
		List<Integer> o_blue_150 = new ArrayList<>();
		List<Integer> i_blue_150 = new ArrayList<>();
		

		
		double[] red_input_600, blue_input_600, green_input_600, red_input_300, blue_input_300, green_input_300, red_input_150, blue_input_150, green_input_150;	//mat������ ������ 1���� �迭�� �ֱ����� �Ҵ�Ǵ� 1���� �迭			
		red_input_600 = new double[org_width_600 * org_height_600];																											//input�� �Է¿����� org�� ���������� �ֱ����� �迭�̴�.													
		blue_input_600 = new double[org_width_600 * org_height_600];																										
		green_input_600 = new double[org_width_600 * org_height_600];
		red_input_300 = new double[width_300 * height_300];
		blue_input_300 = new double[width_300 * height_300];
		green_input_300 = new double[width_300 * height_300];
		red_input_150 = new double[width_150 * height_150];
		blue_input_150 = new double[width_150 * height_150];
		green_input_150 = new double[width_150 * height_150];
		
		double[] red_org_600, blue_org_600, green_org_600, red_org_300, blue_org_300, green_org_300, red_org_150, blue_org_150, green_org_150;									
		red_org_600 = new double[org_width_600 * org_height_600];
		blue_org_600 = new double[org_width_600 * org_height_600];
		green_org_600 = new double[org_width_600 * org_height_600];
		red_org_300 = new double[width_300 * height_300];
		blue_org_300 = new double[width_300 * height_300];
		green_org_300 = new double[width_300 * height_300];
		red_org_150 = new double[width_150 * height_150];
		blue_org_150 = new double[width_150 * height_150];
		green_org_150 = new double[width_150 * height_150];

		for (int i = 0; i < org_red_600.cols(); i++) {								//1���� �迭�� mat������ ������ �־���(990x990)
			for (int j = 0; j < org_red_600.rows(); j++) {
				o_red_600.add((int) org_red_600.get(i, j)[0]);
				o_green_600.add((int) org_green_600.get(i, j)[0]);
				o_blue_600.add((int) org_blue_600.get(i, j)[0]);
				
				red_org_600[i * org_width_600 + j] = o_red_600.get(i * org_width_600 + j);
				green_org_600[i * org_width_600 + j] = o_green_600.get(i * org_width_600 + j);
				blue_org_600[i * org_width_600 + j] = o_blue_600.get(i * org_width_600 + j);
			}

		}
		
		for (int i = 0; i < input_red_600.cols(); i++) {								//1���� �迭�� mat������ ������ �־���(990x990)
			for (int j = 0; j < input_red_600.rows(); j++) {
				
				i_red_600.add((int) input_red_600.get(i, j)[0]);
				i_green_600.add((int) input_green_600.get(i, j)[0]);
				i_blue_600.add((int) input_blue_600.get(i, j)[0]);

				red_input_600[i * input_width_600 + j] = i_red_600.get(i * input_width_600 + j);
				green_input_600[i * input_width_600 + j] = i_green_600.get(i * input_width_600 + j);
				blue_input_600[i * input_width_600 + j] = i_blue_600.get(i * input_width_600 + j);
			}

		}
		
		
		
		for (int i = 0; i < org_red_300.rows(); i++) {									//1���� �迭�� mat������ ������ �־���(495x495)
			for (int j = 0; j < org_red_300.cols(); j++) {

				o_red_300.add((int) org_red_300.get(i, j)[0]);
				i_red_300.add((int) input_red_300.get(i, j)[0]);

				o_green_300.add((int) org_green_300.get(i, j)[0]);
				i_green_300.add((int) input_green_300.get(i, j)[0]);

				o_blue_300.add((int) org_blue_300.get(i, j)[0]);
				i_blue_300.add((int) input_blue_300.get(i, j)[0]);

				red_org_300[i * width_300 + j] = o_red_300.get(i * width_300 + j);
				red_input_300[i * width_300 + j] = i_red_300.get(i * width_300 + j);
				green_org_300[i * width_300 + j] = o_green_300.get(i * width_300 + j);
				green_input_300[i * width_300 + j] = i_green_300.get(i * width_300 + j);
				blue_org_300[i * width_300 + j] = o_blue_300.get(i * width_300 + j);
				blue_input_300[i * width_300 + j] = i_blue_300.get(i * width_300 + j);

			}

		}
		
		for (int i = 0; i < org_red_150.rows(); i++) {									//1���� �迭�� mat������ ������ �־���(247x247)
			for (int j = 0; j < org_red_150.cols(); j++) {

				o_red_150.add((int) org_red_150.get(i, j)[0]);
				i_red_150.add((int) str_red_150.get(i, j)[0]);

				o_green_150.add((int) org_green_150.get(i, j)[0]);
				i_green_150.add((int) str_green_150.get(i, j)[0]);

				o_blue_150.add((int) org_blue_150.get(i, j)[0]);
				i_blue_150.add((int) str_blue_150.get(i, j)[0]);

				red_org_150[i * width_150 + j] = o_red_600.get(i * width_150 + j);
				red_input_150[i * width_150 + j] = i_red_600.get(i * width_150 + j);
				green_org_150[i * width_150 + j] = o_green_600.get(i * width_150 + j);
				green_input_150[i * width_150 + j] = i_green_600.get(i * width_150 + j);
				blue_org_150[i * width_150 + j] = o_blue_600.get(i * width_150 + j);
				blue_input_150[i * width_150 + j] = i_blue_600.get(i * width_150 + j);

			}

		}
		
		for (int k = -2; k <= 2; k++) {																								//247x247������ �������տ����� ��ǥ�� ����
			for (int l = -2; l <= 2; l++) {																							//�������տ��� = v = |org(r)-input(r)| + |org(g)-input(g)| + |org(b)-input(b)| / count�� �ּڰ�
				for (int i = 0; i < height_150 ; i++) {
					for (int j = 0; j < width_150; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < width_150 && j - l < height_150)) {								//������ ��ġ�� �κи� ���ϱ����� if��

							V = Math.abs(red_org_150[i * width_150 + j] - red_input_150[(i - k) * width_150 + (j - l)])				//�������տ����� ���Ѵ�.
									+ Math.abs(green_org_150[i * width_150 + j] - green_input_150[(i - k) * width_150 + (j - l)])
									+ Math.abs(blue_org_150[i * width_150 + j] - blue_input_150[(i - k) * width_150 + (j - l)]);
		
							Vsum += V;
							count++;				
						}

					}
				}
				Vmean = Vsum / count;

				if (min > Vmean) {																									//mad�� �ּڰ��� ã���ش�/
					min = Vmean;
					x_150 = k;																										//mad�� �ּڰ��϶� x��ǥ
					y_150 = l;																										//mad�� �ּڰ��϶� y��ǥ
				}					
				Vsum = 0;
				count = 0;
				V = 0;

			}
		}
		
		V = 0;								
		Vsum = 0;
		count = 0;
		min = 1000000;
		

		x_f_300 = 2 * x_150;										//247x247������ 2���� ũ���̹Ƿ� *2������
		y_f_300 = 2 * y_150;
		
		for (int k = y_f_300 - 3; k <= y_f_300 + 3; k++) {			//495x495������ �������տ����� ��ǥ�� ����
			for (int l = x_f_300 - 3; l <= x_f_300 + 3; l++) {
				for (int i = 0; i < height_300; i++) {
					for (int j = 0; j < width_300; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < width_300 && j - l < height_300)) {   //������ ��ġ�� �κи� ���ϱ����� if��

							V = Math.abs(red_org_300[i * width_300 + j] - red_input_300[(i - k) * width_300 + (j - l)])
									+ Math.abs(green_org_300[i * width_300 + j] - green_input_300[(i - k) * width_300 + (j - l)])
									+ Math.abs(blue_org_300[i * width_300 + j] - blue_input_300[(i - k) * width_300 + (j - l)]);
		
							Vsum += V;
							count++;

						}

					}
				}
				Vmean = Vsum / count;

				if (min > Vmean) {
					min = Vmean;
					x_300 = k;
					y_300 = l;
				}
				Vsum=0;
				count = 0;
				V = 0;

			}
		}
		
		V = 0;
		Vsum = 0;
		count = 0;
		min = 1000000;

		x_f_600 = 2 * x_300;
		y_f_600 = 2 * y_300;
		
		for (int k = y_f_600 - 3; k <= y_f_600 + 3; k++) {			//990x990������ �������տ����� ��ǥ�� ����
			for (int l = x_f_600 - 3; l <= x_f_600 + 3; l++) {
				for (int i = 0; i < org_height_600; i++) {
					for (int j = 0; j < org_width_600; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < org_width_600 && j - l < org_height_600)) {	//������ ��ġ�� �κи� ���ϱ����� if��

							V = Math.abs(red_org_600[i * org_width_600 + j] - red_input_600[(i - k) * org_width_600 + (j - l)])
									+ Math.abs(green_org_600[i * org_width_600 + j] - green_input_600[(i - k) * org_width_600 + (j - l)])
									+ Math.abs(blue_org_600[i * org_width_600 + j] - blue_input_600[(i - k) * org_width_600 + (j - l)]);
		
							Vsum += V;
							count++;
							if(V > pixel_th)					//�������տ����߿� v�� ���� 150�̻��� �ȼ��� ��(�ο����� ���̰� ū���� ���� ã�°�)
								count2++;

						}

					}
				}
				Vmean = Vsum / count;

				if (min > Vmean) 
				{
					min = Vmean;
					x = k;
					y = l;
					pixel = count2;
				}
				Vsum = 0;
				count2 = 0;
				count = 0;
				V = 0;

			}
		}
		
		
		
		
		for(int i = 10; i < org_width_600 - 10; i++)																						//ssim���ϱ� ���� �л�,���,���л��� ���� 
		{
			for(int j = 10; j < org_height_600 - 10; j++)
			{
				if ((i - y >= 0 && j - x >= 0 && i - y < org_width_600 && j - x < org_height_600)) {
				new_og_sum += green_org_600[i * org_width_600 + j];
				new_ig_sum += green_input_600[(i - x)* org_width_600 + (j - y)];
				new_og_var += green_org_600[i * org_width_600 + j] * green_org_600[i * org_width_600 + j];
				new_ig_var += green_input_600[(i - x)* org_width_600 + (j - y)] * green_input_600[(i - x)* org_width_600 + (j - y)];
				new_g_cov += green_org_600[i * org_width_600 + j] * green_input_600[(i - x)* org_width_600 + (j - y)];
				count_green++;
				}				
			}
		}
		
		
		new_og_avg = new_og_sum / count_green;																				//�Է¿���� ���������� ���
		new_ig_avg = new_ig_sum / count_green;	
		new_og_var = (new_og_var / count_green) - (new_og_avg * new_og_avg);												//�Է¿���� ���������� �л�
		new_ig_var = (new_ig_var / count_green) - (new_ig_avg * new_ig_avg);
		new_g_cov = (new_g_cov / count_green) - (new_og_avg * new_ig_avg);													//���л�
		ssimg = (((2 * new_og_avg * new_ig_avg) + c1) * ((2 * new_g_cov) + c2)) / (((new_og_avg * new_og_avg) + (new_ig_avg * new_ig_avg) + c1) * (new_og_var + new_ig_var + c2));
		

	//	System.out.println(x + "," + y + "\t" + min+"\t"+pixel+"\t"+ssimg);
		
		return min;
	}

  	
    public int Canny(Mat src){
		  Mat src_gray = new Mat();
		  Mat detected_img = new Mat();
//		  Mat edge_img=new Mat();
		  
		  
		  
		  
		  int width = src.rows();
		  int height = src.cols();
		  
		  Imgproc.cvtColor(src,src_gray,Imgproc.COLOR_BGR2GRAY);
		  
//		  Imgcodecs.imwrite(path+"/6_crop_gray.png", src_gray);											//������ �̹��� ����			
		  Imgproc.Canny(src_gray,detected_img,125.0, 250.0);											//������ �̹��� ����
//		  Imgcodecs.imwrite(path+"/java_histmatch_bound_bound.png", detected_img);											
//		  edge_img=Imgcodecs.imread(path+"/Canny.png",CvType.CV_8UC1);
		  List<Integer> edge_detect = new ArrayList<>();
		  int count=0;
		  int[] pixels = new int[width * height];
		  for(int i = 0;i<detected_img.cols();i++){
		   for(int j = 0;j<detected_img.rows();j++){
		    edge_detect.add((int) detected_img.get(j, i)[0]);
		    pixels[i * width + j] = edge_detect.get(i * width + j);
		    if(pixels[i* width +j]!=0)
		     count++;
		   }
		  }
		  System.out.print(count+"\t");
		  
		  return count;
		  
		 }
    public static void histMatch(Mat reference, Mat target,  String img_path){
		double HISTMATCH = 0.000001;
		double min, max;
		List<Mat> ref_channels = new ArrayList<Mat>(3);
		Core.split(reference, ref_channels);
		List<Mat> tgt_channels = new ArrayList<Mat>(3);
		Core.split(target,  tgt_channels);
		
		MatOfFloat range= new MatOfFloat(0,256);
		MatOfInt histSize = new MatOfInt(256);
		
		for(int i = 0; i <3 ; i++){
			Mat ref_hist = new Mat();
			Mat tgt_hist = new Mat();
			Mat ref_hist_accum = new Mat();
			Mat tgt_hist_accum = new Mat();
			MatOfInt channels = new MatOfInt(i);
			
			Mat refIm = new Mat();
			refIm = ref_channels.get(i);
			Mat tgtIm = new Mat();
			tgtIm = tgt_channels.get(i);
			//Imgcodecs.imwrite(img_path+i+"iimg.png", tgtIm);

			Imgproc.calcHist(ref_channels, channels, new Mat(), ref_hist, histSize, range);
			Imgproc.calcHist(tgt_channels, channels, new Mat(), tgt_hist, histSize, range);
			
	
			MinMaxLocResult mmr_ref = Core.minMaxLoc(ref_hist);
			if (mmr_ref.maxVal == 0)
			{
				System.out.println("ERROR: max is 0 in ref_hist");

			}
			Core.normalize(ref_hist, ref_hist,mmr_ref.minVal/mmr_ref.maxVal, 1, Core.NORM_MINMAX, -1, new Mat());
			MinMaxLocResult mmr_tgt = Core.minMaxLoc(tgt_hist);
			if (mmr_tgt.maxVal == 0)
			{
				System.out.println("ERROR: max is 0 in tgt_hist");

			}
			
			Core.normalize(tgt_hist, tgt_hist, mmr_tgt.minVal/mmr_tgt.maxVal,1, Core.NORM_MINMAX, -1, new Mat());
			ref_hist.copyTo(ref_hist_accum);
			tgt_hist.copyTo(tgt_hist_accum);


			double[] src_cdf_data=null; 
			src_cdf_data = new double[256];

			double[] dst_cdf_data=null;
			dst_cdf_data = new double[256];

			for (int j = 0; j <256; j++) {

					src_cdf_data[j] = ref_hist_accum.get(j,0)[0];
					dst_cdf_data[j] = tgt_hist_accum.get(j,0)[0];
					
					
			}

			for(int j = 1; j<256; j++)
			{
				src_cdf_data[j] += src_cdf_data[j-1];
				dst_cdf_data[j] += dst_cdf_data[j-1];
				
			}

			for(int x=0; x<256; x++){
				ref_hist_accum.put(x, 0, src_cdf_data[x]);
				tgt_hist_accum.put(x, 0, dst_cdf_data[x]);
			}

			MinMaxLocResult mmr_ref_accum = Core.minMaxLoc(ref_hist_accum);
			Core.normalize(ref_hist_accum, ref_hist_accum, mmr_ref_accum.minVal/mmr_ref_accum.maxVal,1, Core.NORM_MINMAX, -1, new Mat());
			MinMaxLocResult mmr_tgt_accum = Core.minMaxLoc(tgt_hist_accum);
			Core.normalize(tgt_hist_accum, tgt_hist_accum, mmr_tgt_accum.minVal/mmr_tgt_accum.maxVal,1, Core.NORM_MINMAX, -1, new Mat());
			for (int j = 0; j <256; j++) {
				src_cdf_data[j] = ref_hist_accum.get(j,0)[0];
				dst_cdf_data[j] = tgt_hist_accum.get(j,0)[0];
		}
			Mat lut = new Mat(1,256,CvType.CV_8UC1);
			char[] M=null;
			M = new char[tgt_hist_accum.rows()];
	
			char last = 0;
			
			for(int l = 0; l< tgt_hist_accum.rows(); l++){
				
				double F1 = dst_cdf_data[l];
				
				for(char k = last; k < ref_hist_accum.rows(); k++){
					double F2 = src_cdf_data[k];
					if(Math.abs(F2-F1)<HISTMATCH || F2>F1){
						M[l] = k;
						last = k;
						break;
					}

				}

			}

			for(int l = 0; l< 256; l++){
				lut.put(0,l,M[l]);
			}

			Core.LUT(tgt_channels.get(i), lut, tgt_channels.get(i));
			

		}
		str_red = tgt_channels.get(2);
		str_green = tgt_channels.get(1);
		str_blue = tgt_channels.get(0);
		
//		Imgcodecs.imwrite("bin/java_hist_Blue.png", tgt_channels.get(0));
//		Imgcodecs.imwrite("bin/java_hist_Green.png", tgt_channels.get(1));
//		Imgcodecs.imwrite("bin/java_hist_Red.png", tgt_channels.get(2));
		
		Core.merge(tgt_channels, edge);
//		Imgcodecs.imwrite("bin/java_histmatch.png", result);

	}

   BufferedImage getRotateImage(BufferedImage image, double angle){//angle : degree

                double _angle = Math.toRadians(angle);
                double sin = Math.abs(Math.sin(_angle));
                double cos = Math.abs(Math.cos(_angle));
                double w = image.getWidth();
                double h = image.getHeight();
                int newW = (int)Math.floor(w*cos + h*sin);
                int newH = (int)Math.floor(w*sin + h*cos);              

                Frame f=new Frame();
                GraphicsConfiguration gc = f.getGraphicsConfiguration();
                BufferedImage result = gc.createCompatibleImage(newW, newH, Transparency.TRANSLUCENT);
                Graphics2D g = result.createGraphics();               

                g.translate((newW-w)/2, (newH-h)/2);
                g.rotate(_angle, w/2, h/2);
                g.drawRenderedImage(image, null);
                g.dispose();

                return result;
        }
   
   
   
  
  
    @Override
    public void run() {
        try {
        	long file_qrcode_start = System.currentTimeMillis();
        	
            System.out.println("���� ���� �۾��� �����մϴ�.");
                        
            bw=new BufferedWriter(new OutputStreamWriter( socket.getOutputStream()) );               
            dis = new DataInputStream(socket.getInputStream());
             // ���ϸ��� ���� �ް� ���ϸ� ����.
            String fName = dis.readUTF();
            System.out.println("���ϸ� " + fName + "�� ���۹޾ҽ��ϴ�.");
            String path = qholoserver.class.getResource("").getPath(); 					// path = ���� Ŭ������ ���� ��θ� �����´�.
            // ������ �����ϰ� ���Ͽ� ���� ��� ��Ʈ�� ����
            File f = new File(path+fName);
            fos = new FileOutputStream(f);
            bos = new BufferedOutputStream(fos);
            System.out.println(fName + "������ �����Ͽ����ϴ�.");
            
            
            
            // ����Ʈ �����͸� ���۹����鼭 ���
            int rgb;
            int gray;
            int red;
            int green;
            int blue;
            boolean  foundpattern=false;
       
            
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
          
            byte[] data;
            data=(byte[])ois.readObject(); // byte[] b
            bos.write(data, 0,data.length);
            
            bos.flush();
            bos.close();
            fos.close();

      
            System.out.println(path); //--> ���� ��ΰ� ��µ�           
            //File imageFile = new File(path + fName);											//imageFile = ���÷κ��͹��� ����ڰ� �Կ��� ����
            File imageFile = new File(path+"4.jpg");	// test code �����
            
            BufferedImage image = ImageIO.read(imageFile);										//���÷� ���� image�� Buffreimage�� ����
            int w = image.getWidth(null);  														//w, h = 900
            int h = image.getHeight(null);  
            System.out.println(w+ " " + h);
            
//            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//            ColorConvertOp op = new ColorConvertOp(cs, null);
//           BufferedImage imagefix = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY );   	//BufferedImage(w,h,imageType)
//            imagefix = op.filter(image, null);													//grayscale�̹��� ��ȯ
//            saveImage("grayImage",imagefix,path+"1_gray.png");									//�Կ��̹����� grayscale�̹��� ����
          
            BufferedImage imagebi = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);		//900 * 900ũ���� Binary�̹������ۿ��� ����
            
            long a = System.currentTimeMillis();
            Mat grayimage = bufferedImageToMat(image);
            int gray_th = grayscale(grayimage);
            long b = System.currentTimeMillis();
            
        //    BufferedImage imagebm=new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
          
          
            long c = System.currentTimeMillis();
        	
           for(int xx=0;xx<w;xx++)
        	   for(int yy=0;yy<h;yy++){
              
              rgb = image.getRGB(xx,yy);						//xx,yy��ǥ�� rgb������
              
         //     gray= imagefix.getRGB(xx,yy);
               
              red = (rgb >> 16 ) & 0x000000FF;					//red�� ����
               green = (rgb >> 8 ) & 0x000000FF;				//green�� ����
               blue = (rgb) & 0x000000FF;						//blue�� ����
               
             double check_QRCODE = (0.5042 * gray_th) - 20.298;
               
          //   if(blue>50&&red>50&&green>50)
           if(((blue + red + green)/3) > check_QRCODE)
               imagebi.setRGB(xx, yy,Color.WHITE.getRGB()); //else imagebi.setRGB(xx, yy, Color.BLACK.getRGB());		imagebi = r>127, g>127, b>127�� ������ �Ͼ������ ���� 
            
             
         /*    if(((gray) & 0x000000FF)>127)
            	 imagebm.setRGB(xx, yy,Color.WHITE.getRGB()); 
             else imagebm.setRGB(xx, yy,Color.BLACK.getRGB()); //�׷��̽����ϵ� ���� INT���� ������ rgb���� ���� ������.
           */                                                                                   // ex. (100,100,100) or (98,98,98)�� r=100 ,g=100,b=100
             
           }
           
           long d = System.currentTimeMillis();
       	
           saveImage("imagebi",imagebi,path+"imagebi.png");												//�����
           Mat ocrop_Image = bufferedImageToMat(imagebi);
           //Mat ocrop_Image = Imgcodecs.imread("bin/imagebi.png");											//�����
           Mat red_img = new Mat();
           Mat kernel = Mat.ones(4,4, CvType.CV_32F);
           
           Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_OPEN, kernel);
           //Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_ERODE, kernel);
          Imgcodecs.imwrite("bin/ERODE_imagebi.png",red_img);											//�����
           
          imagebi = mat2Img(red_img);																//ħ����â
         imagebi = loadImage("ERODE_imagebi.png");														//�����
           
            
        /*    int[] rgbarr = new int[w * h];  
            rgbarr = image.getRGB(0, 0, w, h, rgbarr, 0, w);  */
            ResultPointABC pointsABC = new ResultPointABC(); 									//pointsABC��ü����
            pointsABC.setABC(0, 0, 0, 0, 0, 0,0,0); //KYT RESET       							//QR�ڵ��� ��ǥ
            try {
               
                  LuminanceSource source =new BufferedImageLuminanceSource(imagebi);			//bitmap �����ڸ� ��������� imagebi�����͸� LuminanceSource���·� �ٲ�
                   BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));			//bitmap �����ڸ� ��������ؼ� �����ڸ� ���� BinaryBitmap(Binarizer binarizer), Binarizer(LuminanceSource source)  
                   QRCodeReader reader=new QRCodeReader();										//��ü����
                   Result result = reader.decode(bitmap);										//bitmap�� QR�ڵ带 ã�Ƽ� �ص�
                   System.out.println("Barcode text: " + result.getText());
                   if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0)		

                	   //qr�ڵ��� ��ǥ�� �ϳ��� 0�ϰ��
                   {																						

																							

                	   	//foundpattren = false = �ٽ����޽��� ����
                      foundpattern=false;
                   }
                   else 
                	   foundpattern=true;
                   
            	} catch (Exception e) {
          
                  // System.out.println(e.toString());
                  // e.printStackTrace();

            } 
            finally 
            {
               
               if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0)
               {
                   foundpattern=false;
               }
               else foundpattern=true;
               
             try {
                
             } catch(Exception ie) {
                //System.out.println(ie.toString());ie.printStackTrace();
             }
          }

          
            
           
      /*     if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0){
               
              for(int t=1;t<4;t++){ //������ ��ǥ�� ���� ���� ������, ������ 90���� ȸ������ ��ǥ�� ����Ǵ��� Ȯ��. 
                 pointsABC.setABC(0, 0, 0, 0, 0, 0,0,0); //KYT RESET
                    
                   
                 try {

                          LuminanceSource source =new BufferedImageLuminanceSource(getRotateImage(imagebi,90));
                         BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                         QRCodeReader reader=new QRCodeReader();                     
                         Result result = reader.decode(bitmap);									//bitmap�� QR�ڵ带 ã�Ƽ� �ص�
                         System.out.println("Code Text: " + result.getText());
   
                  } catch (Exception e) {
                      System.out.println(e.toString());
                         e.printStackTrace();
   
                  } finally {
                   try {
                      
                   } catch(Exception ie) {
                      System.out.println(ie.toString());ie.printStackTrace();
                   }
                }
                 
                 if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0){
                    System.out.println("Try "+t+": ");
                 } else {
                    foundpattern=true;
                    image=getRotateImage(image,t*90); //���� �̹����� ȸ����Ų ��ŭ ȸ��������� ��Ȯ�� ��ǥ�� ����                     
                    t=5;
                    System.out.println("����ã�� Try "+t+"��°��: ");   
                 } 
         
              }
           }
            if( foundpattern==false && sendOK==false)
            {
            	Netsendrecheck();sendOK=true;
            } //��ǥ �ϳ��� 0�̸� �ٽ� ����ּ���.  
            
            if( sendOK==false && ((int)pointsABC.getCx()-(int)pointsABC.getBx())<110 ) {System.out.println("�Ÿ��� �־� �ٽ�����û a");Netsendrecheckdist();sendOK=true;} //�ν��� ��ǥ ������ �ϳ��� 110���� �̸� �ٽ� ����ּ���.  
            if( sendOK==false && ((int)pointsABC.getAy()-(int)pointsABC.getBy())<110 ) {System.out.println("�Ÿ��� �־� �ٽ�����û b");Netsendrecheckdist();sendOK=true;}
            if( sendOK==false && ((int)pointsABC.getCx()-(int)pointsABC.getAx())<110 ) {System.out.println("�Ÿ��� �־� �ٽ�����û c");Netsendrecheckdist();sendOK=true;}            
           if( sendOK==false && ((int)pointsABC.getAy()-(int)pointsABC.getCy())<110 ) {System.out.println("�Ÿ��� �־� �ٽ�����û d");Netsendrecheckdist();sendOK=true;}   
  */          
  //         long file_qrcode_end = System.currentTimeMillis();
           
 
   //        long file_pro_start = System.currentTimeMillis();
           
           
           //QR�ڵ��ν��� �� QR�ڵ���ǥ-> pointsABC��ü�� ����Ǿ�����
           
             if(sendOK==false){
             Point[] coords = new Point[NUM_CORNERS];
             coords[0] = new Point( (int)pointsABC.getBx(), (int)pointsABC.getBy()); 						//coodrs[0] = ���ʻ�� qr��ǥ
             coords[1] = new Point( (int)pointsABC.getCx(), (int)pointsABC.getCy()); 						//coodrs[1] = �����ʻ�� qr��ǥ
             coords[2] = new Point( (int)pointsABC.getDx(), (int)pointsABC.getDy()); 						//coodrs[2] = �������ϴ� qr��ǥ
             coords[3] = new Point( (int)pointsABC.getAx(), (int)pointsABC.getAy()); 						//coodrs[3] = �����ϴ� qr��ǥ
       //      System.out.println(coords[0]+"  "+coords[1]+"  "+coords[2]+"  "+coords[3]);
             
             
            // crop the image so it tightly bounds the quadrilateral
             Rectangle bounds = rectangleBounds(coords);													//bounds = �ּ�X��ǥ, �ּ�Y��ǥ, ��, ����
             BufferedImage boundedIm = cropImage(image, bounds.x, bounds.y, 
                                                           bounds.width, bounds.height);					//boundedIm = �Կ������� crop�� �̹���
             saveImage("bounded", boundedIm, path+"4_bounded.png"); //KYT removes							//�̹��� ����(�����)
             boundedIm = loadImage("4_bounded.png"); //KYT removes											//�̹��� ����(�����)

             // adjust coordinates to apply to bounded image
             for (int i=0; i < NUM_CORNERS; i++) 															//bounded image�� ���� ��ǥ ����
             {
               coords[i].x -= bounds.x;
               coords[i].y -= bounds.y;
             }
            
             // convert the quadrilteral into a perspective transform
             PerspectiveTransform persTF = makeWarpTransform(coords, WARP_WIDTH, WARP_HEIGHT);				//wrap���ֱ� ���ؼ� coords�� ��ǥ�� 990x990���� �ٲ���
             																					

	//persTF = coords[]
             // apply perspective tranformation to image
             System.setProperty("com.sun.media.jai.disableMediaLib", "true"); // ���� ����
             BufferedImage warpIm = warpImage(boundedIm, persTF);								//wrapIm = crop�� �̹���(boundedim)�� wrap���� �̹���
 //            saveImage("warpIm", warpIm, path+"4_warpIm.png"); //KYT removes								//������ �̹��� ����
             
             if (warpIm == null)
              // return;
             System.out.println("warpImage Not Found Code! please Retry!!"); 
             // find top-left coordinate of warped image
             Point topLeft = findWarpTopLeft(boundedIm, persTF);											//bounded image(crop�� �̹���)�� ���� �����ǥ�� ���� 
             if (topLeft == null)
              // return; // ���â ȭ�� �������� , ����
             System.out.println("Not Found Code! please Retry!!"); 
             BufferedImage cropIm = cropWarp(warpIm, topLeft, WARP_WIDTH, WARP_HEIGHT);						//wrapIm(crop�� �̹����� wrap���� �̹���)�� topleft��ǥ�� 990,990���� �̿� crop���ش�.
     
             

             saveImage("warped and cropped", cropIm, path+"6_crop.png");
             
             
   //          long file_pro_end = System.currentTimeMillis();
             
       //      long file_app_start = System.currentTimeMillis();
             
             
       /*      cropIm= loadImage("6_crop.png");
             
             BufferedImage cropcropim = cropImage(cropIm,140,140,850,850);
             saveImage("qr_crop",cropcropim,path+"QR_crop.png");
             Mat qr_crop_image = Imgcodecs.imread("bin/org/qw_crop.png");
             */

//           Mat crop_Image = Imgcodecs.imread("bin/6_crop.png", Imgcodecs.CV_LOAD_IMAGE_COLOR);						//������
//          Mat canny_image = Imgcodecs.imread("bin/4_bounded.png", Imgcodecs.CV_LOAD_IMAGE_COLOR);					//����������
             Mat tgt = Imgcodecs.imread("bin/6_crop.png");
             Mat ref = Imgcodecs.imread("bin/org/6_crop.png");
             
             SplitRGB(ref);
//    		Mat tgt2 = bufferedImageToMat(cropIm);
//    		Imgcodecs.imwrite("bin/6_croptobuffer.png",tgt2);
           
             histMatch(ref, tgt, path);
    		
    		
    		
    		
      /*     int blur = 0;																					

		//�̹����� �������� �����ϴ� ����
           double canny_check_th = (canny_check * 0.5);																//������ +-60%

������ th������ ����
           double canny_check_th1 = (canny_check + canny_check_th);
           double canny_check_th2 = (canny_check - canny_check_th);
           blur = Canny(qr_crop_image, path);																			

//������ �������� �����ش�
            
            
            
            if((sendOK == false && canny_check_th2 >= blur) || (sendOK==false && blur >= canny_check_th1)){ 			//�Կ��� ������ �������� th������ ������ ������ �޼��� �ѱ�
            	System.out.println("�� �̹���");
            	Netsendrecheck(); //�ٽ� �����ÿ�! ������ 
            	sendOK = true;
            	}
            if(sendOK == false){

        */  
        //    Mat aqswde = Imgcodecs.imread("N:/600/c2/6_crop.png", CvType.CV_8UC1); 
             
    		int blur = 0;
    		int blurcheck = 0;
    		blur = Canny(edge);	
    		blurcheck = (int) (canny_check * 0.25);
    		
    		  if((sendOK == false && blur < blurcheck)){ 			//�Կ��� ������ �������� th������ ������ ������ �޼��� �ѱ�
              	System.out.println("�� �̹���");
              	Netsendrecheck(); //�ٽ� �����ÿ�! ������ 
              	sendOK = true;
              	}
              
    		if(sendOK == false){
    		  
    		  																			
    		//������ ������׷��� �����ش�.																			
        
    //       System.out.println("��Ī ��......");
           
          // int n = Search(crop_Image, str_red);
         
   		
           
           
           
          // double edge = Canny(crop_Image, "bin/");
           //int result = CalculateValue(str_red, str_green, str_blue, org_red, org_green, org_blue);
           //int[] graph = {0};
           double mad = 0;
           double ssim_th_point  = 0;
           double realssim = 0;
           double falsessim = 0;
           
            Mat org_red_resize_300 = new Mat();				//���� resize������ �����ϴ� ����(495)
			Mat org_green_resize_300 = new Mat();
			Mat org_blue_resize_300 = new Mat();
			
			Mat org_red_resize_150 = new Mat();				//���� resize������ �����ϴ� ����(247)
			Mat org_green_resize_150 = new Mat();
			Mat org_blue_resize_150 = new Mat();
			
			
			Mat str_red_resize_300 = new Mat();				//�Է� resize������ �����ϴ� ����
			Mat str_green_resize_300 = new Mat();
			Mat str_blue_resize_300 = new Mat();
			
			Mat str_red_resize_600 = new Mat();
			Mat str_green_resize_600 = new Mat();
			Mat str_blue_resize_600 = new Mat();
			
			Mat str_red_resize_150 = new Mat();				
			Mat str_green_resize_150 = new Mat();
			Mat str_blue_resize_150 = new Mat();
			
			Size sz_600 = new Size(600,600);
			Size sz_300 = new Size(300,300);			//resizeũ�� (495)
			Size sz_150 = new Size(150,150);			//resizeũ�� (247)
          
           
           Imgproc.resize(org_red, org_red_resize_300, sz_300);		//org resize(src,dst,size);
			Imgproc.resize(org_green, org_green_resize_300, sz_300);
			Imgproc.resize(org_blue, org_blue_resize_300, sz_300);
			
			Imgproc.resize(str_red, str_red_resize_300, sz_300);		//input resize
			Imgproc.resize(str_green, str_green_resize_300, sz_300);
			Imgproc.resize(str_blue, str_blue_resize_300, sz_300);
			
			Imgproc.resize(org_red, org_red_resize_150, sz_150);		//org resize(src,dst,size);
			Imgproc.resize(org_green, org_green_resize_150, sz_150);
			Imgproc.resize(org_blue, org_blue_resize_150, sz_150);
			
			Imgproc.resize(str_red, str_red_resize_150, sz_150);		//input resize
			Imgproc.resize(str_green, str_green_resize_150, sz_150);
			Imgproc.resize(str_blue, str_blue_resize_150, sz_150);
			
			
			if(str_red.cols() != 600 || str_red.rows() != 600)
			{
				Imgproc.resize(str_red, str_red_resize_600, sz_600);		//input resize
				Imgproc.resize(str_green, str_green_resize_600, sz_600);
				Imgproc.resize(str_blue, str_blue_resize_600, sz_600);
				mad = CalculateValue(str_red_resize_600, str_green_resize_600, str_blue_resize_600, org_red, org_green, org_blue, org_red_resize_300, org_green_resize_300, org_blue_resize_300, str_red_resize_300, str_green_resize_300, str_blue_resize_300, org_red_resize_150, org_green_resize_150, org_blue_resize_150, str_red_resize_150, str_green_resize_150, str_blue_resize_150);
			}
			
			else
			{
				mad = CalculateValue(str_red, str_green, str_blue, org_red, org_green, org_blue, org_red_resize_300, org_green_resize_300, org_blue_resize_300, str_red_resize_300, str_green_resize_300, str_blue_resize_300, org_red_resize_150, org_green_resize_150, org_blue_resize_150, str_red_resize_150, str_green_resize_150, str_blue_resize_150);
			}
			

//			mad = CalculateValue(str_red, str_green, str_blue, org_red, org_green, org_blue);
           all[0] = mad;											//mad����
          
           all[1] = ssimg;											//ssimg����

           all[2] = pixel/pixel_max;            					//����ȭ�� �����ȼ��� ����
           
           all[3] = 0;												//����
           
           
           
           if(all[2] >= 1)											//�Ķ���͸� ���Ҷ�max_pixel���� �ִ밪�� �ƴҰ�츦 ���
           {
        	   all[2] = 1;
           }
           
           ssim_center_point = Math.sqrt(Math.pow(true_ssim_center - false_ssim_center,2) + Math.pow(true_pixel_center - false_pixel_center,2));			//ssim-pixel�� ������ �ο��ϱ����ؼ� ���Ǵ� ����  
           ssim_th_point = ssim_center_point / 5;																		

           // DEEP LEARNING  �߰���
           Socket socket = new Socket(qholoserver.Deeplearningip, qholoserver.deeplearningport);		//���̽� ip, port
           System.out.println("\nDeepLearning Server�� ���� �����۾� ����");
           
           String fName2 = "bin/org/6_crop.png";
           
           OutputStream out = socket.getOutputStream();
           FileInputStream fis = new FileInputStream(fName2);
           File imgfile = new File(fName2);
           
           byte buffer[] = new byte[2048];
           
           while (fis.available() > 0) {
        	      out.write(buffer, 0, fis.read(buffer));
        	    }
        	    out.close();
        	    fis.close();
        	    
        	    ServerSocket serverSocket2 = new ServerSocket(qholoserver.myport);
        	    Socket clientsocket = serverSocket2.accept();
        	    InetAddress addr = clientsocket.getInetAddress();
        	    //System.out.println( addr.getHostAddress() + "���� ���� !!");
        	    
        	    String msg = new String();
        	   
        	    InputStream input = clientsocket.getInputStream();
        	    BufferedReader br = new BufferedReader(new InputStreamReader(input));
        	    msg = br.readLine();
        	    
        	    serverSocket2.close();
        	    clientsocket.close();
        	    //System.out.println(msg);
        	    //System.out.println("1".getBytes());
        	    System.out.println(msg);
        	    
                if(msg.equals("Real"))																			
     								//1�� ��ǰ
                {
             	   System.out.println("��ǰ");
             	   NetsendOK();
                    sendOK=true;
                }
                else if(msg.equals("Fake"))																		

     									//1�� ��ǰ
                {
             	   System.out.println("��ǰ");
             	   Netsendfake();
             	   sendOK=true;
             	   
                }
                else
                {
             	  System.out.println("���ν� ���Ǻ��մϴ�.");																	

     								//1�� �Ǻ����� ���ν��ϰ�� (b)������ �����
             	  all[3] = ((-10 /(th2 - th1)) * (mad - th1)) + 5;																

     								//mad�� ���� -5~5
                   realssim = -1 * (Math.sqrt((Math.pow(all[1] - true_ssim_center,2) + Math.pow(all[2] - true_pixel_center,2))));
                   falsessim = Math.sqrt((Math.pow(all[1] - false_ssim_center,2) + Math.pow(all[2] - false_pixel_center,2)));
                   all[3] += (realssim + falsessim) / ssim_th_point;																	

     							//mad�� ������ ����Ǿ��ִ� �迭�� ssim-pixel������ ������
                   if(all[3] >= 4 && sendOK==false )																			

     									//mad�� ������ ssim-pixel������ ���� 4�̻��ϰ�� 2����ǰ
            	   	  {
                 	  System.out.println("��ǰ");
                 	  NetsendOK();
                       sendOK=true;
                   }
                   else if(all[3] < 4 && all[3] > -4 && sendOK==false)																

     							//mad�� ������ ssim-pixel������ ���� 4~-4������ ������� ���ν� ���Կ�
                   {
                 	  System.out.println("���ν�");
                 	  Netsendrecheckcount();
                 	  sendOK=true;
                   }
                   else if( -4 >= all[3] && sendOK==false)																		

     								//mad�� ������ ssim-pixel������ ���� -4�����ϰ�� 2����ǰ
                   {
                 	  System.out.println("��ǰ");
                 	  Netsendfake();
                 	  sendOK=true;
                   }
                     
                }
                
           // Deep
									//5~-5������        
                /*
           if(th1 > all[0] && sendOK==false)																			

								//1�� ��ǰ
           {
        	   System.out.println("��ǰ");
        	   NetsendOK();
               sendOK=true;
           }
           else if(th2 < all[0] && sendOK==false)																		

									//1�� ��ǰ
           {
        	   System.out.println("��ǰ");
        	   Netsendfake();
        	   sendOK=true;
        	   
           }
           */

           long file_app_end = System.currentTimeMillis();
           
           
           
    //       System.out.println((b-a)/1000.0);
   //        System.out.println((d-c)/1000.0);
    //       System.out.println("����qr�ڵ���� = " + (file_qrcode_end - file_qrcode_start )/1000.0);
    //       System.out.println("����crop/warp = " + (file_pro_end - file_pro_start )/1000.0);
     //      System.out.println("�����ν� ���Ȯ�� = " + (file_app_end - file_app_start )/1000.0);
          
    	   
           
           System.out.println(all[0]+"	"+all[1]+"	"+all[2]+"	"+all[3]);

    		  } 
         //  System.out.println(all[0]+"    "+all[1]+"    "+all[2]+"     "+all[3]);         
        /*
           double[][] n = new double[10][10];
        	n=ExcelRead();
           for(int i =0; i<7; i++)
        	   for(int j= 0 ; j<3; j++)
           System.out.print(n[i][j]+"\t");
          
           ExcelRead(result);
            */
          // ExcelWrite(ExcelRead());
  
           /*
           double o_th=0, x_th=0;
           if(edge<61844){
        	   o_th = 53.98862377;
        	   x_th = 61.98862377;
        	   if(result<o_th){
        		   System.out.println("��ǰ");
        	   }
        	   else if(result >= o_th && result <=x_th){
        		   System.out.println("�ٽ����");
        	   }
        	   else if(result > x_th)
        	   {
        		   System.out.println("��ǰ");
        	   }
           }
           else{
        	   o_th = (0.0005 * edge) + 23.066;
        	   x_th = (0.0005 * edge) + 31.066;
        	   if(result<o_th){
        		   System.out.println("��ǰ");
        	   }
        	   else if(result >= o_th && result <=x_th){
        		   System.out.println("�ٽ����");
        	   }
        	   else if(result > x_th)
        	   {
        		   System.out.println("��ǰ");
        	   }
           }
           
           if(result>=120){
              Netsendrecheckcount2();
              sendOK=true;
           }
           */
   		/*
           int th_o=0;
           
           if((result > th_o  && sendOK==false)){
              if(count2==0){
              Netsendrecheckcount();
              System.out.println("�ٽ����2");
              count2++;
              System.out.println(count2+"= count2");
              }
              else if(count2 == 1){
                 System.out.println("��¥");
                 Netsendfake();
                 count2=0;
                       sendOK=true;
              }
               } else if(result <= th_o &&sendOK==false){
                  System.out.println("��ǰ");
                  NetsendOK();
                  count2=0;
                  sendOK=true;
                  }
          */
             }
             
                
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            System.out.println("���� 1");
        } catch (ClassNotFoundException e1) {
         // TODO Auto-generated catch block
            System.out.println("���� 2");

           System.out.println(e1.toString());
         e1.printStackTrace();
      } catch (ArrayIndexOutOfBoundsException e2) {
         if(sendOK==false){ System.out.println("��� ���� ������ �ٽ����");
              Netsendrecheck(); //�ٽ� �����ÿ�! ������ 
              sendOK=true;}
      } catch (NullPointerException e3) { 
         if(sendOK==false){System.out.println("������Ʈ ������ �ٽ����");
            Netsendrecheck(); //�ٽ� �����ÿ�! ������ 
            sendOK=true;}
      }

       
    }
}