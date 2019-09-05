import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

/*
 * Used for assessing the accuracy simulated results - assumes SURVIVOR was used to produce 
 * the mutated genome and that IRIS was used to refine SV calls from Sniffles
 */
public class EvaluateSimulatedAccuracy {
	static String groundTruthFilename = "";
	static String irisCallsFilename = "";
	static void usage()
	{
		System.out.println("Usage: java EvaluateSimulatedAccuracy [args]");
		System.out.println("  Example: java EvaluateSimulatedAccuracy ground_truth=bigsim.insertions.fa iris_calls=out.vcf");
		System.out.println();
		System.out.println("Required args:");
		System.out.println("  ground_truth (String) - the FASTA file containing the variants added during the simulation");
		System.out.println("  iris_calls    (String) - the VCF file with variant calls refined by IRIS");
		System.out.println();
	}
	static TreeMap<Place, String> readGroundTruth(String filename) throws Exception
	{
		TreeMap<Place, String> res = new TreeMap<Place, String>();
		Scanner input = new Scanner(new FileInputStream(new File(filename)));
		while(input.hasNext())
		{
			String name = input.nextLine().substring(1);
			String seq = input.nextLine();
			String[] split = name.split("_");
			String chromosome = split[0];
			long pos = Long.parseLong(split[1]);
			res.put(new Place(chromosome, pos), seq);
		}
		input.close();
		return res;
	}
	static boolean parseArgs(String[] args)
	{
		for(int i = 0; i<args.length; i++)
		{
			if(args[i].indexOf('=') == -1)
			{
				continue;
			}
			int equalIdx = args[i].indexOf('=');
			String key = args[i].substring(0, equalIdx);
			while(key.length() > 0 && key.charAt(0) == '-')
			{
				key = key.substring(1);
			}
			String val = args[i].substring(1 + equalIdx);
			if(key.equals("ground_truth"))
			{
				groundTruthFilename = val;
			}
			else if(key.equals("iris_calls"))
			{
				irisCallsFilename = val;
			}
		}
		if(groundTruthFilename.length() == 0 || irisCallsFilename.length() == 0)
		{
			return false;
		}
		return true;
	}
	static Place getNearestVariant(Place curPlace, TreeMap<Place, String> truth)
	{
		if(truth.containsKey(curPlace))
		{
			return curPlace;
		}
		Place lower = truth.lowerKey(curPlace);
		Place higher = truth.higherKey(curPlace);
		if(lower == null && higher == null)
		{
			return null;
		}
		else if(lower == null)
		{
			return higher;
		}
		else if(higher == null)
		{
			return lower;
		}
		if(!lower.chr.equals(curPlace.chr) && !higher.chr.equals(curPlace.chr))
		{
			return null;
		}
		else if(!lower.chr.equals(curPlace.chr))
		{
			return higher;
		}
		else if(!higher.chr.equals(curPlace.chr))
		{
			return lower;
		}
		long lowDist = curPlace.pos - lower.pos;
		long highDist = higher.pos - curPlace.pos;
		if(lowDist <= highDist)
		{
			return lower;
		}
		else
		{
			return higher;
		}
	}
	public static void main(String[] args) throws Exception
	{
		if(!parseArgs(args))
		{
			usage();
			return;
		}
		TreeMap<Place, String> truth = readGroundTruth(groundTruthFilename);
		
		VcfEntryIterator vei = new VcfEntryIterator(irisCallsFilename);
		
		int falsePositives = 0;
		
		ArrayList<Long> distances = new ArrayList<Long>();
		ArrayList<Long> editDistances = new ArrayList<Long>();
		
		for(VcfEntry cur : vei)
		{
			if(!cur.getType().equals("INS"))
			{
				continue;
			}
			
			Place curPlace = new Place(cur.getChromosome(), cur.getPos());
			String curSeq = cur.getSeq();
			Place truthKey = getNearestVariant(curPlace, truth);
			if(truthKey == null)
			{
				falsePositives++;
			}
			else
			{
				String trueSeq = truth.get(truthKey);
				truth.remove(truthKey);
				
				long dist = Math.abs(curPlace.pos - truthKey.pos);
				int editDistance = ResultsTableWriter.editDistance(trueSeq, curSeq);
				distances.add(dist);
				editDistances.add((long)editDistance);
			}
			
			
		}
		
		int falseNegatives = truth.size();
		
		System.out.println("False positives: " + falsePositives);
		System.out.println("False negatives: " + falseNegatives);
		System.out.println("Matches: " + distances.size());
		System.out.println("Average genomic distance: " + average(distances));
		System.out.println("Average sequence edit distance: " + average(editDistances));
		
	}
	static double average(ArrayList<Long> list)
	{
		double res = 0;
		for(long x : list) res += x;
		return list.size() == 0 ? 0.0 : res / list.size();
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
