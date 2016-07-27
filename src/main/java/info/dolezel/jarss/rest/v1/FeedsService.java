/*
 * Copyright (C) 2016 Lubos Dolezel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.dolezel.jarss.rest.v1;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import info.dolezel.jarss.FeedsEngine;
import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.Feed;
import info.dolezel.jarss.data.FeedCategory;
import info.dolezel.jarss.data.FeedData;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import info.dolezel.jarss.rest.v1.entities.FeedSubscriptionData;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
@RolesAllowed("user")
@Path("feeds")
public class FeedsService {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("tree")
	public Response getFeedsTree(@Context SecurityContext securityContext) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		User user = (User) securityContext.getUserPrincipal();
		
		tx.rollback();
		
		return Response.ok("[{ \"id\": 1, \"title\": \"Test group\", \"unread\": 5,"
				+ "\"nodes\": [{ \"id\": 2, \"title\": \"Test feed\", \"unread\": 5 }] }]").build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response subscribeFeed(@Context SecurityContext context, FeedSubscriptionData data) {
		FeedCategory fc = null;
		Session session;
		Transaction tx;
		User user;
		FeedData feedData;
		Feed f;
		boolean createdNewFD = false;
		
		if (data.getUrl() == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDescription("Feed URL missing")).build();
		}
		
		user = (User) context.getUserPrincipal();
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		tx = session.beginTransaction();
		
		if (data.getCategoryId() != 0) {
			fc = (FeedCategory) session.createQuery("from FeedCategory where id = :id").setInteger("id", data.getCategoryId()).uniqueResult();
			
			if (fc == null) {
				tx.rollback();
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed category not found")).build();
			} else if (!fc.getUser().equals(user)) {
				tx.rollback();
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed category not owned by user")).build();
			}
		}
		
		// Try to look up existing FeedData
		feedData = (FeedData) session.getNamedQuery("FeedData.getByUrl").setString("url", data.getUrl()).uniqueResult();
		if (feedData == null) {
			feedData = new FeedData();
			feedData.setUrl(data.getUrl());
			
			try {
				loadFeedDetails(feedData);
			} catch (Exception e) {
				tx.rollback();
				return Response.status(Response.Status.BAD_GATEWAY).entity(new ErrorDescription("Cannot fetch the feed")).build();
			}
			
			session.save(feedData);
			createdNewFD = true;
		}
		
		f = new Feed();
		f.setUser(user);
		f.setFeedCategory(fc);
		f.setData(feedData);
		f.setName(feedData.getTitle());
		
		session.save(f);
		
		tx.commit();
		
		if (createdNewFD)
			FeedsEngine.getInstance().submitFeedRefresh(feedData);
		
		return Response.noContent().build();
	}
	
	@DELETE
	@Path("{id}")
	public Response unsubscribeFeed(@Context SecurityContext context, @PathParam("id") int feedId) {
		Session session;
		Transaction tx;
		User user;
		Feed f;
		FeedData fd;
		
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		tx = session.beginTransaction();
		user = (User) context.getUserPrincipal();
		
		f = (Feed) session.createQuery("from Feed where id = :id").setInteger("id", feedId).uniqueResult();
		if (!f.getUser().equals(user)) {
			tx.rollback();
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
		}
		
		fd = f.getData();
		session.delete(f);
		
		if (fd.getFeeds().isEmpty())
			session.delete(fd);
		
		tx.commit();
		
		return Response.noContent().build();
	}
	
	@PUT
	@Path("{id}")
	public Response modifyFeed(@PathParam("id") int feedId) {
		return Response.noContent().build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	public Response getFeedParameters(@PathParam("id") int feedId) {
		return Response.ok().build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}/articles")
	public Response getArticles(@PathParam("id") int feedId) {
		return Response.ok().build();
	}

	private void loadFeedDetails(FeedData feedData) throws Exception {
		URL url = new URL(feedData.getUrl());
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(url));
		String iconUrl;

		feedData = new FeedData();
		feedData.setTitle(feed.getTitle());
		
		try {
			if (feed.getImage() != null)
				iconUrl = feed.getImage().getUrl();
			else
				iconUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/favicon.ico").toString();

			if (iconUrl != null) {
				byte[] data;
				data = IOUtils.toByteArray(new URL(iconUrl).openConnection().getInputStream());
				feedData.setIconData(data);
			}

		} catch (IOException ex) {
			Logger.getLogger(FeedsService.class.getName()).log(Level.WARNING, "Cannot fetch feed icon", ex);
		}
	}
}
