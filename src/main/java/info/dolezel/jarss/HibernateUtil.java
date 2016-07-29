/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

/**
 * Hibernate Utility class with a convenient method to get the SessionFactory / EntityManagerFactory
 * objects.
 *
 * @author lubos
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;
	private static EntityManagerFactory emFactory;
    private static String driverClass, dialect, url, username, password;

    public static void setupDatabaseConnection() throws HibernateException {
		Map<String, Object> conf = new HashMap<>();
		conf.put("hibernate.connection.url", url);
        conf.put("hibernate.connection.username", username);
        conf.put("hibernate.connection.password", password);
        conf.put("hibernate.connection.driver_class", driverClass);
        conf.put("hibernate.dialect", dialect);
        
        conf.put("hibernate.hbm2ddl.auto", "update");
        conf.put("hibernate.connection.pool_size", "10");
        conf.put("hibernate.show_sql", "true");
		conf.put("hibernate.current_session_context_class", "org.hibernate.context.internal.ThreadLocalSessionContext");
		
		emFactory = Persistence.createEntityManagerFactory("hibernate", conf);
		sessionFactory = emFactory.unwrap(SessionFactory.class);
    }
    
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
	
	public static EntityManager getEntityManager() {
		return emFactory.createEntityManager();
	}
	
	public static void shutdown() {
		if (emFactory != null) {
			emFactory.close();
			emFactory = null;
		}
	}

    public static String getDriverClass() {
        return driverClass;
    }

    public static void setDriverClass(String driverClass) {
        HibernateUtil.driverClass = driverClass;
    }

    public static String getDialect() {
        return dialect;
    }

    public static void setDialect(String dialect) {
        HibernateUtil.dialect = dialect;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        HibernateUtil.url = url;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        HibernateUtil.username = username;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        HibernateUtil.password = password;
    }
}
