/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.rest.v1.AuthenticationFilter;
import info.dolezel.jarss.rest.v1.FeedsService;
import info.dolezel.jarss.rest.v1.InitialSetupService;
import info.dolezel.jarss.rest.v1.NoCacheFilter;
import info.dolezel.jarss.rest.v1.UserService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
public class RestApplication extends ResourceConfig {
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
	
	public RestApplication() {
		register(InitialSetupService.class);
		register(UserService.class);
		register(NoCacheFilter.class);
		register(RolesAllowedDynamicFeature.class);
		register(FeedsService.class);
		register(AuthenticationFilter.class);
		
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				cleanupTokens();
			}
		}, 1, 1, TimeUnit.HOURS);
		
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				refreshFeeds();
			}
		}, 1, 15, TimeUnit.MINUTES);
	}
	
	private void cleanupTokens() {
		try {
			Session session = HibernateUtil.getSessionFactory().getCurrentSession();
			Transaction tx = session.beginTransaction();
			
			session.createQuery("delete from Token where expiry < current_date").executeUpdate();

			tx.commit();
			
			Logger.getLogger(RestApplication.class.getName()).log(Level.FINE, "Deleted expired tokens");
			
		} catch (Exception e) {
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error cleaning up tokens", e);
		}
	}
	
	private void refreshFeeds() {
		try {
			
		} catch (Exception e) {
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error refreshing feeds", e);
		}
	}
}
