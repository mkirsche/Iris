import java.util.ArrayList;
import java.util.Arrays;

public class BestDeletionFinder {
	
/*
 * Finds the best deletion given a variant ID and list of SAM alignment records of the assembled sequence
 * back to a portion of the reference genome
 */
public static  NewSequenceMap.UpdatedEntry findBestDeletion(String id, ArrayList<String> alignmentRecords) throws Exception
{
	return findBestDeletionFromOffset(alignmentRecords, BestInsertFinder.getExpectedOffset(id));
}
	
/*
 * Find the best deletion from a list of SAM records and
 * how far into the reference a deletion is expected
 */
public static NewSequenceMap.UpdatedEntry findBestDeletionFromOffset(ArrayList<String> alignmentRecords, long offset) throws Exception
{
	ArrayList<DeletionRecord> allDeletions = new ArrayList<DeletionRecord>();
	
	// Iterate over the alignments and decide which one is the best match
	for(String record : alignmentRecords)
	{
		ArrayList<NewSequenceMap.UpdatedEntry> candidates = BestInsertFinder.getSVsByType(record, 'D');
		
		// Iterate over alignments for this read
		for(NewSequenceMap.UpdatedEntry candidate : candidates)
		{
			long distance = Math.abs(candidate.pos - offset);
			int length = candidate.seq.length();
			
			// Throw out short and far away deletions
			if(distance > IrisSettings.INSERTION_MAX_DIST || length < IrisSettings.INSERTION_MIN_LENGTH)
			{
				continue;
			}
			
			allDeletions.add(new DeletionRecord(candidate.pos, candidate.seq.length()));
		}
	}
	
	if(allDeletions.size() == 0)
	{
		return null;
	}
	
	DeletionRecord res = getConsensus(allDeletions);
	
	char[] filler = new char[res.length];
	Arrays.fill(filler, 'A');
	String seq = new String(filler);
	
	return new NewSequenceMap.UpdatedEntry(seq, res.pos);
}

static DeletionRecord getConsensus(ArrayList<DeletionRecord> records)
{
	int n = records.size();
	int[] scores = new int[n];
	for(int i = 0; i<n; i++)
		for(int j = 0; j<n; j++)
		{
			if(records.get(i).pos == records.get(j).pos)
			{
				scores[j]++;
			}
			if(records.get(i).length == records.get(j).length)
			{
				scores[j]++;
			}
		}
	int maxScore = 0;
	for(int score : scores) maxScore = Math.max(maxScore, score);
	
	ArrayList<Long> bestPos = new ArrayList<Long>();
	ArrayList<Integer> bestLength = new ArrayList<Integer>();
	
	for(int i = 0; i<n; i++)
	{
		if(scores[i] == maxScore)
		{
			bestPos.add(records.get(i).pos);
			bestLength.add(records.get(i).length);
		}
	}
	
	return new DeletionRecord(bestPos.get((bestPos.size() - 1) / 2), bestLength.get((bestLength.size() - 1) / 2));
}

static class DeletionRecord implements Comparable<DeletionRecord>
{
	long pos;
	int length;
	DeletionRecord(long pos, int length)
	{
		this.pos = pos;
		this.length = length;
	}
	public int compareTo(DeletionRecord o)
	{
		if(pos != o.pos)
		{
			return Long.compare(pos, o.pos);
		}
		return length - o.length;
	}
}
}
