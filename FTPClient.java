   import java.awt.*;
   import java.awt.event.*;
   import java.net.*;
   import java.io.*;
   import java.util.*;
   import java.security.*;

   public class FTPClient implements Runnable 
   {
		/**
		* The place the connection between the client
		* and server is made
		*/
      private Socket connection;
   	/**
   	* <code> Stream </code> reading from the server
   	*/
      private DataInputStream input;
   	/**
   	* <code> Stream </code> writing to the server
   	*/
      public DataOutputStream output;
		/**
		* <code> Thread </code> that continuously reads
		* in from the server.
		*/
      private Thread inputThread;
     	
		/**
		* Instantiate various variables and start the client
		*/
      public static void main(String[] args) throws IOException
      {
         System.out.print("Connect to: ");
         String stuff=new Scanner(System.in).next();
         String hostname=null;
         int port=5000;
         try{hostname=stuff.substring(0, stuff.indexOf(":"));
            port=Integer.parseInt(stuff.substring(stuff.indexOf(":")+1));}
            catch(StringIndexOutOfBoundsException e){hostname=stuff;}
         (new FTPClient()).gogogo(hostname, port);
      }
	/**
   * Make connection to server and get associated streams.
   * Start separate thread to allow the client to
   * continually read input from the server.
	
	* @param host The address of the <code> FTPServer </code>
	* @param port The port the server is being run on
	*/
      public void gogogo(String host, int port) throws IOException
      {
         try {
            connection = new Socket(InetAddress.getByName(host), port );
            input = new DataInputStream(connection.getInputStream() );
            output = new DataOutputStream(connection.getOutputStream() );
         }
            catch(ConnectException e)
            {
               System.out.println("Connection refused by "+host+" at port "+port+".");
               System.exit(1);
            }
            catch(UnknownHostException e)
            {
               System.out.println("Could not find "+host+".");
               System.exit(1);
            }
            catch ( IOException e ) {
               e.printStackTrace();         
            }
         Scanner scan=new Scanner(System.in);
      
         if(input.readBoolean())
         {
            System.out.println("This server requires that you authenticate yourself.");
            char[]pass = null;
            try {
               pass = PasswordField.getPassword(System.in, "Enter password: ");
            } 
               catch(IOException e){e.printStackTrace();}
         	
            System.out.print("\r");
            if(pass==null)
            {
               System.out.println("Incorrect password.  Connection refused.");
               System.exit(0);
            }
            output.writeUTF(hash(pass));
            if(input.readBoolean())
            {
               System.out.println("Incorrect password.  Connection refused.");
               System.exit(0);
            }
         }
      
         inputThread = new Thread( this );
         inputThread.start();
			//Writes to the server
         while(true)
         {
            try{String request=scan.nextLine().trim();
               if(request.equalsIgnoreCase("exit")||request.equalsIgnoreCase("quit")||request.equalsIgnoreCase("bye")||request.equalsIgnoreCase("close"))
                  System.exit(0);
               output.writeUTF(request);
            }
               catch(NoSuchElementException e){System.exit(0);}
         }
      }
   /**
   * Control thread that allows continuous update of the
   * text area display.
   */
      public void run()
      {      
         while ( true ) 
         {
            try {
               String text=input.readUTF();
               try
               {
                  if(text.substring(0, 5).equals("<EOF>"))	//current directory
                     System.out.print(text.substring(5)+"> ");
                  else if(text.substring(0, 6).equals("<FILE>"))	//file is being transfered
                     recieveFile(text);
						else if(text.substring(0, 8).equals("<FOLDER>"))
							 new File(text.substring(text.indexOf(">")+1)).mkdir();
                  else
                     System.out.println(text);
               }
                  catch(StringIndexOutOfBoundsException e){System.out.println(text);}
						catch(Exception e){e.printStackTrace();}
            }
               catch(SocketException e){
                  System.out.println("Server closed by host.");
                  System.exit(0);}
               catch(EOFException e){
                  System.out.println("Server closed by host.");
                  System.exit(0);}
               catch ( IOException e ) {
                  e.printStackTrace();         
               }
            
         }
      }
		/**
		* Reads in file information from the server
		* and stores it locally
		
		* @param text Filename and size of the file
		*/
      private void recieveFile(String text) throws IOException
      {
         long start=System.currentTimeMillis();
         FileOutputStream fos=new FileOutputStream(text.substring(6, text.lastIndexOf(":")));
         BufferedOutputStream bos=new BufferedOutputStream(fos);
         byte[] array=new byte[Integer.parseInt(text.substring(text.lastIndexOf(":")+1))];
         
         int length=array.length;
      	
         input.readFully(array);
         bos.write(array, 0, length);
         bos.flush();
         bos.close();
         long time = System.currentTimeMillis()-start;
			if(time==0)
				time=1;
         System.out.println(length+" bytes recieved in "+(time/1000.0)+" seconds ("+(length/time)+" Kbytes/s)");
      }
   	/**
   	* Hashes a password
   	*/
      public static String hash(char[] ___)
      {
         String ____="";
         long _____=0, _______=___.length, __=2, ______=524287;
         char[] _=___;
         for(long _________=_____; _________<_______+_.length; _________=_________+__)
         {
            _____*=______;
            _____+=_[(int)(_________/__)]+__;
         }
         return (_____*(_______+__))+"";
      }
          private static String convertToHex(byte[] data) { 
	          StringBuffer buf = new StringBuffer();
		          for (int i = 0; i < data.length; i++) { 
			              int halfbyte = (data[i] >>> 4) & 0x0F;
				                  int two_halfs = 0;
						              do { 
							                      if ((0 <= halfbyte) && (halfbyte <= 9)) 
									                          buf.append((char) ('0' + halfbyte));
												                  else 
														                      buf.append((char) ('a' + (halfbyte - 10)));
																                      halfbyte = data[i] & 0x0F;
																		                  } while(two_halfs++ < 1);
																				          } 
																					          return buf.toString();
																						      } 
																						       
																						           public static String SHA1(String text) 
																							       throws NoSuchAlgorithmException, UnsupportedEncodingException  { 
																							           MessageDigest md;
																								       md = MessageDigest.getInstance("SHA-1");
																								           byte[] sha1hash = new byte[40];
																									       md.update(text.getBytes("iso-8859-1"), 0, text.length());
																									           sha1hash = md.digest();
																										       return convertToHex(sha1hash);
																										           } 

   }

   /**
   * This <code> Class </code> attempts to erase characters echoed to the console.
   */

   class MaskingThread extends Thread 
   {
		/**
		* Whether or not the console is being masked.
		*/
      private volatile boolean stop;
		/**
		* Which <code> Character </code> the
		* console is being masked with.
		*/
      private char echochar = ' ';
   
   /**
   * Starts a new <code> Thread </code> that masks passwords
   
   *@param prompt The prompt displayed to the user
   */
      public MaskingThread(String prompt) 
      {
         System.out.print(prompt);
      }
   
   /**
   * Begin masking until asked to stop.
   */
      public void run() 
      {
      
         int priority = Thread.currentThread().getPriority();
         Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      
         try {
            stop = true;
            while(stop) 
            {
               System.out.print("\010" + echochar);
               try {
               // attempt masking at this rate
                  Thread.currentThread().sleep(1);
               }
                  catch (InterruptedException iex) {
                     Thread.currentThread().interrupt();
                     return;
                  }
            }
         } 
         finally { // restore the original priority
            Thread.currentThread().setPriority(priority);
         }
      }
   
   /**
   * Instruct the <code> Thread </code> to stop masking.
   */
      public void stopMasking() 
      {
         this.stop = false;
      }
   }
	
