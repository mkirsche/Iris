import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BestInsertFinder {
	
/*
 * Finds the best insertion given a variant ID and list of SAM alignment records of the assembled insertion sequence
 * back to a portion of the reference genome
 */
public static NewSequenceMap.UpdatedEntry findBestInsert(String id, ArrayList<String> alignmentRecords) throws Exception
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
		ArrayList<NewSequenceMap.UpdatedEntry> candidates = getSVsByType(record, 'I');
		
		// Iterate over alignments for this read
		for(NewSequenceMap.UpdatedEntry candidate : candidates)
		{
			long distance = Math.abs(candidate.pos - offset);
			int length = candidate.seq.length();
			
			// Throw out short and far away insertions
			if(distance > IrisSettings.INSERTION_MAX_DIST || length < IrisSettings.INSERTION_MIN_LENGTH)
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
		ArrayList<NewSequenceMap.UpdatedEntry> candidates = getSVsByType(record, 'I');
		for(NewSequenceMap.UpdatedEntry candidate : candidates)
		{
			long distance = Math.abs(candidate.pos - offset);
			int length = candidate.seq.length();
			
			if(distance > IrisSettings.INSERTION_MAX_DIST || length < IrisSettings.INSERTION_MIN_LENGTH)
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
	long pos = IrisVcfEntry.getPosFromKey(id);
	return Math.min(pos, IrisSettings.GENOME_REGION_BUFFER); // TODO check for indexing errors here
}

/*
 * Scoring function for an insertion 
 */
static double scoreInsertion(long length, double lengthScore, long distance, int support)
{
	return 20 * (length + lengthScore) * support - distance * distance;
}

static String makeFiller(int length)
{
	char[] res = new char[length];
	Arrays.fill(res, 'A');
	return new String(res);
}

/*
 * Get all insertions (sequence/position) from a SAM-formatted alignment record
 */
static ArrayList<NewSequenceMap.UpdatedEntry> getSVsByType(String record, char type) throws Exception
{
	String[] samFields = record.split("\t");
	char[] cigarChars = samFields[5].toCharArray();
	String queryString = samFields[9];
	
	ArrayList<NewSequenceMap.UpdatedEntry> res = new ArrayList<NewSequenceMap.UpdatedEntry>();
	
	if(queryString.equals("*"))
	{
		return res;
	}
	
	int refPos = Integer.parseInt(samFields[3]);
	int queryPos = 0;
	int segmentLength = 0;
	
	try {
		for(char c : cigarChars)
		{
			if(c >= '0' && c <= '9')
			{
				segmentLength = segmentLength * 10 + (c - '0');
			}
			else
			{
				if(c == type)
				{
					if(c == 'I')
					{
						// Found an insertion - add it to the list!
						String insertionSequence = queryString.substring(queryPos, queryPos + segmentLength);
						res.add(new NewSequenceMap.UpdatedEntry(insertionSequence, refPos));
					}
					else if(c == 'D')
					{
						char[] filler = new char[segmentLength];
						Arrays.fill(filler, 'A');
						res.add(new NewSequenceMap.UpdatedEntry(new String(filler), refPos));
					}
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
		throw new Exception("Error processing CIGAR string in " + record);
	}
	
	return res;
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
