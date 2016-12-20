/*
 * Config.java 
 *
 * Created on December 20, 2005, 12:40 PM
 */

package genomes;
import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileReader;
import java .io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/**
 *
 * @author chris
 */
public class Config {
    
    public enum MODE {UNINITIALIZED, PEPTIDE, DNA};
    public enum REPORT {UNINITIALIZED, ALL, DEBUG, FILES, TAXIDS};
    
    public  File BASE_DIR;
    public  File DATA_DIR;
    public  File LOG_DIR;
    public  File DIST_DIR;
    public  File TAX_DIR;
    public  File WEB_DIR;
    
    public  File NCBI_DIST_DIR;
    public  File NCBI_FUNGI_DIST_DIR;
    public  File NCBI_BACTERIA_DIST_DIR;
    public  File NCBI_EUKARYOTA_DIST_DIR;
    public  File NCBI_ARCHAEA_DIST_DIR;
    public  File NCBI_VIRAL_DIST_DIR;
    
    public  File ENSEMBL_DIR;
    public  File NAMES_DMP;
    public  File NODES_DMP;
    public  File GIPROT2TAXID;
    
    public String GZIP_CMD;
    public String FORMATDB_CMD;
    public File DBFILE;
    public File LOGFILE;
    
    public MODE DATA_MODE;
    public REPORT REPORT_MODE;
    
    public HashMap<String, String> MASKED_DIRS;
    
    private BufferedWriter log;
    
    public boolean DEBUG;
    public boolean ULTRA_COMPRESS;
    public int ULTRA_COMPRESS_LEVEL;
    
    public Config(boolean nolog, MODE m, REPORT r, File configfile) throws Exception{
        DATA_MODE = m;
        REPORT_MODE = r;
        ULTRA_COMPRESS = false;
        ULTRA_COMPRESS_LEVEL = 4;
        
        MASKED_DIRS = new HashMap<String,String>();
        readConfig(configfile);
        
        DATA_DIR = new File(BASE_DIR, "data");
        LOG_DIR  = new File(BASE_DIR, "log");
        DIST_DIR = new File(BASE_DIR, "distfiles");
        TAX_DIR  = new File(BASE_DIR, "taxonomy");
        WEB_DIR  = new File(BASE_DIR, "web");
        
        LOG_DIR.mkdir();
        DATA_DIR.mkdir();
        WEB_DIR.mkdir();
        
        
        NCBI_DIST_DIR = new File(DIST_DIR, "ncbi");
        NCBI_FUNGI_DIST_DIR = new File(NCBI_DIST_DIR, "Fungi");
        NCBI_BACTERIA_DIST_DIR = new File(NCBI_DIST_DIR, "Bacteria");
        NCBI_EUKARYOTA_DIST_DIR = new File(NCBI_DIST_DIR, "Eukaryota");
        NCBI_ARCHAEA_DIST_DIR = new File(NCBI_DIST_DIR, "Archaea");
        NCBI_VIRAL_DIST_DIR = new File(NCBI_DIST_DIR, "Viral");

        
        ENSEMBL_DIR = new File(DIST_DIR, "ensembl");
        System.out.println(ENSEMBL_DIR);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
        String ts = sdf.format(Calendar.getInstance().getTime());
        LOGFILE = new File(LOG_DIR, ts + "_java.log" );
        File formatdblog =  new File(LOG_DIR, ts + "_formatdb.log" );
        
        FORMATDB_CMD += " -l " + formatdblog.toString();
        
        if(nolog) {
            // log = new BufferedWriter(new OutputStreamWriter( System.err ));
	    log = null;
	} else {
            log = new BufferedWriter(new FileWriter( LOGFILE ));
        
	    log.write("L96 Config: CONFIGFILE: "+configfile.toString()+"\n");
	}
    }
    
    private void readConfig(File configfile) throws Exception{
        BufferedReader R = new BufferedReader( new FileReader(configfile) );
        String line;
        Pattern pat = Pattern.compile("\\s*(.+?)\\s*=\\s*(.+?)\\s*$");
        while( null!=(line=R.readLine()) ){
            if( line.startsWith("#") ) continue;
            Matcher  mat = pat.matcher(line);
            if( mat.matches() ){
                if( mat.group(1).equalsIgnoreCase("BASE_DIR") )
                    BASE_DIR = new File(mat.group(2));
                else if( mat.group(1).equalsIgnoreCase("DBFILE") )
                    DBFILE = new File(mat.group(2));
                else if( mat.group(1).equalsIgnoreCase("GZIP_CMD") )
                    GZIP_CMD = mat.group(2);
                else if( mat.group(1).equalsIgnoreCase("FORMATDB_CMD") )
                    FORMATDB_CMD = mat.group(2);
                else if( mat.group(1).equalsIgnoreCase("MASKED") )
                    MASKED_DIRS.put(mat.group(2), " ");
                else if( mat.group(1).equalsIgnoreCase("NAMES_DMP") )
                    NAMES_DMP = new File(mat.group(2));
                else if( mat.group(1).equalsIgnoreCase("NODES_DMP") )
                    NODES_DMP = new File(mat.group(2));
                else if( mat.group(1).equalsIgnoreCase("GIPROT2TAXID") )
                    GIPROT2TAXID = new File(mat.group(2));
                else if( mat.group(1).equalsIgnoreCase("ULTRA_COMPRESS") && ( mat.group(2).equalsIgnoreCase("y") || mat.group(2).equalsIgnoreCase("1") ) )
                    ULTRA_COMPRESS = true;
                else if( mat.group(1).equalsIgnoreCase("ULTRA_COMPRESS_LEVEL") )
                    ULTRA_COMPRESS_LEVEL = Integer.parseInt(mat.group(2));
            }
        }
	R.close();
    }
    
    
    public void log(final String s) throws Exception{
        // System.out.print(s);
	if (null != log) {
	    log.write(s);
	    log.flush();
	}
    }
    
    
}
