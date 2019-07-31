import java.io.File;
import java.io.PrintWriter;

public class Logger {
	static PrintWriter out;
	static void init(String fn) throws Exception
	{
		Logger.out = new PrintWriter(new File(fn));
	}
	static void log(String s)
	{
		out.println(s);
	}
	static void close()
	{
		Logger.out.close();
	}
}
