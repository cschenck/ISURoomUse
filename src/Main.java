import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Main {
	
	private static enum Day {
		Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
	}
	
	private static class Pair implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2298269488622104394L;
		public final int a, b;
		public Pair(int a, int b)
		{
			this.a = a;
			this.b = b;
		}
		public boolean overlaps(Pair p) {
			return (b + 10 >= p.a && p.b + 10 >= a) || (p.b + 10 >= a && b + 10 >= p.a);
		}
	}
	
	private static class RoomUsageData implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2908624900836522667L;
		private Map<Day, List<Pair>> times;
		public RoomUsageData() {
			times = new HashMap<Main.Day, List<Pair>>();
			for(Day d : Day.values())
				times.put(d, new ArrayList<Pair>());
		}
		
		public void addTime(Day d, int start, int end)
		{
			//we need to check to see if this new use time overlaps with any of our old times
			//since standard passing time between classes is 10min, we'll add 10min when computing
			//anything that uses the end times of the class to the end time
			Pair current = new Pair(start, end);
			for(int i = 0; i < times.get(d).size(); i++)
			{
				Pair old = times.get(d).get(i);
				//for something to overlap either the end time of the old one and start time of the new one overlap
				//or the opposite
				if(old.overlaps(current))
				{
					//take the earliest start as the new start
					int newStart = (current.a < old.a ? current.a : old.a);
					//take the latest end as the new end
					int newEnd = (current.b < old.b ? old.b : current.b);
					//remove the old entry that overlapped
					times.get(d).remove(i);
					//now call this method with the new time
					this.addTime(d, newStart, newEnd);
					return;
				}
			}
			
			//if we made it here nothing overlaps
			times.get(d).add(current);
			
			//now lets sort the list of times so that when we print them out, they are in a logical order
			Collections.sort(times.get(d), new Comparator<Pair>() {
				public int compare(Pair o1, Pair o2) {
					//since all the times that get here are gaurneteed to not be overlapping
					//we can just sort by start time
					return o1.a - o2.a;
				}
			});
		}
		
		public String getUsage(Day d, int start, int end)
		{
			String ret = "";
			Pair current = new Pair(start, end);
			for(Pair p : times.get(d))
			{
				if(p.overlaps(current))
					ret += convertMinutesToCommonTime(p.a) + "-" + convertMinutesToCommonTime(p.b) + ",";
			}
			return ret;
		}
		
		public String getUsage(Day d)
		{
			return getUsage(d, 0, 24*60 - 1);
		}
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		mainMenu();	
	}
	
	private static void mainMenu()
	{
		System.out.println("Room Usage Aggregator");
		System.out.println("Type \'help\' to get a listing of commands");
		
		Map<String, Map<String, RoomUsageData>> data = null;
		File f = new File("cachedUsage.serial");
		if(f.exists())
		{
			try {
				data = (Map<String, Map<String, RoomUsageData>>) new ObjectInputStream(new FileInputStream(f)).readObject();
				System.out.println("Found cached usage results.");
			} catch (ClassNotFoundException | IOException e) {
				data = null;
			}
		}
		 if(data == null)
			System.out.println("Unable to find cached usage results. Please run \'reload\' before trying to access room usage data.");
		
		String input = "";
		Scanner in = new Scanner(System.in);
		String setBuilding = "";
		String setRoom = "";
		while(!input.equals("exit"))
		{
			try {
				System.out.print(setBuilding + "/" + setRoom + ">");
				input = in.nextLine();
				if(input.startsWith("help"))
				{
					Scanner s = new Scanner(input);
					s.next();
					String arg = "";
					if(s.hasNext())
						arg = s.next();
					
					if(arg.equals(""))
					{
						System.out.println("help");
						System.out.println("reload");
						System.out.println("set");
						System.out.println("clear");
						System.out.println("list");
						System.out.println("usage");
						System.out.println("exit");
						System.out.println("Type \'help\' plust a command to get more information");
					}
					else if(arg.equals("reload"))
					{
						System.out.print("Connects to ISU's course servers and parses room usage data. ");
						System.out.print("The results are automatically cached for later use. ");
						System.out.println("Accepts no arguments.");
					}
					else if(arg.equals("set"))
					{
						System.out.print("Takes exactly one argument. ");
						System.out.print("If no building is set, then the argument is the building to set. ");
						System.out.println("If a building is set, then the argument is the room in the building to set. ");
					}
					else if(arg.equals("clear"))
					{
						System.out.print("If a room is set, then clears it. ");
						System.out.print("If no room is set, then clears the building. ");
						System.out.println("Takes no arguments.");
					}
					else if(arg.equals("list"))
					{
						System.out.print("If no arguments are given and no building or room is set, lists all the buildings in alphabetical order. ");
						System.out.print("If no argumetns are given and a building is set, lists all the rooms in that building. ");
						System.out.print("If a building is given as an argument, lists all the rooms in that building. ");
						System.out.println("Multiple buildings can be given as an argument by appending with a semicolon (e.g. A;B). ");
					}
					else if(arg.equals("usage"))
					{
						System.out.println("Takes the following arguments:");
						System.out.println("-b A;B;C = argument specifies the building/s to list usage for. Required if no building has been set.");
						System.out.println("-r Z;X;Y = argument specifies the room/s to list usage for. Optional and only valid if one building is being listed. If one building is being listed and a room is set, lists only that room's usage.");
						System.out.println("-d M;N;O = argument specifies the day/s to list usage for. Values (starting with Monday) are: M, T, W, R, F, S. Optional");
						System.out.println("-t S E = arguments specify the time range to list usage for (S=start, E=end). Time must be of the form [hours]:[mins][am/pm]. Optional");
					}
					else if(arg.equals("exit"))
					{
						System.out.println("Exits the program.");
					}
				}
				else if(input.startsWith("reload"))
				{
					data = reload(f);
				}
				else if(input.startsWith("set"))
				{
					Scanner s = new Scanner(input);
					s.next();
					String arg = "";
					if(s.hasNext())
						arg = s.next();
					else
					{
						System.err.println("ERROR: Exactly one argument is required. Type \'help set\' for help.");
						continue;
					}
					
					if(setBuilding.equals(""))
					{
						if(data.containsKey(arg))
							setBuilding = arg;
						else
							System.err.println("ERROR: " + arg + " is not a building.");
					}
					else
					{
						if(data.get(setBuilding).containsKey(arg))
							setRoom = arg;
						else
							System.err.println("ERROR: " + arg + " is not a room in " + setBuilding);
					}
				}
				else if(input.startsWith("clear"))
				{
					if(!setRoom.equals(""))
						setRoom = "";
					else
						setBuilding = "";
							
				}
				else if(input.startsWith("list"))
				{
					list(data, input, setBuilding);
				}
				else if(input.startsWith("usage"))
				{
					usage(data, input, setBuilding, setRoom);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param data
	 * @param input
	 */
	private static void usage(Map<String, Map<String, RoomUsageData>> data, String input, String setBuilding, String setRoom) {
		Scanner s = new Scanner(input);
		List<String> buildings = new ArrayList<String>();
		List<String> rooms = new ArrayList<String>();
		List<Day> days = new ArrayList<Day>();
		int start = 0;
		int end = 24*60 - 1;
		while(s.hasNext())
		{
			String command = s.next();
			if(command.equals("-b"))
			{
				if(!s.hasNext())
				{
					System.err.println("ERROR: -b requires an argument.");
					return;
				}
				String[] ls = s.next().split(";");
				for(String l : ls)
					buildings.add(l);
			}
			else if(command.equals("-r"))
			{
				if(!s.hasNext())
				{
					System.err.println("ERROR: -r requires an argument.");
					return;
				}
				String[] ls = s.next().split(";");
				for(String l : ls)
					rooms.add(l);
			}
			else if(command.equals("-d"))
			{
				if(!s.hasNext())
				{
					System.err.println("ERROR: -d requires an argument.");
					return;
				}
				String[] ls = s.next().split(";");
				for(String l : ls)
				{
					if(convertStringToDay(l) == null)
					{
						System.err.println("ERROR: " + l + " is not a valid day.");
						return;
					}
					days.add(convertStringToDay(l));
				}
			}
			else if(command.equals("-t"))
			{
				if(!s.hasNext())
				{
					System.err.println("ERROR: -t requires two arguments.");
					return;
				}
				String ts = s.next();
				if(!s.hasNext())
				{
					System.err.println("ERROR: -t requires two arguments.");
					return;
				}
				String te = s.next();
				
				start = convertCommonTimeToMintues(ts);
				end = convertCommonTimeToMintues(te);
				if(start < 0 || end < 0)
				{
					System.err.println("ERROR: improperly formatted time. Time should be [hours]:[mins][am/pm] (e.g. 12:30pm).");
					return;
				}
			}
		}
		
		if(buildings.isEmpty() && setBuilding.equals(""))
		{
			System.err.println("ERROR: No building set or given as an argument.");
			return;
		}
		else if(buildings.size() > 1 && rooms.size() > 0)
		{
			System.err.println("ERROR: More than one building given as an argument and room/s given as argument.");
			return;
		}
		if(start > end)
		{
			System.err.println("ERROR: The start time is after the end time.");
			return;
		}
		
		for(String building : buildings)
		{
			if(!data.containsKey(building))
			{
				System.err.println("ERROR: " + building + " is not a building.");
				return;
			}
		}
		
		if(buildings.isEmpty())
			buildings.add(setBuilding);
		
		for(String room : rooms)
		{
			if(!data.get(buildings.get(0)).containsKey(room))
			{
				System.err.println("ERROR: " + room + " is not a room in " + buildings.get(0) + ".");
				return;
			}
		}
		
		if(buildings.size() == 1 && !setRoom.equals(""))
			rooms.add(setRoom);
		
		if(days.isEmpty())
		{
			for(Day d : Day.values())
				days.add(d);
		}
		
		for(String building : buildings)
		{
			if(buildings.size() > 1 || rooms.size() <= 0)
			{
				rooms.clear();
				rooms.addAll(data.get(building).keySet());
			}
			
			for(String room : rooms)
			{
				//if there is just one day we're printing for, we want to print the room on the same line as the usage info
				//otherwise we want the room on a seperate line
				if(days.size() > 1)
					System.out.println("=================================" + building + " " + room);
				else
					System.out.print(building + " " + room + ": ");
					
				for(Day d : days)
				{
					if(days.size() > 1)
						System.out.print(d.toString() + ": ");
					System.out.println(data.get(building).get(room).getUsage(d, start, end));
				}
			}
		}
		
	}

	/**
	 * @param data
	 * @param input
	 * @param setBuilding
	 */
	private static void list(Map<String, Map<String, RoomUsageData>> data,
			String input, String setBuilding) {
		Scanner s = new Scanner(input);
		s.next();
		String arg = "";
		if(s.hasNext())
			arg = s.next();
		
		List<String> list = new ArrayList<String>();
		if(arg.equals(""))
		{
			if(setBuilding.equals("")) //list the buildings
				list.addAll(data.keySet());
			else
				list.addAll(data.get(setBuilding).keySet());
		}
		else
		{
			String[] bs = arg.split(";");
			for(String b : bs)
			{
				if(!data.containsKey(b))
				{
					System.err.println(b + " is not a building.");
					continue;
				}
				for(String r : data.get(b).keySet())
					list.add(b + " " + r);
			}
		}
		
		Collections.sort(list); //alphabetize the list
		for(String l : list)
			System.out.println(l);
	}

	/**
	 * @param f
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static Map<String, Map<String, RoomUsageData>> reload(File f)
			throws IOException, FileNotFoundException {
		Map<String, Map<String, RoomUsageData>> data;
		System.out.println("Contacting ISU's servers");
		data = downloadUsageData();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
		oos.writeObject(data);
		oos.flush();
		oos.close();
		System.out.println("Done.");
		return data;
	}
	
	private static Map<String, Map<String, RoomUsageData>> downloadUsageData() throws IOException
	{
		Map<String, Map<String, RoomUsageData>> data = new HashMap<String, Map<String, RoomUsageData>>();
		URL url = new URL("http://classes.iastate.edu/index.jsp");
		Scanner scan = new Scanner(new BufferedInputStream(url.openStream()));
		while(scan.hasNextLine() && !scan.nextLine().equals("<option selected=\"selected\" value=\"\">Select a Department</OPTION>")) {}
		
		while(scan.hasNextLine())
		{
			String line = scan.nextLine();
			if(!line.startsWith("<option value="))
				break;
			String dept = line.substring(15,20);
			dept = dept.replace(" ", "+");
			String address = "http://classes.iastate.edu/soc.jsp?term=S2013&dept="
								+ dept + "+&term2=S2013&dept2=" + dept 
								+ "+&course=&shour=06&sminute=00&sampm=am&ehour=11"
								+ "&eminute=55&eampm=pm&credit=+&instructor=&title="
								+ "&edreq=&spclcourse=&partterm=2006-01-012006-12-31"
								+ "&smonth=01&sday=01&emonth=12&eday=31"; 
					
//					"http://classes.iastate.edu/soc.jsp?term=F2012&dept="
//							+ dept + "&term2=S2013&dept2="
//							+ "&course=&shour=06&sminute=00&sampm=am&ehour=11&eminute=55"
//							+ "&eampm=pm&credit=+&instructor=&title=&edreq=&spclcourse="
//							+ "&partterm=2006-01-012006-12-31&smonth=01&sday=01&emonth=12&eday=31";
			parsePage(address, data);
		}
		
		return data;
	}
	
										//Building -> Room -> Room usage data
	private static void parsePage(String address, Map<String, Map<String, RoomUsageData>> data) throws IOException
	{
		URL url = new URL(address);
		Scanner scan = new Scanner(new BufferedInputStream(url.openStream()));
		
		String last = "";
		String current = "";
		while(scan.hasNext())
		{
			last = current;
			current = scan.next();
			//check to see if we've read the tag that always comes immediately before usage times
			if(last.equals("<td") && current.equals("align=\"left\">"))
			{
				List<Day> days = new ArrayList<Day>();
				//next check to see if the next word is a single character, indicating it is a day
				while(scan.hasNext())
				{
					last = current;
					current = scan.next();
					if(current.length() == 1) //its a day
						days.add(convertStringToDay(current));
					else //its not a day, so lets break to check to see if its a time
						break;
				}
				
				//check to see if the last thing we read was a time
				if(days.size() <= 0 || !(current.contains("am") || current.contains("pm")))
					continue; //not a valid usage time
				
				
				int start, end;
				//sometimes the two times are on seperate lines, and sometimes they are smashed together with a -
				if(current.contains("-"))
				{
					int hyphen = current.indexOf("-");
					start = convertCommonTimeToMintues(current.substring(0, hyphen));
					end = convertCommonTimeToMintues(current.substring(hyphen + 1));
				}
				else
				{
					start = convertCommonTimeToMintues(current);
					if(!scan.hasNext())
						continue;
					scan.next(); //scan away the hyphen
					if(!scan.hasNext())
						continue;
					end = convertCommonTimeToMintues(scan.next());
				}
				
				//next we have to parse out the building. It will look something like:
				//</td> <td align="left"> BUILDING ROOM </td>
				if(!scan.hasNext() || !scan.next().equals("</td>"))
					continue;
				if(!scan.hasNext() || !scan.next().equals("<td"))
					continue;
				if(!scan.hasNext() || !scan.next().equals("align=\"left\">"))
					continue;
				String building = "";
				current = "";
				while(scan.hasNext())
				{
					last = current;
					current = scan.next();
					
					if(current.equals("</td>"))
						break;
					
					building += last;
				}
				
				String room = last;
				
				//check to make sure we actually have a room and a building
				if(building.equals("") || room.equals(""))
					continue;
				
				if(data.get(building) == null)
					data.put(building, new HashMap<String, RoomUsageData>());
				if(data.get(building).get(room) == null)
					data.get(building).put(room, new RoomUsageData());
				RoomUsageData usage = data.get(building).get(room);
				for(Day d : days)
					usage.addTime(d, start, end);
			}
			
		}
		
	}
	
	private static Day convertStringToDay(String s)
	{
		switch(s.charAt(0))
		{
			case 'M':
				return Day.Monday;
			case 'T':
				return Day.Tuesday;
			case 'W':
				return Day.Wednesday;
			case 'R':
				return Day.Thursday;
			case 'F':
				return Day.Friday;
			case 'S':
				return Day.Saturday;
		}
		
		return null;
	}
	
	private static String convertMinutesToCommonTime(int time)
	{
		String m = "am";
		if(time >= 12*60)
		{
			m = "pm";
			if(time >= 13*60)
				time -= 12*60;
		}
		
		return ((int)time/60) + ":" + (time%60 < 10 ? "0" : "") + ((int)time%60) + m;
	}
	
	private static int convertCommonTimeToMintues(String time)
	{
		time = time.trim();
		if(!time.endsWith("am") && !time.endsWith("pm"))
			return -1;
		boolean pm = time.endsWith("pm");
		//remove the pm or am
		time = time.substring(0, time.length() - 2);
		if(!time.contains(":"))
			return -1;
		//remove the :
		time = time.replace(":", " ");
		Scanner scan = new Scanner(time);
		if(!scan.hasNextInt())
			return -1;
		int hours = scan.nextInt();
		if(!scan.hasNextInt())
			return -1;
		int min = scan.nextInt();
		
		if(hours <= 0 || hours > 12 || min < 0 || min >= 60)
			return -1;
		
		if(hours >= 12)
			hours -= 12;
		
		return (pm ? 12*60 : 0) + hours*60 + min;
	}

}
