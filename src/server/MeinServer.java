package server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.share.sampled.file.TAudioFileFormat;

import com.sun.net.httpserver.*;

import callbacks.AudioCallBack;
import callbacks.HtmlUploaderCallBack;
import callbacks.StandardCallBack;
import filesystem.Filesystem;
import filesystemExceptions.ServerDirectoryNotFoundException;
import handlers.FileHandler;
import handlers.FilesystemHandler;
import handlers.HtmlFileHandler;
import handlers.TextHandler;
import httpsServer.HttpsServerCreator;

//import java.util.*;
//import javax.mail.*;
//import javax.mail.internet.*;
//import javax.activation.*;

public class MeinServer {

	private static int port = 0;
	private static File[] listOfFiles = null;
	private static int[] durations;
	private static String remoteIP = null;
	private static HttpServer server = null;
	private static Filesystem sys = null;
	private static FilesystemHandler sysHandler = null;
	/*
	 * private static int getMP3duration(File file) throws
	 * UnsupportedAudioFileException, IOException {
	 * 
	 * AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file); if
	 * (fileFormat instanceof TAudioFileFormat) { Map<?, ?> properties =
	 * ((TAudioFileFormat) fileFormat).properties(); String key = "duration";
	 * Long microseconds = (Long) properties.get(key); int mili = (int)
	 * (microseconds / 1000); int sec = (mili / 1000) % 60; int min = (mili /
	 * 1000) / 60; return min*60 + sec + 1; } else { throw new
	 * UnsupportedAudioFileException(); } }
	 * 
	 * public static byte[] loadFileFromURL(String url){ URL fileURL; File f;
	 * InputStream input; try { if(url.contains(remoteIP)){ fileURL = new
	 * URL(url); input = fileURL.openStream(); } else{ f = new File(url); input
	 * = Files.newInputStream(Paths.get(url)); }
	 * 
	 * 
	 * ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	 * 
	 * int nRead; byte[] data = new byte[16384];
	 * 
	 * while ((nRead = input.read(data, 0, data.length)) != -1) {
	 * buffer.write(data, 0, nRead); }
	 * 
	 * buffer.flush(); return buffer.toByteArray(); } catch (IOException e) {
	 * e.printStackTrace(); } return null; }
	 * 
	 * public static void fileInclude(HttpServer server, File[] listOfFiles,
	 * String context) { durations = new int[listOfFiles.length]; for (int i =
	 * 0; i < listOfFiles.length; i++) { if (!listOfFiles[i].isDirectory()){
	 * server.createContext("/" + listOfFiles[i].getName().replace(" ", "_"),
	 * new FileHandler(new File(context + "/" + listOfFiles[i].getName()),
	 * remoteIP, port, true)); try { durations[i] = getMP3duration(new File
	 * (context + "/" + listOfFiles[i].getName())); } catch
	 * (UnsupportedAudioFileException | IOException e) { System.err.println(
	 * "UNSUPPORTED AUDIOFILE - ONLY \".mp3\" IS SUPPORTED"); //System.exit(-1);
	 * } } } }
	 * 
	 * public static String filePreview(File[] listOfFiles, String IP) {
	 * StringBuilder sb = new StringBuilder();
	 * 
	 * for (int i = 0; i < listOfFiles.length; i++) { if
	 * (listOfFiles[i].isFile()) { String servType = "http";
	 * if(server.getClass().equals(HttpsServer.class)) servType = "https";
	 * sb.append("<p style=\"text-align:center;\"><a href=\"" + servType + "://"
	 * + IP + ":" + port + "/" + listOfFiles[i].getName().replace(" ", "_") +
	 * "\" style=\"text-align:center;\">" + listOfFiles[i].getName().replace(" "
	 * , "_") + "</a></p>"); } } return sb.toString(); }
	 */

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
		}
		
		if(authDB == null) return;
		if(servertype == null) return;
		if(port == 0) return;
		
		if(servertype.equals("https")){
			server = HttpsServerCreator.create(port);
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
		}else if(servertype.equals("http")){
			server = HttpServer.create(new InetSocketAddress(port), 0);
		}else return;
		
		sys = new Filesystem(filesystemroot, "");
		if (filesystemroot != null && !filesystemroot.isEmpty() & !filesystemroot.equals("/"))
			getDirTree(filesystemroot, sys);
		if(servertype.equals("http")) sysHandler = new FilesystemHandler(sys, remoteIP, server, authDB);
		else if (servertype.equals("https")) sysHandler = new FilesystemHandler(sys, remoteIP, (HttpsServer) server, authDB);
		server.setExecutor(null);
		server.start();
	}

}
