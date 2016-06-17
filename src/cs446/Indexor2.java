/**
 * 
 */
package cs446;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class Indexor2 {
	HashMap<String, HashMap<Integer,ArrayList<Integer>>> inverse_index = new HashMap<String, HashMap<Integer,ArrayList<Integer>>>();
	HashMap<Integer,String> docid_docname = new HashMap<Integer,String>();
	HashMap<Integer,String> docid_playname = new HashMap<Integer,String>();
	HashMap<Integer,Integer> scenename_wordcount = new HashMap<Integer,Integer>();
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
	            this.scenename_wordcount.put(docid,list.length);
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
	
	
	// count the freq by query words
	public HashMap<String,Integer> 	query_freq(String[] words){
        HashMap<String,Integer> results = new HashMap<String,Integer>();
		for (String word : words){
			if (results.get(word)!=null){
				results.put(word, results.get(word) +1);
			}
			else{
				results.put(word, 1);
			}
		}
		return results;
	}
	// count the freq in doc id by query words
	public HashMap<String,Integer> 	doc_query_freq(String[] words,Integer docid){
        HashMap<String,Integer> results = new HashMap<String,Integer>();
		for (String word : words){
			if (inverse_index.get(word) !=null){
				ArrayList<Integer> list= inverse_index.get(word).get(docid);
				int count =0;
				if (list != null){
					count = list.size();
				}
				results.put(word, count);
			}
			else{
				results.put(word, 0);
			}
			
		}
		return results;
	}

	public HashMap<String,Integer> 	col_query_freq(String[] words){
        HashMap<String,Integer> results = new HashMap<String,Integer>();
		for (String word : words){
			if (inverse_index.get(word) !=null){
				HashMap<Integer, ArrayList<Integer>> list= inverse_index.get(word);
				int count =0;
				if (list != null){
					int innercount = 0;
					for (Integer docid: list.keySet()){
						innercount +=list.get(docid).size();
					}
					count = innercount;
				}
				results.put(word, count);
			}
			else{
				results.put(word, 0);
			}
			
		}
		return results;
	}
	
	

	public void  run_queries(String[] queries,String model) throws IOException{
		int counter = 0;
		String text = "";
		for(String query : queries){
			counter+=1;
			String queryresult="";
			if (model=="bm25"){
		        HashMap<Integer,Double> bm25scores = getall_score_bm25(query);
				List<Entry<Integer, Double>> sortedresults =sort_output(bm25scores);
				queryresult = text_output("Q"+counter,sortedresults,"yongbinliang-bm25");
			}
			else if (model =="ql"){
				getall_score_ql_jm(query);
		        HashMap<Integer,Double> qlscores = getall_score_ql_jm(query);
				List<Entry<Integer, Double>> sortedresults =sort_output(qlscores);
				queryresult = text_output("Q"+counter,sortedresults,"yongbinliang-ql");
			}
			
			text+=queryresult;
		}
		save_text(text,model +".trecrun");
	
	}
	
	
	//query language model with smoothing Jelinek-Mercer 
	public HashMap<Integer,Double> getall_score_ql_jm(String query){
        HashMap<Integer,Double> results = new HashMap<Integer,Double>();
        Set<Integer> fulldocids = docid_docname.keySet();

        for (Integer docid : fulldocids){
        	double doc_score = score_ql_jm(query,docid);
        	results.put(docid, doc_score);
        }
        
        return results;
	}
	
	///ql with Jelinek-Mercer  lambda 0.8
	public double score_ql_jm(String query, int documentid){
		String[] words = query.split("\\s+");

        double lambda = 0.8;
        double doclen = (double) scenename_wordcount.get(documentid);
        double col_size = collection_size();
        
        HashMap<String,Integer> doc_queryfreqs = doc_query_freq(words,documentid);
        HashMap<String,Integer> col_queryfreqs = col_query_freq(words);
        double totalscore = 0;
        for (String word : words){
        	double fqi = doc_queryfreqs.get(word);
        	double cqi = col_queryfreqs.get(word);
        	double part1 =(1-lambda)*fqi/doclen;
        	double part2 =(lambda)*cqi/col_size;
        	
        	double wordscore = Math.log(part1 + part2);
        	totalscore +=wordscore;
        }
        
        return totalscore;
	}
	
	
	
	//calculate bm25 score for all docs
	public HashMap<Integer,Double> getall_score_bm25(String query){
        HashMap<Integer,Double> results = new HashMap<Integer,Double>();
        Set<Integer> fulldocids = docid_docname.keySet();

        for (Integer docid : fulldocids){
        	double doc_score = score_bm25(query,docid);
        	results.put(docid, doc_score);
        }
        
        return results;
	}
	
	//calculate bm25 for 1 doc, k1=1.2, k2=100. , b=0.75
	public double score_bm25(String query, int documentid){
		String[] words = query.split("\\s+");

        double b =0.75;
        double k1 = 1.2;
        double k2 = 100.0;
        double doclen = (double) scenename_wordcount.get(documentid);
        double avgdoclen =avgsceneleng();
        double K = get_K(b,k1,doclen,avgdoclen);
        
        HashMap<String,Integer> queryfreqs = query_freq(words);
        HashMap<String,Integer> doc_queryfreqs = doc_query_freq(words,documentid);
        double totalscore = 0;
        for (String word : words){
        	double fi = doc_queryfreqs.get(word);
        	double qfi = queryfreqs.get(word);
        	double part2 =(k1+1)*fi/(K+fi);
        	double part3 =(k2+1)*qfi/(k2+qfi);
        	
        	double ri = 0;
        	double R = 0;
        	double N = scenename_wordcount.size();
        	double ni = 0;
        	if (get_scenes_partitions(word) !=null){
        		ni = get_scenes_partitions(word).size();
        	}        	
        	double part1 =((ri+0.5)/(R-ri+0.5)) / ((ni-ri+0.5)/(N-ni-R+ri+0.5));
        	part1 = Math.log(part1);
        	
        	double wordscore = part1 * part2 * part3;
        	totalscore +=wordscore;
        	
        }
        
        return totalscore;
	}
	
	//return K in bm25
	public double get_K(double b,double k1,double doclen, double avgleng){
		double results = k1*((1-b)+b*(doclen/avgleng));
		return results;
	}
	
	public double avgsceneleng() {
        return collection_size()/this.scenename_wordcount.size();
	}
	
	public double collection_size() {
		//avg scene size
        int sum = 0;
        for (int f : this.scenename_wordcount.values()) {
            sum += f;
        }
        return sum;
	}

	//helper output the count of outgoing pages
	public static  List<Entry<Integer, Double>>  sort_output(HashMap<Integer,Double> map){
		Set<Entry<Integer, Double>> set = map.entrySet();
		List<Entry<Integer, Double>> list2 = new ArrayList<>(set);
		Collections.sort(list2, new Comparator<Entry<Integer, Double>>() {
			public int compare(Entry<Integer, Double> a, Entry<Integer, Double> b) {
				return  (b.getValue() - a.getValue() >0 ? 1:-1);
			}
		});
		
		return list2;
	}
	
	
	
	//save each quer's as text
	public String text_output(String Qn,List<Entry<Integer, Double>> outputs, String name ){
		String text = "";
		for (int i = 0; i < outputs.size(); i++) {
			String playname = docid_docname.get(outputs.get(i).getKey());
			int rank = i+1;
			text += Qn +" skip " + playname +" \t "+ rank + " " +
				 " "+outputs.get(i).getValue()+" "+name+"\n";
		}
		
		return text;
	}
	//store text as file
	public void  save_text(String text,String filename) throws IOException{
		text = text.trim();
		BufferedWriter out = new BufferedWriter(new FileWriter("outputs/"+filename));
		out.write(text);
		out.close();    
	}
	
	
	
	// running the code to generate the answers	
	public static void main(String[] args) throws IOException, ParseException {
		Indexor2 indexor = new Indexor2();
		indexor.readjason("inputs/shakespeare-scenes.json");
        System.out.println(indexor.docid_docname);

        String[] queries = {"the king queen royalty",
        					"servant guard soldier",
        					"hope dream sleep",
        					"ghost spirit",
        					"fool jester player",
        					"to be or not to be"};

        indexor.run_queries(queries,"bm25");
        indexor.run_queries(queries,"ql");
        
        
        
        //testing
        HashMap<Integer,Double> bm25scores = indexor.getall_score_bm25(queries[3]);
		System.out.println("bm25 scores");
		System.out.println(bm25scores);
		List<Entry<Integer, Double>> sortedresults =indexor.sort_output(bm25scores);
		System.out.println(sortedresults);
		System.out.println();
		
        HashMap<Integer,Double> qljmscores = indexor.getall_score_ql_jm(queries[3]);
		System.out.println("qljm scores");
		System.out.println(qljmscores);
		List<Entry<Integer, Double>> sortedresults2 = indexor.sort_output(qljmscores);
		System.out.println(sortedresults2);
		System.out.println();
		
		
		///get top 10 from pooling from both of the 2 ranking system
		ArrayList<String> top10 = new ArrayList<String>();
			for (int i =0; i<sortedresults.size() && 10 >top10.size();i++ ){
				top10.add(indexor.docid_docname.get(sortedresults.get(i).getKey()));
				top10.add(indexor.docid_docname.get(sortedresults2.get(i).getKey()));
		}
		
		for (String item : top10){
			System.out.println(item);
		}
		
		
	}
}

