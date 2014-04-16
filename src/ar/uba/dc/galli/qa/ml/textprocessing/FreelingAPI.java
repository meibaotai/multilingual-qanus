package ar.uba.dc.galli.qa.ml.textprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

import com.google.gson.Gson;

import ar.uba.dc.galli.qa.ml.utils.Configuration;
import ar.uba.dc.galli.qa.ml.utils.EnumTypes;
import ar.uba.dc.galli.qa.ml.utils.TextEntity;
import ar.uba.dc.galli.qa.ml.utils.Timer;
import ar.uba.dc.galli.qa.ml.utils.Utils;

import edu.upc.freeling.ChartParser;
import edu.upc.freeling.DepTxala;
import edu.upc.freeling.HmmTagger;
import edu.upc.freeling.LangIdent;
import edu.upc.freeling.ListSentence;
import edu.upc.freeling.ListWord;
import edu.upc.freeling.Maco;
import edu.upc.freeling.MacoOptions;
import edu.upc.freeling.Nec;
import edu.upc.freeling.Senses;
import edu.upc.freeling.Splitter;
import edu.upc.freeling.Tokenizer;
import edu.upc.freeling.Ukb;
import edu.upc.freeling.Util;
import edu.upc.freeling.Word;
import edu.upc.freeling.Ner;


/**
 * Interface for all freeling services. 
 * @author jpeller
 *
 */
public class FreelingAPI {
	

	private static final String FREELINGDIR = "/usr/local";
	private static final String DATA = FREELINGDIR + "/share/freeling/";
	private static final boolean VERBOSE = true;
	
	
	/**
	 * {@link #tokenize(String text) tokenize}
	 */
	public Tokenizer tk;
	
	/**
	 * {@link #split(String text) split}
	 */
	public Splitter sp;
	/**
	 * Morphological analysis
	 */
	public Maco mf;
	public HmmTagger tg;
	public ChartParser parser;
	public DepTxala dep;
	public Nec neclass;
	public Ner ner;
	public Senses sen;
	public Ukb dis;
	public String lang;
	
	public ListSentence ls;
	public String input_string;
	private EnumTypes asked_entity;
	
	private static FreelingAPI instance = null;
		  
	//Singleton pattern to execute only once the system call
	public static FreelingAPI getInstance() 
	{		
		if(instance == null)
		{
			System.loadLibrary( "freeling_javaAPI" );
			Util.initLocale( "default" );
			instance = new FreelingAPI(Configuration.LANG);
		}
		return instance;

	}
	
	//Singleton pattern to execute only once the system call
	public static FreelingAPI getInstance(String langYear) 
	{		
		if(instance == null)
		{
			System.loadLibrary( "freeling_javaAPI" );
			Util.initLocale( "default" );
			instance = new FreelingAPI(Configuration.langFromLangYear(langYear));
		}
		return instance;

	}
	
	
	
	public FreelingAPI(String in_lang)
	{
		lang = in_lang;
		if(lang.compareTo("es") != 0 && lang.compareTo("en") != 0 && lang.compareTo("pt") != 0 && lang.compareTo("simple") != 0)
		{
			System.out.println("FreelingAPI: lang must be 'es', 'en', 'pt' or 'simple'");
			return;
		}
		
		if(lang.compareTo("simple") == 0) lang = "en";
		
		Timer timer = new Timer();
		
		if(VERBOSE) System.out.print("Loading freeling["+lang+"]... ");
			
	    
		tk = new Tokenizer( DATA + lang + "/tokenizer.dat" );
		sp = new Splitter( DATA + lang + "/splitter.dat" );
		
		loadMaco(lang); 
		tg = new HmmTagger( DATA + lang + "/tagger.dat", true, 2 );
	    //parser = new ChartParser(   DATA + LANG + "/chunker/grammar-chunk.dat" );
	    //dep = new DepTxala( DATA + LANG + "/dep/dependences.dat",parser.getStartSymbol() );
		ner = new Ner(DATA + lang + "/np.dat");
		neclass = new Nec( DATA + lang + "/nerc/nec/nec-ab-poor1.dat" );
	    
	    
	    //sen = new Senses(DATA + LANG + "/senses.dat" ); // sense dictionary
	    //dis = new Ukb( DATA + LANG + "/ukb.dat" ); // sense disambiguator
	    
		if(VERBOSE) System.out.println(" [ "+ timer.tic() + " secs ] Ok!");
	}
	

