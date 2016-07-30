/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest.ttrss;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.Token;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.util.StringUtils;
import java.io.IOException;
import java.util.Calendar;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * Based on https://tt-rss.org/gitlab/fox/tt-rss/wikis/ApiReference and testing.
 *
 * @author lubos
 */
@Path("/")
public class TTRSSService {

	private static final int API_LEVEL = 0;
	private static final String VERSION = "1.5.0";
	private static final JsonReaderFactory factory = Json.createReaderFactory(null);

	private Token token;
	private User user;
	private EntityManager em;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response handler(@Context ContainerRequest request) throws IOException {
		JsonReader jsonReader;
		JsonObject json, response = null;
		int seq = 0;
		int status = 0;
		EntityTransaction tx = null;

		jsonReader = factory.createReader(request.getEntityStream());
		json = jsonReader.readObject();

		try {
			String op, sid;

			em = HibernateUtil.getEntityManager();
			tx = em.getTransaction();
			
			tx.begin();

			seq = json.getInt("seq");
			op = json.getString("op");
			sid = json.getString("sid");

			if (op == null) {
				throw new Exception("UNKNOWN_METHOD");
			}

			if ("login".equals(op)) {
				response = login(json);
			} else {

				if (sid != null) {
					token = Token.loadToken(em, sid);
				}
				if (token == null) {
					throw new Exception("NOT_LOGGED_IN");
				}

				switch (op) {
					case "isLoggedIn":
						response = isLoggedIn();
						break;
					case "logout":
						response = logout();
						break;
					case "getVersion":
						response = getVersion();
						break;
					case "getApiLevel":
						response = getApiLevel();
						break;
					case "getUnread":
						response = getUnread();
						break;
					case "getCounters":
						response = getCounters();
						break;
					case "getFeeds":
						response = getFeeds();
						break;
					case "getCategories":
						response = getCategories();
						break;
					case "getHeadlines":
						response = getHeadlines();
						break;
					case "updateArticle":
						response = updateArticle();
						break;
					case "getArticle":
						response = getArticle();
						break;
					case "getConfig":
						response = getConfig();
						break;
					case "updateFeed":
						response = updateFeed();
						break;
					case "catchupFeed":
						response = catchupFeed();
						break;
					default:
						throw new Exception("UNKNOWN_METHOD");
				}
			}

			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();

			if (tx != null) {
				tx.rollback();
			}

			response = Json.createObjectBuilder().add("error", e.getMessage()).build();
			status = 1;
		}
		
		if (em != null)
			em.close();

		JsonObject msg = Json.createObjectBuilder().add("seq", seq).add("status", status)
				.add("content", response).build();
		return Response.ok(msg.toString()).build();
	}

	private JsonObject login(JsonObject json) throws Exception {
		String login, password;

		login = json.getString("user");
		password = json.getString("password");

		Query query = em.createNamedQuery("User.getByLogin", User.class);
		String hash;

		query.setParameter("login", login);
		user = (User) query.getSingleResult();

		if (user == null) {
			throw new Exception("LOGIN_ERROR");
		}

		hash = StringUtils.calculateHash(password, new String(user.getSalt()));
		if (!hash.equals(user.getPassword())) {
			throw new Exception("LOGIN_ERROR");
		}

		// Auth ok, issue a token
		Calendar cal = Calendar.getInstance();
		token = new Token();

		cal.add(Calendar.SECOND, Token.TOKEN_VALIDITY);
		token.setExpiry(cal.getTime());
		token.setUser(user);
		token.setValue(StringUtils.randomString(8));

		em.persist(token);

		return Json.createObjectBuilder()
				.add("session_id", token.getValue())
				.add("api_level", API_LEVEL).build();
	}

	private JsonObject logout() {
		em.remove(token);
		return Json.createObjectBuilder().add("status", "OK").build();
	}

	private JsonObject isLoggedIn() {
		return Json.createObjectBuilder().add("status", true).build();
	}

	private JsonObject getApiLevel() {
		return Json.createObjectBuilder()
				.add("level", API_LEVEL).build();
	}

	private JsonObject getVersion() {
		return Json.createObjectBuilder()
				.add("version", VERSION).build();
	}

	private JsonObject getUnread() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getCounters() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getFeeds() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getCategories() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getHeadlines() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject updateArticle() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getArticle() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject getConfig() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject updateFeed() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private JsonObject catchupFeed() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
