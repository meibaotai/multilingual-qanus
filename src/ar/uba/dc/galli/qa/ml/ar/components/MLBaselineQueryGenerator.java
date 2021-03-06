package ar.uba.dc.galli.qa.ml.ar.components;

import java.io.File;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;
import sg.edu.nus.wing.qanus.textprocessing.StopWordsFilter;

import ar.uba.dc.galli.qa.ml.ar.qasys.Question;
import ar.uba.dc.galli.qa.ml.utils.Configuration;
import ar.uba.dc.galli.qa.ml.utils.EnumTypes.QuestionSubType;
import ar.uba.dc.galli.qa.ml.utils.TextEntity;
import ar.uba.dc.galli.qa.ml.utils.Utils;

public class MLBaselineQueryGenerator {

	public MLBaselineQueryGenerator() {
		// TODO Auto-generated constructor stub
	}
	

	public static String generateQuery(String l_QuestionTarget, String l_QuestionText, String l_QuestionPOS, String l_ExpectedAnswerType, Question question)
	{
	
		String l_Query = "";
		
		if(Configuration.QUERYGENERATION == 1)
		{
			l_Query = FormQuery(l_QuestionTarget, question);	
		}
		else if(Configuration.QUERYGENERATION == 2)
		{
			l_Query = FormQuery2(l_QuestionTarget, question);
		}
		else if(Configuration.QUERYGENERATION == 3)
		{
			l_Query = FormQuery3(l_QuestionTarget, question);
		}
		else
		{
			System.out.println("Configuration.QUERYGENERATION not in {1,2,3}");
			System.exit(1);
		}
		
		/*
		QuestionSubType l_SubType = GetQuestionSubType(l_QuestionText, l_QuestionPOS, l_ExpectedAnswerType);
		switch (l_SubType) {
		case HUM_IND_TYPE1:
			//System.out.println(l_QuestionText + " - TYPE1");
			l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);
			break;
		case HUM_IND_TYPE1_IC:
			//System.out.println(l_QuestionText + " - TYPE1-IC");
			l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);
			l_Query += " " + l_QuestionTarget;
			break;
		case HUM_IND_TYPE2:				
			l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);				
			break;
		default:				
			// Build a query string from the question information
			l_Query = FormQuery(l_QuestionTarget, question);				
			break;
		} // end switch
		*/
		return l_Query;
		
	}
	
	
	/**
	 * Attempt to further classify questions into certain specially identified sub-types.
	 *
	 * Currently two sub-types of HUM:ind questions are identified:
	 *  HUM_IND_TYPE1 and HUM_IND_TYPE1_IC:
	 *    Who is the CEO of Microsoft?
	 *    Who is the CEO? (incomplete type - IC)
	 *  HUM_IND_TYPE2
	 *    Who nominated Miers for the post?
	 *    Who supervised the transplant?
	 *
	 * @param a_QuestionText [in] the question
	 * @param a_QuestionTextPOS [in] the question with POS annotations
	 * @param a_MainType [in] the annotated question classification (according to Li and Roth)
	 * @return The identified sub-type of the question.
	 */
	public static QuestionSubType GetQuestionSubType(String a_QuestionText, String a_QuestionTextPOS, String a_MainType) {


		QuestionSubType l_SubType = QuestionSubType.OTHERS;


		if (a_MainType.compareToIgnoreCase("HUM:ind") == 0) {

			// Check for TYPE 1 (hum:ind) questions
			// Of the format "Who is|was < -- Noun Phrase -- >?
			// i.e. Who is director of the mint?
			//      Who is the chief executive of WWE?
			// Incomplete TYPE 1 questions include:
			//      Who is the chairman? (missing the "of...")

			if (a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)")
					|| a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE1_IC;
			} else if (a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")
					|| a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE1;
			} else if (a_QuestionTextPOS.matches("[wW]ho/WP( )([^w]+/VBD(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE2;
			}


		}

		return l_SubType;


	} // end GetQuestionSubType()


	
	/**
	 * We can identify the "subject" of a Type 1 HUM:ind question
	 * A Type 1 question include questions like
	 * Who is the president?
	 * Who is the chief executive of Merill Lynch?
	 * 
	 * The subject in these cases are president and chief execute of Merill Lynch respectively.
	 * 
	 * @param a_QuestionText [in] the question, must be of type HUM:ind TYPE1 or TYPE1_IC
	 * @param a_QuestionTextPOS [in] the question with POS annotations
	 * @param a_SubType [in] subtype of the question (either HUM_IND_TYPE1 or HUM_IND_TYPE1_IC
	 *
	 * @return a string expressing the subject of the question, or null on errors
	 */
	public static String GetSubjectOfHumIndQuestion(String a_QuestionText, String a_QuestionTextPOS, QuestionSubType a_Type) {

		String l_Subject = "";

		switch (a_Type) {
			case HUM_IND_TYPE1_IC:
				//Pattern l_Pattern1 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)");
				Pattern l_Pattern1 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+)");
				//Pattern l_Pattern2 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)");
				Pattern l_Pattern2 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+)");

