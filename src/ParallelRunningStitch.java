/*
 * Managing the parallelization of computing consensus sequences/positions
 */
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelRunningStitch {
	IrisGenomeQuery gq;
	SupportingReadMap readMap;
	NewSequenceMap results;
	IntermediateResultsStore irs;
	int numThreads;
	String[] keys;
	AtomicInteger variantsProcessed = new AtomicInteger(0);
	AtomicInteger variantsWithErrors = new AtomicInteger(0);
	ConcurrentLinkedQueue<Integer> todo;
	
	ParallelRunningStitch(SupportingReadMap readMap, int numThreads, IrisGenomeQuery gq) throws Exception
	{
		this.gq = gq;
		this.readMap = readMap;
		keys = readMap.keyArray();
		Logger.log("Number of variants to refine with supporting reads: " + keys.length);
		this.numThreads = numThreads;
		
		irs = new IntermediateResultsStore(IrisSettings.INTERMEDIATE_RESULTS_FILE, IrisSettings.RESUME);
		
		todo = new ConcurrentLinkedQueue<Integer>();
		for(int i = 0; i<keys.length; i++)
		{
			todo.add(i);
		}
		results = new NewSequenceMap();
	}
	
	void run() throws Exception
	{
		// Here the last thread in the array is the main thread, so it calls
		// run() instead of start() and doesn't get joined below
		Rayon[] threads = new Rayon[numThreads];
		for(int i = 0; i<numThreads; i++)
		{
			threads[i] = new Rayon();
			if(i == numThreads - 1)
			{
				threads[i].run();
			}
			else
			{
				threads[i].start();
			}
		}
		for(int i = 0; i<numThreads-1; i++)
		{
			threads[i].join();
		}
		irs.fillMapFromStore(results);
	}
	
	public class Rayon extends Thread {
		
		@Override
		public void run() {
			while(!todo.isEmpty()) {
				Integer cur = todo.poll();
				if(cur != null)
				{
					String variantKey = keys[cur];
					Logger.log("Starting to process " + variantKey);
					
					if(IrisSettings.RESUME && irs.set.contains(variantKey))
					{
						Logger.log("Using results from previous run for " + variantKey);
						int numDone = variantsProcessed.incrementAndGet();
						Logger.log("Done processing " + variantKey + " (total processed = " + numDone + ")");
						continue;
					}
					
					ArrayList<String> readNames = readMap.get(variantKey);
					NewSequenceMap.UpdatedEntry ue;
					try {
						ue = NewSequenceMap.fromReadNames(variantKey, readMap.seqMap, readNames, gq);
						if(ue == null) {
							Logger.log("No refined SV found for " + variantKey);
							irs.addNullVariant(variantKey);
							continue;
						}
						Logger.log("Found refined SV of new length " + ue.seq.length() + 
								" and new pos " + ue.pos + " for " + variantKey);
						irs.addVariant(variantKey, ue.seq, ue.pos);
						results.add(variantKey, ue.seq, ue.pos);
					} catch (Exception e) {
						variantsWithErrors.incrementAndGet();
						e.printStackTrace();
						
						// Remove Racon's files in the case of a crash since there can be many of them
						if(IrisSettings.CLEAN_INTERMEDIATE_FILES)
						{
							File f;
							
							// Remove racon intermediate files
							String raconInAll = variantKey + ".racon.fa";
							String raconInSingle = variantKey + ".racon.seq.fa";
							String raconInAlign = variantKey + ".racon.align.sam";
							String raconOutFn = variantKey + ".racon.out";
							if((f = new File(raconInAll)).exists()) f.delete();
							if((f = new File(raconInSingle)).exists()) f.delete();
							if((f = new File(raconInAlign)).exists()) f.delete();
							if((f = new File(raconOutFn)).exists()) f.delete();
							if(IrisSettings.RACON_ITERS > 1)
							{
								for(int i = 2; i<=IrisSettings.RACON_ITERS; i++)
								{
									String fastaName = raconOutFn + "_" + i + ".fa";
									String outName = raconOutFn + "_" + i + ".sam";
									if((f = new File(fastaName)).exists()) f.delete();
									if((f = new File(outName)).exists()) f.delete();
								}
							}
							
							// Remove falconsense intermediate files
							String falconInFn = variantKey + ".falcon.in";
							String falconOutFn = variantKey + ".falcon.out";
							if((f=new File(falconInFn)).exists()) f.delete();
							if((f=new File(falconOutFn)).exists()) f.delete();
							
							// Remove alignment files
							String alignInFn = variantKey + ".align.in";
							String alignOutFn = variantKey + ".align.out";
							String genomeSampleFn = variantKey + ".region.fa";
							if((f=new File(alignInFn)).exists()) f.delete();
							if((f=new File(alignOutFn)).exists()) f.delete();
							if((f=new File(genomeSampleFn)).exists()) f.delete();
							if((f=new File(genomeSampleFn + "-enc.2.ngm")).exists()) f.delete();
							if((f=new File(genomeSampleFn + "-ht-13-2.2.ngm")).exists()) f.delete();
							
							// Remove read extraction files
							String samFileName = variantKey + ".sam";
							String bamFileName = variantKey + ".bam";
							String fastqFileName = variantKey + ".fastq";
							if((f=new File(samFileName)).exists()) f.delete();
							if((f=new File(bamFileName)).exists()) f.delete();
							if((f=new File(fastqFileName)).exists()) f.delete();
						}
						
						Logger.log("Found error in " + variantKey);
					}
					int numDone = variantsProcessed.incrementAndGet();
					Logger.log("Done processing " + variantKey + " (total processed = " + numDone + ")");
					
					// Safeguard against creating too many intermediate files
					if(numDone >= 50 && variantsWithErrors.get() * 2 > numDone)
					{
						Logger.log("Terminating thread because too many refinements crashed");
						break;
					}
				}
			}
			
		}
		
	}
}
