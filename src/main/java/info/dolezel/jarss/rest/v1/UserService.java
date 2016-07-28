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
import info.dolezel.jarss.data.Token;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import info.dolezel.jarss.rest.v1.entities.LoginData;
import info.dolezel.jarss.rest.v1.entities.RegistrationData;
import info.dolezel.jarss.util.StringUtils;
import java.util.Calendar;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

/**
 *
 * @author lubos
 */
@Path("user")
@RolesAllowed("user")
public class UserService {
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@PermitAll
	@Path("register")
	public Response registerUser(RegistrationData regData) throws Exception {
		Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = sess.beginTransaction();
		int userCount;
		String role = "user";
		
		userCount = ((Number) sess.createCriteria(User.class).setProjection(Projections.rowCount()).uniqueResult()).intValue();
		
		if (userCount == 0)
			role = "admin";
		
		if (userCount > 0 && !InitialSetupService.isRegistrationAllowed()) {
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Registrations are not allowed")).build();
		}
		
		Query query = sess.getNamedQuery("User.getByLogin");
        
        query.setString("login", regData.getUser());
        
        if (query.uniqueResult() != null) {
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("User already exists")).build();
		}
		
		User user = new User();
		String salt = StringUtils.randomString(10);
		
		user.setLogin(regData.getUser());
		user.setEmail(regData.getEmail());
		user.setSalt(salt.getBytes());
		user.setPassword(StringUtils.calculateHash(regData.getPassword(), salt));
		user.setRole(role);
		
		sess.save(user);
		tx.commit();
		
		return Response.noContent().build();
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	@Path("login")
	public Response login(LoginData loginData) throws Exception {
		Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = sess.beginTransaction();
		Query query = sess.getNamedQuery("User.getByLogin");
		User user;
		String hash;
		
		query.setString("login", loginData.getLogin());
		user = (User) query.uniqueResult();
		
		if (user == null) {
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Authentication failed")).build();
		}
		
		hash = StringUtils.calculateHash(loginData.getPassword(), new String(user.getSalt()));
		if (!hash.equals(user.getPassword())) {
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Authentication failed")).build();
		}
		
		// Auth ok, issue a token
		Token token = new Token();
		Calendar cal = Calendar.getInstance();
		
		cal.add(Calendar.SECOND, Token.TOKEN_VALIDITY);
		token.setExpiry(cal.getTime());
		token.setUser(user);
		token.setValue(StringUtils.randomString(8));
		
		sess.save(token);
		tx.commit();
		
		return Response.ok("{\"token\": \"" + token.getValue() + "\"}").build();
	}
}
