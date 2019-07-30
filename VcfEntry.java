import java.util.Arrays;

/*
 * Methods for handling VCF v4.2 entries for structural variants
 */
public class VcfEntry implements Comparable<VcfEntry> {

	String originalLine;
	String[] tabTokens;
	
	public VcfEntry(String line) throws Exception
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
			return Integer.parseInt(s);
		} catch(Exception e) {
			throw new Exception("SVLEN field is not an integer: " + s);
		}
	}
	
	public void setLength(int len) throws Exception
	{
		setInfo("SVLEN", len+"");
	}
	
	public String getType() throws Exception
	{
		return getInfo("SVTYPE");
	}
	
	public void setType(String s) throws Exception
	{
		setInfo("SVTYPE", s);
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
		throw new Exception("Trying to access VCF INFO field which is not found: " + field);
	}
	
	public void updateInsertionSequence(String newSeq) throws Exception
	{
		setAlt(newSeq);
		setLength(newSeq.length());
	}
	
	public void setInfo(String field, String val) throws Exception
	{
		String oldVal = getInfo(field);
		String toReplace = field + '=' + oldVal;
		String replacement = field + '=' + val;
		tabTokens[7].replace(toReplace, replacement);
	}
	
	public String getKey() throws Exception
	{
		return getChromosome() + ":" + getPos() + ":" + getId();
	}
	
	@Override
	public int compareTo(VcfEntry o) {
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
