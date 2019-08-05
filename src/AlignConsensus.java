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
	static ArrayList<String> getConsensusAlignmentRecords(String id, ArrayList<String> consensusSequences, GenomeQuery gq) throws Exception
	{
		String ngmlrInFn = id + ".ngmlr.in";
		String ngmlrOutFn = id + ".ngmlr.out";
		String genomeSampleFn = id + ".region.fa";
		writeNgmlrInput(consensusSequences, ngmlrInFn);
		writeGenomeSample(id, genomeSampleFn, gq);
		executeNgmlr(ngmlrInFn, genomeSampleFn, ngmlrOutFn);
		return getNgmlrAlignmentStrings(ngmlrOutFn);
	}
	
	static void writeGenomeSample(String id, String gsFn, GenomeQuery gq) throws Exception
	{
		String chr = VcfEntry.getChrFromKey(id);
		long pos = VcfEntry.getPosFromKey(id);
		long start = Math.max(1, pos - Settings.GENOME_REGION_BUFFER);
		long end = pos + Settings.GENOME_REGION_BUFFER;
		String sample = gq.genomeSubstring(chr, start, end);
		
		PrintWriter out = new PrintWriter(new File(gsFn));
		out.println(String.format(">%s\n%s", chr, sample));
		out.close();
	}
	
	/*
	 * Given the assembled sequences, write them in FASTA format so they can be processed by ngmlr
	 */
	static void writeNgmlrInput(ArrayList<String> seqs, String ngmlrInputFileName) throws Exception
	{
		PrintWriter out = new PrintWriter(new File(ngmlrInputFileName));
		int n = seqs.size();
		for(int i = 0; i<n; i++)
		{
			out.println(String.format(">consensus%d\n%s", i, seqs.get(i)));
		}
		out.close();
	}
	
	/*
	 * Run ngmlr through the command line using parameters from Settings
	 */
	static void executeNgmlr(String ngmlrIn, String genomeSample, String ngmlrOut) throws Exception
	{
		String ngmlrCommand = String.format(
				 "%s -t %d -r %s -q %s -o %s", 
				 Settings.NGMLR_PATH, Settings.NGMLR_THREADS,
				 genomeSample, ngmlrIn, ngmlrOut);
		// Use bin/sh because pipes will not work when called directly
		String[] fullNgmlrCommand = new String[] {"/bin/sh", "-c", ngmlrCommand};
		Process child = Runtime.getRuntime().exec(fullNgmlrCommand);
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("error running ngmlr on " + ngmlrIn);
		}
	}
	
	/*
	 * Given the output produced from ngmlr, get all of the alignment records
	 */
	static ArrayList<String> getNgmlrAlignmentStrings(String ngmlrOutputFileName) throws Exception
	{
		File toRead = new File(ngmlrOutputFileName);
		if(!toRead.exists())
		{
			throw new Exception("could not find ngmlr output file: " + ngmlrOutputFileName);
		}
		
		ArrayList<String> alignmentRecords = new ArrayList<>();
				
		Scanner input = new Scanner(new FileInputStream(toRead));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(!line.startsWith("#"))
			{
				alignmentRecords.add(line);
			}
		}
		
		input.close();
		
		return alignmentRecords;
	}

}
