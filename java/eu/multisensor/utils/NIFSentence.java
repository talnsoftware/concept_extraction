package eu.multisensor.utils;

import edu.upf.taln.nif.annotation.SentenceAnnotation;
import edu.upf.taln.nif.annotation.TextAnnotation;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class NIFSentence
{

	private final static Logger log = LoggerFactory.getLogger(NIFSentence.class);

	public String text;
	public int end;
	public int start;
	public List<NIFToken> tokens = new ArrayList<NIFToken>();
	public String[] tokenArray;

	@Override
	public String toString()
	{
		return "(" + start + "," + end + ")=" + text;
	}

	public String[] getTokenArray()
	{
		return tokenArray;
	}

	public static List<NIFSentence> createSents(TreeSet<SentenceAnnotation> sentencesAndTokens, String body)
	{

		ArrayList<NIFSentence> list = new ArrayList<NIFSentence>();
		for (SentenceAnnotation sentence : sentencesAndTokens)
		{
			Pair<Integer, Integer> sentSpan = sentence.getOffsets();

			NIFSentence nifSent = new NIFSentence();
			nifSent.start = sentSpan.getLeft();
			nifSent.end = sentSpan.getRight();

			try
			{
				nifSent.text = body.substring(nifSent.start, nifSent.end);

			}
			catch (Exception e)
			{
				if (nifSent.end > body.length())
				{
					log.error("Sentence end offset (" + nifSent.end + ") outside of body range (length: " + body.length() + ").");

				}
				else
				{
					log.error("Unexpected error");
				}
				throw e;
			}

			nifSent.tokenArray = new String[sentence.tokens.size()];
			int idx = 0;
			for (TextAnnotation tokEntry : sentence.tokens)
			{

				Pair<Integer, Integer> tokSpan = tokEntry.getOffsets();

				NIFToken nifTok = new NIFToken();
				nifTok.start = tokSpan.getLeft();
				nifTok.end = tokSpan.getRight();
				nifTok.text = tokEntry.anchor;

				nifSent.tokenArray[idx] = nifTok.text;
				nifSent.tokens.add(nifTok);
				idx++;
			}

			list.add(nifSent);
		}

		return list;
	}
}