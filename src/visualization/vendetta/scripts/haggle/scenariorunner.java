// XML stuff
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;

// File stuff
import java.io.*;

// ArrayList...:
import java.util.*;

// Threading:
import java.lang.Thread;
import java.lang.Runnable;

// GUI stuff:
import javax.swing.*;
import java.awt.*;

public class scenariorunner implements Runnable
{
	private static class action 
	{
		public long timestamp;
		public String cmd;

		public action(long timestamp, String cmd)
		{
			this.timestamp = timestamp;
			this.cmd = cmd;
		}
	}
	
	private static class actionComparator implements Comparator<action>
	{
		public final int compare ( action a, action b )
		{
			return (int) (a.timestamp - b.timestamp);
		}
	}
	
	private static class linkTuple
	{
		public int firstNode;
		public int secondNode;
		public int startTime;
		public int stopTime;
		
		public linkTuple(
			int _firstNode, 
			int _secondNode,
			int _startTime,
			int _stopTime)
		{
			firstNode = _firstNode;
			secondNode = _secondNode;
			startTime = _startTime;
			stopTime = _stopTime;
		}
	}
	
	static linkTuple[] parseLinkTuples(String[] trace)
	{
		linkTuple[]	tmp, retval;
		int			i, j;
		
		tmp = new linkTuple[trace.length];
		j = 0;
		for(i = 0; i < trace.length; i++)
		{
			String[] brokenDown = trace[i].split("\\s+");
			if(brokenDown.length >= 4)
			{
				tmp[j] = 
					new linkTuple(
						Integer.parseInt(brokenDown[0]),
						Integer.parseInt(brokenDown[1]),
						Integer.parseInt(brokenDown[2]),
						Integer.parseInt(brokenDown[3]));
				j++;
			}
		}
		
		if(j != trace.length)
		{
			retval = new linkTuple[j];
			for(i = 0; i < j; i++)
				retval[i] = tmp[i];
		}else
			retval = tmp;
		return retval;
	}
	
	static linkTuple[] preprocess(linkTuple[] tuple)
	{
		if(tuple.length == 0)
			return tuple;
		
		int						i,j,a,b, minNode, maxNode;
		int					minTime, maxTime, startTime, stopTime;
		int[]					connectivityMap;
		ArrayList<linkTuple>	retval = new ArrayList<linkTuple>();
		
		minTime = tuple[0].startTime;
		maxTime = tuple[0].startTime;
		minNode = tuple[0].firstNode;
		maxNode = tuple[0].firstNode;
		
		for(i = 0; i < tuple.length; i++)
		{
			if(minTime > tuple[i].startTime)
				minTime = tuple[i].startTime;
			if(minTime > tuple[i].stopTime)
				minTime = tuple[i].stopTime;
			if(maxTime < tuple[i].startTime)
				maxTime = tuple[i].startTime;
			if(maxTime < tuple[i].stopTime)
				maxTime = tuple[i].stopTime;
			
			if(minNode > tuple[i].firstNode)
				minNode = tuple[i].firstNode;
			if(minNode > tuple[i].secondNode)
				minNode = tuple[i].secondNode;
			if(maxNode < tuple[i].firstNode)
				maxNode = tuple[i].firstNode;
			if(maxNode < tuple[i].secondNode)
				maxNode = tuple[i].secondNode;
		}
		
		connectivityMap = new int[maxTime - minTime + 1];
		
		for(a = minNode; a <= maxNode; a++)
			for(b = a+1; b <= maxNode; b++)
			{
				for(i = 0; i < connectivityMap.length; i++)
					connectivityMap[i] = 0;
				
				for(j = 0; j < tuple.length; j++)
				{
					if(	tuple[j].firstNode == a &&
						tuple[j].secondNode == b)
					{
						for(i = tuple[j].startTime; i < tuple[j].stopTime; i++)
							connectivityMap[i - minTime] = 
								connectivityMap[i - minTime] | 1;
					}
					if(	tuple[j].firstNode == b &&
						tuple[j].secondNode == a)
					{
						for(i = tuple[j].startTime; i < tuple[j].stopTime; i++)
							connectivityMap[i - minTime] = 
								connectivityMap[i - minTime] | 2;
					}
				}
				
				// FIXME: call other function to do different things here...
				
				// j is the value of the previous element in the connectivity 
				// map.
				j = 0;
				startTime = 0;
				stopTime = 0;
				for(i = 0; i < connectivityMap.length; i++)
				{
					if(j == 0 && connectivityMap[i] != 0)
					{
						startTime = i + minTime;
						j = connectivityMap[i];
					}
					if(j != 0 && connectivityMap[i] == 0)
					{
						stopTime = i + minTime;
						j = connectivityMap[i];
						retval.add(new linkTuple(a,b,startTime,stopTime));
					}
				}
				if(j != 0)
				{
					stopTime = connectivityMap.length + minTime;
					retval.add(new linkTuple(a,b,startTime,stopTime));
				}
			}
		
		return retval.toArray(new linkTuple[0]);
	}
	
