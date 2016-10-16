package filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealDirectory implements Directory {
    private String name;
    private Directory parent;
    private Map<String, Directory> children;
    private Map<String, File> contents;
    private String fileSystemAnchor;
    
    @Override
    public String getPath(){
    	if(parent == null){
    		return name;
    	}
    	else return parent.getPath() + "/" + name;
    }
    
    public Map<String, Directory> getChildren(){
    	if(this.children == null) return null;
    	return this.children;
    }
    
    public Map<String, File> getContent(){
    	if(this.contents == null) return null;
    	return this.contents;
    }
    
    public Directory getParent(){
    	return this.parent;
    }
    
    /**
     * Should not be used ATM
     */
    public void addFile(byte[] b, String name){
		File f = new File(getPath() + name);
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(f);
			fout.write(b);
			fout.close();
			this.contents.put(name, f);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Adds an entry into the "contents" HashMap
     * All Spaces will be replaced with underscores (calls: replace(" ", "_") on entry name)
     * 
     * @param f File to put
     */
    public void addFile(File f){
    	if(f == null) throw new NullPointerException();
    	this.contents.put(f.getName().replace(" ", "_"), f);
    }
    
    /**
     * Adds an entry into the "contents" HashMap under a pseudo-name
     * All Spaces will be replaced with underscores (calls: replace(" ", "_") on entry name)
     * 
     * @param f File to put
     * @param pseudo pseudo-name
     */
    public void addFile(File f, String pseudo){
    	if(f == null) throw new NullPointerException();
    	if(pseudo != null && !pseudo.isEmpty()) this.contents.put(pseudo.replace(" ", "_"), f);
    	else this.contents.put(f.getName().replace(" ", "_"), f);
    }
    
    public void mkdir(String dirName, Directory parent){
    	if(dirName == null || parent == null) throw new NullPointerException();
    	if(dirName.isEmpty()) throw new IllegalArgumentException();
    	parent.mkdir(dirName);
    }
    
    public void mkdir(String dirName){
    	if(dirName == null) throw new NullPointerException();
    	if(dirName.isEmpty()) throw new IllegalArgumentException();
    	RealDirectory d = new RealDirectory(dirName, this);
    	this.children.put(dirName, d);
    }
    
  //TODO: Implement Exceptions to throw
    public RealDirectory(String name, String fileSystemAnchor){
    	this.name = name;
    	this.parent = null;
    	this.children = new HashMap<String, Directory>();
    	this.contents = new HashMap<String, File>();
    	this.fileSystemAnchor = fileSystemAnchor;
    }
    
    //TODO: Implement Exceptions to throw
    public RealDirectory(String name, Directory parent){
    	this.name = name;
    	this.parent = parent;
    	this.children = new HashMap<String, Directory>();
    	this.contents = new HashMap<String, File>();
    }
    
    @Override
    public String toString(){
    	return this.name;
    }
}