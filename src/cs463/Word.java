package cs463;

import java.util.ArrayList;
import java.util.TreeMap;

public class Word {
	int freq = 1; // note that we start at 1 since we're counting
	private boolean isBlacklisted;
	/* Key : Filepath, Value : Array of positions of corresponding files 
	 * Example :  {33=[2], 45=[52,61,74]} 
	 * */
	private TreeMap<String, ArrayList<Long>> documentList = new TreeMap<String, ArrayList<Long>>();
	private TreeMap<Integer, ArrayList<Long>> documentListID = new TreeMap<Integer, ArrayList<Long>>();
	String rootWord;

	public void setRootWord(String rootWord){
		this.rootWord = rootWord;
	}		
	
	public String getRootWord(){
		return this.rootWord;
	}		

	/* Get the identifier of the referenced document from an index
	 * FIXME try catch ktlp ktlp
	 */
	public void addDocumentRefID(String docName, long pos, TreeMap<String, Integer> docIndex){
		
		Integer docID =  docIndex.get(docName);
		ArrayList<Long> posList = documentListID.get( docID );
		
		if (posList == null) {
			posList = new ArrayList<Long>();
			posList.add(pos);
			
			documentListID.put( docID, posList );
		} else {
			posList.add(pos);
		}			
	}

	/* Store the referenced docname in a string list*/
	public void addDocumentReference(String docName, Long pos){
		ArrayList<Long> posList = documentList.get( docName );
		
		if (posList == null) {
			posList = new ArrayList<Long>();
			posList.add(pos);
			
			documentList.put( docName, posList );
		} else {
			posList.add(pos);
		}		
	}

	public int getDocumentList_size(){
		return documentList.size();
	}		

	public int getDocumentListID_size(){
		return documentListID.size();
	}	
	
	public TreeMap<String, ArrayList<Long>>  getDocumentList(){
		return documentList;
	}		
	
	public TreeMap<Integer, ArrayList<Long>>  getDocumentListID(){
		return documentListID;
	}		
	
	public void incrAppearances() {
		++freq;
	}

	public int getAppearances() {
		return freq;
	}

	public void resetAppearances() {
		freq = 0;
	}
	
	public boolean isBlacklisted() {
		return isBlacklisted;
	}

	public void setBlacklisted(boolean isBlacklisted) {
		this.isBlacklisted = isBlacklisted;
	}
}