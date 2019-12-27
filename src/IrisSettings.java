import java.io.File;
import java.nio.file.Paths;

/*
 * Holds settings such as input/output filenames and various parameters
 */
public class IrisSettings {
	static int VCF_PADDING_BEFORE = 1;
	static int VCF_PADDING_AFTER = 0;
	
	// Mandatory filenames
	static String VCF_FILE = "";
	static String VCF_OUT_FILE = "";
	static String GENOME_FILE = "";
	static String READS_FILE = "";
	
	// System options
	static int THREADS = 4;
	static int ALIGNMENT_THREADS = 1;
	static boolean CLEAN_INTERMEDIATE_FILES = true;
	static boolean RESUME = false;
	static boolean PROCESS_DELETIONS = false;
	static String LOG_OUT_FILE = "";
	static String TABLE_OUT_FILE = "results.tsv";
	static String INTERMEDIATE_RESULTS_FILE = "resultsstore.txt";
	static String RNAMES_FIELDNAME = "RNAMES";
	static String OUT_DIR = "";
	
	// External tool paths
	static String getIrisWorkingDir()
	{
		String res = Iris.class.getResource("Iris.class").getPath();
		if(res.contains("/"))
		{
			res = res.substring(0, res.lastIndexOf('/')) + "/..";
		}
		else
		{
			res = "..";
		}
		return res;
	}
	static String WORKING_DIR = getIrisWorkingDir();//System.getProperty("java.class.path") + "/..";
	static String SAMTOOLS_PATH = WORKING_DIR + "/" + "external_scripts/samtools";
	static String FALCONSENSE_PATH = WORKING_DIR + "/" + "external_scripts/falcon_sense";
	static String NGMLR_PATH = WORKING_DIR + "/" + "external_scripts/ngmlr";
	static String MINIMAP_PATH = WORKING_DIR + "/" + "external_scripts/minimap2";
	static String RACON_PATH = WORKING_DIR + "/" + "external_scripts/racon";
	
	// Consensus options
	static double FALCONSENSE_MIN_IDT = 0.7;
	static int FALCONSENSE_MIN_LEN = 500;
	static int FALCONSENSE_MAX_READ_LEN = 1234567;
	static int FALCONSENSE_MIN_OVL_LEN = 250;
	static int FALCONSENSE_MIN_COV = 2;
	static int FALCONSENSE_N_CORE = 1;
	static boolean USE_FALCONSENSE = false;
	static int RACON_BUFFER = 1000;
	static int RACON_ITERS = 1;
	
	// Alignment options
	static boolean USE_NGMLR = false;
	static int GENOME_REGION_BUFFER = 100000;
	static String MINIMAP_MODE = "map-ont";
	
	// Insertion filter
	static int INSERTION_MIN_LENGTH = 30;
	static int INSERTION_MAX_DIST = 100;
	static double MAX_LENGTH_CHANGE = 0.25;
	static int MAX_OUTPUT_LENGTH = 100000;
	static boolean KEEP_LONG_VARIANTS = false;
	
	static void usage()
	{
		System.out.println();
		System.out.println("Usage: java Iris [args]");
		System.out.println("  Example: java Iris genome_in=genome.fa vcf_in=sniffles.vcf ");
		System.out.println("      reads_in=reads.bam vcf_out=refined.vcf");
		System.out.println();
		System.out.println("Required args:");
		System.out.println("  genome_in (String) - the FASTA file containing the reference genome");
		System.out.println("  vcf_in    (String) - the VCF file with variant calls/supporting reads determined by Sniffles");
		System.out.println("  reads_in  (String) - the BAM file containing the reads (must be indexed with samtools)");
		System.out.println("  vcf_out   (String) - the name of the refined VCF file to be produced");
		System.out.println();
		System.out.println("Optional args:");
		System.out.println("  threads          (int) [4]    - the number of threads to use when running Iris");
		System.out.println("  padding_before   (int) [1]    - the number of bases to output before the variant in REF/ALT fields");
		System.out.println("  padding_after    (int) [0]    - the number of bases to output after the variant in REF/ALT fields");
		System.out.println("  samtools_path    (String)     - the path to samtools if not using included binary");
		System.out.println("  ngmlr_path       (String)     - the path to ngmlr if using ngmlr and not using included binary");
		System.out.println("  minimap_path     (String)     - the path to minimap if using minimap and not using included binary");
		System.out.println("  falconsense_path (String)     - the path to falconsense if using falconsense and not using included binary");
		System.out.println("  racon_path       (String)     - the path to racon if not using included binary");
		System.out.println("  log_out          (String)     - the name of the log file to be produced");
		System.out.println("  out_dir          (String)     - the directory where intermediate files go");
		System.out.println("  genome_buffer    (int)    [100k] - the genome region on each side of the SV to align assembled reads to");
		System.out.println("  min_ins_length   (int)    [30]   - the min length allowed for a refined insertion sequence");
		System.out.println("  max_ins_dist     (int)    [100]  - the max distance a refined insertion call can be from its old position");
		System.out.println("  max_out_length   (int)    [100k] - the max length of variant which will be output");
		System.out.println("  max_len_change   (float)  [0.25] - the max proportion by which a variant's length can change");
		System.out.println("  --ngmlr                       - align with ngmlr instead of minimap");
		System.out.println("  --falconsense                 - compute consensus with falconsense instead of racon");
		System.out.println("  --keep_files                  - don't remove intermediate files - used for debugging");
		System.out.println("  --also_deletions              - also try to refine deletion positions/lengths");
		System.out.println("  --resume                      - use the results already computed from a previously terminated run");
		System.out.println("  --pacbio                      - if using minimap as the aligner, run in pacbio mode");
		System.out.println("  --rerunracon                  - if using racon for consensus, run it twice");
		System.out.println("  --keep_long_variants          - output original VCF line for very long variants instead of ignoring them");
		System.out.println();
	}
	
