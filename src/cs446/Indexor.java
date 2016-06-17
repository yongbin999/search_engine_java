/**
 * 
 */
package cs446;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author yong
 * indexor
 */
public class Indexor {
	HashMap<String, HashMap<Integer,ArrayList<Integer>>> inverse_index = new HashMap<String, HashMap<Integer,ArrayList<Integer>>>();
	HashMap<Integer,String> docid_docname = new HashMap<Integer,String>();
	HashMap<Integer,String> docid_playname = new HashMap<Integer,String>();
	HashMap<String,Integer> scenename_wordcount = new HashMap<String,Integer>();
	HashMap<String,Integer> playname_wordcount = new HashMap<String,Integer>();
	
	// read the json file into the hashmaps above
	public void readjason(String filename){
		JSONParser parser = new JSONParser();
		try {

			Object obj = parser.parse(new FileReader(filename));
			JSONObject jsonObject = (JSONObject) obj;

			JSONArray slideContent = (JSONArray) jsonObject.get("corpus");
	        Iterator i = slideContent.iterator();

	        while (i.hasNext()) {
	            JSONObject slide = (JSONObject) i.next();
	            int docid = ((Long)slide.get("sceneNum")).intValue();
	            String docname = (String)slide.get("sceneId");
	            this.docid_docname.put(docid,docname);
	            
	            String playname = (String)slide.get("playId");
		        this.docid_playname.put(docid,playname);
	            
	            String text = (String)slide.get("text");
	            String[] list = text.split("\\s+");
	            this.scenename_wordcount.put(docname,list.length);
	            if (this.playname_wordcount.get(playname) ==null){
	            	this.playname_wordcount.put(playname,list.length);
		        }
	            else{
	            	int currentcount =this.playname_wordcount.get(playname);
	            	this.playname_wordcount.put(playname,currentcount+list.length);
	            }
		        
	            int pos = 0;
	            for (String word : list){
	            	HashMap<Integer, ArrayList<Integer>> currentmap =this.inverse_index.get(word);
	            	if (currentmap != null){
	            		//check if doc has a list of posting already
	            		ArrayList<Integer> currentlist = currentmap.get(docid);
	            		if(currentlist != null){
	            			currentlist.add(pos);
	            		}
	            		else{
	            			currentlist = new ArrayList<Integer>();
	            			currentlist.add(pos);
	            		}

	            		currentmap.put(docid, currentlist);
	            	}
	            	else{
	            		ArrayList<Integer> onelist = new ArrayList<Integer>();
	            		onelist.add(pos);
	            		HashMap<Integer, ArrayList<Integer>>  doc_list = new HashMap<Integer, ArrayList<Integer>>() ;
	            		doc_list.put(docid, onelist);
	            		currentmap =doc_list;
	            	}
	            	
	            	this.inverse_index.put(word, currentmap);
	            	pos++;
	            }
	        }
	        
	        
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//question1: terms0.txt Find scene(s) where "thee" or "thou" is used more than "you"
	public HashSet<String> get_scence_bymorecompare(String word, String[] morewords){
		HashSet<String> results = new HashSet<String>();
        HashMap<Integer, ArrayList<Integer>> base = this.inverse_index.get(word);
        
        for(Entry<Integer, ArrayList<Integer>> entry : base.entrySet()) {
            Integer key = entry.getKey();
            Integer base_count = entry.getValue().size();

            //for each moreword, compare and add to set
            for (String moreword : morewords){
            	Integer morewordsize = 0;
                HashMap<Integer, ArrayList<Integer>> morewordcompare = this.inverse_index.get(moreword);
            	if(morewordcompare.get(key) !=null){
            		morewordsize = morewordcompare.get(key).size();
            		if(morewordsize > base_count){
                        String docname = this.docid_docname.get(key);
                    	results.add(docname);
                    }
            	}
            	
            }
            
        }
        return results;
	}
	
	
	//question2 - Find scene(s) where Verona, Rome, or Italy is mentioned.
	public HashSet<String> get_scenes_byname(String[] words){
        HashSet<String> results = new HashSet<String>();

        //for each word, find all and add to set
        for (String word : words){
            HashMap<Integer, ArrayList<Integer>> word_docs = this.inverse_index.get(word);
            for(Entry<Integer, ArrayList<Integer>> entry : word_docs.entrySet()) {
                Integer key = entry.getKey();
                String docname = this.docid_docname.get(key);
                results.add(docname);
            }
        }
        return results;
	}
	//return doc id of the outterjoint docs
	public HashSet<Integer> get_scenes_partitions(String[] words){
        HashSet<Integer> results = new HashSet<Integer>();

        //for each word, find all and add to set
        for (String word : words){
            HashMap<Integer, ArrayList<Integer>> word_docs = this.inverse_index.get(word);
            for(Entry<Integer, ArrayList<Integer>> entry : word_docs.entrySet()) {
                Integer key = entry.getKey();
                results.add(key);
            }
        }
        return results;
	}
	public HashSet<Integer> get_scenes_partitions(String word){
        HashSet<Integer> results = new HashSet<Integer>();
        //for each word, find all and add to set
            HashMap<Integer, ArrayList<Integer>> word_docs = this.inverse_index.get(word);
            for(Entry<Integer, ArrayList<Integer>> entry : word_docs.entrySet()) {
                Integer key = entry.getKey();
                results.add(key);
            }
        return results;
	}
	
	
	//question3 & 4 - Find the play(s) where 'word' is mentioned.
	public HashSet<String> get_play_byword(String word){
        HashSet<String> results = new HashSet<String>();
        HashMap<Integer, ArrayList<Integer>> word_posting = this.inverse_index.get(word);
        
        // for each doc, look up the playname, add to set
        for(Entry<Integer, ArrayList<Integer>> entry : word_posting.entrySet()) {
            Integer key = entry.getKey();
            String playname = this.docid_playname.get(key);
            results.add(playname);
        }
        return results;
	}
	
	//helper
	//get all docs with same scemces by words. inner joint query
	public HashSet<Integer> get_scenes_together(String[] words){
        HashSet<Integer> baselist = get_scenes_partitions(words[0]);
        //for each set, find intersection
        for (int i = 1; i <words.length; i++){
            HashSet<Integer> listnext = get_scenes_partitions(words[i]);
            baselist.retainAll(listnext);
        }
        return baselist;
	}
	
	//Phrase-based Queries
	//get phrases by first finding all interections, then scan see if sequential
	public HashSet<String> get_scence_byphrase(String phrase){
		String[] words = phrase.split("\\s+");
        HashSet<String> results = new HashSet<String>();
        ArrayList<Integer> docid_scenes = new ArrayList<Integer>(get_scenes_together(words));

        //init the doc postings by finding inner join of the words
        HashMap<Integer, ArrayList<Integer>> base_postings= new HashMap<Integer, ArrayList<Integer>>();
        for(Integer docid: docid_scenes){
    		ArrayList<Integer> base_doc_positions = new ArrayList<Integer>(inverse_index.get(words[0]).get(docid));    		
    		base_postings.put(docid,base_doc_positions);
        }
        
        //filter down the list one word at the time
        for (int i = 1; i <words.length; i++){
        	HashMap<Integer, ArrayList<Integer>> word_posting= inverse_index.get(words[i]);
        	Set<Integer> docids = base_postings.keySet();
        	//for each doc id, check if the contains "base + i-th word" 
        	for(Integer docid: docids){
        		ArrayList<Integer> base_doc_positions = base_postings.get(docid);
        		ArrayList<Integer> doc_positions = word_posting.get(docid);
        		ArrayList<Integer> templist = new ArrayList<Integer>();
            	for (int base_position : base_doc_positions){
            		if (doc_positions.contains(base_position+i)){
            			templist.add(base_position);
            		}
            	}
            	//save the shorted list per docid, or empty if none match
            	base_postings.put(docid,templist);
        	}
		}       
        
        //last step output the final list
        for(Integer docid :base_postings.keySet()){
        	if (!base_postings.get(docid).isEmpty()){
        		String docname = this.docid_docname.get(docid);
	        	results.add(docname);
        	}
        }
        return results;
	}
	
	
	//save outputs
	public void output_stats(HashSet<String> list,String name) throws IOException{
			String text = "";
			List<String> newList = new ArrayList<>(list);
			java.util.Collections.sort(newList);
			
			for (int i = 0; i < list.size(); i++) {
				text += newList.get(i) +"\n";
			}
			text = text.trim();
			BufferedWriter out = new BufferedWriter(new FileWriter("outputs/"+name + ".txt"));
			out.write(text);
			out.close();
			System.out.println(newList);
	        System.out.println();
	}	
	
	//save inverse index as json
	public void save_index() throws IOException{
        JSONObject json = new JSONObject(this.inverse_index);
        try {
    		FileWriter file = new FileWriter("outputs/inverseindex.json");
    		file.write(json.toJSONString());
    		file.flush();
    		file.close();

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
	}
	//read inverse index as json
	public void read_index(String filename) throws ParseException{

		JSONParser parser = new JSONParser();
		HashMap<String, HashMap<Integer, ArrayList<Integer>>> read_index = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();
        try {
        	Object obj = parser.parse(new FileReader(filename));
    		JSONObject jsonObject = (JSONObject) obj;
    		Set words = jsonObject.keySet();
            for(Object word:words ){
                String key = (String)word;
                HashMap<Integer, ArrayList<Integer>> value =  (HashMap<Integer, ArrayList<Integer>>)jsonObject.get(word); 
                read_index.put(key, value);
            }
            
            //save the results to the index in the object
            this.inverse_index = read_index;

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
	}
	
	//print avg scene play stats
	public void print_doc_stats() throws IOException{
		//avg scene size
        int sum = 0;
        for (int f : this.scenename_wordcount.values()) {
            sum += f;
        }
        System.out.println("avg scene size: "+sum/this.scenename_wordcount.size());
        
        //min scene
        int minValueInMap=(Collections.min(this.scenename_wordcount.values()));  // This will return min value in the Hashmap
        for (Entry<String, Integer> entry : this.scenename_wordcount.entrySet()) {
            if (entry.getValue()==minValueInMap) {
                System.out.println("min scene: " +entry.getKey() +" count: "+ entry.getValue());
            }
        }
        
        
        //max play size
        int maxplay=(Collections.max(this.playname_wordcount.values()));  // This will return min value in the Hashmap
        for (Entry<String, Integer> entry : this.playname_wordcount.entrySet()) {
            if (entry.getValue()==maxplay) {
                System.out.println("max play: " +entry.getKey() +" count: "+ entry.getValue());
            }
        }
        //min play size
        int minplay=(Collections.min(this.playname_wordcount.values()));  // This will return min value in the Hashmap
        for (Entry<String, Integer> entry : this.playname_wordcount.entrySet()) {
            if (entry.getValue()==minplay) {
                System.out.println("min play: " +entry.getKey() +" count: "+ entry.getValue());
            }
        }
        
        //get graph data
        int scenesize = this.scenename_wordcount.size();
        int[][] matrix = new int[scenesize][3];
        String[] words = {"you", "thou", "thee"};

        for(int i =0; i<words.length; i++){
        HashMap<Integer, ArrayList<Integer>> base = this.inverse_index.get(words[i]);
	        for(Entry<Integer, ArrayList<Integer>> entry : base.entrySet()) {
	            Integer base_count = entry.getValue().size();
	            matrix[entry.getKey()][i] = base_count;
	        }
        }
        System.out.println("graph data: done"); 
        
        BufferedWriter br = new BufferedWriter(new FileWriter("outputs/myfile.csv"));
        StringBuilder sb = new StringBuilder();

        sb.append("docid,you,thee,thou\n");
        for (int i =0; i<matrix.length; i++) {
        	sb.append(i);
        	sb.append(",");
	        for (int j =0; j<matrix[0].length; j++) {
		         
		         sb.append(String.valueOf(matrix[i][j]));
		         sb.append(",");
	        }
	        sb.append("\n");
        }
        
        br.write(sb.toString());

        br.close();
        
        
	}
	
	
	
	
	// running the code to generate the answers	
	public static void main(String[] args) throws IOException, ParseException {
		Indexor indexor = new Indexor();
		indexor.readjason("inputs/shakespeare-scenes.json");
        System.out.println(indexor.docid_docname);
        
        System.out.println("question1- Find scene(s) where 'thee' or 'thou' is used more than 'you'");
        String[] wordmore = {"thee","thou"};
        HashSet<String> question1 = indexor.get_scence_bymorecompare("you",wordmore);
        indexor.output_stats(question1,"terms0");
                
        System.out.println("question2 -	Find scene(s) where Verona, Rome, or Italy is mentioned.");
        String[] wordlist = {"verona", "rome", "italy"};
        HashSet<String> question2 = indexor.get_scenes_byname(wordlist);
        indexor.output_stats(question2,"terms1");
        
        System.out.println("question3 - Find the play(s) where 'falstaff' is mentioned.");
        HashSet<String> question3 = indexor.get_play_byword("falstaff");
        indexor.output_stats(question3,"terms2");
        
        System.out.println("question4 - Find the play(s) where 'soldier' is mentioned.");
        HashSet<String> question4 = indexor.get_play_byword("soldier");
        indexor.output_stats(question4,"terms3");
        
        System.out.println("question5- Find scene(s) where 'lady macbeth' is mentioned.");
        String phrase1 = "lady macbeth";
        HashSet<String> question5 = indexor.get_scence_byphrase(phrase1);
        indexor.output_stats(question5,"phrase0");
        
        System.out.println("question6- Find scene(s) where 'a rose by any other name' is mentioned.");
        String phrase2 = "a rose by any other name";
        HashSet<String> question6 = indexor.get_scence_byphrase(phrase2);
        indexor.output_stats(question6,"phrase1");
        
        System.out.println("question7- Find scene(s) where 'cry havoc' is mentioned.");
        String phrase3 = "cry havoc";
        HashSet<String> question7 = indexor.get_scence_byphrase(phrase3);
        indexor.output_stats(question7,"phrase2");
        
        System.out.println("plays and scene size");
        indexor.print_doc_stats();
        System.out.println();
        
        //save and read inverted index
        //indexor.save_index();
        //indexor.read_index("outputs/inverseindex.json");
	}
}

