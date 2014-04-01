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

public class Scavenger {
	
	/* Quick and dirty hack for increased performance instead of using normal HASH */
	private static class MutableInt {
		  int value = 1; // note that we start at 1 since we're counting
		  private boolean blacklisted;
		  
		  public void increment () { ++value;      }
		  public int  get ()       { return value; }
		  public boolean isBlacklisted() {
			return blacklisted;
		  }
		  public void setBlacklisted(boolean blacklisted) {
			this.blacklisted = blacklisted;
		  }
	}
	
	/* Same hashmap is used both for words and blacklist. Is it is blacklisted, proper flag is set 
	 * When we are going to print the results, we must check if the word is blacklisted and return
	 * if it is not
	 */
	
	static HashMap<String, MutableInt> index = new HashMap<String, MutableInt>();
	private static Scanner tokenize;


	public static void indexWord( String word, boolean isBlackList)
	{
		MutableInt count = index.get(word);
		if (count == null) {
		    index.put(word, new MutableInt());
		    index.get(word).setBlacklisted( isBlackList );
		}
		else {
		    count.increment();
		}
	}
	
	public static void printIndex()
	{
		int freq;
		for (String word: index.keySet())
		{
			if ( index.get(word).isBlacklisted() == true )
			{
				System.out.println("Word : " + word + " is blacklisted");
				continue;
			}
			freq = index.get(word).value;
	        System.out.println( word  + " -> " + freq);
		} 
	}
		

	/* Wrapper for indexFile, especially for loading Blacklists */
	public static void loadBlackListFilesOfDir ( File fileInfo )
	{
		indexFilesOfDir( fileInfo, true);	// true for BlackList
	}
	
	/* Wrapper for indexFile, especially for loading word files */
	public static void loadWordFilesOfDir ( File fileInfo )
	{
		indexFilesOfDir( fileInfo, false); // false for Word File
	}	
	
	private static void indexFilesOfDir(final File dir, boolean isBlackList) {
	    for (final File fileEntry : dir.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	indexFilesOfDir(fileEntry, isBlackList );
	        } else {
	        	indexFile( fileEntry, isBlackList );
	        }
	    }
	}	
	
	private static void indexFile ( File fileInfo, boolean isBlackList )
	{
		BufferedReader reader = null;

		try {
		    reader = new BufferedReader(new FileReader(fileInfo));
		    String text = null;

		    while ((text = reader.readLine()) != null) {
			    tokenize = new Scanner( text );
			    while (tokenize.hasNext()) {
			    	indexWord( tokenize.next(), isBlackList );
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
	
	public static void main(String[] args) {

		/* First load the blacklist */
		final File blacklistDir = new File("./blacklist");
		loadBlackListFilesOfDir(blacklistDir);
		
		
		final File folder = new File("./fileSources");
		loadWordFilesOfDir(folder);
		//File file = new File("./fileSources/file1.txt");
		//indexFile( file );
		printIndex();
		
	}

}
