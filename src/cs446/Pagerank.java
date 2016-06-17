package cs446;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class Pagerank {

	HashMap<String, ArrayList<String>> adjacency_list = new HashMap<String, ArrayList<String>>();
	HashMap<String, Integer> inv_adjacency_list = new HashMap<String, Integer>();
	HashMap<String, Double> adjacency_rankscore = new HashMap<String,Double>();
	HashMap<String, Double> adjacency_prescore = new HashMap<String,Double>();
	HashMap<String, Double> adjacency_initscore = new HashMap<String,Double>();
	double threshold = 0.01;
	double lambda = 0.15;
	
	public Pagerank(double lambda,double threshold,String file) throws IOException{
		this.threshold = threshold;
		this.lambda = lambda;
		this.read_graph(file);
	}
	
	public void run(){
		//set each page equally likely.
		//line7
		init_adj_lists();
		System.out.println("before: " +this.adjacency_rankscore.entrySet().toArray()[0]);
				
		//start pagerank
		this.start_pagerank();
		
		
	}
	public void start_pagerank(){

		//start page rank
		boolean converged = false;
		while(!converged){
			//store last rounds's score, and check for converge at the end 
			HashMap<String, Double> adjacency_tminus1score = new HashMap<String,Double>(adjacency_prescore);
						
			// reinit tempscore for each page
			//line11
			this.adjacency_prescore = new HashMap<String, Double>(this.adjacency_initscore);
			
			//for each outgoing link, check if it exists as a page, if so add update pre-rankscore
			//line13
			for (Map.Entry<String, ArrayList<String>> page_list: this.adjacency_list.entrySet()){
				ArrayList<String> subset_exists = new ArrayList<String>();
				ArrayList<String> subset_notexists = new ArrayList<String>();
	
				//if each outgoing links also exists in P, add to subsetlist Q
				for (String link : page_list.getValue()){
						if (this.adjacency_list.get(link).size() != 0){
							subset_exists.add(link);
						}
						else
							subset_notexists.add(link);
					}
			
				for (String qlink : subset_exists){
					//if (p,q)->L, pagelink also exist as outgoing link
					//line14
					if(this.adjacency_list.containsKey(qlink)){
						//line17 Rq <- Rq +(1−λ)Ip/|Q|
						double score =this.adjacency_prescore.get(qlink) +
								(1-lambda) * this.adjacency_rankscore.get(page_list.getKey()) / subset_exists.size();
						this.adjacency_prescore.put(qlink,score);
					}
				}
				for (String qlink : subset_notexists){
					// add random sinks for all the other pages that is not in qlink
					//line21 
					if(this.adjacency_list.containsKey(qlink)){
						double score =this.adjacency_prescore.get(qlink) +
								(1-lambda) * this.adjacency_rankscore.get(page_list.getKey()) / this.adjacency_prescore.size();
						this.adjacency_prescore.put(qlink,score);
					}
				}
				
				//line24 update temp score into real rank score
				//System.out.println(this.adjacency_prescore);
				this.adjacency_rankscore = this.adjacency_prescore;
				
			}//end for each page

			//line9
			converged = check_converge(this.adjacency_prescore,adjacency_tminus1score, this.threshold);
			
		}//end while pagerank		
		
	}
	
	
	//helper read graph
	public void read_graph(String filename) throws IOException{

		File file = new File(System.getProperty("user.dir") + "/"+ filename);
		BufferedReader in = new BufferedReader(new InputStreamReader(
		                      new FileInputStream(file), "UTF8"));
	    String line = in.readLine();
	    while (line!=null) {
	    	String[] items = line.split("\t");
	    	//add pages
	    	if (this.adjacency_list.get(items[0]) == null){
	    		ArrayList<String> newlist = new ArrayList<String>();
	    		newlist.add(items[1]);
		    	this.adjacency_list.put(items[0], newlist );
		    	this.adjacency_rankscore.put(items[0], 0.0);
		    	this.adjacency_prescore.put(items[0], 0.0);
		    	this.adjacency_initscore.put(items[0], 0.0);
	    	}
	    	else{
	    		ArrayList<String> list = this.adjacency_list.get(items[0]);
	    		list.add(items[1]);
	    	}
	    	//add links as pages
	    	if (this.adjacency_list.get(items[1]) == null){
	    		ArrayList<String> newlist = new ArrayList<String>();
		    	this.adjacency_list.put(items[1], newlist );
		    	this.adjacency_rankscore.put(items[1], 0.0);
		    	this.adjacency_prescore.put(items[1], 0.0);
		    	this.adjacency_initscore.put(items[1], 0.0);
	    	}
	    	
	    	//count incomming links count
	    	if (this.inv_adjacency_list.get(items[1]) == null){
		    	this.inv_adjacency_list.put(items[1], 1);
	    	}
	    	else{
		    	this.inv_adjacency_list.put(items[1], this.inv_adjacency_list.get(items[1])+1);
	    	}
	    	
	    	line = in.readLine();
	    }
	    in.close();
	}	
		
	//helper check if current values are conversed for pagerank
	public static boolean check_converge(HashMap<String, Double> adjlist1,HashMap<String, Double> adjlist2,Double threshold){
		boolean converged = true;
		double results =0;
		//check for norm
		for (Entry<String, Double> page_score : adjlist1.entrySet()){
			results = Math.pow(adjlist2.get(page_score.getKey()) - page_score.getValue(),2);
			
		}
		results = Math.pow(results,0.5);
			
		if (results <threshold){
				converged = true;
			}
			else {
				converged = false;
			}
		
		return converged;
	}
	
	//helper init the pre pagerank by average.
	public void init_adj_lists(){
		this.adjacency_initscore.forEach((k,v) 
				-> this.adjacency_initscore.put(k, this.lambda/this.adjacency_initscore.size()) );
		this.adjacency_rankscore = this.adjacency_initscore;
	}
	
	//run output codes
	public  void run_output() throws IOException{
		//output tope 50 incomming links
		ArrayList<String> results = output_top50(this.inv_adjacency_list,50);
		System.out.println(results);
		this.output_stats(results,"inlink",0);
		
		System.out.println("after: " +this.adjacency_rankscore.entrySet().toArray()[0]);
		
		//output tope 50 incomming links
		ArrayList<String> results2 = output_top50_float(this.adjacency_rankscore,50);
		System.out.println(results2);
		this.output_stats(results2,"pagerank",0);
	}
	
	//helper output the count of outgoing pages
	public static  ArrayList<String>  output_top50(HashMap<String,Integer> map,int top_n){
		Set<Entry<String, Integer>> set = map.entrySet();
		List<Entry<String, Integer>> list2 = new ArrayList<>(set);
		Collections.sort(list2, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
				return b.getValue() - a.getValue();
			}
		});

		//output the top 50
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < top_n && i < list2.size(); i++) {
			results.add(list2.get(i).toString());
		}
		
		return results;
	}
	
	//helper conver the hashmap to page rank score
	public static  ArrayList<String>  output_top50_float(HashMap<String,Double> map,int top_n){
		Set<Entry<String, Double>> set = map.entrySet();
		List<Entry<String, Double>> list2 = new ArrayList<>(set);
		Collections.sort(list2, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> a, Entry<String, Double> b) {
				return (int)(b.getValue() - a.getValue());
			}
		});

		//output the top 50
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < top_n && i < list2.size(); i++) {
			results.add(list2.get(i).toString());
		}
		
		return results;
	}
	
	//save outputs
	public void output_stats(ArrayList<String> list,String name, int limit) throws IOException{
		int printlimit = list.size();
		String text = "";
		
		if(limit>0){
			printlimit = limit;
		}
		
		List<String> newList = new ArrayList<>(list.subList(0,printlimit));
		
		for (int i = 0; i < printlimit; i++) {
			int rank = i+1;
			text += newList.get(i) +" "+rank + "\n";
		}
		text = text.trim();
		BufferedWriter out = new BufferedWriter(new FileWriter("outputs/"+name + ".txt"));
		out.write(text);
		out.close();
}

	
	//*******************************************************************
	

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		
		double lambda = 0.15;
		double tau = 0.01; //0.01
		String file = "inputs/links.srt";//"links.srt""testpagerank.srt"
		Pagerank instance = new Pagerank(lambda,tau,file);
		
		Object[] items = instance.adjacency_list.entrySet().toArray();
		System.out.println("sample adj list link");
		System.out.println(items[instance.adjacency_list.values().toArray().length-1]);
		System.out.println("P pages: " + instance.adjacency_list.size());
		
		//start the whole page rank process
		instance.run();
		
		// saves data
		instance.run_output();
		
		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println(estimatedTime/1000 + " seconds");	
		
		

		System.out.println("index page rank:" + instance.adjacency_rankscore.get("index"));
		
	}//end main


}
