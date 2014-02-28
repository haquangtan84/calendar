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

import static org.exoplatform.calendar.ws.CalendarRestApi.HEADER_LINK;

import java.util.Collection;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.ws.bean.CollectionResource;
import org.exoplatform.calendar.ws.bean.Resource;
import org.exoplatform.common.http.HTTPMethods;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

public abstract class AbstractTestEventRestApi extends TestRestApi {
  
  protected String XML_DATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<root><data>hello world xml</data></root>";

  protected String JSON_DATA = "{\"data\":\"hello world json\"}";

  @SuppressWarnings("rawtypes")
  public void runTestGetEvents(String uri, String eventType) throws Exception {
    for (int i = 0; i < 20; i++) {
      CalendarEvent uEvt = createEvent(userCalendar);
      uEvt.setEventType(eventType);
      calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    }
   
    login("root");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource calR = (CollectionResource)response.getEntity();
    Collection evs = calR.getData();
    assertEquals(10, evs.size());
    assertEquals(-1, calR.getFullSize());
    assertNull(response.getHttpHeaders().get(HEADER_LINK));
    
    String queryParams = "?returnSize=true";
    response = service(HTTPMethods.GET, uri + queryParams, baseURI, h, null, writer);
    calR = (CollectionResource)response.getEntity();
    evs = calR.getData();
    assertEquals(10, evs.size());
    assertEquals(20, calR.getFullSize());
    assertNotNull(response.getHttpHeaders().get(HEADER_LINK));
    
    login("john");
    //john can read event from private calendar
    response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    calR = (CollectionResource<?>)response.getEntity();
    evs = calR.getData();
    assertEquals(0, evs.size());
    
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    uEvt.addParticipant("john", "");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    //john can read event that he is participant
    response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    calR = (CollectionResource<?>)response.getEntity();
    evs = calR.getData();
    assertEquals(1, evs.size());
    
    //jsonp
    response = service(HTTPMethods.GET, uri + "?jsonp=callback", baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    String data = (String) response.getEntity();
    StringBuilder sb = new StringBuilder("callback(").append(new JsonGeneratorImpl().createJsonObject(calR)).append(");");
    assertEquals(sb.toString(), data);
  }
  
  public void runTestGetEvents_Public(String uri, String eventType) throws Exception {
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);

    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource<?> calR = (CollectionResource<?>)response.getEntity();
    Collection<?> evs = calR.getData();
    assertEquals(0, evs.size());
    
    userCalendar.setPublicUrl("/test/url.ics");
    calendarService.saveUserCalendar("root", userCalendar, false);
    //john can read event from public calendar
    response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    calR = (CollectionResource<?>)response.getEntity();
    evs = calR.getData();
    assertEquals(1, evs.size());    
  }
  
  public void runTestGetEvents_Group(String uri, String eventType) throws Exception {
    CalendarEvent gEvt = createEvent(groupCalendar);
    gEvt.setEventType(eventType);
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, true);
    
    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource<?> calR = (CollectionResource<?>)response.getEntity();
    Collection<?> evs = calR.getData();
    assertEquals(1, evs.size());
  }
  
  public void runTestGetEvents_Shared(String uri, String eventType) throws Exception {
    CalendarEvent sEvt = createEvent(sharedCalendar);
    sEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", sharedCalendar.getId(), sEvt, true);
    
    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource<?> calR = (CollectionResource<?>)response.getEntity();
    Collection<?> evs = calR.getData();
    assertEquals(1, evs.size());
    
    login("mary");
    //sharedCalendar is not shared to mary
    writer = new ByteArrayContainerResponseWriter();
    response = service(HTTPMethods.GET, uri, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    calR = (CollectionResource<?>)response.getEntity();
    evs = calR.getData();
    assertEquals(0, evs.size());
  }
  
  public void runTestGetEventById(String uri, String eventType) throws Exception {
    login("root");
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri + "notExists", baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
    
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    //
    response = service(HTTPMethods.GET, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    Resource calR0 = (Resource)response.getEntity();
    assertNotNull(calR0);
    assertEquals(uEvt.getId(), calR0.getId());
    
    login("john");
    //
    response = service(HTTPMethods.GET, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
    
    uEvt.addParticipant("john", "");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    //john can read event in private calendar if he's participant
    response = service(HTTPMethods.GET, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    
    //jsonp
    response = service(HTTPMethods.GET, uri + uEvt.getId() + "?fields=id&jsonp=callback", baseURI, h, null, writer);
    String data = (String) response.getEntity();
    StringBuilder sb = new StringBuilder("callback(").append("{\"id\":\"" + uEvt.getId() + "\"}").append(");");
    assertEquals(sb.toString(), data);
  }
  
  public void runTestGetEventById_Public(String uri, String eventType) throws Exception {
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    
    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
    
    userCalendar.setPublicUrl("test/url.ics");
    calendarService.saveUserCalendar("root", userCalendar, false);
    //
    response = service(HTTPMethods.GET, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    Resource calR0 = (Resource)response.getEntity();
    assertNotNull(calR0);
    assertEquals(uEvt.getId(), calR0.getId());
  }
  
  public void runTestGetEventById_Group(String uri, String eventType) throws Exception {
    CalendarEvent gEvt = createEvent(groupCalendar);
    gEvt.setEventType(eventType);
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, true);
    
    login("john", "/platform/administrators:member");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri + gEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
  }
  
  public void runTestGetEventById_Shared(String uri, String eventType) throws Exception {
    CalendarEvent sEvt = createEvent(sharedCalendar);
    sEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", sharedCalendar.getId(), sEvt, true);
    
    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, uri + sEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());

    login("mary");
    //
    response = service(HTTPMethods.GET, uri + sEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }
  
  public void runTestUpdateEvent(String uri, String eventType) throws Exception {
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);

    JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
    JsonValue json = generatorImpl.createJsonObject(uEvt);

    byte[] data = json.toString().getBytes("UTF-8");
    h = new MultivaluedMapImpl();
    h.putSingle("content-type", "application/json");
    h.putSingle("content-length", "" + data.length);

    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.PUT, uri + uEvt.getId(), baseURI, h, data, writer);
    assertEquals(HTTPStatus.UNAUTHORIZED, response.getStatus());
    
    login("root");
    //
    response = service(HTTPMethods.PUT, uri + uEvt.getId(), baseURI, h, data, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
  }
  
  public void runTestDeleteEventById(String uri, String eventType) throws Exception {
    CalendarEvent uEvt = createEvent(userCalendar);
    uEvt.setEventType(eventType);
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);

    JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
    JsonValue json = generatorImpl.createJsonObject(uEvt);

    byte[] data = json.toString().getBytes("UTF-8");
    h = new MultivaluedMapImpl();
    h.putSingle("content-type", "application/json");
    h.putSingle("content-length", "" + data.length);

    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.DELETE, uri + uEvt.getId(), baseURI, h, null, writer);
    assertEquals(HTTPStatus.UNAUTHORIZED, response.getStatus());
    
    login("root");
    //
    response = service(HTTPMethods.DELETE, uri + uEvt.getId(), baseURI, h, data, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    assertNull(calendarService.getEventById(uEvt.getId()));
  }  
}
