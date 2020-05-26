/*
 * Makes a few consistency checks for the resulting VCF entries 
 * Currently this is not part of the pipeline and is just used for validation during development.
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class VcfValidate
{
	public static void main(String[] args) throws Exception
	{
		String fn = "/home/mkirsche/splicetest/hg002_crossstitch.refined.vcf";
		Scanner input = new Scanner(new FileInputStream(new File(fn)));
		
		IrisVcfHeader header = new IrisVcfHeader();
		
		// Deletion statistics
		int consistentDeletions = 0;
		int totalDeletions = 0;
		
		// Insertion statistics
		int consistentInsertions = 0;
		int totalInsertions = 0;
		int insertionsWithoutSeq = 0;
		
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.startsWith("#"))
			{
				header.addLine(line);
			}
			else
			{
				IrisVcfEntry entry = new IrisVcfEntry(line);
				String type = entry.getType();
				int refLength = entry.getRef().length();
				int altLength = entry.getAlt().length();
				if(type.equals("DEL"))
				{
					totalDeletions++;
					
					// Length in the SVLEN field
					int svlenField = Integer.parseInt(entry.getInfo("SVLEN"));
					
					// Difference in REF and ALT fields
					int diff = altLength - refLength;
					
					// Start minus end
					int endDist = (int)entry.getPos() - Integer.parseInt(entry.getInfo("END"));
					
					// Make sure REF is longer than ALT since it's a deletion
					if(diff < 0)
					{
						// Check for inconsistencies
						if(svlenField != diff || diff != endDist)
						{
							System.out.println("INCONSISTENT DELETION");
							System.out.println("  id: " + entry.getId() + "; svlen: " + svlenField + ", altLen - refLen: " + diff + ", endDist: " + endDist);
							System.out.println("  " + entry.getRef()+" "+entry.getAlt() + "\n");
						}
						else
						{
							consistentDeletions++;
						}
					}
					else
					{
						// If REF was shorter than ALT, it's probably symbolic notation
						System.out.println("DELETION WITHOUT SEQUENCE: ");
						System.out.println("  " + line);
						System.out.println();
					}
				}
				
				else if(type.equals("INS"))
				{
					totalInsertions++;
					
					// Check if it has a sequence
					boolean hasSeq = entry.getSeq().length() > 0;
					if(!hasSeq)
					{
						insertionsWithoutSeq++;
					}
					
					boolean consistent = true;
					
					// ALT length minus REF length
					int diff = altLength - refLength;
					
					String seq = entry.getSeq();
					
					// Check if the length of SEQ is equal to the ALT - REF difference
					if(hasSeq && seq.length() != diff)
					{
						consistent = false;
						System.out.println("INCONSISTENT INSERTION");
						System.out.println("  id: " + entry.getId() + "; altLen - refLen: " + diff + ", seqLength: " + seq.length());
						System.out.println("  " + entry.getRef()+" "+entry.getAlt() + "\n");
					}
					
					// Check that SVLEN field is the right length
					if(entry.hasInfoField("SVLEN") && Integer.parseInt(entry.getInfo("SVLEN")) != diff)
					{
						consistent = false;
						System.out.println("INCONSISTENT INSERTION");
						System.out.println("  id: " + entry.getId() + "; altLen - refLen: " + diff + ", SVLEN: " + entry.getInfo("SVLEN"));
						System.out.println("  " + entry.getRef() + " " + entry.getAlt() + "\n");
					}
					
					// Check END minus position
					if(Integer.parseInt(entry.getInfo("END")) - (int)entry.getPos() != 0)
					{
						consistent = false;
						int endDiff = Integer.parseInt(entry.getInfo("END")) - (int)entry.getPos();
						System.out.println("INCONSISTENT INSERTION");
						System.out.println("  id: " + entry.getId() + "; altLen - refLen: " + diff + ", END - pos:" + endDiff);
						System.out.println("  " + entry.getRef() + " " + entry.getAlt() + "\n");
					}
					
					if(consistent)
					{
						consistentInsertions++;
					}
				}
			}
		}
		
		input.close();
		
		// Output some statistics
		System.out.println("Found " + consistentDeletions + " consistent deletions (out of " + totalDeletions + ")");
		System.out.println("Found " + consistentInsertions + " consistent insertions (out of " + totalInsertions + ")");
		System.out.println("  Without sequence: " + insertionsWithoutSeq);
	}
}
