package callbacks;

import java.io.File;
import java.util.Random;

import com.sun.net.httpserver.HttpHandler;

import callbacks.CallBack;
import handlers.TextHandler;

public class AudioCallBack implements CallBack {
	
	private static File[] listOfFiles = null;
	private static int[] durations = null;
	private static String remoteIP = null;
	private static int port;

	public AudioCallBack(File list[], int playtimes[], String remoteAddress, int comPort){
		this.listOfFiles = list;
		this.durations = playtimes;
		this.remoteIP = remoteAddress;
		this.port = comPort;
	}
	
	public static String contextForAudioFile(int i) {
		return 	"<html><head><meta http-equiv=\"refresh\" content=\""
				+ durations[i]
				+ ";url=http://" 
				+ remoteIP + ":" 
				+ port 
				+ "/audio\" /></head>"
				+ "<audio style=\"display:block; margin: 0 auto; width:50%;\" controls=\"true\" autoplay=\"true\" type=\"audio/mp3\" src=\"" 
				+ listOfFiles[i].getName().replace(" ", "_")
				+ "\">player is unsupported by your browser</audio></html>";
	}
	
	public static String contextForAudioFile(int duration, File file) {
		return 	"<html><head><meta http-equiv=\"refresh\" content=\""
				+ duration
				+ ";url=http://" 
				+ remoteIP + ":" 
				+ port 
				+ "/audio\" /></head>"
				+ "<audio style=\"display:block; margin: 0 auto; width:50%;\" controls=\"true\" autoplay=\"true\" type=\"audio/mp3\" src=\"" 
				+ file.getName().replace(" ", "_")
				+ "\">player is unsupported by your browser</audio></html>";
	}
	
	@Override
	public void callBackFunction(HttpHandler handler) {
		if(!(handler instanceof TextHandler)) return;
		TextHandler o = (TextHandler) handler;
		
		if(listOfFiles == null || listOfFiles.length <= 0) return;
		o.response = contextForAudioFile(new Random().nextInt(listOfFiles.length));
	}
}
