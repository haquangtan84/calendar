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
import static org.exoplatform.calendar.ws.CalendarRestApi.CATEGORY_URI;

import org.exoplatform.calendar.service.EventCategory;

public class CategoryResource extends Resource {
  private static final long serialVersionUID = 6940106249883390857L;
  private String name;
  
	public CategoryResource(EventCategory data) {
	  setId(data.getId());
	  setHref(new StringBuffer(CAL_BASE_URI).append(CATEGORY_URI).append(data.getId()).toString());
	  name = data.getName();
	}
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
