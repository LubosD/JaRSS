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
import info.dolezel.jarss.data.FeedItem;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.data.Token;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.ArticleHeadlineData;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import info.dolezel.jarss.rest.v1.entities.FeedDetails;
import info.dolezel.jarss.rest.v1.entities.FeedSubscriptionData;
import info.dolezel.jarss.util.StringUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.io.IOUtils;

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
		EntityManager em;
		User user = (User) securityContext.getUserPrincipal();
		JsonArrayBuilder jsonBuilder;
		List<FeedCategory> fcs;
		List<Feed> rootFeeds;
		
		em = HibernateUtil.getEntityManager();
		
		jsonBuilder = Json.createArrayBuilder();
		
		try {
			fcs = em.createQuery("select fc from FeedCategory fc where fc.user = :user order by fc.name asc").setParameter("user", user).getResultList();

			for (FeedCategory fc : fcs) {
				JsonObjectBuilder obj = getFeedCategory(em, fc);
				jsonBuilder.add(obj);
			}

			rootFeeds = em.createQuery("select f from Feed f where f.user = :user and f.feedCategory is null order by f.name asc").setParameter("user", user).getResultList();
			for (Feed feed : rootFeeds) {
				JsonObjectBuilder obj = getFeed(em, feed);
				jsonBuilder.add(obj);
			}

			return Response.ok(jsonBuilder.build().toString()).build();
		} finally {
			em.close();
		}
	}
	
	private JsonObjectBuilder getFeed(EntityManager em, Feed feed) {
		JsonObjectBuilder objFeed = Json.createObjectBuilder();
		Date readAllBefore;

		if (feed.getReadAllBefore() != null)
			readAllBefore = feed.getReadAllBefore();
		else
			readAllBefore = new Date(0);
		
		long count = (Long) em.createQuery("select count(*) from FeedItemData fid left outer join fid.feedItems as fi where fid.feedData = :fd and (fi is null or (fi.feed = :feed and fi.read = false)) and fid.date > :readAllBefore", Long.class)
				.setParameter("fd", feed.getData())
				.setParameter("feed", feed)
				.setParameter("readAllBefore", readAllBefore).getSingleResult();

		objFeed.add("id", feed.getId());
		objFeed.add("title", feed.getName());
		objFeed.add("unread", count);
		objFeed.add("isCategory", false);
		
		Date lastFetch = feed.getData().getLastFetch();
		if (lastFetch != null)
			objFeed.add("lastFetchTime", lastFetch.getTime());
		
		String lastError = feed.getData().getLastError();
		if (lastError != null)
			objFeed.add("lastError", lastError);
		
		return objFeed;
	}
	
	private JsonObjectBuilder getFeedCategory(EntityManager em, FeedCategory fc) {
		JsonObjectBuilder obj = Json.createObjectBuilder();
		int fcUnread = 0;
		Collection<Feed> feeds;
		JsonArrayBuilder nodes = Json.createArrayBuilder();

		obj.add("id", fc.getId());
		obj.add("title", fc.getName());

		feeds = fc.getFeeds();
		for (Feed feed : feeds) {
			JsonObject objFeed = getFeed(em, feed).build();

			fcUnread = objFeed.getInt("unread");

			nodes.add(objFeed);
		}

		obj.add("unread", fcUnread);
		obj.add("nodes", nodes);
		obj.add("isCategory", true);
		
		return obj;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response subscribeFeed(@Context SecurityContext context, FeedSubscriptionData data) {
		FeedCategory fc = null;
		EntityManager em;
		EntityTransaction tx;
		User user;
		FeedData feedData;
		Feed f;
		boolean createdNewFD = false;
		
		if (data.getUrl() == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDescription("Feed URL missing")).build();
		}
		
		user = (User) context.getUserPrincipal();
		em = HibernateUtil.getEntityManager();
		tx = em.getTransaction();
		tx.begin();
		
		try {
			if (data.getCategoryId() != 0) {
				try {
					fc = (FeedCategory) em.createQuery("select fc from FeedCategory fc where fc.id = :id", FeedCategory.class).setParameter("id", data.getCategoryId()).getSingleResult();
				} catch (NoResultException e) {
					return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed category not found")).build();
				}

				if (!fc.getUser().equals(user)) {
					return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed category not owned by user")).build();
				}
			}

			// Try to look up existing FeedData
			try {
				feedData = (FeedData) em.createNamedQuery("FeedData.getByUrl").setParameter("url", data.getUrl()).getSingleResult();
			} catch (NoResultException e) {
				feedData = new FeedData();
				feedData.setUrl(data.getUrl());

				try {
					loadFeedDetails(feedData);
				} catch (Exception ex) {
					e.printStackTrace();
					return Response.status(Response.Status.BAD_GATEWAY).entity(new ErrorDescription("Cannot fetch the feed")).build();
				}

				em.persist(feedData);
				createdNewFD = true;
			}

			f = new Feed();
			f.setUser(user);
			f.setFeedCategory(fc);
			f.setData(feedData);
			f.setName(feedData.getTitle());

			em.persist(f);

			tx.commit();

			if (createdNewFD)
				FeedsEngine.getInstance().submitFeedRefresh(feedData);

			return Response.noContent().build();
		} finally {
			if (tx.isActive())
				tx.rollback();
			em.close();
		}
	}
	
	@DELETE
	@Path("{id}")
	public Response unsubscribeFeed(@Context SecurityContext context, @PathParam("id") int feedId) {
		EntityManager em;
		EntityTransaction tx;
		User user;
		Feed f;
		FeedData fd;
		
		em = HibernateUtil.getEntityManager();
		tx = em.getTransaction();
		user = (User) context.getUserPrincipal();
		
		try {
			tx.begin();

			try {
				f = (Feed) em.createQuery("from Feed where id = :id", Feed.class).setParameter("id", feedId).getSingleResult();
			} catch (NoResultException e) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed does not exist")).build();
			}
			if (!f.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}

			fd = f.getData();
			em.remove(f);

			if (fd.getFeeds().isEmpty())
				em.remove(fd);

			tx.commit();

			return Response.noContent().build();
		} finally {
			if (tx.isActive())
				tx.rollback();
			em.close();
		}
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
	@Path("{id}/headlines")
	public Response getArticleHeadlines(@Context SecurityContext context, @PathParam("id") int feedId, @QueryParam("skip") int skip, @QueryParam("limit") int limit) {
		EntityManager em;
		List<Object[]> articles;
		Query query;
		ArticleHeadlineData[] result;
		Feed feed;
		User user;
		
		user = (User) context.getUserPrincipal();
		em = HibernateUtil.getEntityManager();
		
		try {
		
			feed = em.find(Feed.class, feedId);
			if (feed == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed does not exist")).build();
			}
			if (!feed.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}

			query = em.createQuery("SELECT fid, fi from FeedItemData fid LEFT OUTER JOIN fid.feedItems AS fi where fid.feedData = :fd and (fi is null or fi.feed = :feed) order by fid.date desc", Object[].class)
					.setParameter("fd", feed.getData())
					.setParameter("feed", feed)
					.setFirstResult(skip);

			if (limit > 0)
				query.setMaxResults(limit);

			articles = query.getResultList();

			result = new ArticleHeadlineData[articles.size()];
			for (int i = 0; i < articles.size(); i++) {
				FeedItemData article = (FeedItemData) articles.get(i)[0];
				FeedItem feedItem = (FeedItem) articles.get(i)[1];

				ArticleHeadlineData data = new ArticleHeadlineData();
				String text;

				data.setPublished(article.getDate().getTime());
				data.setTitle(article.getTitle());
				data.setId(article.getId());

				text = StringUtils.html2text(article.getText());

				if (text.length() > 130)
					text = text.substring(0, 130);

				data.setExcerpt(text);
				data.setLink(article.getLink());

				if (feedItem != null) {
					data.setRead(feedItem.isRead());
					data.setStarred(feedItem.isStarred());
				}

				result[i] = data;
			}

			return Response.ok(result).build();
		} finally {
			em.close();
		}
	}
	
	// Return details from the RSS feed itself
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}/details")
	public Response getFeedDetails(@Context SecurityContext context, @PathParam("id") int feedId) {
		EntityManager em;
		User user;
		Feed feed;
		FeedData feedData;
		FeedDetails details;
		
		em = HibernateUtil.getEntityManager();
		
		try {
			user = (User) context.getUserPrincipal();
			feed = em.find(Feed.class, feedId);
			if (feed == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed does not exist")).build();
			}
			if (!feed.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}

			feedData = feed.getData();
			details = new FeedDetails();
			details.setDescription(feedData.getDescription());
			details.setWebsite(feedData.getWebsiteUrl());
			details.setLastFetchError(feedData.getLastError());
			details.setTitle(feedData.getTitle());

			return Response.ok(details).build();
		} finally {
			em.close();
		}
	}

	private void loadFeedDetails(FeedData feedData) throws Exception {
		URL url = new URL(feedData.getUrl());
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(url)); // TODO: handle redirects !
		String iconUrl;

		feedData.setTitle(feed.getTitle());
		feedData.setWebsiteUrl(feed.getLink());
		feedData.setDescription(feed.getDescription());
		
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
	
	@POST
	@Path("{id}/markAllRead")
	public Response markAllRead(@Context SecurityContext context, @PathParam("id") int feedId, @QueryParam("allBefore") long timeMillis) {
		EntityManager em;
		EntityTransaction tx;
		User user;
		Feed feed;
		Date newDate;
		
		user = (User) context.getUserPrincipal();
		em = HibernateUtil.getEntityManager();
		tx = em.getTransaction();
		
		tx.begin();
		
		try {
			feed = em.find(Feed.class, feedId);
			if (feed == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed does not exist")).build();
			}
			if (!feed.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}

			newDate = new Date(timeMillis);
			if (feed.getReadAllBefore() == null || feed.getReadAllBefore().before(newDate)) {
				feed.setReadAllBefore(newDate);
				em.persist(feed);
			}

			em.createQuery("delete from FeedItem fi where fi.feed = :feed and fi.data.date < :date and not fi.starred and not fi.exported and size(fi.tags) = 0")
					.setParameter("feed", feed)
					.setParameter("date", newDate)
					.executeUpdate();

			tx.commit();

			return Response.noContent().build();
		} finally {
			if (tx.isActive())
				tx.rollback();
			em.close();
		}
	}
	
	@POST
	@Path("{id}/forceFetch")
	public Response forceFetch(@Context SecurityContext context, @PathParam("id") int feedId) {
		EntityManager em = HibernateUtil.getEntityManager();
		User user;
		Feed feed;
		
		try {
			user = (User) context.getUserPrincipal();

			feed = em.find(Feed.class, feedId);
			if (feed == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed does not exist")).build();
			}
			if (!feed.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}

			FeedsEngine.getInstance().submitFeedRefresh(feed.getData());

			return Response.noContent().build();
		} finally {
			em.close();
		}
	}
	
	private byte[] emptyImage;
	
	@GET
	@Path("{id}/icon")
	@Produces("image/png")
	@PermitAll
	public Response getFeedIcon(@Context ServletContext ctx, @PathParam("id") int feedId, @QueryParam("token") String tokenString) throws IOException {
		EntityManager em = HibernateUtil.getEntityManager();
		Token token;
		Feed feed;
		byte[] data;
		
		try {
			token = Token.loadToken(em, tokenString);

			feed = em.find(Feed.class, feedId);
			if (feed == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(emptyGif(ctx)).build();
			}
			if (!feed.getUser().equals(token.getUser())) {
				return Response.status(Response.Status.FORBIDDEN).entity(emptyGif(ctx)).build();
			}

			data = feed.getData().getIconData();
			if (data == null)
				data = emptyGif(ctx);

			return Response.ok(data).build();
		} finally {
			em.close();
		}
	}

	private byte[] emptyGif(ServletContext ctx) throws IOException {
		if (emptyImage != null)
			return emptyImage;
		
		String path = ctx.getRealPath("/data/jarss/img/empty.gif");
		byte[] data =  IOUtils.toByteArray(new FileInputStream(path));
		
		emptyImage = data;
		return data;
	}
}
