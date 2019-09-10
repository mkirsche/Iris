import java.util.HashMap;
import java.util.TreeMap;
/*
 * Data structure for storing a set of variant positions
 */
public class PosStore {
	static HashMap<String, TreeMap<Place, Long>> positions;
	static void init(String filename) throws Exception
	{
		positions = new HashMap<String, TreeMap<Place, Long>>();
		VcfEntryIterator vei = new VcfEntryIterator(filename);
		for(VcfEntry cur : vei)
		{
			String type = cur.getType();
			String chr = cur.getChromosome();
			long len = cur.getLength();
			long pos = cur.getPos();
			if(!positions.containsKey(type))
			{
				positions.put(type, new TreeMap<Place, Long>());
			}
			positions.get(type).put(new Place(chr, pos), len);
		}
	}
	static long getLength(String type, String chr, long pos)
	{
		return positions.get(type).get(new Place(chr, pos));
	}
	static Place getNearestVariant(String type, String chr, long pos)
	{
		if(!positions.containsKey(type))
		{
			return null;
		}
		TreeMap<Place, Long> rightType = positions.get(type);
		Place query = new Place(chr, pos);
		Place floor = rightType.floorKey(query), ceiling = rightType.ceilingKey(query);
		if(floor != null && !floor.chr.equals(chr))
		{
			floor = null;
		}
		if(ceiling != null && !ceiling.chr.equals(chr))
		{
			ceiling = null;
		}
		if(floor == null && ceiling == null)
		{
			return null;
		}
		else if(floor == null)
		{
			return ceiling;
		}
		else if(ceiling == null)
		{
			return floor;
		}
		else
		{
			long floorDist = Math.abs(pos - floor.pos);
			long ceilingDist = Math.abs(pos - ceiling.pos);
			if(floorDist <= ceilingDist)
			{
				return floor;
			}
			else
			{
				return ceiling;
			}
		}
	}
	static class Place implements Comparable<Place>
	{
		String chr;
		long pos;
		Place(String chr, long pos)
		{
			this.chr = chr;
			this.pos = pos;
		}
		public int compareTo(Place o)
		{
			if(!chr.equals(o.chr))
			{
				return chr.compareTo(o.chr);
			}
			return Long.compare(pos, o.pos);
		}
	}
}
