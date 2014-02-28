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

package org.exoplatform.calendar.ws;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarCollection;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarImportExport;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.EventDAO;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.FeedData;
import org.exoplatform.calendar.service.Invitation;
import org.exoplatform.calendar.service.MultiListAccess;
import org.exoplatform.calendar.service.MultiListAccess.LinkableListAccess;
import org.exoplatform.calendar.service.RssData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.ws.bean.AttachmentResource;
import org.exoplatform.calendar.ws.bean.CalendarResource;
import org.exoplatform.calendar.ws.bean.CategoryResource;
import org.exoplatform.calendar.ws.bean.CollectionResource;
import org.exoplatform.calendar.ws.bean.EventResource;
import org.exoplatform.calendar.ws.bean.FeedResource;
import org.exoplatform.calendar.ws.bean.InvitationResource;
import org.exoplatform.calendar.ws.bean.Resource;
import org.exoplatform.calendar.ws.bean.TaskResource;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;

@Path("/calendar")
public class CalendarRestApi implements ResourceContainer {
  private Log log = ExoLogger.getExoLogger(CalendarRestApi.class);
  public static String VERSION_LASTEST = "v1";
  public static final String TEXT_ICS = "text/calendar";
  public static final MediaType TEXT_ICS_TYPE = new MediaType("text","calendar");
  public final static String BASE_URL = "/cs/calendar";
  public final static String BASE_EVENT_URL = BASE_URL + "/event";

  public final static String CAL_BASE_URI = "/calendar";

  public final static String CALENDAR_URI = "/calendars/";
  public final static String EVENT_URI = "/events/";
  public final static String TASK_URI = "/tasks/";
  public final static String ICS_URI = "/ics";
  public final static String ATTACHMENT_URI = "/attachments/";
  public final static String OCCURRENCE_URI = "/occurrences";
  public final static String CATEGORY_URI = "/categories/";
  public final static String FEED_URI = "/feeds/";
  public final static String RSS_URI = "/rss";
  public final static String INVITATION_URI ="/invitations/";
  public static final String HEADER_LINK = "link";
  
  private OrganizationService orgService;
  private int query_limit = 10;
  private SubResourceHrefBuilder subResourcesBuilder = new SubResourceHrefBuilder(this);

  private final static CacheControl cc = new CacheControl();
  static {
    cc.setNoCache(true);
    cc.setNoStore(true);
  }
  
  public CalendarRestApi(OrganizationService orgService, InitParams params) {
    this.orgService = orgService;
    
    if (params != null && params.getValueParam("query_limit") != null) {
      query_limit = Integer.parseInt(params.getValueParam("query_limit").getValue());
    }
  }

  @GET
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubResources(@Context UriInfo uri) {
    Map<String, String[][]> subResources = new HashMap<String, String[][]>();
    subResources.put("subResourcesHref", subResourcesBuilder.buildResourceMap(uri));

    return Response.ok(subResources, MediaType.APPLICATION_JSON).cacheControl(cc).build();
  }

  /**
   * Returns a calendar in the list when:
   * the authenticated user is the owner of the calendar
   * the authenticated user belongs to the group of the calendar
   * the calendar has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @GET
  @RolesAllowed("users")
  @Path("/calendars/")
  @Produces({MediaType.APPLICATION_JSON})
  public Response getCalendars(@QueryParam("type") int type,
                                                @QueryParam("offset") int offset, 
                                                @QueryParam("limit") int limit,
                                                @QueryParam("fields") String fields,
                                                @QueryParam("jsonp") String jsonp,
                                                @Context UriInfo uri) {
    try {
      limit = parseLimit(limit);
      CalendarCollection<Calendar> cals = calendarServiceInstance().getAllCalendars(currentUserId(), type, offset, limit);
      if(cals == null || cals.isEmpty()) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      Collection data = new LinkedList();
      Iterator<Calendar> calIterator = cals.iterator();
      while (calIterator.hasNext()) {
         Calendar cal = calIterator.next();
         data.add(extractObject(new CalendarResource(cal), fields));
      }
      
      CollectionResource calData = new CollectionResource(data, cals.getFullSize());
      calData.setOffset(offset);
      calData.setLimit(limit);
      
      if (jsonp != null) {
        JsonValue value = new JsonGeneratorImpl().createJsonObject(calData);
        StringBuilder sb = new StringBuilder(jsonp);
        sb.append('(').append(value).append(");");
        return Response.ok(sb.toString(), new MediaType("text", "javascript")).header(HEADER_LINK, buildFullUrl(uri, offset, limit, calData.getFullSize())).cacheControl(cc).build();
      }
      
      //
      return Response.ok(calData, MediaType.APPLICATION_JSON).header(HEADER_LINK, buildFullUrl(uri, offset, limit, calData.getFullSize())).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();

  }

  /**
   *  Creates a calendar if:
   * this is a personal calendar and the user is authenticated
   * this is a group calendar and the user is authenticated and belongs to the group
   */
  @POST
  @RolesAllowed("users")
  @Path("/calendars/")
  @Produces(MediaType.APPLICATION_JSON)
	public Response createCalendar(Calendar cal) {

		if (cal.getGroups() != null && cal.getGroups().length > 0) {
			// Create a group calendar
			if (isInGroups(cal.getGroups())) {
				calendarServiceInstance().savePublicCalendar(cal, true);
			} else {
				return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
			}
		} else {
		  if (cal.getCalendarOwner() != null && !cal.getCalendarOwner().equals(currentUserId())) {
		    return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
		  } else {
		    // Create a personal calendar
		    calendarServiceInstance().saveUserCalendar(currentUserId(), cal, true);		    
		  }
		}

		CalendarResource calData = new CalendarResource(cal);
		return Response.ok(calData, MediaType.APPLICATION_JSON).status(HTTPStatus.CREATED).cacheControl(cc).build();
	}

