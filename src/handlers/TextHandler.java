package handlers;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import callbacks.CallBack;

public class TextHandler implements HttpHandler {
	public String response = "<html><head>Empty</head></html>";
	CallBack b;
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		
//		System.out.println(t.getLocalAddress() + t.getRequestURI().toString() + " was visited by " + t.getRemoteAddress());
		
		t.sendResponseHeaders(200, response.length());
		OutputStream os = t.getResponseBody();
		TextHandler reference = this;
		Thread sender = new Thread (new Runnable(){
			public void run(){
				try {
					os.write(response.getBytes());
					os.close();
				} catch (IOException e) {
				}
				b.callBackFunction(reference);
			}
		});
		sender.start();
	}

	/**
	 * Responds with a String
	 * 
	 * @param response
	 *            The Response
	 */
	public TextHandler(String response, CallBack b) {
		super();
		this.response = response;
		this.b = b;
	}
}
