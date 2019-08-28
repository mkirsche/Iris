import java.io.File;
import java.io.PrintWriter;

public class Logger {
	static PrintWriter out = null;
	static long startTime;
	static void init(String fn) throws Exception
	{
		startTime = System.currentTimeMillis();
		if(fn.length() != 0)
		{
			out = new PrintWriter(new File(fn));
		}
	}
	static void log(String s)
	{
		s = addTimeToLog(s);
		if(out != null) out.println(s);
		else System.out.println(s);
	}
	static String addTimeToLog(String s)
	{
		long curTime = System.currentTimeMillis() - startTime;
		return s + " (time = " + formatTime(curTime) + ")";
	}
	static void close()
	{
		if(out != null) out.close();
	}
	
	static String formatTime(long ms)
	{
		long days = ms / 1000 / 60 / 60 / 24;
		long hours = (ms / 1000 / 60 / 60) % 24;
		long minutes = (ms / 1000 / 60) % 60;
		long seconds = (ms / 1000) % 60;
		long millis = ms % 1000;
		return String.format("%02d:%02d:%02d:%02d.%03d", days, hours, minutes, seconds, millis);
	}
}
