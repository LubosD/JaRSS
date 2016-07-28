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
package info.dolezel.jarss.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import org.jsoup.Jsoup;

/**
 *
 * @author lubos
 */
public final class StringUtils {
	private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final SecureRandom rnd = new SecureRandom();
	
	private StringUtils() {
	}
	
	public static String html2text(String html) {
		return Jsoup.parse(html).text();
	}

	public static String calculateHash(String password, String salt) throws NoSuchAlgorithmException {
		MessageDigest crypt;
		crypt = MessageDigest.getInstance("SHA-1");
		crypt.update(password.getBytes());
		crypt.update(salt.getBytes());
		return DatatypeConverter.printHexBinary(crypt.digest());
	}

	public static String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		}
		return sb.toString();
	}
}
