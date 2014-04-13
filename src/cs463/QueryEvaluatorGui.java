package cs463;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class QueryEvaluatorGui extends JFrame {

	static public class IndexedWord {
		int numOfDocs; // In how many docs it appears
		int startFromDocID; // Start from Pos in PostingFile
		Double IDF;
		Double tf; // Tf as calculated before (appearances/MaxAppearancesInDocs)
	}

	static TreeMap<String, IndexedWord> vocabularyIndex = new TreeMap<String, IndexedWord>();
	static Double vectorTable[][];
	
	
	public static void loadVocabulary(File fileInfo) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileInfo), "UTF8"));
			String line;
			String[] indexParts;
			String word;
			while ((line = reader.readLine()) != null) {
				/*
				 * Must be defined here otherwise every word stored in the HASH
				 * is showing to the same address
				 */
				IndexedWord wordInfo = new IndexedWord();

				line = line.replaceAll("\\s+", "");
				indexParts = line.split(":");
				word = indexParts[0];
				wordInfo.numOfDocs = Integer.parseInt(indexParts[1]);
				wordInfo.startFromDocID = Integer.parseInt(indexParts[2]);

				vocabularyIndex.put(word, wordInfo);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	/* Convert a document ID to a PATH */
	public static List<String> docIDtoPath(File idToPathFile,
			List<Integer> docIDlist) {
		System.out.println("Convert IDs to Paths");
		/* Sort the List so we don't lost anything */

		if (docIDlist == null || docIDlist.size() == 0) {
			System.out.println(" No documents are given");
			return null;
		}
		Collections.sort(docIDlist);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(idToPathFile));
			int skipLines = 0;
			/* Skip lines until we reach the target */
			for (Integer docId : docIDlist) {

				/* Skip from last (0) to next docID */
				while (skipLines != docId) {
					reader.readLine();
					skipLines++;
				}
				/* Print the stuff */
				String docInfo = reader.readLine();
				System.out.println(docInfo);
				skipLines++; // To falsify the condition above
			}
		} finally {
			return null;
		}
	}

	/* Returns something like <(word1, <doc1,doc2,doc3>>), (word2, <doc2,doc4,doc5>>) */
	public static TreeMap<String, List<String>>  searchForDocs(File postingFile, String[] keywords) {
		
		TreeMap<String, List<String>> relevantDocs = new TreeMap<String, List<String>>();
		for (String keyword : keywords){
			List<String> relDocsInfo;
			relDocsInfo = searchForDocs(postingFile, keyword);
			
			if (relDocsInfo == null){
				System.out.println("Could not find relevant docs for keyword : " + keyword);
			}
			relevantDocs.put(keyword, relDocsInfo);
		}
		
		
		return relevantDocs;
		
	}
	
	/*
	 * Return corresponding lines from Posting for documents including the
	 * wordString
	 */
	public static List<String> searchForDocs(File postingFile, String wordString) {
		JFrame frame = new JFrame();
		IndexedWord wordInfo;
		List<String> docInfos = new ArrayList<String>();
		wordInfo = vocabularyIndex.get(wordString);

		/*
		 * JOptionPane.showMessageDialog(frame," *** START FINDING ****\n"
		 * +"Find word : " + wordString +"\nWord : " + wordString
		 * +"\nAppearances : " + wordInfo.numOfDocs +"\nPointer : " +
		 * wordInfo.startFromDocID);
		 */

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(postingFile));
			int skipLines = 0;
			/* Skip lines until we reach the target */
			while (skipLines < wordInfo.startFromDocID) {
				reader.readLine();
				skipLines++;
			}

			int docsRead = 0;
			while (docsRead < wordInfo.numOfDocs) {
				docInfos.add(reader.readLine());
				docsRead++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return docInfos;
	}

	public static double log2(int i) {
		return (Math.log(i) / Math.log(2));
	}

	static int TOTAL_NUMBER_OF_DOCUMENTS = 50;
	/* We must calculate the weight only for the document that are related to our search */
	public static TreeMap<Integer,TreeMap<String,Double>> buildVector(File posting, TreeMap<String, List<String>> keywordDocList) {

		if (vocabularyIndex.size() == 0) {
			System.out.println("No vocabulary has been loader");
			System.exit(-1);
		}

		TreeMap<Integer, TreeMap<String,Double>> relDocsWeight = new TreeMap<Integer, TreeMap<String,Double>>();

		/* For every word in the vocabulary */
		for( String word : vocabularyIndex.keySet())
		{
			/* If there a word in the query */
			if ( keywordDocList.get(word) != null )
			{
				/* Calculate the weight for the word in each document and save it */
				List<String> relevantDocuments = keywordDocList.get(word);
				for (String docInfo : relevantDocuments)
				{
					docInfo = docInfo.replaceAll("\\s+", "");
					String[] docInfoParts = docInfo.split(":");
					Double tf;
					Integer docID;
					docID = new Integer(docInfoParts[0]);
					tf = new Double(docInfoParts[1]);
					double wordIDF = log2(TOTAL_NUMBER_OF_DOCUMENTS / relevantDocuments.size());		
					double weight = tf * wordIDF;			
					
					/* Store IDF to the corresponding word */
					vocabularyIndex.get(word).IDF = wordIDF;
					
					/* Push it to the proper position */
					TreeMap<String, Double> entry = new TreeMap<String, Double>();
					entry.put(word, weight);
					relDocsWeight.put(docID, entry);
				}
			}		
		}
		printVectorTable( relDocsWeight );
		return relDocsWeight;
	}


	public static Vector<Double>  treeToVector( TreeMap<String, Double> docWeightVector )
	{
		Vector<Double> vector = new Vector<Double>();

		for (String s : vocabularyIndex.keySet()){
			if (docWeightVector.get(s) != null)
				vector.add( docWeightVector.get(s) );
			else
				vector.add((double) 0);
		}
		return vector;
	}
	
	
	public static double cosineSimilarity(Vector<Double> vectorA, Vector<Double> vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < vectorA.size(); i++) {
	        dotProduct += vectorA.elementAt(i) * vectorB.elementAt(i);
	        normA += Math.pow(vectorA.elementAt(i), 2);
	        normB += Math.pow(vectorB.elementAt(i), 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}	
	
	
	public static Vector<Double> buildQueryVector(Vector<String> queryVec) {
		HashMap<String, Integer> queryFreqMap = new HashMap<String, Integer>();
		Integer freq;
		Vector<Double> queryWeight = new Vector<Double>();
		
		/* Calculate the frequencies */
		for (String s : queryVec) {
			freq = queryFreqMap.get(s);
			if (freq == null) {
				freq = 0;
			}
			queryFreqMap.put(s, ++freq);
		}

		/* Search for the maximum frequency of the query */
		Entry<String, Integer> maxEntry = null;
		for (Entry<String, Integer> entry : queryFreqMap.entrySet()) {
			if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
				maxEntry = entry;
			}
		}

		/* Calculate the weight */
		for (String s : vocabularyIndex.keySet()) {
	
			double weight = 0;
			if ( queryVec.contains(s))
			{
				System.out.println("Found match for : " + s );
				Double wordIDF = vocabularyIndex.get(s).IDF;
				if ( wordIDF == null )
				{
					System.out.println("Could not calculate IDF for word : " + s);
					System.exit(-1);
				}
				
				Double tf = (double) (queryFreqMap.get(s) / maxEntry.getValue());
				weight = tf * wordIDF;
			}
			queryWeight.add(weight);
		}
		
		return queryWeight;
	}

	public static void printVectorTable( TreeMap<Integer,TreeMap<String,Double>>  vectorTable) 
	{
		System.out.println("********* VECTOR TABLE **************");
		
		for (Integer docId : vectorTable.keySet())
		{
			System.out.println("Vector for Document : " + docId);
			for ( String word : vectorTable.get(docId).keySet())
			{
				Double weight = vectorTable.get(docId).get(word);
				if ( weight != null && weight != 0)
					System.out.println( "Word : " + word + " @docId " + docId + " Weight : " + weight );
			}
			
		}
	}
	
	
	
	public static void compareQueryToDocs( Vector<Double> queryWeight, TreeMap<Integer,TreeMap<String,Double>> docWeightList)
	{
		
		for (Integer docID : docWeightList.keySet()) {
			System.out.println("Compare Query to Doc " + docID);

			Vector<Double> vector;			
			vector = treeToVector( docWeightList.get(docID) );
			System.out.println( "Doc Vector : " + vector.toString());
			System.out.println( "Query Vector : " + queryWeight.toString());			
			System.out.println( cosineSimilarity( vector , queryWeight) );
		}
		
		
	}

	public static void main(String[] args) {

		String searchForWord = null;
		// ////////////////////////////////////////
		JTextField wordField = new JTextField(10);
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new javax.swing.BoxLayout(myPanel,
				javax.swing.BoxLayout.Y_AXIS));

		myPanel.add(new JLabel("Word :"));
		myPanel.add(wordField);
		myPanel.add(Box.createVerticalStrut(10)); // a spacer
		int result = JOptionPane.showConfirmDialog(null, myPanel,
				"Query Evaluator", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			searchForWord = wordField.getText();

		}
		if (result == JOptionPane.CANCEL_OPTION) {

			System.exit(0);
		}

		// ///////////////////////////////////////////////////

		/* Load the vocabulary */
		final File vocabularyIndex = new File(
				"./CollectionIndex/VocabularyFiles.txt");
		loadVocabulary(vocabularyIndex);

		/* find manually the docIDs */
		final File posting = new File("./CollectionIndex/PostingFile.txt");
		// List<String> docInfos = searchForDocs(posting, word);
		// searchDocIDs(posting, "wheat");

		/* find manually the Paths */
		final File docInfo = new File("./CollectionIndex/DocumentsFile.txt");
		// docIDtoPath(docInfo, docIDList);
		TreeMap<String, List<String>> relevantDocuments;
		Vector<String> keywords = new Vector<String>();
		keywords.add( searchForWord);
		relevantDocuments =  searchForDocs(posting, keywords.toArray( new String[keywords.size()]));

		// vectorModel(word, docInfos);
		TreeMap<Integer,TreeMap<String,Double>> docVectors;
		docVectors = buildVector(posting, relevantDocuments);
		
		Vector<String> query = new Vector<String>();
		query.add(searchForWord);
		buildQueryVector(query);
		
		compareQueryToDocs( buildQueryVector(query), docVectors );
		
	}

}
