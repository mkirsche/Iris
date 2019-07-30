import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

public class GenomeQuery {
	String filename;
	
	// Assume that samtools is on the user's path
	static String samtoolsCommand = "samtools";
	
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
		String samtoolsTestCommand = "samtools > /dev/null 2>&1; echo $?";
		Process child = Runtime.getRuntime().exec(samtoolsTestCommand);
        InputStream seqStream = child.getInputStream();
		Scanner seqInput = new Scanner(seqStream);
		
		// Make sure an exit code is output
        if(!seqInput.hasNext())
        {
        	seqInput.close();
        	throw new Exception("samtools test did not produce an exit code: \"samtools > /dev/null 2>&1; echo $?\"");
        }
        String exitCodeString = seqInput.next();
        seqInput.close();
        
        // Wrap in try-catch to make sure the command produced an integer exit code
        try {
        	int exitCode = Integer.parseInt(exitCodeString);
        	
        	// Non-zero means the command failed, usually because samtools is not installed or on path
        	if(exitCode != 0)
        	{
        		throw new Exception("samtools produced non-zero exit code (" 
        				+ exitCode + ") - perhaps it is not on your path: " + samtoolsCommand);
        	}
        } catch(Exception e) {
        	throw new Exception("samtools exit code test produced non-integer output: " + exitCodeString);
        }
	}
	
	String genomeSubstring(String chr, long startPos, long endPos) throws Exception
	{
		String faidxCommand = String.format("samtools faidx %s %s:%d-%d", filename, chr, startPos, endPos);
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