  /**
   * Returns the calendar if:
   * the authenticated user is the owner of the calendar
   * the authenticated user belongs to the group of the calendar
   * the calendar has been shared with the authenticated user or with a group of the authenticated user
   */
  @GET
  @RolesAllowed("users")
  @Path("/calendars/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCalendarById(@PathParam("id") String id, @QueryParam("fields") String fields, @QueryParam("jsonp") String jsonp) {
    try {
      Calendar cal = calendarServiceInstance().getCalendarById(id);
      if(cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      CalendarResource calData = null;
      if (this.hasViewCalendarPermission(cal, currentUserId())) {
        calData = new CalendarResource(cal);
      }
      
      if (calData == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build(); 
      
      Object resource = extractObject(calData, fields);
      if (jsonp != null) {
        String json = null;
        if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
        else {
          JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
          json = generatorImpl.createJsonObject(resource).toString();
        }
        StringBuilder sb = new StringBuilder(jsonp);
        sb.append('(').append(json).append(");");
        return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
      }
      
      //
      return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *  Updates the calendar if:
   * the authenticated user is the owner of the calendar
   * for group calendars, the authenticated user has edit rights on the calendar
   */
  @PUT
  @RolesAllowed("users")
  @Path("/calendars/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateCalendarById(@PathParam("id") String id, Calendar calObj) {
    try {
      Calendar cal = calendarServiceInstance().getCalendarById(id);
      if(cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      //Only allow to edit if user is owner of calendar, or have edit permission on group calendar
      //don't allow to edit shared calendar, or remote calendar
      if ((currentUserId().equals(cal.getCalendarOwner()) || cal.getGroups() != null) &&
          Utils.isCalendarEditable(currentUserId(), cal)) {
        calendarServiceInstance().saveCalendar(cal.getCalendarOwner(), calObj, calObj.getCalType(), false);
        return Response.ok().cacheControl(cc).build();
      }
      
      //
      return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
    } catch (Exception e) {
      e.printStackTrace();
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Deletes the calendar if:
   * the authenticated user is the owner of the calendar
   * for group calendars, the authenticated user has edit rights on the calendar
   * If it is a shared calendar the calendar is not shared anymore (but the original calndar is not deleted)
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/calendars/{id}")
  public Response deleteCalendarById(@PathParam("id") String id) {
    try {
      Calendar cal = calendarServiceInstance().getCalendarById(id);
      if(cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();

      cal.setCalType(calendarServiceInstance().getTypeOfCalendar(currentUserId(), id));
      if (Utils.isCalendarEditable(currentUserId(), cal) || cal.getCalType() == Calendar.TYPE_SHARED) {
        switch (cal.getCalType()) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().removeUserCalendar(cal.getCalendarOwner(), id);
          return Response.ok().build();
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().removePublicCalendar(id);
          return Response.ok().build();
        case Calendar.TYPE_SHARED:
          if (this.hasViewCalendarPermission(cal, currentUserId())) {
            calendarServiceInstance().removeSharedCalendar(currentUserId(),id);
            return Response.ok().build();
          }
        default:
          break;
        }
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }
  
  /**
   * Returns the iCalendar export if:
   * the calendar is public
   * the authenticated user is the owner of the calendar
   * the authenticated user belongs to the group of the calendar
   * the calendar has been shared with the authenticated user or with a group of the authenticated user
   */
  @GET
  @RolesAllowed("users")
  @Path("/calendars/{id}/ics")
  @Produces(TEXT_ICS)
  public Response exportCalendarToIcs(@PathParam("id") String id) {
    try {
      Calendar cal = calendarServiceInstance().getCalendarById(id);
      
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      
      if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId())) {
        int type = calendarServiceInstance().getTypeOfCalendar(currentUserId(),id);
        String username = currentUserId();
        if (type == -1) {
          //this is a workaround
          //calendarService can't find type of calendar correctly 
          type = Calendar.TYPE_PRIVATE;
          username = cal.getCalendarOwner();
        }
        
        CalendarImportExport iCalExport = calendarServiceInstance().getCalendarImportExports(CalendarService.ICALENDAR);
        ArrayList<String> calIds = new ArrayList<String>();
        calIds.add(id);
        OutputStream out = iCalExport.exportCalendar(username, calIds, String.valueOf(type), Utils.UNLIMITED);
        InputStream in = new ByteArrayInputStream(out.toString().getBytes());
        return Response.ok(in, TEXT_ICS_TYPE)
            .header("Cache-Control", "private max-age=600, s-maxage=120").
            header("Content-Disposition", "attachment;filename=\"" + cal.getName() + Utils.ICS_EXT).cacheControl(cc).build();
      }
      
      //
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();

  }

  /**
   * Returns an event in the list when :
   * the calendar of the event is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/events/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEvents(@QueryParam("startTime") String start,
                                           @QueryParam("endTime") String end,
                                           @QueryParam("category") String category,
                                           @QueryParam("offset") int offset,
                                           @QueryParam("limit") int limit, 
                                           @QueryParam("returnSize") boolean returnSize,
                                           @QueryParam("fields") String fields,
                                           @QueryParam("jsonp") String jsonp,
                                           @Context UriInfo uri) throws Exception {
    limit = parseLimit(limit);
    String username = currentUserId();
    
    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    
    //find all viewable calendars of user: private, group, share calendars
    String[] calIds = findViewableCalendars(username);
    EventQuery eventQuery = buildEventQuery(start, end, category, calIds, null, username, CalendarEvent.TYPE_EVENT, returnSize);
    ListAccess<CalendarEvent> events = evtDAO.findEventsByQuery(eventQuery);
    
    long fullSize = returnSize ? events.getSize() : -1;
    List data = new LinkedList();
    for (CalendarEvent event : events.load(offset, limit)) {
      data.add(extractObject(new EventResource(event), fields));
    }
    CollectionResource evData = new CollectionResource(data, fullSize);    
    evData.setOffset(offset);
    evData.setLimit(limit);
    
    ResponseBuilder response = null;
    
    if (jsonp != null) {
      JsonValue value = new JsonGeneratorImpl().createJsonObject(evData);
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(value).append(");");
      response = Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc);
    } else {
      response = Response.ok(evData, MediaType.APPLICATION_JSON).cacheControl(cc);
    }
    
    if (returnSize) {
      response.header(HEADER_LINK, buildFullUrl(uri, offset, limit, fullSize));
    }
    
    //
    return response.build();
  }

  /**
   * Returns the event if:
   * the calendar of the event is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @GET
  @RolesAllowed("users")
  @Path("/events/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventById(@PathParam("id") String id,
                                                 @QueryParam("fields") String fields,
                                                 @QueryParam("expand") String expand,
                                                 @QueryParam("jsonp") String jsonp) {
    try {
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if(ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      boolean inParticipant = false;
      String[] participant = ev.getParticipant();
      if (participant != null) {
        Arrays.sort(participant);
        if (Arrays.binarySearch(participant, currentUserId()) > -1) inParticipant = true;
      }
     
      if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId()) || inParticipant) {
        Object resource = null;
        if ("calendar".equals(expand)) {
          resource = extractObject(new EventResource<Calendar>(ev).setCal(cal), fields);
        } else {
          resource = extractObject(new EventResource<String>(ev), fields);
        }
        
        ResponseBuilder response = null;
        
        if (jsonp != null) {
          String json = null;
          if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
          else {
            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            json = generatorImpl.createJsonObject(resource).toString();
          }
          StringBuilder sb = new StringBuilder(jsonp);
          sb.append('(').append(json).append(");");
          response = Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc);
        } else {
          response = Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc);
        }
        
        //
        return response.build();
      } 

      //
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Updates the event if:
   * the authenticated user is the owner of the calendar of the event
   * for group calendars, the authenticated user has edit rights on the calendar
   * the calendar of the event has been shared with the authenticated user, with modification rights
   * the calendar of the event has been shared with a group of the authenticated user, with modification rights
   */
  @PUT
  @RolesAllowed("users")
  @Path("/events/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateEventById(@PathParam("id") String id,  CalendarEvent evObject) {
    try {
      CalendarEvent old = calendarServiceInstance().getEventById(id);
      if(old == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();

      Calendar cal = calendarServiceInstance().getCalendarById(old.getCalendarId());
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        evObject.setId(old.getId());
        int calType = -1;
        try {
          calType = Integer.parseInt(evObject.getCalType());
        }catch (NumberFormatException e) {
          calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), evObject.getCalendarId());
        } 
        switch (calType) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().saveUserEvent(currentUserId(), evObject.getCalendarId(), evObject, false);
          break;
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().savePublicEvent(evObject.getCalendarId(), evObject, false);
          break;
        case Calendar.TYPE_SHARED:
          calendarServiceInstance().saveEventToSharedCalendar(currentUserId(), evObject.getCalendarId(),evObject,false);
          break;

        default:
          break;
        }
        return Response.ok().cacheControl(cc).build();
      }
      
      //
      return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Deletes the event if:
   * the authenticated user is the owner of the calendar of the event
   * for group calendars, the authenticated user has edit rights on the calendar
   * the calendar of the event has been shared with the authenticated user, with modification rights
   * the calendar of the event has been shared with a group of the authenticated user, with modification rights
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/events/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteEventById(@PathParam("id") String id) {
    try {
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if(ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        int calType = Calendar.TYPE_ALL;
        try {
          calType = Integer.parseInt(ev.getCalType());
        } catch (NumberFormatException e) {
          calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), ev.getCalendarId());
        }
        switch (calType) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().removeUserEvent(currentUserId(), ev.getCalendarId(), id);
          break;
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().removePublicEvent(ev.getCalendarId(),id);
          break;
        case Calendar.TYPE_SHARED:
          calendarServiceInstance().removeSharedEvent(currentUserId(), ev.getCalendarId(), id);
          break;

        default:
          break;
        }
        return Response.ok().cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *  Returns attachments if:
   * the calendar of the event is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/events/{id}/attachments")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAttachmentsFromEvent(@PathParam("id") String id, 
                                                                      @QueryParam("offset") int offset, 
                                                                      @QueryParam("limit") int limit,
                                                                      @QueryParam("fields") String fields,
                                                                      @QueryParam("jsonp") String jsonp,
                                                                      @Context UriInfo uriInfo){
    try {
      limit = parseLimit(limit);
      
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if(ev == null || ev.getAttachment() == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      if(ev != null && ev.getAttachment() != null) {
        Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
        boolean inParticipant = false;
        if (ev.getParticipant() != null) {
          String[] participant = ev.getParticipant();
          Arrays.sort(participant);
          int i = Arrays.binarySearch(participant, currentUserId());
          if (i > -1) inParticipant = true;
        }
      
        if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId()) || inParticipant) {
          Iterator<Attachment> it = ev.getAttachment().iterator();
          List attResource = new ArrayList();
          Utils.skip(it, offset);
          int counter = 0;
          while (it.hasNext()) {
            Attachment a = it.next();
            attResource.add(extractObject(new AttachmentResource(a), fields));
            if(++counter == limit) break;
          }
          CollectionResource evData = new CollectionResource(attResource, ev.getAttachment().size());
          evData.setOffset(offset);
          evData.setLimit(limit);
          
          if (jsonp != null) {
            JsonValue value = new JsonGeneratorImpl().createJsonObject(evData);
            StringBuilder sb = new StringBuilder(jsonp);
            sb.append('(').append(value).append(");");
            return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, evData.getFullSize())).build();
          }
          
          //
          return Response.ok(evData, MediaType.APPLICATION_JSON).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, evData.getFullSize())).cacheControl(cc).build();
        }
        
        //
        return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Creates the attachment if:
   * the authenticated user is the owner of the calendar of the event
   * for group calendars, the authenticated user has edit rights on the calendar
   * the calendar of the event has been shared with the authenticated user, with modification rights
   * the calendar of the event has been shared with a group of the authenticated user, with modification rights
   */
  @POST
  @RolesAllowed("users")
  @Path("/events/{id}/attachments")
  @Consumes("multipart/*")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createAttachmentForEvent(@PathParam("id") String id, Iterator<FileItem> iter) {
    try {
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if (ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();

      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());

      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        int calType = Calendar.TYPE_ALL;
        List<AttachmentResource> rs = new LinkedList<AttachmentResource>();
        List<Attachment> attachment = new ArrayList<Attachment>();
        try {
          calType = Integer.parseInt(ev.getCalType());
        } catch (NumberFormatException e) {
          calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), ev.getCalendarId());
        }

        attachment.addAll(ev.getAttachment());
        while (iter.hasNext()) {
          FileItem file  = iter.next();
          String fileName = file.getName();
          if(fileName != null) {
            String mimeType = new MimeTypeResolver().getMimeType(fileName.toLowerCase());
            Attachment at = new Attachment();
            at.setMimeType(mimeType);
            at.setSize(file.getSize());
            at.setName(file.getName());
            at.setInputStream(file.getInputStream());
            attachment.add(at);
            rs.add(new AttachmentResource(at));
          }
        }
        ev.setAttachment(attachment);

        switch (calType) {
          case Calendar.TYPE_PRIVATE:
            calendarServiceInstance().saveUserEvent(currentUserId(), ev.getCalendarId(), ev, false);
            break;
          case Calendar.TYPE_PUBLIC:
            calendarServiceInstance().savePublicEvent(ev.getCalendarId(), ev, false);
            break;
          case Calendar.TYPE_SHARED:
            calendarServiceInstance().saveEventToSharedCalendar(currentUserId(), ev.getCalendarId(), ev, false);
            break;
          default:
            break;
        }
        
        CollectionResource<?> evData = new CollectionResource<AttachmentResource>(rs, rs.size());
        return Response.ok(evData, MediaType.APPLICATION_JSON).status(HTTPStatus.CREATED).cacheControl(cc).build();
      }

      //
      return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Returns an event in the list when:
   * the calendar is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/calendars/{id}/events")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventsByCalendar(@PathParam("id") String id,                                       
                                                             @QueryParam("startTime") String start,
                                                             @QueryParam("endTime") String end,
                                                             @QueryParam("category") String category,
                                                             @QueryParam("offset") int offset,
                                                             @QueryParam("limit") int limit,
                                                             @QueryParam("fields") String fields,
                                                             @QueryParam("jsonp") String jsonp,
                                                             @QueryParam("returnSize") boolean returnSize,
                                                             @Context UriInfo uri) throws Exception {
    limit = parseLimit(limit);
    String username = currentUserId();
    
    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    
    long fullSize = returnSize ? 0 : -1;
    List data = new LinkedList();
    Calendar calendar = service.getCalendarById(id);
    
    if (calendar != null) {
      String participant = null;
      if (calendar.getPublicUrl() == null && !hasViewCalendarPermission(calendar, username)) {
        participant = username;
      }

      EventQuery eventQuery = buildEventQuery(start, end, category, null, calendar.getId(), participant, CalendarEvent.TYPE_EVENT, returnSize);
      ListAccess<CalendarEvent> events = evtDAO.findEventsByQuery(eventQuery);
      //
      if (returnSize) {
        fullSize = events.getSize();
      }
      for (CalendarEvent event : events.load(offset, limit)) {
        data.add(extractObject(new EventResource(event), fields));
      }        
    } else {
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    }
    //
    CollectionResource evData = new CollectionResource(data, fullSize);
    evData.setOffset(offset);
    evData.setLimit(limit);
    
    ResponseBuilder response = null;
    if (jsonp != null) {
      JsonValue json = new JsonGeneratorImpl().createJsonObject(evData);
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(json).append(");");
      response = Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc);
    } else {
     response = Response.ok(evData, MediaType.APPLICATION_JSON).cacheControl(cc);
    }
    
    if (returnSize) {
      response.header(HEADER_LINK, buildFullUrl(uri, offset, limit, fullSize));
    }
    
    //
    return response.build();
  }

  /**
   * Creates the event only if:
   * the authenticated user is the owner of the calendar
   * for group calendars, the authenticated user has edit rights on the calendar
   * the calendar has been shared with the authenticated user, with modification rights
   * the calendar has been shared with a group of the authenticated user, with modification rights
   */
  @POST
  @RolesAllowed("users")
  @Path("/calendars/{id}/events")
  public Response createEventForCalendar(@PathParam("id") String id, CalendarEvent evObject) {
    try {
      Calendar cal = calendarServiceInstance().getCalendarById(id);
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        int calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), id);      
        switch (calType) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().saveUserEvent(currentUserId(), id, evObject, true);
          break;
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().savePublicEvent(id, evObject, true);
          break;
        case Calendar.TYPE_SHARED:
          calendarServiceInstance().saveEventToSharedCalendar(currentUserId(), id, evObject, true);
          break;
        default:
          break;
        }
        EventResource<?> rs = new EventResource<Serializable>(evObject);
        return Response.ok(rs, MediaType.APPLICATION_JSON).status(HTTPStatus.CREATED).cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();

  }

  /**
   * An occurrence is represented as an Event resource type, without the recurrence information but with 
   * the recurrenceId. Returns an occurrence in the list when :
   * the calendar of the event is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @GET
  @RolesAllowed("users")
  @Path("/events/{id}/occurrences")
  public Response getOccurrencesFromEvent(@PathParam("id") String id,
                                                                     @QueryParam("offset") int offset,
                                                                     @QueryParam("limit") int limit,
                                                                     @QueryParam("start") String start,
                                                                     @QueryParam("end") String end,
                                                                     @QueryParam("fields") String fields,
                                                                     @QueryParam("jsonp") String jsonp,
                                                                     @Context UriInfo uriInfo) {
    try {
      limit = parseLimit(limit);
      java.util.Calendar[] dates = parseDate(start, end);
      
      CalendarEvent recurEvent = calendarServiceInstance().getEventById(id);
      if (recurEvent == null)  return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      TimeZone tz = java.util.Calendar.getInstance().getTimeZone();
      String timeZone = tz.getID();
      
      Map<String,CalendarEvent> occMap = calendarServiceInstance().getOccurrenceEvents(recurEvent, dates[0], dates[1], timeZone);
      if(occMap == null || occMap.isEmpty()) {
        return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      }
      
      Calendar cal = calendarServiceInstance().getCalendarById(recurEvent.getCalendarId());
      boolean inParticipant = false;
      if (recurEvent.getParticipant() != null) {
        String[] participant = recurEvent.getParticipant();
        Arrays.sort(participant);
        int i = Arrays.binarySearch(participant, currentUserId());
        if (i > -1) inParticipant = true;
      }
    
      if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId()) || inParticipant) {
        Collection data = new ArrayList();
        Iterator<CalendarEvent> evIter = occMap.values().iterator();
        Utils.skip(evIter, offset);
        int counter =0;
        while (evIter.hasNext()) {
          data.add(extractObject(new EventResource(evIter.next()), fields));
          if(++counter == limit) break;
        }
        
        CollectionResource evData = new CollectionResource(data, occMap.values().size());
        
        if (jsonp != null) {
          JsonValue json = new JsonGeneratorImpl().createJsonObject(evData);
          StringBuilder sb = new StringBuilder(jsonp);
          sb.append('(').append(json).append(");");
          return Response.ok(sb.toString(), new MediaType("text", "javascript")).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, evData.getFullSize())).cacheControl(cc).build();
        }
        
        //
        return Response.ok(evData, MediaType.APPLICATION_JSON).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, evData.getFullSize())).cacheControl(cc).build();
      }
      
      //
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Returns an task in the list when :
   * the calendar of the event is public
   * the authenticated user is the owner of the calendar of the event
   * the authenticated user belongs to the group of the calendar of the event
   * the authenticated user is a participant of the event
   * the calendar of the event has been shared with the authenticated user or with a group of the authenticated user
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/tasks/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTasks(@QueryParam("startTime") String start,
                                          @QueryParam("endTime") String end,
                                          @QueryParam("category") String category,
                                          @QueryParam("offset") int offset,
                                          @QueryParam("limit") int limit,
                                          @QueryParam("fields") String fields,
                                          @QueryParam("jsonp") String jsonp,
                                          @QueryParam("returnSize") boolean returnSize,
                                          @Context UriInfo uriInfo) throws Exception {
    limit = parseLimit(limit);
    String username = currentUserId();
    
    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    
    //find all viewable calendars of user: private, group, share calendars
    String[] calIds = findViewableCalendars(username);
    EventQuery eventQuery = buildEventQuery(start, end, category, calIds, null, username, CalendarEvent.TYPE_TASK, returnSize);
    ListAccess<CalendarEvent> events = evtDAO.findEventsByQuery(eventQuery);
    
    long fullSize = returnSize ? events.getSize() : -1;
    List data = new LinkedList();
    for (CalendarEvent event : events.load(offset, limit)) {
      data.add(extractObject(new TaskResource(event), fields));
    }
    CollectionResource evData = new CollectionResource(data, fullSize);    
    evData.setOffset(offset);
    evData.setLimit(limit);

    
    ResponseBuilder response = null;
    if (jsonp != null) {
      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(evData);
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(json).append(");");
      response = Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc);
    } else {
      response = Response.ok(evData, MediaType.APPLICATION_JSON).cacheControl(cc);
    }
    
    if (returnSize) {
      response.header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, fullSize));
    }
    
    //
    return response.build();
  }

  /**
   *
   * Returns the task if: same rules as /events/{id}
   * {@link #getEventById(String, String, String, String)}
   */
  @GET
  @RolesAllowed("users")
  @Path("/tasks/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTaskById(@PathParam("id") String id,
                                                @QueryParam("fields") String fields,
                                                @QueryParam("expand") String expand,
                                                @QueryParam("jsonp") String jsonp) {
    try {
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if(ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      boolean inParticipant = false;
      if (ev.getParticipant() != null) {
        String[] participant = ev.getParticipant();
        Arrays.sort(participant);
        if (Arrays.binarySearch(participant, currentUserId()) > -1) inParticipant = true;;
      }
    
      if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId()) || inParticipant) {
        
        Object resource = null;

        if ("calendar".equals(expand)) {
          resource = extractObject(new TaskResource<Calendar>(ev).setCalendar(cal), fields);
        } else {
          resource = extractObject(new TaskResource<String>(ev), fields);
        }
        
        if (jsonp != null) {
          String json = null;
          if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
          else {
            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            json = generatorImpl.createJsonObject(resource).toString();
          }
          StringBuilder sb = new StringBuilder(jsonp);
          sb.append('(').append(json).append(");");
          return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
        }
        
        //
        return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
      }
      
      //
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *
   * Updates the task if: same rules as /events/{id}
   * {@link #updateEventById(String, CalendarEvent)}
   */
  @PUT
  @RolesAllowed("users")
  @Path("/tasks/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateTaskById(@PathParam("id") String id, CalendarEvent evObject) {
    try {
      CalendarEvent old = calendarServiceInstance().getEventById(id);
      if (old == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(old.getCalendarId());
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        evObject.setId(old.getId());
        int calType = -1;
        try {
          calType = Integer.parseInt(evObject.getCalType());
        }catch (NumberFormatException e) {
          calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), evObject.getCalendarId());
        } 
        switch (calType) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().saveUserEvent(currentUserId(), evObject.getCalendarId(), evObject, false);
          break;
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().savePublicEvent(evObject.getCalendarId(), evObject, false);
          break;
        case Calendar.TYPE_SHARED:
          calendarServiceInstance().saveEventToSharedCalendar(currentUserId(), evObject.getCalendarId(),evObject,false);
          break;

        default:
          break;
        }
        return Response.ok().cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *  Deletes the task if: same rules as /events/{id}
   *  {@link #deleteEventById(String)} 
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/tasks/{id}")
  public Response deleteTaskById(@PathParam("id") String id) {
    try {
      CalendarEvent ev = calendarServiceInstance().getEventById(id);
      if (ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        int calType = Calendar.TYPE_ALL;
        try {
          calType = Integer.parseInt(ev.getCalType());
        } catch (NumberFormatException e) {
          calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), ev.getCalendarId());
        }
        switch (calType) {
        case Calendar.TYPE_PRIVATE:
          calendarServiceInstance().removeUserEvent(currentUserId(), ev.getCalendarId(), id);
          break;
        case Calendar.TYPE_PUBLIC:
          calendarServiceInstance().removePublicEvent(ev.getCalendarId(),id);
          break;
        case Calendar.TYPE_SHARED:
          calendarServiceInstance().removeSharedEvent(currentUserId(), ev.getCalendarId(), id);
          break;

        default:
          break;
        }
        return Response.ok().cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Returns the attachment if: same rules as /events/{id}
   * {@link #getEventById(String, String, String, String)}
   */
  @GET
  @RolesAllowed("users")
  @Path("/attachments/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAttachmentById(@PathParam("id") String id, @QueryParam("fields") String fields, @QueryParam("jsonp") String jsonp) {
    try {
      CalendarEvent ev = this.findEventAttachment(id);
      if (ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Attachment att = calendarServiceInstance().getAttachmentById(id);
      if(att == null)  return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      boolean inParticipant = false;
      if (ev.getParticipant() != null) {
        String[] participant = ev.getParticipant();
        Arrays.sort(participant);
        int i = Arrays.binarySearch(participant, currentUserId());
        if (i > -1) inParticipant = true;
      }
    
      if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, currentUserId()) || inParticipant) {
          
        AttachmentResource evData = new AttachmentResource(att);
        Object resource = extractObject(evData, fields);
        if (jsonp != null) {
          String json = null;
          if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
          else {
            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            json = generatorImpl.createJsonObject(resource).toString();
          }
          StringBuilder sb = new StringBuilder(jsonp);
          sb.append('(').append(json).append(");");
          return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
        }

        //
        return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
      }

      //
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Deletes the attachment if: same rules as /events/{id}
   * {@link #deleteEventById(String)}
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/attachments/{id}")
  public Response deleteAttachmentById(@PathParam("id") String id){
    try {
      CalendarEvent ev = this.findEventAttachment(id);
      if (ev == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Calendar cal = calendarServiceInstance().getCalendarById(ev.getCalendarId());
      if (cal == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      if (Utils.isCalendarEditable(currentUserId(), cal)) {
        calendarServiceInstance().removeAttachmentById(id);
        return Response.ok().cacheControl(cc).build();
      } 
      return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *  Returns the categories if an user is authenticated (the common categories + the personal categories)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/categories")
  public Response getEventCategories(@QueryParam("offset") int offset,
                                                          @QueryParam("limit") int limit,
                                                          @QueryParam("fields") String fields,
                                                          @QueryParam("jsonp") String jsonp,
                                                          @Context UriInfo uriInfo) {
    limit = parseLimit(limit);

    try {
      List<EventCategory> ecData = calendarServiceInstance().getEventCategories(currentUserId(), offset, limit);
      if(ecData == null || ecData.isEmpty()) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      Collection data = new ArrayList();
      
      for(EventCategory ec:ecData) {
        data.add(extractObject(new CategoryResource(ec), fields));
      }

      CollectionResource resource = new CollectionResource(data, ecData.size());
      resource.setOffset(offset);
      resource.setLimit(limit);
      
      if (jsonp != null) {
        JsonValue json = new JsonGeneratorImpl().createJsonObject(resource);
        StringBuilder sb = new StringBuilder(jsonp);
        sb.append('(').append(json).append(");");
        return Response.ok(sb.toString(), new MediaType("text", "javascript")).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, resource.getFullSize())).cacheControl(cc).build();
      }
      
      //
      return Response.ok(resource, MediaType.APPLICATION_JSON).header(HEADER_LINK, buildFullUrl(uriInfo, offset, limit, resource.getFullSize())).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }
  
  @GET
  @RolesAllowed("users")
  @Path("/categories/{id}")
  public Response getEventCategoryById(@PathParam("id") String id, @QueryParam("fields") String fields, @QueryParam("jsonp") String jsonp) {
    try {
      List<EventCategory> data = calendarServiceInstance().getEventCategories(currentUserId());
      if(data == null || data.isEmpty()) {
        return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      }
      EventCategory category = null;
      for (int i = 0; i < data.size(); i++) {
        if(id.equals(data.get(i).getId())) {
          category = data.get(i);
          break;
        }
      }
      
      if(category == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      CategoryResource categoryR = new CategoryResource(category);
      Object resource = extractObject(categoryR, fields);
      if (jsonp != null) {
        String json = null;
        if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
        else {
          JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
          json = generatorImpl.createJsonObject(resource).toString();
        }
        StringBuilder sb = new StringBuilder(jsonp);
        sb.append('(').append(json).append(");");
        return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
      }
      
      //
      return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *
   *  Gets a feed with the given id    
   *  Returns the feed if the authenticated user is the owner of the feed
   */
  @GET
  @RolesAllowed("users")
  @Path("/feeds/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFeedById(@PathParam("id") String id,
                                                @QueryParam("fields") String fields,
                                                @QueryParam("expand") String expand,
                                                @QueryParam("jsonp") String jsonp) {
    try {
      FeedData feed = null;
      for (FeedData feedData : calendarServiceInstance().getFeeds(currentUserId())) {
        if (feedData.getTitle().equals(id)) {
          feed = feedData;
          break;
        }        
      }
      
      if(feed == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed syndFeed = input.build(new XmlReader(new ByteArrayInputStream(feed.getContent())));
      List<SyndEntry> entries = new ArrayList<SyndEntry>(syndFeed.getEntries());
      List<String> calIds = new ArrayList<String>();
      for (SyndEntry entry : entries) {
        String calendarId = entry.getLink().substring(entry.getLink().lastIndexOf("/")+1) ;
        calIds.add(calendarId);
      }
      
      //split expand parameter with format 'expand=members(offset:10,limit:10)'
      String fieldName = null;
      int offset = -1;
      int limit = -1;
      if (expand != null) {
        int i =expand.indexOf('('); 
        if (i > 0) {
          fieldName = expand.substring(0, i);
          offset = Integer.parseInt(expand.substring(expand.indexOf("offset:") + "offset:".length(), expand.indexOf(',')));
          limit = Integer.parseInt(expand.substring(expand.indexOf("limit:") + "limit:".length(), expand.indexOf(')')));
          limit = this.parseLimit(limit);
        } else {
          fieldName = expand;
        }
      }
      
      Object resource = null;
      if ("calendars".equals(fieldName)) {
        List<Calendar> calendars = new ArrayList<Calendar>();
        for(String calId : Utils.subList(calIds, offset, limit)) {
          calendars.add(calendarServiceInstance().getCalendarById(calId));
        }
        resource =  extractObject(new FeedResource<Calendar>(feed, calIds).setCalendars(calendars.toArray(new Calendar[calendars.size()])), fields);
      } else {
        resource = extractObject(new FeedResource<String>(feed, calIds), fields);
      }
      
      if (jsonp != null) {
        String json = null;
        if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
        else {
          JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
          json = generatorImpl.createJsonObject(resource).toString();
        }
        StringBuilder sb = new StringBuilder(jsonp);
        sb.append('(').append(json).append(");");
        return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
      }
      
      //
      return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Updates a feed with the given id   
   * Updates the feed if the authenticated user is the owner of the feed 
   */
  @PUT
  @RolesAllowed("users")
  @Path("/feeds/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFeedById(@PathParam("id") String id, RssData feedObj) {
    try {
      feedObj.setTitle(id);
      FeedData feed = null;
      for (FeedData feedData : calendarServiceInstance().getFeeds(currentUserId())) {
        if (feedData.getTitle().equals(id)) {
          feed = feedData;
          break;
        }        
      }
      
      if (feed == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed syndFeed = input.build(new XmlReader(new ByteArrayInputStream(feed.getContent())));
      List<SyndEntry> entries = new ArrayList<SyndEntry>(syndFeed.getEntries());
      List<String> calIds = new ArrayList<String>();
      LinkedHashMap<String, Calendar> calendars = new LinkedHashMap<String, Calendar>();
      for (SyndEntry entry : entries) {
        String calendarId = entry.getLink().substring(entry.getLink().lastIndexOf("/")+1) ;
        Calendar calendar = calendarServiceInstance().getCalendarById(calendarId);
        int calType = calendarServiceInstance().getTypeOfCalendar(currentUserId(), calendarId);
        switch (calType) {
          case Calendar.TYPE_PRIVATE:
            calendars.put(Calendar.TYPE_PRIVATE + Utils.COLON + calendarId, calendar);
            break;
          case Calendar.TYPE_PUBLIC:
            calendars.put(Calendar.TYPE_PUBLIC + Utils.COLON + calendarId, calendar);
            break;
          case Calendar.TYPE_SHARED:
            calendars.put(Calendar.TYPE_SHARED + Utils.COLON + calendarId, calendar);
            break;
          default:
            break;
        }
        calIds.add(calendarId);
      }
      calendarServiceInstance().removeFeedData(currentUserId(),id);
      
      calendarServiceInstance().generateRss(currentUserId(), calendars, feedObj);
      
      return Response.ok().cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Deletes a feed with the given id    
   * Deletes the feed if the authenticated user is the owner of the feed 
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/feeds/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteFeedById(@PathParam("id") String id) {
    try {
      calendarServiceInstance().removeFeedData(currentUserId(),id);
      return Response.ok().cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   *
   *  Gets the RSS stream of the feed with the given id
   *  
   *  Returns the RSS stream if:
   *   - the calendar is public
   *   - the authenticated user is the owner of the calendar
   *   - the authenticated user belongs to the group of the calendar
   *   - the calendar has been shared with the authenticated user or with a group of the authenticated user
   */
  @GET
  @RolesAllowed("users")
  @Path("/feeds/{id}/rss")
  @Produces(MediaType.APPLICATION_XML)
  public Response getRssFromFeed(@PathParam("id") String id, @Context UriInfo uri) {
    try {
      String username = currentUserId();
      String feedname = id;
      FeedData feed = null;
      for (FeedData feedData : calendarServiceInstance().getFeeds(username)) {
        if (feedData.getTitle().equals(feedname)) {
          feed = feedData;
          break;
        }        
      }
      
      if (feed == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed syndFeed = input.build(new XmlReader(new ByteArrayInputStream(feed.getContent())));
      List<SyndEntry> entries = new ArrayList<SyndEntry>(syndFeed.getEntries());
      List<CalendarEvent> events = new ArrayList<CalendarEvent>();
      List<Calendar> calendars = new ArrayList<Calendar>();
      for (SyndEntry entry : entries) {
        String calendarId = entry.getLink().substring(entry.getLink().lastIndexOf("/")+1) ;
        calendars.add(calendarServiceInstance().getCalendarById(calendarId));
      }
      
      for (Calendar cal : calendars) {
        if (cal.getPublicUrl() != null || this.hasViewCalendarPermission(cal, username)) {
          int calType = calendarServiceInstance().getTypeOfCalendar(username, cal.getId());
          switch (calType) {
            case Calendar.TYPE_PRIVATE:
              events.addAll(calendarServiceInstance().getUserEventByCalendar(username, Arrays.asList(cal.getId())));
              break;
            case Calendar.TYPE_SHARED:
              events.addAll(calendarServiceInstance().getSharedEventByCalendars(username, Arrays.asList(cal.getId())));
              break;
            case Calendar.TYPE_PUBLIC:
              EventQuery eventQuery = new EventQuery();
              eventQuery.setCalendarId(new String[] { cal.getId() });
              events.addAll(calendarServiceInstance().getPublicEvents(eventQuery));
              break;
            default:
              break;
          }
        }
      }

      if(events.size() == 0) {
        return Response.status(HTTPStatus.NOT_FOUND).entity("Feed " + feedname + "is removed").cacheControl(cc).build();
      } 
      return Response.ok(makeFeed(username, events, feed, uri), MediaType.APPLICATION_XML).cacheControl(cc).build();
    } catch (Exception e) {
      if(log.isDebugEnabled()) log.debug(e.getMessage());
    }
    return Response.status(HTTPStatus.UNAVAILABLE).cacheControl(cc).build();
  }

  /**
   * Returns an invitation in the list if:<br/>
   * the authenticated user is the participant of the invitation<br/>
   * the authenticated user has edit rights on the calendar of the event of the invitation<br/>
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/invitations")
  public Response getInvitations(@QueryParam("startTime") String start,
                                                  @QueryParam("endTime") String end,
                                                  @QueryParam("offset") int offset,
                                                  @QueryParam("limit") int limit,
                                                  @QueryParam("fields") String fields,
                                                  @QueryParam("jsonp") String jsonp,
                                                  @QueryParam("returnSize") boolean returnSize,
                                                  @Context UriInfo uri) throws Exception {
    java.util.Calendar[] dates = parseDate(start, end);
    limit = parseLimit(limit);
    //uInvites and eInvites currently load duplicated results
    MultiListAccess invitations = new MultiListAccess(true);
    String username = currentUserId();

    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    //Find all invitations that user is participant
    EventQuery uQuery = new EventQuery();
    uQuery.setFromDate(dates[0]);
    uQuery.setToDate(dates[1]);
    uQuery.setParticipants(new String[] {username});
    uQuery.setReturnSize(returnSize);
    ListAccess<Invitation> uInvites = evtDAO.findInvitationsByQuery(uQuery);
    //
    invitations.add((LinkableListAccess<Invitation>)uInvites);

    //Find invitations of editable calendars    
    String[] calendarIds = findEditableCalendars(username);
    if (calendarIds.length > 0) {
      EventQuery eQuery = new EventQuery();
      eQuery.setFromDate(dates[0]);
      eQuery.setToDate(dates[1]);
      eQuery.setCalendarId(calendarIds);
      eQuery.setReturnSize(returnSize);
      ListAccess<Invitation> eInvites = evtDAO.findInvitationsByQuery(eQuery);
      //
      invitations.add((LinkableListAccess<Invitation>)eInvites);
    }
    
    long fullSize = returnSize ? invitations.getSize() : -1;
    List data = new LinkedList();
    for (Object invitation : invitations.load(offset, limit)) {
      data.add(extractObject(new InvitationResource((Invitation)invitation), fields));
    }
    CollectionResource evData = new CollectionResource(data, fullSize);    
    evData.setOffset(offset);
    evData.setLimit(limit);

    ResponseBuilder response = null;
    if (jsonp != null) {
      JsonValue json = new JsonGeneratorImpl().createJsonObject(evData);
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(json).append(");");
      response =  Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc);
    } else {
      response = Response.ok(evData, MediaType.APPLICATION_JSON).cacheControl(cc);
    }
    
    if (returnSize) {
      response.header(HEADER_LINK, buildFullUrl(uri, offset, limit, fullSize));
    }
    
    //
    return response.build();
  }

  /**
   * Returns the invitation if:<br/>
   * the authenticated user is the participant of the invitation<br/>
   * the authenticated user has edit rights on the calendar of the event of the invitation<br/>
   */
  @GET
  @RolesAllowed("users")
  @Path("/invitations/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getInvitationById(@PathParam("id") String id, @QueryParam("fields") String fields, @QueryParam("jsonp") String jsonp) throws Exception {
    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    String username = currentUserId();
    
    Invitation invitation = evtDAO.getInvitationById(id);
    if (invitation == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    
    //dont return invitation if user is not participant and not have edit permission
    if (!username.equals(invitation.getParticipant())) {
      CalendarEvent event = service.getEventById(invitation.getEventId());
      Calendar calendar = service.getCalendarById(event.getCalendarId());

      if (!Utils.isCalendarEditable(username, calendar)) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    }

    InvitationResource iv = new InvitationResource(invitation);
    Object resource = extractObject(iv, fields);
    if (jsonp != null) {
      String json = null;
      if (resource instanceof Map) json = new JSONObject((Map)resource).toString();
      else {
        JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
        json = generatorImpl.createJsonObject(resource).toString();
      }
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(json).append(");");
      return Response.ok(sb.toString(), new MediaType("text", "javascript")).cacheControl(cc).build();
    }

    //
    return Response.ok(resource, MediaType.APPLICATION_JSON).cacheControl(cc).build();
  }

  /**
   *  Updates the invitation if the authenticated user is the participant of the invitation
   */
  @PUT
  @RolesAllowed("users")
  @Path("/invitations/{id}")
  public Response updateInvitationById(@PathParam("id") String id, @QueryParam("status") String status) {
    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    String username = currentUserId();
    
    Invitation invitation = evtDAO.getInvitationById(id);
    if (invitation != null) {
      //Update only if user is participant
      if (invitation.getParticipant().equals(username)) {
        evtDAO.updateInvitation(id, status);
        return Response.ok().cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
    } else {
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    }    
  }

  /**
   * Deletes the invitation if the authenticated user has edit rights on the calendar of the event of the invitation
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/invitations/{id}")
  public Response deleteInvitationById(@PathParam("id") String invitationId) throws Exception {
    CalendarService calService = calendarServiceInstance();
    EventDAO evtDAO = calService.getEventDAO();
    String username = currentUserId();

    Invitation invitation = evtDAO.getInvitationById(invitationId);
    if (invitation == null) return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();

    CalendarEvent event = calService.getEventById(invitation.getEventId());
    Calendar calendar = calService.getCalendarById(event.getCalendarId());

    if (Utils.isCalendarEditable(username, calendar)) {
      evtDAO.removeInvitation(invitationId);
      return Response.ok().cacheControl(cc).build();
    } else {
      return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
    }
  }

  /**
   * Returns an invitation in the list when:
   * the authenticated user is the participant of the invitation
   * the authenticated user has edit rights on the calendar of the event of the invitation
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @RolesAllowed("users")
  @Path("/events/{id}/invitations/")
  public Response getInvitationsFromEvent(@PathParam("id") String eventId,
                                                                  @QueryParam("offset") int offset, 
                                                                  @QueryParam("limit") int limit,
                                                                  @QueryParam("status") String status, 
                                                                  @QueryParam("fields") String fields,
                                                                  @QueryParam("jsonp") String jsonp,
                                                                  @Context UriInfo uri) throws Exception {
    limit = parseLimit(limit);
    CalendarService calService = calendarServiceInstance();

    CalendarEvent event = calService.getEventById(eventId);
    String username = currentUserId();

    List<Invitation> invitations = Collections.<Invitation>emptyList();
    if (event != null) {
      //All invitations in event
      invitations = new LinkedList<Invitation>(Arrays.asList(event.getInvitations()));
      
      //Only return user's invitation if calendar is not editable
      Calendar calendar = calService.getCalendarById(event.getCalendarId());
      if (!Utils.isCalendarEditable(username, calendar)) {
        Iterator<Invitation> iter = invitations.iterator();
        
        while(iter.hasNext()) {
          if (!iter.next().getParticipant().equals(username)) {
            iter.remove();
          }
        }
      }
      
      //Return only invitation with specific status
      if (status != null) {
        Iterator<Invitation> iter = invitations.iterator();
        while(iter.hasNext()) {
          if (!iter.next().getStatus().equals(status)) {
            iter.remove();
          }
        }
      }
    }
    
    int fullSize = invitations.size();
    List data = new LinkedList();
    for (Invitation invitation : Utils.subList(invitations, offset, limit)) {
      data.add(extractObject(new InvitationResource(invitation), fields));
    }
    
    CollectionResource evData = new CollectionResource(data, fullSize);    
    evData.setOffset(offset);
    evData.setLimit(limit);

    if (jsonp != null) {
      JsonValue json = new JsonGeneratorImpl().createJsonObject(evData);
      StringBuilder sb = new StringBuilder(jsonp);
      sb.append('(').append(json).append(");");
      return Response.ok(sb.toString(), new MediaType("text", "javascript")).header(HEADER_LINK, buildFullUrl(uri, offset, limit, fullSize)).cacheControl(cc).build();
    }
    
    //
    return Response.ok(evData, MediaType.APPLICATION_JSON).header(HEADER_LINK, buildFullUrl(uri, offset, limit, fullSize)).cacheControl(cc).build();
  }

  @POST
  @RolesAllowed("users")
  @Path("/events/{id}/invitations/")
  public Response createInvitationForEvent(@PathParam("id") String id, @QueryParam("participant") String participant, 
                                           @QueryParam("status") String status) throws Exception {
    if (participant == null || participant.trim().isEmpty() || status == null) {
      return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
    }

    CalendarService service = calendarServiceInstance();
    EventDAO evtDAO = service.getEventDAO();
    String username = currentUserId();
    
    CalendarEvent event = service.getEventById(id);
    if (event != null) {
      Calendar calendar = service.getCalendarById(event.getCalendarId());
      if (!Utils.isCalendarEditable(username, calendar)) {
        return Response.status(HTTPStatus.UNAUTHORIZED).cacheControl(cc).build();
      }
      
      Invitation invite = evtDAO.createInvitation(id, participant, status);
      if (invite != null) {
        InvitationResource rs = new InvitationResource(invite);
        return Response.ok(rs, MediaType.APPLICATION_JSON).status(HTTPStatus.CREATED).cacheControl(cc).build();
      } else {
        return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
      }
    } else {
      return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
    }
  }

  /**
   * Parse date by ISO8601 standard
   * if start is null, start is current time
   * if end is null, end is current time plus 1 week
   * @param start
   * @param end
   * @return array of start, end date
   */
  private java.util.Calendar[] parseDate(String start, String end) {
    java.util.Calendar from = GregorianCalendar.getInstance();
    java.util.Calendar to = GregorianCalendar.getInstance();
    if(Utils.isEmpty(start)) {
      from = java.util.Calendar.getInstance();
      from.set(java.util.Calendar.HOUR, 0);
      from.set(java.util.Calendar.MINUTE, 0);
      from.set(java.util.Calendar.SECOND, 0);
      from.set(java.util.Calendar.MILLISECOND, 0);
    } else {
      from = ISO8601.parse(start);
    }
    if(Utils.isEmpty(end)) {
      to.add(java.util.Calendar.WEEK_OF_MONTH, 1);
    } else {
      to = ISO8601.parse(end);
    }
    return new java.util.Calendar[] {from, to};
  }
  
  /**
   * Doesn't allow limit parameter to exceed the default query_limit
   */
  private int parseLimit(int limit) {
    return (limit <=0 || limit > query_limit) ? query_limit : limit;
  }
  
  private String buildFullUrl(UriInfo uriInfo, int offset, int limit, long fullSize) {
      if (fullSize <= 0) {
        return "";
      }
      offset = offset < 0 ? 0 : offset;
      
      long prev = offset - limit;
      prev = offset > 0 && prev < 0 ? 0 : prev;
      long prevLimit = offset - prev;
      //
      StringBuilder sb = new StringBuilder();
      if (prev >= 0) {
        sb.append("<").append(uriInfo.getPath()).append("?offset=");
        sb.append(prev).append("&limit=").append(prevLimit).append(">").append(Utils.SEMICOLON).append("rel=\"previous\",");
      }
      
      long next = offset + limit;
      //
      if (next < fullSize) {
        sb.append("<").append(uriInfo.getPath()).append("?offset=");
        sb.append(next).append("&limit=").append(limit).append(">").append(Utils.SEMICOLON).append("rel=\"next\",");
      }
      
      //first page
      long firstLimit = limit > fullSize ? fullSize : limit;
      sb.append("<").append(uriInfo.getPath()).append("?offset=0&limit=").append(firstLimit).append(">");
      sb.append(Utils.SEMICOLON).append("rel=\"first\",");
      //last page
      long lastIndex = fullSize - (fullSize % firstLimit);
      if (lastIndex == fullSize) {
        lastIndex = fullSize - firstLimit;
      }
      if (lastIndex > 0) {
        sb.append("<").append(uriInfo.getPath()).append("?offset=").append(lastIndex);
        sb.append("&limit=").append(fullSize - lastIndex).append(">");
        sb.append(Utils.SEMICOLON).append("rel=\"last\"");
      }
      if (sb.charAt(sb.length() - 1) == ',') {
        sb.deleteCharAt(sb.length() - 1);
      }
      
      return sb.toString();
  }
  
  private static CalendarService calendarServiceInstance() {
    return (CalendarService)ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(CalendarService.class);
  }
  
  /**
   * 
   * @param auhtor : the feed create
   * @param events : list of event from data
   * @return
   * @throws Exception
   */
  private String makeFeed(String author, List<CalendarEvent> events, FeedData feedData, UriInfo uri) throws Exception {
    URI baseUri = uri.getBaseUri();
    String baseURL = baseUri.getScheme() + "://" + baseUri.getHost() + ":" + Integer.toString(baseUri.getPort());
    String baseRestURL = baseUri.toString();

    SyndFeed feed = new SyndFeedImpl();      
    feed.setFeedType("rss_2.0");
    feed.setTitle(feedData.getTitle());
    feed.setLink(baseURL + feedData.getUrl());
    feed.setDescription(feedData.getTitle());     
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    SyndEntry entry;
    SyndContent description; 
    for(CalendarEvent event : events) {
      if (Utils.EVENT_NUMBER > 0 && Utils.EVENT_NUMBER <= entries.size()) break;
      entry = new SyndEntryImpl();
      entry.setTitle(event.getSummary());
      entry.setLink(baseRestURL + BASE_EVENT_URL + Utils.SLASH + author + Utils.SLASH + event.getId() 
                    + Utils.SPLITTER + event.getCalType() + Utils.ICS_EXT);    
      entry.setAuthor(author) ;
      description = new SyndContentImpl();
      description.setType(Utils.MIMETYPE_TEXTPLAIN);
      description.setValue(event.getDescription());
      entry.setDescription(description);        
      entries.add(entry);
      entry.getEnclosures() ;
    }
    feed.setEntries(entries);      
    feed.setEncoding("UTF-8") ;     
    SyndFeedOutput output = new SyndFeedOutput();      
    String feedXML = output.outputString(feed);      
    feedXML = StringUtils.replace(feedXML,"&amp;","&");  
    return feedXML;
  }

  private boolean isInGroups(String[] groups) {
  	Identity identity = ConversationState.getCurrent().getIdentity();
  	for (String group : groups) {
  		if (identity.isMemberOf(group)) {
  			return true;
  		}
  	}

  	return false;
	}
  
  private boolean hasViewCalendarPermission(Calendar cal, String username) throws Exception {
    if (cal.getCalendarOwner() != null && cal.getCalendarOwner().equals(username)) return true;
    else if (cal.getGroups() != null) {
      return isInGroups(cal.getGroups());
    } else if (cal.getViewPermission() != null) {
      return Utils.canEdit(orgService, cal.getViewPermission(), username);
    }
    return false;
  }
  
  private String[] findViewableCalendars(String username) {
    CalendarCollection<Calendar> calendars = calendarServiceInstance().getAllCalendars(username, Calendar.TYPE_ALL, 0, -1);
    String[] calIds = new String[calendars.size()];
    int i = 0;
    for (Calendar cal : calendars) {
      calIds[i++] = cal.getId();
    }
    return calIds;
  }
  
  private String[] findEditableCalendars(String username) {
    CalendarCollection<Calendar> calendars = calendarServiceInstance().getAllCalendars(username, Calendar.TYPE_ALL, 0, -1);
    Iterator<Calendar> iter = calendars.iterator();
    while (iter.hasNext()) {
      if (!Utils.isCalendarEditable(username, iter.next())) {
        iter.remove();
      }
    }
    String[] calendarIds = new String[calendars.size()];
    int i = 0;
    for (Calendar cal : calendars) {
      calendarIds[i++] = cal.getId();
    }
    return calendarIds;
  }
  
  private EventQuery buildEventQuery(String start, String end, String category, String[] calIds, String calendarPath,
                                               String participant, String eventType, boolean returnSize) {
    java.util.Calendar[] dates = parseDate(start, end);    
    
    //Find all invitations that user is participant
    EventQuery uQuery = new EventQuery();
    uQuery.setCalendarPath(calendarPath);
    uQuery.setCalendarId(calIds);
    if (category != null) {
      uQuery.setCategoryId(new String[] {category});      
    }
    if (participant != null) {
      uQuery.setParticipants(new String[] {participant});      
    }
    uQuery.setEventType(eventType);
    uQuery.setFromDate(dates[0]);
    uQuery.setToDate(dates[1]);
    uQuery.setReturnSize(returnSize);
    return uQuery;
  }
  
  private CalendarEvent findEventAttachment(String attachmentID) throws Exception {
    int idx = attachmentID.indexOf("/calendars/");
    if (idx != -1) {
      int calendars =  idx + "/calendars/".length();
      int calendar = attachmentID.indexOf('/', calendars) + 1;
      int event = attachmentID.indexOf('/', calendar);
      if (calendar != -1 && event != -1) {
        String eventId = attachmentID.substring(calendar, event);
        return calendarServiceInstance().getEventById(eventId);      
      }      
    }
    return null;
  }
  
  private Object extractObject(Resource iv, String fields) {
    if (fields != null && iv != null) {
      String[] f = fields.split(",");
      
      if (f.length > 0) {
        JSONObject obj = new JSONObject(iv);        
        Map<String, Object> map = new HashMap<String, Object>();
        
        for (String name : f) {
          try {
            map.put(name, obj.get(name));
          } catch (JSONException e) {
            log.warn("Can't extract property {} from object {}", name, iv);            
          }
        }
        return map;
      }
    }
    return iv;
  }
  
  private String currentUserId() {
    return ConversationState.getCurrent().getIdentity().getUserId();
  }
}
