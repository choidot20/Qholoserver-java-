/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.datamatrix.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.DetectorResult2;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.detector.WhiteRectangleDetector;
import com.google.zxing.common.detector.WhiteRectangleDetector2;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 */
public final class Detector2 {

  private final BitMatrix image;
  private final BitMatrix image1;
  private final BitMatrix image2;
  private final WhiteRectangleDetector rectangleDetector; //요놈이 네모찾는것. 
  private final WhiteRectangleDetector rectangleDetector2; //요놈이 네모찾는것. 

  public Detector2(BitMatrix image) throws NotFoundException {
	this.image = image;
    image1=new BitMatrix(240,240);
    image2=new BitMatrix(240,240);
   // Log.d("디텍터 초기화->", Integer.toString(image.getWidth())+"X"+Integer.toString(image.getHeight()));//W480*H240
    //Log.d("image1 alloc", "image1 g할당중이다.");//W480*H240
   
    for(int y=0;y<240;y++){
    	for(int x=0;x<240;x++){
    		if(image.get(x,y))image1.set(x,y);    		
    	}
     }
     
    for(int y=0;y<240;y++){
    	for(int x=240;x<480;x++){
    		if(image.get(x,y))image2.set((x-240),y);    		
    	}
     }
    //Log.d("image1, image2", image.toString()+"\n\n\n"+image1.toString()+"\n\n\n"+image2.toString());//W480*H240
    rectangleDetector = new WhiteRectangleDetector(image1);
    rectangleDetector2 = new WhiteRectangleDetector(image2);
    
   
 
  }

