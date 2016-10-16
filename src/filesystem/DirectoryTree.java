package filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import filesystemExceptions.ServerContentNotFoundException;
import filesystemExceptions.ServerDirectoryNotFoundException;

public class DirectoryTree {
	private Directory root;
	private Directory current;
	
	public Directory getCurrent(){
		return this.current;
	}
	
	public Directory getRoot(){
		return this.root;
	}
	
	public void toRoot(){
		this.current = this.root;
	}
	
	public void mkdir(String dirName){
		this.current.mkdir(dirName, current);
	}
	
	public Directory open(String path) throws ServerDirectoryNotFoundException{
		Directory goal = null;
		if(path == null || path.isEmpty()) throw new ServerDirectoryNotFoundException(path);
		if(path.lastIndexOf("/") == path.length()-1 && !path.equals("/")){
			path = path.substring(0, path.length()-1);
		}
		if(path.indexOf("/") == 0){
			path = path.substring(1+root.toString().length());
			if(path.indexOf("/") == 0) path = path.substring(1);
			goal = root;
			if(path.isEmpty()){
				current = goal;
				return goal;
			}
		}else{
			goal = current;
		}
		String steps[] = path.split("/");
		
		for(int i = 0; i<steps.length; i++){
			String next = steps[i];
			if(goal.getChildren().containsKey(next)){
				goal = goal.getChildren().get(next);
			}
			else{
				//for(String s : goal.getChildren().keySet()) System.out.println("ls: " + s + " vs " + next);
				throw new ServerDirectoryNotFoundException(next + " of " + path);
			}
		}
		current = goal;
		return goal;
	}
	
	public byte[] getFile(String path) throws ServerContentNotFoundException, NullPointerException{
		if(path == null) throw new NullPointerException();
		
		if(path.lastIndexOf("/") == path.length()-1)
			path = path.substring(0, path.length()-1);
		if(path.indexOf("/") == 0)
			path = path.substring(1);
		if(path.indexOf(root.toString()) == 0){
			path = path.substring(root.toString().length());
			toRoot();
		}
		if(path.indexOf("/") == 0)
			path = path.substring(1);
				
		String steps[] = path.split("/");
		if(steps[steps.length-1].contains(".")){
			try{
				for(int i = 0; i<steps.length-1; i++){
					String dir = steps[i];
					open(dir);
				}
				if(current.getContent().containsKey(steps[steps.length-1])){
					File f = current.getContent().get(steps[steps.length-1]);
					FileInputStream fin = new FileInputStream(f);
					byte b[] = new byte[fin.available()];
					fin.read(b);
					fin.close();
					return b;
				}
				else{
					throw new ServerContentNotFoundException();
				}
			} catch(ServerDirectoryNotFoundException | IOException e){
				throw new ServerContentNotFoundException(e.getMessage());
			}
		}
		throw new ServerContentNotFoundException();
	}

	public DirectoryTree(String rootName, String fileSystemRoot) {
	    root = new RealDirectory(rootName, fileSystemRoot);
	    current = root;
	}
}
