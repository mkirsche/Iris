import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SupportingReadMap {
	HashMap<String, ArrayList<String>> map;
	HashMap<String, String> seqMap;
	
	SupportingReadMap(String filename) throws Exception
	{
		map = new HashMap<String, ArrayList<String>>();
		seqMap = new HashMap<String, String>();
		File f = new File(filename);
		if(!f.exists())
		{
			throw new Exception("Tried to create a supporting read map from a file which does not exist");
		}
		
		// Also store the variant positions for uniqueness checking later
		PosStore.init(filename);
		
		VcfEntryIterator vei = new VcfEntryIterator(filename);
		for(VcfEntry ve : vei)
		{
			boolean shouldProcess = ve.getType().equals("INS");
			
			if(Settings.PROCESS_DELETIONS && ve.getType().equals("DEL"))
			{
				shouldProcess = true;
			}
			
			if(shouldProcess)
			{			
				String key = ve.getKey();
				String supportingReadList = ve.getInfo("RNAMES");
				String[] nameTokens = supportingReadList.split(",");
				ArrayList<String> val = new ArrayList<String>();
				for(String s : nameTokens)
				{
					if(s.length() != 0)
					{
						val.add(s);
					}
				}
				
				String seq = ve.getSeq();
				if(seq.length() > 0 || !ve.getType().equals("INS"))
				{
					map.put(key, val);
					seqMap.put(key, seq);
				}
				else
				{
					Logger.log("Skipping " + key + " because there is no sequence");
				}
			}
		}
		
	}
	
	public boolean contains(String key)
	{
		return map.containsKey(key);
	}
	
	public ArrayList<String> get(String key)
	{
		return map.get(key);
	}
	
	public String[] keyArray()
	{
		String[] res = new String[map.size()];
		int idx = 0;
		for(String s : map.keySet())
		{
			res[idx++] = s;
		}
		return res;
	}
	
}
