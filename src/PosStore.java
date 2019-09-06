import java.util.HashMap;
import java.util.TreeSet;
/*
 * Data structure for storing a set of variant positions
 */
public class PosStore {
	static HashMap<String, TreeSet<Place>> positions;
	static void init(String filename) throws Exception
	{
		positions = new HashMap<String, TreeSet<Place>>();
		VcfEntryIterator vei = new VcfEntryIterator(filename);
		for(VcfEntry cur : vei)
		{
			String type = cur.getType();
			String chr = cur.getChromosome();
			long pos = cur.getPos();
			if(!positions.containsKey(type))
			{
				positions.put(type, new TreeSet<Place>());
			}
			positions.get(type).add(new Place(chr, pos));
		}
	}
	static Place getNearestVariant(String type, String chr, long pos)
	{
		if(!positions.containsKey(type))
		{
			return null;
		}
		TreeSet<Place> rightType = positions.get(type);
		Place query = new Place(chr, pos);
		Place floor = rightType.floor(query), ceiling = rightType.ceiling(query);
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
