/**
 * 
 */
package org.bonitasoft.example;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author Nicolas Chabanoles
 *
 */
public class URIMatcher extends BaseMatcher<HttpUriRequest> {

	protected enum Verb {
		GET, PUT, POST, DELETE
	}
	
	private String uri;
	private Verb verb;

	public URIMatcher(Verb verb, String uri) {
		this.verb = verb;
		this.uri = uri;
	}
	
	@Override
	public boolean matches(Object item) {
		if (!(item instanceof HttpRequestBase)) return false;
		
		HttpRequestBase query = (HttpRequestBase) item;
		return verb.name().equals(query.getMethod()) && query.getURI().toString().contains(uri);
		
	}

	@Override
	public void describeTo(Description description) {
		// TODO Auto-generated method stub
		
	}
}