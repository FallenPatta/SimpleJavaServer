package handlers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.org.apache.bcel.internal.util.SyntheticRepository;

import authentication.FileSystemAuthenticator;
import filesystem.Directory;
import filesystem.Filesystem;
import filesystem.SimplePair;
import filesystemExceptions.ServerContentNotFoundException;
import filesystemExceptions.ServerDirectoryNotFoundException;
import javafx.util.Pair;
import sun.misc.IOUtils;

public class FilesystemHandler implements HttpHandler {
	
	private String htmlStart = "<html>";
	private String htmlEnd = "</html>";
	private byte response[];
	
	private String remoteIP;
	
	private final HttpServer server;
	private final String servType;
	private Filesystem fileSystem;
	private String authDB;

	@Override
	public void handle(HttpExchange t) throws IOException {
		if(t.getRequestMethod().equals("GET")){
			sendContent(t);
		}
		else if(t.getRequestMethod().equals("POST")){
			postHandler(t);
		}
	}
	
	
	/**
	 * Handlet GET Requests
	 * 
	 * @param t
	 */
	private void sendContent(HttpExchange t){
		StringBuilder view = new StringBuilder();
//		System.out.println(t.getRequestURI().toString());
		Directory selected = null;
		response = null;
		
		//TODO: implement dynamic request handling
		try{
			try {
				selected = fileSystem.open(URLDecoder.decode(t.getRequestURI().toString(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		
			//Init Style
			view.append("<head>");
			Path styleSheet = Paths.get("pagestyles/column_style.html");
			Path styleSheet2 = Paths.get("pagestyles/top_border_style.html");
			Path ulForm = Paths.get("html/upload_form.html");
			Path mkdirForm = Paths.get("html/mkdir_form.html");
			try {
				view.append(new String(Files.readAllBytes(styleSheet)));
				view.append(new String(Files.readAllBytes(styleSheet2)));
				view.append("<div class=\"flex-container2\">");
				view.append("<div class=\"column_left2\">");
				view.append(new String(Files.readAllBytes(ulForm)).replace("INPUTADDRESS", servType + "://" + remoteIP + ":" + t.getLocalAddress().getPort()
						+ "/" + URLEncoder.encode(selected.getPath(), "UTF-8")));
				view.append("</div>");
				view.append("<div class=\"column_right2\">");
				view.append(new String(Files.readAllBytes(mkdirForm)).replace("INPUTADDRESS", servType + "://" + remoteIP + ":" + t.getLocalAddress().getPort()
						+ "/" + URLEncoder.encode(selected.getPath(), "UTF-8")));
				view.append("</div>");
				view.append("</div>");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			view.append("</head><body>");
						
			//Add Internals
			Map<String, Directory> children = selected.getChildren();
			Set<String> childNames = children.keySet();
			ArrayList<String> cNames = new ArrayList<String>(childNames);
			Collections.sort(cNames);
			view.append("<div class=\"flex-container\">");
			view.append("<div class=\"column_left\">");
			for(String name : cNames){
				view.append("<div class=\"separated\"><p><a href=\""+servType+"://" + remoteIP + ":" + t.getLocalAddress().getPort() //style=\"text-align:left;\"
						+ (fileSystem.getRootname().isEmpty()?"":"/") + children.get(name).getPath() + "\">"
						+ name + "</a></p></div>");
			}
			view.append("</div>");
			Map<String, File> contents = selected.getContent();
			Set<String> contentNames = contents.keySet();
			cNames = new ArrayList<String>(contentNames);
			Collections.sort(cNames);
			view.append("<div class=\"column_right\"");
			for(String name : cNames){
				view.append("<p><div class=\"separated\"><a href=\""+servType+"://" + remoteIP + ":" + t.getLocalAddress().getPort()
						+ (fileSystem.getRootname().isEmpty()?"":"/") + selected.getPath() + "/" + name + "\">"
						+ name + "</a></div></p>");
			}
			view.append("</div>");
			view.append("</div></body>");
			
			response = (htmlStart + view.toString() + htmlEnd).getBytes();
			
		} catch (ServerDirectoryNotFoundException e){
			//System.out.println("Reqest is no Directory");
		}
		
		if(response == null) try{
			response = fileSystem.getTree().getFile(URLDecoder.decode(t.getRequestURI().toString(), "UTF-8"));
		} catch(ServerContentNotFoundException | NullPointerException | UnsupportedEncodingException e){
			//System.out.println("Reqest is no File");
		}
		
		if(response == null){
			response = 	standartRelocator("html/filesystemhandler_defaultresponse.html",3);
			//Send
			OutputStream os = t.getResponseBody();
			Thread sender = new Thread(new Runnable(){
	
				@Override
				public void run() {
					try {
						t.sendResponseHeaders(200, response.length);
						os.write(response);
						os.close();
					} catch (IOException e) {
					}
				}
				
			});
			sender.start();
		}
		else{	
			//Send
			OutputStream os = t.getResponseBody();
			Thread sender = new Thread(new Runnable(){
	
				@Override
				public void run() {
					try {
						t.sendResponseHeaders(200, response.length);
						os.write(response);
						os.close();
					} catch (IOException e) {
					}
				}
				
			});
			sender.start();
		}
	}
	
	/**
	 * Handhabt Post Requests
	 * 
	 * @param t Der Exchange
	 */
	private void postHandler(HttpExchange t){
		InputStream in = t.getRequestBody();
		new Thread(new Runnable(){
			@Override
			public void run(){
				byte input[] = null;
				try {
					//input = IOUtils.readFully(in, -1, true);
					input = readBuffer(in);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				//System.out.println(new String(input));
				
				//System.out.println(input);
				//sendResponse((htmlStart + new String(response) + htmlEnd).getBytes(), t);
					byte boundry[] = HtmlUploadHelper.getBoundry(input);
					ArrayList<File> formEntrys = HtmlUploadHelper.getFormEntrys(input, boundry);
					input = null;
					ArrayList<File> fileEntrys = HtmlUploadHelper.getFileEntrys(formEntrys);
					ArrayList<byte[]> mkdirEntrys = HtmlUploadHelper.getMkdirEntrys(formEntrys);
					
					//TODO: Add Interface for dynamic entrys
					//MKDIR POST requests
					try {
						fileSystem.open(URLDecoder.decode(t.getRequestURI().toString(), "UTF-8"));
					} catch (ServerDirectoryNotFoundException | UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
					for(int i = 0; i<mkdirEntrys.size(); i++){
						String dirToMake = HtmlUploadHelper.getDirName(mkdirEntrys.get(i));
						fileSystem.mkdir(dirToMake);
						try {
							fileSystem.open(dirToMake);
							Map<String, Directory> chil = fileSystem.getTree().getCurrent().getParent().getChildren();
							Set<String> nam = chil.keySet();
//							for(String n: nam){
//								System.out.println(n);
//							}
						} catch (ServerDirectoryNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					//File POST requests
					for(int i = 0; i<fileEntrys.size(); i++){
						SimplePair<String,File> p = HtmlUploadHelper.getFileEntryBytes(fileEntrys.get(i));
						//TODO: Check which files can be handled
							try{
								//TODO: Make Upload permanent and save Filesystem to Harddrive and check for empty uploads
									File f = null;
									if(fileSystem.getFSRoot().isEmpty() || fileSystem.getFSRoot().equals("/")){
										f = File.createTempFile("UPLOAD", p.getKey().replace(" ", "_"));
										f.deleteOnExit();
										f=p.getVal();
									} else{
										String dir = fileSystem.getFSRoot();
										dir = dir.lastIndexOf(File.pathSeparator) == dir.length()-1 ? dir.substring(0, dir.length()-1) : dir;
										String fsdir = fileSystem.getTree().getCurrent().getPath().replaceFirst(fileSystem.getRootname(), "");
										f = new File(dir + fsdir + "/" + p.getKey().replace(" ", "_"));
										f.getParentFile().mkdirs();
										FileOutputStream fout = new FileOutputStream(f);
										fout.write(Files.readAllBytes(p.getVal().toPath()));
										fout.close();
										p.getVal().delete();
									}
									
									fileSystem.getTree().getCurrent().addFile(f, p.getKey().replace(" ", "_"));
							} catch (IOException e) {
								e.printStackTrace();
							}
					}
					//TODO: Send updated Serverpage or wait for refresh, whichever one is faster
					sendContent(t);
					if(!(fileSystem.getFSRoot().isEmpty() || fileSystem.getFSRoot().equals("/")))
						for(File p : formEntrys){
							p.delete();
						}
						for(File p: fileEntrys){
							p.delete();
						}
			}
		}).start();
	}
	
	private byte[] standartRelocator(String file, int sec){
		try {
			return new String(Files.readAllBytes(Paths.get(file)))
			.replace("[NUMSEC]", Integer.toString(sec))
			.replace("[IPADDR]", this.remoteIP)
			.replace("[PORTNUM]", Integer.toString(server.getAddress().getPort()))
			.replace("[SERVERROOT]", fileSystem.getRootname())
			.replace("[SERVERTYPE]", this.servType)
			.getBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private byte[] readBuffer(InputStream is) throws IOException{
		File f = File.createTempFile("temporary", "bufferfile");
		FileOutputStream buffer = new FileOutputStream(f);

		int nRead;
		byte[] data = new byte[16384];
		byte b[] = null;
		try{
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
			buffer.flush();
		}
		buffer.close();
		b = Files.readAllBytes(f.toPath());
		f.delete();
		}catch(IOException e){
			f.delete();
			throw new IOException(e.getMessage());
		}
		return b;
	}
	
	
	public void hookUpFilesystem(){
		try{
		HttpContext cont  = server.createContext("/"+this.fileSystem.getRootname(), this);
		cont.setAuthenticator(new FileSystemAuthenticator(this.fileSystem.getRootname(), authDB));
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Will take any String and store it
	 * @param ip the IP String
	 */
	public void setRemoteIP(String ip){
		this.remoteIP = ip;
	}
	
	public FilesystemHandler(Filesystem f, String remoteIP, HttpServer server, String database){
		this.fileSystem = f;
		this.server = server;
		this.remoteIP = remoteIP;
		this.servType = "http";
		this.authDB = database;
		System.out.println(servType);
		//hookUpFilesystem();
	}
	
	public FilesystemHandler(Filesystem f, String remoteIP, HttpsServer server, String database){
		this.fileSystem = f;
		this.server = server;
		this.remoteIP = remoteIP;
		this.servType = "https";
		this.authDB = database;
		System.out.println(servType);
		//hookUpFilesystem();
	}

}
