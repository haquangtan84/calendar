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

import static org.exoplatform.calendar.ws.CalendarRestApi.CALENDAR_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.CAL_BASE_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.CATEGORY_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.TASK_URI;

import java.io.Serializable;
import java.util.Date;

import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.Reminder;
import org.exoplatform.calendar.service.Utils;
import org.json.JSONException;
import org.json.JSONObject;

public class TaskResource<T extends Serializable> extends Resource {
  private static final long serialVersionUID = -5290204215375549320L;

  private String name;
  private String note;
  private Date from;
  private Date to;
  private T calendar;
  private String[] categories;
  private String[] delegation;
  private int priority;
  private Reminder[] reminder;
  private Attachment[] attachments;
  private String status;  
  
  public TaskResource(CalendarEvent data) {
   setId(data.getId());
   setHref(new StringBuffer(CAL_BASE_URI).append(TASK_URI).append(data.getId()).toString());
   name = data.getSummary();
   note = data.getDescription();
   from = data.getFromDateTime();
   to = data.getToDateTime();
   calendar = (T) new StringBuffer(CAL_BASE_URI).append(CALENDAR_URI).append(data.getCalendarId()).toString();
   categories = new String[]{new StringBuffer(CAL_BASE_URI).append(CATEGORY_URI).append(data.getEventCategoryId()).toString()};
   if(data.getTaskDelegator() != null)delegation = data.getTaskDelegator().split(Utils.COLON);
   int priority = 0;
   for (int i = 0; i < CalendarEvent.PRIORITY.length; i++) {
     if (data.getPriority().equalsIgnoreCase(CalendarEvent.PRIORITY[i])) {
       if (i == CalendarEvent.PRI_MEDIUM) {
         priority = 5;
       } else if (i == CalendarEvent.PRI_LOW) {
         priority = 6;
       } else {
         priority = i;
       }
     }
   }
   this.priority = priority; 
   if(data.getReminders() != null) reminder = data.getReminders().toArray(new Reminder[]{});
   if(data.getAttachment() != null) attachments = data.getAttachment().toArray(new Attachment[]{});
   status = data.getStatus();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public T getCalendar() {
    return calendar;
  }

  public TaskResource<T> setCalendar(T calendar) {
    this.calendar = calendar;
    return this;
  }
  
  public String getNote() {
    return note;
  }

  public Date getFrom() {
    return from;
  }

  public Date getTo() {
    return to;
  }

  public String[] getCategories() {
    return categories;
  }

  public String[] getDelegation() {
    return delegation;
  }

  public int getPriority() {
    return priority;
  }

  public Reminder[] getReminder() {
    return reminder;
  }

  public Attachment[] getAttachments() {
    return attachments;
  }

  public String getStatus() {
    return status;
  }
  
//  private String name;
//  private String note;
//  private Date from;
//  private Date to;
//  private T calendar;
//  private String[] categories;
//  private String[] delegation;
//  private int priority;
//  private Reminder[] reminder;
//  private Attachment[] attachments;
//  private String status;  
  
//  @Override
//  public String toString() {
//    try {
//      JSONObject json = new JSONObject(super.toString());
//      json.append("", value)
//    } catch (JSONException e) {
//      e.printStackTrace();
//    }
//    return null;
//  }
}