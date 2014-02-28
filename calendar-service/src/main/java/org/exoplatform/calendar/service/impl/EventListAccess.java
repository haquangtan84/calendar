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
import javax.jcr.query.Query;

import java.util.LinkedList;
import java.util.List;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarException;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;

public class EventListAccess extends AbstractListAccess<CalendarEvent> {

  private EventDAOImpl evtDAO;

  private String   queryStm;

  public EventListAccess(EventDAOImpl evtDAO, EventQuery eventQuery) {
    super(eventQuery.isReturnSize());
    this.evtDAO = evtDAO;
    queryStm = buildQuery(eventQuery);
  }

  protected CalendarEvent[] loadData(int offset, int limit) {
    try {
      QueryImpl jcrQuery = evtDAO.createJCRQuery(queryStm, Query.SQL);
      if (limit > 0) {
        jcrQuery.setOffset(offset);
        jcrQuery.setLimit(limit);
      }

      NodeIterator iter = jcrQuery.execute().getNodes();
      List<CalendarEvent> events = new LinkedList<CalendarEvent>();

      while (iter.hasNext()) {
        CalendarEvent event = evtDAO.getEventFromNode(iter.nextNode());
        events.add(event);
      }
      return events.toArray(new CalendarEvent[events.size()]);
    } catch (Exception ex) {
      throw new CalendarException(null, ex);
    }
  }
  
  private String buildQuery(EventQuery eventQuery) {
    //events from user, groups, public calendars, and events that contains specific participant
    StringBuilder sql = new StringBuilder("SELECT * FROM ");
    sql.append(Utils.EXO_CALENDAR_EVENT);

    sql.append(" WHERE");
    //find event from specific calendar
    if (eventQuery.getCalendarPath() != null) {
      sql.append(" jcr:path LIKE '").append(eventQuery.getCalendarPath()).append("/%'");
    }

    if (eventQuery.getCalendarId() != null || eventQuery.getParticipants() != null) {
      sql.append(" AND (");
      //calendarIds: public and groups, shared calendars
      if (eventQuery.getCalendarId() != null) {
        for (String calId : eventQuery.getCalendarId()) {
          sql.append(" OR ").append(Utils.EXO_CALENDAR_ID).append(" = '").append(calId).append("'");
        }
      }
      //participant
      if (eventQuery.getParticipants() != null) {
        for (String participant : eventQuery.getParticipants()) {
          sql.append(" OR ").append(Utils.EXO_PARTICIPANT).append(" = '").append(participant).append("'");
        }
      }
      sql.append(")");
    }
    
    //date time
    if(eventQuery.getFromDate() != null) {
      sql.append(" AND (").append(Utils.EXO_FROM_DATE_TIME)
      .append(" >= TIMESTAMP '").append(ISO8601.format(eventQuery.getFromDate())).append("')");
    }
    if(eventQuery.getToDate() != null) {
      sql.append(" AND (").append(Utils.EXO_TO_DATE_TIME)
      .append(" <= TIMESTAMP '").append(ISO8601.format(eventQuery.getToDate())).append("')");
    }
    //category
    String[] categoryIds = eventQuery.getCategoryId();
    if(categoryIds != null && categoryIds.length > 0) {
      sql.append(" AND (");
      for (int i = 0; i < categoryIds.length; i++) {
        sql.append(Utils.EXO_EVENT_CATEGORYID);
        sql.append(" = '").append(categoryIds[i]).append("'");
        if (i < categoryIds.length - 1) {
          sql.append(" OR ");
        }
      }
      sql.append(")");
    }
    //event or task
    if(!Utils.isEmpty(eventQuery.getEventType())) {
      sql.append(" AND ").append(Utils.EXO_EVENT_TYPE).append("='");
      sql.append(eventQuery.getEventType()).append("'");
    }
    
    int i = sql.indexOf("WHERE AND");
    if (i != -1) {
      sql.replace(i, i + 9, "WHERE");
    }
    if ((i = sql.indexOf("( OR")) != -1) {
      sql.replace(i, i + 4, "(");
    }
    return sql.toString();
  }
}