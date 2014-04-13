package ar.uba.dc.galli.qa.ml.ar;

import java.io.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;



import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ar.uba.dc.galli.qa.ml.ar.featurescoring.FeatureScoringStrategy;
import ar.uba.dc.galli.qa.ml.ar.qasys.Question;
import ar.uba.dc.galli.qa.ml.textprocessing.FreelingAPI;
import ar.uba.dc.galli.qa.ml.textprocessing.FreelingAll;
import ar.uba.dc.galli.qa.ml.textprocessing.StanfordAPI;
import ar.uba.dc.galli.qa.ml.utils.Configuration;
import ar.uba.dc.galli.qa.ml.utils.TextEntity;



import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;
import sg.edu.nus.wing.qanus.framework.commons.*;
import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;


/**
 * Looks at given questions and find the answers for the questions from the
 * knowledge base. This is done by making use of registered retrieval
 * strategy modules.
 *
 * Currently only 1 strategy module is supported, in the sense that we do not
 * have any module to choose between the answers of different strategy modules.
 *
 * TODO Ranker
 * The results of these strategy modules (if more than 1) can be optionally
 * combined with a ranker. This is planned for subsequent releases
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class AnswerRetriever{


	// Name of file where output is placed
	protected String m_OutputFileName;
	protected File m_QuestionSource;
	protected File m_ResultFolder;
	protected File m_LuceneFolder;
	
	protected String m_LangYear;

	// Hold all the retrieved answer strings until they are ready to be written to file.
	protected DataItem m_DataItem_Results;

	// Error analysis module
	protected ErrorAnalyzer m_ErrorAnalyzer = null;

	protected IRegisterableModule l_Module;


	/**
	 * Constructor.
	 * @param a_QuestionsSource [in] folder containing annotated questions in XML files
	 * @param a_ResultFolder [in] folder to write answers to, in the form of XML files
	 * @param file 
	 */
	public AnswerRetriever(File a_LuceneFolder, File a_QuestionsSource, File a_ResultFolder, String a_LangYear ) {

		m_QuestionSource = a_QuestionsSource;
		m_ResultFolder = a_ResultFolder;
		m_DataItem_Results = new DataItem("DOCSTREAM");
		m_LuceneFolder = a_LuceneFolder;
		m_LangYear = a_LangYear;
		
		//FeatureScoringStrategy l_Module = new FeatureScoringStrategy(m_LuceneFolder);

		
	} 


	/**
	 * Sets the error analysis engine to use with this stage engine.
	 * If not set, no error analysis will be carried out
	 *
	 * @param a_ErrorAnalyzer [in] error analysis engine to use
	 */
	public void SetErrorAnalysisEngine(ErrorAnalyzer a_ErrorAnalyzer) {

		m_ErrorAnalyzer = a_ErrorAnalyzer;
		
	} // end SetErrorAnalysisEngine()


	/**
	 * Start to retrieve answers for all of the questions found in the question folder
	 * and output the answers in the result folder.
	 * @return true if all questions processed correctly, false on any errors
	 */
	public boolean Go() {


		//lr = new LuceneReader(index);
		FreelingAPI free = FreelingAPI.getInstance(m_LangYear);
		StanfordAPI stan = new StanfordAPI();
		
		
		Question[] qs = QuestionParser.getQuestions(m_QuestionSource);
		Question[] gr;
		String group_entity;
		
		int cov10, cov08, cov09, exact;
		
		int[] totals = {0,0,0,0};
		int[] antitotals = {0,0,0,0};
		int up_to = Configuration.UP_TO_N_QUESTIONS; //qs.length; //qs.length

		TextEntity[] first_question_ners = {};
		//System.out.println("# value  token  luc    doc    pass   answ   dcovr  pcovr  acovr  dfreq  pfreq  afreq  dspan  pspan  aspan  tokens    titulo       texto");
		//Aca va un traductor
		for (int i = 0; i < up_to ; i++) 
		{
			if(qs[i].isProcessed())continue;
			group_entity = "";
			gr = QuestionParser.getGroup(qs, qs[i].getGroup());
			
			first_question_ners = new TextEntity[0];
			for (int j = 0; j < gr.length && j < up_to; j++) 
			{
				gr[j].setQCType(stan);
		
				gr[j].process(free, first_question_ners);
		
				//System.out.println(gr[j].getQType());
				QuestionParser.getById(qs, gr[j].getId()).setProcessed(true);

			}
		}
		System.out.format("%ntotal: %d, n-passages: %d, exac: %d,  cov10: %d, cov09: %d, cov08: %d %n", 
				up_to, Configuration.N_PASSAGES,  totals[3], totals[0],totals[2],  totals[1]);
		//Utils.println("total-questions: "+(up_to)+", w+", without: "+without_answer);
		//WriteResultsToFile();

		
		// Signal the error analysis engine that everything is over.
		if (m_ErrorAnalyzer != null) {
			m_ErrorAnalyzer.FinishedAnalysis();
		}

		return true;

	} // end Go()



	/**
	* Identifying string.
	* @return a string that uniquely identifies this module
	*/
	public String GetIdentifier() {
		return "GenericAnswerRetriever";
	} // end GetIdentifier()


	public synchronized void Notify(DataItem a_Item) {

		HashMap<String,DataItem> l_Answers = new HashMap<String,DataItem>();
		DataItem l_RankedAnswerTODO = null;


			// TODO questions do not follow an order now

			//System.out.println(a_Item.toXMLString()); // Display received question for debugging if needed

			IStrategyModule l_StrategyModule = null;
			if (l_Module instanceof IStrategyModule) {
				l_StrategyModule = (IStrategyModule) l_Module;			
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerRetriever.class.getName(), "Notify", "Wrong module type.");
			}

			IAnalyzable l_AnalyzableModule = null;
			if (l_Module instanceof IAnalyzable && m_ErrorAnalyzer != null) {
				l_AnalyzableModule = (IAnalyzable) l_Module;

				// Retrieve useful analysis info
				DataItem l_AnalysisInfo = l_AnalyzableModule.GetAnalysisInfoForQuestion(a_Item);
				// Perform analysis
				if (l_AnalysisInfo != null) {
					m_ErrorAnalyzer.PerformAnalysisOnQuestionAndCandidates(l_AnalysisInfo);
				}
			} else {
				
				DataItem l_RetrievedAnswer = l_StrategyModule.GetAnswerForQuestion(a_Item);
				l_RankedAnswerTODO = l_RetrievedAnswer;

				l_Answers.put(l_Module.GetModuleID(), l_RetrievedAnswer);				
				
			} // end if
			


		// TODO Answer ranker
		if (l_RankedAnswerTODO != null) {
			SaveAnswer(l_RankedAnswerTODO);
		}


	} // end Notify()


	/**
	 * Stores the provided answer for later write to file
	 * @param a_AnswerStructure [in] the structure containing the answer string 
	 */
	private void SaveAnswer(DataItem a_AnswerStructure) {
		m_DataItem_Results.AddField("Answer", a_AnswerStructure);		
	} // end WriteAnswerToOutputFile()


	/**
	 * Write the stored answers all to file.
	 */
	private void WriteResultsToFile() {
		
		BufferedWriter l_OutputFile = null;
		try {
			l_OutputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_OutputFileName), "UTF8"));			
		} catch (Exception e) {
			l_OutputFile = null;			
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error preparing temp file : [" + m_OutputFileName + "] ", e);
		}


		try {
			l_OutputFile.write(m_DataItem_Results.toXMLString());
		} catch (Exception e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error writing to temp XML file : [" + m_OutputFileName + "] ", e);
		}

		// Save the temporary file -------------------------------------------------------
		if (l_OutputFile != null) {
			try {				
				l_OutputFile.flush();
				l_OutputFile.close();
				l_OutputFile = null;
			} catch (IOException e) {
				Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error closing file : [" + m_OutputFileName + "] ", e);
			}
		} // end if
		
	} // end WriteResultsToFile()

	
} // end class AnswerRetriever
