package handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import callbacks.CallBack;
import callbacks.StandardCallBack;
import javafx.util.Pair;
import sun.misc.IOUtils;

public class HtmlFileHandler implements HttpHandler {
	
	final private String remoteIP;
	final private int port;
	final private Path tmpDir;
	final private String htmlStart = "<html><body>";
	final private String htmlEnd = "</body></html>";
	private byte response[] = "<html><head>Empty</head></html>".getBytes();
	private ArrayList<File> uploadedFiles = new ArrayList<File>();
	private ArrayList<File> enquededFiles = new ArrayList<File>();
	CallBack b;
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		
//		System.out.println(remoteIP + ":" + port + t.getRequestURI().toString() + " was visited by " + t.getRemoteAddress());
		
//		System.out.println("Request: " + t.getRequestMethod());
		
		if(t.getRequestMethod().equals("GET")){
			sendResponse((htmlStart + new String(response) + htmlEnd).getBytes(), t);
			b.callBackFunction(this);
		}
		else if(t.getRequestMethod().equals("POST")){
			InputStream in = t.getRequestBody();
			final HtmlFileHandler ref = this;
			new Thread(new Runnable(){
				@Override
				public void run(){
					byte input[] = null;
					try {
						input = IOUtils.readFully(in, -1, true);
					} catch (IOException e) {
					}
					
					//System.out.println(input);
					sendResponse((htmlStart + new String(response) + htmlEnd).getBytes(), t);
						byte boundry[] = getBoundry(input);
						ArrayList<byte[]> formEntrys = getFormEntrys(input, boundry);
						ArrayList<byte[]> fileEntrys = getFileEntrys(formEntrys);
						System.out.println(fileEntrys.size());
						
						for(int i = 0; i<fileEntrys.size(); i++){
							Pair<String, byte[]> p = getFileEntryBytes(fileEntrys.get(i));
							if(true || p.getKey().contains(".mp3")){   //TODO: Check which files can be handled
								try{
									File f = File.createTempFile("UPLOAD", p.getKey().replace(" ", "_"));
									f.deleteOnExit();
									FileOutputStream fout = new FileOutputStream(f);
									fout.write(p.getValue());
									fout.close();
									enquededFiles.add(f);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						b.callBackFunction(ref);
				}
			}).start();
		}
	}
	
	private byte[] getBoundry(byte[] input){
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int b = 0;
		while ((b=in.read()) > 0){
			if(b == '\n' || b == '\r') break;
			out.write(b);
		}
		
		return out.toByteArray();
	}
	
	private ArrayList<byte[]> getFormEntrys(byte[] input, byte[] boundry){
		
		ArrayList<byte[]> entrys = new ArrayList<byte[]>();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte bufferArray[] = new byte[boundry.length];
		int match = 0;
		for(int i = 0; i<input.length; i++){
			byte check = input[i];
			if(check == boundry[match]){
				bufferArray[match] = input[i];
				match++;
				if(match == boundry.length){
					try {
						os.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					entrys.add(os.toByteArray());
					os.reset();
					match = 0;
				}
			}
			else{
				for(int dequeue = 0; dequeue < match; dequeue++) os.write(bufferArray[dequeue]);
				os.write(check);
				match = 0;
			}
		}
		
		return entrys;
	}
	
	private ArrayList<byte[]> getFileEntrys(ArrayList<byte[]> input){
		ArrayList<byte[]> output = new ArrayList<byte[]>();
		
		for(byte b[] : input){
			String comp = new String(b);
			if(comp.contains("filename")){
				output.add(b);
			}
		}
		
		return output;
	}
	
	private Pair<String, byte[]> getFileEntryBytes(byte[] input){
		String comp = new String(input);
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		String fname = comp.substring(comp.indexOf("filename=\"")+"filename=\"".length());
		fname = fname.substring(0, fname.indexOf("\""));
		if(false && !fname.contains(".mp3")) return new Pair<String, byte[]>("", null);  //TODO: Check which files can be handled
		//</uits:UITS>
		int start[] = new int[2];
		for(int i = 0; i<4; i++){
			start[0] = -1; start[1] = -1;
			while(!(start[0] == '\r' && start[1] == '\n')){
				start[0] = start[1];
				start[1] = in.read();
			}
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while(in.available()>0){
			out.write(in.read());
		}
		Pair<String, byte[]> p = new Pair<String, byte[]>(fname, out.toByteArray());
		//System.err.println(new String(out.toByteArray()).substring(0, 10000));
		return p;
	}
	
	/**
	 * Sendet eine Antwort
	 * Ist nonblocking
	 * @param b byte[] das gesendet werden soll
	 * @param t HttpExchange über den der Austausch läuft
	 * 
	 * @return gibt den sendenden Thread zurück;
	 */
	private Thread sendResponse(byte b[], HttpExchange t){
		try {
			t.sendResponseHeaders(200, b.length);
			OutputStream os = t.getResponseBody();
			Thread sender = new Thread (new Runnable(){
				public void run(){
					try {
						os.write(b);
						os.close();
					} catch (IOException e) {
					}
				}
			});
			sender.start();
			return sender;
		} catch (IOException e) {
		}
		return null;
	}
	/**
	 * 
	 * Sendet eine Antwort
	 * Ist blocking
	 * 
	 * @param b byte[] das gesendet werden soll
	 * @param t HttpExchange über den der Austausch läuft
	 * 
	 */
	private void sendResponseAndWait(byte b[], HttpExchange t){
		try {
			sendResponse(b,t).join();
		} catch (InterruptedException e) {
		}
	}
	
	public void uploadFiles(HttpServer server){
		if(enquededFiles != null && enquededFiles.size() > 0){
			for(File f : enquededFiles){
				uploadedFiles.add(f);
				server.createContext("/" + f.getName(),
						new FileHandler(f, remoteIP, port, true));
//				System.out.println("Uploaded: " + "/" + f.getName());
			}
			enquededFiles.clear();
		}
	}

	/**
	 * Responds with a String
	 * 
	 * @param b
	 *          The handlers CallBack
	 * @param fname
	 * 			The html files path
	 */
	public HtmlFileHandler(String fname, CallBack b, String remoteIP, int port) {
		super();
		if(!fname.contains(".html")) throw new IllegalArgumentException();
		try {
			byte responseArray[] = Files.readAllBytes(Paths.get(fname));
			String molding = new String(responseArray).replace("INPUTADDRESS", "https://"+remoteIP+":"+port+"/fileUpload");
			this.response = molding.getBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(b != null) this.b = b;
		else this.b = new StandardCallBack();
		this.remoteIP = remoteIP;
		this.port = port;
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("aaaaa", "fffff");
		} catch (IOException e) {
		}
		this.tmpDir = Paths.get(tmpFile.getParent());
		tmpFile.delete();
	}
}
