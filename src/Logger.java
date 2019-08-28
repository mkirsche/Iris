import java.io.File;
import java.io.PrintWriter;

public class Logger {
	static PrintWriter out;
	static long startTime;
	static void init(String fn) throws Exception
	{
		startTime = System.currentTimeMillis();
		Logger.out = new PrintWriter(new File(fn));
	}
	static void log(String s)
	{
		s = addTimeToLog(s);
		out.println(s);
	}
	static void std_log(String s)
	{
		s = addTimeToLog(s);
		System.out.println(s);
	}
	static String addTimeToLog(String s)
	{
		long curTime = System.currentTimeMillis() - startTime;
		return s + " (time = " + formatTime(curTime) + ")";
	}
	static void close()
	{
		Logger.out.close();
	}
	
	static String formatTime(long ms)
	{
		long days = ms / 1000 / 60 / 60 / 24;
		long hours = (ms / 1000 / 60 / 60) % 24;
		long minutes = (ms / 1000 / 60) % 60;
		double seconds = (ms / 1000) % 60 + (ms % 1000) / 1000.0;
		return String.format("%02d:%02d:%02d:%02.3f", days, hours, minutes, seconds);
	}
}
