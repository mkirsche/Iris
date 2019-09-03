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
	
	void printEntry(VcfEntry oldEntry, NewSequenceMap.UpdatedEntry newEntry) throws Exception
	{
		out.println(new TableEntry(oldEntry, newEntry));
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
		
		TableEntry(VcfEntry oldEntry, NewSequenceMap.UpdatedEntry newEntry) throws Exception
		{
			chromosome = oldEntry.getChromosome();
			originalPosition = oldEntry.getPos();
			originalSequence = oldEntry.getSeq();
			originalLength = originalSequence.length();
			newPosition = originalPosition;
			newSequence = originalSequence;
			newLength = originalLength;
			type = oldEntry.getType();
			refined = newEntry != null;
			if(refined)
			{
				newPosition = newEntry.pos;
				newSequence = newEntry.seq;
				newLength = newSequence.length();
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
