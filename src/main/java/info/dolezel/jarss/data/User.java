package info.dolezel.jarss.data;

import java.io.Serializable;
import java.security.Principal;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity(name="JarssUser")
@NamedQuery(name="User.getByLogin",query="SELECT u FROM JarssUser u where u.login = :login")
public  class User implements Serializable, Principal {


    @Column(nullable=false)
    @Basic
    private String password;


    @Column(nullable=false)
    @Basic
    private byte[] salt;


    @OneToMany(targetEntity=Feed.class,mappedBy="user")
    private Collection<Feed> feeds;


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;


    @Column(unique=true,nullable=false)
    @Basic
    private String login;


    @Column(nullable=false)
    @Basic
    private String email;


    @OneToMany(targetEntity=Tag.class, cascade = CascadeType.REMOVE)
    private Collection<Tag> tags;


    @OneToMany(targetEntity=FeedCategory.class,mappedBy="user",cascade = CascadeType.REMOVE)
    private Collection<FeedCategory> feedCategories;
	
	private String role;

    public User(){

    }


   public String getPassword() {
        return this.password;
    }


  public void setPassword (String password) {
        this.password = password;
    }



   public byte[] getSalt() {
        return this.salt;
    }


  public void setSalt (byte[] salt) {
        this.salt = salt;
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



   public String getLogin() {
        return this.login;
    }


  public void setLogin (String login) {
        this.login = login;
    }



   public String getEmail() {
        return this.email;
    }


  public void setEmail (String email) {
        this.email = email;
    }



   public Collection<Tag> getTags() {
        return this.tags;
    }


  public void setTags (Collection<Tag> tags) {
        this.tags = tags;
    }



   public Collection<FeedCategory> getFeedCategories() {
        return this.feedCategories;
    }


  public void setFeedCategories (Collection<FeedCategory> feedCategories) {
        this.feedCategories = feedCategories;
    }

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	@Transient
	public String getName() {
		return getLogin();
	}

}

