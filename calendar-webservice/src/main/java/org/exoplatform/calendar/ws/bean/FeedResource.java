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

import static org.exoplatform.calendar.ws.CalendarRestApi.CAL_BASE_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.FEED_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.RSS_URI;

import java.io.Serializable;
import java.util.Collection;

import org.exoplatform.calendar.service.FeedData;

public class FeedResource<T extends Serializable> extends Resource {
  private static final long serialVersionUID = 7911451293360539750L;

  private String            name;

  private String                    rss;

  private T[]                  calendars;

  public FeedResource(FeedData data, Collection<String> calendars) {
    setId(data.getFeed());
    setHref(new StringBuffer(CAL_BASE_URI).append(FEED_URI).append(data.getTitle()).toString());
    name = data.getTitle();
    rss = new StringBuffer(CAL_BASE_URI).append(FEED_URI)
                                        .append(data.getTitle())
                                        .append(RSS_URI)
                                        .toString();
    if (calendars != null)
      this.calendars = (T[]) calendars.toArray(new String[] {});
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getRss() {
    return this.rss;
  }
  
  public T[] getCalendars() {
    return this.calendars;
  }
  
  public FeedResource<T> setCalendars(T[] calendars) {
    this.calendars = calendars;
    return this;
  }
}
