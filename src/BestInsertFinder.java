import java.util.ArrayList;
import java.util.HashMap;

public class BestInsertFinder {
	
/*
 * Finds the best insertion given a variant ID and list of SAM alignment records of the assembled insertion sequence
 * back to a portion of the reference genome
 */
public static  NewSequenceMap.UpdatedEntry findBestInsert(String id, ArrayList<String> alignmentRecords) throws Exception
{
	return findBestInsertFromOffset(alignmentRecords, getExpectedOffset(id));
}
	
/*
 * Find the best insertion from a list of SAM records and
 * how far into the reference an insertion is expected
 */
public static NewSequenceMap.UpdatedEntry findBestInsertFromOffset(ArrayList<String> alignmentRecords, long offset) throws Exception
{
	// Bookkeeping information to maintain the best insertion
	double bestScore = 0.0;
	NewSequenceMap.UpdatedEntry bestEntry = null;
	
	// In case the exact same insertion occurs in multiple reads, keep that info to increase scores of repeated sequences
	HashMap<String, Integer> insertSupport = new HashMap<String, Integer>();
	
	// Get expected length
	HashMap<Integer, Double> lengthScores = getLengthScores(alignmentRecords, offset);
	
	// Iterate over the alignments and decide which one is the best match
	for(String record : alignmentRecords)
	{
		ArrayList<NewSequenceMap.UpdatedEntry> candidates = getAllInsertions(record);
		
		// Iterate over alignments for this read
		for(NewSequenceMap.UpdatedEntry candidate : candidates)
		{
			long distance = Math.abs(candidate.pos - offset);
			int length = candidate.seq.length();
			
			// Throw out short and far away insertions
			if(distance > Settings.INSERTION_MAX_DIST || length < Settings.INSERTION_MIN_LENGTH)
			{
				continue;
			}
			
			// Update count for this insertion
			String candidateKey = candidate.seq+ "#" + candidate.pos;
			if(insertSupport.containsKey(candidateKey))
			{
				insertSupport.put(candidateKey, 1 + insertSupport.get(candidateKey));
			}
			else
			{
				insertSupport.put(candidateKey, 1);
			}
			
			// Compute score and see if this is a new best
			double currentScore = scoreInsertion(length, lengthScores.get(length), distance, insertSupport.get(candidateKey));		
			if(bestEntry == null || currentScore > bestScore)
			{
				bestScore = currentScore;
				bestEntry = candidate;
			}
		}
	}
	
	return bestEntry;
}

/*
 * Computes how often insertions of different lengths occur in the dataset and scores each length
 * If f(x) is the number of insertions with length x, then 
 * score(x) = f(x) + sum(i = 1 to 10)[(f(x-i) + f(x+i)) / (i+1)]
 * Note that insertions which are too short or far away are ignored in these counts
 */
static HashMap<Integer, Double> getLengthScores(ArrayList<String> alignmentRecords, long offset) throws Exception
{
	HashMap<Integer, Integer> lengthFreq = new HashMap<Integer, Integer>();
	for(String record : alignmentRecords)
	{
		ArrayList<NewSequenceMap.UpdatedEntry> candidates = getAllInsertions(record);
		for(NewSequenceMap.UpdatedEntry candidate : candidates)
		{
			long distance = Math.abs(candidate.pos - offset);
			int length = candidate.seq.length();
			
			if(distance > Settings.INSERTION_MAX_DIST || length < Settings.INSERTION_MIN_LENGTH)
			{
				continue;
			}
			
			lengthFreq.put(length, lengthFreq.containsKey(length) ? (1 + lengthFreq.get(length)) : 1);
		}
	}
	
	HashMap<Integer, Double> lengthScore = new HashMap<Integer, Double>();
	for(int x : lengthFreq.keySet())
	{
		double curScore = 0.0;
		for(int i = x - 10; i <= x + 10; i++)
		{
			if(lengthFreq.containsKey(i))
			{
				curScore += lengthFreq.get(x) + (1 + Math.abs(i - x));
			}
		}
		lengthScore.put(x, curScore);
	}
	return lengthScore;
}

/*
 * Gets the position in the genome region we expect the SV to occur at based on its position in the whole genome
 */
static long getExpectedOffset(String id)
{
	long pos = VcfEntry.getPosFromKey(id);
	return Math.min(pos, Settings.GENOME_REGION_BUFFER); // TODO check for indexing errors here
}

/*
 * Scoring function for an insertion 
 */
static double scoreInsertion(long length, double lengthScore, long distance, int support)
{
	return 20 * (length + lengthScore) * support - distance * distance;
}

/*
 * Get all insertions (sequence/position) from a SAM-formatted alignment record
 */
static ArrayList<NewSequenceMap.UpdatedEntry> getAllInsertions(String record) throws Exception
{
	String[] samFields = record.split("\t");
	char[] cigarChars = samFields[5].toCharArray();
	String queryString = samFields[9];
	
	int refPos = Integer.parseInt(samFields[3])-1;
	int queryPos = 0;
	int segmentLength = 0;
	ArrayList<NewSequenceMap.UpdatedEntry> insertions = new ArrayList<NewSequenceMap.UpdatedEntry>();
	
	try {
	for(char c : cigarChars)
	{
		if(c >= '0' && c <= '9')
		{
			segmentLength = segmentLength * 10 + (c - '0');
		}
		else
		{
			if(c == 'I')
			{
				// Found an insertion - add it to the list!
				String insertionSequence = queryString.substring(queryPos, queryPos + segmentLength);
				insertions.add(new NewSequenceMap.UpdatedEntry(insertionSequence, refPos));
			}
			
			// Move the ref/query positions based on the alignment type
			if(advancesQuery(c))
			{
				queryPos += segmentLength;
			}
			if(advancesReference(c))
			{
				refPos += segmentLength;
			}
			
			segmentLength = 0;
		}		
	}
	} catch(Exception e) {
		throw new Exception("Error processing CIGAR string " + new String(cigarChars));
	}
	return insertions;
}
/*
 * Whether or not a given CIGAR character advances the position in the reference
 */
static boolean advancesReference(char c)
{
	return c == 'M' || c == 'D' || c == 'N' || c == '=' || c == 'X';
}
/*
 * Whether or not a given CIGAR character advances the position in the query
 */
static boolean advancesQuery(char c)
{
	return c == 'M' || c == 'I' || c == 'S' || c == '=' || c == 'X';
}
}
