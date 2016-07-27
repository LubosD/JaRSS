package info.dolezel.jarss.data;

import java.io.Serializable;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public  class Tag implements Serializable {


    @Basic
    private int color;


    @Basic
    private String name;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @ManyToMany(targetEntity=FeedItem.class,mappedBy="tags")
    private Collection<FeedItem> feedItems;

    public Tag(){

    }


   public int getColor() {
        return this.color;
    }


  public void setColor (int color) {
        this.color = color;
    }



   public String getName() {
        return this.name;
    }


  public void setName (String name) {
        this.name = name;
    }



   public int getId() {
        return this.id;
    }


  public void setId (int id) {
        this.id = id;
    }



   public Collection<FeedItem> getFeedItems() {
        return this.feedItems;
    }


  public void setFeedItems (Collection<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

}

