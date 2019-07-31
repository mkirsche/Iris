import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Scanner;

class VcfEntryIterator implements Iterable<VcfEntry> 
{
	Scanner input;
	String last;
	VcfEntryIterator(String fn) throws Exception
	{
		input = new Scanner(new FileInputStream(new File(fn)));
		last = null;
	}
	@Override
	public Iterator<VcfEntry> iterator() {
		
		return new Iterator<VcfEntry>() {
			@Override
			public boolean hasNext() {
				if(last != null)
				{
					return true;
				}
				while(input.hasNext() && last == null)
				{
					String cur = input.nextLine();
					if(cur.length() == 0 || cur.charAt(0) == '@')
					{
						continue;
					}
					last = cur;
				}
				return last != null;
			}
			@Override
			public VcfEntry next() {
				try {
					String curLine = last;
					last = null;
					return new VcfEntry(curLine);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			
		};
	}
	
}