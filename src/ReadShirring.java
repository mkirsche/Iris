import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * Gathering reads together to get a consensus sequence
 */
public class ReadShirring {
	static ArrayList<String> getReads(String key, ArrayList<String> readNames) throws Exception
	{
		String readFile = Settings.READS_FILE;
		String samFileName = key + ".sam";
		String bamFileName = key + ".bam";
		String fastqFileName = key + ".fastq";
		
		// Handle case when read file is actually a list of files
		ArrayList<String> readFiles = new ArrayList<String>();
		String[] readFileNames = readFile.split(",");
		for(String rfn : readFileNames)
		{
			readFiles.add(rfn);
		}
		
		// Get SAM file with all relevant reads
		extractReads(key, readNames, readFiles, samFileName);
		
		// Convert to bam file for input into bam2fastq
		samToBam(samFileName, bamFileName);
		if(Settings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(samFileName).delete();
		}
				
		// Generate bam2fq command for converting alignments to fastq file
		bamToFastq(bamFileName, fastqFileName);
		if(Settings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(bamFileName).delete();
		}
		
		ArrayList<String> readSequences = getReadsFromFastq(fastqFileName);
		if(Settings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(fastqFileName).delete();
		}
				
		return readSequences;
	}
	
	// Gets reads with names in the list that are within 10kbp of a target SV and outputs them to a SAM file
	static void extractReads(String key, ArrayList<String> readNames, ArrayList<String> readFiles, String samFileName) throws Exception
	{
		String chr = VcfEntry.getChrFromKey(key);
		long pos = VcfEntry.getPosFromKey(key);
		
		// Make grep query string to get header or any lines with 
		StringBuilder grepQuery = new StringBuilder("\"" + "^@");
		for(int i = 0; i<readNames.size(); i++)
		{
			grepQuery.append("|" + readNames.get(i));
		}
		grepQuery.append("\"");
		
		// Set up command to output alignments near SV and grep for names
		for(int i = 0; i<readFiles.size(); i++)
		{
			String readFile = readFiles.get(i);
			String samtoolsCommand = String.format("%s view " 
				+ (i == 0 ? "-h " : "") + "%s %s:%d-%d | grep -E %s >> %s", 
					Settings.SAMTOOLS_PATH,
					readFile,
					chr,
					pos - 10000,
					pos + 10000,
					grepQuery.toString(),
					samFileName);
			
			// Use bin/sh because pipes will not work when called directly
			String[] fullSamtoolsCommmand = new String[] {"/bin/sh", "-c", samtoolsCommand};
			Process child = Runtime.getRuntime().exec(fullSamtoolsCommmand);
			int p = child.waitFor();
			if(p != 0)
			{
				throw new Exception("getting alignments supporting " + key + " failed: " + samtoolsCommand);
			}
		}
	}
	
	static void samToBam(String samFileName, String bamFileName) throws Exception
	{
		String toBamCommand = String.format("%s view -h -S -b %s > %s", Settings.SAMTOOLS_PATH, samFileName, bamFileName);
		String[] fullBamtoolsCommmand = new String[] {"/bin/sh", "-c", toBamCommand};
		Process child = Runtime.getRuntime().exec(fullBamtoolsCommmand);
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("converting alignments to bam failed: " + toBamCommand);
		}
	}
	
	static void bamToFastq(String bamFileName, String fastqFileName) throws Exception
	{
		String toFastqCommand = String.format("%s bam2fq %s > %s", 
				Settings.SAMTOOLS_PATH,
				bamFileName, 
				fastqFileName);
		String[] fullFastqCommmand = new String[] {"/bin/sh", "-c", toFastqCommand};
		Process child = Runtime.getRuntime().exec(fullFastqCommmand);
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("converting alignments to fastq failed: " + toFastqCommand);
		}
	}
	
	static ArrayList<String> getReadsFromFastq(String fastqFileName) throws Exception
	{
		File f = new File(fastqFileName);
		if(!f.exists())
		{
			throw new Exception("trying to get reads from a fastq file which does not exist: " + fastqFileName);
		}
		Scanner input = new Scanner(new FileInputStream(f));
		ArrayList<String> res = new ArrayList<String>();
		while(input.hasNext())
		{
			try {
				input.nextLine();
				res.add(input.nextLine());
				for(int i = 0; i<2; i++) input.nextLine();
			} catch(Exception e) {
				input.close();
				throw new Exception("trying to get reads from malformed fastq file: " + fastqFileName);
			}
		}
		input.close();
		return res;
	}
}
