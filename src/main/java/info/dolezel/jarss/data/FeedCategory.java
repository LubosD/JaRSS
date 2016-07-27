package info.dolezel.jarss.data;

import java.io.Serializable;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public  class FeedCategory implements Serializable {


    @Basic
    private String name;


    @OneToMany(targetEntity=Feed.class,mappedBy="feedCategory")
    private Collection<Feed> feeds;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @ManyToOne(targetEntity=User.class)
    private User user;

    public FeedCategory(){

    }


   public String getName() {
        return this.name;
    }


  public void setName (String name) {
        this.name = name;
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



   public User getUser() {
        return this.user;
    }


  public void setUser (User user) {
        this.user = user;
    }

}

