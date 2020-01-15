/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing;



// 패턴 포인트를 저장후 전송 

public class ResultPointABC {

  private static float pointAx;
  private static float pointBx;
  private static float pointCx;
  private static float pointDx;
  private static float pointAy;
  private static float pointBy;
  private static float pointCy;
  private static float pointDy;
  private static int fake=0;
  private static float count=0;
  private static int setCodedetect=0;
  public ResultPointABC() {    
    
  }
  
  public void setABC(float ax, float ay,float bx, float by,float cx,  float cy,float dx,  float dy) {
	    this.pointAx = ax;
	    this.pointBx = bx;
	    this.pointCx = cx;
	    this.pointDx = dx;
	    this.pointAy = ay;
	    this.pointBy = by;
	    this.pointCy = cy;
	    this.pointDy = dy;
	    
	  }

  public final float getAx() {
    return pointAx;
  }

  public final float getBx() {
    return pointBx;
  }
  public final float getCx() {
	    return pointCx ;
  }
  
  public final float getDx() {
	    return pointDx ;
 }
  public final float getAy() {
	    return pointAy;
	  }

  public final float getBy() {
	    return pointBy;
	  }
  public final float getCy() {
		    return pointCy ;
		  }
  
  public final float getDy() {
	    return pointDy ;
	  }

  public void settrue() {
	  this.fake=1;	  
	  }
  public void setfalse() {
	  this.fake=0;	  
  }
  
  public int gettrue() {
	  return this.fake;	  
  }
  
  public int getsetCodedetect() {
	  return this.setCodedetect;	  
  }
  
  public void setCodedetect () {
	  this.setCodedetect=1;
	 
	  } 
  
  public void clrCodedetect () {
	  this.setCodedetect=0;	  
	  } 
  
  
  
  public void clrcount() {
	  this.count=0;
	 
	  } 
  
  public int getcount() {
	  return (int)this.count; //int로 넘겨줌 
	 
	  }
  
  public void inccount(float ms) {
	  this.count=this.count+ms;
	 
	  }
  
  public int getPointX(int destX) {
	  
	  int x=0;
	  
	  float a,b,c,d,e,f,m;
	  //포인트Ax :393.0포인트Ay :356.0포인트Bx :104.5포인트By :346.5포인트Cx :105.0포인트Cy :58.0 세로
      //  포인트Ax :94.5포인트Ay :356.0포인트Bx :98.0포인트By :98.0포인트Cx :356.0포인트Cy :101.0  가로
	  //  인식점은 역 기역 모양 인데,. 카메라를 세로로 찍었기 때문에  아래와 같이 된다.  
	  //
	  // 인식점과 인식점 사이는 인식점 포함 35개 바둑판 모양. 인식점 두개 빼면 총 33개 영역 교차 점이 생김.
	  // 인식점과 인식점 사이의 차는 34로 나누면 나옴. 34로 나누어야 함.
	  //
	  //
	  //         ___________________________________
	  //         |                                 |
	  //         | *                             * |
	  //         | B                             C |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         | *                               |
      //  	     | A                               |
	  //         -----------------------------------
	   
	  //  * 절대 주의 ,. 카메라 입력시 좌우 좌표가 바뀌어 있다. 
	  //X좌표는 C-B        Y 좌표는 A-B
	  // 정상적인 경우 
		  a=pointCx-pointBx; //둘사이 거리 계산  , 기울기에 따른 거리도 계산 
		  b=a/34; //한간의 길이 계산 ,  길이는 소숫점까지 계산해야 한다. 픽셀이기 때문에 한칸이 한픽셀이 아니다. 전체 길이안에 포함된 한칸당 픽셀 갯수를 구해야 한다.
		         // 한칸당 픽셀 갯수는 (C-B)/34
		  c=destX*b; //Bx로 부터 destx까지의 거리를 계산. float 값이다. * 거리는 입력받은 좌표가 아니라 한칸당 픽셀값의 갯수의 곱이다. 한칸당 픽셀수= 둘사이 거리/34 * 입력받은 좌표  
		  
		//  if(pointBx<pointAx){  //B가 C보다 높이가 높으면 , 높든지 낮든지  편차가 있는 Cx' 값은 항상 Cx 값보다 작다. 편차를 거리에 따른 비율로 빼주면 된다. 
			  e=pointAx-pointBx;
		//  }else {e=pointBx-pointAx;}
		  
		 
		  f=(c/a)*e; //거리에 따른 편차 원점에서 멀수록 편차가 거짐.  
			 
		
		  
		  d=pointBx+(c-f); //X좌표는 원전 0,0부터의 거리 이므로 편차에 따른 Bx값을 더해줘야 한다.  
		  
		  x=(int)d;
	     return x;
	  }
  
public int getPointY(int destY) {
	  
	  int y=0;
	  
	  float a,b,c,d,e,f,m;
	  //포인트Ax :393.0포인트Ay :356.0포인트Bx :104.5포인트By :346.5포인트Cx :105.0포인트Cy :58.0 세로
      //  포인트Ax :94.5포인트Ay :356.0포인트Bx :98.0포인트By :98.0포인트Cx :356.0포인트Cy :101.0  가로
	  //  인식점은 역 기역 모양 인데,. 카메라를 세로로 찍었기 때문에  아래와 같이 된다.  
	  //
	  // 인식점과 인식점 사이는 인식점 포함 35개 바둑판 모양. 인식점 두개 빼면 총 33개 영역 교차 점이 생김.
	  // 인식점과 인식점 사이의 차는 34로 나누면 나옴. 34로 나누어야 함.
	  //
	  //
	  //         ___________________________________
	  //         |                                 |
	  //         | *                             * |
	  //         | B                             C |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         |                                 |
	  //         | *                               |
      //  	     | A                               |
	  //         -----------------------------------
	  
	  //X좌표는 C-B        Y 좌표는 A-B
	     
	  a=pointAy-pointBy; //둘사이 거리 계산 
	  b=a/34; //한간의 길이 계산 
	  c=destY*b; //destx 좌표 구함. float 값이다.
	  
	 // if(pointBy<pointCy){  //B가 C보다 높이가 높으면 , 높든지 낮든지  편차가 있는 Cx' 값은 항상 C가 B보다 높으면 더해주고, 낮으면 빼준다. 편차를 거리에 따른 비율로 더하거나 빼주면 된다. 
	  e=pointCy-pointBy;
	 // }else {e=pointBy-pointCy;}
	  
	  f=(c/a)*e; //거리에 따른 편차 원점에서 멀수록 편차가 거짐.  
		   
	  d=pointBy+(c-f); //Y좌표는 원전 0,0부터의 거리 이므로 편차에 따른 BY값을 더해줘야 한다.  
	  
	  y=(int)d;
     
	  return y;
	  }

}
