import java.util.ArrayList;

public class BestInsertFinder {
	
/*
 * Finds the best insertion given a variant ID and list of SAM alignment records of the assembled insertion sequence
 * back to a portion of the reference genome
 */
public static  NewSequenceMap.UpdatedEntry findBestInsert(String id, ArrayList<String> alignmentRecords)
{
	return findBestInsertFromOffset(alignmentRecords, getExpectedOffset(id));
}
	
/*
 * Find the best insertion from a list of SAM records and
 * how far into the reference we an insertion is expected
 */
public static NewSequenceMap.UpdatedEntry findBestInsertFromOffset(ArrayList<String> alignmentRecords, long offset)
{
	double bestScore = 0.0;
	NewSequenceMap.UpdatedEntry bestEntry = null;
	
	// Iterate over the alignments and decide which one is the best match
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
			
			double currentScore = scoreInsertion(length, distance);
			
			if(bestEntry == null || currentScore > bestScore)
			{
				bestEntry = candidate;
			}
		}
	}
	
	return bestEntry;
}

static long getExpectedOffset(String id)
{
	long pos = VcfEntry.getPosFromKey(id);
	return Math.min(pos, Settings.GENOME_REGION_BUFFER); // TODO check for indexing errors here
}

/*
 * Scoring function for an insertion 
 */
static double scoreInsertion(long length, long distance)
{
	return 20 * length - distance * distance;
}

/*
 * Get all insertions (sequence/position) from a SAM-formatted alignment record
 */
static ArrayList<NewSequenceMap.UpdatedEntry> getAllInsertions(String record)
{
	String[] samFields = record.split("\t");
	char[] cigarChars = samFields[5].toCharArray();
	String queryString = samFields[9];
	
	int refPos = Integer.parseInt(samFields[3])-1;
	int queryPos = 0;
	int segmentLength = 0;
	ArrayList<NewSequenceMap.UpdatedEntry> insertions = new ArrayList<>();
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
