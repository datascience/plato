package eu.scape_project.pw.planning.utils;

import junit.framework.Assert;

import org.junit.Test;

import eu.scape_project.planning.utils.JGet;

public class JGetTest {
	@Test
	public void testConnection(){
		try {
			String content = JGet.wget("http://www.google.com");
			Assert.assertFalse(content.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not connect to google.com");
		}
	}

}
