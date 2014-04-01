package cs463;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import mitos.stemmer.Stemmer;

public class Scavenger {

	/*
	 * Quick and dirty hack for increased performance instead of using normal
	 * HASH
	 */
	private static class Word {
		int value = 1; // note that we start at 1 since we're counting
		private boolean blacklisted;
		private List<String> documentList = new ArrayList();
		String rootWord;

		public void setRootWord(String rootWord){
			this.rootWord = rootWord;
		}		
		
		public String getRootWord(){
			return this.rootWord;
		}		
		
		public void addDocumentReference(String docName){
			documentList.add(docName);
		}

		public int getDocumentList_size(){
			return documentList.size();
		}		

		public List<String>  getDocumentList(){
			return documentList;
		}		
		
		public void incrAppearances() {
			++value;
		}

		public int getAppearances() {
			return value;
		}

		public boolean isBlacklisted() {
			return blacklisted;
		}

		public void setBlacklisted(boolean blacklisted) {
			this.blacklisted = blacklisted;
		}
	}

	/*
	 * Same hashmap is used both for words and blacklist. Is it is blacklisted,
	 * proper flag is set When we are going to print the results, we must check
	 * if the word is blacklisted and return if it is not
	 */

	static HashMap<String, Word> index = new HashMap<String, Word>();
	private static Scanner tokenize;

	/*
	 * *******************************************
	 * 				INDEXING SECTION 
	 * *******************************************
	 */

	public static void indexWord(String wordString, boolean isBlackList, File fileInfo) {
		Word word = index.get(wordString);
		if (word == null) {
			index.put(wordString, new Word());
			index.get(wordString).setBlacklisted(isBlackList);
		} else {
			word.incrAppearances();
			index.get(wordString).documentList.add(fileInfo.getName());
		}
	}

	public static void printIndex() {
		for (String wordString : index.keySet()) {
			if (index.get(wordString).isBlacklisted() == true) {
				System.out.println("Word : " + wordString + " is blacklisted");
				continue;
			}
			Word word = index.get(wordString);
			System.out.println(wordString + " -> " + word.getAppearances() + "->"  + word.getRootWord() + " -> " + word.getDocumentList_size());
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
					indexWord(tokenize.next(), isBlackList, fileInfo);
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
			
			//System.out.print("Word : " + word + Stemmer.Stem(word));
			index.get(word).setRootWord( Stemmer.Stem(word));
		}		
	}

	/*
	 * *******************************************
	 * 				MAIN
	 * *******************************************
	 */
	
	public static void main(String[] args) {

		//System.exit(0);

		/* First load the blacklist */
		final File blacklistDir = new File("./blacklist");
		loadBlackListFilesOfDir(blacklistDir);

		final File folder = new File("./fileSources");
		loadWordFilesOfDir(folder);
		
		StemTheIndexBaby();
		// File file = new File("./fileSources/file1.txt");
		// indexFile( file );
		printIndex();

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