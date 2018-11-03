/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nour.subtitle.editor.translate;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import nour.subtitle.editor.database.DatabaseOperations;
import nour.subtitle.editor.showSrt.Subtitle;
import nour.subtitle.editor.srt.utils.DBConnection;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author NourSoft
 */
public class OfflineTranslationOperation {

    Connection connection = DBConnection.getConnection();
    private static Statement statement = null;
    ObservableList<Subtitle> list = FXCollections.observableArrayList();
    List<String> matching = new ArrayList<String>();

    List<String> subjectsResult = new ArrayList<String>();
    List<String> verbsResult = new ArrayList<String>();
    List<String> objectsResult = new ArrayList<String>();

    List<String> finalResult = new ArrayList<String>();

    public List<String> analyseNLP(String text) {

        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        /**
         * tokenize / TokenizerAnnotator - Tokenizes the text. This splits the
         * text into roughly “words”, using rules or methods suitable for the
         * language being processed.
         *
         * ssplit / WordsToSentencesAnnotator - Splits a sequence of tokens into
         * sentences.
         *
         * pos / POSTaggerAnnotator - Labels tokens with their POS tag.
         *
         * lemma / MorphaAnnotator - Generates the word lemmas for all tokens in
         * the corpus.
         *
         * ner / NERClassifierCombiner - Recognizes named (PERSON, LOCATION,
         * ORGANIZATION, MISC), numerical (MONEY, NUMBER, ORDINAL, PERCENT), and
         * temporal (DATE, TIME, DURATION, SET) entities.
         *
         * parse / ParserAnnotator - Provides full syntactic analysis, using
         * both the constituent and the dependency representations. The
         * constituent-based output is saved in TreeAnnotation.
         *
         * dcoref / DeterministicCorefAnnotator - Implements both pronominal and
         * nominal coreference resolution.
         */
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods

            System.out.println("******************************SENTENCE ANALYSIS**************************************\n");

            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                System.out.println("Word: " + word + " <---> PartOfSpeech: " + pos + " <---> SyntacticCategory: " + ne);

                matching.add(word);
                partOfSpeechControl(pos);

            }

            System.out.println("****************************************************************************************\n");

            System.out.println("******************************PART OF SPEECH ANALYSIS************************************\n");
            System.out.println("### = " + subjectsResult.size() + " ### " + "Subject found");

            System.out.println("### = " + objectsResult.size() + " ### " + "Object found");

            System.out.println("### = " + verbsResult.size() + " ### " + "Verbs found");
            System.out.println("*****************************************************************************************\n");

            for (String subject : subjectsResult) {
                finalResult.add(subject);
            }

            for (String subject : objectsResult) {
                finalResult.add(subject);
            }

            for (String subject : verbsResult) {
                finalResult.add(subject);
            }

            // this is the parse tree of the current sentence
            // -->       Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            // -->      System.out.println("parse tree:\n" + tree);
            // this is the Stanford dependency graph of the current sentence
            // -->     SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            // -->     System.out.println("dependency graph:\n" + dependencies);
        }

        return matching;

    }

    public String oflineTranslate(String text, String fromLan) {
        PreparedStatement ps = null;
        String result = null;
        String escapedSQL = StringEscapeUtils.escapeSql(text);

        boolean dot = false;
        boolean exist = false;
        boolean exist2 = false;

        String words[] = text.split(" ");
        int size = words.length;
        String words2;

        for (int i = 0; i < size; i++) {
            String word = words[i];

            if (words[i].contains(".")) {
                dot = true;
                words[i] = words[i].substring(0, words[i].length() - 1);

            }

            try {

                if (fromLan.equals("English")) {
                    String getTrSub = "SELECT turkish_word FROM  dictionary WHERE english_word LIKE '%" + words[i] + "%'";

                    ps = connection.prepareStatement(getTrSub);

                } else if (fromLan.equals("Turkish")) {
                    String getEnSub = "SELECT english_word FROM  dictionary WHERE turkish_word LIKE '%" + words[i] + "%'";
                    ps = connection.prepareStatement(getEnSub);
                }
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    result = "Mohamed Nourdine";
                } else {

                    do {
                        if (fromLan.equals("English")) {
                            result = rs.getString("turkish_word");

                        } else if (fromLan.equals("Turkish")) {
                            result = rs.getString("english_word");
                        }
                    } while (rs.next());
                }

            } catch (SQLException ex) {
                Logger.getLogger(DatabaseOperations.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return result;
    }

    public void partOfSpeechControl(String partOfSpeech) {
        String result = null;
        // Lets give the different part of speach categories for our sentences
        String subjects[] = {"CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT",
            "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH"};

        String verbs[] = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};

        String objects[] = {"WDT", "WP", "WP", "WRB"};

        for (String subject : subjects) {
            if (partOfSpeech.equalsIgnoreCase(subject)) {
                subjectsResult.add(result);
            }
        }

        for (String subject : verbs) {
            if (partOfSpeech.equalsIgnoreCase(subject)) {
                verbsResult.add(result);
            }
        }

        for (String subject : objects) {
            if (partOfSpeech.equalsIgnoreCase(subject)) {
                objectsResult.add(result);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        List<String> result = new ArrayList<String>();
        OfflineTranslationOperation offline = new OfflineTranslationOperation();

        result = offline.analyseNLP("Mohamed is a good boy");

        for (String string : result) {
            System.out.print(string + "-->");
        }

    }
}
