package callbacks;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import handlers.HtmlFileHandler;

public class HtmlUploaderCallBack implements CallBack
{
	final HttpServer server;

	@Override
	public void callBackFunction(HttpHandler o) {
		if(!o.getClass().equals(HtmlFileHandler.class)) return;
		HtmlFileHandler handler = (HtmlFileHandler)o;
		handler.uploadFiles(server);
	}
	
	public HtmlUploaderCallBack(HttpServer server){
		this.server = server;
	}

}
