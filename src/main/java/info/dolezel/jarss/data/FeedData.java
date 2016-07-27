package info.dolezel.jarss.data;

import java.io.Serializable;

import java.sql.Timestamp;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@NamedQuery(name="FeedData.getByUrl",query="SELECT data FROM FeedData data where data.url = :url")
@Table(indexes = { @Index(columnList = "url") })
public  class FeedData implements Serializable {


    @OneToMany(fetch=FetchType.LAZY,targetEntity=FeedItemData.class,mappedBy="feedData",cascade = CascadeType.REMOVE)
    private Collection<FeedItemData> feedItemData;


    @Basic
    private String lastError;


    @OneToMany(targetEntity=Feed.class,mappedBy="data")
    private Collection<Feed> feeds;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @Basic
    private String title;


    @Basic
    private Timestamp lastFetch;

    private String url;


    @Lob
    @Basic(fetch=FetchType.LAZY)
    private byte[] iconData;

    public FeedData(){

    }


   public Collection<FeedItemData> getFeedItemData() {
        return this.feedItemData;
    }


  public void setFeedItemData (Collection<FeedItemData> feedItemData) {
        this.feedItemData = feedItemData;
    }



   public String getLastError() {
        return this.lastError;
    }


  public void setLastError (String lastError) {
        this.lastError = lastError;
    }



   public Collection<Feed> getFeeds() {
        return this.feeds;
    }


  public void setFeeds (Collection<Feed> feeds) {
        this.feeds = feeds;
    }



   public int getId() {
        return this.id;
    }


  public void setId (int id) {
        this.id = id;
    }



   public String getTitle() {
        return this.title;
    }


  public void setTitle (String title) {
        this.title = title;
    }



   public Timestamp getLastFetch() {
        return this.lastFetch;
    }


  public void setLastFetch (Timestamp lastFetch) {
        this.lastFetch = lastFetch;
    }



   public String getUrl() {
        return this.url;
    }


  public void setUrl (String url) {
        this.url = url;
    }



   public byte[] getIconData() {
        return this.iconData;
    }


  public void setIconData (byte[] iconData) {
        this.iconData = iconData;
    }

}

