/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss;

import info.dolezel.jarss.data.Feed;
import info.dolezel.jarss.data.FeedCategory;
import info.dolezel.jarss.data.FeedData;
import info.dolezel.jarss.data.FeedItem;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.data.Tag;
import info.dolezel.jarss.data.Token;
import info.dolezel.jarss.data.User;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * Hibernate Utility class with a convenient method to get Session Factory
 * object.
 *
 * @author lubos
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static String driverClass, dialect, url, username, password;

    public static void setupDatabaseConnection() throws HibernateException {
        // Create the SessionFactory from standard (hibernate.cfg.xml)
        // config file.
        
        Configuration conf = new Configuration();
        
        conf.addAnnotatedClass(Feed.class)
                .addAnnotatedClass(FeedCategory.class)
                .addAnnotatedClass(FeedItem.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Tag.class)
                .addAnnotatedClass(FeedData.class)
				.addAnnotatedClass(Token.class)
                .addAnnotatedClass(FeedItemData.class);
        
        // <!-- Database connection settings -->
        conf.setProperty("hibernate.connection.url", url);
        conf.setProperty("hibernate.connection.username", username);
        conf.setProperty("hibernate.connection.password", password);
        conf.setProperty("hibernate.connection.driver_class", driverClass);
        conf.setProperty("hibernate.dialect", dialect);
        
        conf.setProperty("hibernate.hbm2ddl.auto", "update");
        conf.setProperty("hibernate.connection.pool_size", "10");
        conf.setProperty("hibernate.show_sql", "true");
		conf.setProperty("hibernate.current_session_context_class", "org.hibernate.context.internal.ThreadLocalSessionContext");
        
        //conf.configure();
        
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(conf.getProperties())
                .build();
        
        sessionFactory = conf.buildSessionFactory(serviceRegistry);
		System.out.println("Session factory: " + sessionFactory);
    }
    
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
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