	public void loadMaco(String lang)
	{
		// Create options set for maco analyzer.
	    // Default values are Ok, except for data files.
	    MacoOptions op = new MacoOptions( lang );

	    boolean quantities_and_dictionary = lang.compareTo("pt") != 0;
	    
	    op.setActiveModules(
	      //false, true, true, true, true, true, true, true, true, true, false );
	    	false,
	    	true, 
	    	true,
	    	true,
	    	true,
	    	true,
	    	quantities_and_dictionary, //quantities error if true
	    	quantities_and_dictionary, //dictionary error if true
	    	true,
	    	true,
	    	false
	    	);

	    op.setDataFiles(
	      "",
	      DATA + lang + "/locucions.dat",
	      DATA + lang + "/quantities.dat",
	      DATA + lang + "/afixos.dat",
	      DATA + lang + "/probabilitats.dat",
	      DATA + lang + "/dicc.src",
	      DATA + lang + "/np.dat",
	      DATA + "common/punct.dat",
	      DATA + lang + "/corrector/corrector.dat" );
	    
	    mf = new Maco( op );
	}
	
	/**
	 * Just a nasty System.out.println() alias
	 * @param str
	 */
	public void print(String str)
	{
		System.out.println(str);
	}
	

	/**
	 * Tokenize a text string
	 * 
	 */
	public ListWord tokenize(String text)
	{
		ListWord res = tk.tokenize(text);
		
		return res;
	}
	
	
	public String[] splitString(String text)
	{
		ListSentence sentences = split(tokenize(text));
		ListWord list_word;
		if(sentences.size() == 0)
		{
			String[] res = {text};
			System.out.println("Body: "+text);
			return res;
		}
			
		String[] results = new String[(int) sentences.size()];
		String sentence;
		int i1;
		for (int i = 0; i < sentences.size(); i++)
	    {
	    	list_word = sentences.get(i);
	    	sentence = "";
	    	for (i1 = 0; i1 < list_word.size(); i1++) 
	    	{
	    		sentence+=list_word.get(i1).getForm()+" ";
			}
	    	
	    	if(i1 > 0)
	    		sentence = sentence.substring(0, sentence.length() - 1);
	    	
	    	System.out.println("Setence "+i+": "+sentence);
	    	results[i] = sentence;
	    }
		
		return results;
		
	}

	/**
	 * Splits in sentences a string text
	 * @param text
	 * @return
	 */
	public ListSentence split(String text)
	{
		ListWord list_word = tokenize(text);
		// Split the tokens into distinct sentences.
	    
		ListSentence ls_aux = sp.split( list_word, true );
	    
	    return ls_aux;
	}
	/**
	 * Splits in sentences a ListWord (tokenizer output)
	 * 
	 */
	public ListSentence split(ListWord list_word)
	{
		// Split the tokens into distinct sentences.
	    ListSentence ls_aux = sp.split( list_word, true );

/*	    for (int i = 0; i < ls.size(); i++) {
			print(ls.get(i));
		}
		*/
	    return ls_aux;
	}
	
	//TODO: do
	public String addPunctuation(String text)
	{
		return text;
	}
	
