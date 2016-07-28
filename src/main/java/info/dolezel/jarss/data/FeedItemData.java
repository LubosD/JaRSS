package info.dolezel.jarss.data;

import java.io.Serializable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(indexes = { @Index(columnList = "date") })
public  class FeedItemData implements Serializable {


    @Temporal(TemporalType.TIMESTAMP)
    private Date date;


    @ManyToOne(targetEntity=FeedData.class)
    private FeedData feedData;


    @Basic
    private String author;


    @Basic
    private String link;


    @Basic
    private String guid;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @Column(columnDefinition="TEXT")
    @Basic
    private String text;


    @Basic
    private String title;


    @OneToMany(targetEntity=FeedItem.class,mappedBy="data")
    private Collection<FeedItem> feedItems;
	
	@OneToMany(cascade = CascadeType.REMOVE, mappedBy="feedItem", targetEntity=FeedItemEnclosure.class)
	private Collection<FeedItemEnclosure> enclosures;

    public FeedItemData(){

    }

	public FeedItemData(int id) {
		this.id = id;
	}


   public Date getDate() {
        return this.date;
    }


  public void setDate (Date date) {
        this.date = date;
    }



   public FeedData getFeedData() {
        return this.feedData;
    }


  public void setFeedData (FeedData feedData) {
        this.feedData = feedData;
    }



   public String getAuthor() {
        return this.author;
    }


  public void setAuthor (String author) {
        this.author = author;
    }



   public String getLink() {
        return this.link;
    }


  public void setLink (String link) {
        this.link = link;
    }



   public String getGuid() {
        return this.guid;
    }


  public void setGuid (String guid) {
        this.guid = guid;
    }



   public int getId() {
        return this.id;
    }


  public void setId (int id) {
        this.id = id;
    }



   public String getText() {
        return this.text;
    }


  public void setText (String text) {
        this.text = text;
    }



   public String getTitle() {
        return this.title;
    }


  public void setTitle (String title) {
        this.title = title;
    }



   public Collection<FeedItem> getFeedItems() {
        return this.feedItems;
    }


  public void setFeedItems (Collection<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

	public Collection<FeedItemEnclosure> getEnclosures() {
		return enclosures;
	}

}

