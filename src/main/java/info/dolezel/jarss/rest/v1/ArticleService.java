/*
 * Copyright (C) 2016 lubos
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

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.Feed;
import info.dolezel.jarss.data.FeedItem;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.data.FeedItemEnclosure;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.ArticleData;
import info.dolezel.jarss.rest.v1.entities.ArticleEnclosure;
import info.dolezel.jarss.rest.v1.entities.ArticleUpdateData;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
@Path("article")
@RolesAllowed("user")
public class ArticleService {
	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getArticle(@Context SecurityContext context, @PathParam("id") int id) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		Long count;
		ArticleData response;
		User user;
		
		FeedItemData fid = session.get(FeedItemData.class, id);
		if (fid == null) {
			tx.rollback();
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Article does not exist")).build();
		}
		
		user = (User) context.getUserPrincipal();
		
		// Does the article belong to a feed subscribed by the current user?
		count = (Long) session.createQuery("select count(*) from Feed f inner join f.data as fd where fd = :fd and f.user = :user")
				.setEntity("user", user)
				.setEntity("fd", fid.getFeedData()).uniqueResult();
		if (count == 0) {
			tx.rollback();
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("You are not subscribed to this feed")).build();
		}
		
		response = new ArticleData();
		response.setAuthor(fid.getAuthor());
		response.setDescription(fid.getText());
		
		List<ArticleEnclosure> enclosures;
		enclosures = fid.getEnclosures().stream().map((FeedItemEnclosure t) -> new ArticleEnclosure(t)).collect(Collectors.toList());
		response.setEnclosures(enclosures);
		
		tx.commit();
		
		return Response.ok(response).build();
	}
	
	@Path("{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateArticle(@Context SecurityContext context, @PathParam("id") int id, ArticleUpdateData data) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		User user;
		Feed feed;
		FeedItem fi;
		
		user = (User) context.getUserPrincipal();
		
		feed = (Feed) session.createQuery("select f from Feed f inner join f.data as fd inner join fd.feedItemData as fid where f.user = :user and fid.id = :fid")
				.setEntity("user", user)
				.setInteger("fid", id).uniqueResult();
		if (feed == null) {
			tx.rollback();
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("You are not subscribed to this feed")).build();
		}
		
		// Try to find existing FeedItem
		fi = (FeedItem) session.createQuery("from FeedItem fi where fi.feed.user = :user and fi.data.id = :id")
				.setEntity("user", user)
				.setInteger("id", id)
				.uniqueResult();
		
		if (fi == null) {
			fi = new FeedItem();
			fi.setData(new FeedItemData(id));
			fi.setFeed(feed);
		}
		
		if (data.getRead() != null)
			fi.setRead(data.getRead());
		if (data.getStarred() != null)
			fi.setStarred(data.getStarred());
		if (data.getPublished() != null)
			fi.setExported(data.getPublished());
		
		session.saveOrUpdate(fi);
		tx.commit();
		
		return Response.noContent().build();
	}
}