	public static Node parseXML(String xml)
	{
		try{
			InputSource iSource = new InputSource(new StringReader(xml));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(iSource);
			doc.getDocumentElement().normalize();
			return (Node) doc.getDocumentElement();
		}catch(Exception e){
		}
		return (Node) null;
	}
	
	public static String getTagContent(Node node)
	{
		if(node != null)
		{
			NodeList fstNm = ((Element) node).getChildNodes();
			if(fstNm != null)
			{
			if(fstNm.getLength() > 0)
			{
				return ((Node) fstNm.item(0)).getNodeValue();
			}
			}
		}
		return null;
	}
	
	public static String getTagContent(Node node, String element_name, int num)
	{
		if(node != null)
		{
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
			NodeList nodes = getSubTags(node, element_name);
			if(nodes != null)
			{
				Element subElmnt = (Element) nodes.item(num);
				if(subElmnt != null)
				{
				NodeList fstNm = subElmnt.getChildNodes();
				if(fstNm != null)
				{
					if(fstNm.getLength() > 0)
					{
					return ((Node) fstNm.item(0)).getNodeValue();
					}
				}
				}
			}
			}
		}
		return null;
	}
	
	public static String getTagContent(Node node, String element_name)
	{
		return getTagContent(node, element_name, 0);
	}
	
	public static Node getSubTag(Node node, String element_name, int num)
	{
		if(node.getNodeType() == Node.ELEMENT_NODE)
		{
			// FIXME: figure out a faster way to create an empty node:
			Node tmp = node.cloneNode(false);
			while(tmp.hasChildNodes())
			tmp.removeChild(tmp.getFirstChild());
				
			NodeList children = node.getChildNodes();
			int i, j;
				
			j = 0;
			for(i = 0; i < children.getLength(); i++)
			if(children.item(i).getNodeName().equals(element_name))
			{
				if(j == num)
				return children.item(i);
				j++;
			}
		}
		return null;
	}
	
	public static Node getSubTag(Node node, String element_name)
	{
		return getSubTag(node, element_name, 0);
	}
	
	public static NodeList getSubTags(Node node, String element_name)
	{
		if(node.getNodeType() == Node.ELEMENT_NODE)
		{
			// FIXME: figure out a faster way to create an empty node:
			Node tmp = node.cloneNode(false);
			while(tmp.hasChildNodes())
			tmp.removeChild(tmp.getFirstChild());
			
			NodeList children = node.getChildNodes();
			int i;
			
			for(i = 0; i < children.getLength(); i++)
			if(children.item(i).getNodeName().equals(element_name))
				tmp.appendChild(children.item(i));
				
			return tmp.getChildNodes();
		}
		return null;
	}
	
	static public String getContents(String filename) 
	{
		File aFile = new File(filename);
		StringBuilder contents = new StringBuilder();
		
		try {
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
			String line = null;
			while((line = input.readLine()) != null)
			{
				contents.append(line);
				contents.append(System.getProperty("line.separator"));
			}
			}
			finally {
			input.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		
		return contents.toString();
	}
	
	static public String[] getLinesOfFile(String filename)
	{
		File aFile = new File(filename);
		ArrayList<String> lines = new ArrayList<String>();

		try {
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
			String line = null;
			while((line = input.readLine()) != null)
			{
				String lineWithoutComment = line.split("#")[0];
				if(!lineWithoutComment.equals(""))
				lines.add(lineWithoutComment);
			}
			}
			finally {
			input.close();
			}
			return lines.toArray(new String[0]);
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	static public int system(String cmd)
	{
		//System.out.println("Executing: " + cmd);
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedWriter stdIn;
			stdIn = 
				new BufferedWriter(
					new OutputStreamWriter(
						p.getOutputStream()));
			try {
			stdIn.close();
			p.waitFor();
			return p.exitValue();
			}catch (Exception e) {
			}
			p.waitFor();
			return p.exitValue();
		}catch(Exception e){
		}
		return -1;
	}
	
	static public Process popen(String cmd)
	{
		//System.out.println("Executing: " + cmd);
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			return p;
		}catch(Exception e){
		}
		return null;
	}
	
	public static String pathFromFileName(String filename)
	{
		int i;
		String path = "";
		String[] path_element = filename.split("/");
		if(path_element != null)
			for(i = 0; i < path_element.length-1; i++)
			{
			if(!path_element[i].equals(""))
				path += "/" + path_element[i];
			}
		path += "/";
		return path;
	}
	
	public static String getFileName(String filename)
	{
		String[] split = filename.split("/");
		if(split == null)
			return "";
		if(split.length < 1)
			return "";
		return split[split.length-1];
	}
	
	public static int mySystem(String cmd)
	{
		if(controller)
			cmd = "do_quiet.sh " + controllerIP + " 9797 " + cmd;
		return system("./" + cmd);
	}
	
	public static int upload(String filename)
	{
		if(controller)
		{
			String cmd;
			Process p;
			BufferedWriter stdIn;
			String fileContents;
			
			cmd = "do.sh " + controllerIP + " 9797 " + "upload " + filename;
			p = popen(cmd);
			if(p != null)
			{
				stdIn = 
					new BufferedWriter(
						new OutputStreamWriter(
							p.getOutputStream()));
				fileContents = getContents(filename);
				try {
				stdIn.write(fileContents, 0, fileContents.length());
				stdIn.close();
				p.waitFor();
				return p.exitValue();
				}catch (Exception e) {
				}
			}
		}
		return -1;
	}

	// Is a controller used?
	public static boolean controller;
	public static String controllerIP;
	public static String[] main_args;

	public static void main_run(String[] args)
	{
		int i;
		Date now = new Date();
		
		// input argument check
		if(args == null)
			return;
		if(args.length < 1)
		{
			System.out.println("Not enough input arguments.");
			return;
		}
		
		// Scenario file name:
		String scenario_file_name = null;
		// Is a controller used?
		controller = false;
		controllerIP = null;

		for(i = 0; i < args.length; i++)
		{
			if(args[i].equals("--run-with-controller"))
			{
				controller = true;
				if(args.length >= i+1)
				{
					controllerIP = args[i+1];
					i++;
				}
			}else{
				scenario_file_name = args[i];
			}
		}
		if(scenario_file_name == null)
		{
			System.out.println("No scenario file specified.");
			return;
		}
		
		// FIXME: Check that it is an actual address.
		if(controller && controllerIP == null)
		{
			System.out.println("No IP address specified.");
			return;
		}
		
		// read and parse scenario:
		Node scenario_file = parseXML(getContents(scenario_file_name));
		if(scenario_file == null)
		{
			System.out.println(
				"Unable to load scenario file " + scenario_file_name + ".");
			return;
		}
		
		parseScenario_ok = true;
		if(cancelButton_pressed)
			return;
		// Check "magic" string:
		// (See "man file" for why it's called "magic".
		String magic = getTagContent(scenario_file, "magic");
		if(magic == null)
		{
			System.out.println("No magic tag!");
			return;
		}
		if(!magic.equals("haggle"))
		{
			System.out.println("Magic tag wrong!");
			return;
		}
		
		magicTag_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Get scenario name:
		String scenario_name = getTagContent(scenario_file, "name");
		
		// Figure out the base path for all files:
		String scenario_path = 
			pathFromFileName(scenario_file_name);
		
		// FIXME: check!
		scenarioFile_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Get the number of nodes in this scenario:
		/*
		int nodeCount = 0;
		{
			String tmp;
			tmp = getTagContent(scenario_file, "nodecount");
			if(tmp != null)
			nodeCount = Integer.parseInt(tmp);
		}
		if(nodeCount == 0)
			return;
		*/
		nodeCount_ok = true;
		if(cancelButton_pressed)
			return;
		
		ArrayList<action> actions = new ArrayList<action>();
		int node1 = 0;
		int node2 = 0;
		int nodeCount = 0;
		// Get the trace file name:
		String traceFile = getTagContent(scenario_file, "tracefile");
		if(traceFile == null)
		{
			System.out.println("TRACEFILE!");
			return;
		}else{
			readTraceFile_ok = true;
			if(cancelButton_pressed)
				return;
			traceFile = scenario_path + traceFile;
			
			// Go through the trace and create actions for the events in it:
			String[] trace = getLinesOfFile(traceFile);
			
			if(trace != null)
			{
				
				linkTuple[]	tuple = parseLinkTuples(trace);
				
				int minNode = tuple[0].firstNode;	
				int maxNode = tuple[0].firstNode;
				for(i = 0; i < tuple.length; i++)
				{
					if(tuple[i].firstNode < minNode)
						minNode = tuple[i].firstNode;
					if(tuple[i].secondNode < minNode)
						minNode = tuple[i].firstNode;
					if(tuple[i].firstNode > maxNode)
						maxNode = tuple[i].firstNode;
					if(tuple[i].secondNode > maxNode)
						maxNode = tuple[i].firstNode;
				}
				nodeCount = (maxNode - minNode) + 1;
				// FIXME: preprocess:
				tuple = preprocess(tuple);
				
				for(i = 0; i < tuple.length; i++)
				{
					if(tuple[i] != null)
					{
						if(	tuple[i].firstNode <= maxNode && 
							tuple[i].secondNode <= maxNode)
							if(tuple[i].firstNode < tuple[i].secondNode)
							{
								// In the trace, the nodes are called 1, 2, 3, 
								// but in the testbed, the nodes are called 
								// 0, 1, 2:
								String cmdUp = 
									"linkup.sh node-" + 
									(tuple[i].firstNode-minNode) + 
									" node-" + 
									(tuple[i].secondNode-minNode);
								String cmdDown = 
									"linkdown.sh node-" +
									(tuple[i].firstNode-minNode) +
									" node-" +
									(tuple[i].secondNode-minNode);
								
								actions.add(
									new action(
										tuple[i].startTime, 
										cmdUp));
								actions.add(
									new action(
										tuple[i].stopTime, 
										cmdDown));
							}
					}
				}
			}else{
				System.out.println(
					"Unable to load trace file " + traceFile + ".");
				return;
			}
		}
		parseTraceFile_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Get the data object list file name:
		String doListFile = getTagContent(scenario_file, "dolist");
		if(doListFile != null)
		{
			doListFile = scenario_path + doListFile;
		
			// Go through the data object insertion list and create actions:
			String[] doList = getLinesOfFile(doListFile);
			
			if(doList != null)
			{
				for(i = 0; i < doList.length; i++)
				{
					String[] value = doList[i].split("\t");
					
					if(value.length >= 4)
					{
						
					}
				}
			}else{
				System.out.println(
					"Unable to load data object list file " + doListFile + ".");
				return;
			}
		}
		readDOList_ok = true;
		if(cancelButton_pressed)
			return;
		// FIXME: Shutdown any running Haggle.

		// Make sure that all logs are cleared.
		for(i = 0; i < nodeCount; i++)
		{
			mySystem("remove_logs.sh node-" + i);
		}
		
		// Create execution log file.
		File oFile = new File(now.getTime() + ".log");
		PrintStream output = null;
		try {
			output = new PrintStream(new FileOutputStream(oFile));
		}
		catch (IOException ex){
			ex.printStackTrace();
		}

		clearNodes_ok = true;
		if(cancelButton_pressed)
			return;

		// Make sure they all started!
		if(mySystem("check_nodes.sh " + nodeCount) != 0)
		{
			System.out.println("Check nodes failed!");
			return;
		}
		
		checkNodes_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Initialize filters.
		if(mySystem("initfilter.sh") != 0)
		{
			System.out.println("Init filters failed!");
			return;
		}
		
		initFilter_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Start haggle on each node:
		for(i = 0; i < nodeCount; i++)
		{
			mySystem(
				"start_program_on_node.sh" +
				" node-" + i +
				" haggle" +
				" \"--non-interactive\"");
		}
		
		startHaggle_ok = true;
		if(cancelButton_pressed)
			return;
		// FIXME: make sure haggle started!
		
		// Wait until haggle has initialized.
		try
		{
			Thread.sleep(15000);
		}catch(Exception e){}
		// Get the file name/path of the application to start:
		String appName = getTagContent(scenario_file, "Application");
		if(appName != null)
		{
			// Start the application on all nodes
			for(i = 0; i < nodeCount; i++)
			{
				mySystem(
					"start_program_on_node.sh" +
					" node-" + i + " " + appName);
			}
		}
		
		startApplication_ok = true;
		if(cancelButton_pressed)
			return;
		// FIXME: make sure the application started!
		
		// Wait until application has initialized.
		try
		{
			Thread.sleep(15000);
		}catch(Exception e){}

		// Sort the action list on time of execution.
		Collections.sort(actions, new actionComparator());
		
		// An array is easier to access:
		action[] event = actions.toArray(new action[0]);

		// Run the scenario:
		if(event.length > 0)
		{
			runScenarioBar_max = (int) event[event.length-1].timestamp;
			long done;
			long start = done = new Date().getTime();
			output.println("Real Start time:" + start);
			for(i = 0; i < event.length; i++)
			{
				long time_to_sleep = (start + event[i].timestamp*1000) - done;
				if(time_to_sleep > 0)
				{
					try{
						Thread.sleep(time_to_sleep);
					}catch(Exception e){}
				}
				mySystem(event[i].cmd);
				done = new Date().getTime();
				output.println(event[i].cmd + " " + done);
				runScenarioBar_value = (int) event[i].timestamp;
			}
		}
		runScenario_ok = true;
		if(cancelButton_pressed)
			return;

		output.close();

		try
		{
			Thread.sleep(15000);
		}catch(Exception e){}

		// Stop application (if there was one)
		if(appName != null)
		{
			for(i = 0; i < nodeCount; i++)
			{
				mySystem(
					"stop_program_on_node.sh" + 
					" node-" + i + " " + appName);
			}
			
			// The reason for not placing the two ssh commands in one is to 
			// make the nodes shut down faster by allowing them to shut down 
			// simultaneously.
			
			// Make sure all nodes have stopped running the application:
			for(i = 0; i < nodeCount; i++)
			{
				mySystem(
					"wait_for_app_to_stop.sh" +
					" node-" + i + " " + appName);
			}
		}
		try
		{
			Thread.sleep(15000);
		}catch(Exception e){}
		stopApplication_ok = true;
		if(cancelButton_pressed)
			return;
	
		// Stop haggle:
		for(i = 0; i < nodeCount; i++)
		{
			mySystem(
				"stop_program_on_node.sh" +
				" node-" + i + " haggle");
		}
		
		// The reason for not placing the two ssh commands in one is to make
		// the nodes shut down faster by allowing them to shut down 
		// simultaneously.

		// Make sure all nodes have stopped running haggle:
		for(i = 0; i < nodeCount; i++)
		{
			mySystem(
				"wait_for_app_to_stop.sh" +
				" node-" + i + " haggle");
		}
		stopHaggle_ok = true;
		if(cancelButton_pressed)
			return;
		
		// Upload execution log: (FIXME: TEMPORARY NAME!)
//		upload(now.getTime() + ".log");
		// download all logs etc:
		for(i = 0; i < nodeCount; i++)
		{
			mySystem("download_logs.sh node-" + i);
		}

		
		// tarball the result:
		mySystem("save_logs.sh " + nodeCount + " " + getFileName(scenario_file_name));
		saveLogs_ok = true;
		if(cancelButton_pressed)
			return;
		

		// clear all logs etc:
		for(i = 0; i < nodeCount; i++)
		{
			mySystem("remove_logs.sh node-" + i);
		}
		removeLogs_ok = true;

//		system("rm " + now.getTime() + ".log");

		if(cancelButton_pressed)
			return;
	}
	
	private static boolean	is_running_with_gui;
	private static boolean	should_dump_to_log;
	private static boolean	should_dump_to_log_occasionally;
	private static boolean	is_finished;
	
	public void run()
	{
		main_run(main_args);
		
		// FIXME: enable "OK" button or whatever.
		
		// Tell the main thread that we're done:
		is_finished = true;
	}
	
	public static JCheckBox makeNewCheckbox(JPanel inPanel, String title)
	{
		JCheckBox	retval;
		retval = new JCheckBox(title);
		retval.setSelected(false);
		retval.setEnabled(false);
		inPanel.add(retval);
		return retval;
	}
	
	static private class boxAndBar {
		public JCheckBox box;
		public JProgressBar bar;
		boxAndBar(JCheckBox _box, JProgressBar _bar)
		{
			box = _box;
			bar = _bar;
		}
	};
	
	public static boxAndBar makeNewCheckboxAndProgressBar(JPanel inPanel, String title)
	{
		JCheckBox		box;
		JProgressBar	bar;
		JPanel			panel;
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		
		box = new JCheckBox(title);
		box.setSelected(false);
		box.setEnabled(false);
		
		bar = new JProgressBar(0, 0);
		bar.setValue(0);
		bar.setEnabled(true);
		// FIXME: set indeterminate state:
		//bar.set...
		
		panel.add(box);
		panel.add(bar);
		panel.add(Box.createHorizontalGlue());
		
		inPanel.add(panel);
		
		return new boxAndBar(box, bar);
	}
	
	public static JButton makeNewButton(
							JPanel inPanel, 
							String title, 
							boolean enabled,
							boolean isDefault)
	{
		JButton	retval;
		retval = new JButton(title);
		retval.setSelected(false);
		retval.setEnabled(enabled);
		retval.setDefaultCapable(isDefault);
		inPanel.add(retval);
		return retval;
	}
	
	public static boolean
			parseScenario_ok,
			magicTag_ok,
			scenarioFile_ok,
			nodeCount_ok,
			readTraceFile_ok,
			parseTraceFile_ok,
			readDOList_ok,
			clearNodes_ok,
			checkNodes_ok,
			initFilter_ok,
			startHaggle_ok,
			startApplication_ok,
			runScenario_ok,
			stopApplication_ok,
			stopHaggle_ok,
			saveLogs_ok,
			removeLogs_ok,
			okButton_pressed,
			cancelButton_pressed;
	public static int
			runScenarioBar_max,
			runScenarioBar_value;
	
	public static void main(String[] args)
	{
		JFrame		theFrame = null;
		JCheckBox	
			parseScenario = null,
			magicTag = null,
			scenarioFile = null,
			nodeCount = null,
			readTraceFile = null,
			parseTraceFile = null,
			readDOList = null,
			clearNodes = null,
			checkNodes = null,
			initFilter = null,
			startHaggle = null,
			startApplication = null,
			runScenario = null,
			stopApplication = null,
			stopHaggle = null,
			saveLogs = null,
			removeLogs = null;
		JButton
			okButton = null,
			cancelButton = null;
		JProgressBar
			runScenarioBar = null;
		
		main_args = args;
		is_finished = false;
		
		parseScenario_ok = false;
		magicTag_ok = false;
		scenarioFile_ok = false;
		nodeCount_ok = false;
		readTraceFile_ok = false;
		parseTraceFile_ok = false;
		readDOList_ok = false;
		clearNodes_ok = false;
		checkNodes_ok = false;
		initFilter_ok = false;
		startHaggle_ok = false;
		startApplication_ok = false;
		runScenario_ok = false;
		stopApplication_ok = false;
		stopHaggle_ok = false;
		saveLogs_ok = false;
		removeLogs_ok = false;
		okButton_pressed = false;
		cancelButton_pressed = false;
		runScenarioBar_max = 0;
		runScenarioBar_value = 0;
		
		// FIXME: are we running with a GUI, etc.?
		is_running_with_gui = false;
		should_dump_to_log = false;
		should_dump_to_log_occasionally = false;
		
		if(is_running_with_gui)
		{
			// Set up GUI:
			JPanel topPanel = null;
			JPanel resultPanel = null;
			JPanel buttonPanel = null;
			
			theFrame = new JFrame("Scenario progress");
			if(theFrame == null)
				return;
			
			resultPanel = new JPanel(new GridLayout(0,1));
			
			parseScenario = 
				makeNewCheckbox(resultPanel, "Load scenario description");
			magicTag = 
				makeNewCheckbox(resultPanel, "Magic tag check");
			scenarioFile = 
				makeNewCheckbox(resultPanel, "Load scenario file");
			nodeCount = 
				makeNewCheckbox(resultPanel, "Count nodes");
			readTraceFile = 
				makeNewCheckbox(resultPanel, "Read trace file");
			parseTraceFile = 
				makeNewCheckbox(resultPanel, "Parse trace file");
			readDOList = 
				makeNewCheckbox(resultPanel, "Read data object list");
			clearNodes = 
				makeNewCheckbox(resultPanel, "Clear nodes");
			checkNodes = 
				makeNewCheckbox(resultPanel, "Check nodes");
			initFilter = 
				makeNewCheckbox(resultPanel, "Init filters");
			startHaggle = 
				makeNewCheckbox(resultPanel, "Start haggle");
			startApplication = 
				makeNewCheckbox(resultPanel, "Start application");
			boxAndBar bb = 
				makeNewCheckboxAndProgressBar(resultPanel, "Run scenario: ");
			runScenario = bb.box;
			runScenarioBar = bb.bar;
			stopApplication = 
				makeNewCheckbox(resultPanel, "Stop application");
			stopHaggle = 
				makeNewCheckbox(resultPanel, "Stop haggle");
			saveLogs = 
				makeNewCheckbox(resultPanel, "Save logs");
			removeLogs = 
				makeNewCheckbox(resultPanel, "Remove logs");
			
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
			
			buttonPanel.add(Box.createHorizontalGlue());
			cancelButton = makeNewButton(buttonPanel, "Cancel", true, false);
			okButton = makeNewButton(buttonPanel, "OK", false, true);
			
			resultPanel.add(buttonPanel);
			theFrame.setContentPane(resultPanel);
			resultPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
			theFrame.getRootPane().setDefaultButton(okButton);
			
			theFrame.setResizable(false);
			theFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			theFrame.pack();
			theFrame.setLocationRelativeTo(null);
			
			// Show the GUI:
			theFrame.setVisible(true);
		}
		
		if(should_dump_to_log)
		{
			// FIXME: set up to write to log.
		}
		
		// Start running the test:
		Thread thread = new Thread(new scenariorunner());
		if(thread != null)
			thread.start();
		else{
			is_finished = true;
			// FIXME: note that the thread wouldn't start!
		}
		
		while(!is_finished)
		{
			try{
				Thread.sleep(1000);
			}catch(Exception e){}
			
			if(is_running_with_gui)
			{
				// Update gui
				
				parseScenario.setSelected(parseScenario_ok);
				magicTag.setSelected(magicTag_ok);
				scenarioFile.setSelected(scenarioFile_ok);
				nodeCount.setSelected(nodeCount_ok);
				readTraceFile.setSelected(readTraceFile_ok);
				parseTraceFile.setSelected(parseTraceFile_ok);
				readDOList.setSelected(readDOList_ok);
				clearNodes.setSelected(clearNodes_ok);
				checkNodes.setSelected(checkNodes_ok);
				initFilter.setSelected(initFilter_ok);
				startHaggle.setSelected(startHaggle_ok);
				startApplication.setSelected(startApplication_ok);
				runScenario.setSelected(runScenario_ok);
				if(runScenarioBar_max > 0)
				{
					runScenarioBar.setMaximum(runScenarioBar_max);
					runScenarioBar.setValue(runScenarioBar_value);
				}
				stopApplication.setSelected(stopApplication_ok);
				stopHaggle.setSelected(stopHaggle_ok);
				saveLogs.setSelected(saveLogs_ok);
				removeLogs.setSelected(removeLogs_ok);
			}
			
			if(	should_dump_to_log_occasionally || 
				(is_finished && should_dump_to_log))
			{
				// FIXME: dump to file.
			}
			
			// FIXME: did the user press cancel?
		}
		
		if(is_running_with_gui)
		{
			okButton.setEnabled(true);
			
			// FIXME: Wait for OK button to be pressed
		}
	}
}
