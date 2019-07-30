import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

public class GenomeQuery {
	String filename;
	
	// Assume that samtools is on the user's path
	static String samtoolsCommand = "/usr/local/bin/samtools";
	
	GenomeQuery(String filename) throws Exception
	{
		testSamtoolsInstalled();
		boolean validFile = new File(filename).exists();
		if(!validFile) {
			throw new Exception("geonome file does not exist: " + filename);
		}
		this.filename = filename;
	}
	
	/*
	 * Runs a simple samtools command and inspects the exit code to make sure it is installed
	 */
	void testSamtoolsInstalled() throws Exception
	{
		String samtoolsTestCommand = samtoolsCommand;
		System.out.println(samtoolsTestCommand);
		Process child = Runtime.getRuntime().exec(samtoolsTestCommand);
        int seqExit = child.waitFor();
		
		// Make sure an exit code is output
        // Exit code > 1 means the command failed, usually because samtools is not installed or on path
        if(seqExit > 1)
        {
        	throw new Exception("samtools produced bado exit code (" 
        			+ seqExit + ") - perhaps it is not on your path: " + samtoolsCommand);
        }
	}
	
	String genomeSubstring(String chr, long startPos, long endPos) throws Exception
	{
		String faidxCommand = String.format(samtoolsCommand + " faidx %s %s:%d-%d", filename, chr, startPos, endPos);
		System.out.println(faidxCommand);
		Process child = Runtime.getRuntime().exec(faidxCommand);
        InputStream seqStream = child.getInputStream();
		Scanner seqInput = new Scanner(seqStream);
		
		// Make sure it produced an actual output
		if(!seqInput.hasNext())
        {
        	seqInput.close();
        	throw new Exception("samtools faidx did not produce an output: " + faidxCommand);
        }
		// Read in and ignore sequence name
		seqInput.next();
		
		// Make sure there's also a sequence
		if(!seqInput.hasNext())
		{
			seqInput.close();
        	throw new Exception("samtools faidx produced a sequence name but not an actual sequence: " + faidxCommand);
		}
		
		String res = seqInput.next();
		seqInput.close();
		return res;
	}
}
