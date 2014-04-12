package cs463;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;


public class QueryEvaluator {


	static public  class IndexedWord
	{
		int numOfDocs;
		int startFromDocID;
	}
	static TreeMap<String, IndexedWord> index = new TreeMap<String, IndexedWord>();

	public static void loadVocabulary(File fileInfo) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileInfo), "UTF8"));			
			String line;
			String[] indexParts;
			String word;
			while ((line = reader.readLine()) != null) {
				/* Must be defined here otherwise every word stored in the HASH
				 * is showing to the same address
				 */
				IndexedWord wordInfo = new IndexedWord();

				line = line.replaceAll("\\s+", "");
				indexParts = line.split(":");
				word = indexParts[0];
				wordInfo.numOfDocs = Integer.parseInt( indexParts[1] );
				wordInfo.startFromDocID = Integer.parseInt( indexParts[2] );

				index.put(word, wordInfo);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static List<String> docIDtoPath( File idToPathFile, List<Integer> docIDlist)
	{
		System.out.println("Convert IDs to Paths");
		/* Sort the List so we don't lost anything */
		
		if ( docIDlist == null ||  docIDlist.size() == 0 )
		{
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
				while ( skipLines != docId )
				{
					reader.readLine();
					skipLines++;
				}
				/* Print the stuff */
				String docInfo = reader.readLine();	
				System.out.println(docInfo);				
				skipLines++; // To falsify the condition above
			}
		}
		finally 
		{
			return null;			
		}
	}
	
	
	
	public static List<Integer> searchForDocs( File postingFile, String findMe )
	{
		System.out.println(" *** START FINDING ****");
		IndexedWord wordInfo;
		List<Integer> docIDs = new ArrayList<Integer>();
		System.out.println("Find word : " + findMe);
		wordInfo = index.get(findMe);
		System.out.println("Word : " + findMe );
		System.out.println("Appearances : " + wordInfo.numOfDocs);
		System.out.println("Pointer : " + wordInfo.startFromDocID);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(postingFile));
			int skipLines = 0;
			/* Skip lines until we reach the target */
			while( skipLines <  wordInfo.startFromDocID  ){
				reader.readLine();
				skipLines++;
			}
			
			int docsRead = 0;
			while ( docsRead < wordInfo.numOfDocs )
			{
				String docInfo = reader.readLine();	
				docInfo = docInfo.replaceAll("\\s+", "");
				String[] docInfoParts = docInfo.split(":");
				docIDs.add( new Integer( docInfoParts[0] ));
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


		return docIDs;
	}


	public static void main(String[] args) {

		/* Load the vocabulary */
		final File vocabularyIndex = new File(
				"./CollectionIndex/VocabularyFiles.txt");
		loadVocabulary(vocabularyIndex);

		/*find manually the docIDs*/
		final File posting = new File(
				"./CollectionIndex/PostingFile.txt");
		List<Integer> docIDList = searchForDocs(posting, "wight");
		//searchDocIDs(posting, "wheat");

		/*find manually the Paths*/
		final File docInfo = new File(
				"./CollectionIndex/DocumentsFile.txt");		
		docIDtoPath(docInfo, docIDList);
		//searchDocIDs(posting, "wheat");		
		
	}

}
