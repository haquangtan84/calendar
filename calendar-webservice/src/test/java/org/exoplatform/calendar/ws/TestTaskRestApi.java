/*
 * Copyright (C) 2003-2014 eXo Platform SAS. 
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
package org.exoplatform.calendar.ws;

import static org.exoplatform.calendar.ws.CalendarRestApi.CAL_BASE_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.TASK_URI;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.ws.bean.Resource;
import org.exoplatform.calendar.ws.bean.TaskResource;
import org.exoplatform.common.http.HTTPMethods;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 21, 2014
 */
public class TestTaskRestApi extends AbstractTestEventRestApi {
  
  public void testGetTasks() throws Exception {
    runTestGetEvents(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }

  public void testGetTasks_Public() throws Exception {
    runTestGetEvents_Public(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }

  public void testGetTasks_Group() throws Exception {
    runTestGetEvents_Group(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
  
  public void testGetTasks_Shared() throws Exception {
    runTestGetEventById_Shared(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
  
  public void testGetTaskById() throws Exception {
    runTestGetEventById(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }

  public void testGetTaskById_Public() throws Exception {
    runTestGetEventById_Public(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
  
  public void testGetTaskById_Group() throws Exception {
    runTestGetEventById_Group(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
  
  public void testGetTaskById_Shared() throws Exception {
    runTestGetEventById_Shared(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
  
  @SuppressWarnings("unchecked")
  public void testGetTaskById_Expand() throws Exception {    
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(CalendarEvent.TYPE_TASK);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    
    login("root");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + TASK_URI + uEvt.getId() + "?expand=calendar", baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());    
    Resource calR0 = (Resource)response.getEntity();
    assertNotNull(calR0);
    assertEquals(uEvt.getId(), calR0.getId());
    
    TaskResource<Calendar> calR1 = (TaskResource<Calendar>) response.getEntity();
    assertNotNull(calR1.getCalendar());
    assertEquals(calR1.getCalendar().getName(), userCalendar.getName());
  }
  
  public void testUpdateTask() throws Exception {
    runTestUpdateEvent(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }

  public void testDeleteTaskById() throws Exception {
    runTestDeleteEventById(CAL_BASE_URI + TASK_URI, CalendarEvent.TYPE_TASK);
  }
}
