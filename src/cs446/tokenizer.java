package cs446;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;


class waiting extends TimerTask {
    @Override
	public void run() {
       System.out.print("."); 
    }
 }


public class tokenizer extends Thread {
	
	
	// runner maybe implement multi thread later
	public static ArrayList<String> run(String text,HashSet<String> stoplist) throws IOException {
		Timer timer = new Timer();

		timer.scheduleAtFixedRate(new waiting(),0,1000);
		
		System.out.print("tokenizing ");
		ArrayList<String> item = tokenize(text);
		System.out.print(" stopword-checking ");
		item = stopper(item,stoplist);
		System.out.print(" stemming ");
		item = stemmer(item);
		System.out.println();
		
		timer.cancel();
		return item;
	}
	
	//tokenizer
	public static ArrayList<String> tokenize(String text ) throws IOException{
		//then lowcase everything
		text =  text.toLowerCase();

		//All other punctuation should be considered word separators: "200,000" should become ["200", "000"]
		ArrayList<String> list = new ArrayList<String>();
		int startword = 0;
		int endword = 0;
		boolean inword = false;
		
		for (int i=0;i<text.length();i++){
			if (Character.isLetterOrDigit(text.charAt(i)) && inword==false){
				inword= true;
				startword = i;
			}
			
			else if (inword == true && text.charAt(i)=='.' && text.length()-i >=3){
				//ww.w. => ww w.
				if (Character.isLetterOrDigit(text.charAt(i-2)) &&
						Character.isLetterOrDigit(text.charAt(i-1)) &&
						Character.isLetterOrDigit(text.charAt(i+1)) && 
						text.charAt(i+2)=='.'){
						//text = text.substring(0, i)+" "+text.substring(i+1, text.length());
					
						inword = false;
						endword = i;
						String addword = text.substring(startword, endword);
						list.add(addword);
				}
				//w.w. =>w-w.
				else if(Character.isLetterOrDigit(text.charAt(i-1)) &&
						Character.isLetterOrDigit(text.charAt(i+1)) && 
						text.charAt(i+2)=='.'){

						text = text.substring(0, i)+"-"+text.substring(i+1, text.length());
						}
				else if (!Character.isLetterOrDigit(text.charAt(i))){
					inword = false;
					endword = i;
					String addword = text.substring(startword, endword);
					addword= addword.replace("-", "");
					list.add(addword);
				}
			}
			
			//else if if dont want to capture in place change ph.d.
			else if (inword == true &&!Character.isLetterOrDigit(text.charAt(i))){
				inword = false;
				endword = i;
				list.add(text.substring(startword, endword));

			}
			
			
				
		}
		

		//Consider abbreviations such as "U.S.A." as one term: "USA" -- there are equivalents in p2-input-part-A.txt
		//String removed_dot = text.replaceAll(", " ");
		
		
		
		return list;
	}
	
	//stopper
	public static ArrayList<String> stopper(ArrayList<String> tokens, HashSet<String> stoplist ){
		ArrayList<String> list = tokens;
		
		//create an iterator that reads the words from list and remove them if its in the stop list
		for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
		    String word = it.next();
			if (stoplist.contains(word)){
				it.remove();
			}
		}

