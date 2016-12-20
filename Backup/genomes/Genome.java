/*
 * Genome.java
 *
 * Created on December 20, 2005, 12:10 PM
 */

package genomes;

import java.io.File;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
/**
 *
 * @author chris
 * HAS TO BE FIXED !!!!
 */
public class Genome implements Serializable{
    
    static final long serialVersionUID = 222L;
    
    public enum TYPE {NCBI_BACTERIA, NCBI_FUNGI, NCBI_EUKARYOTA, ENSEMBL};
    
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
    public Genome(File distDir, dmpReader r, Config c) throws Exception {
        this.distDir = new LinkedList<File>();
        this.distDir.add(distDir);
        this.type = getType(c);
        this.taxid = getTaxid(r);
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
            String cp = new String(names_it.next());         
            hm.put(cp, new String(g.names.get(cp)));
        }     
        this.names = hm;
        this.sciName = new String(this.names.get("#SCIENTIFICNAME#"));
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

    private static Integer readTaxid(dmpReader r, BufferedReader br) throws Exception {
	Integer gi = getGi(br);
	Integer t = r.getTaxid(gi);
	if (t != null) {
	    return t;
	}
	t = r.getTaxid(getNCBIName(br));
	return t;
    }
    
    // tries to get the taxid of this organism either by gi or name
    private Integer getTaxid(dmpReader r) throws Exception {
        if (this.type == TYPE.ENSEMBL) {
            Integer t = r.getTaxid( getEnsemblName(distDir.get(0)) );
            if (t != null) return t;
        } else if (this.type == TYPE.NCBI_BACTERIA || this.type == TYPE.NCBI_FUNGI) {
            File files[] = distDir.get(0).listFiles(new ExtensionFileFilter(".faa"));
            if (files.length==0) throw new Exception("No .faa file in "+distDir.get(0).toString());
	    BufferedReader br = new BufferedReader(new FileReader(files[0]));
	    try {
		Integer gi = getGi(br);
		Integer t = r.getTaxid(gi);
		if (t != null) {
		    return t;
		}
		t = r.getTaxid(getNCBIName(br));
		if ( t!=null ) return t;
		t = r.getTaxid( distDir.get(0).getName().replace('_', ' ') );
		if (t != null) return t;
	    } finally {
		br.close();
	    }
        } else if (this.type == TYPE.NCBI_EUKARYOTA) {
            // normal case: a directory with the proteindata
            File chrDir = new File(distDir.get(0),"protein");
            // they may be chr subdirs or files (e.g. INFLUENZA)
            if ( !chrDir.exists() ) {
                File chrs[] = distDir.get(0).listFiles(new ExtensionFileFilter(".faa"));
                if (chrs.length > 0) chrDir = distDir.get(0);
                else {
                    //subdirs:
                    chrs = distDir.get(0).listFiles();
                    // take the first of this directory-list
                    // it is only used for the extraction of the taxid from a file in this dir
                    if (chrs.length>0) chrDir = chrs[0];
                }
            }
            File files[] = chrDir.listFiles(new ExtensionFileFilter(".faa"));
	    BufferedReader br = null;
	    try {
		if (files.length != 0) {
		    br = new BufferedReader( new FileReader(files[0]));
		    Integer t = readTaxid(r, br);
		    if (t != null) return t;
		} else {
		    files = chrDir.listFiles(new ExtensionFileFilter(".fa.gz"));
		    if (files.length!=0) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream( new FileInputStream(files[0]))));
			Integer t = readTaxid(r, br);
			if (t != null) return t;
		    } else {
			throw new Exception("No '.faa' or '.fa.gz' file in "+chrDir);
		    }
		}
	    } finally {
		if (br != null) br.close();
	    }
        }
        throw new Exception("Cannot get taxid of '"+distDir.get(0).toString()+"'");
    }
    
    // parse gi from fasta header - might be an 'old' gi which is not contained in gi_taxid_prot.dmp
    private static Integer getGi(BufferedReader br) throws Exception {
        Pattern P = Pattern.compile(">.*?gi.*?(\\d+).*");
        String line;
        Integer res=null;
        while ( (line=br.readLine())!=null) {
            Matcher M = P.matcher(line);
            if (M.matches()) {
                res = new Integer(M.group(1));
                break;
            }
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
        } else {
	    throw new Exception("Cannot evaluate type of '"+distDir.get(0).toString()+"'");
	}
    }
    
    // converts ensembl filenames to scientific names which might be contained in ncbi taxonomy
    private String getEnsemblName(File f) throws Exception {
        int i=f.getName().indexOf('_');
        int j=f.getName().indexOf('_',i+1);
        if (j < 0) j = f.getName().length();
        if ( i>0 ) return f.getName().replace('_',' ').substring(0, j);
        else throw new Exception("Cannot parse organism name from ENSEMBL directory: "+f);
    }
    
    // parse scientific name from fasta header of ncbi genomes
    private static String getNCBIName(BufferedReader br) throws Exception {
        Pattern P = Pattern.compile(">.*?gi.*?\\d+.+?\\[([^\\[]+?)\\]\\s*$");
        String line;
        String res = null;
        while ( (line=br.readLine()) != null) {
            Matcher M = P.matcher(line);
            if (M.matches()) {
                res = M.group(1);
                break;
            }
        }
        return res;
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
            format(prot[i], target, 'T');
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
                    format(files[j], target, 'T');
                    this.aa.add(target);
                }
            }else{
            // e.g. INFLUENZA no subdirs
                if ( dirs[i].toString().endsWith(".faa") ) {
                    File target = new File(dataDir, dirs[i].getName());
                    format(dirs[i], target, 'T');    
                    this.aa.add(target);
                }
            }
        }
    }
    
    private void formatENSEMBLE(File f) throws Exception {
        File prot[] = new File(f,"/pep/").listFiles();
        for (int i=0; i<prot.length; ++i) {
            String n = prot[i].getName().substring(0, prot[i].getName().lastIndexOf('.'));
            File target = new File(dataDir, n);
            format(prot[i], target, 'T');
            this.aa.add(target);
        }
    }
    
    
    private void formatNCBIBacteria(File f) throws Exception {
        File prot[] = f.listFiles(new ExtensionFileFilter(".faa"));
        for (int i=0; i<prot.length; ++i) {
            String n = prot[i].getName();
            File target = new File(dataDir, n);
            format(prot[i], target, 'T');
            this.aa.add(target);
        }
        File dna[] = f.listFiles(new ExtensionFileFilter(".fna"));
        for (int i=0; i<dna.length; ++i) {
            String n = dna[i].getName();
            File target = new File(dataDir, n);
            format(dna[i], target, 'F');
            this.dna.add(target);
        }
    }
    
    
    
    private void format(File src, File targ, char pep)throws Exception {
        String cmd = "cat "+src+" | "+formatdb+" -i stdin -p "+pep+" -n "+targ;
        if (src.toString().endsWith(".gz")) {
            // or better using zcat?
            cmd = "gzip -d -c "+src+" | "+formatdb+" -i stdin -p "+pep+" -n "+targ;
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
    
    // merges to genomes (bacteria)
    public void merge(Genome g) { this.distDir.addAll(g.distDir); }
    
    public File getDataDir() { return dataDir; }
    
    public void formatGenome() throws Exception {
        dataDir.mkdirs();
        Iterator<File> it = distDir.iterator();
        while (it.hasNext()) {
            if (this.type == TYPE.ENSEMBL) {
                formatENSEMBLE(it.next());
            } else if (this.type == TYPE.NCBI_BACTERIA) {
                formatNCBIBacteria(it.next());
            } else if (this.type == TYPE.NCBI_FUNGI) {
                formatNCBIFungi(it.next());
            } else if (this.type == TYPE.NCBI_EUKARYOTA) {
                formatNCBIEukaryota(it.next());
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
    
    public Vector<File> getAA() { return aa; }
    public Vector<File> getDNA() { return dna; }
    
    public boolean hasDNA() {
        if ( dna.size()>0 ) return true;
        else return false;
    }
    
    public boolean hasAA() {
        if ( aa.size()>0 ) return true;
        else {
        
        	
        }
        return false;
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
