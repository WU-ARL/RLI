/*
 * Copyright (c) 2005-2013 Jyoti Parwatikar
 * and Washington University in St. Louis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

/*
 * File: BoxedRangeModel.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/23/2003
 *
 * Description:
 *
 * Modification History:
 *
 */
public interface BoxedRangeModel
{
  public double getXMax();
  public double getXMin();
  public double getYMax();
  public double getYMin();
  public double getXValue();
  public double getXExtent();
  public double getYValue();
  public double getYExtent();

  //sets outer boundary
  public void setXMin(double mn);
  public void setXMax(double mx);
  public void setYMin(double mn);
  public void setYMax(double mx);
  public void setXBoundary(double mx, double mn);
  public void setYBoundary(double mx, double mn);
  public void setBoundary(double x_mx, double x_mn, double y_mx, double y_mn);

  //sets inner visible boundary
  public void setXVisible(double val, double xtnt);
  public void setYVisible(double val, double xtnt);
  public void setXValue(double val);
  public void setXExtent(double val);
  public void setYValue(double val);
  public void setYExtent(double val);
  public void setVisible(double x_val, double x_xtnt, double y_val, double y_xtnt);

  //support for listeners
  public void addBoxedRangeListener(BoxedRangeListener l);
  public void removeBoxedRangeListener(BoxedRangeListener l);

  //print limits
  public void print(int d);
}
