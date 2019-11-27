import java.util.Arrays;

/*
 * Methods for handling VCF v4.2 entries for structural variants
 */
public class IrisVcfEntry implements Comparable<IrisVcfEntry> {

	String originalLine;
	String[] tabTokens;
	
	public IrisVcfEntry(String line) throws Exception
	{
		originalLine = line;
		tabTokens = line.split("\t");
		if(tabTokens.length < 8)
		{
			throw new Exception("VCF line had too few entries: "
					+ Arrays.toString(tabTokens));
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder("");
		for(int i = 0; i<tabTokens.length; i++)
		{
			sb.append(tabTokens[i]);
			if(i < tabTokens.length - 1)
			{
				sb.append("\t");
			}
		}
		return sb.toString();
	}
	
	public String getChromosome()
	{
		return tabTokens[0];
	}
	
	public void setChromosome(String s)
	{
		tabTokens[0] = s;
	}
	
	public long getPos() throws Exception
	{
		try {
			return Long.parseLong(tabTokens[1]);
		} catch(Exception e) {
			throw new Exception("Tried to access invalid VCF position: " + tabTokens[1]);
		}
	}
	
	public void setPos(long val)
	{
		tabTokens[1] = val+"";
	}
	
	public String getId()
	{
		return tabTokens[2];
	}
	
	public String getRef()
	{
		return tabTokens[3];
	}
	
	public void setRef(String s)
	{
		tabTokens[3] = s;
	}
	
	public String getAlt()
	{
		return tabTokens[4];
	}
	
	public void setAlt(String s)
	{
		tabTokens[4] = s;
	}
	
	public int getLength() throws Exception
	{
		String s = getInfo("SVLEN");
		try {
			return (int)(.5 + Double.parseDouble(s));
		} catch(Exception e) {
            try {
                String seq = getSeq();
                String type = getType();
                if(type.equals("INS")) return seq.length();
                else return -seq.length();
            } catch(Exception f) {
			    throw new Exception("SVLEN field is not an integer: " + s);
            }
		}
	}
	
	/*
	 * Set a particular VCF INFO field, adding the field if it doesn't already exist
	 */
	public void setInfo(String field, String val) throws Exception
	{
		String[] infoFields = tabTokens[7].split(";");
		for(String semitoken : infoFields)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(field))
			{
				String updatedToken = key + "=" + val;
				
				// Special case if this is the first INFO field
				if(tabTokens[7].startsWith(semitoken))
				{
					tabTokens[7] = tabTokens[7].replaceFirst(semitoken, updatedToken);
				}
				else
				{
					tabTokens[7] = tabTokens[7].replaceAll(";" + semitoken, ";" + updatedToken);
				}
				return;
			}
		}
		
		// Field not found, so add it!
		tabTokens[7] += ";" + field + "=" + val;
	}
	
	public void setLength(int len) throws Exception
	{
		setInfo("SVLEN", len+"");
	}
	
	public String getType() throws Exception
	{
		String res = getInfo("SVTYPE");
		if(res.length() == 0)
		{
			int refLength = getRef().length();
			int altLength = getAlt().length();
			if(refLength > altLength)
			{
				return "INS";
			}
			else if(refLength < altLength)
			{
				return "DEL";
			}
			else
			{
				return "";
			}
		}
		else return res;
	}
	
	public void setType(String s) throws Exception
	{
		setInfo("SVTYPE", s);
	}
	
	public String getSeq() throws Exception
	{
		if(hasInfoField("SEQ"))
		{
			return getInfo("SEQ");
		}
		String ref = getRef(), alt = getAlt();
		if(alt.startsWith("<"))
		{
			return "";
		}
		String type = getType();
		
		// If the SV is a deletion, swap REF and ALT and treat as an insertion
		if(type.equals("DEL"))
		{
			String tmp = ref;
			ref = alt;
			alt = tmp;
			type = "INS";
		}
		if(ref.equals("X") || ref.equals("N"))
		{
			return alt;
		}
		else if(alt.equals("X") || ref.equals("N"))
		{
			return ref;
		}
		else if(type.equals("INS"))
		{
			int startPad = 0, endPad = 0;
			int totalPad = ref.length();
			while(startPad + endPad < totalPad && startPad < alt.length())
			{
				if(ref.charAt(startPad) == alt.charAt(startPad))
				{
					startPad++;
				}
				else if(ref.charAt(ref.length() - 1 - endPad) == alt.charAt(alt.length() - 1 - endPad))
				{
					endPad++;
				}
				else
				{
					break;
				}
			}
			if(startPad + endPad == totalPad)
			{
				return alt.substring(startPad, alt.length() - endPad);
			}
			else
			{
				return alt;
			}
		}
		else
		{
			return alt;
		}
	}
	
	public String getInfo(String field) throws Exception
	{
		String infoToken = tabTokens[7];
		String[] semicolonSplit = infoToken.split(";");
		for(String semitoken : semicolonSplit)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(field))
			{
				return semitoken.substring(1 + equalIndex);
			}
		}
		return "";
	}
	
	public boolean hasInfoField(String fieldName)
	{
		String infoToken = tabTokens[7];
		String[] semicolonSplit = infoToken.split(";");
		for(String semitoken : semicolonSplit)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(fieldName))
			{
				return true;
			}
		}
		return false;
	}
	
	public void updateInsertionSequence(String newSeq) throws Exception
	{
		setRef("");
		if(newSeq.length() < IrisSettings.MAX_OUTPUT_LENGTH) setAlt(newSeq);
		else setAlt("");
		setLength(newSeq.length());
		if(hasInfoField("SEQ") && newSeq.length() < IrisSettings.MAX_OUTPUT_LENGTH)
		{
			setInfo("SEQ", newSeq);
		}
	}
	
	public void updateDeletion(String newSeq) throws Exception
	{
		if(newSeq.length() < IrisSettings.MAX_OUTPUT_LENGTH) setRef(newSeq);
		else setRef("");
		setAlt("");
		setLength(-newSeq.length());
		if(hasInfoField("SEQ") && newSeq.length() < IrisSettings.MAX_OUTPUT_LENGTH)
		{
			setInfo("SEQ", newSeq);
		}
	}
	
	public String getKey() throws Exception
	{
		return getChromosome() + ":" + getPos() + ":" + getType() + ":" + getId();
	}
	
	static String getChrFromKey(String key)
	{
		String[] tokens = key.split(":");
		return tokens[0];
	}
	
	static long getPosFromKey(String key)
	{
		String[] tokens = key.split(":");
		return Long.parseLong(tokens[1]);
	}
	
	static String getTypeFromKey(String key)
	{
		String[] tokens = key.split(":");
		return tokens[2];
	}
	
	@Override
	public int compareTo(IrisVcfEntry o) {
		for(int i = 0; i<tabTokens.length && i < o.tabTokens.length; i++)
		{
			if(!tabTokens[i].equals(o.tabTokens[i]))
			{
				return tabTokens[i].compareTo(o.tabTokens[i]);
			}
		}
		return tabTokens.length - o.tabTokens.length;
	}

}
