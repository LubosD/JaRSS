/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest.v1;

import info.dolezel.jarss.HibernateUtil;
import info.dolezel.jarss.data.Feed;
import info.dolezel.jarss.data.FeedCategory;
import info.dolezel.jarss.data.FeedData;
import info.dolezel.jarss.data.FeedItem;
import info.dolezel.jarss.data.FeedItemData;
import info.dolezel.jarss.data.User;
import info.dolezel.jarss.rest.v1.entities.ArticleHeadlineData;
import info.dolezel.jarss.rest.v1.entities.ErrorDescription;
import info.dolezel.jarss.rest.v1.entities.FeedCategoryData;
import info.dolezel.jarss.util.StringUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author lubos
 */
@Path("categories")
@RolesAllowed("user")
public class FeedCategoryService {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll(@Context SecurityContext context) {
		List<FeedCategoryData> data;
		List<info.dolezel.jarss.data.FeedCategory> list;
		Session session;
		Transaction tx;
		User user = (User) context.getUserPrincipal();
		
		session = HibernateUtil.getSessionFactory().openSession();
		tx = session.beginTransaction();
		list = session.createQuery("from FeedCategory where user = :user")
				.setEntity("user", user).list();
		
		data = new ArrayList<>(list.size());
		
		for (info.dolezel.jarss.data.FeedCategory fc : list) {
			FeedCategoryData json = new FeedCategoryData();
			json.setId(fc.getId());
			json.setName(fc.getName());
			
			data.add(json);
		}
		
		return Response.ok(data).build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createNew(@Context SecurityContext context, @Context HttpServletRequest request, FeedCategoryData fc) throws Exception {
		Session session;
		Transaction tx;
		User user = (User) context.getUserPrincipal();
		FeedCategory newCat = new FeedCategory();
		URL url;
		
		session = HibernateUtil.getSessionFactory().openSession();
		tx = session.beginTransaction();
		
		if (fc.getName() == null || fc.getName().isEmpty()) {
			tx.rollback();
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDescription("Invalid category name")).build();
		}
		
		newCat.setName(fc.getName());
		newCat.setUser(user);
		
		session.save(newCat);
		
		tx.commit();
		
		url = new URL(request.getRequestURL().append('/').append(newCat.getId()).toString());
		
