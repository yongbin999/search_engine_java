package cs446;

import static org.junit.Assert.*;

import org.junit.Test;

public class webcrawlerTest {

	webcrawler cw = new webcrawler(100, "http://ciir.cs.umass.edu");
	
	@Test
	public void testTobe_visit_link() {
		fail("Not yet implemented");
	}

	@Test
	public void testAlready_seen() {
		
		assertTrue(!cw.links_queue.isEmpty());

		cw.parser(cw.links_queue,"http://ciir.cs.umass.edu");
		cw.parser(cw.links_queue,"http://ciir.cs.umass.edu");
		System.out.println("@Test testAlready_seen(): link size "+ cw.links_queue.size());
		assertEquals(cw.links_queue.size(), 13);
		
	}

	@Test
	public void testCheck_robots() {
		fail("Not yet implemented");
	}

	@Test
	public void testGet_robots() {
		fail("Not yet implemented");
	}

	@Test
	public void testCheck_sitemap() {
		fail("Not yet implemented");
	}

	@Test
	public void testGet_sitemap() {
		fail("Not yet implemented");
	}

	@Test
	public void testGet_header() {
		fail("Not yet implemented");
	}

	@Test
	public void testGet_response() {
		fail("Not yet implemented");
	}

}
