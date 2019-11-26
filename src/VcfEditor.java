import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * Handles the creation of a new VCF file given the old file and map of updated insertion sequences 
 */
public class VcfEditor {
	
	String oldFile;
	String newFile;
	String tableFile;
	NewSequenceMap nsm;
	GenomeQuery gq;
	
	/*
	 * oldFile is the original VCF files produced by Sniffles
	 * newFile is the name of the new VCF file to make
	 * nsm is the map from variant key to new sequence/position
	 * gq is the wrapper around samtools faidx for getting sequences from the original genome
	 */
	VcfEditor(String oldFile, String newFile, String tableFile, NewSequenceMap nsm, GenomeQuery gq) throws Exception
	{
		this.oldFile = oldFile;
		if(!(new File(oldFile)).exists()) {
			throw new Exception("old vcf file does not exist: " + oldFile);
		}
		this.newFile = newFile;
		this.tableFile = tableFile;
		this.nsm = nsm;
		this.gq = gq;
	}
	
	/*
	 * Gets padding characters before and after a variant based on its position
	 */
	String getBeforeAfter(IrisVcfEntry ve) throws Exception
	{
		String chr = ve.getChromosome();
		long pos = ve.getPos() + 1;
		
		if(!consumesReference(ve.getType()))
		{
			long start = pos - IrisSettings.VCF_PADDING_BEFORE;
			long end = pos + IrisSettings.VCF_PADDING_AFTER - 1;
			String res = gq.genomeSubstring(chr, start, end);
			return res;
		}
		else
		{
			long start1 = pos - IrisSettings.VCF_PADDING_BEFORE;
			long end1 = pos - 1;
			long start2 = pos + ve.getLength();
			long end2 = start2 + IrisSettings.VCF_PADDING_AFTER - 1;
			String res = gq.genomeSubstring(chr, start1, end1) + gq.genomeSubstring(chr, start2, end2);
			return res;
		}
	}
	
	boolean consumesReference(String type)
	{
		if(type.equals("DEL"))
		{
			return true;
		}
		return false;
	}
	
	/*
	 * Updates the REF and ALT sequences for a variant by adding appropriate padding 
	 */
	void updateBeforeAfter(IrisVcfEntry ve) throws Exception
	{
		String beforeAfter = getBeforeAfter(ve);
		ve.setRef(beforeAfter.substring(0, IrisSettings.VCF_PADDING_BEFORE) 
				+ ve.getRef() + beforeAfter.substring(IrisSettings.VCF_PADDING_BEFORE));
		ve.setAlt(beforeAfter.substring(0, IrisSettings.VCF_PADDING_BEFORE) 
				+ ve.getAlt() + beforeAfter.substring(IrisSettings.VCF_PADDING_BEFORE));
	}
	
	void run() throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(oldFile)));
		PrintWriter out = new PrintWriter(new File(newFile));
		ResultsTableWriter tableOut = new ResultsTableWriter(tableFile);
		
		VcfHeader header = new VcfHeader();
		boolean headerPrinted = false;
		
		while(input.hasNext())
		{
			String line = input.nextLine();
			
			// Handle header line separately
			if(line.charAt(0) == '#')
			{
				header.addLine(line);
			}
			
			else
			{
				if(!headerPrinted)
				{
					headerPrinted = true;
					header.addInfoField("IRIS_REFINED", "1", "String", "Whether or not a variant has been refined by Iris");
					header.addInfoField("IRIS_PROCESSED", "1", "String", "Whether or not a variant has been considered by Iris for refinement");
					header.print(out);
				}
				IrisVcfEntry ve = new IrisVcfEntry(line);
				ve.setInfo("IRIS_PROCESSED", "1");
				if(Math.abs(ve.getLength()) > IrisSettings.MAX_OUTPUT_LENGTH)
				{
					if(IrisSettings.KEEP_LONG_VARIANTS)
					{
						Logger.log("Printing original VCF entry for " + ve.getKey() + " because length is too long");
						out.println(ve);
					}
					else
					{
						Logger.log("Not outputting SV for " + ve.getKey() + " because length is too long");
					}
					continue;
				}
				String key = ve.getKey();
				
				// Print the entry to the results table
				NewSequenceMap.UpdatedEntry newEntry = null;
				if(nsm.containsKey(key))
				{
					newEntry = new NewSequenceMap.UpdatedEntry(nsm.getSeq(key), nsm.getPos(key));
				}
				
				try {
					tableOut.printEntry(ve, gq, nsm.containsKey(key) ? newEntry : null);
				} catch (Exception e) {
					Logger.log("Failed to write " + ve + " to results table");
				}
				
				// If this variant is in the map, update its info according to the new sequence/position
				if(nsm.containsKey(key) && nsm.getPos(key) != -1)
				{
					ve.setInfo("IRIS_REFINED", "1");
					// Make necessary replacements
					if(ve.getType().equals("INS"))
					{
						Logger.log("Outputting refined insertion for " + key);
						String newSeq = nsm.getSeq(key);
						long newPos = nsm.getPos(key);
						
						ve.setPos(newPos);
						ve.updateInsertionSequence(newSeq);
						updateBeforeAfter(ve);
						
						out.println(ve);
					}
					else if(ve.getType().equals("DEL"))
					{
						Logger.log("Outputting refined deletion for " + key);
						int newLength = nsm.getSeq(key).length();
						long newPos = nsm.getPos(key);
						String newSeq = gq.genomeSubstring(ve.getChromosome(), newPos + 1, newPos + newLength);
						
						ve.setPos(newPos);
						ve.updateDeletion(newSeq);
						updateBeforeAfter(ve);
						
						out.println(ve);
					}
				}
				else
				{
					ve.setInfo("IRIS_REFINED", "0");
					// When there is no sequence, don't change REF/ALT
					if(ve.getSeq().length() == 0)
					{
						out.println(ve);
						continue;
					}
					if(ve.getType().equals("INS"))
					{
						Logger.log("Outputting original insertion for " + key);
						String seq = ve.getSeq();
						if(seq.length() != 0)
						{
							ve.setRef("");
							ve.setAlt(seq);
							updateBeforeAfter(ve);
						}
					}
					else if(ve.getType().equals("DEL"))
					{
						Logger.log("Outputting original deletion for " + key);
						String chr = ve.getChromosome();
						// Fix off-by-one in indexing
						long pos = ve.getPos() + 1;
						ve.setRef(gq.genomeSubstring(chr, pos, pos + Math.abs(ve.getLength()) - 1));
						ve.setAlt("");
						updateBeforeAfter(ve);
					}
					out.println(ve);
				}
			}
			
			
		}
		
		// This case only occurs when the VCF is only the header, so don't bother add new fields here
		if(!headerPrinted)
		{
			headerPrinted = true;
			header.print(out);
		}
		tableOut.close();
		out.close();
		input.close();
	}

}
