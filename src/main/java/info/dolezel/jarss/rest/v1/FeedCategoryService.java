/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest.v1;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.FeedCategory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
@Path("categories")
@RolesAllowed("user")
public class FeedCategoryService {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll(@Context SecurityContext context) {
		List<FeedCategory> data;
		List<info.dolezel.jarss.data.FeedCategory> list;
		Session session;
		Transaction tx;
		User user = (User) context.getUserPrincipal();
		
		session = HibernateUtil.getSessionFactory().openSession();
		tx = session.beginTransaction();
		list = session.createQuery("from FeedCategory where user = :user")
				.setEntity("user", user).list();
		
		data = new ArrayList<>(list.size());
		
		for (info.dolezel.jarss.data.FeedCategory fc : list) {
			FeedCategory json = new FeedCategory();
			json.setId(fc.getId());
			json.setName(fc.getName());
			
			data.add(json);
		}
		
		return Response.ok(data).build();
	}
}
