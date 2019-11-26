public class Iris {
public static void main(String[] args) throws Exception
{
	runIris(args);
}

static void runIris(String[] args) throws Exception
{
	Logger.init(IrisSettings.LOG_OUT_FILE);
	IrisSettings.parseArgs(args);
	SupportingReadMap srm = new SupportingReadMap(IrisSettings.VCF_FILE);
	GenomeQuery gq = new GenomeQuery(IrisSettings.GENOME_FILE);
	
	ParallelRunningStitch prs = new ParallelRunningStitch(srm, IrisSettings.THREADS, gq);
	prs.run();
	
	NewSequenceMap nsm = prs.results;
	
	VcfEditor ved = new VcfEditor(IrisSettings.VCF_FILE, IrisSettings.VCF_OUT_FILE, 
			IrisSettings.TABLE_OUT_FILE, nsm, gq);
	ved.run();
	
	Logger.log("Iris completed - output is in " + IrisSettings.VCF_OUT_FILE);
	Logger.log("Total number of variants with errors: " + prs.variantsWithErrors.get() + " out of " + prs.variantsProcessed.get());
	Logger.close();
}
}
