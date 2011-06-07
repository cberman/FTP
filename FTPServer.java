   import java.awt.*;
   import java.awt.event.*;
   import java.net.*;
   import java.io.*;
   import javax.swing.*;
   import java.util.*;
	import java.security.*;

    public class FTPServer extends Thread
   {   
   
   /**
   * @author Collin Berman 
   * @version 5.2.2011
   */
   
   	/**
   	* Array of all the connected clients.  An array is
   	* used instead of an <code> ArrayList </code> so that
   	* an effective maximum on connections can be maintained.
   	*/
      private User[] users;
   	/**
   	* Maintains information about this server used when
   	* accepting new connections.
   	*/
      private ServerSocket server;
   	/**
   	* Maximum number of connections to this server allowed
   	*/
      private int maxConnections=-1;
   	/**
   	* Port that this server is run on
   	*/
      public int port=-1;
   	/**
   	* Message sent to the client after a <code> NOOP </code> command
   	*/
      public String noop;
   	/**
   	* Path that new connections start at
   	*/
      public String defaultPath;
   	/**
   	* Highest path that clients are allowed to navigate to
   	*/
      public String root;
   	/**
   	* Message sent to clients on successful connection
   	*/
      public String welcome;
   	/**
   	* Used to navigate between directories.  Either
   	* a \ or a /, depending on the operating system.
   	*/
      public String fd;
   	/**
   	* The password to the server, stored as a <code> char </code>
   	* array instead of a <code> String </code> because <code> String</code>s
   	* are immutable.
   	*/
      private char[] pass;
   	/**
   	* Whether or not this server is secured with a password.
   	*/
      public boolean hasPass=false;
      
   	/**
   	* Create a new server and instantiate various variables
   	*/
       public FTPServer() throws IOException
      {
         String os=System.getProperty("os.name");
         System.out.println("OS detected as "+os);
         System.out.println("IP detected as "+InetAddress.getLocalHost().getHostAddress());
         if(os.contains("Linux"))
            fd="/";
         else if(os.contains("Windows"))
            fd="\\";
         PrintStream out=null;
         if(!new File("server.cfg").exists())
         {
            System.out.println("server.cfg not found");
            try{out=new PrintStream(new FileOutputStream("server.cfg", true));}
                catch(Exception e){e.printStackTrace(); System.exit(1);}
            System.out.println("server.cfg created successfully");
         }
         else
         {
            try{out=new PrintStream(new FileOutputStream("server.cfg", true));}
                catch(Exception e){e.printStackTrace(); System.exit(1);}
            System.out.println("server.cfg successfully loaded");
         }
         BufferedReader f=null;
         try{f=new BufferedReader(new FileReader("server.cfg"));}
             catch(Exception e){
               e.printStackTrace();
               System.exit(1);}
         StringTokenizer st;
         for(String line=f.readLine(); line!=null; line=f.readLine())
         {
            st=new StringTokenizer(line);
            String header=st.nextToken();
            if(header.equalsIgnoreCase("CONNECTIONS:"))
               maxConnections=Integer.parseInt(st.nextToken());
            else if(header.equalsIgnoreCase("NOOP:"))
            {
               noop=st.nextToken();
               while(st.hasMoreTokens())
                  noop+=" "+st.nextToken();
            }
            else if(header.equalsIgnoreCase("DIRECTORY:"))
            {
               defaultPath=st.nextToken();
               while(st.hasMoreTokens())
                  defaultPath+=" "+st.nextToken();
            }
            else if(header.equalsIgnoreCase("ROOT:"))
            {
               root=st.nextToken();
               while(st.hasMoreTokens())
                  root+=" "+st.nextToken();
            }
            else if(header.equalsIgnoreCase("CONNECT:"))
            {
               welcome=st.nextToken();
               while(st.hasMoreTokens())
                  welcome+=" "+st.nextToken();
            }
            else if(header.equalsIgnoreCase("PORT:"))
               port=Integer.parseInt(st.nextToken());
         }
         if(maxConnections==-1)
         {
            System.out.println("Header CONNECTIONS not found; Max connections set as 5");
            maxConnections=5;
            out.println("CONNECTIONS: 5");
         }
         if(noop==null)
         {
            System.out.println("Header NOOP not found; NOOP response set as \"200 <OK>\"");
            noop="200 <OK>";
            out.println("NOOP: 200 <OK>");
         }
         if(welcome==null)
         {
            System.out.println("Header CONNECT not found; New connection response set as \"Connection successful\"");
            welcome="Connection successful";
            out.println("CONNECT: Connection successful");
         }
         if(os.contains("Linux"))
         {
            if(defaultPath==null)
            {
               System.out.println("Header DIRECTORY not found; Start directory set as /");
               defaultPath="/";
               out.println("DIRECTORY: /");
            }
            if(root==null)
            {
               System.out.println("Header ROOT not found; Root directory set as /");
               root="/";
               out.println("ROOT: /");
            }
         }
         else if (os.contains("Windows"))
         {
            if(defaultPath==null)
            {
               System.out.println("Header DIRECTORY not found; Start directory set as C:\\");
               defaultPath="C:\\";
               out.println("DIRECTORY: C:\\");
            }
            if(root==null)
            {
               System.out.println("Header ROOT not found; Root directory set as C:\\");
               root="C:\\";
               out.println("ROOT: C:\\");
            }
         }
         if(port==-1)
         {
            System.out.println("Header PORT not found; Port set as 5000");
            port=5000;
            out.println("PORT: 5000");
         }
      	
         users = new User[ maxConnections ];
      
         try {
            server = new ServerSocket( port, 2 );
         }
             catch(BindException e)
            {
               System.out.println("Something is already being run on this port.");
               System.exit(1);
            }
             catch( IOException e ) {
               e.printStackTrace();
               System.exit( 1 );
            }
         server.setSoTimeout(500);
      	//password input
         pass = null;
         try {
            pass = PasswordField.getPassword(System.in, 
               "Enter a password for the server (hit ENTER for no password): ");
         } 
             catch(IOException e){e.printStackTrace();}
         System.out.print("\n\r");	//carriage return
         if(pass!=null)
            hasPass=true;
      }
   	
   	/**
   	* Constantly checks for new connections and accepts them.
   	*/
       public void run()
      {
         while(true)
         {
            for ( int i = 0; i < users.length; i++ ) 
            {
               try{this.sleep(5000);}
                   catch(InterruptedException e){e.printStackTrace();}
               if(users[i]==null)
               {
                  try {
                     users[ i ] = new User( server.accept(), this);
                     users[ i ].start();
                  }
                      catch(SocketTimeoutException e){}
                      catch( IOException e ) {
                        e.printStackTrace();
                        System.exit( 1 );
                     }
               }
               else if(users[i].disconnected){
                  try{
                     display(users[i].IP+" has disconnected.");
                     users[i]=null;
                  }
                      catch(Exception e){
                        e.printStackTrace();
                        System.exit(1);}}
            }
         }
      }
   	/**
   	* Writes a message to standard out
   	
   	* @param s The message to be displayed
   	*/
       public void display( String s ) throws IOException
      {
         System.out.println(s);
      }
   	/**
   	* Interprets a message from the client and
   	* delegates the task to a new method
   	
   	* @param p The client that sent the command
   	* @param text The message sent by the client
   	*/
       public void interpret(User p, String text) throws IOException
      {
         String[] word=text.split("[ \t\n\f\r]");
         if(word[0].equalsIgnoreCase("noop"))
            p.output.writeUTF(noop);
         else if(word[0].equalsIgnoreCase("ls")||word[0].equalsIgnoreCase("list")||word[0].equalsIgnoreCase("dir"))
            list(p, word);
         else if(word[0].equalsIgnoreCase("flist"))
            flist(p, word);
         else if(word[0].equalsIgnoreCase("dlist"))
            dlist(p, word);
         else if(word[0].equalsIgnoreCase("cdup"))
            cdup(p);
         else if(word[0].equalsIgnoreCase("cd")||word[0].equalsIgnoreCase("cwd"))
            cwd(p, word);
         else if(word[0].equalsIgnoreCase("size"))
         {
            String size=size(p, word);
            if(size!="")
               p.output.writeUTF(size);
         }
         else if(word[0].equalsIgnoreCase("get")||word[0].equalsIgnoreCase("retr"))
            retr(p, word);
         else if(word[0].equalsIgnoreCase("pwd"))
            p.output.writeUTF(p.path.getPath());
         else if(word[0].equalsIgnoreCase("rm")||word[0].equalsIgnoreCase("dele"))
            rm(p, word);
         else if(word[0].equalsIgnoreCase("mdtm"))
            mdtm(p, word);
         else if(word[0].equalsIgnoreCase("mget"))
            mget(p, word);
         else if(word[0].equalsIgnoreCase("fget"))
            fget(p, word);
         else if(word[0].equalsIgnoreCase("shutdown"))	//temp
            System.exit(0);
         else
            p.output.writeUTF(word[0]+" is not recognized as a command.");
         p.output.writeUTF("<EOF>"+p.path.getPath());
      }
   	/**
   	* Sends the client a list of files and directories
   	* of the specified directory.  If no directory is
   	* specified, the current one is used.
   	
   	* @param p The client that requested the list
   	* @param word The exact message sent by the client
   	*/
       public void list(User p, String[] word) throws IOException
      {
         String[] files=null;
         File dir=null;
         if(word.length==0)
            dir=p.path;
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            dir=new File(p.path.getPath()+fd+newPath);
            if(!dir.exists())
            {
               p.output.writeUTF(newPath+": No such file or directory");
               return;
            }
            else if(!dir.isDirectory())
            {
               p.output.writeUTF(newPath+": Not a directory");
               return;
            }
         }
         files=dir.list();
         if(files==null)
         {
            p.output.writeUTF("Access to "+dir.getName()+" denied");
            return;
         }
         p.output.writeUTF(".\n..");
         for(String i:files)
            p.output.writeUTF(i);
      }
   	/**
   	* Sends the client a list of files 
   	* of the specified directory.  If no directory is
   	* specified, the current one is used.
   	
   	* @param p The client that requested the list
   	* @param word The exact message sent by the client
   	*/
       public void flist(User p, String[] word) throws IOException
      {
         File[] files=null;
         File dir=null;
         if(word.length==0)
            dir=p.path;
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            dir=new File(p.path.getPath()+fd+newPath);
            if(!dir.exists())
            {
               p.output.writeUTF(newPath+": No such file or directory");
               return;
            }
            else if(!dir.isDirectory())
            {
               p.output.writeUTF(newPath+": Not a directory");
               return;
            }
         }
         files=dir.listFiles();
         if(files==null)
         {
            p.output.writeUTF("Access to "+dir.getName()+" denied");
            return;
         }
         for(File i:files)
            if(!i.isDirectory())
               p.output.writeUTF(i.getName());
      }
   	/**
   	* Sends the client a list of directories 
   	* in the specified directory.  If no directory is
   	* specified, the current one is used.
   	
   	* @param p The client that requested the list
   	* @param word The exact message sent by the client
   	*/
       public void dlist(User p, String[] word) throws IOException
      {
         File[] files=null;
         File dir=null;
         if(word.length==0)
            dir=p.path;
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            dir=new File(p.path.getPath()+fd+newPath);
            if(!dir.exists())
            {
               p.output.writeUTF(newPath+": No such file or directory");
               return;
            }
            else if(!dir.isDirectory())
            {
               p.output.writeUTF(newPath+": Not a directory");
               return;
            }
         }
         files=dir.listFiles();
         if(files==null)
         {
            p.output.writeUTF("Access to "+dir.getName()+" denied");
            return;
         }
         p.output.writeUTF(".\n..");
         for(File i:files)
            if(i.isDirectory())
               p.output.writeUTF(i.getName());
      }
   	/**
   	* Changes the directory to the parent directory
   	* of the current directory.
   	
   	* @param p The client that requested the directory change
   	*/
       public void cdup(User p) throws IOException
      {
         String[] word=new String[2];
         word[0]="cwd";
         word[1]="..";
         cwd(p, word);
      }
   	/**
   	* Changes the directory to the specified directory
   	
   	* @param p The client that requested the directory change
   	* @param word The exact message sent by the client
   	*/
       public void cwd(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.path=new File(defaultPath);
         else if(word[1].equals(".")&&word.length==2);
         else if(word[1].equals("..")&&word.length==2)
         {
            if(p.path.getPath().equals(root))
               p.output.writeUTF("You are not allowed to go any higher");
            else
               p.path=new File(p.path.getPath().substring(0, p.path.getPath().lastIndexOf(fd)+1));
         }
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            File dir=new File(p.path.getPath()+fd+newPath);
            if(!dir.exists())
               p.output.writeUTF(newPath+": No such file or directory");
            else if(!dir.isDirectory())
               p.output.writeUTF(newPath+": Not a directory");
            else
               p.path=dir;
         }
      }
   	/**
   	* Sends the client the size of a file or folder.
   	* Uses a <code> Queue </code>
   	
   	* @param p The client that requested the file size
   	* @param word The exact message sent by the client
   	*/
       public String size(User p, String[] word) throws IOException
      {
         if(word.length==1)
         {
            p.output.writeUTF(word[0]+" requires a filename argument");
            return "";
         }
         long divisor=1;
         String unit="b";
         String newPath="";
         boolean coolGuy=true;
         for(int i=1; i<word.length; i++)
            if(word[i].charAt(0)=='-')
               switch(word[i].charAt(1))
               {
                  case 'b':
                     divisor=1;
                     unit="b";
                     break;
                  case 'k':
                     divisor=1024;
                     unit="kb";
                     break;
                  case 'm':
                     divisor=1048576;
                     unit="mb";
                     break;
                  case 'g':
                     divisor=1073741824;
                     unit="gb";
                     break;
                  default:
                     p.output.writeUTF(word[i].charAt(1)+" is not a valid identifier");
               }
            else
            {
               if(!coolGuy)
                  newPath+=" ";
               else
                  coolGuy=false;
               newPath+=word[i];
            }
         File file=new File(p.path.getPath()+fd+newPath);
         if(!file.exists())
         {
            p.output.writeUTF(newPath+": No such file or directory");
            return "";
         }
         Queue<File> q=new LinkedList<File>();
         q.offer(file);
         long sum=0;
         while(!q.isEmpty())
         {
            file=q.remove();
            if(file.isDirectory())
               try
               {
                  for(File i:file.listFiles())
                     q.offer(i);
               }
                   catch(NullPointerException e){
                     try{p.output.writeUTF("Access to "+file.getName()+" denied");}
                         catch(SocketException ex){p.disconnected=true;}}
            else
               sum+=file.length();
         }
         return (sum/divisor)+unit;
      }
   	/**
   	* Sends the client a file
   	
   	* @param p The client that requested the file
   	* @param word The exact message sent by the client
   	*/
       public void retr(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.output.writeUTF(word[0]+" requires a filename argument");
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            File file=new File(p.path.getPath()+fd+newPath);
            if(!file.exists())
               p.output.writeUTF(file.getName()+": No such file or directory");
            else if(file.isDirectory())
               p.output.writeUTF(file.getName()+": Is a directory");
            else
               sendFile(p, file, file.getName());
         }
      }
   	/**
   	* Sends the client a file
   	
   	* @param p The client that requested the file
   	* @param path The path of the file requested by the client
   	*/
       public void sendFile(User p, File file, String name) throws IOException
      {
         byte[] array=new byte[0];
         boolean tooBig=false;
         try{array=new byte[(int)file.length()];}
             catch(OutOfMemoryError e){tooBig=true;}
         if(tooBig||(long)array.length!=file.length())
            p.output.writeUTF("File too big to transfer");
         else
         {
            new BufferedInputStream(new FileInputStream(file)).read(array, 0, array.length);
            p.output.writeUTF("<FILE>"+name+":"+array.length);
            p.output.write(array, 0, array.length);
            p.output.flush();
         }
      }
   	/**
   	* Removes a file from the server
   	
   	* @param p The client that requested the file removal
   	* @param word The exact message sent by the client
   	*/
       public void rm(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.output.writeUTF(word[0]+" requires a filename argument");
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            File file=new File(p.path.getPath()+fd+newPath);
            if(!file.exists())
               p.output.writeUTF(newPath+": No such file or directory");
            else if(file.isDirectory())
               p.output.writeUTF("Cannot remove "+newPath+": Is a directory");
            else
               file.delete();
         }
      }
   	/**
   	* Sends the client the date a file was last modified
   	* in milliseconds since the epoch.
   	
   	* @param p The client that requested the date
   	* @param word The exact message sent by the client
   	*/
       public void mdtm(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.output.writeUTF(word[0]+" requires a filename argument");
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            File file=new File(p.path.getPath()+fd+newPath);
            if(!file.exists())
               p.output.writeUTF(newPath+": No such file or directory");
            else
               p.output.writeUTF(file.lastModified()+"");
         }
      }
   	/**
   	* Sends the client all the files in a folder
   	* that match the given regular expression.
   	
   	* @param p The client that requested the date
   	* @param word The exact message sent by the client
   	*/
       public void mget(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.output.writeUTF(word[0]+" requires an argument");
         else
         {
            String regex="";
            for(int i=1; i<word.length; i++)
               regex+=(i>1)?" "+word[i]:word[i];
            for(File i:p.path.listFiles())
               if(i.exists()&&!i.isDirectory()&&matches(regex, i.getName()))
                  sendFile(p, i, i.getName());
         }
      }
		/**
		* Used by <code> mget </code> to determine if
		* a specific file should be sent.
		
		* @param regex The regular expression.
		* @param name The name of the file
		*/
       public boolean matches(String regex, String name)
      {
         for(int i=0; i<regex.length(); i++)
         {
            char c=regex.charAt(i);
            switch(c)
            {
               case '?':
                  break;
               case '*':
                  for(int s=i; s<name.length(); s++)
                     if(matches(regex.substring(i+1), name.substring(s)))
                        return true;
                  break;
               default:
                  if(c!=name.charAt(i))
                     return false;
            }
         }
         return true;
      }
   	/**
   	* Sends the client a folder, and all it's subdirectories
   	
   	* @param p The client that requested the date
   	* @param word The exact message sent by the client
   	*/
       public void fget(User p, String[] word) throws IOException
      {
         if(word.length==1)
            p.output.writeUTF(word[0]+" requires a filename argument");
         else
         {
            String newPath="";
            for(int i=1; i<word.length; i++)
               newPath+=(i>1)?" "+word[i]:word[i];
            File file=new File(p.path.getPath()+fd+newPath);
            if(!file.exists())
               p.output.writeUTF(newPath+": No such file or directory");
            else if(!file.isDirectory())
               p.output.writeUTF(newPath+": Not a directory");
            else
            {
               int start=file.getPath().length()-file.getName().length();
               Stack<File> stack=new Stack<File>();
               stack.push(file);
               while(!stack.isEmpty())
               {
                  file=stack.pop();
                  if(file.isDirectory())
                  {
                     p.output.writeUTF("<FOLDER>"+file.getPath().substring(start));
                     for(File i:file.listFiles())
                        stack.push(i);
                  }
                  else if(file.exists())
                     sendFile(p, file, file.getPath().substring(start));
               }
            }
         }
      }
   	/**
   	* Hashes a password
   	*/
       public String hash(char[] ___)
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
   	/**
   	* Checks to see if the password sent by the client
   	* mathces the server's password.
   	
   	* @param hash The hashed password from the client
   	*/
       public boolean isPass(String hash)
      {
         return hash.equals(hash(pass));
      }
   	/**
   	* Creates a new server and starts the required <code> Thread</code>s
   	*/
       public static void main( String args[] ) throws IOException
      {
         FTPServer server = new FTPServer();
         System.out.println("Server up and running");
         server.start();
         (new Client()).start();
      }
   }
	
	/**
	* <code> Class </code> maintaining information on each client
	*/
    class User extends Thread 
   {
   	/**
   	* The place the connection between the client
   	* and server is made
   	*/
      private Socket connection;
   	/**
   	* <code> Stream </code> reading from the client
   	*/
      private DataInputStream input;
   	/**
   	* <code> Stream </code> writing to the client
   	*/
      public DataOutputStream output;
   	/**
   	* <code> FTPServer </code> associated with this client
   	*/
      private FTPServer control;
   	/**
   	* The directory the user is currently in
   	*/
      public File path;
   	/**
   	* The IP address of the client machine
   	*/
      public String IP;
   	/**
   	* Tells the server if it's safe to remove this user
   	*/
      public boolean disconnected = false;
   	
   	/**
   	* Creates a new connection
   	
   	* @param socket The place the connection is made
   	* @param server The host server
   	*/
       public User( Socket socket, FTPServer server)
      {
         connection = socket;
         try {
            input = new DataInputStream(connection.getInputStream());
            output = new DataOutputStream(connection.getOutputStream());
         }
             catch( IOException e ) {
               e.printStackTrace();
               System.exit( 1 );
            }
      
         control = server;
         path=new File(control.defaultPath);
      }
   	
   	/**
   	* Reads in requests from the client and
   	* sends them to the server for interpretation.
   	*/
       public void run()
      {
         try {
            IP=connection.getInetAddress().getHostAddress();
            control.display( "New connection from "+IP );
            output.writeBoolean(control.hasPass);
            if(control.hasPass)
            {
               String hash=input.readUTF();
               if(!control.isPass(hash))
               {
                  output.writeBoolean(true);
                  disconnected=true;
               }
               else
                  output.writeBoolean(false);
            }
            output.writeUTF(control.welcome);
            output.writeUTF("<EOF>"+path.getPath());
         	
            while ( true ) 
            {
               String text="";
               try{
                  text = input.readUTF();}
                   catch(EOFException e){
                     disconnected=true;
                     break;}
                   catch(SocketException e){
                     disconnected=true;
                     break;}
            
               control.interpret(this, text);
            }         
         
         }
             catch( Exception e ) {
               e.printStackTrace();
            }
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

	/**
	* Test <code> Class </code> used to run a
	* server and a client at the same time.
	*/
    class Client extends Thread
   {
   	/**
   	* Starts a new <code> FTPClient </code> running.
   	*/
       public void run()
      {
         String[] args={};
         try
         {FTPClient.main(args);}
             catch(IOException e){
               e.printStackTrace();}
      }
   }