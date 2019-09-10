import java.util.ArrayList;
import java.util.HashMap;

/*
 * Maps variant IDs to their new sequence/position
 */
public class NewSequenceMap {
	
	HashMap<String, UpdatedEntry> map;

	NewSequenceMap()
	{
		map = new HashMap<String, UpdatedEntry>();
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
		ArrayList<String> readSeqs = ReadShirring.getReads(key, names);
		Logger.log("Found " + readSeqs.size() + " relevant reads for " + key);
		ArrayList<String> consensusSequences = FalconSense.getConsensusSequences(key, readSeqs);
		Logger.log("Found " + consensusSequences.size() + " consensus sequences for " + key);
		ArrayList<String> alignmentRecords = AlignConsensus.getConsensusAlignmentRecords(key, consensusSequences, gq);
		Logger.log("Found " + alignmentRecords.size() + " alignment records for" + key);
		String type = VcfEntry.getTypeFromKey(key);
		UpdatedEntry res = null;
		if(type.equals("INS"))
		{
			res = BestInsertFinder.findBestInsert(key, alignmentRecords);
		}
		else if(type.contentEquals("DEL"))
		{
			res = BestDeletionFinder.findBestDeletion(key, alignmentRecords);
		}
		if(res != null && VcfEntry.getPosFromKey(key) > Settings.GENOME_REGION_BUFFER)
		{
			res.pos += VcfEntry.getPosFromKey(key) - Settings.GENOME_REGION_BUFFER;
		}
		
		if(res != null)
		{
			String originalChr = VcfEntry.getChrFromKey(key);
			long originalPos = VcfEntry.getPosFromKey(key);
			PosStore.Place originalPlace = new PosStore.Place(originalChr, originalPos);
			
			PosStore.Place closestToRefined = PosStore.getNearestVariant(type, originalChr, res.pos);
			
			if(originalPlace.compareTo(closestToRefined) != 0)
			{
				Logger.log("Did not change position of " + key + " to " + res.pos + " because too close to other variant");
				return null;
			}
			
			long oldLength = PosStore.getLength(type, closestToRefined.chr, closestToRefined.pos);
			long newLength = res.seq.length();
			
			if(newLength > oldLength * (1 + Settings.MAX_LENGTH_CHANGE))
			{
				Logger.log("Did not change " + key + " because new sequence too long");
				return null;
			}
			
			if(newLength < oldLength * (1 - Settings.MAX_LENGTH_CHANGE))
			{
				Logger.log("Did not change " + key + " because new sequence too short");
				return null;
			}
		}
		
		return res;
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
