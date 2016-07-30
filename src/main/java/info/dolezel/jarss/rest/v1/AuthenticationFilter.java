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
import java.io.IOException;
import java.security.Principal;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author lubos
 */
@Provider
@PreMatching // Needed with Jersey, but not with Resteasy
public class AuthenticationFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (auth == null)
			return;
		
		String[] parts = auth.split(" ", 2);
		
		if (parts.length == 2 && "Bearer".equals(parts[0])) {
			EntityManager em = HibernateUtil.getEntityManager();
			EntityTransaction tx = em.getTransaction();
			Token token;
			
			tx.begin();
			
			try {
			
				token = Token.loadToken(em, parts[1]);

				if (token == null)
					return; // Invalid token

				tx.commit();
			} finally {
				if (tx.isActive())
					tx.rollback();
				em.close();
			}
			
			setUser(requestContext, token.getUser());
		}
	}
	
	private void setUser(ContainerRequestContext requestContext, final User user) {
		requestContext.setSecurityContext(new SecurityContext() {
			@Override
			public Principal getUserPrincipal() {
				return user;
			}

			@Override
			public boolean isUserInRole(String role) {
				if ("user".equals(role))
					return true;
				else
					return user.getRole().equals(role);
			}

			@Override
			public boolean isSecure() {
				return false;
			}

			@Override
			public String getAuthenticationScheme() {
				return "database";
			}
			
		});
	}
	
}
