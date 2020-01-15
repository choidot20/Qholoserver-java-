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



// ���� ����Ʈ�� ������ ���� 

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
	  return (int)this.count; //int�� �Ѱ��� 
	 
	  }
  
  public void inccount(float ms) {
	  this.count=this.count+ms;
	 
	  }
  
  public int getPointX(int destX) {
	  
	  int x=0;
	  
	  float a,b,c,d,e,f,m;
	  //����ƮAx :393.0����ƮAy :356.0����ƮBx :104.5����ƮBy :346.5����ƮCx :105.0����ƮCy :58.0 ����
      //  ����ƮAx :94.5����ƮAy :356.0����ƮBx :98.0����ƮBy :98.0����ƮCx :356.0����ƮCy :101.0  ����
	  //  �ν����� �� �⿪ ��� �ε�,. ī�޶� ���η� ����� ������  �Ʒ��� ���� �ȴ�.  
	  //
	  // �ν����� �ν��� ���̴� �ν��� ���� 35�� �ٵ��� ���. �ν��� �ΰ� ���� �� 33�� ���� ���� ���� ����.
	  // �ν����� �ν��� ������ ���� 34�� ������ ����. 34�� ������� ��.
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
	   
	  //  * ���� ���� ,. ī�޶� �Է½� �¿� ��ǥ�� �ٲ�� �ִ�. 
	  //X��ǥ�� C-B        Y ��ǥ�� A-B
	  // �������� ��� 
		  a=pointCx-pointBx; //�ѻ��� �Ÿ� ���  , ���⿡ ���� �Ÿ��� ��� 
		  b=a/34; //�Ѱ��� ���� ��� ,  ���̴� �Ҽ������� ����ؾ� �Ѵ�. �ȼ��̱� ������ ��ĭ�� ���ȼ��� �ƴϴ�. ��ü ���̾ȿ� ���Ե� ��ĭ�� �ȼ� ������ ���ؾ� �Ѵ�.
		         // ��ĭ�� �ȼ� ������ (C-B)/34
		  c=destX*b; //Bx�� ���� destx������ �Ÿ��� ���. float ���̴�. * �Ÿ��� �Է¹��� ��ǥ�� �ƴ϶� ��ĭ�� �ȼ����� ������ ���̴�. ��ĭ�� �ȼ���= �ѻ��� �Ÿ�/34 * �Է¹��� ��ǥ  
		  
		//  if(pointBx<pointAx){  //B�� C���� ���̰� ������ , ������ ������  ������ �ִ� Cx' ���� �׻� Cx ������ �۴�. ������ �Ÿ��� ���� ������ ���ָ� �ȴ�. 
			  e=pointAx-pointBx;
		//  }else {e=pointBx-pointAx;}
		  
		 
		  f=(c/a)*e; //�Ÿ��� ���� ���� �������� �ּ��� ������ ����.  
			 
		
		  
		  d=pointBx+(c-f); //X��ǥ�� ���� 0,0������ �Ÿ� �̹Ƿ� ������ ���� Bx���� ������� �Ѵ�.  
		  
		  x=(int)d;
	     return x;
	  }
  
public int getPointY(int destY) {
	  
	  int y=0;
	  
	  float a,b,c,d,e,f,m;
	  //����ƮAx :393.0����ƮAy :356.0����ƮBx :104.5����ƮBy :346.5����ƮCx :105.0����ƮCy :58.0 ����
      //  ����ƮAx :94.5����ƮAy :356.0����ƮBx :98.0����ƮBy :98.0����ƮCx :356.0����ƮCy :101.0  ����
	  //  �ν����� �� �⿪ ��� �ε�,. ī�޶� ���η� ����� ������  �Ʒ��� ���� �ȴ�.  
	  //
	  // �ν����� �ν��� ���̴� �ν��� ���� 35�� �ٵ��� ���. �ν��� �ΰ� ���� �� 33�� ���� ���� ���� ����.
	  // �ν����� �ν��� ������ ���� 34�� ������ ����. 34�� ������� ��.
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
	  
	  //X��ǥ�� C-B        Y ��ǥ�� A-B
	     
	  a=pointAy-pointBy; //�ѻ��� �Ÿ� ��� 
	  b=a/34; //�Ѱ��� ���� ��� 
	  c=destY*b; //destx ��ǥ ����. float ���̴�.
	  
	 // if(pointBy<pointCy){  //B�� C���� ���̰� ������ , ������ ������  ������ �ִ� Cx' ���� �׻� C�� B���� ������ �����ְ�, ������ ���ش�. ������ �Ÿ��� ���� ������ ���ϰų� ���ָ� �ȴ�. 
	  e=pointCy-pointBy;
	 // }else {e=pointBy-pointCy;}
	  
	  f=(c/a)*e; //�Ÿ��� ���� ���� �������� �ּ��� ������ ����.  
		   
	  d=pointBy+(c-f); //Y��ǥ�� ���� 0,0������ �Ÿ� �̹Ƿ� ������ ���� BY���� ������� �Ѵ�.  
	  
	  y=(int)d;
     
	  return y;
	  }

}
