import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.sql.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Crawler
{
	Connection connection;
	int urlID;
	public Properties props;
	int nextURLIDScanned = 0;
	int nextURLID = 1;
	int max;

	Crawler() {
		urlID = 0;
	}

	public void readProperties() throws IOException {
      	props = new Properties();
      	FileInputStream in = new FileInputStream("database.properties");
      	props.load(in);
      	in.close();
	}

	public void openConnection() throws SQLException, IOException
	{
		String drivers = props.getProperty("jdbc.drivers");
      	if (drivers != null) System.setProperty("jdbc.drivers", drivers);

      	String url = props.getProperty("jdbc.url");
      	String username = props.getProperty("jdbc.username");
      	String password = props.getProperty("jdbc.password");

		connection = DriverManager.getConnection( url, username, password);
   	}

	public void createDB() throws SQLException, IOException {
		openConnection();

        Statement stat = connection.createStatement();
		
		// Delete the table first if any
		try {
			stat.executeUpdate("DROP TABLE URLS");
			stat.executeUpdate("DROP TABLE KEYWORD");
		}
		catch (Exception e) {
		}
			
		// Create the table
        stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200), title VARCHAR(500))");
        stat.executeUpdate("CREATE TABLE KEYWORD (word VARCHAR(100), urlid INT)");
	}

	public boolean urlInDB(String urlFound) throws SQLException, IOException {
        Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM urls WHERE url LIKE '"+urlFound+"'");

		if (result.next()) {
	        //System.out.println("URL "+urlFound+" already in DB");
			return true;
		}
	       // System.out.println("URL "+urlFound+" not yet in DB");
		return false;
	}

	public boolean insertURLInDB( String url) {
		String tmpQuery = "";
		String originalText = "";
		try {
        	Statement stat = connection.createStatement();
			Document doc = Jsoup.connect(url).get();
			originalText = doc.text();
			originalText = originalText.replaceAll("[^a-zA-Z0-9 ]", "");
			//System.out.println(originalText);
			processWords(originalText, urlID);

			String firstHundredChars = "";
			if (originalText.length() >= 100) {
				firstHundredChars = originalText.substring(0, 100);
			} else {
				firstHundredChars = originalText;
			}
		
			//System.out.println(firstHundredChars);
			//System.out.println(firstHundredChars.length());
	    	String query = "INSERT INTO urls VALUES ('"+urlID+"','"+url+"','"+firstHundredChars+"','"+doc.title().replaceAll("[^a-zA-Z0-9 ]", "")+"')";
	    	tmpQuery = query;
	   		stat.executeUpdate( query );
			urlID++;
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			//System.out.println(originalText);
			System.out.println("SQLException: " + tmpQuery);
			return false;
		} catch(IOException ee) {
			//ee.printStackTrace();
			//System.out.println("IOException: " + url);
		  	return false;
		}
	}

	public void processWords(String str, int urlID) {
		String[] words = str.split(" ");
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < words.length; i ++) {
			//System.out.println("++++++++++" + words[i] + "+++++++++++");
			if (words[i].equals("")) {
				continue;
			}
			if (!map.containsKey(words[i].toLowerCase())) {
				map.put(words[i].toLowerCase(), 1);
				String query = "INSERT INTO keyword VALUES ('"+words[i]+"',"+urlID+")" + ";";
				try {
					Statement stat = connection.createStatement();
					stat.executeUpdate(query);
				} catch(Exception e) {}
			} else {
				map.put(words[i].toLowerCase(), map.get(words[i].toLowerCase())+1);
			}
		}
	}

