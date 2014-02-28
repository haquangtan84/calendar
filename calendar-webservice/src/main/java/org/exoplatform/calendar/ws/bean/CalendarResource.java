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
import static org.exoplatform.calendar.ws.CalendarRestApi.CALENDAR_URI;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.Utils;

public class CalendarResource extends Resource {

  /**
   * 
   */
  private static final long serialVersionUID = -4500214043430048066L;

  private String name;
  private String description ;
  private String type;
  private String timeZone;
  private String color;
  private String owner;
  private String viewPermision;
  private String editPermission;
  private String[] groups;
  private String publicURL;
  private String privateURL;
  private String icsURL;

  public CalendarResource(Calendar data) {
    setId(data.getId());
    setHref(new StringBuffer(CAL_BASE_URI).append(CALENDAR_URI).append(data.getId()).toString());
    name = data.getName();
    description = data.getDescription();
    type = String.valueOf(data.getCalType());
    timeZone = data.getTimeZone();
    color = data.getCalendarColor();
    owner = data.getCalendarOwner();
    
    StringBuffer sb = new StringBuffer();
    if(data.getViewPermission() != null)
    for (String s: data.getViewPermission()) {
      sb.append(s).append(Utils.SEMICOLON);
    }
    viewPermision = sb.toString();
    sb = new StringBuffer();
    if(data.getEditPermission() != null)
    for (String s: data.getEditPermission()) {
      sb.append(s).append(Utils.SEMICOLON);
    }
    editPermission = sb.toString();
    groups = data.getGroups();
    publicURL = data.getPublicUrl();
    privateURL = data.getPrivateUrl();
    icsURL = data.getCalendarPath();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return type;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public String getColor() {
    return color;
  }

  public String getOwner() {
    return owner;
  }

  public String getViewPermision() {
    return viewPermision;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public String[] getGroups() {
    return groups;
  }

  public String getPublicURL() {
    return publicURL;
  }

  public String getPrivateURL() {
    return privateURL;
  }

  public String getIcsURL() {
    return icsURL;
  }
}