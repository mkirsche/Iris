import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.junit.Test;


public class UnitTester {
	
	static String sampleEntry = "21\t"
			+ "9076661\t"
			+ "28581\t"
			+ "N\t"
			+ "TAATGATCATTCTTGGTGATGTTT\t"
			+ ".\t"
			+ "PASS\t"
			+ "IMPRECISE;SVMETHOD=Snifflesv1.0.11;CHR2=21;END=9076661;"
			+ "STD_quant_start=1.087115;STD_quant_stop=11.762807;"
			+ "Kurtosis_quant_start=-1.371155;Kurtosis_quant_stop=-1.671550;"
			+ "SVTYPE=INS;"
			+ "RNAMES=SRR3212019.124574,SRR3212042.151583,SRR3212074.141721;"
			+ "SUPTYPE=AL,SR;SVLEN=23;STRANDS=+-;RE=23;REF_strand=7,4;AF=0.676471\t" 
			+ "GT:DR:DV 0/1:11:23";
	
	@Test public void testSuppReadMap() throws Exception
	{
		String vcfFile = "sreads.vcf";
		IrisVcfEntry ve = new IrisVcfEntry(sampleEntry);
		
		PrintWriter out = new PrintWriter(new File(vcfFile));
		out.println(ve);
		out.close();
		
		SupportingReadMap srm = new SupportingReadMap(vcfFile);
		String key = ve.getKey();
		assert(srm.contains(key));
		ArrayList<String> rnames = srm.get(key);
		assert(rnames.size() == 3);
		assert(rnames.get(0).equals("SRR3212019.124574"));
		assert(rnames.get(1).equals("SRR3212042.151583"));
		assert(rnames.get(2).equals("SRR3212074.141721"));
		
		new File(vcfFile).delete();
	}
	
	@Test public void testPadding() throws Exception
	{
		// First produce a genome file
		String chrName = "21";
		String genomeSeq = "ACGTACGTACGTACGTACGT";
		String genomeFn = "sample.fa";
		
		PrintWriter out = new PrintWriter(new File(genomeFn));
		out.println(">"+chrName);
		out.println(genomeSeq);
		out.close();
		
		IrisGenomeQuery gq = new IrisGenomeQuery("sample.fa");
		
		// Now produce old VCF file
		String oldVcf = "sample.vcf";
		int oldPos = 10;
		IrisVcfEntry oldve = new IrisVcfEntry(sampleEntry);
		oldve.setChromosome(chrName);
		oldve.setPos(oldPos);
		
		out = new PrintWriter(new File(oldVcf));
		out.println(oldve);
		out.close();
		
		// Now set up updated variant and add it to a map
		String newAlt = "AAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		int newPos = 12;
		
		NewSequenceMap nsm = new NewSequenceMap();
		nsm.add("21:10:28581", newAlt, newPos);
		
		// Run the VCF editor
		String newVcf = "out.vcf";
		VcfEditor ved = new VcfEditor("sample.vcf", newVcf, "out.tsv", nsm, gq);
		ved.run();
		
		VcfEntryIterator vei = new VcfEntryIterator(newVcf);
		int count = 0;
		for(IrisVcfEntry ve : vei)
		{
			String neededBefore = genomeSeq.substring(newPos - IrisSettings.VCF_PADDING_BEFORE - 1, newPos - 1);
			String neededAfter = genomeSeq.substring(newPos - 1, newPos + IrisSettings.VCF_PADDING_AFTER - 1);
			assert(ve.getRef().equals(neededBefore + neededAfter));
			assert(ve.getAlt().equals(neededBefore + newAlt + neededAfter));
			count++;
		}
		
		new File(genomeFn).delete();
		new File(genomeFn + ".fai").delete();
		new File(oldVcf).delete();
		new File(newVcf).delete();
		
		assert(count == 1);
	}
	
	@Test
	public void IrisVcfEntryFields() throws Exception
	{
		IrisVcfEntry ve = new IrisVcfEntry(sampleEntry);
		assertEquals(ve.getChromosome(), "21");
		assertEquals(ve.getPos(), 9076661L);
		assertEquals(ve.getId(), "28581");
		assertEquals(ve.getRef(), "N");
		assertEquals(ve.getAlt(), "TAATGATCATTCTTGGTGATGTTT");
		assertEquals(ve.getLength(), 23);
		assertEquals(ve.getType(), "INS");
	}
	
	@Test(expected = Exception.class)
	public void invalidIrisVcfEntryException() throws Exception
	{
		String line = "0\t\0\t0\t0\t0\t0\t0";
		new IrisVcfEntry(line); 
	}

}
