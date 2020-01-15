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

package com.google.zxing.common;

import com.google.zxing.ResultPoint;

/**
 * <p>Encapsulates the result of detecting a barcode in an image. This includes the raw
 * matrix of black/white pixels corresponding to the barcode, and possibly points of interest
 * in the image, like the location of finder patterns or corners of the barcode in the image.</p>
 *
 * @author Sean Owen
 */
public class DetectorResult2 {

	public static BitMatrix bits1;
	public static BitMatrix bits2;
  
  public static boolean Detect_OK=false;
  
  public DetectorResult2(BitMatrix bits11,BitMatrix bits22,boolean detect) {
	   bits1 = bits11;
	   bits2 = bits22;
	   Detect_OK=detect;
  }
  
  public BitMatrix getBits1() {
    return bits1;
  }

  public BitMatrix getBits2() {
	    return bits2;
	  }
  public void setDetect() {
	  Detect_OK=true;
	  }
  public void clearDetect() {
	  Detect_OK=false;
	  }
  
  public boolean getDetect() {
	 return Detect_OK;
	  }
}