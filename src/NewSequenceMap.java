import java.util.ArrayList;
import java.util.HashMap;

/*
 * Maps variant IDs to their new sequence/position
 */
public class NewSequenceMap {
	
	HashMap<String, UpdatedEntry> map;

	NewSequenceMap()
	{
		map = new HashMap<>();
	}
	
	void add(String key, String seq, long pos)
	{
		map.put(key, new UpdatedEntry(seq, pos));
	}
	
	public boolean containsKey(String key)
	{
		return map.containsKey(key);
	}
	
	public String getSeq(String key)
	{
		if(!map.containsKey(key))
		{
			return null;
		}
		return map.get(key).seq;
	}
	
	public Long getPos(String key)
	{
		if(!map.containsKey(key))
		{
			return null;
		}
		return map.get(key).pos;
	}
	
	static UpdatedEntry fromReadNames(String key, ArrayList<String> names, GenomeQuery gq) throws Exception
	{
		String seq = "";
		int pos = -1;
		
		ArrayList<String> readSeqs = ReadShirring.getReads(key, names);
		ArrayList<String> consensusSequences = FalconSense.getConsensusSequences(key, readSeqs);
		ArrayList<String> alignmentRecords = AlignConsensus.getConsensusAlignmentRecords(key, consensusSequences, gq);
		return new UpdatedEntry(seq, pos);
	}
	
	static class UpdatedEntry
	{
		String seq;
		long pos;
		UpdatedEntry(String ss, long pp)
		{
			seq = ss;
			pos = pp;
		}
	}
	
}