/*
	public String makeAbsoluteURL(String url, String parentURL) {
		if (url.indexOf(":")<0) {
			// the protocol part is already there.
			return url;
		}

		if (url.length > 0 && url.charAt(0) == '/') {
			// It starts with '/'. Add only host part.
			int posHost = url.indexOf("://");
			if (posHost <0) {
				return url;
			}
			int posAfterHist = url.indexOf("/", posHost+3);
			if (posAfterHist < 0) {
				posAfterHist = url.Length();
			}
			String hostPart = url.substring(0, posAfterHost);
			return hostPart + "/" + url;
		} 

		// URL start with a char different than "/"
		int pos = parentURL.lastIndexOf("/");
		int posHost = parentURL.indexOf("://");
		if (posHost <0) {
			return url;
		}
		
		
		

	}
*/

	public void fetchURLs(String urlScanned) {
		try {
			URL url = new URL(urlScanned);
			//System.out.println("urlscanned="+urlScanned+" url.path="+url.getPath());
 
    			// open reader for URL
    		InputStreamReader in = 
       			new InputStreamReader(url.openStream());

			// read contents into string builder
    		StringBuilder input = new StringBuilder();
    		int ch;
			while ((ch = in.read()) != -1) {
         			input.append((char) ch);
			}

     			// search for all occurrences of pattern
    		String patternString =  "<a\\s+href\\s*=\\s*(\"[^\"]*\"|[^\\s>]*)\\s*>";
    		Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    		Matcher matcher = pattern.matcher(input);

			while (matcher.find()) {
    			int start = matcher.start();
    			int end = matcher.end();
    			String match = input.substring(start, end);
				String urlFound = matcher.group(1);
				if (urlFound.startsWith("\"mailto")) {
					continue;
				}
				String new_URL = transformURL(urlScanned, urlFound);
				//System.out.println("URL transformed: " + new_URL);

				// Check if it is already in the database
				if (!urlInDB(new_URL) && new_URL.contains(this.props.getProperty("crawler.domain"))) {
					
					if (insertURLInDB(new_URL)) {
						//System.out.println(new_URL);
						nextURLID ++;
						if (nextURLID >= max) {
							return;
						}
					}
				}
 			}

		} catch (Exception e) {
			System.out.println("++++++++++++++++++++");
       		e.printStackTrace();
      	}
	}

	public String transformURL(String base_url, String url) {
		//System.out.println("urlScanned: " + base_url + " urlFound: " + url);
		url = url.substring(1, url.length()-1);

		if (url.charAt(0) == '/') {
			int i = 0;
			int cnt = 0;
			while (i < url.length()) {
				if (url.charAt(i) == '/') {
					cnt ++;
					if (cnt == 3) {
						break;
					}
				}
				i ++;
			}
			if (i < base_url.length())
				base_url = base_url.substring(0, i);
			url = url.substring(1);
		}

		if (url.length() == 0) {
			return base_url;
		}

		if (url.contains("..") || url.contains("./") || url.contains("/.")) {
			//if (url.contains("../.."))
				//System.out.println("urlScanned: " + base_url + " urlFound: " + url);
			Stack<String> stack = stringToStack(base_url);
			if (stack.size() > 1) {
				stack.pop();
			}
			while (!url.isEmpty()) {
				int i = url.indexOf('/');
				String tmp = "";
				if (i != -1) {
					tmp = url.substring(0, i);
					url = url.substring(i + 1);
				} else {
					tmp = url;
					url = "";
				}

				if (tmp.equals("..")) {
					stack.pop();
				} else if (tmp.equals(".")) {
				} else {
					stack.push(tmp);
				}
			}
			
			String result = restoreStack(stack);
			//System.out.println("transformed: " + result);
			return result.substring(1);
		}

		if (base_url.endsWith(".html")) {
			int i = base_url.length()-1;
			while (base_url.charAt(i) != '/') {
				i --;
			}
			base_url = base_url.substring(0, i);
		}

		if (url.charAt(url.length()-1) == '/') {
			url = url.substring(0, url.length()-1);
			if (url.equals("")) {
				return base_url;
			}
		}
		if (url.startsWith("http")) {
			return url;
		} else {
			if (url.startsWith("/")) {
				url = base_url + url;
			} else {
				url = base_url + "/" + url;
			}
			return url;
		}
	}

	public Stack<String> stringToStack(String url_str) {
		String[] paths = url_str.split("/");
		Stack<String> stack = new Stack<String>();
		for (String path : paths) {
			stack.push(path);
		}
		return stack;
	}

	public String restoreStack(Stack<String> stack) {
		String result = "";
		while (!stack.isEmpty()) {
			result = "/" + stack.pop() + result;
		}
		return result;
	}

	public void crawl() {
		while (nextURLIDScanned < nextURLID && nextURLID < max) {
			int urlIndex = nextURLIDScanned;
			String link = fetchURLfromDB(urlIndex);
			//link = "https://fdsfdfsf";
			fetchURLs(link);
			nextURLIDScanned ++;
		}
	}

	public String fetchURLfromDB(int id) {
		try {
			Statement stat = connection.createStatement();
			ResultSet result = stat.executeQuery( "SELECT * FROM urls WHERE urlid = " + id + ";");

			if (result.next()) {
	        	String data = result.getString("url");
	   			return data;
			} else {
				return null;
			}
		} catch(Exception e) {
			return "";
		}
	}

   	public static void main(String[] args)
   	{
		Crawler crawler = new Crawler();

		try {
//			String str = "Gene Spafford's Personal Pages: Spaf's Home Page Spaf's Home , . Page Links";
//			System.out.println(str.replaceAll("[^a-zA-Z0-9]", " "));
			
			crawler.readProperties();
			String root = crawler.props.getProperty("crawler.root");
			crawler.max = Integer.parseInt(crawler.props.getProperty("crawler.maxurls"));
			crawler.createDB();
			if (crawler.insertURLInDB(root)) {
				crawler.crawl();	
			}

			//Document doc = Jsoup.connect("https://www.cs.purdue.edu").get();
			//System.out.println("DOC: " + doc.text());
		}
		catch( Exception e) {
			System.out.println("---------------------------");
         	e.printStackTrace();
		}


		
    }
}