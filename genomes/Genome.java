/*
 * Genome.java
 *
 * Created on December 20, 2005, 12:10 PM
 */

package genomes;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Vector;
import java.util.LinkedList;
/**
 *
 * @author chris
 * HAS TO BE FIXED !!!!
 */
public class Genome implements Serializable{
    
    static final long serialVersionUID = 222L;
    
    public enum TYPE {NCBI_BACTERIA, NCBI_FUNGI, NCBI_EUKARYOTA, NCBI_ARCHAEA, NCBI_VIRAL,ENSEMBL};
    
    private Vector<String> lineage; //root to species
    private HashMap<String,String> names;
    private Vector<Integer> taxidLineage;
    private final String sciName;
    private final Integer taxid;
    private LinkedList<File> distDir;
    private File dataDir;
    private Vector<File> dna;
    private Vector<File> aa;
    private final TYPE type;
    private final String gzip;
    private final String formatdb;
    
    
    
    /** Creates a new instance of Genome */
    public Genome(File distDir, dmpReader r, Config c, int taxid) throws Exception {
        this.distDir = new LinkedList<File>();
        this.distDir.add(distDir);
        this.type = getType(c);
        this.taxid = taxid;
        this.names = r.getNames(taxid);
        this.sciName = new String(names.get("#SCIENTIFICNAME#"));
        this.lineage = r.getLineageVec(taxid);
        this.taxidLineage = r.getTaxidVec(taxid);
        this.dataDir      = getDataDir(c);
        this.gzip = c.GZIP_CMD;
        this.formatdb = c.FORMATDB_CMD;
        this.aa = new Vector<File>();
        this.dna = new Vector<File>();
    }
    
    public Genome(Genome g) throws Exception {
        this.distDir = new LinkedList<File>();
        Iterator<File> it = g.distDir.iterator();
        this.distDir.add( new File( new String(it.next().toString())) );
        this.type = g.type;
        this.taxid = new Integer(g.taxid.intValue());
        HashMap<String, String> hm = new HashMap<String, String>();
        Iterator<String> names_it = g.names.keySet().iterator();
        while ( names_it.hasNext() ) {
            String cp = names_it.next();         
            hm.put(cp, g.names.get(cp));
        }     
        this.names = hm;
        this.sciName = this.names.get("#SCIENTIFICNAME#");
        this.lineage = realStringClone(g.lineage);
        this.taxidLineage = realIntegerClone(g.taxidLineage);
        this.dataDir      = new File(g.dataDir.toString());
        this.gzip = g.gzip;
        this.formatdb = g.formatdb;
        this.aa = realFileClone(g.aa);
        this.dna = realFileClone(g.dna);
        
    }
    
    private Vector<Integer> realIntegerClone(Vector<Integer> input) {
        Vector<Integer> res = new Vector<Integer>();
        Iterator<Integer> it = input.iterator();
        while ( it.hasNext() ) {
            res.add( new Integer(it.next().intValue()));
        }
        return res;
    }
    private Vector<String> realStringClone(Vector<String> input) {
        Vector<String> res = new Vector<String>();
        Iterator<String> it = input.iterator();
        while ( it.hasNext() ) {
            res.add( new String(it.next()));
        }
        return res;
    }
    private Vector<File> realFileClone(Vector<File> input) {
        Vector<File> res = new Vector<File>();
        Iterator<File> it = input.iterator();
        while ( it.hasNext() ) {
            res.add( new File(it.next().toString()));
        }
        return res;
    }

  
 
      
    
    // determines from where this genome originates
    private TYPE getType(Config c) throws Exception {
        if ( distDir.get(0).toString().startsWith(c.ENSEMBL_DIR.toString()) ) {
            return TYPE.ENSEMBL;
        } else if ( distDir.get(0).toString().startsWith(c.NCBI_FUNGI_DIST_DIR.toString()) ) {
            return TYPE.NCBI_FUNGI;
        } else if ( distDir.get(0).toString().startsWith(c.NCBI_BACTERIA_DIST_DIR.toString()) ) {
            return TYPE.NCBI_BACTERIA;
        } else if ( distDir.get(0).toString().startsWith(c.NCBI_EUKARYOTA_DIST_DIR.toString()) ) {
            return TYPE.NCBI_EUKARYOTA;
        } else if ( distDir.get(0).toString().startsWith(c.NCBI_ARCHAEA_DIST_DIR.toString()) ) {
            return TYPE.NCBI_ARCHAEA;
        }else if ( distDir.get(0).toString().startsWith(c.NCBI_VIRAL_DIST_DIR.toString()) ) {
            return TYPE.NCBI_VIRAL;
        }else {
	    throw new Exception("Cannot evaluate type of '"+distDir.get(0).toString()+"'");
	}
    }

        
    // create directory name for formatdb files of this organism
    private File getDataDir(Config c) {
        String fname = sciName.toLowerCase();
        fname = fname.replaceAll("[^a-zA-Z1-9_]+","_");
        fname = fname.replaceAll("_*$","");      
        String subdir = lineage.get(2);
        subdir = subdir.replaceAll("[^a-zA-Z1-9_]+","_");
        subdir = subdir.replaceAll("_*$","");
        return new File(c.DATA_DIR, subdir+"/"+fname);
    }
    
    
    private void formatNCBIFungi(File f) throws Exception {
        File prot[] = f.listFiles();
        for (int i=0; i<prot.length; ++i) {
            String n = prot[i].getName();
            File target = new File(dataDir, n);
            format(prot[i], target, "prot");
            this.aa.add(target);
        }
    }
    private void formatNCBIViral(File f) throws Exception {
        File prot[] = f.listFiles();
        for (int i=0; i < prot.length; ++i) {
            String n = prot[i].getName();
            File target = new File(dataDir, n);
            format(prot[i], target, "prot");
            this.aa.add(target);
        }
    }
    
