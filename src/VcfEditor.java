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
	NewSequenceMap nsm;
	GenomeQuery gq;
	
	/*
	 * oldFile is the original VCF files produced by Sniffles
	 * newFile is the name of the new VCF file to make
	 * nsm is the map from variant key to new sequence/position
	 * gq is the wrapper around samtools faidx for getting sequences from the original genome
	 */
	VcfEditor(String oldFile, String newFile, NewSequenceMap nsm, GenomeQuery gq) throws Exception
	{
		this.oldFile = oldFile;
		if(!(new File(oldFile)).exists()) {
			throw new Exception("old vcf file does not exist: " + oldFile);
		}
		this.newFile = newFile;
		this.nsm = nsm;
		this.gq = gq;
	}
	
	/*
	 * Gets padding characters before and after a variant based on its position
	 */
	String getBeforeAfter(VcfEntry ve) throws Exception
	{
		String chr = ve.getChromosome();
		long pos = ve.getPos() + 1;
		
		if(!consumesReference(ve.getType()))
		{
			long start = pos - Settings.VCF_PADDING_BEFORE;
			long end = pos + Settings.VCF_PADDING_AFTER - 1;
			String res = gq.genomeSubstring(chr, start, end);
			return res;
		}
		else
		{
			long start1 = pos - Settings.VCF_PADDING_BEFORE;
			long end1 = pos - 1;
			long start2 = pos + ve.getLength();
			long end2 = start2 + Settings.VCF_PADDING_AFTER - 1;
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
	void updateBeforeAfter(VcfEntry ve) throws Exception
	{
		String beforeAfter = getBeforeAfter(ve);
		ve.setRef(beforeAfter.substring(0, Settings.VCF_PADDING_BEFORE) 
				+ ve.getRef() + beforeAfter.substring(Settings.VCF_PADDING_BEFORE));
		ve.setAlt(beforeAfter.substring(0, Settings.VCF_PADDING_BEFORE) 
				+ ve.getAlt() + beforeAfter.substring(Settings.VCF_PADDING_BEFORE));
	}
	
	void run() throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(oldFile)));
		PrintWriter out = new PrintWriter(new File(newFile));
		while(input.hasNext())
		{
			String line = input.nextLine();
			
			// Handle header line separately
			if(line.charAt(0) == '#')
			{
				// Just copy the header
				out.println(line);
			}
			
			else
			{
				VcfEntry ve = new VcfEntry(line);
				String key = ve.getKey();
				
				// If this variant is in the map, update its info according to the new sequence/position
				if(nsm.containsKey(key) && nsm.getPos(key) != -1)
				{
					// Make necessary replacements
					String newSeq = nsm.getSeq(key);
					long newPos = nsm.getPos(key);
					
					ve.setPos(newPos);
					ve.updateInsertionSequence(newSeq);
					updateBeforeAfter(ve);
					
					out.println(ve);
				}
				else
				{
					if(ve.getType().equals("INS"))
					{
						updateBeforeAfter(ve);
					}
					else if(ve.getType().equals("DEL"))
					{
						String chr = ve.getChromosome();
						// Fix off-by-one in indexing
						long pos = ve.getPos() + 1;
						ve.setRef(gq.genomeSubstring(chr, pos, pos+Math.abs(ve.getLength()) - 1));
						ve.setAlt("");
						//System.out.println(ve.getRef()+" "+ve.getAlt());
						updateBeforeAfter(ve);
					}
					out.println(ve);
				}
			}
			
			
		}
		out.close();
		input.close();
	}

}
