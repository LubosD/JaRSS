package info.dolezel.jarss.data;

import java.io.Serializable;

import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public  class Feed implements Serializable {


    @ManyToOne(targetEntity=FeedData.class)
    private FeedData data;


    @Column(nullable=false)
    @Basic
    private String name;


    @ManyToOne(targetEntity=FeedCategory.class)
    private FeedCategory feedCategory;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @Temporal(TemporalType.TIMESTAMP)
    private Date readAllBefore;


    @ManyToOne(targetEntity=User.class)
    private User user;


    @OneToMany(targetEntity=FeedItem.class,mappedBy="feed")
    private Collection<FeedItem> feedItems;

    public Feed(){

    }


   public FeedData getData() {
        return this.data;
    }


  public void setData (FeedData data) {
        this.data = data;
    }



   public String getName() {
        return this.name;
    }


  public void setName (String name) {
        this.name = name;
    }



   public FeedCategory getFeedCategory() {
        return this.feedCategory;
    }


  public void setFeedCategory (FeedCategory feedCategory) {
        this.feedCategory = feedCategory;
    }



   public int getId() {
        return this.id;
    }


  public void setId (int id) {
        this.id = id;
    }



   public Date getReadAllBefore() {
        return this.readAllBefore;
    }


  public void setReadAllBefore (Date readAllBefore) {
        this.readAllBefore = readAllBefore;
    }



   public User getUser() {
        return this.user;
    }


  public void setUser (User user) {
        this.user = user;
    }



   public Collection<FeedItem> getFeedItems() {
        return this.feedItems;
    }


  public void setFeedItems (Collection<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

}

