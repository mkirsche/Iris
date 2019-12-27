import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * A wrapper around Racon for polishing the SV sequence
 */
public class Racon {
	
	static String getDraft(String oldSeq, IrisGenomeQuery gq, String id) throws Exception
	{
		String type = IrisVcfEntry.getTypeFromKey(id);
		long pos = IrisVcfEntry.getPosFromKey(id);
		String chr = IrisVcfEntry.getChrFromKey(id);
		
		long start = Math.max(1, pos - IrisSettings.RACON_BUFFER);
		long end = pos + IrisSettings.RACON_BUFFER;
		
		if(type.equals("INS"))
		{
			return gq.genomeSubstring(chr, start, pos-1) + oldSeq 
					+ gq.genomeSubstring(chr, pos, end);
		}
		else if(type.equals("DEL"))
		{
			return gq.genomeSubstring(chr, start, end);
		}
		else
		{
			return oldSeq;
		}
	}
	
	static boolean isAlphanumeric(String s)
	{
		for(int i = 0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			if(!Character.isLetterOrDigit(c))
			{
				return false;
			}
		}
		return true;
	}
	
	static ArrayList<String> getConsensusSequences(String id, String oldSeq, IrisGenomeQuery gq, ArrayList<String> reads) throws Exception
	{
		if(!isAlphanumeric(oldSeq))
		{
			Logger.log("Could not run racon on " + id + " because of non-alphanumeric variant sequence: " + oldSeq);
			return new ArrayList<String>();
		}
		String raconInAll = IrisSettings.addOutDir(id + ".racon.fa");
		String raconInSingle = IrisSettings.addOutDir(id + ".racon.seq.fa");
		String raconInAlign = IrisSettings.addOutDir(id + ".racon.align.sam");
		String raconOutFn = IrisSettings.addOutDir(id + ".racon.out");
		String draft = getDraft(oldSeq, gq, id);
		writeRaconInput(reads, raconInAll, raconInSingle, raconInAlign, draft);
		int numRuns = IrisSettings.RACON_ITERS;
		executeRacon(raconInAll, raconInSingle, raconInAlign, raconOutFn, numRuns);
		ArrayList<String> res = parseRaconOutput(raconOutFn);
		if(IrisSettings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(raconInAll).delete();
			new File(raconInSingle).delete();
			new File(raconInAlign).delete();
			new File(raconOutFn).delete();
			if(IrisSettings.RACON_ITERS > 1)
			{
				for(int i = 2; i<=IrisSettings.RACON_ITERS; i++)
				{
					new File(raconOutFn + "_" + i + ".fa").delete();
					new File(raconOutFn + "_" + i + ".sam").delete();
				}
			}
		}
		return res;
	}
	
	/*
	 * Given a list of reads as strings, name them arbitrarily and write to a file
	 * Use FASTA format, which is what Racon uses
	 * Also write a file with a single sequence consisting of the relevant region of the genome
	 * plus a candidate SV
	 * Then compute alignments from the reads to the candidate sequence
	 * 
	 * Returns the maximum length of any read
	 * 
	 */
	static void writeRaconInput(ArrayList<String> reads, String raconAllFileName, 
			String raconSingleFileName, String raconAlignFileName, String oldSeq) throws Exception
	{
		// Write file of all sequences
		PrintWriter out = new PrintWriter(new File(raconAllFileName));
		int n = reads.size();
		for(int i = 0; i<n; i++)
		{
			out.println(String.format(""
					+ ">read%d\n%s", i, reads.get(i)));
		}
		out.close();
		
		// Write file of single sequence
		out = new PrintWriter(new File(raconSingleFileName));
		out.println(">draft1\n" + oldSeq);
		out.close();
		
		// Compute minimap alignments of reads to sequence
		AlignConsensus.executeMinimap(raconAllFileName, raconSingleFileName, raconAlignFileName);
		
		// Make sure there were actual alignments
		AlignmentParser ap = new AlignmentParser(raconAlignFileName);
		if(ap.countAlignedReads() == 0)
		{
			throw new Exception("No alignments found for polishing in " + raconAlignFileName);
		}
	}
	
	/*
	 * Run Racon through the command line using parameters from Settings
	 */
	static void executeRacon(String raconInAll, String raconInSingle,
			String raconInAlignments, String raconOut, int numRuns) throws Exception
	{
		if(numRuns > 1)
		{
			String intermediateOutput = raconOut + "_" + numRuns + ".fa";
			executeRacon(raconInAll, raconInSingle, raconInAlignments, intermediateOutput, 1);
			String intermediateAlign = raconOut + "_" + numRuns + ".sam";
			AlignConsensus.executeMinimap(raconInAll, intermediateOutput, intermediateAlign);
			executeRacon(raconInAll, intermediateOutput, intermediateAlign, raconOut, numRuns - 1);
			return;
		}
		String fsCommand = String.format(
				 "%s %s %s %s", 
				 IrisSettings.RACON_PATH, raconInAll, raconInAlignments, raconInSingle);
		
		ArrayList<String> fullFsCommand = new ArrayList<String>();
		for(String s : fsCommand.split(" ")) fullFsCommand.add(s);
		Process child = new ProcessBuilder()
				.command(fullFsCommand)
				.redirectOutput(new File(raconOut))
				.start();
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("error running racon on " + raconInSingle +" "+p);
		}
	}
	
	/*
	 * Given the output produced from Racon, get the corrected sequence
	 */
	static ArrayList<String> parseRaconOutput(String raconOutputFileName) throws Exception
	{
		File toRead = new File(raconOutputFileName);
		if(!toRead.exists())
		{
			throw new Exception("could not find racon output file: " + raconOutputFileName);
		}
		
		StringBuilder currentSequence = new StringBuilder("");
		
		Scanner input = new Scanner(new FileInputStream(toRead));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.startsWith(">"))
			{
				continue;
			}
			currentSequence.append(line);
		}
		
		input.close();
		
		ArrayList<String> res = new ArrayList<String>();
		res.add(currentSequence.toString());
		return res;
	}

}