		return list;
	}
	
	//stemmer
	public static ArrayList<String> stemmer(ArrayList<String> text) {
		ArrayList<String> list = text;

		ListIterator<String> listIterator = list.listIterator();
		while (listIterator.hasNext()) {
			String word = listIterator.next();
			int pos = word.length() - 1;

			// part a
			// - Replace sses by ss (e.g., stresses stress).
			if (pos >= 3 && word.endsWith("sses")) {
				listIterator.set(word.substring(0, pos - 3) + "ss");
			}
			
			// if suffix is us or ss do nothing (e.g., stress stress).
			else if (pos >= 3 && word.endsWith("ss")) {
			}
			
			// - Delete s if the preceding word part contains a vowel not immediately before
			//"Given a word of length k: If the word ends in s, and there is some i<(k-2) such that word[i] is a vowel, then remove the s."
			// the s (e.g., gaps gap but gas gas).
			else if (pos >= 1 && word.charAt(pos) == 's') {
				String preword = word.substring(0, pos - 1);// ga
				String prechar = word.substring(pos - 1, pos);// p  && !contains_vowels(prechar)

				if (contains_vowels(preword) ) {
					listIterator.set(word.substring(0, pos));
				}
			}

			// - Replace ied or ies by i if preceded by more than one letter,
			// otherwise by ie
			// (e.g., ties tie, cries cri).
			else if (word.endsWith("ies") || word.endsWith("ied")) {
				if (pos == 3) {
					listIterator.set(word.substring(0, pos));
				} else if (pos > 3) {
					listIterator.set(word.substring(0, pos - 1));
				}

			}
			
			

			// Step 1b:
			// - Replace eed, eedly by ee if it is in the part of the word after
			// the first nonvowel
			// following a vowel (e.g., agreed agree, feed feed).
			if (word.endsWith("eed") || word.endsWith("eedly")) {
				int preword_index = word.lastIndexOf("eed");
				String preword = word.substring(0, preword_index);
				String prechar = word.substring(preword_index - 1, preword_index);

				if (contains_vowels(preword) && !contains_vowels(prechar)) {
					listIterator.set(word.substring(0, preword_index) + "ee");
				}
			}

			// - Delete ed, edly, ing, ingly if the preceding word part contains a vowel, and
			// then if the word ends in at, bl, or iz add e (e.g., fished fish, pirating
			// pirate), or if the word ends with a double letter that is not ll, ss, or zz, remove
			// the last letter (e.g., falling fall, dripping drip), or if the word is short, add
			// e (e.g., hoping hope).
			else if (word.endsWith("ed") || word.endsWith("edly")|| word.endsWith("ing")|| word.endsWith("ingly")) {
				int preword_index = 0;
				if (word.endsWith("ed") || word.endsWith("edly")){
					preword_index = word.lastIndexOf("ed");
				}
				else {
					preword_index = word.lastIndexOf("ing");
				}
				
				String preword = word.substring(0, preword_index);
				int preword_last = preword.length()-1;
				String temp_word = preword;
				
				if (contains_vowels(preword) ){

					//if the word ends in at, bl, or iz add e(e.g. pirating pirate)	
					if (preword.endsWith("at")||preword.endsWith("bl")||preword.endsWith("iz")) {
						temp_word= preword+"e";
					}

					//word ends with a double letter that is not ll, ss, or zz, eg. dripping drip
					else if (preword.charAt(preword_last)==preword.charAt(preword_last-1) && 
							(!preword.endsWith("ll") && !preword.endsWith("ss")&& !preword.endsWith("zz"))) {
						temp_word = (word.substring(0, preword_last));
					}
					
					if (temp_word.length()<=3){
						listIterator.set(temp_word +"e");
					}
					else{
						listIterator.set(temp_word);
					}
				}
				
				
			}
			

		}

		return list;
	}
	
	//helper contains a vowel?
	public static boolean contains_vowels(String word){
		if (word.contains("a") || word.contains("e")|| word.contains("i") || word.contains("o")|| word.contains("u")){	
			return true;
		}
		else 
			return false;
	}
	
	//helper read text
	public static String read_text(String filename) throws IOException{
		File file = new File(System.getProperty("user.dir") + "/" + filename);
		FileReader in = new FileReader(file);
		BufferedReader br = new BufferedReader(in);

		StringBuilder sb = new StringBuilder();
		String line = br.readLine();
		while (line != null) {
			sb.append(line);
			sb.append(" ");
			line = br.readLine();
		}
		
		String texdoc = sb.toString();
		br.close();

		return texdoc;
	}
	
	
	//helper read stoplist
	public static HashSet<String> read_stoplist(String filename) throws IOException{
		HashSet<String> stoplist = new HashSet<String>();

		File file = new File(System.getProperty("user.dir") + "/"+ filename);
	    FileReader in = new FileReader(file);
	    BufferedReader br = new BufferedReader(in);
	    String word = br.readLine();
	    while (word!=null) {
	    	stoplist.add(word);
	    	word = br.readLine();
	    }
	    in.close();
	    
		return stoplist;
	}
	
	//setup to get the graph data
	public static void print_graph_data(ArrayList<String> list) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		int uniquewords = 0;
		int totalwords = 0;
		List<Integer> uniquecount = new ArrayList<Integer>();
		List<Integer> totalcount = new ArrayList<Integer>();
		
		for (String word : list) {
			totalwords+=1;
			if (map.get(word) == null){
				map.put(word, 1);
				uniquewords += 1;
			}
			else
				map.put(word, map.get(word) + 1);
			
			if (totalwords % 1000 == 0){
				uniquecount.add(uniquewords);
				totalcount.add(totalwords);
			}
		}
		
		System.out.println(uniquecount);
		System.out.println(totalcount);

	}
	
	//setup the top 200 array list
	public static ArrayList<String> get_stats(ArrayList<String> list, int top_n) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		for (String word : list) {
			if (map.get(word) == null)
				map.put(word, 1);
			else
				map.put(word, map.get(word) + 1);
		}
		// System.out.println(map.toString());

		
		//sort for the top 200
		Set<Entry<String, Integer>> set = map.entrySet();
		List<Entry<String, Integer>> list2 = new ArrayList<>(set);
		Collections.sort(list2, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
				return b.getValue() - a.getValue();
			}
		});

		//output the top 200
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < top_n && i < list2.size(); i++) {
			results.add(list2.get(i).toString());
		}
		
		return results;
	}
	
	
	//output file
	public static void output_tokens(ArrayList<String> list,String name, int limit) throws IOException{
			int printlimit = list.size();
			String text = "";
			
			if(limit>0){
				printlimit = limit;
			}
			
			List<String> newList = new ArrayList<>(list.subList(0,printlimit));
			
			for (int i = 0; i < printlimit; i++) {
				text += newList.get(i) + "\n";
			}
			text = text.trim();
			BufferedWriter out = new BufferedWriter(new FileWriter(name + ".txt"));
			out.write(text);
			out.close();
	}
	
	
	
	
	
	
	//***************************** main runner 
	public static void main(String[] args) throws IOException{
		HashSet<String> stoplist= read_stoplist("inputs/stoplist.txt");
		System.out.println("stoplist:");
		System.out.println(stoplist);
		System.out.println();
		

		System.out.println("reading parta");
		String text = read_text("inputs/p2-input-part-A.txt");
		System.out.println("originaltext:");
		System.out.println(text);
		System.out.println();

		ArrayList<String> results = run(text,stoplist);		

		System.out.println("first 50 words:");
		System.out.println(results.subList(0, Math.max(results.size()-1, 50)));
		
		//output results from parta
		output_tokens(results,"outputs/parta_tokens", -1);
		System.out.println("finished parta");
		System.out.println();
		
		//do partb
		System.out.println("reading partb");
		text = read_text("inputs/p2-input-part-b.txt");
		results = run(text,stoplist);
		

		print_graph_data(results);
		results = get_stats(results,200);
		output_tokens(results,"outputs/partb_tokens", 200);
		
		System.out.println("finished partb");
	}
	
}
