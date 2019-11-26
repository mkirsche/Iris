import java.util.*;
import java.io.*;
public class PrintInsertionSequences {
public static void main(String[] args) throws Exception
{
	String fn = "/home/mkirsche/git/RepeatMasker/hg002.refined.vcf";
	String ofn = "/home/mkirsche/git/RepeatMasker/hg002.insertions.fasta";
	Scanner input = new Scanner(new FileInputStream(new File(fn)));
	int count = 0;
	PrintWriter out = new PrintWriter(new File(ofn));
	while(input.hasNext())
	{
		String line = input.nextLine();
		if(line.length() == 0 || line.startsWith("#"))
		{
			continue;
		}
		IrisVcfEntry ve = new IrisVcfEntry(line);
		if(ve.getType().equals("INS"))
		{
			String seq = ve.getSeq();
			if(seq.length() > 0)
			{
				out.printf(">insertion%d\n%s\n", count+1, seq);
				count++;
			}
		}
	}
	System.out.printf("Found %d insertions\n", count);
	input.close();
	out.close();
}
}
