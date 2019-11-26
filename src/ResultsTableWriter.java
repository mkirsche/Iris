/*
 * A class for handling the creation of a results table
 */

import java.io.File;
import java.io.PrintWriter;

public class ResultsTableWriter {
	
	PrintWriter out;
		
	ResultsTableWriter(String fileName) throws Exception
	{
		out = new PrintWriter(new File(fileName));
		out.println(TableEntry.makeHeader());
	}
	
	void printEntry(IrisVcfEntry oldEntry, GenomeQuery gq, NewSequenceMap.UpdatedEntry newEntry) throws Exception
	{
		out.println(new TableEntry(oldEntry, gq, newEntry));
	}
	
	void close()
	{
		out.close();
	}
	
	static int editDistance(String from, String to)
	{
		int n = from.length();
		int m = to.length();
		int[][] dp = new int[2][m+1];
		for(int i = 1; i<=m; i++)
		{
			dp[0][i] = i;
		}
		for(int i = 1; i <= n; i++)
		{
			for(int j = 1; j <= m; j++)
			{
				boolean sameChar = from.charAt(i-1) == to.charAt(j-1);
				int min = (sameChar ? 0 : 1) + dp[(i-1)%2][j-1];
				min = Math.min(min, 1 + dp[(i-1)%2][j]);
				min = Math.min(min, 1 + dp[i%2][j-1]);
				dp[i%2][j] = min;
			}
		}
		return dp[n%2][m];
	}

	static class TableEntry
	{
		String chromosome;
		long originalPosition;
		String originalSequence;
		int originalLength;
		String type;
		boolean refined;
		long newPosition;
		String newSequence;
		int newLength;
		int editDistance;
		
		TableEntry(IrisVcfEntry oldEntry, GenomeQuery gq, NewSequenceMap.UpdatedEntry newEntry) throws Exception
		{
			chromosome = oldEntry.getChromosome();
			originalPosition = oldEntry.getPos();
			originalSequence = oldEntry.getSeq();
			type = oldEntry.getType();
			// Fix cases with the old format where deletions 
			if((originalSequence.equals("N") || originalSequence.equals("X")) 
					&& oldEntry.hasInfoField("SVLEN"))
			{
                int oldLength = (int)(.5 + Double.parseDouble(oldEntry.getInfo("SVLEN")));
                                originalSequence = gq.genomeSubstring(chromosome, originalPosition + 1,
                                                                originalPosition + oldLength);
			}
			originalLength = originalSequence.length();
			newPosition = originalPosition;
			newSequence = originalSequence;
			newLength = originalLength;
			refined = newEntry != null;
			if(refined)
			{
				newPosition = newEntry.pos;
				newSequence = newEntry.seq;
				newLength = newSequence.length();
				if(type.equals("DEL"))
				{
					newSequence = gq.genomeSubstring(chromosome, newPosition + 1, 
							newPosition + newLength);
				}
				editDistance = editDistance(originalSequence, newSequence);
			}
		}
		
		public String toString()
		{
			return chromosome + "\t" 
					+ originalPosition + "\t"
					+ originalSequence + "\t"
					+ originalLength + "\t"
					+ type + "\t"
					+ refined + "\t"
					+ (refined ? newPosition : originalPosition) + "\t"
					+ (refined ? newSequence : originalSequence) + "\t"
					+ (refined ? newLength : originalLength) + "\t"
					+ (refined ? editDistance : 0);
		}
		
		static String makeHeader()
		{
			return "Chromosome\t"
					+ "OriginalPosition\t"
					+ "OriginalSequence\t"
					+ "OriginalLength\t"
					+ "SVType\t"
					+ "Refined\t"
					+ "NewPosition\t"
					+ "NewSequence\t"
					+ "NewLength\t"
					+ "EditDistance";
		}
	}
}
