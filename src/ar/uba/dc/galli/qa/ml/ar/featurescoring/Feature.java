package ar.uba.dc.galli.qa.ml.ar.featurescoring;

/**
 * Base class of features implemented in the FeatureScoring strategy.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 04Jan2010
 */
public abstract class Feature {


	/**
	 * Retrieves the score of a passage, relative to a search string
	 * @param a_SearchStr [in] the array of strings involved in the scoring
	 * @param a_Passage [in] the passage we want to score
	 * @return a score between 0 and 1 for the passage
	 */
	public abstract double GetScore(String[] a_SearchStr, String a_Passage);

	/**
	 * a wrapper for GetScore first parameter type (String[])
	 * @author julian
	 * @param a_SearchString
	 * @param a_Passage
	 * @return
	 */
	public double GetScore(String string, String passage) {
		String[] param = {string};
		return GetScore(param, passage);
	}
} // end class Feature
