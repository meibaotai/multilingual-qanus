package ar.uba.dc.galli.qa.ml.ar.qasys;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;

import edu.upc.freeling.ListSentence;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;

import ar.uba.dc.galli.qa.ml.ar.Controller;
import ar.uba.dc.galli.qa.ml.textprocessing.FreelingAPI;
import ar.uba.dc.galli.qa.ml.textprocessing.StanfordAPI;
import ar.uba.dc.galli.qa.ml.utils.Configuration;
import ar.uba.dc.galli.qa.ml.utils.EnumTypes;
import ar.uba.dc.galli.qa.ml.utils.TextEntity;
import ar.uba.dc.galli.qa.ml.utils.Utils;
import ar.uba.dc.galli.qa.ml.utils.comparatos.EqualNoPunctComparator;

public class Question {

	private String id;
	private String group;
	private String question;
	private String answer;
	private String question_en;
	
	private String q_type;
	private String q_ans;
	private String support;

	private String[] old_group_entities;
	private String[] all_entities;
	

	private boolean nlp_processed = false;
	public TextEntity[] entities;
	public TextEntity[] verbs;
	public TextEntity[] nouns;
	public TextEntity[] adjectives;
	
	public String[] l_annotatedNER;
	public String[] l_annotatedPOS;
	
	public EnumTypes qc_class;
	public EnumTypes qc_subclass;
	public double qc_confidence;

	public TextEntity[] first_question_ners = {};
	
	private boolean processed = false;

	public boolean isProcessed() {return processed;}
	public void setProcessed(boolean processed) {this.processed = processed;}

	public Logger LOGGER = Logger.getLogger(Controller.class .getName());
	public String qc_all;

	
	public Question(String id, String group, String question, String answer, String question_en, String[] in_group_entities, String q_type, String q_ans, String support) {
		setId(id);
		setGroup(group);
		setText(question);
		setAnswer(answer);
		setQuestionEn(question_en);
		setQType(q_type);
		setSupport(support);
		setQAns(q_ans);
		old_group_entities= in_group_entities;
	}

	public void annotate(TextEntity[] in_first_question_ners)
	{
		first_question_ners = in_first_question_ners;
		stanfordAnnotation();
		freelingAnnotation();
		
	
	}
	
	public void stanfordAnnotation()
	{
		String[] question = {this.getQuestionEn()};
		setQCType();
		l_annotatedNER = StanfordAPI.getInstance().ner.ProcessText(question);
		l_annotatedPOS = StanfordAPI.getInstance().pos.ProcessText(question);
	}
	
	public void freelingAnnotation()
	{
		FreelingAPI free = FreelingAPI.getInstance();
		if(!nlp_processed)
		{
			ListSentence ls = free.process(question);
			entities = free.getEntities(ls);
			verbs = free.getVerbs(ls);
			nouns = free.getNouns(ls);	
			adjectives = free.getAdjectives(ls);
			nlp_processed = true;
			//print();
		}
	}
		
	private void print() {
		
		
		System.out.format("%n%nPregunta: %s ner: %s, verb: %s, noun: %s, adj: %s, gne: %s %n", this.getText(), 
				Utils.toJson(entities), Utils.toJson(verbs), Utils.toJson(nouns), 
				Utils.toJson(adjectives), Utils.toJson(first_question_ners));
		// TODO Auto-generated method stub
		
	}
	public boolean hasFirstQuestionNers()
	{
		return first_question_ners.length > 0;
	}
	
	public TextEntity[] getFirstQuestionNers()
	{
		return first_question_ners;
	}

	
	public boolean hasOnlyOneEntity()
	{
		return entities.length == 1;
	}

	public TextEntity[] getEntities()
	{
		return entities;
	}

	public TextEntity[] getVerbs()
	{
		return verbs;
	}

	public TextEntity[] getNouns()
	{
		return nouns;
	}

	public String entity()
	{
		return entities[0].term;
	}
	
	public String getNersAndNounsString()
	{
		return Utils.concatString(Utils.flattenTextEntities(entities))+" "+Utils.concatString(Utils.flattenTextEntities(nouns))+" "+Utils.concatString(Utils.flattenTextEntities(adjectives));
	}
	
