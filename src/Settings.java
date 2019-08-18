/*
 * Holds settings such as input/output filenames and various parameters
 */
public class Settings {
	static int VCF_PADDING_BEFORE = 1;
	static int VCF_PADDING_AFTER = 0;
	static String VCF_FILE = "pbAll.sniffles.vcf";
	static String VCF_OUT_FILE = "out.vcf";
	static String GENOME_FILE = "base.fa";
	static String READS_FILE = "pbAll.bam";
	static int THREADS = 4;
	static String SAMTOOLS_PATH = "/usr/local/bin/samtools";
	static boolean CLEAN_INTERMEDIATE_FILES = true;
	
	static double FALCONSENSE_MIN_IDT = 0.7;
	static int FALCONSENSE_MIN_LEN = 500;
	static int FALCONSENSE_MAX_READ_LEN = 123_456_789;
	static int FALCONSENSE_MIN_OVL_LEN = 250;
	static int FALCONSENSE_MIN_COV = 2;
	static int FALCONSENSE_N_CORE = 2;
	static String FALCONSENSE_PATH = "/usr/local/bin/falcon_sense";
	
	static int GENOME_REGION_BUFFER = 100_000;
	
	static String NGMLR_PATH = "/usr/local/bin/ngmlr";
	static int NGMLR_THREADS = 4;
	
	static int INSERTION_MIN_LENGTH = 30;
	static int INSERTION_MAX_DIST = 5000;
	
	static void usage()
	{
		System.out.println("Usage: (haven't written this message yet)");
	}

	static void parseArgs(String[] args) throws Exception
	{
		for(int i = 0; i<args.length; i++)
		{
			if(args[i].indexOf('=') == -1)
			{
				continue;
			}
			int equalIdx = args[i].indexOf('=');
			String key = args[i].substring(0, equalIdx);
			String val = args[i].substring(1 + equalIdx);
			switch(key) 
			{
				case "padding_before": 
					VCF_PADDING_BEFORE = Integer.parseInt(val);
					break;
				case "padding_after":
					VCF_PADDING_AFTER = Integer.parseInt(val);
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
		if(VCF_FILE == null || GENOME_FILE == null || VCF_OUT_FILE == null || READS_FILE == null)
		{
			usage();
			System.exit(1);
		}
	}
}
