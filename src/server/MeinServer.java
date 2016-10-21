package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;

import com.sun.net.httpserver.*;

import callbacks.StandardCallBack;
import filesystem.Filesystem;
import filesystemExceptions.ServerDirectoryNotFoundException;
import handlers.FilesystemHandler;
import handlers.TextHandler;
import httpsServer.HttpsServerCreator;

//import java.util.*;
//import javax.mail.*;
//import javax.mail.internet.*;
//import javax.activation.*;

public class MeinServer {

	private static int port = 0;
	private static String remoteIP = null;
	private static HttpServer server = null;
	private static Filesystem sys = null;
	private static FilesystemHandler sysHandler = null;

	public static ArrayList<File> getDirTree(String root, Filesystem sys) {
		ArrayList<File> dirTree = new ArrayList<File>();
		File anchor = new File(root);
		ArrayList<File> bottom = new ArrayList<File>();
		bottom.add(anchor);
		String spacer = !sys.getRootname().isEmpty() ? "/" + sys.getRootname() : "/";

		while (!bottom.isEmpty()) {
			File exp = bottom.remove(0);
			if (exp.isDirectory()) {
				File children[] = exp.listFiles();
				for (File f : children) {
					if (f.isDirectory()) {
						String pName = f.getParent().replaceFirst(root, spacer);
						try {
							sys.open(pName);
							sys.mkdir(f.getName());
						} catch (ServerDirectoryNotFoundException e) {
							System.err.println("MKDIR");
							e.printStackTrace();
						}
						bottom.add(f);
					} else if (f.isFile() && f.canRead()) {
						String pName = f.getParent().replaceFirst(root, spacer);
						try {
							sys.open(pName);
							sys.getTree().getCurrent().addFile(f);
						} catch (ServerDirectoryNotFoundException e) {
							System.err.println("FILE");
							e.printStackTrace();
						}
					}
				}
			}
			dirTree.add(exp);
		}

		return dirTree;
	}
	
	public static String getPubIP(){
		URL publicIP;
		try {
			publicIP = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(publicIP.openStream()));

			return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws Exception {

		URL publicIP = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(publicIP.openStream()));

		String ip = in.readLine();
		System.out.println("public IP:" + ip);
		remoteIP = ip;

		String filesystemroot = null;
		String servertype = null;
		String authDB = null;
		boolean redirect = true;

		for (String s : args) {
			if (s.contains("servertype:=")) {
				if (s.substring(s.indexOf("servertype:=") + "servertype:=".length()).contains("https")) {
					servertype = "https";
				} else {
					servertype = "http";
				}
			}
			if (s.contains("fsRoot:=")) {
				filesystemroot = s.substring(s.indexOf("fsRoot:=") + "fsRoot:=".length());
				System.out.println("FSRoot: " + filesystemroot);
			}
			if(s.contains("authDB:=")){
				authDB = s.substring(s.indexOf("authDB:=")+"authDB:=".length());
				System.out.println("authDB: " + authDB);
			}
			if(s.contains("serverPort:=")){
				try{
				port = Integer.parseInt(s.substring(s.indexOf("serverPort:=")+"serverPort:=".length()));
				} catch(NumberFormatException e){
					
				}
				System.out.println("serverPort: " + port);
			}
			if(s.contains("domain:=")){
				remoteIP =  s.substring(s.indexOf("domain:=")+"domain:=".length());
				System.out.println("domain: " + remoteIP);
			}
			if(s.contains("redirect:=")){
				if(s.substring(s.indexOf("redirect:=")+"redirect:=".length()).equals("false")){
					redirect = false;
				}
				else if(s.substring(s.indexOf("redirect:=")+"redirect:=".length()).equals("true")){
					
				}
				else{
					System.err.println("Redirect must be \"true\" or \"false\".");
					System.exit(0);
				}
				System.out.println("redirect: " + redirect);
			}
		}
		
		if(authDB == null) return;
		if(servertype == null) return;
		if(port == 0) return;
		
		if(servertype.equals("https")){
			server = HttpsServerCreator.create(port);
			if(redirect){
				HttpServer redirServer = HttpServer.create(new InetSocketAddress(80), 0);
				redirServer.createContext("/", new TextHandler("<html><head><META HTTP-EQUIV=REFRESH CONTENT="
						+"\"1; URL=" 
						+ "https://" 
						+ remoteIP
						+ ":"+port
						+ "/\"></head></html>"
						, new StandardCallBack()));
				redirServer.setExecutor(null);
				redirServer.start();
			}
		}else if(servertype.equals("http")){
			server = HttpServer.create(new InetSocketAddress(port), 0);
		}else return;
		
		sys = new Filesystem(filesystemroot, "");
		if (filesystemroot != null && !filesystemroot.isEmpty() & !filesystemroot.equals("/"))
			getDirTree(filesystemroot, sys);
		if(servertype.equals("http")) sysHandler = new FilesystemHandler(sys, remoteIP, server, authDB);
		else if (servertype.equals("https")) sysHandler = new FilesystemHandler(sys, remoteIP, (HttpsServer) server, authDB);
		sysHandler.hookUpFilesystem();
		server.setExecutor(null);
		server.start();
	}

}
