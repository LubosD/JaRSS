	/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest.v1;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import info.dolezel.jarss.rest.v1.entities.InitialSetupData;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hibernate.Session;

/**
 *
 * @author lubos
 */
@Path("initial-setup")
@PermitAll
public class InitialSetupService {
	private static boolean configured;
    private static boolean registrationAllowed;
    private static String user, password, url;

	static {
		try {
			loadConfiguration();
			
			if (url != null)
				applyConfiguration();
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.getLogger(InitialSetupService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static void loadConfiguration() {
		Preferences prefs = Preferences.userNodeForPackage(HibernateUtil.class);
		
		url = prefs.get("url", null);
        user = prefs.get("user", "root");
        password = prefs.get("password", "");
        registrationAllowed = prefs.getBoolean("registrationAllowed", false);
	}
	
	private static void saveConfiguration() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(HibernateUtil.class);
			
			prefs.put("url", url);
			prefs.put("user", user);
			prefs.put("password", password);
			prefs.putBoolean("registrationAllowed", registrationAllowed);
			
			prefs.flush();
		} catch (BackingStoreException ex) {
			Logger.getLogger(InitialSetupService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/completed")
	public Response getCompleted() {
		String json = "{\"configured\": " + configured + "}";
		return Response.ok(json).build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/setup")
	public Response postSetup(InitialSetupData data) {
		if (configured) {
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("JaRSS is already configured")).build();
		}
		
		if (data.getUrl() == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDescription("Database URL is mandatory")).build();
		}
		
		url = data.getUrl();
		user = data.getUser();
		password = data.getPassword();
		registrationAllowed = data.isRegistrationAllowed();
		
		try {
			Session session;
			
			applyConfiguration();
			session = HibernateUtil.getSessionFactory().openSession();
			
			saveConfiguration();
			session.close();
		} catch (Exception e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDescription(e.getMessage())).build();
		}
		
		return Response.noContent().build();
	}
	
	private static void applyConfiguration() throws Exception {
		String dialect, driverClass;
        
        if (url == null) {
            configured = false;
            return;
        }
        
        if (url.startsWith("jdbc:mysql://")) {
            dialect = "org.hibernate.dialect.MySQLDialect";
            driverClass = "com.mysql.jdbc.Driver";
		} else if (url.startsWith("jdbc:postgresql://")) {
            dialect = "org.hibernate.dialect.PostgreSQLDialect";
            driverClass = "org.postgresql.Driver";
        } else {
            configured = false;
            throw new Exception("Unsupported database URL; use PostgreSQL or MySQL");
        }
		
		if (user == null)
			throw new Exception("Database user cannot be empty");
        
        HibernateUtil.setDialect(dialect);
        HibernateUtil.setDriverClass(driverClass);
        HibernateUtil.setPassword(password);
        HibernateUtil.setUrl(url);
        HibernateUtil.setUsername(user);
        HibernateUtil.setupDatabaseConnection();
        
        configured = true;

	}
	
	public static boolean isRegistrationAllowed() {
		return registrationAllowed;
	}
}
