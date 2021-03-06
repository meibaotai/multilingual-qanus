package ar.uba.dc.galli.qa.ml.textprocessing;


import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Wrapper over Stanford's CRF NER engine by the Stanford NLP group.
 * (http://nlp.stanford.edu/)
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class FreelingAll implements ITextProcessingModule {


	private FreelingAPI l_freelingAPI;
	/**
	 * Constructor.
	 * @param lang [in] String with the lang token identifier
	 */
	public FreelingAll() {
		l_freelingAPI = FreelingAPI.getInstance();
	} // end constructor


	/**
	 * Takes in a set of sentences, and performs named entity recognition with the sentences.
	 *
	 * @param a_Sentences [in] array containing sentences to be tagged
	 * @return null on errors, or array of tagged sentences.
	 */
	public String[] ProcessText(String[] a_Sentences) {
				
		if (a_Sentences == null) {
			return null;
		}

		LinkedList<String> l_ParsedSentences = new LinkedList<String>();

		// Parse each sentence
		for (String l_Sentence : a_Sentences) {

			try {
				String l_Result = l_freelingAPI.getAll(l_Sentence);
				l_ParsedSentences.add(l_Result);
			} catch (Exception e) {								
				Logger.getLogger("QANUS").log(Level.WARNING, "Unable to perform NER on sentence [" + l_Sentence + "]");
			}

		}

		// Return parsed sentences
		return l_ParsedSentences.toArray(new String[0]);

	} // end ProcessText()

	public String GetModuleID() {
		return "FreeAll";
	}

} // end class StanfordNER
