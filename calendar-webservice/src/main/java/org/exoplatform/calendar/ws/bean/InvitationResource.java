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
import static org.exoplatform.calendar.ws.CalendarRestApi.EVENT_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.INVITATION_URI;

import java.io.Serializable;

import org.exoplatform.calendar.service.Invitation;

public class InvitationResource extends Resource implements Serializable {
  private static final long serialVersionUID = -5546515171185717545L;

  private String            event;

  private String            participant;

  private String            status;

  public InvitationResource(Invitation data) {
    setId(data.getId());
    setHref(new StringBuffer(CAL_BASE_URI).append(INVITATION_URI).append(data.getId()).toString());
    event = new StringBuffer(CAL_BASE_URI).append(EVENT_URI).append(data.getEventId()).toString();
    this.participant = data.getParticipant();
    this.status = data.getStatus();
  }

  public String getEvent() {
    return event;
  }

  public String getParticipant() {
    return participant;
  }

  public String getStatus() {
    return status;
  }

}
