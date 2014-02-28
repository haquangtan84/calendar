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
import static org.exoplatform.calendar.ws.CalendarRestApi.FEED_URI;
import static org.exoplatform.calendar.ws.CalendarRestApi.RSS_URI;

import javax.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.RssData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.ws.bean.FeedResource;
import org.exoplatform.common.http.HTTPMethods;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 21, 2014
 *  
 *  Rest end point: "/feeds/{id}"
 *  
 *  Verb         Roles      
 *  ---------------------------------------------------------------------------------
 *  GET          Returns the feed if the authenticated user is the owner of the feed
 *  PUT          Updates the feed if the authenticated user is the owner of the feed
 *  DELETE    Deletes the feed if the authenticated user is the owner of the feed
 *  
 *  
 *  Rest end point: "/feeds/{id}/rss"
 *  
 *  Verb         Roles      
 *  ---------------------------------------------------------------------------------
 *  GET          Returns the RSS stream if:
 *                    - the calendar is public
 *                    - the authenticated user is the owner of the calendar
 *                    - the authenticated user belongs to the group of the calendar
 *                    - the calendar has been shared with the authenticated user or with a group of the authenticated user
 */
public class TestFeedRestApi extends TestRestApi {
  
  /** .*/
  private String calendarFeedNane = "Calendar_Feed";
  
  /** .*/
  private RssData rssData;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    CalendarEvent calEvent = this.createEvent(this.userCalendar);
    
    calendarService.saveUserEvent("root", userCalendar.getId(), calEvent, true);

    LinkedHashMap<String, Calendar> calendars = new LinkedHashMap<String, Calendar>();
    calendars.put(Utils.PRIVATE_TYPE + Utils.COLON + userCalendar.getId(), userCalendar);
    
    calEvent.setId("Event" + IdGenerator.generate());
    calendarService.savePublicEvent(groupCalendar.getId(), calEvent, true);
    calendars.put(Utils.PUBLIC_TYPE + Utils.COLON + groupCalendar.getId(), groupCalendar);

    rssData = new RssData();
    
    rssData.setName(calendarFeedNane + Utils.RSS_EXT);
    String url = "http://localhost:80/rest/calendar/feed/"+calendarFeedNane ;
    rssData.setUrl(url);
    rssData.setTitle(calendarFeedNane);
    rssData.setDescription("Description");
    rssData.setLink(url);
    rssData.setVersion("rss_2.0");

    calendarService.generateRss("root", calendars, rssData);
  }

  @SuppressWarnings("rawtypes")
  public void testGetFeedById() throws Exception {
    //
    this.login("root");
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane, baseURI, h, null, writer);
    assertNotNull(response);
    assertEquals(HTTPStatus.OK, response.getStatus());
    assertEquals(1, calendarService.getFeeds("root").size());

    FeedResource calR = (FeedResource)response.getEntity();
    assertNotNull(calR);
    assertEquals(rssData.getTitle(), calR.getName());
    assertTrue(calR.getCalendars()[0] instanceof String);
    
    //get fields "name", "calendars"
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + "?fields=name,calendars", baseURI, h, null, writer);
    assertTrue(response.getEntity() instanceof HashMap);
    
    //expand "calendars"
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + "?expand=calendars", baseURI, h, null, writer);
    calR = (FeedResource)response.getEntity();
    assertTrue(calR.getCalendars()[0] instanceof Calendar);
    
    //jsonp
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + "?fields=name&jsonp=callback", baseURI, h, null, writer);
    String data = (String) response.getEntity();
    assertEquals("callback({\"name\":\"Calendar_Feed\"});", data);
    
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + "?jsonp=callback", baseURI, h, null, writer);
    data = (String) response.getEntity();
    StringBuilder sb = new StringBuilder("callback({\"name\":\"Calendar_Feed\"");
    sb.append(",\"calendars\":[\"").append(userCalendar.getId()).append("\",\"").append(groupCalendar.getId()).append("\"]");
    sb.append(",\"rss\":\"/calendar/feeds/Calendar_Feed/rss\",\"id\":\"Calendar_Feed\",\"href\":\"/calendar/feeds/Calendar_Feed\"");
    sb.append("});");
    assertEquals(sb.toString(), data);

    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + "?expand=calendars(offset:0,limit:1)&jsonp=callback", baseURI, h, null, writer);
    data = (String) response.getEntity();
    JsonGeneratorImpl generator = new JsonGeneratorImpl();
    JsonValue value = generator.createJsonObject(userCalendar);
    sb = new StringBuilder("callback({\"name\":\"Calendar_Feed\"");
    sb.append(",\"calendars\":[").append(value).append("]");
    sb.append(",\"rss\":\"/calendar/feeds/Calendar_Feed/rss\",\"id\":\"Calendar_Feed\",\"href\":\"/calendar/feeds/Calendar_Feed\"");
    sb.append("});");
    assertEquals(sb.toString(), data);
    
    //
    this.login("john");
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane, baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }

  public void testUpdateFeedById() throws Exception {

    JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
    JsonValue json = generatorImpl.createJsonObject(rssData);

    byte[] data = json.toString().getBytes("UTF-8");
    h = new MultivaluedMapImpl();
    h.putSingle("content-type", "application/json");
    h.putSingle("content-length", "" + data.length);
    h.putSingle("username", "root");
    //
    this.login("root");
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.PUT, CAL_BASE_URI + FEED_URI + calendarFeedNane, baseURI, h, data, writer);
    assertNotNull(response);
    assertEquals(HTTPStatus.OK, response.getStatus());
    assertEquals(1, calendarService.getFeeds("root").size());
    
    //
    this.login("john");
    response = service(HTTPMethods.PUT, CAL_BASE_URI + FEED_URI + calendarFeedNane, baseURI, h, data, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }

  public void testDeleteFeedById() throws Exception {
    //
    this.login("root");
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.DELETE, CAL_BASE_URI + FEED_URI + calendarFeedNane, baseURI, h, null, writer);
    assertNotNull(response);
    assertEquals(HTTPStatus.OK, response.getStatus());
    assertEquals(0, calendarService.getFeeds("root").size());
  }

  public void testGetRssFromFeed() throws Exception {
    //
    this.login("root");
    
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + RSS_URI, baseURI, h, null, writer);
    assertNotNull(response);
    assertEquals(HTTPStatus.OK, response.getStatus());
    assertEquals(MediaType.APPLICATION_XML_TYPE, response.getContentType());
    
    this.login("john");
    response = service(HTTPMethods.GET, CAL_BASE_URI + FEED_URI + calendarFeedNane + RSS_URI, baseURI, h, null, writer);
    assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
  }
}
