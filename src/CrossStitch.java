public class CrossStitch {
public static void main(String[] args) throws Exception
{
	Logger.init("log.out");
	Settings.parseArgs(args);
	SupportingReadMap srm = new SupportingReadMap(Settings.VCF_FILE);
	
	ParallelRunningStitch prs = new ParallelRunningStitch(srm, Settings.THREADS);
	prs.run();
	
	NewSequenceMap nsm = prs.results;
	GenomeQuery gq = new GenomeQuery(Settings.GENOME_FILE);
	
	VcfEditor ved = new VcfEditor(Settings.VCF_FILE, Settings.VCF_OUT_FILE, nsm, gq);
	ved.run();
	
	Logger.close();
}
}