	public String getGroupEntity()
	{	
		if(Configuration.GROUP_ENTITY_NER_AND_NOUNS)
			return getNersAndNounsString();
		
		
		if(entities.length > 0 ) 
			return Utils.concatString(Utils.flattenTextEntities(entities));
		
		if(nouns.length > 0)
			return Utils.concatString(Utils.flattenTextEntities(nouns));
		
		return clean(getText());
			
	}
	
	private String[] generateQueries1(FreelingAPI free, String group_entity)
	{
		//LOGGER.info("generateQueries1");
		freelingAnnotation();
		String[] queries = {};

		queries = ArrayUtils.add(queries,String.format("ALL:(%s)",clean(question)));
		
		return queries;
		
	}
	
	private String[] generateQueries2(FreelingAPI free, String group_entity)
	{
		//LOGGER.info("generateQueries1");
		freelingAnnotation();
		String[] queries = {};

		if(!group_entity.isEmpty())
		{
			queries = ArrayUtils.add(queries,String.format("ALL:(%s %s)", group_entity,clean(question)));
		}
		else
		{
			queries = ArrayUtils.add(queries,String.format("ALL:(%s)",clean(question)));
		}
		
		return queries;
		
	}
	
	private String[] generateQueries3(FreelingAPI free, String group_entity)
	{
		//LOGGER.info("generateQueries4");
		freelingAnnotation();
		String[] queries = {};


		if(!group_entity.isEmpty())
		{
			queries = ArrayUtils.add(queries, String.format("TITLE:(\"%s\")^200", group_entity));
			queries = ArrayUtils.add(queries, String.format("TITLE:(%s)^100", group_entity));
		}
		
		for(TextEntity e : entities)
			queries = ArrayUtils.add(queries, String.format("TITLE:(\"%s\")^2", e.term));

		String all_entities = Utils.concatString(Utils.flattenTextEntities(entities));
		

		if(!all_entities.isEmpty()){
			queries = ArrayUtils.add(queries, String.format("TITLE:(%s %s)^2", group_entity, all_entities));
			queries = ArrayUtils.add(queries, String.format("ALL:(%s %s)", group_entity,all_entities));	
		}

		String all = all_entities+" "+Utils.concatString(Utils.flattenTextEntities(nouns));

		queries = ArrayUtils.add(queries, String.format("TITLE:(%s %s)^2", group_entity, all));
		queries = ArrayUtils.add(queries, String.format("ALL:(%s %s)", group_entity,all));	

		all+=" "+Utils.concatString(Utils.flattenTextEntities(verbs));;
		
		queries = ArrayUtils.add(queries,String.format("ALL:(%s %s)", group_entity,all));
		queries = ArrayUtils.add(queries,String.format("ALL:(%s %s)", group_entity,clean(question)));
		
		
		String huge_query = "("+queries[0]+") OR ";
		for (int i = 1; i < queries.length; i++) {
			huge_query+= "("+queries[i]+") OR ";
		}
		
		huge_query = huge_query.substring(0, huge_query.length() - 3);
		String[] res = {huge_query};
		return res;
	}
	
	
	
	private String[] generateQueries4(FreelingAPI free, String group_entity)
	{
		//LOGGER.info("generateQueries4");
		freelingAnnotation();
		String[] queries = {};

		if(!group_entity.isEmpty())
		{
			queries = ArrayUtils.add(queries, String.format("TITLE:(\"%s\")^200", group_entity));
			queries = ArrayUtils.add(queries, String.format("TITLE:(%s)^100", group_entity));
		}
		
		for(TextEntity e : entities)
			queries = ArrayUtils.add(queries, String.format("TITLE:(\"%s\")^2", e.term));

		String all_entities = Utils.concatString(Utils.flattenTextEntities(entities));
		

		if(!all_entities.isEmpty()){
			queries = ArrayUtils.add(queries, String.format("TITLE:(%s %s)^2", group_entity, all_entities));
			queries = ArrayUtils.add(queries, String.format("ALL:(%s %s)", group_entity,all_entities));	
		}

		String all = all_entities+" "+Utils.concatString(Utils.flattenTextEntities(nouns));

		queries = ArrayUtils.add(queries, String.format("TITLE:(%s %s)^2", group_entity, all));
		queries = ArrayUtils.add(queries, String.format("ALL:(%s %s)", group_entity,all));	

		all+=" "+Utils.concatString(Utils.flattenTextEntities(verbs));;
		
		queries = ArrayUtils.add(queries,String.format("ALL:(%s %s)", group_entity,all));
		queries = ArrayUtils.add(queries,String.format("ALL:(%s %s)", group_entity,clean(question)));
		return queries;
	}
	
	
	

