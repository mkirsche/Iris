import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * A wrapper around Falconsense for getting the consensus of a set of reads
 */
public class FalconSense {
	
	/*
	 * Given the reads, output them in falconsense input format, and pass them on
	 */
	static ArrayList<String> getConsensusSequences(String id, ArrayList<String> reads) throws Exception
	{
		String falconInFn = id + ".falcon.in";
		String falconOutFn = id + ".falcon.out";
		int maxLength = writeFalconSenseInput(reads, falconInFn);
		executeFalconSense(falconInFn, maxLength, falconOutFn);
		ArrayList<String> res = parseFalconSenseOutput(falconOutFn);
		if(Settings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(falconInFn).delete();
			new File(falconOutFn).delete();
		}
		return res;
	}
	
	/*
	 * Given a list of reads as strings, name them arbitrarily and write to a file
	 * Use Falconsense input format, which is as follows:
	 * 
	 * name[0] seq[0]
	 * name[i] seq[i] (for i != 0)
	 * + +
	 * name[1] seq[1]
	 * name[i] seq[i] (for i != 1)
	 * + +
	 * ...
	 * name[n-1] seq[n-1]
	 * name[i] seq[i] (for i != n-1)
	 * + +
	 * - - 
	 * 
	 * Returns the maximum length of any read
	 * 
	 */
	static int writeFalconSenseInput(ArrayList<String> reads, String falconInputFileName) throws Exception
	{
		PrintWriter out = new PrintWriter(new File(falconInputFileName));
		int n = reads.size();
		int maxLength = 0;
		for(int i = 0; i<n; i++)
		{
			maxLength = Math.max(maxLength, reads.get(i).length());
			out.println(String.format("read%d %s", i, reads.get(i)));
			for(int j = 0; j<n; j++)
			{
				if(i == j) continue;
				out.println(String.format("read%d %s", j, reads.get(j)));
			}
			out.println("+ +");
		}
		out.println("- -");
		out.close();
		return maxLength + 1;
	}
	
	/*
	 * Run Falconsense through the command line using parameters from Settings
	 */
	static void executeFalconSense(String falconIn, int maxLength, String falconOut) throws Exception
	{
		String fsCommand = String.format(
				 "%s --min_idt %f --min_len %d --max_read_len %d "
				 + "--min_ovl_len %d --min_cov %d --n_core %d "
				 ,//+ "> %s < %s", 
				 Settings.FALCONSENSE_PATH, Settings.FALCONSENSE_MIN_IDT,
				 Settings.FALCONSENSE_MIN_LEN, maxLength,
				 Settings.FALCONSENSE_MIN_OVL_LEN, Settings.FALCONSENSE_MIN_COV,
				 Settings.FALCONSENSE_N_CORE);//, falconOut, falconIn);
		// Use bin/sh because pipes will not work when called directly
		ArrayList<String> fullFsCommand = new ArrayList<String>();
		for(String s : fsCommand.split(" ")) fullFsCommand.add(s);
		Process child = new ProcessBuilder()
				.command(fullFsCommand)
				.redirectInput(new File(falconIn))
				.redirectOutput(new File(falconOut))
				.start();
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("error running falconsense on " + falconIn+" "+p);
		}
	}
	
	/*
	 * Given the output produced from Falconsense, get the different possible sequences
	 */
	static ArrayList<String> parseFalconSenseOutput(String falconOutputFileName) throws Exception
	{
		File toRead = new File(falconOutputFileName);
		if(!toRead.exists())
		{
			throw new Exception("could not find falconsense output file: " + falconOutputFileName);
		}
		
		ArrayList<String> consensusSequences = new ArrayList<String>();
		
		StringBuilder currentSequence = new StringBuilder("");
		
		Scanner input = new Scanner(new FileInputStream(toRead));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.startsWith(">"))
			{
				if(currentSequence.length() > 0)
				{
					consensusSequences.add(currentSequence.toString());
				}
				currentSequence = new StringBuilder("");
			}
			else
			{
				currentSequence.append(line);
			}
		}
		
		if(currentSequence.length() > 0)
		{
			consensusSequences.add(currentSequence.toString());
		}
		
		input.close();
		
		return consensusSequences;
	}

}
