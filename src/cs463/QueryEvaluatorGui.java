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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mitos.stemmer.Stemmer;

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

		List<String> docPathList = new ArrayList<String>();

		if (docIDlist == null || docIDlist.size() == 0) {
			System.out.println(" No documents are given");
			System.out.println(docIDlist);
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
				docPathList.add(docInfo);
				// System.out.println(docInfo);
				skipLines++; // To falsify the condition above
			}
		} finally {
			return docPathList;
		}
	}

	/*
	 * Returns something like <(word1, <doc1,doc2,doc3>>), (word2,
	 * <doc2,doc4,doc5>>)
	 */
	public static TreeMap<String, List<String>> searchForDocs(File postingFile,
			TreeMap<String, Double> termMap) {

		TreeMap<String, List<String>> relevantDocs = new TreeMap<String, List<String>>();
		Iterator<String> termMap_it = termMap.keySet().iterator();

		/*
		 * Because java sucks we use the iterator so that we can delete words
		 * that do not exist from the term map
		 */
		while (termMap_it.hasNext()) {
			String keyword = termMap_it.next();
			System.out.println("" + keyword);
			List<String> relDocsInfo;
			relDocsInfo = searchForDocs(postingFile, keyword);

			if (relDocsInfo != null) {
				// termMap_it.remove();
				relevantDocs.put(keyword, relDocsInfo);
			}
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

		if (wordInfo == null) {
			// System.out.println("Word : " + wordString +
			// " could not be found");
			System.out.println("\t--> Not found");
			return null;
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(postingFile));
			int skipLines = 0;
			/* Skip lines until we reach the target */
			while (skipLines < wordInfo.startFromDocID) {
				reader.readLine();
				skipLines++;
			}
			// System.out.println("Start from line : " + skipLines);
			System.out.println("\t# Docs : " + wordInfo.numOfDocs);
			int docsRead = 0;
			while (docsRead < wordInfo.numOfDocs) {
				String line = reader.readLine();
				// System.out.println("-->  " + line);
				docInfos.add(line);
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

	public static double log2(double d) {
		return (Math.log(d) / Math.log(2));
	}

	static int TOTAL_NUMBER_OF_DOCUMENTS = 50;

	/*
	 * We must calculate the weight only for the document that are related to
	 * our search
	 */
	public static TreeMap<Integer, TreeMap<String, Double>> buildVector(
			File posting, TreeMap<String, List<String>> keywordDocList) {

		if (vocabularyIndex.size() == 0) {
			System.out.println("No vocabulary has been loader");
			System.exit(-1);
		}

		TreeMap<Integer, TreeMap<String, Double>> relDocsWeight = new TreeMap<Integer, TreeMap<String, Double>>();

		/* For every word in the vocabulary */
		for (String word : vocabularyIndex.keySet()) {
			/* If there a word in the query */
			if (keywordDocList.get(word) != null) {
				/*
				 * Calculate the weight for the word in each document and save
				 * it
				 */

				List<String> relevantDocuments = keywordDocList.get(word);
				for (String docInfo : relevantDocuments) {
					docInfo = docInfo.replaceAll("\\s+", "");
					String[] docInfoParts = docInfo.split(":");
					Double tf;
					Integer docID;
					docID = new Integer(docInfoParts[0]);
					tf = new Double(docInfoParts[1]);
					/*
					 * Ton xristo sou kai thn panagia sou ..... Java needs
					 * casting other division return only the major number
					 */
					double wordIDF = log2(TOTAL_NUMBER_OF_DOCUMENTS
							/ (double) relevantDocuments.size());
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
		return relDocsWeight;
	}

	public static Vector<Double> treeToVector(
			TreeMap<String, Double> docWeightVector, Integer docID) {
		Vector<Double> vector = new Vector<Double>();
		Double w;
		for (String s : vocabularyIndex.keySet()) {
			w = docWeightVector.get(s);

			if (w == null || w == 0.0)
				vector.add((double) 0.0);
			else
				vector.add(w);

		}
		return vector;
	}

	public static Vector<Double> treeToVector(
			TreeMap<String, Double> docWeightVector) {
		Vector<Double> vector = new Vector<Double>();
		Double w;

		for (String s : vocabularyIndex.keySet()) {

			w = docWeightVector.get(s);

			if (w == null || w == 0.0)
				vector.add((double) 0.0);
			else
				vector.add(w);
		}
		return vector;
	}

	public static Double cosineSimilarity(Vector<Double> docVector1,
			Vector<Double> docVector2) {
		double dotProduct = 0.0;
		double magnitude1 = 0.0;
		double magnitude2 = 0.0;
		double cosineSimilarity = 0.0;

		if (docVector1.size() != docVector2.size()) {
			System.out.println("Vector have different sizes ... wtf ?");
		}

		for (int i = 0; i < docVector2.size(); i++) // docVector1 and docVector2
													// must be of same length
		{
			dotProduct += docVector1.get(i) * docVector2.get(i); // a.b
			magnitude1 += Math.pow(docVector1.get(i), 2); // (a^2)
			magnitude2 += Math.pow(docVector2.get(i), 2); // (b^2)
		}

		magnitude1 = Math.sqrt(magnitude1);// sqrt(a^2)
		magnitude2 = Math.sqrt(magnitude2);// sqrt(b^2)

		if (magnitude1 == 0.0 || magnitude2 == 0.0) {
			return (double) 0.0;
		} else {
			cosineSimilarity = (dotProduct / (magnitude1 * magnitude2));
		}

		return cosineSimilarity;
	}

	private static TreeMap<String, Double> parseWordWeight(
			String[] weightenWordList) {
		TreeMap<String, Double> wordWeightTree = new TreeMap<String, Double>();

		for (String word : weightenWordList) {
			String keyword = word;
			Double weight = (double) 1;

			/* Split the keyword to word and weight */
			if (word.contains(":")) {
				keyword = word.split(":")[0];
				weight = Double.parseDouble(word.split(":")[1]);
			}
			if (weight > 1 || weight < 0) {
				System.out
						.println("Wrong weight has been given. Assign it to 1");
				weight = (double) 1;
			}
			System.out.println(" Word : " + keyword + " ,Weight : " + weight);
			wordWeightTree.put(keyword, weight);
		}

		return wordWeightTree;
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

			Double weight = (double) 0;
			if (queryVec.contains(s)) {
				System.out.println("Found match for : " + s);
				Double wordIDF = vocabularyIndex.get(s).IDF;
				if (wordIDF == null) {
					System.out.println("Could not calculate IDF for word : "
							+ s);
					System.exit(-1);
				}

				Double tf = (double) (queryFreqMap.get(s) / maxEntry.getValue());
				weight = tf * wordIDF;
			}
			queryWeight.add(weight);
		}

		return queryWeight;
	}

	public static TreeMap<String, Double> buildWeightenQueryMap(
			TreeMap<String, Double> wordWeightTree) {

		/* Get a tree of <word, weight> */
		for (String s : vocabularyIndex.keySet()) {
			/* Check if value exists on the tree */
			if (wordWeightTree.get(s) == null)
				wordWeightTree.put(s, 0.0);
		}
		return wordWeightTree;
	}

	public static void printVectorTable(
			TreeMap<Integer, TreeMap<String, Double>> vectorTable) {
		System.out.println("********* VECTOR TABLE **************");

		for (Integer docId : vectorTable.keySet()) {
			// System.out.println("Vector for Document : " + docId);
			for (String word : vectorTable.get(docId).keySet()) {
				Double weight = vectorTable.get(docId).get(word);
				// if ( weight != null && weight != 0)
				// System.out.println( "Word : " + word + " @docId " + docId +
				// " Weight : " + weight );
			}

		}
	}

	public static TreeMap<String, Double> StemTheVectorBaby(
			TreeMap<String, Double> weightenWordMap) {
		Stemmer.Initialize();
		TreeMap<String, Double> weightenStemMap = new TreeMap<String, Double>();

		for (String word : weightenWordMap.keySet()) {
			weightenStemMap.put(word, weightenWordMap.get(word));
		}

		return weightenStemMap;
	}

	/*
	 * arg1 : weightened query vector arg2 : Map with docID as key, and each
	 * docID is associated with a second map for words inside the document with
	 * appropriate weight
	 */
	public static TreeMap<Integer, Double> compareQueryToDocs(
			TreeMap<String, Double> queryWeight,
			TreeMap<Integer, TreeMap<String, Double>> docWeightList) {

		TreeMap<Integer, Double> similarDocs = new TreeMap<Integer, Double>();

		for (Integer docID : docWeightList.keySet()) {

			Vector<Double> docVector;
			Vector<Double> queryVector;

			queryVector = treeToVector(queryWeight);
			docVector = treeToVector(docWeightList.get(docID), docID);
			Double similarity = cosineSimilarity(docVector, queryVector);

			similarDocs.put(docID, similarity);
		}
		return similarDocs;
	}

	static Map sortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static void compareQueryToDocs(Vector<Double> queryWeight,
			TreeMap<Integer, TreeMap<String, Double>> docWeightList) {

		for (Integer docID : docWeightList.keySet()) {

			Vector<Double> docVector;
			docVector = treeToVector(docWeightList.get(docID));
			// System.out.println( "Doc Vector : " + vector.toString());
			// System.out.println( "Query Vector : " + queryWeight.toString());
			double similarity = cosineSimilarity(docVector, queryWeight);
			System.out.println("Compare Query to Doc " + docID
					+ " similarity : " + similarity);
		}

	}

	public static void printSimilarDocs(TreeMap<Integer, Double> weightDocList) {
		System.out.println("*********** SIMILAR DOCS *************");

		final File docDescrFile = new File(
				"./CollectionIndex/DocumentsFile.txt");

		ArrayList<Integer> keys = new ArrayList<Integer>(sortByValue(
				weightDocList).keySet());
		for (int i = keys.size() - 1; i >= 0; i--) {
			List<Integer> dummyList = new ArrayList<Integer>();
			dummyList.add(keys.get(i));

			System.out.println("DocId : " + keys.get(i) + " Weight : "
					+ weightDocList.get(keys.get(i)) + " PATH : "
					+ docIDtoPath(docDescrFile, dummyList));

		}
		System.out.println("*************************************");
	}

	public static TreeMap<String, Double> findRelatedTerms(
			TreeMap<String, Double> termMap) {
		TreeMap<String, Double> relatedTerms = null;
		try {
			/*
			 * We create a new tree map, because if we modify the current
			 * (termMap) due to multiple threads ConcurrentModificationException
			 * is thrown and I 'm to lazy to take care of it. cheers
			 */
			relatedTerms = new TreeMap<String, Double>();

			UniversalWordNet uwn = new UniversalWordNet();

			/*
			 * Enhance previous map with related words weighten as dependant
			 * variables
			 */
			for (String term : termMap.keySet()) {
				relatedTerms.put(term, termMap.get(term));
				TreeMap<String, Float> relatedWords;

				// Fetch the list
				relatedWords = uwn.findRelatedTerms(term);

				for (String word : relatedWords.keySet()) {
					Double dependantWeight;
					dependantWeight = termMap.get(term)
							* relatedWords.get(word);
					relatedTerms.put(word, dependantWeight);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return relatedTerms;
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

		/* find manually the Paths */
		final File docInfo = new File("./CollectionIndex/DocumentsFile.txt");
		// docIDtoPath(docInfo, docIDList);
		Vector<String> keywords = new Vector<String>();

		/* Original words with corresponding weights */
		TreeMap<String, Double> weightenQuery;
		weightenQuery = parseWordWeight(searchForWord.split(" "));

		/*
		 * Stemmed words - Same map could be used by I leave the garbage
		 * collector to its magic
		 */
		TreeMap<String, Double> weightenStemQuery;
		weightenStemQuery = StemTheVectorBaby(weightenQuery);

		/*
		 * Again ... I could use the previous Map but NO. I insist ... garbage
		 * collector must do something. Once again ... java sucks .... cannot
		 * cast float to double ? Here the map includes stemmed words and
		 * relative words
		 */
		TreeMap<String, Double> weightenExtendedQuery;
		weightenExtendedQuery = findRelatedTerms(weightenStemQuery);

		System.out.println(" ===== Extended Map ======");
		System.out.println(weightenExtendedQuery);
		// recalculate the weights for each word

		/*
		 * Do some black magic and find the relevant documents for each word
		 * (stemmed and relative)
		 */
		TreeMap<String, List<String>> relevantDocuments;
		relevantDocuments = searchForDocs(posting, weightenExtendedQuery);

		/* Parfait ... now build the vectors for each document bitch */
		TreeMap<Integer, TreeMap<String, Double>> docVectors;
		docVectors = buildVector(posting, relevantDocuments);

		/* Return the previous tree map with 0 if word does not exist */
		System.out.println(weightenExtendedQuery);
		weightenExtendedQuery = buildWeightenQueryMap(weightenExtendedQuery);

		TreeMap<Integer, Double> similarDocs = compareQueryToDocs(
				weightenExtendedQuery, docVectors);

		printSimilarDocs(similarDocs);
	}

}
