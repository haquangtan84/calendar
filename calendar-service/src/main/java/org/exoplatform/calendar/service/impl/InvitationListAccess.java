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

import javax.jcr.NodeIterator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarException;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.Invitation;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;

public class InvitationListAccess extends AbstractListAccess<Invitation> {

  private EventDAOImpl evtDAO;

  private EventQuery   query;

  public InvitationListAccess(EventDAOImpl evtDAO, EventQuery query) {
    super(query.isReturnSize());
    this.evtDAO = evtDAO;
    this.query = query;
  }

  protected Invitation[] loadData(int offset, int limit) {
    try {
      QueryImpl jcrQuery = evtDAO.createJCRQuery(query.getQueryStatement(), query.getQueryType());
      if (limit > 0) {
        jcrQuery.setOffset(offset);
        jcrQuery.setLimit(limit);        
      }

      NodeIterator events = jcrQuery.execute().getNodes();
      List<Invitation> invitations = new LinkedList<Invitation>();

      while (events.hasNext()) {
        CalendarEvent event = evtDAO.getEventFromNode(events.nextNode());
        if (query.getParticipants() != null && query.getParticipants().length > 0) {
          Arrays.sort(query.getParticipants());          
          for (Invitation ivt : event.getInvitations()) {            
            if (Arrays.binarySearch(query.getParticipants(), ivt.getParticipant()) >= 0) {
              invitations.add(ivt);
            }
          }
        } else {
          invitations.addAll(Arrays.asList(event.getInvitations()));          
        }
      }
      return invitations.toArray(new Invitation[invitations.size()]);
    } catch (Exception ex) {
      throw new CalendarException(null, ex);
    }
  }
}
