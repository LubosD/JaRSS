/*
 * Copyright (C) 2016 lubos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.dolezel.jarss.data;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 *
 * @author lubos
 */
@Entity
public class Token implements Serializable {
	public static final int TOKEN_VALIDITY = 10*60; // 10 minutes
	
	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;
	
	@Column(unique = true)
	private String value;
	
	@ManyToOne(targetEntity=User.class)
    private User user;
	
	@Temporal(TemporalType.TIMESTAMP)
    private Date expiry;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Date getExpiry() {
		return expiry;
	}

	public void setExpiry(Date expiry) {
		this.expiry = expiry;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Transient
	public static Token loadToken(EntityManager em, String value) {
		Token token;
		Calendar cal = Calendar.getInstance();
		
		try {
			token = (Token) em.createQuery("select t from Token t where value = :value", Token.class)
				.setParameter("value", value).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
			
		if (token.getExpiry().before(cal.getTime()))
			return null; // Token expired, will be reaped later

		cal.add(Calendar.SECOND, Token.TOKEN_VALIDITY);
		token.setExpiry(cal.getTime());

		// Refresh token expiry
		em.persist(token);
		return token;
	}
}
