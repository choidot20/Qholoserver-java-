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
    

   	static Mat str_red = new Mat();
    static Mat str_green = new Mat();
    static Mat str_blue = new Mat();
    static Mat edge = new Mat();
    
   //graph = new int[100];


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
         bw.flush(); //����
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
          bw.flush(); //����
          bw.close();        
       
        } catch (IOException e) {
            System.err.println(e); // ������ �ִٸ� �޽��� ���
            //System.exit(1);
        }
        //*/
    }
    public void Netsendrecheckcount(){
       try {System.out.println("��ǰ���� �ν�. �ٽ� Ȯ��");
       
         bw.write("RERECHECK"); 
//         bw.newLine();
         bw.flush(); //����
         bw.flush(); //����
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
            bw.flush(); //����
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
         bw.flush(); //����
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
         bw.flush(); //����
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
		  System.out.println("�����ȼ��� = " + count+"\t");
		  
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
        	
            System.out.println("���� ���� �۾��� �����մϴ�.");
                        
               bw=new BufferedWriter(new OutputStreamWriter( socket.getOutputStream()) );               
             dis = new DataInputStream(socket.getInputStream());
             // ���ϸ��� ���� �ް� ���ϸ� ����.
            String fName = dis.readUTF();
            System.out.println("���ϸ� " + fName + "�� ���۹޾ҽ��ϴ�.");
            String path = Original_extraction.class.getResource("").getPath(); 					// path = ���� Ŭ������ ���� ��θ� �����´�.
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
            File imageFile = new File(path + fName);											//imageFile = ���÷κ��͹��� ����ڰ� �Կ��� ����
           
            BufferedImage image = ImageIO.read(imageFile);										//���÷� ���� image�� Buffreimage�� ����
            int w = image.getWidth(null);  														//w, h = 900
            int h = image.getHeight(null);  
          
            
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
       	
//           saveImage("imagebi",imagebi,path+"imagebi.png");												//�����
           Mat ocrop_Image = bufferedImageToMat(imagebi);
//           Mat ocrop_Image = Imgcodecs.imread("bin/imagebi.png");											//�����
           Mat red_img = new Mat();
           Mat kernel = Mat.ones(3,3, CvType.CV_32F);
           
           Imgproc.morphologyEx(ocrop_Image, red_img, Imgproc.MORPH_OPEN, kernel);
//           Imgcodecs.imwrite("bin/ERODE_imagebi.png",red_img);											//�����
           
          imagebi = mat2Img(red_img);																//ħ����â
//         imagebi = loadImage("ERODE_imagebi.png");														//�����
           													//�����
           
            
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
            
           
           
           //QR�ڵ��ν��� �� QR�ڵ���ǥ-> pointsABC��ü�� ����Ǿ�����
           
             if(sendOK==false){
             Point[] coords = new Point[NUM_CORNERS];
             coords[0] = new Point( (int)pointsABC.getBx(), (int)pointsABC.getBy()); 						//coodrs[0] = ���ʻ�� qr��ǥ
             coords[1] = new Point( (int)pointsABC.getCx(), (int)pointsABC.getCy()); 						//coodrs[1] = �����ʻ�� qr��ǥ
             coords[2] = new Point( (int)pointsABC.getDx(), (int)pointsABC.getDy()); 						//coodrs[2] = �������ϴ� qr��ǥ
             coords[3] = new Point( (int)pointsABC.getAx(), (int)pointsABC.getAy()); 						//coodrs[3] = �����ϴ� qr��ǥ
             
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
             BufferedImage warpIm = warpImage(boundedIm, persTF);											//wrapIm = crop�� �̹���(boundedim)�� wrap���� �̹���
//             saveImage("warpIm", warpIm, path+"4_warpIm.png"); //KYT removes								//������ �̹��� ����
             
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
             
             Mat org_img = Imgcodecs.imread("bin/Original_extraction/6_crop.png");
             
            		 
            		 
  
    		int blur = 0;
    		int blurcheck = 0;
  
    		if(sendOK == false){
    			
    			Canny(org_img);
    		
    			
    			System.out.println("���� �Ϸ�");
    			
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