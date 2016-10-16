package handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FileHandler implements HttpHandler {
	
	private static Clip clip;
	private static boolean audioRunning = false;
	private static boolean muted = false;
	private static String remoteIP = null;
	private static int port = 8080;
	
    public static void play(byte[] data) throws LineUnavailableException, UnsupportedAudioFileException, IOException
    {

    try
    {   
    		if(clip != null && clip.isOpen()){
    			clip.close();
    			System.out.println("Starting new Clip");
    		}
	        // read the  file
	        AudioInputStream rawInput = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
	
	        // decode mp3
	        AudioFormat baseFormat = rawInput.getFormat();
	        AudioFormat decodedFormat = new AudioFormat(
	            AudioFormat.Encoding.PCM_SIGNED, 	// Encoding to use
	            baseFormat.getSampleRate(),   		// sample rate (same as base format)
	            16,               					// sample size in bits (thx to Javazoom)
	            baseFormat.getChannels(),     		// # of Channels
	            baseFormat.getChannels()*2,   		// Frame Size
	            baseFormat.getSampleRate(),  		// Frame Rate
	            true                 				// Little Endian
	        );
	        AudioInputStream decodedInput = AudioSystem.getAudioInputStream(decodedFormat, rawInput);
	        
	        AudioFormat format = decodedInput.getFormat();
	        DataLine.Info info = new DataLine.Info(Clip.class, format);
	        clip = (Clip)AudioSystem.getLine(info);
	        clip.open(decodedInput);
	        
	        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
	        gainControl.setValue(-10.0f);
	        
	        clip.start();
	        clip.drain();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e1)
        {
            e1.printStackTrace();
        }
    }
    
    public static byte[] loadFileFromURL(String url){
		URL fileURL;
		InputStream input;
		try {
			if(url.contains(remoteIP)){
				fileURL = new URL(url);
				input = fileURL.openStream();
			}
			else{
				input = Files.newInputStream(Paths.get(url));
			}
		
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = input.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			return buffer.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    

	final private File file;

	@Override
	public void handle(HttpExchange t) throws IOException {
		
		//System.out.println(t.getLocalAddress() + t.getRequestURI().toString() + " was visited by " + t.getRemoteAddress());
		
		File filePath = this.file;
		Path p = Paths.get(filePath.getAbsolutePath());
		byte b[] = Files.readAllBytes(p);

		t.sendResponseHeaders(200, b.length);
		OutputStream os = t.getResponseBody();
		Thread sender = new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					os.write(b);
					os.close();
				} catch (IOException e) {
				}
			}
			
		});
		sender.start();
			if(!muted && !audioRunning){
				audioRunning = true;
				new Thread(new Runnable(){
					public void run() {
						try {
							System.out.println("starting to play: " + filePath.getAbsolutePath().split("/")[filePath.getAbsolutePath().split("/").length-1]);
							play(loadFileFromURL("http://"+remoteIP+":"+port+"/"+filePath.getAbsolutePath().split("/")[filePath.getAbsolutePath().split("/").length-1].replace(" ", "_")));
						} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}else{
				if(muted) System.out.println("player was called but is muted");
				else System.err.println("This player is already running");
			}
		
	}

	public FileHandler(File f, String remoteIP, int port, boolean muted) {
		super();
		this.file = f;
		this.remoteIP = remoteIP;
		this.port = port;
		this.muted = muted;
	}

}
