/*
 * Holds settings such as input/output filenames and various parameters
 */
public class Settings {
	static int VCF_PADDING_BEFORE = 1;
	static int VCF_PADDING_AFTER = 0;
	
	// Mandatory filenames
	static String VCF_FILE = "";
	static String VCF_OUT_FILE = "";
	static String GENOME_FILE = "";
	static String READS_FILE = "";
	
	// System options
	static int THREADS = 4;
	static int ALIGNMENT_THREADS = 4;
	static boolean CLEAN_INTERMEDIATE_FILES = true;
	static boolean RESUME = false;
	static String LOG_OUT_FILE = "";
	static String TABLE_OUT_FILE = "results.tsv";
	static String INTERMEDIATE_RESULTS_FILE = "resultsstore.txt";
	
	// External tool paths
	static String WORKING_DIR = System.getProperty("java.class.path") + "/..";
	static String SAMTOOLS_PATH = WORKING_DIR + "/" + "external_scripts/samtools";
	static String FALCONSENSE_PATH = WORKING_DIR + "/" + "external_scripts/falcon_sense";
	static String NGMLR_PATH = WORKING_DIR + "/" + "external_scripts/ngmlr";
	static String MINIMAP_PATH = WORKING_DIR + "/" + "external_scripts/minimap2";
	
	// Consensus options
	static double FALCONSENSE_MIN_IDT = 0.7;
	static int FALCONSENSE_MIN_LEN = 500;
	static int FALCONSENSE_MAX_READ_LEN = 1234567;
	static int FALCONSENSE_MIN_OVL_LEN = 250;
	static int FALCONSENSE_MIN_COV = 2;
	static int FALCONSENSE_N_CORE = 2;
	
	// Alignment options
	static boolean USE_MINIMAP = false;
	static int GENOME_REGION_BUFFER = 100000;
	
	// Insertion filter
	static int INSERTION_MIN_LENGTH = 30;
	static int INSERTION_MAX_DIST = 5000;
	
	static void usage()
	{
		System.out.println();
		System.out.println("Usage: java CrossStitch [args]");
		System.out.println("  Example: java CrossStitch genome_in=genome.fa vcf_in=sniffles.vcf ");
		System.out.println("      reads_in=reads.bam vcf_out=refined.vcf");
		System.out.println();
		System.out.println("Required args:");
		System.out.println("  genome_in (String) - the FASTA file containing the reference genome");
		System.out.println("  vcf_in    (String) - the VCF file with variant calls/supporting reads determined by Sniffles");
		System.out.println("  reads_in  (String) - the BAM file containing the reads (must be indexed with samtools)");
		System.out.println("  vcf_out   (String) - the name of the refined VCF file to be produced");
		System.out.println();
		System.out.println("Optional args:");
		System.out.println("  threads          (int) [4]    - the number of threads to use when running CrossStitch");
		System.out.println("  padding_before   (int) [1]    - the number of bases to output before the variant in REF/ALT fields");
		System.out.println("  padding_after    (int) [0]    - the number of bases to output after the variant in REF/ALT fields");
		System.out.println("  samtools_path    (String)     - the path to samtools if not using included binary");
		System.out.println("  ngmlr_path       (String)     - the path to ngmlr if using ngmlr and not using included binary");
		System.out.println("  minimap_path     (String)     - the path to minimap if using minimap and not using included binary");
		System.out.println("  falconsense_path (String)     - the path to falconsense if not using included binary");
		System.out.println("  log_out          (String)     - the name of the log file to be produced");
		System.out.println("  genome_buffer    (int) [100k] - the genome region on each side of the SV to align assembled reads to");
		System.out.println("  min_ins_length   (int) [30]   - the min length allowed for a refined insertion sequence");
		System.out.println("  max_ins_dist     (int) [5k]   - the max distance a refined insertion call can be from its old position");
		System.out.println("  --minimap                     - align with minimap instead of ngmlr");
		System.out.println("  --resume                      - use the results already computed from a previously terminated run");
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
				else if(args[i].endsWith("minimap"))
				{
					USE_MINIMAP = true;
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
				default:
					break;
			}
		}
		if(VCF_FILE.length() == 0 || GENOME_FILE.length() == 0 || VCF_OUT_FILE.length() == 0 || READS_FILE.length() == 0)
		{
			usage();
			System.exit(1);
		}
	}
}
