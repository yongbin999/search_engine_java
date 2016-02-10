package cs446;

import java.io.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class webcrawler extends Thread {
	int maxlinks;
	int numofdocs;
	int numof_uniquelinks;
	
	//using standard user agent to honor the robots.txt
	String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";

	// keep track of links already visited, and if came from a sitemap
	HashMap<String, Boolean> hash_links = new HashMap<String, Boolean>();
	// queue of links to be parsed
	Queue<String> links_queue = new LinkedList<String>(); // new queue
	// // inverted index
	// HashMap<String,String[]> inverted_index = new HashMap<String,String[]>();
	
	Set<String> sitemaps =new HashSet<String>(); 
	Set<String> siterobots =new HashSet<String>(); 
	ArrayList<String> robots_restrictlist =new ArrayList<String>(); 
	
	
	//constructor for webcralwer
	public webcrawler(int maxlinks, String seed_link) {
		this.maxlinks = maxlinks;
		links_queue.add(seed_link);
	}
	
	// can create multi thread crawlers ##not working without syncronized linkedlist!
	public void run() {
		this.crawl();
	}

	/*
	 * start crawling from the queue of links
	 */
	public void crawl() {
		// stop when queue empty of reach max link
		while (!links_queue.isEmpty() && numof_uniquelinks < maxlinks) {
			String url = nextlink(links_queue);

			System.out.println("crawling " + url);
			parser(links_queue, url);
		
			// wait 5 seconds before dequeue for next link
			try {
				System.out.println("\t wait 5 sec, current unique links: " + hash_links.size());
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	/*
	 * main parser on url, first step to run
	 */
	public void parser(Queue<String> links_queue, String url) {

		// get the robots and add to our restricted list
		add_robot_restrict(url);
		
		// get sitemap and add all the unique links 
		add_sitemap_links(url);
		

		// connect to the site and parse the doc for links
		try {
			Connection connection = Jsoup.connect(url).followRedirects(true); //.userAgent(USER_AGENT);
			Response res = connection.execute();
				
			if (connection.response().statusCode() == 200) {
				Document doc = res.parse();
				parser_link(links_queue, doc);
				// parser_text(doc);
				this.numofdocs += 1;
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("cant make the connection");
		}
	}

	/*
	 * parser part that retrieve links and checks their header for html or pdf
	 * */
	public void parser_link(Queue<String> links_queue, Document doc) {
	
		try {
			Elements links = doc.select("a[href]");
			for (Element link : links) {
				if (numof_uniquelinks > maxlinks){
					break;
				}
				
				String linkstring = link.absUrl("href");
				validate_add_url(linkstring);
				
				//speed up crawling by getting the sitemap of outgoing links 
				//add_sitemap_links(linkstring);
				
			}
			
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("cant get any links");
		}

	}

	/*
	 * to be used to create the inverted index
	 * */
	public void parser_text(Document doc) {
		// parse links save to index
		String bodyText = doc.body().text();
	}

	
	// ***************************supporting helper methods********************************
	/*
	 * if there is link in queue return next.
	 */
	public String nextlink(Queue<String> links_queue) {
		return links_queue.poll();
	}

	/*
	 * add links from parsing the doc to the visited list and the to queue to be
	 * process
	 */
	public void tobe_visit_link(String url) {
		this.links_queue.add(url);
		this.hash_links.put(url, false);
		this.numof_uniquelinks += 1;
	}

	/*
	 * check if link exists in hashmap, and variations of the http/https/www
	 */
	public boolean already_seen(String url) {

		String stem_url = "";
		String stem_url2 = "";
		
		if (!url.contains("www.")) {
			stem_url = url.substring(0, url.indexOf("//"))+ 
					"www."+ url.substring(url.indexOf("//")+2,url.length());
		}
		else if(url.contains("www.")){
			stem_url = url.substring(0, url.indexOf("//"))+ 
					url.substring(url.indexOf("www.")+4,url.length());
		}
		if (url.contains("https")) {
			stem_url2 = "http"+url.substring(url.indexOf("//"),url.length());
		}else if (url.contains("http")) {
			stem_url2 = "https"+url.substring(url.indexOf("//"),url.length());
		}
		
		if (this.hash_links.get(url) == null || 
				this.hash_links.get(stem_url) == null ||
				this.hash_links.get(stem_url2) == null) {
			
			// check simularity using simhash

			return false;
		} else {
			return true;
		}
	}

	/*
	 * stem or stop some of the url, for ? param forms and # css id tags 
	 */
	public String stem_url(String url) {
		String stem_url = url;

		if (stem_url.contains("?")) {
			stem_url = url.substring(0, stem_url.indexOf("?"));
		} else if (stem_url.contains("#")) {
			stem_url = url.substring(0, stem_url.indexOf("#"));
		}
		
		return stem_url;
	}
	
	/*
	 * largest part of the program, getting the header from pages and save if its html/pdf and unique
	 * */
	public void validate_add_url(String linkstring) {
		String stem_url = stem_url(linkstring);
		
		//Jsoup doesnt connect to PDFs so i just manuall pull any pdf files to links
		if (stem_url.contains(".pdf")){
			tobe_visit_link(stem_url); 
		}
		else if(stem_url.contains("mailto:")||stem_url.contains(".jpg") ){	
		}
		else{
			try {			
				//check for header then respect the robots
				Connection connection = Jsoup.connect(stem_url).followRedirects(true); //.userAgent(USER_AGENT);
				Response res = connection.execute();
				
				String contentType = res.contentType().split("; ")[0];
				if (contentType.equals("text/html") || contentType.equals("application/pdf")) {
					if (already_seen(stem_url) == false && respect_robots(stem_url)) {
						// add new links to the queue
						tobe_visit_link(stem_url); 
	
					}
				}
				
				try {
					Thread.sleep(10); // take small breaks to get the header file
				} 
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				
			} catch (Exception e) {
				System.out.println("");
				//e.printStackTrace();
				System.out.println(stem_url+" - page doesnt exist! or not valid connection");
			}
		}
		
	}



	/*
	 * simhash or simple contains
	 */
	public String get_hashsim(String url) {
		return "";
	}
	
	/*
	 * get the base host web address name
	 * */
	public String get_base_url(String url) {
		String[] link = url.split("//");
		String[] linkbase = link[1].split("/");
		if (linkbase.length >1){
			return "http://"+linkbase[0]+"/";
		}
		else{		
			return "http://"+link[1]+"/";
		}
	}
	/*
	 * check if link allow in robots.txt
	 */
	public Document get_robots(String url) {
		String base= get_base_url(url);
		try {
		Connection connection = Jsoup.connect(base+"robots.txt").followRedirects(true); //.userAgent(USER_AGENT);
		return connection.get();
		}
		catch(Exception e){
			return new Document("");
		}
	}
	//get the links from robots by finding the text location of the start and end
	public ArrayList<String> get_restricted_resources(String baseurl,Document doc) {
	    ArrayList<String> urls = new ArrayList<String>();
	    
	    List<String> body = Arrays.asList(doc.body().text().split(" Disallow: "));
	    //System.out.println((Arrays.asList(body)).toString());
	    int start = body.indexOf("User-agent: *");
	    body = body.subList(start+1, body.size());
	    int end = body.indexOf("User-agent: ");
	    
	    //create sublist of just User agent * rows
	    if (end != -1){
		    body = body.subList(0, end);
	    }
	  
	    // if its contains disallow: then add it.  
	      for (String url : body ) {
	    	  if (url.contains("Allow: ")){
	    		  urls.add( baseurl + url.substring(0,url.indexOf("Allow: ")));
	    	  }
	      }
	      return urls;
	   }
	

	//try to get the robot.txt, if exists, add to our restriction list
	public void add_robot_restrict(String url) {
		String baseurl = get_base_url(url);
		Document robots = get_robots(url);
		if (robots.hasText() && !siterobots.contains(baseurl) ) {
			siterobots.add(baseurl);
			ArrayList<String> restricted_paths = get_restricted_resources(baseurl,robots);
			for (String path : restricted_paths){
				robots_restrictlist.add(path);				
			}
		}
		
	}
	
	//check if the url contains the restricted links from robots
	public boolean respect_robots(String url) {
		for (String path : robots_restrictlist){
			if (url.contains(path)){
				return false;
			}
		}
		return true;
	   }
	

	
	
	/*
	 * check if sitemap exists
	 */
	public ArrayList<String> get_sitemap(String url) {
		String base= get_base_url(url);
		try {
			Connection connection = Jsoup.connect(base+"sitemap.xml").followRedirects(true); //.userAgent(USER_AGENT);
			Document doc= connection.get();		
			return get_sitemapurl(doc);
		}
		catch(Exception e){
			return new ArrayList<String>();
		}
	};
	
	public ArrayList<String> get_sitemapurl(Document doc) {
		    ArrayList<String> urls = new ArrayList<String>();
		      // do whatever you want, for example retrieving the <url> from the sitemap
		      for (Element url : doc.select("url")) {
		         urls.add(url.select("loc").text());
		      }
		      return urls;
		   }
	
	public void add_sitemap_links(String url) {
		String baseurl = get_base_url(url);
		ArrayList<String> sitemap_links = get_sitemap(url);
		
		if (!sitemap_links.isEmpty() && !sitemaps.contains(baseurl) ) {
			sitemaps.add(baseurl);
	
			for  (String sitemap_link : sitemap_links) {
				if (numof_uniquelinks > maxlinks){
					break;
				}
				
				String stem_sitemap_link = stem_url(sitemap_link);
				if (already_seen(stem_sitemap_link) == false && respect_robots(stem_sitemap_link)) {
					validate_add_url(stem_sitemap_link);
				}
				
			}
		}
		
	   }
	
	
	

	public void print_hashlinks() {
		System.out.println("completed crawl of " + maxlinks);

		Set set = this.hash_links.entrySet();
		// Get an iterator
		Iterator i = set.iterator();
		// Display elements
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			System.out.print(me.getKey() + ": ");
			System.out.println(me.getValue());
		}

		System.out.println("*******************");
	}

	public void output_hashlinks() {
		try {
			//System.out.println(this.hash_links.keySet());
			Object[] links = this.hash_links.keySet().toArray();
			String text = "";
			for (int i = 0; i < links.length; i++) {
				text += links[i] + "\n";
			}
			BufferedWriter out = new BufferedWriter(new FileWriter("links" + maxlinks + ".txt"));
			out.write(text);
			out.close();
		} catch (IOException e) {
			System.out.println("Exception ");
		}
	}

	
	
	// *******************************main runner*******************************************
	public static void main(String[] args) {

		int maxlinks = 100; // << set your default crawl max links stop
		if (args.length >= 1) {
			try {
				maxlinks = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("enter a number for the max link to crawl");
			}
		}

		webcrawler cw = new webcrawler(maxlinks, "http://ciir.cs.umass.edu");
		cw.crawl();
		// cw.parser(cw.links_queue,"http://ciir.cs.umass.edu");

		// create new multi thread?
		//(new webcrawler(100,"newseed")).start();
		

		System.out.println("crawled docs: " + cw.numofdocs);
		System.out.println("links on the queue: " + cw.links_queue.size() +"/"+ cw.numof_uniquelinks );
		System.out.println("num of robots found:" + cw.siterobots.size());
		System.out.println("num of sitemap found:" + cw.sitemaps.size());
		cw.output_hashlinks();
		// cw.print_hashlinks();

	}

}
