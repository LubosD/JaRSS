package info.dolezel.jarss.data;

import java.io.Serializable;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public  class FeedItem implements Serializable {


    @ManyToOne(targetEntity=Feed.class)
    private Feed feed;


    @Column(nullable=false)
    @Basic
    private boolean starred;


    @ManyToOne(targetEntity=FeedItemData.class)
    private FeedItemData data;


    @Column(nullable=false)
    @Basic
    private boolean isRead;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @ManyToMany(targetEntity=Tag.class)
    private Collection<Tag> tags;

    public FeedItem(){

    }


   public Feed getFeed() {
        return this.feed;
    }


  public void setFeed (Feed feed) {
        this.feed = feed;
    }



    public boolean isStarred() {
        return this.starred;
    }


  public void setStarred (boolean starred) {
        this.starred = starred;
    }



   public FeedItemData getData() {
        return this.data;
    }


  public void setData (FeedItemData data) {
        this.data = data;
    }



    public boolean isIsRead() {
        return this.isRead;
    }


  public void setIsRead (boolean isRead) {
        this.isRead = isRead;
    }



   public int getId() {
        return this.id;
    }


  public void setId (int id) {
        this.id = id;
    }



   public Collection<Tag> getTags() {
        return this.tags;
    }


  public void setTags (Collection<Tag> tags) {
        this.tags = tags;
    }

}

