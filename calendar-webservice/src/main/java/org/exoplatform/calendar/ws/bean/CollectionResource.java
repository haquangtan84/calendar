/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.calendar.ws.bean;

import java.util.Collection;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 6, 2014  
 */
public class CollectionResource<T> extends Resource {
  /**
   * 
   */
  private static final long serialVersionUID = 8125671009576049265L;
  private int offset = 0;
  private int limit = 0;
  private long size = 0;
  private long fullSize = 0 ;
  private boolean returnSize = false;
  private Collection<T> data ;

  public CollectionResource(Collection<T> data) {
    this.data = data;
    this.size = data.size();
    this.fullSize = data.size();
  }

  public CollectionResource(Collection<T> data, long fullSize){
    this.data = data ;
    this.size = data.size();
    this.fullSize = fullSize;
  }

  
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void setData(Collection<T> data) {
    this.data = data;
  }

  public boolean isReturnSize() {
    return returnSize;
  }

  public void setReturnSize(boolean returnSize) {
    this.returnSize = returnSize;
  }

  public Collection<T> getData() {
    return data;
  }

  public long getFullSize() {
    return fullSize;
  }

  public void setFullSize(long fullSize) {
    this.fullSize = fullSize;
  }
}
