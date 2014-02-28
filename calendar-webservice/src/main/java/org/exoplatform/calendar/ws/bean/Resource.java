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

import java.io.Serializable;

import org.exoplatform.commons.utils.ISO8601;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Resource implements Serializable {
	/**
   * 
   */
  protected static final long serialVersionUID = 8580243046687838325L;
  /**
   * 
   */
  private String id;
  private String href;
	
	String fields = null;
	String datePatern = ISO8601.COMPLETE_DATE_FORMAT;

	
	public String getId(){
		return id;
	}

  public void setId(String id) {
    this.id = id;
  }

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }
  
  @Override
  public String toString() {
    JSONObject json = new JSONObject();
    try {
      json.append("id", id);
      json.append("href", href);
      return json.toString();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }
}
