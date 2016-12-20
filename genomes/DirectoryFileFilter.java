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
        
    public boolean accept(File f){
    	
    	return f.isDirectory();
    }
}
