import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Scanner;

/*
 * Handles the creation of a new VCF file given the old file and map of updated insertion sequences 
 */
public class VcfEditor {
	
	String oldFile;
	String newFile;
	NewSequenceMap nsm;
	GenomeQuery gq;
	
	static int BEFORE = 1;
	static int AFTER = 0;
	
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
		long pos = ve.getPos();
		long start = pos - BEFORE;
		long end = pos + AFTER - 1;
		String res = gq.genomeSubstring(chr, start, end);
		return res;
	}
	
	/*
	 * Updates the REF and ALT sequences for a variant by adding appropriate padding 
	 */
	void updateBeforeAfter(VcfEntry ve) throws Exception
	{
		String beforeAfter = getBeforeAfter(ve);
		ve.setRef(beforeAfter);
		ve.setAlt(beforeAfter.substring(0, BEFORE) + ve.getAlt() + beforeAfter.substring(BEFORE));
	}
	
	static class VcfEntryIterator implements Iterable<VcfEntry> 
	{
		Scanner input;
		VcfEntryIterator(String fn) throws Exception
		{
			input = new Scanner(new FileInputStream(new File(fn)));
		}

		@Override
		public Iterator<VcfEntry> iterator() {
			
			// TODO Auto-generated method stub
			return new Iterator<VcfEntry>() {

				@Override
				public boolean hasNext() {
					// TODO Auto-generated method stub
					return input.hasNext();
				}

				@Override
				public VcfEntry next() {
					// TODO Auto-generated method stub
					try {
						return new VcfEntry(input.nextLine());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
				}
				
			};
		}
		
	}
	
	void run() throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(oldFile)));
		PrintWriter out = new PrintWriter(new File(newFile));
		while(input.hasNext())
		{
			String line = input.nextLine();
			
			// Handle header line separately
			if(line.charAt(0) == '@')
			{
				// Just copy the header
				out.println(line);
			}
			
			else
			{
				VcfEntry ve = new VcfEntry(line);
				String key = ve.getKey();
				
				// If this variant is in the map, update its info according to the new sequence/position
				if(nsm.containsKey(key))
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
					if(ve.getType().equals("INS") || ve.getType().equals("DEL"))
					{
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