  /**
   * <p>Detects a Data Matrix Code in an image.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a Data Matrix Code
   * @throws NotFoundException if no Data Matrix Code can be found
   */
 public DetectorResult2 detect() throws NotFoundException {
// public void detect() throws NotFoundException {

    ResultPoint[] cornerPoints = rectangleDetector.detect();
    
    ResultPoint pointA = cornerPoints[0];
    ResultPoint pointB = cornerPoints[1];
    ResultPoint pointC = cornerPoints[2];
    ResultPoint pointD = cornerPoints[3];
   
    ResultPoint[] cornerPoints2 = rectangleDetector2.detect();
    
    ResultPoint pointA2 = cornerPoints2[0];
    ResultPoint pointB2 = cornerPoints2[1];
    ResultPoint pointC2 = cornerPoints2[2];
    ResultPoint pointD2 = cornerPoints2[3];
   
    //////////////////////////////////////////코너 좌표 볼려고 추가
  //  Log.d("rectangleDetector->", "pointA:"+cornerPoints[0]+"  pointB:"+cornerPoints[1]+"  pointC:"+cornerPoints[2]+"  pointD:"+cornerPoints[3]);
    //////////////////////////////////////////////////
    // Point A and D are across the diagonal from one another,
    // as are B and C. Figure out which are the solid black lines
    // by counting transitions
    List<ResultPointsAndTransitions> transitions = new ArrayList<ResultPointsAndTransitions>(4);
    transitions.add(transitionsBetween(pointA, pointB));
    transitions.add(transitionsBetween(pointA, pointC));
    transitions.add(transitionsBetween(pointB, pointD));
    transitions.add(transitionsBetween(pointC, pointD));
    Collections.sort(transitions, new ResultPointsAndTransitionsComparator());
    
    List<ResultPointsAndTransitions> transitions2 = new ArrayList<ResultPointsAndTransitions>(4);
    transitions2.add(transitionsBetween2(pointA2, pointB2));
    transitions2.add(transitionsBetween2(pointA2, pointC2));
    transitions2.add(transitionsBetween2(pointB2, pointD2));
    transitions2.add(transitionsBetween2(pointC2, pointD2));
    Collections.sort(transitions2, new ResultPointsAndTransitionsComparator());
    
    // Sort by number of transitions. First two will be the two solid sides; last two
    // will be the two alternating black/white sides
    ResultPointsAndTransitions lSideOne = transitions.get(0);
    ResultPointsAndTransitions lSideTwo = transitions.get(1);
  //  Log.d("ResultPointsAndTransitions1 끝", "ResultPointsAndTransitions 할당끝.");
    
    ResultPointsAndTransitions lSideOne2 = transitions2.get(0);
    ResultPointsAndTransitions lSideTwo2 = transitions2.get(1);
   // Log.d("ResultPointsAndTransitions2 끝", "ResultPointsAndTransitions 할당끝.");
    // Figure out which point is their intersection by tallying up the number of times we see the
    // endpoints in the four endpoints. One will show up twice.
    Map<ResultPoint,Integer> pointCount = new HashMap<ResultPoint,Integer>();
    increment(pointCount, lSideOne.getFrom());
    increment(pointCount, lSideOne.getTo());
    increment(pointCount, lSideTwo.getFrom());
    increment(pointCount, lSideTwo.getTo());
    
   // Log.d("ResultPoint1 끝", "ResultPoint1 할당끝.");
    Map<ResultPoint,Integer> pointCount2 = new HashMap<ResultPoint,Integer>();
    increment(pointCount2, lSideOne2.getFrom());
    increment(pointCount2, lSideOne2.getTo());
    increment(pointCount2, lSideTwo2.getFrom());
    increment(pointCount2, lSideTwo2.getTo());
    
   // Log.d("ResultPoint 끝", "ResultPoint 할당끝.");
    ///요가까지했음
    ResultPoint maybeTopLeft = null;
    ResultPoint bottomLeft = null;
    ResultPoint maybeBottomRight = null;
        
    ResultPoint maybeTopLeft2 = null;
    ResultPoint bottomLeft2 = null;
    ResultPoint maybeBottomRight2 = null;
    
    for (Map.Entry<ResultPoint,Integer> entry : pointCount.entrySet()) {
      ResultPoint point = entry.getKey();
      Integer value = entry.getValue();
      if (value == 2) {
        bottomLeft = point; // this is definitely the bottom left, then -- end of two L sides
      } else {
        // Otherwise it's either top left or bottom right -- just assign the two arbitrarily now
        if (maybeTopLeft == null) {
          maybeTopLeft = point;
        } else {
          maybeBottomRight = point;
        }
      }
    }
    
    
   // if (maybeTopLeft == null || bottomLeft == null || maybeBottomRight == null) {
    //  throw NotFoundException.getNotFoundInstance();
   // }
   // Log.d("maybeTopLeft 끝", "maybeTopLeft maybeTopLeft.");
    
    for (Map.Entry<ResultPoint,Integer> entry2 : pointCount2.entrySet()) {
        ResultPoint point2 = entry2.getKey();
        Integer value2 = entry2.getValue();
        if (value2 == 2) {
          bottomLeft2 = point2; // this is definitely the bottom left, then -- end of two L sides
        } else {
          // Otherwise it's either top left or bottom right -- just assign the two arbitrarily now
          if (maybeTopLeft2 == null) {
            maybeTopLeft2 = point2;
          } else {
            maybeBottomRight2 = point2;
          }
        }
      }
      
    //Log.d("maybeTopLeft2 끝", "maybeTopLeft2 maybeTopLeft2.");
    
      if (maybeTopLeft == null || bottomLeft == null || maybeBottomRight == null ||maybeTopLeft2 == null || bottomLeft2 == null || maybeBottomRight2 == null) { //하나라도 검출 안되면 예외로 튕겨나간다. 
    	  System.out.println("Detector2.java NotFoundException ");
        throw NotFoundException.getNotFoundInstance();      
      }
          
    // Bottom left is correct but top left and bottom right might be switched
    ResultPoint[] corners = { maybeTopLeft, bottomLeft, maybeBottomRight };
    ResultPoint[] corners2 = { maybeTopLeft2, bottomLeft2, maybeBottomRight2 };
    // Use the dot product trick to sort them out
    ResultPoint.orderBestPatterns(corners);
    ResultPoint.orderBestPatterns(corners2);

    // Now we know which is which:
    ResultPoint bottomRight = corners[0];
    bottomLeft = corners[1];
    ResultPoint topLeft = corners[2];

    ResultPoint bottomRight2 = corners2[0];
    bottomLeft2 = corners2[1];
    ResultPoint topLeft2 = corners2[2];

    // Which point didn't we find in relation to the "L" sides? that's the top right corner
    ResultPoint topRight;
    if (!pointCount.containsKey(pointA)) {
      topRight = pointA;
    } else if (!pointCount.containsKey(pointB)) {
      topRight = pointB;
    } else if (!pointCount.containsKey(pointC)) {
      topRight = pointC;
    } else {
      topRight = pointD;
    }

    
    ResultPoint topRight2;
    if (!pointCount2.containsKey(pointA2)) {
      topRight2 = pointA2;
    } else if (!pointCount2.containsKey(pointB2)) {
      topRight2 = pointB2;
    } else if (!pointCount2.containsKey(pointC2)) {
      topRight2 = pointC2;
    } else {
      topRight2 = pointD2;
    }

    // Next determine the dimension by tracing along the top or right side and counting black/white
    // transitions. Since we start inside a black module, we should see a number of transitions
    // equal to 1 less than the code dimension. Well, actually 2 less, because we are going to
    // end on a black module:

    // The top right point is actually the corner of a module, which is one of the two black modules
    // adjacent to the white module at the top right. Tracing to that corner from either the top left
    // or bottom right should work here.
    
    int dimensionTop = transitionsBetween(topLeft, topRight).getTransitions();
    int dimensionRight = transitionsBetween(bottomRight, topRight).getTransitions();
    
    int dimensionTop2 = transitionsBetween2(topLeft2, topRight2).getTransitions();
    int dimensionRight2 = transitionsBetween2(bottomRight2, topRight2).getTransitions();
    
    
    if ((dimensionTop & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionTop++;
    }
    dimensionTop += 2;
    
    if ((dimensionTop2 & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionTop2++;
      }
      dimensionTop2 += 2;
    
    if ((dimensionRight & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionRight++;
    }    
    dimensionRight += 2;
    
    if ((dimensionRight2 & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionRight2++;
      }      
      dimensionRight2 += 2;
      
    //String matarr="";
    BitMatrix bits;
    BitMatrix bits2;
    
  //  BitMatrix tempbits=new BitMatrix(41,41); //QR코드 추출해서 여기에 넣음 20120618추가
    BitMatrix tempbits; //QR코드 추출해서 여기에 넣음 20120618추가
    BitMatrix decbits; //QR코드 추출해서 여기에 넣음 20120618추가
    BitMatrix picture; //사진 흑백 변환 
    
    int dimensionCorrected=0; //20120618 아래에서 함수밖으로 꺼냈음
    
    ResultPoint correctedTopRight;
    
    int dimensionCorrected2=0; //20120618 아래에서 함수밖으로 꺼냈음
    
    ResultPoint correctedTopRight2;

    // Rectanguar symbols are 6x16, 6x28, 10x24, 10x32, 14x32, or 14x44. If one dimension is more
    // than twice the other, it's certainly rectangular, but to cut a bit more slack we accept it as
    // rectangular if the bigger side is at least 7/4 times the other:
    if (4 * dimensionTop >= 7 * dimensionRight || 4 * dimensionRight >= 7 * dimensionTop) {
    	// The matrix is rectangular
    	
      correctedTopRight =
          correctTopRightRectangular(bottomLeft, bottomRight, topLeft, topRight, dimensionTop, dimensionRight);
      if (correctedTopRight == null){
        correctedTopRight = topRight;
      }

      dimensionTop = transitionsBetween(topLeft, correctedTopRight).getTransitions();
      dimensionRight = transitionsBetween(bottomRight, correctedTopRight).getTransitions();

      if ((dimensionTop & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionTop++;
      }

      if ((dimensionRight & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionRight++;
      }

      bits = sampleGrid(image1, topLeft, bottomLeft, bottomRight, correctedTopRight, dimensionTop, dimensionRight);
    //  bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, correctedTopRight, 25, 25);     
    } else {
    	// The matrix is square
        
    	int dimension = Math.min(dimensionRight, dimensionTop);
      // correct top right point to match the white module
      correctedTopRight = correctTopRight(bottomLeft, bottomRight, topLeft, topRight, dimension);
      if (correctedTopRight == null){
        correctedTopRight = topRight;
      }

      // Redetermine the dimension using the corrected top right point
     // int dimensionCorrected = Math.max(transitionsBetween(topLeft, correctedTopRight).getTransitions(),
       dimensionCorrected = Math.max(transitionsBetween(topLeft, correctedTopRight).getTransitions(),
                                transitionsBetween(bottomRight, correctedTopRight).getTransitions());
      dimensionCorrected++;
      if ((dimensionCorrected & 0x01) == 1) {
        dimensionCorrected++;
      }
      //*
     bits = sampleGrid(image1,
                        topLeft,
                        bottomLeft,
                        bottomRight,
                        correctedTopRight,
                        dimensionCorrected,
                        dimensionCorrected);
       
    }
    ////////////////////////////////////////////////////////////////////////
    
    
    if (4 * dimensionTop2 >= 7 * dimensionRight2 || 4 * dimensionRight2 >= 7 * dimensionTop2) {
    	// The matrix is rectangular
    	
      correctedTopRight2 =
          correctTopRightRectangular2(bottomLeft2, bottomRight2, topLeft2, topRight2, dimensionTop2, dimensionRight2);
      if (correctedTopRight2 == null){
        correctedTopRight2 = topRight2;
      }

      dimensionTop2 = transitionsBetween2(topLeft2, correctedTopRight2).getTransitions();
      dimensionRight2 = transitionsBetween2(bottomRight2, correctedTopRight2).getTransitions();

      if ((dimensionTop2 & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionTop2++;
      }

      if ((dimensionRight2 & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionRight2++;
      }

      bits2 = sampleGrid(image2, topLeft2, bottomLeft2, bottomRight2, correctedTopRight2, dimensionTop2, dimensionRight2);
    //  bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, correctedTopRight, 25, 25);     
    } else {
    	// The matrix is square
        
    	int dimension2 = Math.min(dimensionRight2, dimensionTop2);
      // correct top right point to match the white module
      correctedTopRight2 = correctTopRight2(bottomLeft2, bottomRight2, topLeft2, topRight2, dimension2);
      if (correctedTopRight2 == null){
        correctedTopRight2 = topRight2;
      }

      // Redetermine the dimension using the corrected top right point
     // int dimensionCorrected = Math.max(transitionsBetween(topLeft, correctedTopRight).getTransitions(),
       dimensionCorrected2 = Math.max(transitionsBetween2(topLeft2, correctedTopRight2).getTransitions(),
                                transitionsBetween2(bottomRight2, correctedTopRight2).getTransitions());
      dimensionCorrected2++;
      if ((dimensionCorrected2 & 0x01) == 1) {
        dimensionCorrected2++;
      }
      //*
     bits2 = sampleGrid(image2,
                        topLeft2,
                        bottomLeft2,
                        bottomRight2,
                        correctedTopRight2,
                        dimensionCorrected2,
                        dimensionCorrected2);
       
    }
    
    
    
    //////////////////////////////////////////코너 좌표 볼려고 추가
   // Log.d("rectangleDetector->", "topLeft:"+topLeft+"  bottomLeft:"+bottomLeft+"  bottomRight:"+bottomRight+"  correctedTopRight:"+correctedTopRight+"  dimensionCorrected:"+dimensionCorrected);
    //////////////////////////////////////////////////
   
   int size;
   size=bits.getWidth(); 
   
   int size2;
   size2=bits2.getWidth(); 
   boolean Detect_OK=false;
   System.out.println("Detector2.java BX사이즈>"+ Integer.toString(size)+"-"+Integer.toString(size2));
   
  //  tempbits=new BitMatrix(dimensionCorrected-3,dimensionCorrected-3);
  //  decbits=new BitMatrix(dimensionCorrected-3,dimensionCorrected-3);
    tempbits=new BitMatrix(size-3,size-3);
    decbits=new BitMatrix(size-3,size-3);
    picture=new BitMatrix(size-3,size-3);
///여기에서bits 를 조작하면 된다. //////////////////////////////////////////
    if(size==52&&size2==52){
    	Detect_OK=true;
					    for(int d=2;d<size-1;d++){ //y    datamatrix에서 뽑아서 QR만 재 생성 한다. 
					    		for(int dd=1;dd<size-2;dd++){ //x
													    	if(bits.get(dd,d)){
													    		decbits.set(dd-1,d-2);//변조 일때 
													    		//tempbits.set(dd-1,d-2); //QR일때
													    		//	matarr=matarr+"1";
													    	}	
													    	else { 
													    //		matarr=matarr+"0";
													    	}    		
					    		}
					    	
					    }
					    
					    
					    for(int d=2;d<size-1;d++){ //y    datamatrix에서 뽑아서 QR만 재 생성 한다. 
							for(int dd=1;dd<size-2;dd++){ //x
												    	if(bits2.get(dd,d)){
												    		picture.set(dd-1,d-2);//변조 일때 
												    		//tempbits.set(dd-1,d-2); //QR일때
												    		//	matarr=matarr+"1";
												    	}	
												    	else { 
												    //		matarr=matarr+"0";
												    	}    		
							}
						
					}
    	   
					//*   
					int[] decindex1=new int[]{38,34,41,30,25,15,19,10,7,2,22,28,36,45,17,12,47,0,43,48,3,21,46,37,44,1,42,24,16,13,20,4,40,18,5,11,23,32,35,29,33,6,9,14,26,31,39,27,8};		
					if(dimensionCorrected>51)  { 
				    for(int x=0;x<size-3;x++)for(int y=0;y<size-3;y++){ //Qr y축
				    		 //  code[x][y]=enccode[x][decindex1[y]];		
				    		   if(decbits.get(x,decindex1[y])){
				    	    		tempbits.set(x,y); 
				    	    	//	matarr=matarr+"1";
				    	    	}	
				    	   } //바코드 보고 싶을때 
				   
				    decbits.clear();
				  
				    for(int x=0;x<size-3;x++)for(int y=0;y<size-3;y++){ //Qr y축 
				    		 //  code[x][y]=enccode[x][decindex1[y]];		
				    		   if(tempbits.get(decindex1[x],y)){
				    			   decbits.set(x,y); 
				    	    	//	matarr=matarr+"1";
				    	    	}	
				    	   } //바코드 보고 싶을때 
					}
				    	//*/
 	
    }
	
	//if(bits.getWidth()==52&&bits.getHeight()==52){	
	//if(size==52&&size2==52){
	//	Detect_OK=true;
	//	} 
	//else { 			
	//	Detect_OK=false;		
	//	 }  
	
	//Log.i("Detector2->", Integer.toString(size));
	//return null; //new detectorResult2.DetectorResult2_setBM(tempbits, decbits ); 
	return new DetectorResult2(decbits, picture, Detect_OK ); //QR용 
  }

  /**
   * Calculates the position of the white top right module using the output of the rectangle detector
   * for a rectangular matrix
   */
  private ResultPoint correctTopRightRectangular(ResultPoint bottomLeft,
                                                 ResultPoint bottomRight,
                                                 ResultPoint topLeft,
                                                 ResultPoint topRight,
                                                 int dimensionTop,
                                                 int dimensionRight) {
	  
		float corr = distance(bottomLeft, bottomRight) / (float)dimensionTop;
		int norm = distance(topLeft, topRight);
		float cos = (topRight.getX() - topLeft.getX()) / norm;
		float sin = (topRight.getY() - topLeft.getY()) / norm;
		
		ResultPoint c1 = new ResultPoint(topRight.getX()+corr*cos, topRight.getY()+corr*sin);
	
		corr = distance(bottomLeft, topLeft) / (float)dimensionRight;
		norm = distance(bottomRight, topRight);
		cos = (topRight.getX() - bottomRight.getX()) / norm;
		sin = (topRight.getY() - bottomRight.getY()) / norm;
		
		ResultPoint c2 = new ResultPoint(topRight.getX()+corr*cos, topRight.getY()+corr*sin);

    if (!isValid(c1)) {
      if (isValid(c2)) {
        return c2;
      }
      return null;
    }
    if (!isValid(c2)){
      return c1;
    }

    int l1 = Math.abs(dimensionTop - transitionsBetween(topLeft, c1).getTransitions()) +
					Math.abs(dimensionRight - transitionsBetween(bottomRight, c1).getTransitions());
		int l2 = Math.abs(dimensionTop - transitionsBetween(topLeft, c2).getTransitions()) + 
		Math.abs(dimensionRight - transitionsBetween(bottomRight, c2).getTransitions());
		
		if (l1 <= l2){
			return c1;
		}
		
		return c2;
  }

  
  private ResultPoint correctTopRightRectangular2(ResultPoint bottomLeft2,
          ResultPoint bottomRight2,
          ResultPoint topLeft2,
          ResultPoint topRight2,
          int dimensionTop2,
          int dimensionRight2) {

float corr = distance(bottomLeft2, bottomRight2) / (float)dimensionTop2;
int norm = distance(topLeft2, topRight2);
float cos = (topRight2.getX() - topLeft2.getX()) / norm;
float sin = (topRight2.getY() - topLeft2.getY()) / norm;

ResultPoint c1 = new ResultPoint(topRight2.getX()+corr*cos, topRight2.getY()+corr*sin);

corr = distance(bottomLeft2, topLeft2) / (float)dimensionRight2;
norm = distance(bottomRight2, topRight2);
cos = (topRight2.getX() - bottomRight2.getX()) / norm;
sin = (topRight2.getY() - bottomRight2.getY()) / norm;

ResultPoint c2 = new ResultPoint(topRight2.getX()+corr*cos, topRight2.getY()+corr*sin);

if (!isValid2(c1)) {
if (isValid2(c2)) {
return c2;
}
return null;
}
if (!isValid2(c2)){
return c1;
}

int l1 = Math.abs(dimensionTop2 - transitionsBetween2(topLeft2, c1).getTransitions()) +
Math.abs(dimensionRight2 - transitionsBetween2(bottomRight2, c1).getTransitions());
int l2 = Math.abs(dimensionTop2 - transitionsBetween2(topLeft2, c2).getTransitions()) + 
Math.abs(dimensionRight2 - transitionsBetween2(bottomRight2, c2).getTransitions());

if (l1 <= l2){
return c1;
}

return c2;
}
  /**
   * Calculates the position of the white top right module using the output of the rectangle detector
   * for a square matrix
   */
  private ResultPoint correctTopRight(ResultPoint bottomLeft,
                                      ResultPoint bottomRight,
                                      ResultPoint topLeft,
                                      ResultPoint topRight,
                                      int dimension) {
		
		float corr = distance(bottomLeft, bottomRight) / (float) dimension;
		int norm = distance(topLeft, topRight);
		float cos = (topRight.getX() - topLeft.getX()) / norm;
		float sin = (topRight.getY() - topLeft.getY()) / norm;
		
		ResultPoint c1 = new ResultPoint(topRight.getX() + corr * cos, topRight.getY() + corr * sin);
	
		corr = distance(bottomLeft, bottomRight) / (float) dimension;
		norm = distance(bottomRight, topRight);
		cos = (topRight.getX() - bottomRight.getX()) / norm;
		sin = (topRight.getY() - bottomRight.getY()) / norm;
		
		ResultPoint c2 = new ResultPoint(topRight.getX() + corr * cos, topRight.getY() + corr * sin);

    if (!isValid(c1)) {
      if (isValid(c2)) {
        return c2;
      }
      return null;
    }
    if (!isValid(c2)) {
      return c1;
    }

    int l1 = Math.abs(transitionsBetween(topLeft, c1).getTransitions() -
                      transitionsBetween(bottomRight, c1).getTransitions());
		int l2 = Math.abs(transitionsBetween(topLeft, c2).getTransitions() -
                      transitionsBetween(bottomRight, c2).getTransitions());

    return l1 <= l2 ? c1 : c2;
  }

  private boolean isValid(ResultPoint p) {
	  return p.getX() >= 0 && p.getX() < image1.getWidth() && p.getY() > 0 && p.getY() < image1.getHeight();
  }

  
 private ResultPoint correctTopRight2(ResultPoint bottomLeft2,
          ResultPoint bottomRight2,
          ResultPoint topLeft2,
          ResultPoint topRight2,
          int dimension2) {

float corr = distance(bottomLeft2, bottomRight2) / (float) dimension2;
int norm = distance(topLeft2, topRight2);
float cos = (topRight2.getX() - topLeft2.getX()) / norm;
float sin = (topRight2.getY() - topLeft2.getY()) / norm;

ResultPoint c1 = new ResultPoint(topRight2.getX() + corr * cos, topRight2.getY() + corr * sin);

corr = distance(bottomLeft2, bottomRight2) / (float) dimension2;
norm = distance(bottomRight2, topRight2);
cos = (topRight2.getX() - bottomRight2.getX()) / norm;
sin = (topRight2.getY() - bottomRight2.getY()) / norm;

ResultPoint c2 = new ResultPoint(topRight2.getX() + corr * cos, topRight2.getY() + corr * sin);

if (!isValid2(c1)) {
if (isValid2(c2)) {
return c2;
}
return null;
}
if (!isValid2(c2)) {
return c1;
}

int l1 = Math.abs(transitionsBetween2(topLeft2, c1).getTransitions() -
transitionsBetween2(bottomRight2, c1).getTransitions());
int l2 = Math.abs(transitionsBetween2(topLeft2, c2).getTransitions() -
transitionsBetween2(bottomRight2, c2).getTransitions());

return l1 <= l2 ? c1 : c2;
}

private boolean isValid2(ResultPoint p) {
return p.getX() >= 0 && p.getX() < image2.getWidth() && p.getY() > 0 && p.getY() < image2.getHeight();
}
  /**
   * Ends up being a bit faster than Math.round(). This merely rounds its
   * argument to the nearest int, where x.5 rounds up.
   */
  private static int round(float d) {
    return (int) (d + 0.5f);
  }

// L2 distance
  private static int distance(ResultPoint a, ResultPoint b) {
    return round((float) Math.sqrt((a.getX() - b.getX())
        * (a.getX() - b.getX()) + (a.getY() - b.getY())
        * (a.getY() - b.getY())));
  }

  /**
   * Increments the Integer associated with a key by one.
   */
  private static void increment(Map<ResultPoint,Integer> table, ResultPoint key) {
    Integer value = table.get(key);
    table.put(key, value == null ? 1 : value + 1);
  }

  private static BitMatrix sampleGrid(BitMatrix image,
                                      ResultPoint topLeft,
                                      ResultPoint bottomLeft,
                                      ResultPoint bottomRight,
                                      ResultPoint topRight,
                                      int dimensionX,
                                      int dimensionY) throws NotFoundException {

    GridSampler sampler = GridSampler.getInstance();

    return sampler.sampleGrid(image,
                              dimensionX,
                              dimensionY,
                              0.5f,
                              0.5f,
                              dimensionX - 0.5f,
                              0.5f,
                              dimensionX - 0.5f,
                              dimensionY - 0.5f,
                              0.5f,
                              dimensionY - 0.5f,
                              topLeft.getX(),
                              topLeft.getY(),
                              topRight.getX(),
                              topRight.getY(),
                              bottomRight.getX(),
                              bottomRight.getY(),
                              bottomLeft.getX(),
                              bottomLeft.getY());
  }

  /**
   * Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.
   */
  private ResultPointsAndTransitions transitionsBetween(ResultPoint from, ResultPoint to) {
    // See QR Code Detector, sizeOfBlackWhiteBlackRun()
    int fromX = (int) from.getX();
    int fromY = (int) from.getY();
    int toX = (int) to.getX();
    int toY = (int) to.getY();
    boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
    if (steep) {
      int temp = fromX;
      fromX = fromY;
      fromY = temp;
      temp = toX;
      toX = toY;
      toY = temp;
    }

    int dx = Math.abs(toX - fromX);
    int dy = Math.abs(toY - fromY);
    int error = -dx >> 1;
    int ystep = fromY < toY ? 1 : -1;
    int xstep = fromX < toX ? 1 : -1;
    int transitions = 0;
    boolean inBlack = image1.get(steep ? fromY : fromX, steep ? fromX : fromY);
    for (int x = fromX, y = fromY; x != toX; x += xstep) {
      boolean isBlack = image1.get(steep ? y : x, steep ? x : y);
      if (isBlack != inBlack) {
        transitions++;
        inBlack = isBlack;
      }
      error += dy;
      if (error > 0) {
        if (y == toY) {
          break;
        }
        y += ystep;
        error -= dx;
      }
    }
    return new ResultPointsAndTransitions(from, to, transitions);
  }

  
  private ResultPointsAndTransitions transitionsBetween2(ResultPoint from, ResultPoint to) {
	    // See QR Code Detector, sizeOfBlackWhiteBlackRun()
	    int fromX = (int) from.getX();
	    int fromY = (int) from.getY();
	    int toX = (int) to.getX();
	    int toY = (int) to.getY();
	    boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
	    if (steep) {
	      int temp = fromX;
	      fromX = fromY;
	      fromY = temp;
	      temp = toX;
	      toX = toY;
	      toY = temp;
	    }

	    int dx = Math.abs(toX - fromX);
	    int dy = Math.abs(toY - fromY);
	    int error = -dx >> 1;
	    int ystep = fromY < toY ? 1 : -1;
	    int xstep = fromX < toX ? 1 : -1;
	    int transitions = 0;
	    boolean inBlack = image2.get(steep ? fromY : fromX, steep ? fromX : fromY);
	    for (int x = fromX, y = fromY; x != toX; x += xstep) {
	      boolean isBlack = image2.get(steep ? y : x, steep ? x : y);
	      if (isBlack != inBlack) {
	        transitions++;
	        inBlack = isBlack;
	      }
	      error += dy;
	      if (error > 0) {
	        if (y == toY) {
	          break;
	        }
	        y += ystep;
	        error -= dx;
	      }
	    }
	    return new ResultPointsAndTransitions(from, to, transitions);
	  }
  /**
   * Simply encapsulates two points and a number of transitions between them.
   */
  private static class ResultPointsAndTransitions {

    private final ResultPoint from;
    private final ResultPoint to;
    private final int transitions;

    private ResultPointsAndTransitions(ResultPoint from, ResultPoint to, int transitions) {
      this.from = from;
      this.to = to;
      this.transitions = transitions;
    }

    ResultPoint getFrom() {
      return from;
    }

    ResultPoint getTo() {
      return to;
    }

    public int getTransitions() {
      return transitions;
    }
    
    @Override
    public String toString() {
      return from + "/" + to + '/' + transitions;
    }
  }

  /**
   * Orders ResultPointsAndTransitions by number of transitions, ascending.
   */
  private static class ResultPointsAndTransitionsComparator
      implements Comparator<ResultPointsAndTransitions>, Serializable {

    public int compare(ResultPointsAndTransitions o1, ResultPointsAndTransitions o2) {
      return o1.getTransitions() - o2.getTransitions();
    }
  }

}
