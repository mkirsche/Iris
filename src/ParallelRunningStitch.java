/*
 * Managing the parallelization of computing consensus sequences/positions
 */
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelRunningStitch {
	GenomeQuery gq;
	SupportingReadMap readMap;
	NewSequenceMap results;
	IntermediateResultsStore irs;
	int numThreads;
	String[] keys;
	AtomicInteger variantsProcessed = new AtomicInteger(0);
	AtomicInteger variantsWithErrors = new AtomicInteger(0);
	ConcurrentLinkedQueue<Integer> todo;
	
	ParallelRunningStitch(SupportingReadMap readMap, int numThreads, GenomeQuery gq) throws Exception
	{
		this.gq = gq;
		this.readMap = readMap;
		keys = readMap.keyArray();
		Logger.log("Number of variants to refine with supporting reads: " + keys.length);
		this.numThreads = numThreads;
		
		irs = new IntermediateResultsStore(Settings.INTERMEDIATE_RESULTS_FILE, Settings.RESUME);
		
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
					
					if(Settings.RESUME && irs.set.contains(variantKey))
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
						Logger.log("Found error in " + variantKey);
					}
					int numDone = variantsProcessed.incrementAndGet();
					Logger.log("Done processing " + variantKey + " (total processed = " + numDone + ")");
				}
			}
			
		}
		
	}
}