    private void formatNCBIEukaryota(File f) throws Exception {
        File dirs[] = f.listFiles();
        for (int i=0; i<dirs.length; ++i) {
            if ( dirs[i].isDirectory() ) {
                File files[] = dirs[i].listFiles();
                for (int j=0; j<files.length; ++j) {
                    String n ="";
                    if (files[j].getName().endsWith(".gz")) {
			n = files[j].getName().substring(0, files[j].getName().lastIndexOf('.'));
		    }
                    else {
			n = files[j].getName();
		    }
                    
                    File target = new File(dataDir, n);
                    format(files[j], target, "prot");
                    this.aa.add(target);
                }
            }else{
            // e.g. INFLUENZA no subdirs
                if ( dirs[i].toString().endsWith(".faa") ) {
                    File target = new File(dataDir, dirs[i].getName());
                    format(dirs[i], target, "prot");
                    this.aa.add(target);
                }
            }
        }
    }
    
    private void formatENSEMBLE(File f) throws Exception {
        File prot[] = f.listFiles(new ExtensionFileFilter(".fa"));
        for (int i=0; i<prot.length; ++i) {
            String n = prot[i].getName().substring(0, prot[i].getName().lastIndexOf('.'));
            File target = new File(dataDir, n);
            format(prot[i], target, "prot");
            this.aa.add(target);
        }
    }
    
    
    private void formatNCBIBacteriaArchaea(File f) throws Exception {
        File prot[] = f.listFiles(new ExtensionFileFilter(".faa"));
        for (int i=0; i<prot.length; ++i) {
            String n = prot[i].getName();
            File target = new File(dataDir, n);
            format(prot[i], target, "prot");
            this.aa.add(target);
        }
        File dna[] = f.listFiles(new ExtensionFileFilter(".fna"));
        for (int i=0; i<dna.length; ++i) {
            String n = dna[i].getName();
            File target = new File(dataDir, n);
            format(dna[i], target, "nucl");
            this.dna.add(target);
        }
    }
    
    
    
    private void format(File src, File targ, String dbtype)throws Exception {

	String cmd = "cat "+src+" | "+formatdb+" -in stdin -dbtype "+dbtype+" -out "+targ;
        if (src.toString().endsWith(".gz")) {
            // or better using zcat?
            cmd = "gzip -d -c "+src+" | "+formatdb+" -in stdin -dbtype "+dbtype+" -out "+targ;
        }

        BashWrapper bw = new BashWrapper(cmd);
        int i = bw.call();
        if ( i!=0 ) throw new Exception("Error on execution of: "+cmd+"\n"); 
        bw.destroy();
        // copy src file to target file
        cmd = "cp "+src+" "+targ;
        if (src.toString().endsWith(".gz")) {
            // or better using zcat?
            cmd = "gzip -d -c "+src+" > "+targ;
        }
        bw = new BashWrapper(cmd);
        i = bw.call();
        if ( i!=0 ) throw new Exception("Error on execution of: "+cmd+"\n"); 
        if (i==0) {
        	bw.destroy();
        }
    }
    
    
    public Vector<Integer> getTaxidLineage() { return taxidLineage; }
    
    public Vector<String> getLineage() { return lineage; }
    
    public Integer getTaxid() { return taxid; }
    
    public String getSciName() { return sciName; }
    
    // merges two genomes (bacteria)
    public void merge(Genome g) { this.distDir.addAll(g.distDir); }
    
    public File getDataDir() { return dataDir; }
    
