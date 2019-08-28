public class CrossStitch {
public static void main(String[] args) throws Exception
{
	Logger.init(Settings.LOG_OUT_FILE);
	Settings.parseArgs(args);
	SupportingReadMap srm = new SupportingReadMap(Settings.VCF_FILE);
	GenomeQuery gq = new GenomeQuery(Settings.GENOME_FILE);
	
	ParallelRunningStitch prs = new ParallelRunningStitch(srm, Settings.THREADS, gq);
	prs.run();
	
	NewSequenceMap nsm = prs.results;
	
	VcfEditor ved = new VcfEditor(Settings.VCF_FILE, Settings.VCF_OUT_FILE, nsm, gq);
	ved.run();
	
	Logger.close();
}
}
