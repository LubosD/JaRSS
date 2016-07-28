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
package info.dolezel.jarss.rest.v1.ws;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.Token;
import info.dolezel.jarss.data.User;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
@WebSocket
public class UnreadNotificationEndpoint {
	private static final Map<Session, User> sessions = new HashMap<>();
	private static final MultiValuedMap<User, Session> userSessions = new HashSetValuedHashMap<>();
	
	@OnWebSocketConnect
	public void onOpen(Session session) {
		
	}
	
	@OnWebSocketError
	public void onError(Session session, Throwable error) {
		synchronized (sessions) {
			sessions.remove(session);
		}
	}
	
	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		User user = null;
		
		synchronized (sessions) {
			user = sessions.remove(session);
		}
		
		if (user != null) {
			synchronized (userSessions) {
				userSessions.removeMapping(user, session);
			}
		}
	}
	
	@OnWebSocketMessage
	public void onMessage(Session session, String text) {
		Transaction tx = null;
		try {
			org.hibernate.Session hsession = HibernateUtil.getSessionFactory().getCurrentSession();
			JsonReader reader;
			JsonObject object;
			Token token;
			
			tx = hsession.beginTransaction();
			reader = Json.createReader(new StringReader(text));
			object = reader.readObject();
			
			token = Token.loadToken(hsession, object.getString("token"));
			if (token == null) {
				tx.rollback();
				
				Logger.getLogger(UnreadNotificationEndpoint.class.getName()).log(Level.WARNING, "Invalid token provided over WebSocket");
				session.close();
			}
			else {
				synchronized (sessions) {
					sessions.put(session, token.getUser());
				}
				synchronized (userSessions) {
					userSessions.put(token.getUser(), session);
				}
			}
			
			tx.commit();
		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			
			Logger.getLogger(UnreadNotificationEndpoint.class.getName()).log(Level.SEVERE, "Error processing incoming WebSocket message", ex);
		}
	}
	
	public static boolean userHasSession(User user) {
		synchronized (userSessions) {
			return userSessions.containsKey(user);
		}
	}
	
	public static void pushMessage(User user, String message) {
		Set<Session> ss;
		
		synchronized (userSessions) {
			ss = new HashSet<>(userSessions.get(user));
		}
		
		for (Session s : ss) {
			try {
				s.getRemote().sendStringByFuture(message);
			} catch (Exception ex) {
				Logger.getLogger(UnreadNotificationEndpoint.class.getName()).log(Level.FINE, "Cannot push notification to session " + s, ex);
			}
		}
	}
}