    public void formatGenome() throws Exception {
        dataDir.mkdirs();
        Iterator<File> it = distDir.iterator();
        while (it.hasNext()) {
            if (this.type == TYPE.ENSEMBL) {
                formatENSEMBLE(it.next());
            } else if (this.type == TYPE.NCBI_BACTERIA) {
                formatNCBIBacteriaArchaea(it.next());
            } else if (this.type == TYPE.NCBI_FUNGI) {
                formatNCBIFungi(it.next());
            } else if (this.type == TYPE.NCBI_EUKARYOTA) {
                formatNCBIEukaryota(it.next());
            }else if (this.type == TYPE.NCBI_ARCHAEA) {
                formatNCBIBacteriaArchaea(it.next());
            }else if (this.type == TYPE.NCBI_VIRAL) {
                formatNCBIViral(it.next());
            }
        }
    }
    
    public boolean matches(String s) {
        s = s.trim();
        if (names.containsKey(s.toLowerCase())) return true;
        for (int i=0; i<lineage.size(); ++i)
            if ( lineage.get(i).equalsIgnoreCase(s) ) return true;
        return false;
    }
    
    public boolean substring_matches(String s) {
        s = s.trim();
        Iterator<String> it = names.keySet().iterator();
        while (it.hasNext()) {
            final String n = it.next();
            if ( n.toLowerCase().contains(s.toLowerCase()) ) return true;
        }
        for (int i=0; i<lineage.size(); ++i)
            if ( lineage.get(i).toLowerCase().contains(s.toLowerCase()) ) return true;
        return false;
    }
    
    
    // debug, print all directories containing genomic source files
    public String distDirsToString() {
        String res ="";
        Iterator<File> it = distDir.iterator();
        while(it.hasNext()) {
            res += it.next() +"\n";
        }
        return res;
    }
    
    // debug, print genome's data human readable
    public String toString() {
        String res = "SCIENTIFIC NAME: "+ sciName+"\n";
        res += "TAXID: "+taxid+"\n";
        for (int i=0; i<distDir.size(); ++i) {
            res += "DISTDIR: "+distDir.get(i).toString()+"\n";
        }
        res += "DATADIR: "+dataDir.toString()+"\n";
        String t = "?";
        if (type==TYPE.NCBI_BACTERIA) t = "NCBI_BACTERIA";
        if (type==TYPE.NCBI_EUKARYOTA) t = "NCBI_EUKARYOTA";
        if (type==TYPE.NCBI_FUNGI) t = "NCBI_FUNGI";
        if (type==TYPE.NCBI_ARCHAEA) t = "NCBI_ARCHAEA";
        if (type==TYPE.NCBI_VIRAL) t = "NCBI_VIRAL";
        if (type==TYPE.ENSEMBL) t = "ENSEMBL";
        res += "TYPE:" + t + "\n";
        for (int i=0; i<lineage.size(); ++i) {
            res += i + " " + lineage.get(i) + " " + taxidLineage.get(i) + "\n";
        }
        Iterator<?> it = names.keySet().iterator();
        res +="NAMES:\n";
        while (it.hasNext()) {
            res += it.next() + "\n";
        }
        
        res += "PROTEIN FILES:\n";
        it = aa.iterator();
        while (it.hasNext()) {
            res += it.next() + "\n";
        }
        res += "DNA FILES:\n";
        it = dna.iterator();
        while (it.hasNext()) {
            res += it.next() + "\n";
        }
        return res;
    }
    
    public String getDNAFiles() {
        String res ="";
        for (int i=0; i<dna.size(); ++i) {
            res += dna.get(i).toString() + "\n";
        }
        return res;
    }
    
    public String getAAFiles() {
        String res ="";
        for (int i=0; i<aa.size(); ++i) {
            res += aa.get(i).toString() + "\n";
        }
        return res;
    }
    
    public Vector<File> getAA() { 
    	return aa;
    }
    public Vector<File> getDNA() {
    	return dna;
    }
    
    public boolean hasDNA() {
   	
    	return dna.size() > 0;
    }
    
    public boolean hasAA() {
    	
    	return aa.size() > 0;
    }
    
    public void changeDirs(String current, String replace) throws Exception {
        for (int i=0; i<distDir.size(); ++i) {
            distDir.set(i, new File(distDir.get(i).getCanonicalPath().replaceFirst(current, replace)) );
        }
        dataDir = new File( dataDir.getCanonicalPath().replaceFirst(current, replace) );
        for (int i=0; i<aa.size(); ++i) {
            aa.set(i, new File(aa.get(i).getCanonicalPath().replaceFirst(current, replace)) );
        }
        for (int i=0; i<dna.size(); ++i) {
            dna.set(i, new File(dna.get(i).getCanonicalPath().replaceFirst(current, replace)) );
        }
    }
}
