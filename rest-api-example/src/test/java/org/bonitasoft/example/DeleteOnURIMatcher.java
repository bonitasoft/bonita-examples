/**
 * 
 */
package org.bonitasoft.example;


/**
 * @author Nicolas Chabanoles
 * 
 */
public class DeleteOnURIMatcher extends URIMatcher {

	public DeleteOnURIMatcher(String uri) {
		super(Verb.DELETE, uri);
	}

}