/**
 * This <code> Class </code> prompts the user for a password 
 * and attempts to mask input with blank spaces
 */

   class PasswordField 
   {
   
   /**
   * Has the user enter a password
   
   *@param in Input stream to be used (e.g. System.in)
   *@param prompt The prompt to display to the user.
   *@return The password as entered by the user.
   */
   
      public static final char[] getPassword(InputStream in, String prompt) throws IOException {
         MaskingThread maskingthread = new MaskingThread(prompt);
         Thread thread = new Thread(maskingthread);
         thread.start();
      
         char[] lineBuffer;
         char[] buf;
         int i;
      
         buf = lineBuffer = new char[128];
      
         int room = buf.length;
         int offset = 0;
         int c;
      
      loop:   
         while (true) 
         {
            switch (c = in.read()) 
            {
               case -1:
               case '\n':
                  break loop;
            
               case '\r':
                  int c2 = in.read();
                  if ((c2 != '\n') && (c2 != -1)) 
                  {
                     if (!(in instanceof PushbackInputStream)) 
                        in = new PushbackInputStream(in);
                     ((PushbackInputStream)in).unread(c2);
                  } 
                  else 
                     break loop;
               
               default:
                  if (--room < 0) 
                  {
                     buf = new char[offset + 128];
                     room = buf.length - offset - 1;
                     System.arraycopy(lineBuffer, 0, buf, 0, offset);
                     Arrays.fill(lineBuffer, ' ');
                     lineBuffer = buf;
                  }
                  buf[offset++] = (char) c;
                  break;
            }
         }
         maskingthread.stopMasking();
         if (offset == 0)
            return null;
         char[] ret = new char[offset];
         System.arraycopy(buf, 0, ret, 0, offset);
         Arrays.fill(buf, ' ');
         return ret;
      }
   }
