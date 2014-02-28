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

import static org.exoplatform.calendar.ws.CalendarRestApi.ATTACHMENT_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.CAL_BASE_URI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class AttachmentResource extends Resource {
  private static final long serialVersionUID = -9218103606107024398L;

  private static final Log LOG = ExoLogger.getExoLogger(AttachmentResource.class);

  private String name;
  private String mimeType; 
  private long weight;

  public AttachmentResource(Attachment data) {
    try {
      setId(data.getDataPath());
    } catch (Exception e) {
      if(LOG.isDebugEnabled()) LOG.debug(e.getMessage());
    }
    try {
     if(getId() != null) setHref(new StringBuffer(CAL_BASE_URI).append(ATTACHMENT_URI).append(URLEncoder.encode(getId(), "ISO-8859-1")).toString());
    } catch (UnsupportedEncodingException e) {
      if(LOG.isDebugEnabled()) LOG.debug(e.getMessage());
    }
    name = data.getName();
    mimeType = data.getMimeType();
    weight = data.getSize();
  }

  public String getName() {
    return name;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getWeight() {
    return weight;
  }  
}
