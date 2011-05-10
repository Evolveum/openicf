import java.io.*;
import java.util.*;
import java.net.*;
import com.tivoli.pd.jutil.*;
import com.tivoli.pd.jadmin.*;
import com.tivoli.pd.nls.*;

class TAMTestPDJrte
{
	public static void main(String[] args) {

		// Some constants
		String LOCAL_LANG = "ENGLISH", LOCALEC = "US", PROGRAM_NAME = "TAMTestPDJrte";

		// Stuff...
		String userID = "", userPassword = "", configURLStr = "";
		URL configURL = null;
		Locale locale = null;
		PDMessages pdMsgs = null;
		PDContext pdContext = null;

		try
		{
			//Gotta put in some error checking for the args array before grabbing values
			userID = args[0];
			userPassword = args[1];
			configURLStr = args[2];

			
			configURL = new URL(configURLStr);
			locale = new Locale(LOCAL_LANG, LOCALEC);

			pdMsgs = new PDMessages();
			PDAdmin.initialize(PROGRAM_NAME, pdMsgs);
			pdContext = new PDContext(locale, userID, userPassword.toCharArray(), configURL);
		

			ArrayList alst = PDServer.listServers(pdContext, pdMsgs);

			if (alst.size() > 0)
			{
				int s = alst.size();
		
				for (int i = 0; i < s; i++)
				{
						String name = (String)alst.get(i);
						PDServer pdServer = new PDServer(pdContext, name, pdMsgs);

						System.out.println("Server Name: " + name.toString());
				}

			}
			else
			{
				System.out.println("No servers found.");
			}

			PDAdmin.shutdown(pdMsgs);

		}
		catch (MalformedURLException e)
		{
			System.out.println(e.toString());
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}

	}
}
