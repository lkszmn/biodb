/*
 * Builder.java
 *
 * Created on December 20, 2005, 1:58 PM
 */
package genomes;

import java.io.File;
import java.util.LinkedList;
import java.util.Iterator;
/**
 * @author chris 2005
 * 
 * @updated Joern Marialke 2011, MPI for Evolutionay Biology Tuebingen
 *
 */
public class Builder {
    
    
    private final Config c;
    
    private dmpReader r;
    
    private GenomesDB db;
    
    private LinkedList<Exception> excepts;
    
    
    /** Creates a new instance of Builder */
    public Builder(Config c) throws Exception{
        excepts = new LinkedList<Exception>();
        this.c = c;
        db = null;
    }
    
    public void update() throws Exception{
    	System.out.println("Builder L37 Running update\n");
    	System.out.println("Builder L38 Initializing new dmpReader\n");
        r = new dmpReader(c);
        
        System.out.println("Builder L41 Initializing new GenomesDB\n");
        db = new GenomesDB();
        
        try{
            //highest priority
        	System.out.println("Builder L46 Dumping Ensembl Dir\n");
            int c1 = dump(c.ENSEMBL_DIR);
            System.out.println("Builder L48 Dumping NCBI EUK Dir\n");
            //get all euks from ncbi, do not merge or override ensembl genomes
            int c2 = dump(c.NCBI_EUKARYOTA_DIST_DIR);
            System.out.println("Builder L51 Dumping NCBI FUN Dir\n");
            //get all fungis from ncbi, do not merge or override existing genomes
            int c3 = dump(c.NCBI_FUNGI_DIST_DIR);
            System.out.println("Builder L54 Dumping NCBI BAC\n");
            //bacterial genomes might be merged
            int c4 = mergedump(c.NCBI_BACTERIA_DIST_DIR);
            System.out.println("Builder L57 Validate DB\n");
            db.validate(c);
            db.save( c.DBFILE );
            
            c.log("\n+-----------Statistics-------------+\n");
            c.log(" "+c1+" ENSEMBL EUKARYOTIC GENOMES\n");
            c.log(" "+c2+" NCBI EUKARYOTIC GENOMES\n");
            c.log(" "+c3+" NCBI FUNGI GENOMES\n");
            c.log(" "+c4+" NCBI PROKARYOTIC GENOMES\n");
            c.log("+---------------------------------+\n");
        }catch(Exception e){
            
            e.printStackTrace();
           // System.out.println(e);
        }
        
        printErrors();
        excepts = new LinkedList<Exception>();
    }
    
    public void formatAll() throws Exception{
        
        if( db==null ) db=GenomesDB.load(c.DBFILE);
        // delete dataDir tree
        File rem[] = c.DATA_DIR.listFiles();
        for(int i=0; i<rem.length; ++i){
            rmrf(rem[i]);
        }
        
        //format all fasta files
        db.formatdb(c);
        
        // save db with formatted dna and aa files!
        db.save( c.DBFILE );
    }
    
    public GenomesDB getDB(){ return db; }
    
    private void printErrors() throws Exception{
        Iterator<Exception> it = excepts.iterator();
        while(it.hasNext()){
            c.log("ERROR: "+it.next().getMessage()+"\n");
        }
    }
    
    /**
     * 
     * @param f The Directory containing all your disfiles (either Ensemble, NCBI.Fungy, NCBI.Eucaryota)
     * @return
     * @throws Exception
     */
    private int dump(File f) throws Exception{
        int count = 0; // Counts all Sub folders (Organisms) 
        File orgDirs[] = f.listFiles(new DirectoryFileFilter());  // @Array containing all subdirs (Organisms) in your distfiles
        // For each Subfolder do something
        for(int i=0; i<orgDirs.length; ++i){
            try{
                c.log(orgDirs[i].toString());
                System.out.println("Builder L115 Dump : "+orgDirs[i].toString()+"\n");
                // Create a new Genome with the Params (Directory, dmpReader r, Config c)
                Genome g = new Genome(orgDirs[i],r,c);
                c.log(g.toString());
                if( isMasked(orgDirs[i]) ){
                    c.log("Directory "+orgDirs[i]+" is masked!\n");
                    continue;
                }
                if( db.contains(g) ){
                    c.log("Genome ("+g.getSciName()+","+g.getTaxid()+") with higher priority already in db, ignoring files in\n");
                    c.log("             " + orgDirs[i] + "\n");
                }else{
                    db.add(g);
                    count++;
                    c.log("Genome saved: " + g.getSciName() + "\n");
                    c.log("              " + g.getTaxid()   + "\n");
                    c.log("              " + g.getDataDir() + "\n");
                    c.log("              " + orgDirs[i]     + "\n");
                }
            }catch(Exception e){
                c.log("ERROR: "+e+"\n");
                excepts.add(e);
            }
        }
        return count;
    }
    
    private int mergedump(File f) throws Exception{
        int count = 0;
        File orgDirs[] = f.listFiles(new DirectoryFileFilter());
        for(int i=0; i<orgDirs.length; ++i){
            try{
                Genome g = new Genome(orgDirs[i],r,c);
                if( db.contains(g) ){
                    db.mergeAdd(g);
                    c.log("Genome ("+g.getSciName()+","+g.getTaxid()+") already in db, merging files in\n");
                    c.log("             " + orgDirs[i] + "\n");
                }else{
                    db.add(g);
                    count++;
                    c.log("Genome saved: " + g.getSciName() + "\n");
                    c.log("              " + g.getTaxid()   + "\n");
                    c.log("              " + g.getDataDir() + "\n");
                    c.log("              " + orgDirs[i]     + "\n");
                }
            }catch(Exception e){
                c.log("ERROR: "+e);
                excepts.add(e);
            }
        }
        return count;
    }
    
    private void rmrf(File f){
        if(f.isFile()) f.delete();
        if(f.isDirectory()) {
            File dirs[] = f.listFiles();
            for(int i=0; i< dirs.length; ++i)
                rmrf(dirs[i]);
        }
        f.delete();
    }
    
    private boolean isMasked(File f) throws Exception{
        Iterator<String> it = c.MASKED_DIRS.keySet().iterator();
        while( it.hasNext() ){
            String mask = it.next();
            //  c.log("MASKED ="+f.getCanonicalPath()+"=?="+mask+"=\n");
            String filename = f.getCanonicalPath();
            if( filename.contains(mask) )  return true;
        }
        return false;
    }
    
}