				Matcher l_Matcher1 = l_Pattern1.matcher(a_QuestionTextPOS);
				Matcher l_Matcher2 = l_Pattern2.matcher(a_QuestionTextPOS);
				if (l_Matcher1.matches()) {
					for (int i = 0; i < l_Matcher1.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher1.group(i);
						} else {
							l_Matcher1.group(i);
						}
					}
				} else if (l_Matcher2.matches()) {
					for (int i = 0; i < l_Matcher2.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher2.group(i);
						} else {
							l_Matcher2.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			case HUM_IND_TYPE1:
				Pattern l_Pattern3 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Pattern l_Pattern4 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Matcher l_Matcher3 = l_Pattern3.matcher(a_QuestionTextPOS);
				Matcher l_Matcher4 = l_Pattern4.matcher(a_QuestionTextPOS);
				if (l_Matcher3.matches()) {
					for (int i = 0; i < l_Matcher3.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher3.group(i);
						} else {
							l_Matcher3.group(i);
						}
					}
				} else if (l_Matcher4.matches()) {
					for (int i = 0; i < l_Matcher4.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher4.group(i);
						} else {
							l_Matcher4.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			case HUM_IND_TYPE2:
				Pattern l_Pattern5 = Pattern.compile("[wW]ho/WP( )([^w]+/VBD(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Matcher l_Matcher5 = l_Pattern5.matcher(a_QuestionTextPOS);
				if (l_Matcher5.matches()) {
					for (int i = 0; i < l_Matcher5.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher5.group(i);
						} else {
							l_Matcher5.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			default:
				// Error
				l_Subject = "";
				break;
		} // end switch


		// Need to strip away POS and trailing question marks
		l_Subject = l_Subject.replaceAll("\\?", "");
		l_Subject = l_Subject.replaceAll("/[A-Z]+", "");


		return l_Subject;


	} // end GetSubjectOfHumIndQuestion()

	/**
	 * Builds a query string with question information.
	 * If POS information is provided with the question text, NN and VB are extracted to
	 * form the query.
	 * If POS information is not available, all individual terms within the question text
	 * are used.
	 *
	 * The formed query is post-processed to ensure that every term is unique,
	 * and stop words are removed.
	 * Stemming is not done because porter's algo does not seem to do a very good job.
	 *
	 * @param a_Target [in] string containing information about the target of the question
	 * @param a_Question [in] string containing the question, can include POS annotations.
	 * @param question 
	 * @return string containing query formed from the question.
	 */
	private static String FormQuery(String a_Target, Question question) {

		// Tracks used word to ensure no repteition of terms in resulting query
		LinkedList<String> l_UsedTerms = new LinkedList<String>();  
		// Used to remove stop words and stem the query
		String[] l_StopWordsFileNames = new String[1];
		l_StopWordsFileNames[0] = Configuration.BASELIBDIR+"lib" + File.separator + "common-word-"+Configuration.getLang()+".txt";
		StopWordsFilter l_StopWords = new StopWordsFilter(l_StopWordsFileNames);

		// Build seach terms dynamically, incorporating relevant information where possible
		// We ensure that the query does not has repeated words by using a LinkedList to collect the hash terms first
		// The linked list helps ensure the terms are ordered unlike if we use a hashset. This could be important
		// as multi-word terms are kept in their correct order.
		String l_Query = "";

		// -- Include the name of the target as part of the query
		StringTokenizer l_ST_Target = new StringTokenizer(a_Target);
		while (l_ST_Target.hasMoreTokens()) {
			String l_Term = l_ST_Target.nextToken();
			if (!l_StopWords.IsStopWord(l_Term)) {
				l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
			}
		}

		// Use POS if available (Always available
		for(TextEntity l_Term : question.getNouns())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getVerbs())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getAdjectives())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);

		for(TextEntity l_Term : question.getNumbers())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);


		// Remove puntuation marks ',', '.', '?', '!' from the end of search terms
		l_Query = l_Query.replaceAll("[,\\.\\?!]", "");


		//
		return l_Query;

	} // end FormQuery()

	private static String FormQuery2(String a_Target, Question question) {

		// Tracks used word to ensure no repteition of terms in resulting query
		LinkedList<String> l_UsedTerms = new LinkedList<String>();  
		// Used to remove stop words and stem the query
		String[] l_StopWordsFileNames = new String[1];
		l_StopWordsFileNames[0] = Configuration.BASELIBDIR+"lib" + File.separator + "common-word-"+Configuration.getLang()+".txt";
		StopWordsFilter l_StopWords = new StopWordsFilter(l_StopWordsFileNames);

		// Build seach terms dynamically, incorporating relevant information where possible
		// We ensure that the query does not has repeated words by using a LinkedList to collect the hash terms first
		// The linked list helps ensure the terms are ordered unlike if we use a hashset. This could be important
		// as multi-word terms are kept in their correct order.
		String l_Query = "";
		String l_Title = "";
		// -- Include the name of the target as part of the query
		StringTokenizer l_ST_Target = new StringTokenizer(a_Target);
		while (l_ST_Target.hasMoreTokens()) {
			String l_Term = l_ST_Target.nextToken();
			if (!l_StopWords.IsStopWord(l_Term)) {
				l_Title += l_Term+" ";
				l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
			}
		}

		// Use POS if available (Always available
		for(TextEntity l_Term : question.getNouns())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getVerbs())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getAdjectives())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);

		for(TextEntity l_Term : question.getNumbers())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);



		// Remove puntuation marks ',', '.', '?', '!' from the end of search terms
		l_Query = l_Query.replaceAll("[,\\.\\?!]", "");


		//
		return "(TITLE: "+l_Title+")^5 OR (ALL:"+l_Query+")";

	} // end FormQuery()
	
	
	/**
	 * \begin{enumerate}
		\item Todas las palabras no stop words entre comillas
		\item Todas las entidades nombradas reconocidas
		\item Todas las construcciones nominales con sus adjetivos
		\item Todas las demás construcciones nominales
		\item Todos los sustantivos con sus adjetivos
		\item Todos los demás sustantivos
		\item Todos los verbos
		\item El target de la pregunta
		\end{enumerate}
	 * @param a_Target
	 * @param question
	 * @return
	 */
	private static String FormQuery3(String a_Target, Question question) {

		// Tracks used word to ensure no repteition of terms in resulting query
		LinkedList<String> l_UsedTerms = new LinkedList<String>();  
		// Used to remove stop words and stem the query
		String[] l_StopWordsFileNames = new String[1];
		l_StopWordsFileNames[0] = Configuration.BASELIBDIR+"lib" + File.separator + "common-word-"+Configuration.getLang()+".txt";
		StopWordsFilter l_StopWords = new StopWordsFilter(l_StopWordsFileNames);

		// Build seach terms dynamically, incorporating relevant information where possible
		// We ensure that the query does not has repeated words by using a LinkedList to collect the hash terms first
		// The linked list helps ensure the terms are ordered unlike if we use a hashset. This could be important
		// as multi-word terms are kept in their correct order.
		String l_Query = "";

		for(TextEntity l_Term : question.getQuotedTokens())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
	
		
		for(String l_Term : question.getPersonNers(false))
			l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
		
		for(String l_Term : question.getOrganizationNers(false))
			l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
		
		for(String l_Term : question.getLocationNers(false))
			l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
		
		for(String l_Term : question.getOtherNers(false)) //NNP
			l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
		
		
		for(TextEntity l_Term : question.getNouns())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getAdjectives())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getVerbs())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);
		
		for(TextEntity l_Term : question.getNumbers())
			l_Query = UpdateQuery(l_UsedTerms, l_Term.term, l_Query);

		
		// -- Include the name of the target as part of the query
		StringTokenizer l_ST_Target = new StringTokenizer(a_Target);
		while (l_ST_Target.hasMoreTokens()) {
			String l_Term = l_ST_Target.nextToken();
			if (!l_StopWords.IsStopWord(l_Term)) {
				l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
			}
		}




		// Remove puntuation marks ',', '.', '?', '!' from the end of search terms
		l_Query = l_Query.replaceAll("[,\\.\\?!]", "");


		//
		return l_Query;

	} // end FormQuery()
	
	/**
	 * Processes a new search term and get a new query string with the term.
	 * Terms that are already used will not be added to the query however.
	 *
	 * @param a_UsedTerms [in] linkied list tracking terms that are already used.
	 * @param a_Term [in] the search term to add
	 * @param a_Query [in] the query to add to
	 * @return a new query string
	 */
	private static String UpdateQuery(LinkedList<String> a_UsedTerms, String a_Term, String a_Query) {

		if (!a_UsedTerms.contains(a_Term.toLowerCase())) {


			a_UsedTerms.add(a_Term.toLowerCase());
			if (a_Query.length() > 0) {
				a_Query += " ";
			}
			a_Query += a_Term;
		}
		return a_Query;

	} // end UpdateQuery()

	

	
}
