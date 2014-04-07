package cs463;

//import org.apache.commons.io.FilenameUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

import mitos.stemmer.Stemmer;

public class Scavenger {

	/*
	 * Same TreeMap is used both for words and blacklist. Is it is blacklisted,
	 * proper flag is set When we are going to print the results, we must check
	 * if the word is blacklisted and return if it is not
	 * 
	 * First approach was to use HashMap, but Treemap is better because it also implements sorting
	 */
 
	static TreeMap<String, Word> index = new TreeMap<String, Word>();
	static TreeMap<String, Integer> docIDmap= new TreeMap<String, Integer>();;

	private static Scanner tokenize;

	/*
	 * *******************************************
	 * 				INDEXING SECTION 
	 * *******************************************
	 */

	public static void indexWord(String wordString, int pos,  boolean isBlackList, File fileInfo) {
		Word word = index.get(wordString);
		
		if (word == null) {
			word = new Word();
			
			word.setBlacklisted(isBlackList);	// Obviously isBlackList is set from the outer world
			if ( !isBlackList )
				word.addDocumentRefID( fileInfo.getAbsolutePath(), pos, docIDmap);
				//word.addDocumentReference(fileInfo.getAbsolutePath(), pos );
			
			index.put(wordString, word);
		} else {
			word.incrAppearances();
			
			if ( !isBlackList ) 
				word.addDocumentRefID( fileInfo.getAbsolutePath(), pos, docIDmap);
				//word.addDocumentReference(fileInfo.getAbsolutePath(), pos);
		}
	}

	
	public static void printIndex() {
		for (String wordString : index.keySet()) {
			if (index.get(wordString).isBlacklisted() == true) {
				System.out.println("Word : " + wordString + " is blacklisted");
				continue;
			}
			Word word = index.get(wordString);
			System.out.println(wordString + " -> " + word.isBlacklisted() +  " -> " + word.getAppearances() + " -> "  + word.getRootWord() + " -> " + word.getDocumentListID());
		}
	}
	
	/* Wrapper for indexFile, especially for loading Blacklists */
	public static void loadBlackListFilesOfDir(File fileInfo) {
		indexFilesOfDir(fileInfo, true); // true for BlackList
	}

	/* Wrapper for indexFile, especially for loading word files */
	public static void loadWordFilesOfDir(File fileInfo) {
		indexFilesOfDir(fileInfo, false); // false for Word File
	}

	private static void indexFilesOfDir(final File dir, boolean isBlackList) {
		for (final File fileEntry : dir.listFiles()) {
			if (fileEntry.isDirectory()) {
				indexFilesOfDir(fileEntry, isBlackList);
			} else {
				indexFile(fileEntry, isBlackList);
			}
		}
	}

	private static void indexFile(File fileInfo, boolean isBlackList) {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(fileInfo));
			String text = null;

			while ((text = reader.readLine()) != null) {
				tokenize = new Scanner(text);
				while (tokenize.hasNext()) {
					/* FIXME : fix the way to get the position */
					indexWord(tokenize.next(), 32, isBlackList, fileInfo);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}

	/*
	 * *******************************************
	 * 				STEMMING 
	 * *******************************************
	 */
	
	public static void StemTheIndexBaby() {
		Stemmer.Initialize();
		
		for (String word : index.keySet()) {
			if (index.get(word).isBlacklisted() == true) {
				continue;
			}
			
			//    System.out.print("Word : " + word + Stemmer.Stem(word));
			index.get(word).setRootWord( Stemmer.Stem(word));
		}		
	}


	/*
	 * *******************************************
	 * 				DUMP TO FILES 
	 * *******************************************
	 */
	
	public static void createPosting( File fileInfo ) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(fileInfo));

			for (String wordString : index.keySet()) {
				if (index.get(wordString).isBlacklisted() == true) {
					continue;
				}
				Word word = index.get(wordString);
				
				/* Get the DocList for that word <docs, <positions>>*/
				TreeMap<Integer, ArrayList<Integer>> docListID;
				docListID = word.getDocumentListID();
				
				/* For each document print the corresponding positions */
				for (Integer docID : docListID.keySet()){					
					writer.write( docID + " : " + docListID.get(docID) + "\n" );
					
				}
			}			

		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}	
}
		
	
	
	/* Creates a file with format 
	 * word : Appearances 
	 * 
	 */
	public static void createCollectionIndex( File fileInfo ) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(fileInfo));

			for (String wordString : index.keySet()) {
				if (index.get(wordString).isBlacklisted() == true) {
					continue;
				}
				Word word = index.get(wordString);
				
				for (Integer docID : word.getDocumentListID().keySet())
				{
					writer.write( wordString + " : " + word.getDocumentList_size() + " : " + docID + "\n" );
				}
			}			

		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}	
}			
	
	
/* In case that we want to include subfolders
	if (fileEntry.isDirectory()) {
		createDocumentDescription(fileEntry);
	} else {
*/			

	/* Stores document description to file and also keeps it in a hash list */
	public static TreeMap<String, Integer> createDocumentDescription(final File dir,
			File docDescrFile) {
		int seqNum = 0;
		BufferedWriter writer = null;
		TreeMap<String, Integer> docDescMap = new TreeMap<String, Integer>();
		
		try {
			writer = new BufferedWriter(new FileWriter(docDescrFile));
			
			for (final File fileEntry : dir.listFiles()) {
				/* Write to File */
				writer.write(seqNum + " : " + fileEntry.getAbsolutePath()
						+ " : txt\n");
				
				/* Write to Hash */
				docIDmap.put(fileEntry.getAbsolutePath() , seqNum);
				seqNum++;
			}
		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
		return docDescMap;
	}

	/*
	 * *******************************************
	 * 				MAIN
	 * *******************************************
	 */
	
	public static void main(String[] args) {

		
		/* Create the document collection */
		final File folder = new File("./fileSources");
		final File docDescrFile = new File("./CollectionIndex/DocumentsFile.txt");
		createDocumentDescription( folder, docDescrFile);
		
		/* Load the blacklist */
		final File blacklistDir = new File("./blacklist");
		loadBlackListFilesOfDir(blacklistDir);
		
		/* Load the vocabulary */
		final File vocabularyFolder = new File("./fileSources");
		loadWordFilesOfDir(vocabularyFolder);
		
		/* Stem it */
		StemTheIndexBaby();
		
		/* Create Collection Index */
		final File collectionIndex = new File("./CollectionIndex/VocabularyFiles.txt");
		createCollectionIndex( collectionIndex );

		/* Create Posting */
		final File postingFile = new File("./CollectionIndex/PostingFile.txt");
		createPosting( postingFile );
				
		
		//printIndex();
	}
}

/*
 * class BlackList extends Scavenger {
 * 
 * }
 * 
 * 
 * class WordList extends Scavenger {
 * 
 * }
 */