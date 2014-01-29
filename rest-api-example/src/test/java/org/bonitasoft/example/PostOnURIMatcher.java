/**
 * 
 */
package org.bonitasoft.example;


/**
 * @author Nicolas Chabanoles
 *
 */
public class PostOnURIMatcher extends URIMatcher {

	public PostOnURIMatcher(String uri) {
		super(Verb.POST, uri);
	}
	
}
