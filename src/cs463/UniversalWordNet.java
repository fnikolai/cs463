package cs463;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.lexvo.uwn.Entity;
import org.lexvo.uwn.Statement;
import org.lexvo.uwn.UWN;

public class UniversalWordNet {

	private UWN uwn = null;

	public UniversalWordNet() throws Exception {
		// Instantiate UWN, providing a pointer to the plugins directory.
		// String plugins =
		// UniversalWordNet.class.getResource("plugins/").getPath();
		uwn = new UWN(new File("plugins/"));
	}

	// The ISO 639-3 code for Greek is "ell". For English use "eng". Please
	// refer to
	// http://www.sil.org/iso639-3/codes.asp
	// for other language codes.
	public String getLanguage(String term) {
		// Java uses Unicode
		// Modern Greek in unicode are between 0370 - 03FF
		// Basic Latin 0021 - 007E
		char c = term.charAt(0); // Get the first character of the word
		if (c >= (char) 880 && c <= (char) 1023) {
			// If character is between 0x0370 - 0x03FF
			return "ell"; // return the greek
		} else if (c >= (char) 33 && c <= (char) 126) {
			// If character is // between 0x0021 - 0x007E
			return "eng"; // return english language
		} else {
			return "eng"; // In all other cases return again eng
		}
	}

	public void exampleUsageOfUWN(String term) throws IOException {

		Entity entity = Entity.createTerm(term, this.getLanguage(term));

		// Get the relations for this term
		// Such relations can be
		// rel:means (synset, i.e. the set of terms that have the same meaning)
		// e.g. s/n7212190
		// rel:has_derived_form
		// rel:etymological_origin_of
		// rel:etymology
		// rel:is_derived_from
		// rel:lexicalization (i.e. different lexicalizations for different
		// languages)
		// rel:similar
		// rel:closely_related
		// rel:opposite
		// rel:subclass (i.e. hyponym of)
		// rel:has_subclass (i.e. hypernym of)
		// rel:instance
		// rel:has_instance
		// rel:has_gloss (i.e. snippet explaining the meaning)
		// rel:part_of (i.e. meronym)
		// rel:has_part (i.e. holonym)
		// rel:lexical_category (i.e. is it a noun, an adjective, etc...)

		Iterator<Statement> it = uwn.get(entity);

		// Iterate over the statements
		while (it.hasNext()) {
			Statement stmt = it.next();

			// Statements are of the form:
			// subject predicate object
			Entity subject = stmt.getSubject();
			Entity object = stmt.getObject();
			Entity predicate = stmt.getPredicate();

			// Print the relation
			System.out.println("Entity: " + subject.getId() + " Relation "
					+ predicate.getId() + " :\"" + object.getId()
					+ "\" with weight " + stmt.getWeight());

			// If this relation is a meaning, check all related words
			if (predicate.getId().equals("rel:means")) {
				System.out.println("Words related with this meaning:");
				Iterator<Statement> meanings = uwn.getTermEntities(object);
				while (meanings.hasNext()) {
					Statement meaning = meanings.next();
					Entity objectOfMeaning = meaning.getObject();
					Entity predicateOfMeaning = meaning.getPredicate();
					Entity subjectOfMeaning = meaning.getSubject();

					// Print only greek and english related words for these
					// meanings
					if (predicateOfMeaning.getId().equals("rel:lexicalization")
							&& (objectOfMeaning.getTermLanguage().equals("ell") || objectOfMeaning
									.getTermLanguage().equals("eng"))) {
						System.out.println("\t" + objectOfMeaning.getTermStr());
					}
				}

				System.out.println("Other relations of this meaning:");
				meanings = uwn.get(object);
				while (meanings.hasNext()) {
					Statement meaning = meanings.next();
					Entity objectOfMeaning = meaning.getObject();
					Entity predicateOfMeaning = meaning.getPredicate();
					Entity subjectOfMeaning = meaning.getSubject();

					// Print only greek and english meanings
					if (!(predicateOfMeaning.getId()
							.equals("rel:lexicalization"))) {
						System.out.println("\tEntity: "
								+ subjectOfMeaning.getId() + " Relation "
								+ predicateOfMeaning.getId() + " :\""
								+ objectOfMeaning.getId() + "\" with weight "
								+ stmt.getWeight());
					}
				}
			}
			System.out.println();

		}

		System.out.println();

	}

	public TreeMap<String, Float>  findRelatedTerms(String term) throws IOException {

		TreeMap<String, Float> relatedTems = new TreeMap<String, Float>();
		Entity entity = Entity.createTerm(term, this.getLanguage(term));

		Iterator<Statement> it = uwn.get(entity);
		TreeMap tm = new TreeMap();
		// Iterate over the statements
		while (it.hasNext()) {
			Statement stmt = it.next();

			// Statements are of the form:
			// subject predicate object
			Entity subject = stmt.getSubject();
			Entity object = stmt.getObject();
			Entity predicate = stmt.getPredicate();

			// If this relation is a meaning, check all related words
			if (predicate.getId().equals("rel:means")) {

				Iterator<Statement> meanings = uwn.getTermEntities(object);
				while (meanings.hasNext()) {
					Statement meaning = meanings.next();
					Entity objectOfMeaning = meaning.getObject();
					Entity predicateOfMeaning = meaning.getPredicate();
					Entity subjectOfMeaning = meaning.getSubject();

					// Print only greek and english related words for these
					// meanings
					if (predicateOfMeaning.getId().equals("rel:lexicalization")
							&& (objectOfMeaning.getTermLanguage().equals("ell") || objectOfMeaning
									.getTermLanguage().equals("eng"))) {
						tm.put(objectOfMeaning.getTermStr(), stmt.getWeight());

					}
				}
			}
		}

		// Get a set of the entries
		Set set = tm.entrySet();
		// Get an iterator
		Iterator i = set.iterator();
		// Display elements
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			relatedTems.put( me.getKey().toString(),  (Float) me.getValue());
//			System.out.print(me.getKey() + ": ");
//			System.out.println(me.getValue());
		}
		System.out.println();
		return relatedTems;

	}

	/**
	 * @param args
	 * @throws Exception
	 * @throws java.io.IOException
	 */
	public static void main(String[] args) throws Exception, IOException {

		UniversalWordNet test = new UniversalWordNet();

		System.out.println("=====Example #1=====\n");
		// Term to use
		String term = "μικρός";
		test.exampleUsageOfUWN(term);

		System.out.println("=====Example #2=====\n");

		term = "information";
		test.exampleUsageOfUWN(term);

		System.out.println("=====Example #3=====\n");
		term = "school";
		test.exampleUsageOfUWN(term);

		System.out.println("=====Example #4=====\n");
		term = "high school";
		test.exampleUsageOfUWN(term);
	}
}
