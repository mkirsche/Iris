public class Iris {
public static void main(String[] args) throws Exception
{
	runIris(args);
}

static void runIris(String[] args) throws Exception
{
	Logger.init(Settings.LOG_OUT_FILE);
	Settings.parseArgs(args);
	SupportingReadMap srm = new SupportingReadMap(Settings.VCF_FILE);
	GenomeQuery gq = new GenomeQuery(Settings.GENOME_FILE);
	
	ParallelRunningStitch prs = new ParallelRunningStitch(srm, Settings.THREADS, gq);
	prs.run();
	
	NewSequenceMap nsm = prs.results;
	
	VcfEditor ved = new VcfEditor(Settings.VCF_FILE, Settings.VCF_OUT_FILE, 
			Settings.TABLE_OUT_FILE, nsm, gq);
	ved.run();
	
	Logger.log("Iris completed - output is in " + Settings.VCF_OUT_FILE);
	Logger.log("Total number of variants with errors: " + prs.variantsWithErrors.get() + " out of " + prs.variantsProcessed.get());
	Logger.close();
}
}
