/*
 * Holds settings such as input/output filenames and various parameters
 */
public class Settings {
	static int VCF_PADDING_BEFORE = 1;
	static int VCF_PADDING_AFTER = 0;
	static String VCF_FILE;
	static String VCF_OUT_FILE;
	static String GENOME_FILE;
	static String READS_FILE;
	
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