		return Response.created(url.toURI()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}/headlines")
	public Response getArticleHeadlines(@Context SecurityContext context, @PathParam("id") int feedId, @QueryParam("skip") int skip, @QueryParam("limit") int limit) {
		List<Tuple> articles;
		TypedQuery<Tuple> query;
		ArticleHeadlineData[] result;
		FeedCategory fc;
		User user;
		CriteriaQuery<Tuple> criteria;
		javax.persistence.criteria.Path<FeedItemData> fid;
		
		EntityManager em = HibernateUtil.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		
		user = (User) context.getUserPrincipal();
		criteria = builder.createTupleQuery();
		
		if (feedId > 0) {
			fc = em.find(FeedCategory.class, feedId);
			if (fc == null) {
				em.close();
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed category does not exist")).build();
			}
			if (!fc.getUser().equals(user)) {
				em.close();
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}
			
			Root<FeedCategory> rootFC = criteria.from(FeedCategory.class);
			Join<FeedCategory, Feed> joinFeed = rootFC.join("feeds", JoinType.INNER);
			Join<Feed, FeedData> joinFeedData = joinFeed.join("data", JoinType.INNER);
			Join<FeedData, FeedItemData> joinFeedItemData = joinFeedData.join("feedItemData", JoinType.INNER);
			Join<FeedItemData, FeedItem> joinFeedItem = joinFeedItemData.join("feedItems", JoinType.LEFT); // left outer
			
			fid = joinFeedItemData;
			criteria.multiselect(joinFeedItemData.alias("fid"), joinFeedItem.alias("fi"));
			criteria.where(builder.equal(rootFC, fc));
			

			/*query = session.createQuery("SELECT fid, fi FROM FeedCategory fc LEFT JOIN fc.feeds AS f LEFT JOIN f.data AS fd "
					+ "LEFT JOIN fd.feedItemData AS fid LEFT OUTER JOIN fid.feedItems AS fi WHERE fc = :fc "
					+ "ORDER BY fid.date DESC")
					.setEntity("fc", fc);*/
		} else {
			Root<Feed> rootF = criteria.from(Feed.class);
			Join<Feed, FeedData> joinFD = rootF.join("data", JoinType.INNER);
			Join<FeedData, FeedItemData> joinFID = joinFD.join("feedItemData", JoinType.INNER);
			Join<FeedItemData, FeedItem> joinFI = joinFID.join("feedItems", JoinType.LEFT); // left outer
			List<Predicate> predicates = new ArrayList<>(2);
			
			fid = joinFID;
			criteria.multiselect(joinFID.alias("fid"), joinFI.alias("fi"));
			
			predicates.add(builder.equal(rootF.get("user"), user));
			
			switch (feedId) {
				case -1: // starred
					predicates.add(builder.isTrue(joinFI.get("starred")));
					break;
				case -2: // published
					predicates.add(builder.isTrue(joinFI.get("published")));
					break;
				case -4: // all
					break; // nothing to do
				default:
					em.close();
					return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed category does not exist")).build();
			}
			
			criteria.where(predicates.toArray(new Predicate[predicates.size()]));
			
			/*else if (feedId == -1) { // starred
			query = session.createQuery("SELECT fid, fi FROM Feed f LEFT JOIN f.data AS fd "
					+ "LEFT JOIN fd.feedItemData AS fid LEFT OUTER JOIN fid.feedItems AS fi WHERE f.user = :user "
					+ "AND fi.starred = true ORDER BY fid.date DESC")
					.setEntity("user", user);
		} else if (feedId == -2) { // published
			query = session.createQuery("SELECT fid, fi FROM Feed f LEFT JOIN f.data AS fd "
					+ "LEFT JOIN fd.feedItemData AS fid LEFT OUTER JOIN fid.feedItems AS fi WHERE f.user = :user "
					+ "AND fi.exported = true ORDER BY fid.date DESC")
					.setEntity("user", user);
		} else if (feedId == -4) { // all
			query = session.createQuery("SELECT fid, fi FROM Feed f LEFT JOIN f.data AS fd "
					+ "LEFT JOIN fd.feedItemData AS fid LEFT OUTER JOIN fid.feedItems AS fi WHERE f.user = :user "
					+ "ORDER BY fid.date DESC")
					.setEntity("user", user);
		*/
		//} else {
		}
		
		criteria.orderBy(builder.desc(fid.get("date")));
		query = em.createQuery(criteria);
		query.setFirstResult(skip);
		
		if (limit > 0)
			query.setMaxResults(limit);
		
		articles = query.getResultList();
		
		result = new ArticleHeadlineData[articles.size()];
		for (int i = 0; i < articles.size(); i++) {
			FeedItemData article = articles.get(i).get("fid", FeedItemData.class);
			FeedItem feedItem = articles.get(i).get("fi", FeedItem.class);
			
			ArticleHeadlineData data = new ArticleHeadlineData();
			String text;
			
			data.setPublished(article.getDate().getTime());
			data.setTitle(article.getTitle());
			data.setId(article.getId());
			
			text = StringUtils.html2text(article.getText());
			
			if (text.length() > 130)
				text = text.substring(0, 130);
			
			data.setExcerpt(text);
			data.setLink(article.getLink());
			
			if (feedItem != null) {
				data.setRead(feedItem.isRead());
				data.setStarred(feedItem.isStarred());
			}
			
			result[i] = data;
		}
		
		em.close();
		return Response.ok(result).build();
	}
	
	@POST
	@Path("{id}/markAllRead")
	public Response markAllRead(@Context SecurityContext context, @PathParam("id") int fcId, @QueryParam("allBefore") long timeMillis) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		User user;
		
		user = (User) context.getUserPrincipal();
		
		if (fcId > 0) {
			FeedCategory fc;
			
			fc = session.get(FeedCategory.class, fcId);
			if (fc == null) {
				return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDescription("Feed category does not exist")).build();
			}
			if (!fc.getUser().equals(user)) {
				return Response.status(Response.Status.FORBIDDEN).entity(new ErrorDescription("Feed not owned by user")).build();
			}
			
			session.createQuery("UPDATE TABLE Feed f LEFT JOIN f.feedCategory AS fc SET readAllBefore = :date WHERE fc = :fc")
					.setEntity("fc", fc)
					.setDate("date", new Date(timeMillis))
					.executeUpdate();
			session.createQuery("DELETE FROM FeedItem fi LEFT JOIN fi.feed AS f LEFT JOIN f.feedCategory AS fc WHERE fc = :fc AND fi.data.date < :date and not fi.starred and not fi.exported and size(fi.tags) = 0")
					.setEntity("fc", fc)
					.setDate("date", new Date(timeMillis))
					.executeUpdate();
		} else {
			// TODO
		}
		
		tx.commit();
		return Response.noContent().build();
	}
}
