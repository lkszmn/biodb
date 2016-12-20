/*
 * Main.java
 *
 * Created on December 20, 2005, 9:44 AM
 */
package genomes;

import java.io.File;
import java.util.LinkedList;


/**
 *
 * @author chris
 * 
 * Version 2.0 Updated by Joern Marialke 
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        // System.out.println("Running Main Class !");
        boolean update = false;
        boolean format = false;
        boolean search = false;
        boolean web    = false;
        boolean wdebug = false;
        boolean dbs_by_taxids = false;
        boolean runcheck = false;
        boolean chdir    = false;
        LinkedList<Integer> taxids = new LinkedList<Integer>();
        // do not create a log file in case of web-usage.
        // toolkit-user has no write permissions for log files
        boolean nolog  = false;
        // set mode - dna or peptide
        Config.MODE datamode  = Config.MODE.UNINITIALIZED;
        
        // set reportmode - all, debug, files, taxids
        // ALL is default
        Config.REPORT reportmode = Config.REPORT.ALL;
        String exp = "";
        String oldpath ="";
        String newpath = "";
        File configfile = null;
        
        if(args.length==0){
            showHelp();
            System.exit(1);
        }
        
        for(int i=0; i<args.length; ++i){
            final String opt = args[i];
            if(opt.equals("-u"))    { update     = true;  System.out.println("Updating Databases  !");          continue; }
            if(opt.equals("-f"))    { format     = true;  System.out.println("Formatting Data     !");           continue; }
            if(opt.equals("-w"))    { web        = true;  System.out.println("Writing Web Content !");           continue; }
            if(opt.equals("-wdebug")){ wdebug    = true;           continue; }
            
            
            if(opt.equals("-nolog")){ nolog      = true;           continue; }
            if(opt.equals("-pep"))  { datamode   = Config.MODE.PEPTIDE;   continue; }
            if(opt.equals("-dna"))  { datamode   = Config.MODE.DNA;       continue; }
            if(opt.equals("-taxid")){ reportmode = Config.REPORT.TAXIDS;  continue; }
            if(opt.equals("-all"))  { reportmode = Config.REPORT.ALL;     continue; }
            if(opt.equals("-debug")){ reportmode = Config.REPORT.DEBUG;   continue; }
            if(opt.equals("-files")){ reportmode = Config.REPORT.FILES;   continue; }
            if(opt.equals("-runcheck")){ runcheck = true; continue; }
            
            if(opt.equals("-s")) {
                search = true;
                i++;
                if( !(i<args.length) ){
                    System.err.println("ERROR, no expression found for '-s'");
                    System.exit(2);
                }
                exp = args[i];
                continue;
            }
            
            
            if(opt.equals("-c")) {
                i++;
                if( !(i<args.length) ){
                    System.err.println("ERROR, no file found for '-c'");
                    System.exit(2);
                }
                configfile = new File(args[i]);
                if( !configfile.exists() ){
                    System.err.println("ERROR, file " + configfile.toString() + " does not exist.");
                    System.exit(2);
                }
                continue;
            }
            
            
            if(opt.equals("-chdir")) {
                chdir = true;
                if( !((i+2)<args.length) ){
                    System.err.println("ERROR, please enter old-path and new-path for -chdir!");
                    System.exit(2);
                }
                oldpath = args[++i];
                newpath = args[++i];
                continue;
            }
            
            if(opt.equals("-dbs")) {
                dbs_by_taxids = true;
                // capture the rest of the line
                for(++i; i<args.length; i++){
                    taxids.add(Integer.parseInt(args[i]));
                }
                break;
            }
            System.err.println("ERROR: Unknown parameter "+opt);
            System.exit(1);
            
            
        }
        
        //check the configfile
        if( configfile==null ){
            String path = ClassLoader.getSystemResource("genomes").getPath();
            if( path.startsWith("file:") ) path = path.substring(5);
            if( path.contains(".jar!/") ) path = new File(path).getParentFile().getParent();
            
            configfile = new File(path, "config.txt");
            if( !configfile.exists() ){
                System.err.println("File "+configfile.toString() +" does not exist.");
                configfile = new File("./config.txt");
            }
            if( !configfile.exists() ){
                System.err.println("File "+configfile.toString() +" does not exist.");
                System.err.println("Please specifiy the configfile with the -c option at the command line.");
                System.exit(1);
            }
        }
        
        // check wether the peptide or the dna tree should be used for searching
        if( (search || dbs_by_taxids) && datamode==Config.MODE.UNINITIALIZED && !(reportmode==Config.REPORT.DEBUG || reportmode==Config.REPORT.ALL)) {
            System.err.println("ERROR: Please specify -pep or -dna flag for your search!");
            System.exit(3);
        }
        
        Config c = new Config(nolog, datamode, reportmode, configfile);
        c.DEBUG = wdebug;
        Builder b = new Builder(c);

        if( update ){
        	 System.out.println("MAIN L155 Update \n");
        	b.update();
        }
        
        if( format ) {
        	System.out.println("MAIN L155 Format \n");
        	b.formatAll();
        }
        // System.out.println("MAIN L157 IF (web && format || update) \n");
        if( web && (format || update) ) new HTMLGenTree( b.getDB(), c);
        else if (web) new HTMLGenTree(c);
        
        if(wdebug) new HTMLGenTree(c);
        
        
        if( search ) {
        	System.out.println("MAIN L166 "+exp+"\n");
            Search s = new Search(c);
            
            String res = s.search(exp);
           System.out.println("MAIN L 169 RESULTS : "+res+"\n");
        }
        
        if( dbs_by_taxids ){
            Search s = new Search(c);
            System.out.println(s.getFiles(taxids));
	    if (!nolog) {
		c.log("MAIN L 176 TAXIDs RESULTS\n");

		GenomesDB db = GenomesDB.load( c.DBFILE );
		Genome test = db.get(taxids.get(0));
		c.log("Xmpl " + taxids.get(0) + " " + test.hasAA() + " " + test.getDataDir() + "\n");
	    }
        }
        
        if( chdir ){
            GenomesDB db = GenomesDB.load( c.DBFILE );
            db.changeDirs(oldpath, newpath);
            db.save(c.DBFILE);
            db.printFull();
        }

        if( runcheck ){
            GenomesDB db = GenomesDB.load( c.DBFILE );
            db.runcheck( new java.io.OutputStreamWriter(System.out) );
            Search s = new Search(c);
            
            System.out.println("There are " + (s.countMatches("Archaea")) + " archaeas in the db.");
            System.out.println("There are " + (s.countMatches("Bacteria")) + " bacterias in the db.");
            System.out.println("There are " + (s.countMatches("Eukaryota")) + " eukaryotes in the db.");
            System.out.println("There are " + (s.countMatches("!Archaea&!Bacteria&!Eukaryota")) + " others in the db.");
        }
	c.log("Logfile is "+c.LOGFILE+"\n");
    }
    
    
    public static void showHelp(){
        System.err.println("Usage: java -jar -server -Xmx2G genomes.jar -u -f -html -s  'expr' ");
        System.err.println("-u                    Create genomeDB");
        System.err.println("-f                    Format all fasta files in genomeDB");
        System.err.println("-w                    Create web files");
        System.err.println("-c 'file'             Specifiy configfile");
        System.err.println("-s 'expr'             Search all genomes for (boolean expression) expr");
        System.err.println("  -files              Search prints files");
        System.err.println("  -taxid              Search prints taxids");
        System.err.println("  -debug              Search prints all data of one genome");
        System.err.println("  -all                (default) Search prints scientific name + taxid + count");
        System.err.println("  -nolog              do not write a log-file (for web usage)");
        System.err.println("-runcheck             do some basic checks");
        System.err.println("-chdir 'old' 'new'    change old to new in pathnames in the db");
        System.err.println("-pep                  Use the protein tree (reports protein files for taxids/searches)");
        System.err.println("-dna                  Use the dna tree (reports dna files for taxids/searches)");
        System.err.println("-dbs  taxid1 taxid2 ...  Get files for taxids, all following arguments are considered to be taxids. Requires -pep/-dna.");
        System.err.println("Examples:");
        System.err.println("java -jar -server -Xmx2G genomes.jar -s 'archaea|(bacteria&!proteobacteria)|man|yeast' ");
        System.err.println("java -jar -server -Xmx2G genomes.jar -u -f ");
    }
}
