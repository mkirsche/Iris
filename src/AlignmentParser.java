/*
 * Utility for parsing SAM-format alignments
 */
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class AlignmentParser {
	
	ArrayList<Alignment> alns;
	AlignmentParser(String fn)  throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(fn)));
		alns = new ArrayList<Alignment>();
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.length() == 0 || line.startsWith("@"))
			{
				continue;
			}
			alns.add(new Alignment(line));
		}
		input.close();
	}
	
	int countAlignedReads()
	{
		int res = 0;
		for(Alignment aln : alns)
		{
			if(aln.isAligned())
			{
				res++;
			}
		}
		return res;
	}
	
	class Alignment
	{
		String[] fields;
		Alignment(String line)
		{
			fields = line.split("\t");
		}
		int getFlag()
		{
			return Integer.parseInt(fields[1]);
		}
		boolean isAligned()
		{
			return (getFlag() & 4) == 0;
		}
	}
}
