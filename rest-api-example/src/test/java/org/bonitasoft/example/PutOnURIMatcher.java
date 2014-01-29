/**
 * 
 */
package org.bonitasoft.example;


/**
 * @author Nicolas Chabanoles
 *
 */
public class PutOnURIMatcher extends URIMatcher {


	public PutOnURIMatcher(String uri) {
		super(Verb.PUT, uri);
	}
}