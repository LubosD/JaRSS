/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import info.dolezel.jarss.data.FeedData;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.rest.v1.InitialSetupService;
import info.dolezel.jarss.rest.v1.RestApplication;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
public class FeedsEngine implements ServletContextListener {
	private static FeedsEngine instance;
	private ScheduledExecutorService executor;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		instance = this;
		executor = Executors.newScheduledThreadPool(5);
		
		InitialSetupService.init();
		
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (InitialSetupService.isConfigured())
					cleanupTokens();
			}
		}, 1, 1, TimeUnit.HOURS);
		
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (InitialSetupService.isConfigured())
					refreshFeeds();
			}
		}, 1, 15, TimeUnit.MINUTES);
	}

	public static FeedsEngine getInstance() {
		return instance;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		executor.shutdownNow();
		instance = null;
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
			Session session;
			Transaction tx;
			List<FeedData> fd;
			
			session = HibernateUtil.getSessionFactory().openSession();
			
			tx = session.beginTransaction();
			fd = session.createQuery("from FeedData").list();
			tx.commit();
			
			for (FeedData f : fd) {
				tx = session.beginTransaction();
				refreshFeed(session, f);
				tx.commit();
			}
		} catch (Exception e) {
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error refreshing feeds", e);
		}
	}
	
	public void submitFeedRefresh(final FeedData fd) {
		executor.schedule(new Runnable() {

			@Override
			public void run() {
				Transaction tx = null;
				
				try {
					Session session = HibernateUtil.getSessionFactory().getCurrentSession();
					
					tx = session.beginTransaction();
					
					refreshFeed(session, fd);
					
					tx.commit();
				} catch (Exception e) {
					if (tx != null)
						tx.rollback();
					
					Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error refreshing feed " + fd.getId(), e);
				}
			}
		}, 0, TimeUnit.SECONDS);
	}
	
	private void refreshFeed(Session sess, FeedData fd) {
		try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(fd.getUrl())));
            
            List<SyndEntryImpl> entries = feed.getEntries();
            
            for (SyndEntryImpl entry : entries) {
                FeedItemData data;
                String guid = entry.getUri();
                String link = entry.getLink();
                
                data = (FeedItemData) sess.createQuery("from FeedItemData where feedData.id = :fd and guid = :guid")
						.setInteger("fd", fd.getId()).setString("guid", guid).uniqueResult();
                
                // Entirely new entry
                if (data == null) {
                    data = new FeedItemData();
                    data.setFeedData(fd);
                    data.setGuid(guid);
                    data.setLink(link);
                    data.setTitle(entry.getTitle());
                    data.setText(entry.getDescription().getValue());
                    data.setDate(new java.sql.Timestamp(entry.getPublishedDate().getTime()));
                    data.setAuthor(entry.getAuthor());
                    
                } else { // Update existing entry if changed
                    if (data.getDate().getTime() != entry.getPublishedDate().getTime())
                        data.setDate(entry.getPublishedDate());
                    if (!data.getText().equals(entry.getDescription().getValue()))
                        data.setText(entry.getDescription().getValue());
                    if (!data.getAuthor().equals(entry.getAuthor()))
                        data.setAuthor(entry.getAuthor());
                    if (!data.getTitle().equals(entry.getTitle()))
                        data.setTitle(entry.getTitle());
                }
                
                sess.saveOrUpdate(data);
                
            }
        } catch (IOException | FeedException e) {
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error retrieving feed from " + fd.getUrl(), e);
        }

	}
}