	public String[] generateQueries(FreelingAPI free, String group_entity)
	{
		
		if(Configuration.QUERYGENERATION == 1)
		{
			return generateQueries1(free, group_entity);	
		}
		else if(Configuration.QUERYGENERATION == 2)
		{
			return generateQueries2(free, group_entity);
			
		}else if (Configuration.QUERYGENERATION == 3)
		{
			return generateQueries3(free, group_entity);
			
		}else if(Configuration.QUERYGENERATION == 4)
		{
			return generateQueries4(free, group_entity);
		}
		else
		{
			LOGGER.severe("No query generator selected. All is doomed from now.");
			return null;
		}
		
		


	}

	public String clean(String in)
	{
		return EqualNoPunctComparator.remove(in);
	}

	public String generateQuery()
	{
		return getText();
	}

	public String generateQueryWith(Question first_in_group)
	{
		return getText()+" "+first_in_group.getText();
	}

	public void setQCType()
	{
		StanfordAPI stan = StanfordAPI.getInstance();
		stan.load(this.getQuestionEn());
		try {
			stan.qc();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		
		qc_all = stan.qc_res;
		qc_class = stan.qc_class;
		qc_subclass = stan.qc_subclass;
		qc_confidence =  Double.parseDouble(stan.qc_confidence);


	}

	public String getId() {
		return id;
	}

	public String getGroup() {
		return group;
	}

	public String getText() {
		return question;
	}
	public String getShortText(){return question.substring(0, question.length() > 30 ? 30: question.length() );}

	public void setId(String id) {
		this.id = id;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public void setText(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String[] getOldGroupEntities() {
		return old_group_entities;
	}
	public void setOldGroupEntities(String[] group_entities) {
		this.old_group_entities = group_entities;
	}
	public String getQType() {
		return q_type;
	}
	public void setQType(String q_type) {
		this.q_type = q_type;
	}
	public String getQAns() {
		return q_ans;
	}
	public void setQAns(String q_ans) {
		this.q_ans = q_ans;
	}
	public String getSupport() {
		return support;
	}
	public void setSupport(String support) {
		this.support = support;
	}
	
	public String getQuestionEn() {return question_en;}
	public void setQuestionEn(String question_en) {this.question_en = question_en;}

	
	public DataItem toDataItem()
	{
		DataItem me = new DataItem("question");
		me.AddAttribute("id", this.getId());
		me.AddAttribute("type", this.getQType());
		me.AddField("q", this.getQuestionEn());
		me.AddAttribute("Target",  this.getQuestionEn());
		me.AddField("Q-QC", this.qc_all);
		me.AddField("Q-POS", this.l_annotatedPOS[0]);
		me.AddField("Q-NER", this.l_annotatedNER[0]);
	/*	<Q-POS id="555.2">Who/WP was/VBD the/DT title/NN sponsor/NN of/IN the/DT team?/NN </Q-POS>

		<Q-NER id="555.1">What/O does/O WMSC/ORGANIZATION stand/O for/O ?/O </Q-NER>

		<q id="555.1" type="FACTOID">What does WMSC stand for?</q>


		<Q-FreeAll id="555.1">What/WP does/VBZ WMSC/NP00000 stand/VB for/IN ?/Fit </Q-FreeAll>


		<Q-QC id="555.1">ABBR:exp</Q-QC>

		*/
		return me;
	}
	


}
