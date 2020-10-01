import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * A wrapper around Ngmlr for aligning assembled sequences back to the reference
 */
public class AlignConsensus {
	
	/*
	 * Given the assembled sequences, align them to a region of the reference with ngmlr
	 */
	static ArrayList<String> getConsensusAlignmentRecords(String id, ArrayList<String> consensusSequences, IrisGenomeQuery gq) throws Exception
	{
		String alignInFn = IrisSettings.addOutDir(id + ".align.in");
		String alignOutFn = IrisSettings.addOutDir(id + ".align.out");
		String genomeSampleFn = IrisSettings.addOutDir(id + ".region.fa");
		writeMinimapInput(consensusSequences, alignInFn);
		writeGenomeSample(id, genomeSampleFn, gq);
		
		executeMinimap(alignInFn, genomeSampleFn, alignOutFn);
		
		ArrayList<String> res = getMinimapAlignmentStrings(alignOutFn);
		
		if(IrisSettings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(alignInFn).delete();
			new File(alignOutFn).delete();
			new File(genomeSampleFn).delete();
		}
		return res;
	}
	
	static void writeGenomeSample(String id, String gsFn, IrisGenomeQuery gq) throws Exception
	{
		String chr = IrisVcfEntry.getChrFromKey(id);
		long pos = IrisVcfEntry.getPosFromKey(id);
		long start = Math.max(1, pos - IrisSettings.GENOME_REGION_BUFFER);
		long end = pos + IrisSettings.GENOME_REGION_BUFFER;
		String sample = gq.genomeSubstring(chr, start, end);
		
		PrintWriter out = new PrintWriter(new File(gsFn));
		out.println(String.format(">%s\n%s", chr, sample));
		out.close();
	}
	
	/*
	 * Given the assembled sequences, write them in FASTA format so they can be processed by ngmlr
	 */
	static void writeMinimapInput(ArrayList<String> seqs, String minimapInputFileName) throws Exception
	{
		PrintWriter out = new PrintWriter(new File(minimapInputFileName));
		int n = seqs.size();
		for(int i = 0; i<n; i++)
		{
			out.println(String.format(">consensus%d\n%s", i, seqs.get(i)));
		}
		out.close();
	}
	
	/*
	 * Run minimap2 through the command line using parameters from Settings
	 */
	static void executeMinimap(String minimapIn, String genomeSample, String minimapOut) throws Exception
	{
		String minimapCommand = String.format(
				 "%s -L -c -a -x %s -t %d %s %s -o %s", 
				 IrisSettings.MINIMAP_PATH, IrisSettings.MINIMAP_MODE, IrisSettings.ALIGNMENT_THREADS,
				 genomeSample, minimapIn, minimapOut);
		ArrayList<String> fullMinimapCommand = new ArrayList<String>();
		for(String s : minimapCommand.split(" ")) fullMinimapCommand.add(s);
		Process child = new ProcessBuilder()
				.command(fullMinimapCommand)
				.start();
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("error running minimap on " + minimapIn);
		}
	}
	
	/*
	 * Given the output produced from minimap, get all of the alignment records
	 */
	static ArrayList<String> getMinimapAlignmentStrings(String minimapOutputFileName) throws Exception
	{
		File toRead = new File(minimapOutputFileName);
		if(!toRead.exists())
		{
			throw new Exception("could not find minimap output file: " + minimapOutputFileName);
		}
		
		ArrayList<String> alignmentRecords = new ArrayList<String>();
				
		Scanner input = new Scanner(new FileInputStream(toRead));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(!line.startsWith("@"))
			{
				//System.out.println(line);
				alignmentRecords.add(line);
			}
		}
		
		input.close();
		
		return alignmentRecords;
	}

}
