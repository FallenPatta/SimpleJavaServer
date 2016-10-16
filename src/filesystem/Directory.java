package filesystem;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface Directory {
    
    public abstract String getPath();
    
    public abstract Map<String, Directory> getChildren();
    
    public abstract  Map<String, File> getContent();
    
    public abstract Directory getParent();
    
    public abstract void addFile(byte[] b, String name);
    
    /**
     * 
     * @param f			File to add
     * @param toRoot	add to root, or add to tmp files?
     */
    public abstract void addFile(File f);
    
    public abstract void addFile(File f, String pseudo);
    
    public abstract void mkdir(String dirName, Directory parent);
    
    public abstract void mkdir(String dirName);
    
    //public Directory makeRoot(String name, String fileSystemAnchor);
    
}
