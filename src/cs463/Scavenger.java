package cs463;

//import org.apache.commons.io.FilenameUtils;
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
import java.io.Reader;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
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

	public static void indexWord(String wordString, long pos,  boolean isBlackList, File fileInfo) {
		Word word = index.get(wordString);
		
		//System.out.println("PRINT : " + wordString);
		
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

		//"´'`’—‘°�–"
		String delimiter = "[]\t\n\r\f\"\'!@#$%^&*()-_=+,<.>/?:';0123456789+ ";
		StringTokenizer tokenizer = null;
		RandomAccessFile readerRA = null;		
		try {
			readerRA = new RandomAccessFile(fileInfo, "r" );
			String line;
			String UTF8word;
			while( (UTF8word = readerRA.readUTF()) != null ) {
				tokenizer = new StringTokenizer(UTF8word, delimiter);
				while (tokenizer.hasMoreTokens()) {
					indexWord( tokenizer.nextToken() , readerRA.getFilePointer() , isBlackList, fileInfo);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			e.printStackTrace();
		} catch (EOFException e) {
		} catch (UTFDataFormatException e) {			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (readerRA != null) {
					readerRA.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}

	/*
	 * *******************************************
	 * 				STEMMINGs 
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
				TreeMap<Integer, ArrayList<Long>> docListID;
				docListID = word.getDocumentListID();
				
				/* For each document print the corresponding positions */
				for (Integer docID : docListID.keySet()){					
					//writer.write( wordString + " : " + docID + " : " + docListID.get(docID) + "\n" ); USE IT FOR DEBUG
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
	public static void createVocabularyIndex( File fileInfo ) {
		BufferedWriter writer = null;
		/* This is used to keep track of pointers into posting File */
		Integer postPointer = 0;
		try {
			writer = new BufferedWriter(new FileWriter(fileInfo));

			for (String wordString : index.keySet()) {
				if (index.get(wordString).isBlacklisted() == true) {
					continue;
				}
				Word word = index.get(wordString);
				Integer numOfRefs = word.getDocumentListID_size();
				writer.write( wordString + " : " + word.getDocumentListID_size() + " : " + postPointer + "\n" );
				postPointer = postPointer + numOfRefs;
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
		createVocabularyIndex( collectionIndex );

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