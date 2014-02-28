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
import static org.exoplatform.calendar.ws.CalendarRestApi.EVENT_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.INVITATION_URI;

import java.util.Collection;
import java.util.Map;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.ws.bean.CollectionResource;
import org.exoplatform.calendar.ws.bean.InvitationResource;
import org.exoplatform.common.http.HTTPMethods;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 21, 2014
 */
public class TestInvitationRestApi extends TestRestApi {
  private CalendarEvent uEvt;
  private CalendarEvent gEvt;
  private CalendarEvent sEvt;
  
  public void setUp() throws Exception {
    super.setUp();
    uEvt = createEvent(userCalendar, "user event");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, true);
    
    gEvt = createEvent(groupCalendar, "group event");
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, true);
    
    sEvt = createEvent(sharedCalendar, "share event");
    calendarService.saveUserEvent("root", sharedCalendar.getId(), sEvt, true);    
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  @SuppressWarnings("rawtypes")
  public void testGetInvitations() throws Exception {
    login("john");
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    assertEquals(0, invitation.getSize());
    
    //john is particiant in uEvt
    //john doesn't have permission on event of root private calendar
    uEvt.addParticipant("john", "yes");
    uEvt.addParticipant("root", "");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    //
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    assertEquals(1, invitation.getSize());
    //returnSize is false by default
    assertEquals(-1, invitation.getFullSize());
    assertNull(response.getHttpHeaders().get(CalendarRestApi.HEADER_LINK));
    
    login("root");  
    //root has edit permission
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + "?returnSize=true", baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    assertEquals(2, invitation.getSize());
    assertEquals(2, invitation.getFullSize());
    assertNotNull(response.getHttpHeaders().get(CalendarRestApi.HEADER_LINK));            
    
    //jsonp
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + "?returnSize=true&jsonp=callback", baseURI, h, null, writer);
    String data = (String) response.getEntity();
    StringBuilder sb = new StringBuilder("callback(").append(new JsonGeneratorImpl().createJsonObject(invitation)).append(");");
    assertEquals(sb.toString(), data);
  }
  
  @SuppressWarnings("rawtypes")
  public void testGetInvitationInShared() throws Exception {    
    login("john");
    //add root as participant in shared calendar
    sEvt.addParticipant("root", "yes");
    calendarService.saveUserEvent("root", sharedCalendar.getId(), sEvt, false);
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    //john has edit permission on shared calendar
    assertEquals(1, invitation.getSize());
    
    login("root");
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    //root has edit permission on sEvt
    assertEquals(1, invitation.getSize());
  }
  
  @SuppressWarnings("rawtypes")
  public void testGetInvitationInGroup() throws Exception {    
    login("john");

    //add root as participant in group calendar
    gEvt.addParticipant("mary", "yes");
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, false);
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();    
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    //john can view groupCalendar but doen't has edit permission
    assertEquals(0, invitation.getSize());

    login("root");
    response= service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    //root has edit permission on gEvt
    assertEquals(1, invitation.getSize());
  }
  
  public void testGetInvitationWithFields() throws Exception {    
    runTestFields(CAL_BASE_URI + INVITATION_URI);
  }
  
  public void testGetInvitationWithJSONP() throws Exception {    
    runTestJSONP(CAL_BASE_URI + INVITATION_URI);
  }
  
  public void testGetInvitationWithOffset() throws Exception {    
    //prepare 1 invitation for root in uEvt
    //20 invitations in gEvt
    uEvt.addParticipant("root", "yes");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);    
    for (int i = 0; i < 20; i++) {
      gEvt.addParticipant("user" + i, "");
    }
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, false);    

    runTestOffset(CAL_BASE_URI + INVITATION_URI, true);
  }

  @SuppressWarnings("unchecked")
  public void testGetInvitationById() throws Exception {
    login("root");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + gEvt.getId() + ":mary", baseURI, h, null, writer);
    //no invitation in database
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());

    gEvt.addParticipant("mary", "no");
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, false);
    
    login("john");
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + gEvt.getId() + ":mary", baseURI, h, null, writer);
    //john is not participant and not has edit permission
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
    
    login("root");
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + gEvt.getId() + ":mary", baseURI, h, null, writer);
    //root is not paritipant but has edit permission
    assertEquals(HTTPStatus.OK, response.getStatus());
    InvitationResource invitation = (InvitationResource)response.getEntity();
    assertEquals("mary", invitation.getParticipant());
    
    uEvt.addParticipant("john", "no");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    
    login("john");
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + 
                       uEvt.getId() + ":john" + "?fields=participant,abcd", baseURI, h, null, writer);
    //john is paritipant and doesn't has edit permission
    assertEquals(HTTPStatus.OK, response.getStatus());
    Map<String, Object> ivtMap = (Map<String, Object>)response.getEntity();
    assertEquals("john", ivtMap.get("participant"));
    //only return field participant, ignore 'abcd' attribute, it doesn't exists
    assertNull(ivtMap.get("status"));
  }

  public void testUpdateInvitationById() throws Exception {
    uEvt.addParticipant("john", "no");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    
    login("root");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);
    InvitationResource invite = (InvitationResource)response.getEntity();
    assertEquals("no", invite.getStatus());

    response = service(HTTPMethods.PUT, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john?status=pending", baseURI, h, null, writer);
    //root has edit permission, but he's not the participant
    assertEquals(HTTPStatus.UNAUTHORIZED, response.getStatus());

    login("john");
    response = service(HTTPMethods.PUT, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john?status=pending", baseURI, h, null, writer);
    //john doesn't has edit permission, but he's participant
    assertEquals(HTTPStatus.OK, response.getStatus());
    
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);
    invite = (InvitationResource)response.getEntity();
    assertEquals("pending", invite.getStatus());
    
    //try to update non-exists invitation
    response = service(HTTPMethods.PUT, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":root?status=pending", baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }

  public void testDeleteInvitationById() throws Exception {
    uEvt.addParticipant("john", "no");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    
    login("john");
    //
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);
    InvitationResource invite = (InvitationResource)response.getEntity();
    assertEquals("no", invite.getStatus());
    
    response = service(HTTPMethods.DELETE, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);    
    //john can get, update, but can't delete invitation because he doesn't 
    //has edit permission
    assertEquals(HTTPStatus.UNAUTHORIZED, response.getStatus());

    login("root");
    response = service(HTTPMethods.DELETE, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    
    response = service(HTTPMethods.GET, CAL_BASE_URI + INVITATION_URI + uEvt.getId() + ":john", baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }
  
  @SuppressWarnings("rawtypes")
  public void testGetInvitationsFromEvent() throws Exception {        
    login("john");
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();    
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + EVENT_URI + "notExistsEvent"+ INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    assertEquals(0, invitation.getSize());
    
    //john is particiant in uEvt
    //john doesn't have permission on event of root private calendar
    uEvt.addParticipant("john", "yes");
    uEvt.addParticipant("root", "");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    //
    response = service(HTTPMethods.GET, CAL_BASE_URI + EVENT_URI + uEvt.getId() + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    assertEquals(1, invitation.getSize());
    //returnSize is always true for getInvitationFromEvent
    assertEquals(1, invitation.getFullSize());
    assertNotNull(response.getHttpHeaders().get(CalendarRestApi.HEADER_LINK));

    login("root");  
    //root has edit permission
    response = service(HTTPMethods.GET, CAL_BASE_URI + EVENT_URI + uEvt.getId() + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    assertEquals(2, invitation.getSize());
    assertEquals(2, invitation.getFullSize());
    assertNotNull(response.getHttpHeaders().get(CalendarRestApi.HEADER_LINK));
  }
  
  @SuppressWarnings("rawtypes")
  public void testGetInvitationsFromEvent_Status() throws Exception {
    uEvt.addParticipant("john", "yes");
    uEvt.addParticipant("root", "no");
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    
    login("root");  
    //root has edit permission
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + EVENT_URI + uEvt.getId() + INVITATION_URI
                                         + "?status=yes", baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    assertEquals(1, invitation.getSize());
    assertEquals(1, invitation.getFullSize());
    InvitationResource ivt = (InvitationResource)invitation.getData().iterator().next();
    assertEquals("yes", ivt.getStatus());
  }  
  
  public void testGetInvitationFromEvent_Fields() throws Exception {
    runTestFields(CAL_BASE_URI + EVENT_URI + gEvt.getId() + INVITATION_URI);
  }
  
  public void testGetInvitationFromEvent_JSONP() throws Exception {
    runTestJSONP(CAL_BASE_URI + EVENT_URI + gEvt.getId() + INVITATION_URI);
  }

  public void testGetInvitationFromEvent_Offset() throws Exception {    
   //prepare 1 invitation for root in uEvt
    //20 invitations in gEvt
    uEvt.addParticipant("root", "yes");
    for (int i = 0; i < 20; i++) {
      uEvt.addParticipant("user" + i, "");
    }
    calendarService.saveUserEvent("root", userCalendar.getId(), uEvt, false);
    
    runTestOffset(CAL_BASE_URI + EVENT_URI + uEvt.getId() + INVITATION_URI, false);
  }  

  public void testCreateInvitationForEvent() throws Exception {
    login("john");
    String queryParams = "?participant=john&status=pending";
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.POST, CAL_BASE_URI + EVENT_URI + "notExistsEvent" + 
                                         INVITATION_URI + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());

    response = service(HTTPMethods.POST, CAL_BASE_URI + EVENT_URI + uEvt.getId() +
                       INVITATION_URI, baseURI, h, null, writer);
    //no query params
    assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());
    
    response = service(HTTPMethods.POST, CAL_BASE_URI + EVENT_URI + uEvt.getId() +
                       INVITATION_URI + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.UNAUTHORIZED, response.getStatus());

    login("root");
    response = service(HTTPMethods.POST, CAL_BASE_URI + EVENT_URI + uEvt.getId() +
                                         INVITATION_URI + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.CREATED, response.getStatus());
    InvitationResource invite = (InvitationResource)response.getEntity();
    assertEquals("john", invite.getParticipant());
    assertEquals("pending", invite.getStatus());

    response = service(HTTPMethods.GET, CAL_BASE_URI + EVENT_URI + uEvt.getId() + INVITATION_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource<?> invitation = (CollectionResource<?>)response.getEntity();
    assertEquals(1, invitation.getSize());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void runTestFields(String uri) throws Exception {
    login("root");

    gEvt.addParticipant("root", "yes");
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, false);
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();    
    String queryParams = "?fields=participant,status";
    ContainerResponse response = service(HTTPMethods.GET, uri + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    assertEquals(1, invitation.getSize());
    
    Collection data = invitation.getData();
    Map<String, Object> dataMap = (Map<String, Object>) data.iterator().next();
    assertNotNull(dataMap.get("participant"));
    assertNotNull(dataMap.get("status"));
    assertNull(dataMap.get("event"));
    assertNull(dataMap.get("id"));
    assertNull(dataMap.get("href"));
  }
  
  private void runTestJSONP(String uri) throws Exception {
    login("root");

    gEvt.addParticipant("root", "yes");
    calendarService.savePublicEvent(groupCalendar.getId(), gEvt, false);
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();    
    String queryParams = "?jsonp=callback";
    ContainerResponse response = service(HTTPMethods.GET, uri + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    String invitation = (String)response.getEntity();
    assertTrue(invitation.matches("callback\\(\\{.+\\}\\);"));
  }
  
  @SuppressWarnings("rawtypes")
  private void runTestOffset(String uri, boolean supportReturnSize) throws Exception {
    login("root");

    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    String queryParams = "?offset=0&limit=1";
    ContainerResponse response = service(HTTPMethods.GET, uri + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    CollectionResource invitation = (CollectionResource)response.getEntity();
    assertEquals(1, invitation.getSize());
    if (supportReturnSize) {
      assertEquals(-1, invitation.getFullSize());      
    }
    
    queryParams = "?offset=10&limit=11&returnSize=true";
    response = service(HTTPMethods.GET, uri + queryParams, baseURI, h, null, writer);
    assertEquals(HTTPStatus.OK, response.getStatus());
    invitation = (CollectionResource)response.getEntity();
    //offset = 10, first invitation is in uEvt:root --> the index at 10 is invitation of user9
    InvitationResource ivt = (InvitationResource)invitation.getData().iterator().next();
    assertEquals("user9", ivt.getParticipant());
    //By default, rest api only allow to load maximum 10 results
    assertEquals(10, invitation.getSize());
    assertEquals(21, invitation.getFullSize());
    assertNotNull(response.getHttpHeaders().get(CalendarRestApi.HEADER_LINK));
  }
}
