package Original_extraction;

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

 
public class Original_extraction {
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
    

   	static Mat str_red = new Mat();
    static Mat str_green = new Mat();
    static Mat str_blue = new Mat();
    static Mat edge = new Mat();
    
   //graph = new int[100];


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
         bw.flush(); //전송
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
          bw.flush(); //전송
          bw.close();        
       
        } catch (IOException e) {
            System.err.println(e); // 에러가 있다면 메시지 출력
            //System.exit(1);
        }
        //*/
    }
    public void Netsendrecheckcount(){
       try {System.out.println("가품으로 인식. 다시 확인");
       
         bw.write("RERECHECK"); 
//         bw.newLine();
         bw.flush(); //전송
         bw.flush(); //전송
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
            bw.flush(); //전송
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
         bw.flush(); //전송
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
         bw.flush(); //전송
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
	        System.out.println(v);
	        return v;
	}
    public void SplitRGB(Mat crop_Img, Mat red_img, Mat blue_img, Mat green_img) {

        List<Mat> RGB = new ArrayList<Mat>(3);
        Core.split(crop_Img, RGB);
        red_img = RGB.get(2);
        green_img = RGB.get(1);
        blue_img = RGB.get(0);

        Imgcodecs.imwrite("bin/Red.png", red_img);
        Imgcodecs.imwrite("bin/Green.png", green_img);
        Imgcodecs.imwrite("bin/Blue.png", blue_img);

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
		  System.out.println("엣지픽셀값 = " + count+"\t");
		  
		  return count;
		  
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
            String path = Original_extraction.class.getResource("").getPath(); 					// path = 현재 클래스의 절대 경로를 가져온다.
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
            File imageFile = new File(path + fName);											//imageFile = 어플로부터받은 사용자가 촬영한 사진
           
            BufferedImage image = ImageIO.read(imageFile);										//어플로 받은 image를 Buffreimage에 저장
            int w = image.getWidth(null);  														//w, h = 900
            int h = image.getHeight(null);  
          
            
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
       	
//           saveImage("imagebi",imagebi,path+"imagebi.png");												//디버깅
           Mat ocrop_Image = bufferedImageToMat(imagebi);
//           Mat ocrop_Image = Imgcodecs.imread("bin/imagebi.png");											//디버깅
           Mat red_img = new Mat();
           Mat kernel = Mat.ones(3,3, CvType.CV_32F);
           
           Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_OPEN, kernel);
//           Imgcodecs.imwrite("bin/ERODE_imagebi.png",red_img);											//디버깅
           
          imagebi = mat2Img(red_img);																//침식팽창
//         imagebi = loadImage("ERODE_imagebi.png");														//디버깅
           													//디버깅
           
            
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
          
                System.out.println(e.toString());
                   e.printStackTrace();

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
                System.out.println(ie.toString());ie.printStackTrace();
             }
          }

          
            
           
           if((int)pointsABC.getBx()==0||(int)pointsABC.getBy()==0||(int)pointsABC.getCx()==0||(int)pointsABC.getCy()==0||(int)pointsABC.getAx()==0||(int)pointsABC.getAy()==0){
               
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
            
           
           
           //QR코드인식후 각 QR코드좌표-> pointsABC객체에 저장되어있음
           
             if(sendOK==false){
             Point[] coords = new Point[NUM_CORNERS];
             coords[0] = new Point( (int)pointsABC.getBx(), (int)pointsABC.getBy()); 						//coodrs[0] = 왼쪽상단 qr좌표
             coords[1] = new Point( (int)pointsABC.getCx(), (int)pointsABC.getCy()); 						//coodrs[1] = 오른쪽상단 qr좌표
             coords[2] = new Point( (int)pointsABC.getDx(), (int)pointsABC.getDy()); 						//coodrs[2] = 오른쪽하단 qr좌표
             coords[3] = new Point( (int)pointsABC.getAx(), (int)pointsABC.getAy()); 						//coodrs[3] = 왼쪽하단 qr좌표
             
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
             BufferedImage warpIm = warpImage(boundedIm, persTF);											//wrapIm = crop된 이미지(boundedim)를 wrap해준 이미지
//             saveImage("warpIm", warpIm, path+"4_warpIm.png"); //KYT removes								//디버깅용 이미지 저장
             
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
             
             Mat org_img = Imgcodecs.imread("bin/Original_extraction/6_crop.png");
             
            		 
            		 
  
    		int blur = 0;
    		int blurcheck = 0;
  
    		if(sendOK == false){
    			
    			Canny(org_img);
    		
    			
    			System.out.println("추출 완료");
    			
    			Netsendrecheck();
 
           
          // double edge = Canny(crop_Image, "bin/");
           //int result = CalculateValue(str_red, str_green, str_blue, org_red, org_green, org_blue);
           //int[] graph = {0};

           
//           System.out.println(all[0]+"	"+all[1]+"	"+all[2]+"	"+all[3]);

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