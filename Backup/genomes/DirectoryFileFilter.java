/*
 * DirectoryFileFilter.java
 *
 * Created on December 22, 2005, 9:51 AM
 */
package genomes;
import java.io.FileFilter;
import java.io.File;
/**
 *
 * @author chris
 */
public class DirectoryFileFilter implements FileFilter {
    
    /** Creates a new instance of DirectoryFileFilter */
    public DirectoryFileFilter() {
    }
    
    public boolean accept(File f){
        if(f.isDirectory()) return true;
        else return false;
    }
}
