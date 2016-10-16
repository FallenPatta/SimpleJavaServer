package filesystem;

import java.io.File;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpServer;

import filesystemExceptions.ServerDirectoryNotFoundException;

public class Filesystem {
	private DirectoryTree tree;
	/** Pfad auf Server */
	private String rootname;
	/** Pfad auf Laufwerk */
	private final String fileSystemRoot;
	
	public DirectoryTree getTree(){
		return this.tree;
	}
	
	/**
	 * 
	 * @return root of the server filesystem
	 */
	public String getRootname(){
		return this.rootname;
	}
	
	public String getFSRoot(){
		return this.fileSystemRoot;
	}
	
	/**
	 * Finds a Directory linearly with he filesystem depth
	 * 
	 * @param path path to open. Paths beginning with "/" will be looked up from the filesystems root
	 * @return the Directory path is pointing to or NULL if the path does not exist
	 */
	public Directory open(String path) throws ServerDirectoryNotFoundException{
		tree.open(path);
		return tree.getCurrent();
	}
	
	public void mkdir(String path){
		tree.mkdir(path);
	}

	public Filesystem(String fileSystemRoot, String rootname){
		this.fileSystemRoot = fileSystemRoot;
		this.rootname = rootname;
		tree = new DirectoryTree(rootname, fileSystemRoot);
	}
	
}