	static long parseLong(String s) throws Exception
	{
		s = s.toLowerCase();
		if(s.endsWith("g") || s.endsWith("b") || s.endsWith("kkk"))
		{
			return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1e9 + .5);
		}
		if(s.endsWith("m") || s.endsWith("kk"))
		{
			return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1e6 + .5); 
		}
		if(s.endsWith("k"))
		{
			return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1e3 + .5); 
		}
		return Long.parseLong(s);
	}
	
	static int parseInt(String s) throws Exception
	{
		return (int)parseLong(s);
	}

	static void parseArgs(String[] args) throws Exception
	{
		for(int i = 0; i<args.length; i++)
		{
			if(args[i].indexOf('=') == -1)
			{
				if(args[i].endsWith("resume"))
				{
					RESUME = true;
				}
				else if(args[i].endsWith("ngmlr"))
				{
					USE_NGMLR = true;
				}
				else if(args[i].endsWith("falconsense"))
				{
					USE_FALCONSENSE = true;
				}
				else if(args[i].endsWith("keep_files"))
				{
					CLEAN_INTERMEDIATE_FILES = false;
				}
				else if(args[i].endsWith("pacbio"))
				{
					MINIMAP_MODE = "map-pb";
				}
				else if(args[i].endsWith("also_deletions"))
				{
					PROCESS_DELETIONS = true;
				}
				else if(args[i].endsWith("rerun_racon"))
				{
					if(RACON_ITERS == 1)
					{
						RACON_ITERS = 2;
					}
				}
				else if(args[i].endsWith("keep_long_variants"))
				{
					KEEP_LONG_VARIANTS = true;
				}
				continue;
			}
			int equalIdx = args[i].indexOf('=');
			String key = args[i].substring(0, equalIdx);
			while(key.length() > 0 && key.charAt(0) == '-')
			{
				key = key.substring(1);
			}
			String val = args[i].substring(1 + equalIdx);
			switch(key) 
			{
				case "threads":
					THREADS = Integer.parseInt(val);
					break;
				case "genome_buffer":
					GENOME_REGION_BUFFER = parseInt(val);
					break;
				case "racon_buffer":
					RACON_BUFFER = parseInt(val);
					break;
				case "padding_before": 
					VCF_PADDING_BEFORE = parseInt(val);
					break;
				case "padding_after":
					VCF_PADDING_AFTER = parseInt(val);
					break;
				case "min_ins_length":
					INSERTION_MIN_LENGTH = parseInt(val);
					break;
				case "max_ins_dist":
					INSERTION_MAX_DIST = parseInt(val);
					break;
				case "samtools_path":
					SAMTOOLS_PATH = val;
					break;
				case "ngmlr_path":
					NGMLR_PATH = val;
					break;
				case "minimap_path":
					MINIMAP_PATH = val;
					break;
				case "falconsense_path":
					FALCONSENSE_PATH = val;
					break;
				case "racon_path":
					RACON_PATH = val;
					break;
				case "log_out":
					LOG_OUT_FILE = val;
					break;
				case "vcf_in":
					VCF_FILE = val;
					break;
				case "genome_in":
					GENOME_FILE = val;
					break;
				case "vcf_out":
					VCF_OUT_FILE = val;
					break;
				case "reads_in":
					READS_FILE = val;
					break;
				case "racon_iters":
					RACON_ITERS = parseInt(val);
					break;
				case "rnames_fieldname":
					RNAMES_FIELDNAME = val;
					break;
				case "max_out_length":
					MAX_OUTPUT_LENGTH = parseInt(val);
					break;
				case "max_len_change":
					MAX_LENGTH_CHANGE = Double.parseDouble(val);
					break;
				case "out_dir":
					OUT_DIR = Paths.get("").toAbsolutePath().toString() + "/" + val;
					File f = new File(OUT_DIR);
					f.mkdir();
				default:
					break;
			}
		}
		if(VCF_FILE.length() == 0 || GENOME_FILE.length() == 0 || VCF_OUT_FILE.length() == 0 || READS_FILE.length() == 0)
		{
			usage();
			System.exit(1);
		}
		INTERMEDIATE_RESULTS_FILE = addOutDir(INTERMEDIATE_RESULTS_FILE);
		TABLE_OUT_FILE = addOutDir(TABLE_OUT_FILE);
		if(LOG_OUT_FILE.length() > 0)
		{
			LOG_OUT_FILE = addOutDir(LOG_OUT_FILE);
		}
	}
	
	/*
	 * Adds the intermediate file output directory to the path of a file
	 */
	static String addOutDir(String fn)
	{
		if(OUT_DIR.length() > 0)
		{
			return OUT_DIR + "/" + fn;
		}
		else return fn;
	}
}
