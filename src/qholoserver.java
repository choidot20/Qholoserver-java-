
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
            // 리스너 소켓 생성 후 대기
            serverSocket = new ServerSocket(5005);
            System.out.println("서버가 시작되었습니다.");
 
            // 연결되면 통신용 소켓 생성
          while(true){
        	
            socket = serverSocket.accept();
            System.out.println("클라이언트와 연결되었습니다.");
          

            // 파일 수신 작업 시작
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
	
	double th1 = 38.7319 , th2 = 55.0602;					//입력받는 th1,th2
  	double true_ssim_center = 0.9658, false_ssim_center = 0.9138;	//입력받는 ssim.pixel중앙값
  	double true_pixel_center = 0.2271, false_pixel_center = 0.7363;
  	double pixel_max = 43308;						//입력받는 max_pixel값
  	double ssim_center_point = 0;
    double pixel_center_point = 0;
    double canny_check = 1000;			//캐니엣지 NE
   	


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
    private Rectangle rectangleBounds(Point[] coords)						//bounds = 최소X좌표, 최소Y좌표, 폭, 높이
    /* calculate the rectangle bound around the quadrilateral
       defined by the coords[] array
    */
    {
      int xMin = coords[0].x;
      int xMax = coords[0].x;

      int yMin = coords[0].y;
      int yMax = coords[0].y;

      for (int i=1; i < NUM_CORNERS; i++) {
        if (coords[i].x < xMin)												//qr코드좌표 값중 x최솟값
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



    private PerspectiveTransform makeWarpTransform(Point[] coords, int w, int h)								//사변형을 매핑하는 PerspectiveTransform생성 
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
        warpOp = new WarpPerspective( persTF.createInverse() );   // invert the transform							//persTF크기의 warp공간
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
       try {System.out.println("좌표 인식 못함. 다시찍기 접속대기 중...");
       
         bw.write("RECHECK"); 
 //        bw.newLine();
         bw.flush(); //전송
         System.out.println("전송 완료");
         bw.close();        
      
       } catch (IOException e) {
           System.err.println(e); // 에러가 있다면 메시지 출력
           //System.exit(1);
       }
       //*/
   }
    public void Netsendrecheckcount2(){
        //*
        try {System.out.println("원본 매칭 실패. 다시찍기 접속대기 중...");
        
          bw.write("RECHECK2"); 
 //         bw.newLine();
          bw.flush(); //전송
          System.out.println("전송 완료");
       
          bw.close();        
       
        } catch (IOException e) {
            System.err.println(e); // 에러가 있다면 메시지 출력
            //System.exit(1);
        }
        //*/
    }
    public void Netsendrecheckcount(){
       try {
    	   System.out.println("최종 미인식");
       
         bw.write("RERECHECK"); 
//         bw.newLine();
         bw.flush(); //전송
         System.out.println("전송 완료");
         bw.close();        
         //count2++;
       } catch (IOException e) {
           System.err.println(e); // 에러가 있다면 메시지 출력
           //System.exit(1);
       }
       
    }
    public void Netsendrecheckdist(){
          //*
          try {System.out.println("거리 가까워 다시찍기 접속대기중..");
          
            bw.write("RECHECK"); 
            //bw.write("DIST");
//            bw.newLine();
            bw.flush(); //전송
            System.out.println("전송 완료");
            bw.close();        
         
          } catch (IOException e) {
              System.err.println(e); // 에러가 있다면 메시지 출력
              //System.exit(1);
          }
          //*/
      }
    
    public void Netsendfake(){
        //*
        try {System.out.println("가짜 결과 넘김 접속대기중..");
        
         bw.write("FAKE"); 
//         bw.newLine();
         bw.flush(); //전송
         System.out.println("전송 완료");
         
         bw.close();        
      
       } catch (IOException e) {
           System.err.println(e); // 에러가 있다면 메시지 출력
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
         System.out.println("정품 결과 넘김 접속 기다리는 중..");
       
         bw.write("OK"); 
//         bw.newLine();
         bw.flush(); //전송
         System.out.println("전송 완료");
         bw.close();      
     
       } catch (IOException e) {
           System.err.println(e); // 에러가 있다면 메시지 출력
          
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
				width_300 = org_red_300.rows(), height_300 = org_red_300.cols(), width_150 = org_red_150.rows(), height_150 = org_red_150.cols(); 				//영상 범위 변수

		List<Integer> o_red_600 = new ArrayList<>();							//1차원 배열에 mat포맷의 영상을 넣기위해 LIST형 변수 할당(o_red = 원본 red영상, i_red = 입력 red영상)
		List<Integer> i_red_600 = new ArrayList<>();							//변수의 i와 o는 입력과 원본영상 구분을 red,green,blue는 영상의 색상을 
		List<Integer> o_green_600 = new ArrayList<>();							//990,495,247은 영상의 크기를 구분한다.
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
		

		
		double[] red_input_600, blue_input_600, green_input_600, red_input_300, blue_input_300, green_input_300, red_input_150, blue_input_150, green_input_150;	//mat포맷의 영상을 1차원 배열에 넣기위해 할당되는 1차원 배열			
		red_input_600 = new double[org_width_600 * org_height_600];																											//input은 입력영상을 org는 원본영상을 넣기위한 배열이다.													
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

		for (int i = 0; i < org_red_600.cols(); i++) {								//1차원 배열에 mat포맷의 영상을 넣어줌(990x990)
			for (int j = 0; j < org_red_600.rows(); j++) {
				o_red_600.add((int) org_red_600.get(i, j)[0]);
				o_green_600.add((int) org_green_600.get(i, j)[0]);
				o_blue_600.add((int) org_blue_600.get(i, j)[0]);
				
				red_org_600[i * org_width_600 + j] = o_red_600.get(i * org_width_600 + j);
				green_org_600[i * org_width_600 + j] = o_green_600.get(i * org_width_600 + j);
				blue_org_600[i * org_width_600 + j] = o_blue_600.get(i * org_width_600 + j);
			}

		}
		
		for (int i = 0; i < input_red_600.cols(); i++) {								//1차원 배열에 mat포맷의 영상을 넣어줌(990x990)
			for (int j = 0; j < input_red_600.rows(); j++) {
				
				i_red_600.add((int) input_red_600.get(i, j)[0]);
				i_green_600.add((int) input_green_600.get(i, j)[0]);
				i_blue_600.add((int) input_blue_600.get(i, j)[0]);

				red_input_600[i * input_width_600 + j] = i_red_600.get(i * input_width_600 + j);
				green_input_600[i * input_width_600 + j] = i_green_600.get(i * input_width_600 + j);
				blue_input_600[i * input_width_600 + j] = i_blue_600.get(i * input_width_600 + j);
			}

		}
		
		
		
		for (int i = 0; i < org_red_300.rows(); i++) {									//1차원 배열에 mat포맷의 영상을 넣어줌(495x495)
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
		
		for (int i = 0; i < org_red_150.rows(); i++) {									//1차원 배열에 mat포맷의 영상을 넣어줌(247x247)
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
		
		for (int k = -2; k <= 2; k++) {																								//247x247영상의 공간정합오차의 좌표를 구함
			for (int l = -2; l <= 2; l++) {																							//공간정합오차 = v = |org(r)-input(r)| + |org(g)-input(g)| + |org(b)-input(b)| / count의 최솟값
				for (int i = 0; i < height_150 ; i++) {
					for (int j = 0; j < width_150; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < width_150 && j - l < height_150)) {								//영상이 겹치는 부분만 비교하기위한 if문

							V = Math.abs(red_org_150[i * width_150 + j] - red_input_150[(i - k) * width_150 + (j - l)])				//공간정합오차를 구한다.
									+ Math.abs(green_org_150[i * width_150 + j] - green_input_150[(i - k) * width_150 + (j - l)])
									+ Math.abs(blue_org_150[i * width_150 + j] - blue_input_150[(i - k) * width_150 + (j - l)]);
		
							Vsum += V;
							count++;				
						}

					}
				}
				Vmean = Vsum / count;

				if (min > Vmean) {																									//mad의 최솟값을 찾아준다/
					min = Vmean;
					x_150 = k;																										//mad가 최솟값일때 x좌표
					y_150 = l;																										//mad가 최솟값일때 y좌표
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
		

		x_f_300 = 2 * x_150;										//247x247영상의 2배의 크기이므로 *2를해줌
		y_f_300 = 2 * y_150;
		
		for (int k = y_f_300 - 3; k <= y_f_300 + 3; k++) {			//495x495영상의 공간정합오차의 좌표를 구함
			for (int l = x_f_300 - 3; l <= x_f_300 + 3; l++) {
				for (int i = 0; i < height_300; i++) {
					for (int j = 0; j < width_300; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < width_300 && j - l < height_300)) {   //영상이 겹치는 부분만 비교하기위한 if문

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
		
		for (int k = y_f_600 - 3; k <= y_f_600 + 3; k++) {			//990x990영상의 공간정합오차의 좌표를 구함
			for (int l = x_f_600 - 3; l <= x_f_600 + 3; l++) {
				for (int i = 0; i < org_height_600; i++) {
					for (int j = 0; j < org_width_600; j++) {
						if ((i - k >= 0 && j - l >= 0 && i - k < org_width_600 && j - l < org_height_600)) {	//영상이 겹치는 부분만 비교하기위한 if문

							V = Math.abs(red_org_600[i * org_width_600 + j] - red_input_600[(i - k) * org_width_600 + (j - l)])
									+ Math.abs(green_org_600[i * org_width_600 + j] - green_input_600[(i - k) * org_width_600 + (j - l)])
									+ Math.abs(blue_org_600[i * org_width_600 + j] - blue_input_600[(i - k) * org_width_600 + (j - l)]);
		
							Vsum += V;
							count++;
							if(V > pixel_th)					//공간정합오차중에 v의 값이 150이상인 픽셀의 수(두영상의 차이가 큰곳의 갯수 찾는것)
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
		
		
		
		
		for(int i = 10; i < org_width_600 - 10; i++)																						//ssim구하기 위해 분산,평균,공분산을 구함 
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
		
		
		new_og_avg = new_og_sum / count_green;																				//입력영상과 원본영상의 평균
		new_ig_avg = new_ig_sum / count_green;	
		new_og_var = (new_og_var / count_green) - (new_og_avg * new_og_avg);												//입력영상과 원본영상의 분산
		new_ig_var = (new_ig_var / count_green) - (new_ig_avg * new_ig_avg);
		new_g_cov = (new_g_cov / count_green) - (new_og_avg * new_ig_avg);													//공분산
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
		  
//		  Imgcodecs.imwrite(path+"/6_crop_gray.png", src_gray);											//디버깅용 이미지 저장			
		  Imgproc.Canny(src_gray,detected_img,125.0, 250.0);											//디버깅용 이미지 저장
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
        	
            System.out.println("파일 수신 작업을 시작합니다.");
                        
            bw=new BufferedWriter(new OutputStreamWriter( socket.getOutputStream()) );               
            dis = new DataInputStream(socket.getInputStream());
             // 파일명을 전송 받고 파일명 수정.
            String fName = dis.readUTF();
            System.out.println("파일명 " + fName + "을 전송받았습니다.");
            String path = qholoserver.class.getResource("").getPath(); 					// path = 현재 클래스의 절대 경로를 가져온다.
            // 파일을 생성하고 파일에 대한 출력 스트림 생성
            File f = new File(path+fName);
            fos = new FileOutputStream(f);
            bos = new BufferedOutputStream(fos);
            System.out.println(fName + "파일을 생성하였습니다.");
            
            
            
            // 바이트 데이터를 전송받으면서 기록
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

      
            System.out.println(path); //--> 절대 경로가 출력됨           
            //File imageFile = new File(path + fName);											//imageFile = 어플로부터받은 사용자가 촬영한 사진
            File imageFile = new File(path+"4.jpg");	// test code 지울것
            
            BufferedImage image = ImageIO.read(imageFile);										//어플로 받은 image를 Buffreimage에 저장
            int w = image.getWidth(null);  														//w, h = 900
            int h = image.getHeight(null);  
            System.out.println(w+ " " + h);
            
//            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//            ColorConvertOp op = new ColorConvertOp(cs, null);
//           BufferedImage imagefix = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY );   	//BufferedImage(w,h,imageType)
//            imagefix = op.filter(image, null);													//grayscale이미지 변환
//            saveImage("grayImage",imagefix,path+"1_gray.png");									//촬영이미지의 grayscale이미지 저장
          
            BufferedImage imagebi = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);		//900 * 900크기의 Binary이미지버퍼영역 생성
            
            long a = System.currentTimeMillis();
            Mat grayimage = bufferedImageToMat(image);
            int gray_th = grayscale(grayimage);
            long b = System.currentTimeMillis();
            
        //    BufferedImage imagebm=new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
          
          
            long c = System.currentTimeMillis();
        	
           for(int xx=0;xx<w;xx++)
        	   for(int yy=0;yy<h;yy++){
              
              rgb = image.getRGB(xx,yy);						//xx,yy좌표의 rgb값추출
              
         //     gray= imagefix.getRGB(xx,yy);
               
              red = (rgb >> 16 ) & 0x000000FF;					//red값 추출
               green = (rgb >> 8 ) & 0x000000FF;				//green값 추출
               blue = (rgb) & 0x000000FF;						//blue값 추출
               
             double check_QRCODE = (0.5042 * gray_th) - 20.298;
               
          //   if(blue>50&&red>50&&green>50)
           if(((blue + red + green)/3) > check_QRCODE)
               imagebi.setRGB(xx, yy,Color.WHITE.getRGB()); //else imagebi.setRGB(xx, yy, Color.BLACK.getRGB());		imagebi = r>127, g>127, b>127인 색상을 하얀색으로 저장 
            
             
         /*    if(((gray) & 0x000000FF)>127)
            	 imagebm.setRGB(xx, yy,Color.WHITE.getRGB()); 
             else imagebm.setRGB(xx, yy,Color.BLACK.getRGB()); //그레이스케일도 역시 INT값을 갖으나 rgb값이 각각 동일함.
           */                                                                                   // ex. (100,100,100) or (98,98,98)등 r=100 ,g=100,b=100
             
           }
           
           long d = System.currentTimeMillis();
       	
           saveImage("imagebi",imagebi,path+"imagebi.png");												//디버깅
           Mat ocrop_Image = bufferedImageToMat(imagebi);
           //Mat ocrop_Image = Imgcodecs.imread("bin/imagebi.png");											//디버깅
           Mat red_img = new Mat();
           Mat kernel = Mat.ones(4,4, CvType.CV_32F);
           
           Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_OPEN, kernel);
           //Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_ERODE, kernel);
          Imgcodecs.imwrite("bin/ERODE_imagebi.png",red_img);											//디버깅
           
          imagebi = mat2Img(red_img);																//침식팽창
         imagebi = loadImage("ERODE_imagebi.png");														//디버깅
           
            
        /*    int[] rgbarr = new int[w * h];  
            rgbarr = image.getRGB(0, 0, w, h, rgbarr, 0, w);  */
            ResultPointABC pointsABC = new ResultPointABC(); 									//pointsABC객체생성
            pointsABC.setABC(0, 0, 0, 0, 0, 0,0,0); //KYT RESET       							//QR코드의 좌표
            try {
               
                  LuminanceSource source =new BufferedImageLuminanceSource(imagebi);			//bitmap 생성자를 만들기위해 imagebi데이터를 LuminanceSource형태로 바꿈
                   BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));			//bitmap 생성자를 만들기위해서 생성자를 만듬 BinaryBitmap(Binarizer binarizer), Binarizer(LuminanceSource source)  
                   QRCodeReader reader=new QRCodeReader();										//객체생성
                   Result result = reader.decode(bitmap);										//bitmap의 QR코드를 찾아서 해독
                   System.out.println("Barcode text: " + result.getText());
                   if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0)		

                	   //qr코드의 좌표가 하나라도 0일경우
                   {																						

																							

                	   	//foundpattren = false = 다시찍기메시지 전송
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
               
              for(int t=1;t<4;t++){ //위에서 좌표가 검출 되지 않으면, 세번더 90도씩 회전시켜 좌표가 검출되는지 확인. 
                 pointsABC.setABC(0, 0, 0, 0, 0, 0,0,0); //KYT RESET
                    
                   
                 try {

                          LuminanceSource source =new BufferedImageLuminanceSource(getRotateImage(imagebi,90));
                         BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                         QRCodeReader reader=new QRCodeReader();                     
                         Result result = reader.decode(bitmap);									//bitmap의 QR코드를 찾아서 해독
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
                    image=getRotateImage(image,t*90); //원본 이미지를 회전시킨 만큼 회전시켜줘야 정확한 좌표에 대응                     
                    t=5;
                    System.out.println("패턴찾은 Try "+t+"번째에: ");   
                 } 
         
              }
           }
            if( foundpattern==false && sendOK==false)
            {
            	Netsendrecheck();sendOK=true;
            } //좌표 하나라도 0이면 다시 찍어주세요.  
            
            if( sendOK==false && ((int)pointsABC.getCx()-(int)pointsABC.getBx())<110 ) {System.out.println("거리가 멀어 다시찍기요청 a");Netsendrecheckdist();sendOK=true;} //인식점 좌표 간격이 하나라도 110이하 이면 다시 찍어주세요.  
            if( sendOK==false && ((int)pointsABC.getAy()-(int)pointsABC.getBy())<110 ) {System.out.println("거리가 멀어 다시찍기요청 b");Netsendrecheckdist();sendOK=true;}
            if( sendOK==false && ((int)pointsABC.getCx()-(int)pointsABC.getAx())<110 ) {System.out.println("거리가 멀어 다시찍기요청 c");Netsendrecheckdist();sendOK=true;}            
           if( sendOK==false && ((int)pointsABC.getAy()-(int)pointsABC.getCy())<110 ) {System.out.println("거리가 멀어 다시찍기요청 d");Netsendrecheckdist();sendOK=true;}   
  */          
  //         long file_qrcode_end = System.currentTimeMillis();
           
 
   //        long file_pro_start = System.currentTimeMillis();
           
           
           //QR코드인식후 각 QR코드좌표-> pointsABC객체에 저장되어있음
           
             if(sendOK==false){
             Point[] coords = new Point[NUM_CORNERS];
             coords[0] = new Point( (int)pointsABC.getBx(), (int)pointsABC.getBy()); 						//coodrs[0] = 왼쪽상단 qr좌표
             coords[1] = new Point( (int)pointsABC.getCx(), (int)pointsABC.getCy()); 						//coodrs[1] = 오른쪽상단 qr좌표
             coords[2] = new Point( (int)pointsABC.getDx(), (int)pointsABC.getDy()); 						//coodrs[2] = 오른쪽하단 qr좌표
             coords[3] = new Point( (int)pointsABC.getAx(), (int)pointsABC.getAy()); 						//coodrs[3] = 왼쪽하단 qr좌표
       //      System.out.println(coords[0]+"  "+coords[1]+"  "+coords[2]+"  "+coords[3]);
             
             
            // crop the image so it tightly bounds the quadrilateral
             Rectangle bounds = rectangleBounds(coords);													//bounds = 최소X좌표, 최소Y좌표, 폭, 높이
             BufferedImage boundedIm = cropImage(image, bounds.x, bounds.y, 
                                                           bounds.width, bounds.height);					//boundedIm = 촬영영상이 crop된 이미지
             saveImage("bounded", boundedIm, path+"4_bounded.png"); //KYT removes							//이미지 저장(디버깅)
             boundedIm = loadImage("4_bounded.png"); //KYT removes											//이미지 저장(디버깅)

             // adjust coordinates to apply to bounded image
             for (int i=0; i < NUM_CORNERS; i++) 															//bounded image에 적할 좌표 조정
             {
               coords[i].x -= bounds.x;
               coords[i].y -= bounds.y;
             }
            
             // convert the quadrilteral into a perspective transform
             PerspectiveTransform persTF = makeWarpTransform(coords, WARP_WIDTH, WARP_HEIGHT);				//wrap해주기 위해서 coords의 좌표를 990x990으로 바꿔줌
             																					

	//persTF = coords[]
             // apply perspective tranformation to image
             System.setProperty("com.sun.media.jai.disableMediaLib", "true"); // 에러 수정
             BufferedImage warpIm = warpImage(boundedIm, persTF);								//wrapIm = crop된 이미지(boundedim)를 wrap해준 이미지
 //            saveImage("warpIm", warpIm, path+"4_warpIm.png"); //KYT removes								//디버깅용 이미지 저장
             
             if (warpIm == null)
              // return;
             System.out.println("warpImage Not Found Code! please Retry!!"); 
             // find top-left coordinate of warped image
             Point topLeft = findWarpTopLeft(boundedIm, persTF);											//bounded image(crop된 이미지)의 왼쪽 상단좌표값 구함 
             if (topLeft == null)
              // return; // 명령창 화면 꺼져버림 , 종료
             System.out.println("Not Found Code! please Retry!!"); 
             BufferedImage cropIm = cropWarp(warpIm, topLeft, WARP_WIDTH, WARP_HEIGHT);						//wrapIm(crop된 이미지를 wrap해준 이미지)를 topleft좌표와 990,990값을 이용 crop해준다.
     
             

             saveImage("warped and cropped", cropIm, path+"6_crop.png");
             
             
   //          long file_pro_end = System.currentTimeMillis();
             
       //      long file_app_start = System.currentTimeMillis();
             
             
       /*      cropIm= loadImage("6_crop.png");
             
             BufferedImage cropcropim = cropImage(cropIm,140,140,850,850);
             saveImage("qr_crop",cropcropim,path+"QR_crop.png");
             Mat qr_crop_image = Imgcodecs.imread("bin/org/qw_crop.png");
             */

//           Mat crop_Image = Imgcodecs.imread("bin/6_crop.png", Imgcodecs.CV_LOAD_IMAGE_COLOR);						//디버깅용
//          Mat canny_image = Imgcodecs.imread("bin/4_bounded.png", Imgcodecs.CV_LOAD_IMAGE_COLOR);					//디버깅용저장
             Mat tgt = Imgcodecs.imread("bin/6_crop.png");
             Mat ref = Imgcodecs.imread("bin/org/6_crop.png");
             
             SplitRGB(ref);
//    		Mat tgt2 = bufferedImageToMat(cropIm);
//    		Imgcodecs.imwrite("bin/6_croptobuffer.png",tgt2);
           
             histMatch(ref, tgt, path);
    		
    		
    		
    		
      /*     int blur = 0;																					

		//이미지의 엣지값을 저장하는 변수
           double canny_check_th = (canny_check * 0.5);																//영상의 +-60%

범위를 th범위로 잡음
           double canny_check_th1 = (canny_check + canny_check_th);
           double canny_check_th2 = (canny_check - canny_check_th);
           blur = Canny(qr_crop_image, path);																			

//영상의 엣지값을 구해준다
            
            
            
            if((sendOK == false && canny_check_th2 >= blur) || (sendOK==false && blur >= canny_check_th1)){ 			//촬영된 영상의 엣지값이 th범위를 벗어날경우 재찰영 메세지 넘김
            	System.out.println("블러 이미지");
            	Netsendrecheck(); //다시 찍으시오! 보내기 
            	sendOK = true;
            	}
            if(sendOK == false){

        */  
        //    Mat aqswde = Imgcodecs.imread("N:/600/c2/6_crop.png", CvType.CV_8UC1); 
             
    		int blur = 0;
    		int blurcheck = 0;
    		blur = Canny(edge);	
    		blurcheck = (int) (canny_check * 0.25);
    		
    		  if((sendOK == false && blur < blurcheck)){ 			//촬영된 영상의 엣지값이 th범위를 벗어날경우 재찰영 메세지 넘김
              	System.out.println("블러 이미지");
              	Netsendrecheck(); //다시 찍으시오! 보내기 
              	sendOK = true;
              	}
              
    		if(sendOK == false){
    		  
    		  																			
    		//영상의 히스토그램을 맞춰준다.																			
        
    //       System.out.println("매칭 중......");
           
          // int n = Search(crop_Image, str_red);
         
   		
           
           
           
          // double edge = Canny(crop_Image, "bin/");
           //int result = CalculateValue(str_red, str_green, str_blue, org_red, org_green, org_blue);
           //int[] graph = {0};
           double mad = 0;
           double ssim_th_point  = 0;
           double realssim = 0;
           double falsessim = 0;
           
            Mat org_red_resize_300 = new Mat();				//원본 resize사진을 저장하는 공간(495)
			Mat org_green_resize_300 = new Mat();
			Mat org_blue_resize_300 = new Mat();
			
			Mat org_red_resize_150 = new Mat();				//원본 resize사진을 저장하는 공간(247)
			Mat org_green_resize_150 = new Mat();
			Mat org_blue_resize_150 = new Mat();
			
			
			Mat str_red_resize_300 = new Mat();				//입력 resize사진을 저장하는 공간
			Mat str_green_resize_300 = new Mat();
			Mat str_blue_resize_300 = new Mat();
			
			Mat str_red_resize_600 = new Mat();
			Mat str_green_resize_600 = new Mat();
			Mat str_blue_resize_600 = new Mat();
			
			Mat str_red_resize_150 = new Mat();				
			Mat str_green_resize_150 = new Mat();
			Mat str_blue_resize_150 = new Mat();
			
			Size sz_600 = new Size(600,600);
			Size sz_300 = new Size(300,300);			//resize크기 (495)
			Size sz_150 = new Size(150,150);			//resize크기 (247)
          
           
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
           all[0] = mad;											//mad저장
          
           all[1] = ssimg;											//ssimg저장

           all[2] = pixel/pixel_max;            					//정규화된 오차픽셀수 저장
           
           all[3] = 0;												//점수
           
           
           
           if(all[2] >= 1)											//파라미터를 구할때max_pixel값이 최대값이 아닐경우를 대비
           {
        	   all[2] = 1;
           }
           
           ssim_center_point = Math.sqrt(Math.pow(true_ssim_center - false_ssim_center,2) + Math.pow(true_pixel_center - false_pixel_center,2));			//ssim-pixel의 점수를 부여하기위해서 사용되는 변수  
           ssim_th_point = ssim_center_point / 5;																		

           // DEEP LEARNING  추가분
           Socket socket = new Socket(qholoserver.Deeplearningip, qholoserver.deeplearningport);		//파이썬 ip, port
           System.out.println("\nDeepLearning Server로 파일 전송작업 시작");
           
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
        	    //System.out.println( addr.getHostAddress() + "님이 접속 !!");
        	    
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
     								//1차 정품
                {
             	   System.out.println("정품");
             	   NetsendOK();
                    sendOK=true;
                }
                else if(msg.equals("Fake"))																		

     									//1차 가품
                {
             	   System.out.println("가품");
             	   Netsendfake();
             	   sendOK=true;
             	   
                }
                else
                {
             	  System.out.println("미인식 재판별합니다.");																	

     								//1차 판별에서 미인식일경우 (b)구역에 들어갈경우
             	  all[3] = ((-10 /(th2 - th1)) * (mad - th1)) + 5;																

     								//mad의 점수 -5~5
                   realssim = -1 * (Math.sqrt((Math.pow(all[1] - true_ssim_center,2) + Math.pow(all[2] - true_pixel_center,2))));
                   falsessim = Math.sqrt((Math.pow(all[1] - false_ssim_center,2) + Math.pow(all[2] - false_pixel_center,2)));
                   all[3] += (realssim + falsessim) / ssim_th_point;																	

     							//mad의 점수가 저장되어있는 배열에 ssim-pixel점수를 더해줌
                   if(all[3] >= 4 && sendOK==false )																			

     									//mad의 점수와 ssim-pixel점수의 합이 4이상일경우 2차정품
            	   	  {
                 	  System.out.println("정품");
                 	  NetsendOK();
                       sendOK=true;
                   }
                   else if(all[3] < 4 && all[3] > -4 && sendOK==false)																

     							//mad의 점수와 ssim-pixel점수의 합이 4~-4점수를 가질경우 미인식 재촬영
                   {
                 	  System.out.println("미인식");
                 	  Netsendrecheckcount();
                 	  sendOK=true;
                   }
                   else if( -4 >= all[3] && sendOK==false)																		

     								//mad의 점수와 ssim-pixel점수의 합이 -4이하일경우 2차가품
                   {
                 	  System.out.println("가품");
                 	  Netsendfake();
                 	  sendOK=true;
                   }
                     
                }
                
           // Deep
									//5~-5점까지        
                /*
           if(th1 > all[0] && sendOK==false)																			

								//1차 정품
           {
        	   System.out.println("정품");
        	   NetsendOK();
               sendOK=true;
           }
           else if(th2 < all[0] && sendOK==false)																		

									//1차 가품
           {
        	   System.out.println("가품");
        	   Netsendfake();
        	   sendOK=true;
        	   
           }
           */

           long file_app_end = System.currentTimeMillis();
           
           
           
    //       System.out.println((b-a)/1000.0);
   //        System.out.println((d-c)/1000.0);
    //       System.out.println("영상qr코드검출 = " + (file_qrcode_end - file_qrcode_start )/1000.0);
    //       System.out.println("영상crop/warp = " + (file_pro_end - file_pro_start )/1000.0);
     //      System.out.println("영상인식 결과확인 = " + (file_app_end - file_app_start )/1000.0);
          
    	   
           
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
        		   System.out.println("정품");
        	   }
        	   else if(result >= o_th && result <=x_th){
        		   System.out.println("다시찍기");
        	   }
        	   else if(result > x_th)
        	   {
        		   System.out.println("가품");
        	   }
           }
           else{
        	   o_th = (0.0005 * edge) + 23.066;
        	   x_th = (0.0005 * edge) + 31.066;
        	   if(result<o_th){
        		   System.out.println("정품");
        	   }
        	   else if(result >= o_th && result <=x_th){
        		   System.out.println("다시찍기");
        	   }
        	   else if(result > x_th)
        	   {
        		   System.out.println("가품");
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
              System.out.println("다시찍기2");
              count2++;
              System.out.println(count2+"= count2");
              }
              else if(count2 == 1){
                 System.out.println("가짜");
                 Netsendfake();
                 count2=0;
                       sendOK=true;
              }
               } else if(result <= th_o &&sendOK==false){
                  System.out.println("정품");
                  NetsendOK();
                  count2=0;
                  sendOK=true;
                  }
          */
             }
             
                
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            System.out.println("에러 1");
        } catch (ClassNotFoundException e1) {
         // TODO Auto-generated catch block
            System.out.println("에러 2");

           System.out.println(e1.toString());
         e1.printStackTrace();
      } catch (ArrayIndexOutOfBoundsException e2) {
         if(sendOK==false){ System.out.println("어레이 오버 에러로 다시찍기");
              Netsendrecheck(); //다시 찍으시오! 보내기 
              sendOK=true;}
      } catch (NullPointerException e3) { 
         if(sendOK==false){System.out.println("널포인트 에러로 다시찍기");
            Netsendrecheck(); //다시 찍으시오! 보내기 
            sendOK=true;}
      }

       
    }
}