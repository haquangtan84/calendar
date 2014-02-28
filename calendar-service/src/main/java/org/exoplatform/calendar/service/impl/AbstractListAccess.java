/*
 * Copyright (C) 2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.calendar.service.impl;

import org.exoplatform.calendar.service.CalendarException;
import org.exoplatform.calendar.service.MultiListAccess.LinkableListAccess;
import org.exoplatform.calendar.service.Utils;

public abstract class AbstractListAccess<T> implements LinkableListAccess<T> {

  private T[] fullData;

  private boolean      returnSize;

  public AbstractListAccess(boolean returnSize) {
    this.returnSize = returnSize;
  }

  @Override
  public T[] load(int index, int length) throws CalendarException {
    if (returnSize) {
      return Utils.subArray(getFullData(), index, length);
    } else {
      return loadData(index, length);
    }
  }

  @Override
  public int getSize() throws CalendarException {
    if (returnSize) {
      return getFullData().length;
    } else {
      return -1;
    }
  }

  private T[] getFullData() {
    if (fullData == null) {
      fullData = loadData(0, -1);
    }
    return fullData;
  }

  protected abstract T[] loadData(int offset, int limit);
  
  @Override
  public void setReturnSize() {
    this.returnSize = true;
  }
}
