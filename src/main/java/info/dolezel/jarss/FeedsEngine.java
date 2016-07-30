/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import info.dolezel.jarss.data.FeedData;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.data.FeedItemEnclosure;
import info.dolezel.jarss.rest.v1.InitialSetupService;
import info.dolezel.jarss.rest.v1.RestApplication;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
		
		executor.scheduleAtFixedRate(() -> {
			if (InitialSetupService.isConfigured())
				cleanupTokens();
		}, 1, 1, TimeUnit.HOURS);
		
		executor.scheduleAtFixedRate(() -> {
			if (InitialSetupService.isConfigured())
				refreshFeeds();
		}, 1, 15, TimeUnit.MINUTES);
	}

	public static FeedsEngine getInstance() {
		return instance;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		executor.shutdownNow();
		HibernateUtil.shutdown();
		instance = null;
	}
	
	private void cleanupTokens() {
		EntityManager em = HibernateUtil.getEntityManager();
		EntityTransaction tx;
		
		tx = em.getTransaction();
		
		try {
			tx.begin();
			
			em.createQuery("delete from Token where expiry < current_date").executeUpdate();

			tx.commit();
			
			Logger.getLogger(RestApplication.class.getName()).log(Level.FINE, "Deleted expired tokens");
			
		} catch (Exception e) {
			tx.rollback();
			
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error cleaning up tokens", e);
		} finally {
			em.close();
		}
	}
	
	private void refreshFeeds() {
		EntityManager em = HibernateUtil.getEntityManager();
		EntityTransaction tx;
		
		tx = em.getTransaction();
		
		try {
			List<FeedData> fd;
			
			tx.begin();
			
			fd = em.createQuery("select fd from FeedData fd", FeedData.class).getResultList();
			tx.commit();
			
			for (FeedData f : fd) {
				tx.begin();
				refreshFeed(em, f);
				tx.commit();
			}
		} catch (Exception e) {
			if (tx.isActive())
				tx.rollback();
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error refreshing feeds", e);
		} finally {
			em.close();
		}
	}
	
	public void submitFeedRefresh(final FeedData fd) {
		executor.schedule(() -> {
			EntityManager em = HibernateUtil.getEntityManager();
			EntityTransaction tx;
			
			tx = em.getTransaction();
			
			try {
				tx.begin();
				
				refreshFeed(em, fd);
				
				tx.commit();
			} catch (Exception e) {
				if (tx.isActive())
					tx.rollback();
				
				Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error refreshing feed " + fd.getId(), e);
			} finally {
				em.close();
			}
		}, 0, TimeUnit.SECONDS);
	}
	
	private void refreshFeed(EntityManager em, FeedData fd) {
		try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(fd.getUrl()))); // TODO: handle redirects!
			
			updateFeedDetails(fd, feed);
            
            List<SyndEntryImpl> entries = feed.getEntries();
			List<String> guids;
			Map<String, FeedItemData> existing;
			
			// Get a list of entry GUIDs
			guids = entries.stream().map((SyndEntryImpl e) -> { return e.getUri(); }).collect(Collectors.toList());
			
			// Select existing and place them into a map
			existing = em.createQuery("select fid from FeedItemData fid where fid.feedData.id = :fd and fid.guid in (:guids)", FeedItemData.class)
					.setParameter("guids", guids)
					.setParameter("fd", fd.getId())
					.getResultList().stream().collect(Collectors.toMap(FeedItemData::getGuid, Function.identity()));
            
            for (SyndEntryImpl entry : entries) {
                FeedItemData data;
				SyndContent content = entry.getDescription();
                String guid = entry.getUri();
                String link = entry.getLink();
				List<SyndEnclosure> enclosures = entry.getEnclosures();
				
				data = existing.get(guid);
                
                if (data == null) {
                    // Entirely new entry
					
					data = new FeedItemData();
                    data.setFeedData(fd);
                    data.setGuid(guid);
                    data.setLink(link);
                    data.setTitle(entry.getTitle());
                    data.setText(content.getValue());
                    data.setDate(new java.sql.Timestamp(entry.getPublishedDate().getTime()));
                    data.setAuthor(entry.getAuthor());
					
					em.persist(data);
					
					for (SyndEnclosure e : enclosures) {
						FeedItemEnclosure fe = new FeedItemEnclosure();
						fe.setUrl(e.getUrl());
						fe.setType(e.getType());
						fe.setLength(e.getLength());
						fe.setFeedItem(data);
						
						em.persist(fe);
					}
                    
                } else { // Update existing entry if changed
                    if (data.getDate().getTime() != entry.getPublishedDate().getTime())
                        data.setDate(entry.getPublishedDate());
                    if (!data.getText().equals(content.getValue()))
                        data.setText(content.getValue());
                    if (!data.getAuthor().equals(entry.getAuthor()))
                        data.setAuthor(entry.getAuthor());
                    if (!data.getTitle().equals(entry.getTitle()))
                        data.setTitle(entry.getTitle());
					
					em.persist(data);
                }    
                
            }
			
			fd.setLastFetch(new Date());
			fd.setLastError(null);
			
			em.persist(fd);
        } catch (IOException | FeedException e) {
			fd.setLastError(e.getMessage());
			em.persist(fd);
			
			Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, "Error retrieving feed from " + fd.getUrl(), e);
        }

	}
	
	private static boolean stringsEqual(String str1, String str2) {
		if (str1 != null)
			return str1.equals(str2);
		else
			return str2 == null;
	}

	private void updateFeedDetails(FeedData feedData, SyndFeed feed) {
		if (!stringsEqual(feedData.getTitle(), feed.getTitle()))
			feedData.setTitle(feed.getTitle());
		
		if (!stringsEqual(feedData.getWebsiteUrl(), feed.getLink()))
			feedData.setWebsiteUrl(feed.getLink());
		
		if (!stringsEqual(feedData.getDescription(), feed.getDescription()))
			feedData.setDescription(feed.getDescription());
	}
}
