package cs446;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Query_eval {
	//stores query score ranks, and relevance
	HashMap<String, HashMap<Integer,String>> query_rank_doc = new HashMap<String, HashMap<Integer,String>>();
	HashMap<String, HashMap<String,String>> query_doc_relevance = new HashMap<String, HashMap<String,String>>();

	HashMap<String, HashMap<Integer, Double>> query_doc_recall = new HashMap<String, HashMap<Integer,Double>>();
	HashMap<String, HashMap<Integer, Double>> query_doc_precision = new HashMap<String, HashMap<Integer,Double>>();
	


	//read in the score for query
	public void read_trecrun(String filename) throws IOException{
		File file = new File(System.getProperty("user.dir") + "/"+ filename);
	    FileReader in = new FileReader(file);
	    BufferedReader br = new BufferedReader(in);
	    String row = br.readLine();
	    while (row!=null) {
	    	String[] columns = row.split(" ");
	    	
	    	if (query_rank_doc.get(columns[0])==null){
	    		HashMap<Integer,String> rank = new HashMap<Integer,String>();
	    		query_rank_doc.put(columns[0], rank);
	    	}
	    	
	    	HashMap<Integer,String> rank = query_rank_doc.get(columns[0]);
	    	rank.put(Integer.parseInt(columns[3]),columns[2]);
	    	
	    	row = br.readLine();
	    }
	    in.close();
	}
	
	//read in the query doc relevance
	public void read_relevance(String filename) throws IOException{
		File file = new File(System.getProperty("user.dir") + "/"+ filename);
	    FileReader in = new FileReader(file);
	    BufferedReader br = new BufferedReader(in);
	    String row = br.readLine();
	    while (row!=null) {
	    	String[] columns = row.split(" ");
	    	
	    	if (query_doc_relevance.get(columns[0])==null){
	    		HashMap<String,String> relevence = new HashMap<String,String>();
	    		query_doc_relevance.put(columns[0], relevence);
	    	}
	    	
	    	HashMap<String,String> relevence = query_doc_relevance.get(columns[0]);
	    	relevence.put(columns[2],columns[3]);
	    
	    	row = br.readLine();
	    }
	    in.close();
	}

	//calculate all precision and recall
	public void score_recall_precision_all(){
		query_doc_recall = new HashMap<String, HashMap<Integer,Double>>();
		query_doc_precision = new HashMap<String, HashMap<Integer,Double>>();
		
		for(String queryID: query_rank_doc.keySet()){
			score_recall_precision(queryID);
		}
	}
	//calculate recall and precision at qN
	public void score_recall_precision(String queryID){
		if (query_doc_precision.get(queryID)==null){
			HashMap<Integer,Double> precisions = new HashMap<Integer,Double>();
			query_doc_precision.put(queryID, precisions);
    	}		
		if (query_doc_recall.get(queryID)==null){
			HashMap<Integer,Double> recalls = new HashMap<Integer,Double>();
			query_doc_recall.put(queryID, recalls);
    	}

		HashMap<Integer,Double> recalls = query_doc_recall.get(queryID);
		HashMap<Integer,Double> precisions = query_doc_precision.get(queryID);
		HashMap<Integer,String> ranks = query_rank_doc.get(queryID);
		HashMap<String,String> rev = query_doc_relevance.get(queryID);
		int recall_count = 0;
		int relevant_count = 0;
		
		//String test = rev.get("FR940317-2-00023");
		//System.out.println(test.equals("1"));
		
		//count all the relevant documents for recall
		for (int rank : ranks.keySet()){
			String docID = ranks.get(rank);
			String rev_rating = rev.get(docID);
			if (rev_rating!=null && (rev_rating.equals("1") || rev_rating.equals("2") ) ){
				recall_count+=1;
			}
		}

		// calculate the score for precision and recall
		for (int i = 1; i<= ranks.size();i++){
			//if no relevant docs in the query, set everything to 0
			if (recall_count==0){
				recalls.put(i, 0.0);
				precisions.put(i, 0.0);
			}
			else{
				String docID = ranks.get(i);
				String rev_rating = rev.get(docID);
				if (rev_rating!=null && (rev_rating.equals("1") || rev_rating.equals("2")) ){
					relevant_count+=1;
				}
				recalls.put(i, (double)relevant_count / (double)recall_count );
				precisions.put(i, (double)relevant_count / (double)i );	
				
			}
		}
		// obj auto updated recall and precision to table
		//query_doc_recall.put(queryID, recalls);
		//query_doc_precision.put(queryID, precisions);
	}

	

	//calculate nornalized discounted cumulitive gain at qN,	NDCG@20 
	public Double cal_ndcg_at_n(int  n){
		Double results = 0.0;
		int counts = query_rank_doc.keySet().size();

		for(String queryID: query_rank_doc.keySet()){
			HashMap<Integer,String> ranks = query_rank_doc.get(queryID);
			HashMap<String,String> rev = query_doc_relevance.get(queryID);
			ArrayList<Double> micro_scores = new ArrayList<Double>();
			ArrayList<Double> optimal_scores = new ArrayList<Double>();
			
			//get optimal score
			List values = new ArrayList(rev.values() );
			Collections.sort(values,Collections.reverseOrder());
			//System.out.println(values);
			//if n larger than size dosomething
			for(int i= 1; i<=n;i++){
				if(i >rev.size()){
					optimal_scores.add( optimal_scores.get(rev.size()-1) );
				}
				else{
					double logofi = Math.log(1+i);
					int rel = Integer.parseInt((String) values.get(i-1));
					double dcgtop = Math.pow(2, rel)-1;
					double dcg = dcgtop / logofi;
					if(i==1){
						optimal_scores.add( dcg);
					}
					else{
						optimal_scores.add( optimal_scores.get(i-2) + dcg);
					}
				}
			}
			//System.out.println(optimal_scores);
			
			//get normalized score
			for(int i= 1; i<=n;i++){
				if(i >rev.size()){
					micro_scores.add( micro_scores.get(rev.size()-1) );
				}
				else{
					double logofi = Math.log(1+i);
					String docID = ranks.get(i);
					String rev_rating = rev.get(docID);
					if (rev_rating !=null && !rev_rating.equals("0")){
						int rel = Integer.parseInt(rev_rating);
						double dcgtop = Math.pow(2, rel)-1;
						double dcg = dcgtop / logofi;
						double optimal = optimal_scores.get(i-1);
						double normalized = dcg / optimal;
						micro_scores.add( normalized );	
					}
				}
			}

			//System.out.println(micro_scores);
			if (micro_scores.size() > 0){
				double sum = 0;
				for(Double d : micro_scores)
				    sum += d;
					sum = sum/micro_scores.size();
				results +=sum;
			}
		}
		// end forloop calculate total ndcg scores at n
		
		return results/counts;
	}
	
	//calculate precision at n
	public Double cal_mean_precision_at_n(int  n){
		Double results = 0.0;
		int counts = query_rank_doc.keySet().size();
		for(String queryID: query_rank_doc.keySet()){
			HashMap<Integer,Double> precisions = query_doc_precision.get(queryID);
			Double precision_score = 0.0;
			if (n<=precisions.size()){
				precision_score = precisions.get(n);
				//System.out.println(precision_score);
			}
			else{//if exceed, then use last
				precision_score = precisions.get(precisions.size());
			}
			results +=precision_score;
			
		}
		return results/counts;
	}
	
	//calculate precision at n
	public Double cal_mean_recall_at_n(int  n){
		Double results = 0.0;
		int counts = query_rank_doc.keySet().size();
		for(String queryID: query_rank_doc.keySet()){
			HashMap<Integer,Double> recalls = query_doc_recall.get(queryID);
			Double precision_score = 0.0;
			if (n<=recalls.size()){
				precision_score = recalls.get(n);
				//System.out.println(precision_score);
			}
			else{//if exceed, then use last
				precision_score = recalls.get(recalls.size());
			}

			results +=precision_score;
		}
		return results/counts;
	}
	
	
	//calculate F1 score  F = 2*R*P / (R +P)
	public Double cal_f1_at_n(int  n){
		Double results = 0.0;
		int counts = query_rank_doc.keySet().size();
		
		//loop through every query
		for(String queryID: query_rank_doc.keySet()){
			HashMap<Integer,Double> precisions = query_doc_precision.get(queryID);
			HashMap<Integer,Double> recalls = query_doc_recall.get(queryID);
			ArrayList<Double> micro_scores = new ArrayList<Double>();
			Double prev_recall_score =0.0;
			
			//count all the relevant documents for f1 score 
			for (int i = 1; i<= n;i++){
				if (n<=precisions.size()){
					Double recall_score = recalls.get(i);
					Double precision_score = precisions.get(i);
					if (recall_score!= null && recall_score >prev_recall_score) {
						Double f1_qn = 2*recall_score*precision_score / (recall_score+precision_score);
						micro_scores.add(f1_qn);
						prev_recall_score = recall_score;
					}
				}
				else{
					Double recall_score = recalls.get(precisions.size());
					Double precision_score = precisions.get(precisions.size());
					if (recall_score!= null && recall_score >prev_recall_score) {
						Double f1_qn = 2*recall_score*precision_score / (recall_score+precision_score);
						micro_scores.add(f1_qn);
						prev_recall_score = recall_score;
					}
				}
			}
			
			//update score
			if (micro_scores.size() >0){
				double sum = 0;
				for(Double d : micro_scores)
				    sum += d;
				results += sum/micro_scores.size();
			}
		}
		// end forloop calculate average f1 scores
		
		return results/counts;
	}
	
	
	//calculate mean average precision
		//micro averaging everytime recalls change, append the precision. take average at end 
		public Double cal_mean_avg_precision(){
			Double results = 0.0;
			int counts = query_rank_doc.keySet().size();
			for(String queryID: query_rank_doc.keySet()){
				//ArrayList<Double> micro_scores = cal_avg_precision(queryID);
				//System.out.println(micro_scores);
				//counts += micro_scores.size();
				results +=cal_avg_precision(queryID); 
				
			}
			return results/counts;
		}
		//calculate teh individual precisions per query
		public Double cal_avg_precision(String queryID){
			HashMap<Integer,Double> recalls = query_doc_recall.get(queryID);
			HashMap<Integer,Double> precisions = query_doc_precision.get(queryID);
			Double prev_recall_score =0.0;
			Double results =0.0;
			//ArrayList<Double> micro_scores = new ArrayList<Double>();
			
			//count all the relevant documents for recall
			for (int i = 1; i<= query_doc_precision.size();i++){
				Double recall_score = recalls.get(i);
				Double precision_score = precisions.get(i);
				if (recall_score!= null && recall_score >prev_recall_score) {

					//micro_scores.add(precision_score);
					results +=precision_score;
					prev_recall_score = recall_score;
				}
			}
			return results/query_doc_precision.size();
		}
		
	//return the 6 different output per model 
	public void run_model(String modelname) throws IOException {
		read_trecrun("inputs/p6-data/"+modelname);
		score_recall_precision_all();

		System.out.println(modelname+" datasize: \t" +query_rank_doc.size());
		System.out.println(modelname+" NDCG@20: \t" +cal_ndcg_at_n(20));
		System.out.println(modelname+" P@5: \t" +cal_mean_precision_at_n(5));
		System.out.println(modelname+" P@10: \t" +cal_mean_precision_at_n(10));
		System.out.println(modelname+" Recall@10: \t" +cal_mean_recall_at_n(10));
		System.out.println(modelname+" F1@10 \t" +cal_f1_at_n(10));
		System.out.println(modelname+" AP: \t" +cal_mean_avg_precision());
		
		run_graph("450");
		System.out.println("standard d"+cal_precision_standard_deviation());
		
	}
	
	//generate data for the graph of recall and precision
	public void run_graph(String queryID) throws IOException {

		HashMap<Integer,Double> recalls = query_doc_recall.get(queryID);
		HashMap<Integer,Double> precisions = query_doc_precision.get(queryID);

		System.out.println("");
		System.out.print("recalls: ");
		for (int i : recalls.keySet()){
			System.out.print(recalls.get(i)+",");
		}
		System.out.println("");
		System.out.print("precision: ");
		for (int i : precisions.keySet()){
			System.out.print(precisions.get(i)+",");
		}
		System.out.println("");

	}
	
	
	public Double cal_precision_standard_deviation(){

	        double mean = cal_mean_avg_precision();
	        ArrayList<Double> diff_scores = new ArrayList<Double>();
	        for(String queryID: query_rank_doc.keySet()){
	        	diff_scores.add( Math.pow(Math.abs(cal_avg_precision(queryID) - mean),2) );				
			}
	        
	        double temp = 0;
	        for(double a :diff_scores)
	            temp += a;
	        temp =temp/ diff_scores.size();

	        return Math.sqrt(temp);
	}
	
	
	public static void main(String[] args) throws IOException {
		
		Query_eval QE = new Query_eval();
		QE.read_relevance("inputs/p6-data/qrels");

		QE.run_model("bm25.trecrun");
		//QE.run_model("ql.trecrun");
		//QE.run_model("sdm.trecrun");
		//QE.run_model("stress.trecrun");
		
		/*
		System.out.println("querysize: \t"+QE.query_rank_doc.keySet().size());
		System.out.println("docrank: \t"+QE.query_rank_doc.get("302"));
		System.out.println("relevance: \t"+QE.query_doc_relevance.get("302"));
		System.out.println("recalls: \t"+QE.query_doc_recall.get("302"));
		System.out.println("precision: \t"+QE.query_doc_precision.get("302"));
		*/
		
	}

}
