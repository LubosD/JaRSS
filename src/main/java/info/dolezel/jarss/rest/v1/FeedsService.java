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

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.FeedSubscriptionData;
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
	@Path("subscribe")
	public Response subscribeFeed(FeedSubscriptionData data) {
		return Response.noContent().build();
	}
	
	@DELETE
	@Path("{id}")
	public Response unsubscribeFeed(@PathParam("id") int feedId) {
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
}
