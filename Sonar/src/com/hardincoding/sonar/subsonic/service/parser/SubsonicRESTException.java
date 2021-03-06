package com.hardincoding.sonar.subsonic.service.parser;

/**
 * @author Sindre Mehus
 * @version $Id: SubsonicRESTException.java 1787 2010-08-27 09:59:06Z sindre_mehus $
 */
public class SubsonicRESTException extends Exception {

	private static final long serialVersionUID = 7111804170931304573L;	
	private final int code;

    public SubsonicRESTException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
