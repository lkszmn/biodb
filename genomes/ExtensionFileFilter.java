/*
 * ExtensionFileFilter.java
 *
 * Created on December 22, 2005, 12:02 PM
 */
package genomes;
import java.io.FileFilter;
import java.io.File;
/**
 *
 * @author chris
 */
public class ExtensionFileFilter implements FileFilter {
    
    /** Creates a new instance of ExtensionFileFilter */
    private final String ext;
    private final String exts[];
    
    public ExtensionFileFilter(String ext) {
        this.exts = null;
        this.ext  = ext;
    }
    
    public ExtensionFileFilter(String[] exts){
        this.exts = exts;
        this.ext  = null;
    }
     public boolean accept(File f){
         if(ext==null){
            for(int i=0; i<exts.length; ++i){
                if(f.toString().endsWith(exts[i])) return true;
            }
            return false;
         } else{
        	 return f.toString().endsWith(ext);
         }
    }
}