	public void process(String text)
	{
		PrintStream old_out = System.out;
		PrintStream old_err = System.err;
		/*PrintStream n;
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("mitic.log", true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
			
		}
		n = new PrintStream(fos);
		
		System.setErr(n);
		System.setOut(n);
		
	

		//Utils.redirectStdOut(n);
		*/
		input_string = text;
		text = addPunctuation(text);
		
		ls = split(tokenize(text));
		
		//Morphological analysis
		mf.analyze(ls);
		
		// Perform part-of-speech tagging.
	    tg.analyze( ls );
	    
	    // Perform named entity (NE) classificiation.
	    neclass.analyze( ls );
	    
	    //Perform named entity recognition (NER);
	    ner.analyze(ls);
	    
	    //sen.analyze( ls );
	    //dis.analyze( ls );
	    //printResults( ls, "tagged" );

	    // Chunk parser
	    //parser.analyze( ls );
	    //printResults( ls, "parsed" );

	    // Dependency parser
	    //dep.analyze( ls );
	    //printResults( ls, "dep" );
	    
	    //Utils.redirectStdOut(old_out);
	    System.setOut(old_out);
	    System.setErr(old_err);
	    /*n.close();
	    
	    try {
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public String cleanUnderscores(String str)
	{
		return str.replace("_", " ");
	}
	
	/**
	 * check if a string was recognized as a NER in process
	 * @param str
	 * @return
	 */
	public boolean isEntity(String str)
	{
		boolean res = false;
		for(TextEntity e : getEntities())
		{
			if(e.term.compareToIgnoreCase(str) == 0)
			{
				res = true;
			}
		}
		return res;
	}
	
	public ListWord getBaseWords()
	{
		ListWord list_word = new ListWord();
		ListWord aux;
		for (int i = 0; i < ls.size(); i++)
	    {
	    	aux = ls.get(i);
	    	for (int i1 = 0; i1 < aux.size(); i1++) 
	    	{
	    		list_word.pushBack(aux.get(i1));
	    	}
	    }
		return list_word;
	}
	
	public TextEntity[] getQWords()
	{
		
		LinkedList<TextEntity> res = new LinkedList<TextEntity>();
		ListWord list_word;
		Word word;
		String form;
		String tag;
		
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int i1 = 0; i1 < list_word.size(); i1++) 
	    	{
	    		word = list_word.get(i1);
	    		if(isQWord(word))
				{
	    			form =	cleanUnderscores(word.getForm());
	    			//tag  = word.getTag()+lang;
	    			tag  = word.getTag();
					res.add(setQcType(new TextEntity(form, "QWord", "FreelingPOS", tag, lang )));
				}
			}
		}
		return res.toArray(new TextEntity[0]);
	}
	
	public TextEntity setQcType(TextEntity qc)
	{
		if(lang.compareTo("es") == 0)return setQcTypeES(qc);
		else return setQcTypeEN(qc);
	}
	
	public TextEntity setQcTypeES(TextEntity question_word)
    {
		EnumTypes asked_entity;
		
    	
    	//*********************************
    	//ver documento en gdrive
    	
    	//substring(0,2) (PT, PR, PE) rol (respectivamente: interrogativo, relativo, exclamativo)

    	//substring(3,4) 
    	//0 - adonde, como,cuando, donde
    	//C - cual, que, quien
    	//F,M - cuanto,  cuyo
    	
    	//substring(4,5) (S, P, 0) numero
    	
    	
    	String tag = question_word.matched_str;
    	String word = question_word.term;
    	
    	if(tag.substring(0,2).compareToIgnoreCase("PE") == 0)
    	{
    		asked_entity = EnumTypes.val("WHAT");

    	}
    	else if(tag.substring(3,4).compareToIgnoreCase("C") == 0)	//C - cual, que, quien
    	{
    		boolean who = word.compareToIgnoreCase("quién") == 0 || 
    				word.compareToIgnoreCase("quien") == 0;
    		boolean whom = word.compareToIgnoreCase("quiénes") == 0 ||
    				word.compareToIgnoreCase("quienes") == 0 ;
    		boolean witch = word.compareToIgnoreCase("cual") == 0 ||
    				word.compareToIgnoreCase("cuales") == 0 ||
					word.compareToIgnoreCase("cuál") == 0 ||
    				word.compareToIgnoreCase("cuáles") == 0 ;
    				
    				
    		if(who)asked_entity = EnumTypes.val("WHO");
    		else if(whom)asked_entity = EnumTypes.val("WHOM");
    		else if(witch)asked_entity = EnumTypes.val("WITCH");
    		else asked_entity = EnumTypes.val("WHAT");
    		
    		
    	}
    	else if(tag.substring(3,4).compareToIgnoreCase("F") == 0 || tag.substring(3,4).compareToIgnoreCase("M") == 0 )//F,M - cuanto,  cuyo
    	{
    		boolean cuyo = word.compareToIgnoreCase("cuyo") == 0 ||
    				word.compareToIgnoreCase("cuya") == 0 ||
					word.compareToIgnoreCase("cuyos") == 0 ||
    				word.compareToIgnoreCase("cuyas") == 0 ;
    		
    		if(cuyo) asked_entity = EnumTypes.val("NOVALUE");
    		else asked_entity = EnumTypes.val("NUM");
    	}
    	else if(tag.substring(3,4).compareToIgnoreCase("0") == 0 ) // 0 - adonde, como,cuando, donde
    	{
    		boolean where = word.compareToIgnoreCase("donde") == 0 ||
    				word.compareToIgnoreCase("dónde") == 0 ||
					word.compareToIgnoreCase("adonde") == 0 ||
    				word.compareToIgnoreCase("adónde") == 0 ;
    		
    		boolean when = word.compareToIgnoreCase("cuando") == 0 ||
    				word.compareToIgnoreCase("cuándo") == 0;
    		
    		if (where) asked_entity = EnumTypes.val("WHERE");
    		else if (when) asked_entity = EnumTypes.val("WHEN");
    		else asked_entity = EnumTypes.val("NOVALUE");
    	}
    	else
        {
        	asked_entity = EnumTypes.val("NOVALUE");
        }
      
    	return new TextEntity(question_word.term, question_word.type, question_word.subtype, question_word.matched_str, question_word.comparator_used, asked_entity);
			
		
	}
	
	public TextEntity setQcTypeEN(TextEntity question_word)
    {
    	
		EnumTypes asked_entity;
		
    	
    	boolean which = question_word.term.compareToIgnoreCase("which") == 0 ;
    	boolean where = question_word.term.compareToIgnoreCase("where") == 0 ;
    	boolean who = question_word.term.compareToIgnoreCase("who") == 0 ;
    	boolean whom = question_word.term.compareToIgnoreCase("whom") == 0 ;
    	
        if (where) asked_entity = EnumTypes.val("WHERE");
        else if (who && !whom) asked_entity = EnumTypes.val("WHO");
        else if (whom) asked_entity = EnumTypes.val("WHOM");
        else if (which) asked_entity = EnumTypes.val("WHICH");
        else
        {
        	asked_entity = EnumTypes.val("NOVALUE");
        }
      
        return new TextEntity(question_word.term, question_word.type, question_word.subtype, question_word.matched_str, question_word.comparator_used, asked_entity);
			
		
	}
	
    public boolean wordPOSExists(String type)
    {
		
		for (int i = 0; i < ls.size(); i++)
	    	for (int i1 = 0; i1 < ls.get(i).size(); i1++) 
	    		if(ls.get(i).get(i1).getTag().compareToIgnoreCase(type) == 0) return true;

		return false;
			
    }
	
	public TextEntity[] getEntities()
	{
		
		LinkedList<TextEntity> res = new LinkedList<TextEntity>();
		ListWord list_word;
		Word word;
		String form, tag;
		EnumTypes ner_type;
		
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int j = 0; j < list_word.size(); j++) 
	    	{
	    		word = list_word.get(j);
	    		if(isNer(word))
				{
	    			
	    			ner_type = this.getNerType(word);
	    			form =	cleanUnderscores(word.getForm());
	    			
	    			if(form.length() == 1)continue;
	    			
	    			tag  = word.getTag()+lang;
					res.add(new TextEntity(form, word.getTag(), word.getForm(), "", "FreelingAPI("+lang+")", ner_type));
					
				}
			}
		}
		return res.toArray(new TextEntity[0]);
	}
	
	public TextEntity[] getNouns()
	{
		
		LinkedList<TextEntity> res = new LinkedList<TextEntity>()
				;
		ListWord list_word;
		Word word;
		String lemma;
		String tag;
		
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int i1 = 0; i1 < list_word.size(); i1++) 
	    	{
	    		word = list_word.get(i1);
	    		if(isNoun(word))
				{
	    			lemma =	cleanUnderscores(word.getLemma());
		    		tag  = word.getTag();
		    		res.add(new TextEntity(cleanUnderscores(word.getForm()), tag, lemma, "FreelingAPI("+lang+")", "NOUN"));
					
				}
			}
		}
		return res.toArray(new TextEntity[0]);
	}
	
	
	public TextEntity[] getAdjectives()
	{
		
		LinkedList<TextEntity> res = new LinkedList<TextEntity>();
		ListWord list_word;
		Word word;
		String lemma;
		String tag;
		
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int i1 = 0; i1 < list_word.size(); i1++) 
	    	{
	    		word = list_word.get(i1);
	    		if(isAdjective(word))
				{
	    			lemma =	cleanUnderscores(word.getForm());
		    		tag  = word.getTag()+lang;
		    		//tag = "NP000"+lang;
		    		TextEntity e = new TextEntity(lemma, tag);
		    		//e.print();
					res.add(e);
					
				}
			}
		}
		return res.toArray(new TextEntity[0]);
	}
	
	private boolean isAdjective(Word word) {
		return (word.getTag().substring(0,1).compareTo("A") == 0);

	}


	public TextEntity[] getVerbs()
	{
		
		LinkedList<TextEntity> res = new LinkedList<TextEntity>()
				;
		ListWord list_word;
		Word word;
		String lemma;
		String tag;
		
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int i1 = 0; i1 < list_word.size(); i1++) 
	    	{
	    		word = list_word.get(i1);
	    		if(isVerb(word))
				{
	    			
	    			lemma =	cleanUnderscores(word.getLemma());
		    		tag  = word.getTag();
		    		res.add(new TextEntity(cleanUnderscores(word.getForm()), tag, lemma, "FreelingAPI("+lang+")", "VERB"));
		    		//tag = "NP000"+lang;
					//res.add(new TextEntity(cleanUnderscores(word.getForm()), "VERB", "Freeling", tag, lemma+"("+lang+")"));
				}
			}
		}
		return res.toArray(new TextEntity[0]);
	}
	
	
	
	public boolean isNer(Word word)
	{
		return (word.getTag().length() > 1 && word.getTag().substring(0,2).compareTo("NP") == 0);
	}
	
	public EnumTypes getNerType(Word word)
	{
		EnumTypes res;
		if((word.getTag().length() > 5 && word.getTag().substring(4,5).compareTo("G") == 0))
		{
			res = EnumTypes.val("LOCATION");
		}
		else if ((word.getTag().length() > 5 && word.getTag().substring(5,6).compareTo("P") == 0))
		{
			res = EnumTypes.val("PERSON");
		}
		else if ((word.getTag().length() > 5 && word.getTag().substring(4,5).compareTo("O") == 0))
		{
			res = EnumTypes.val("ORGANIZATION");
		}
		else
		{
			res = EnumTypes.val("OTHER");
			//Utils.println("Other");
		}
		
		return res;
	}
	
	public boolean isQWord(Word word)
	{
		if(lang.compareTo("es") == 0)
			return (word.getTag().length() > 2 && word.getTag().substring(0,3).compareTo("PT0") == 0) // quién, qué
					||
					(word.getTag().length() > 1 && word.getTag().substring(0,2).compareTo("PR") == 0) // quien, que
					||
					(word.getTag().length() > 1 && word.getTag().substring(0,2).compareTo("PE") == 0); // qué, bajo carácter exclamativo
		else 
			return 
					(word.getTag().length() > 1 && word.getTag().substring(0,2).compareTo("WP") == 0); 
				
		
	}	
	
	/**
	 * Only works for correct language
	 * @param word
	 * @return
	 */
	public boolean isNoun(Word word)
	{
		return (word.getTag().substring(0,1).compareTo("N") == 0 && !isNer(word));
	}
	
	
	/**
	 * 
	 * @param word
	 * @return
	 */
	public boolean isVerb(Word word)
    {
		//print(word.getForm()+" "+word.getTag());
		String tag = word.getTag();
		
		
		if(tag.substring(0,1).compareToIgnoreCase("V") == 0)
		{
			//print(word.getForm());
			//print(word.getTag());
			//print("VERBO! "+word.getForm()+" "+word.getTag()+" "+word.getLemma());
			//print("Verbo!");
			return true;
		}
		else
		{
			//print(word.getTag());
			return false;
		}
    }
	
	
	
	public static void main( String argv[] ) throws IOException 
	{
		
	     FreelingAPI free_pt = FreelingAPI.getInstance("simple-06");
	     
	     String simple2 = "A ' 'priest' ' or ' 'priestess' ' is a person who is allowed to do religious rites. Their office or position is the ' 'priesthood' ', a word which can also be used for such persons collectively. In most religions and cultures in history there have been priests, although they have a lot of different names, and follow different rules. A ' 'priest' ' is a member of a church that has been told to look after his (spiritual) community. He is the head of a parish. Most protestant religions know nothing about ordaining priests. To become a catholic priest, you are required to study Theology. The Orthodox and Protestant Churches also have laymen as clergy. Catholic priests are not allowed to marry. Orthodox priests can be married, but they must not marry after they become a priest. The Catholic Church does not allow women to become priests.";
	     String simple3 = "Pixar animated movie which debuted in 2003. It stars the voices of Albert Brooks, Alexander Gould, and Ellen Degeneres as fish. The movie is about Nemo, a clownfish who gets lost in the ocean around Australia. His father, Marlin, and Marlin's new friend, Dory, spend the entire movie looking for him. The movie won an Oscar in 2004 for \"Best Animated Film.\"";
	     String simple = "Something is wrong here. Something is just maybe wrong. Something must be done now. Dr Pepe Sanchez was on boarding to New York. What the hell. What the fucking hell! Oh my god, Oh my god. Who is him, my lord? Who is the Majestic that you pointed out as 'superior'? Very ''superior '' you said. Really what if. i. just. start bothering you... LOL! now i start correctly trying to make myself understandable.";
	     String simple4 = " Marshall Bruce Mathers III' ') (born October 17, 1972) is a famous rap artist from Michigan. Eminem has a daughter named Hailie Jade Scott and an ex-wife (and soon to be wife again) named Kim Scott. He has 5 albums under his name that have been released ' 'worldwide' '. He is currently contemplating what to do next with his career; the rumour that he will retire may well be true but has been dismissed by Eminem himself, adding he is just taking time out to see what he wants to do next.";
	     String[] res = free_pt.splitString(simple4);
	     for (int i = 0; i < res.length; i++) {
	    	 System.out.println(res[i]);	
		}
	     
	     res = free_pt.splitString(simple2);
	     for (int i = 0; i < res.length; i++) {
	    	 System.out.println(res[i]);	
		}
	     
	     System.exit(1);
	     String pt = "Na parte inicial da sua história, a astronomia envolveu somente a observação e a previsão dos movimentos dos objetos no céu que podiam ser vistos a olho nu. O Rigveda refere-se aos 27 asterismos ou nakshatras associados aos movimentos do Sol e também às 12 divisões Zodíaco do céu. Os Grécia Antiga fizeram importantes contribuições para a astronomia, entre elas a definição de magnitude aparente. A Bíblia contém um número de afirmações sobre a posição da Terra no universo e sobre a natureza das estrelas e dos planetas, a maioria das quais são poéticas e não devem ser interpretadas literalmente; ver Cosmologia Bíblica. Nos anos 500, Aryabhata apresentou um sistema matemático que considerava que a Terra rodava em torno do seu eixo e que os planetas se deslocavam em relação ao Sol.";
	     
	     free_pt.print("Analizando "+pt);
	     free_pt.process(pt);
	     Utils.print(Utils.concatString(Utils.flattenTextEntities(free_pt.getEntities())));
	     Utils.println("");
	     Utils.print(Utils.concatString(Utils.flattenTextEntities(free_pt.getNouns())));
	     Utils.println("");
	     Utils.print(Utils.concatString(Utils.flattenTextEntities(free_pt.getVerbs())));
	     
	     Utils.println("");
		 Utils.print("Finish OK");
	     if(true) return;
	     
	     FreelingAPI free = new FreelingAPI("es");
		 FreelingAPI free2 = new FreelingAPI("en");
		
		 
		 
		 
		 free.print("Ejecutando freeling sobre input...");
		 String input = "Marcelo Jaime habita en la Gran Ciudad de Buenos Aires. También se la conoce como CABA, tal vez te suene. Los perros tienen mucho orgullo de la limpieza de su barrio.";
		 free.print(input);
		 free.process(input);
		 free2.process(input);
		 free.print("ES");
		 free.getNouns();
		 free.print("EN");
		 free2.getNouns();
		 
		 input = "Marcelo Jaime worked at the Gran Ciudad de Buenos Aires. Almost known as CABA, may be that sounds to you. The dogs are proud of their cleanness.";
		 free.print(input);
		 free.process(input);
		 free2.process(input);
		 free.print("ES");
		 free.getNouns();
		 free.print("EN");
		 free2.getNouns();
		 free.print("Ejecute mil catorce");
	
	}



	public String getAll(String l_Sentence) {
		
		process(l_Sentence);
		//LinkedList<TextEntity> res = new LinkedList<TextEntity>();
		ListWord list_word;
		Word word;
		String form, tag;
		EnumTypes ner_type;
		String res = "";
		for (int i = 0; i < ls.size(); i++)
	    {
	    	list_word = ls.get(i);
	    	for (int j = 0; j < list_word.size(); j++) 
	    	{
	    		word = list_word.get(j);
	    		form =	cleanUnderscores(word.getForm());
	    		res+=form+"/"+word.getTag()+" ";
					//res.add(new TextEntity(form, word.getTag(), word.getForm(), "", "FreelingAPI("+lang+")", ner_type));

			}
	    	
	    }
		return res;
		//Gson gson = new Gson();
		//return gson.toJson(ls);
		

	}
	
	
	
	
}
