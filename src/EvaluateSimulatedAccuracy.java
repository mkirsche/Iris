import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
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
	static int DISTANCE_THRESHOLD = 1000;
	static double ID_THRESHOLD = 0.5;
	static String OUTPUT_FILE = "scores.txt";
	static String GENOME_FILE = "";
	static IrisGenomeQuery gq;
	static boolean PRINT_EXAMPLES = false;
	static void usage()
	{
		System.out.println("Usage: java EvaluateSimulatedAccuracy [args]");
		System.out.println("  Example: java EvaluateSimulatedAccuracy ground_truth=bigsim.insertions.fa iris_calls=out.vcf");
		System.out.println();
		System.out.println("Required args:");
		System.out.println("  ground_truth   (String) - the FASTA file containing the variants added during the simulation");
		System.out.println("  iris_calls     (String) - the VCF file with variant calls refined by IRIS");
		System.out.println("Optional args:");
		System.out.println("  min_similarity (float)  - min sequence identity needed for a match to count");
		System.out.println("  max_distance   (int)    - max distance allowed for a match to count");
		System.out.println("  use_genome     (String) - genome file for doing fancier sequence matching");
		System.out.println("  scores_file    (String) - file to write accuracy scores to");
		System.out.println();
	}
	static TreeMap<PosStore.Place, String> readGroundTruthFromFasta(String filename) throws Exception
	{
		TreeMap<PosStore.Place, String> res = new TreeMap<PosStore.Place, String>();
		Scanner input = new Scanner(new FileInputStream(new File(filename)));
		while(input.hasNext())
		{
			String name = input.nextLine().substring(1);
			String seq = input.nextLine();
			String[] split = name.split("_");
			String chromosome = split[0];
			long pos = Long.parseLong(split[1]) + 1;
			res.put(new PosStore.Place(chromosome, pos), seq);
		}
		input.close();
		return res;
	}
	static TreeMap<PosStore.Place, String> readGroundTruthFromVcf(String filename) throws Exception
	{
		VcfEntryIterator vei = new VcfEntryIterator(filename);
		TreeMap<PosStore.Place, String> res = new TreeMap<PosStore.Place, String>();
		
		for(IrisVcfEntry cur : vei)
		{
			if(!cur.getType().equals("INS"))
			{
				continue;
			}
			
			PosStore.Place curPlace = new PosStore.Place(cur.getChromosome(), cur.getPos());
			String curSeq = cur.getSeq();
			res.put(curPlace, curSeq);
		}
		
		return res;
	}
	static boolean parseArgs(String[] args)
	{
		for(int i = 0; i<args.length; i++)
		{
			if(args[i].indexOf('=') == -1)
			{
				if(args[i].endsWith("print_examples"))
				{
					PRINT_EXAMPLES = true;
				}
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
			else if(key.equals("max_distance"))
			{
				DISTANCE_THRESHOLD = Integer.parseInt(val);
			}
			else if(key.equals("min_similarity"))
			{
				ID_THRESHOLD = Double.parseDouble(val);
			}
			else if(key.equals("use_genome"))
			{
				GENOME_FILE = val;
			}
			else if(key.equals("scores_file"))
			{
				OUTPUT_FILE = val;
			}
		}
		if(groundTruthFilename.length() == 0 || irisCallsFilename.length() == 0)
		{
			return false;
		}
		return true;
	}
	static PosStore.Place getNearestVariant(
			PosStore.Place curPlace, TreeMap<PosStore.Place, String> truth)
	{
		if(truth.containsKey(curPlace))
		{
			return curPlace;
		}
		PosStore.Place lower = truth.lowerKey(curPlace);
		PosStore.Place higher = truth.higherKey(curPlace);
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
		
		PrintWriter out = new PrintWriter(new File(OUTPUT_FILE));
		
		TreeMap<PosStore.Place, String> truth = groundTruthFilename.endsWith(".vcf") ?
				readGroundTruthFromVcf(groundTruthFilename)
				: readGroundTruthFromFasta(groundTruthFilename);
		
		VcfEntryIterator vei = new VcfEntryIterator(irisCallsFilename);
		
		System.out.println("True insertions: " + truth.size());
		
		int falsePositives = 0;
		
		ArrayList<Long> distances = new ArrayList<Long>();
		ArrayList<Long> editDistances = new ArrayList<Long>();
		ArrayList<Double> sequenceIdentities = new ArrayList<Double>();
		
		int countLong = 0;
		
		for(IrisVcfEntry cur : vei)
		{
			if(!cur.getType().equals("INS"))
			{
				continue;
			}
			
			PosStore.Place curPlace = new PosStore.Place(cur.getChromosome(), cur.getPos());
			String curSeq = cur.getSeq();
			if(Math.abs(cur.getLength()) > 100000)
			{
				countLong++;
				continue;
			}
			PosStore.Place truthKey = getNearestVariant(curPlace, truth);
			if(truthKey == null || Math.abs(truthKey.pos - cur.getPos()) > DISTANCE_THRESHOLD)
			{
				falsePositives++;
			}
			else
			{
				String trueSeq = truth.get(truthKey);
				if(trueSeq.length() == 0) continue;
				int editDistance = 0;
				String oldCurSeq = curSeq;
				if(GENOME_FILE.length() > 0)
				{
					if(gq == null) gq = new IrisGenomeQuery(GENOME_FILE);
					long start = Math.max(1, curPlace.pos - 10);
					long end = curPlace.pos + 10;
					String before = gq.genomeSubstring(curPlace.chr, start, curPlace.pos - 1);
					String after = gq.genomeSubstring(curPlace.chr, curPlace.pos, end);
					curSeq = before + curSeq + after;
					editDistance = localEditDistance(trueSeq, curSeq);
					 
				}
				else
				{
					editDistance = ResultsTableWriter.editDistance(trueSeq, curSeq);
				}
				double seqIdentity = 1 - 1.0 * editDistance / trueSeq.length();
				if(seqIdentity < ID_THRESHOLD)
				{
					falsePositives++;
					continue;
				}
				if(PRINT_EXAMPLES && seqIdentity > .5 && seqIdentity < .8 && cur.getChromosome().contains("22"))
				{
					System.out.println(cur.getChromosome()+" "+cur.getPos()+" "+truthKey.pos+" "+seqIdentity+" "+oldCurSeq.length()+" "+trueSeq.length()+" "+oldCurSeq+" "+trueSeq);
				}
				truth.remove(truthKey);
				
				long dist = Math.abs(curPlace.pos - truthKey.pos);
				
				distances.add(dist);
				editDistances.add((long)editDistance);
				sequenceIdentities.add(seqIdentity);
				out.println(seqIdentity);
			}
			
			
		}
		
		int falseNegatives = truth.size();
		
		System.out.println("Insertions over 100kbp: " + countLong);
		
		System.out.println("False positives: " + falsePositives);
		System.out.println("False negatives: " + falseNegatives);
		System.out.println("Matches: " + distances.size());
		System.out.println("Average genomic distance: " + average(distances));
		System.out.println("Average sequence edit distance: " + average(editDistances));
		System.out.println("Average sequence identity: " + floatAverage(sequenceIdentities));
		
		out.close();
		
	}
	/*
	 * Gets the edit distance between query and the best possible substring of ref
	 * To allow the use of any substring, the following changes are made to the usual edit distance algorithm
	 *   table[0][i] is 0 for all i, meaning the match can start anywhere in the reference
	 *   min_j(table[n][j]) is used for the answer, meaning the match can end anywhere in the reference
	 */
	static int localEditDistance(String query, String ref)
	{
		String q = query.toLowerCase();
		String r = ref.toLowerCase();
		int n = query.length(), m = ref.length();
		int res = query.length();
		int[][] table = new int[n+1][m+1];
		for(int i = 1; i<=n; i++) table[i][0] = i;
		for(int i = 1; i<=n; i++)
			for(int j = 1; j <= m; j++)
			{
				table[i][j] = table[i-1][j-1] + (q.charAt(i-1) == r.charAt(j-1) ? 0 : 1);
				table[i][j] = Math.min(table[i][j], table[i-1][j] + 1);
				table[i][j] = Math.min(table[i][j], table[i][j-1] + 1);
				if(i == n)
				{
					res = Math.min(res, table[i][j]);
				}
			}
		return res;
	}
	static double floatAverage(ArrayList<Double> list)
	{
		double res = 0;
		for(double x : list) res += x;
		return list.size() == 0 ? 0.0 : res / list.size();
	}
	static double average(ArrayList<Long> list)
	{
		double res = 0;
		for(long x : list) res += x;
		return list.size() == 0 ? 0.0 : res / list.size();
	}
	
}
