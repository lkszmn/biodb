/*
 * GenomesDb.java
 *
 * Created on January 5, 2006, 9:39 AM
 */
package genomes;

import java.io.File;
import java.io.Serializable; 
import java.io.FileInputStream;


import java.util.Iterator;
import java.util.HashMap;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
/**
 *
 * @author chris, extended : Joern Marialke 2011 MPI for Evolutionary Biology
 */
public class GenomesDB implements Serializable{
    
    static final long serialVersionUID = 111L;
    
    // a hash that maps taxid -> genome-object
    private HashMap<Integer,Genome> genomes;
    
    /** Creates a new instance of GenomesDb */
    public GenomesDB() {
        genomes = new HashMap<Integer,Genome>();
    }
    
    public static GenomesDB load(File dbf) throws Exception{
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new FileInputStream(dbf));
        GenomesDB db = (GenomesDB) ois.readObject();
        ois.close();
        return db;
    }
    
    public void save(File out) throws Exception{
        ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream(out) );
        oos.writeObject(this);
        oos.close();
        
        //BufferedWriter buffy = new BufferedWriter(new FileWriter(out+".txt"));
        //buffy.write(this.toString());
        //buffy.close();
        
    }
    
    public void runcheck(java.io.OutputStreamWriter os) throws Exception{
        // check for files
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()){
            Integer i = it.next();
            Genome g = genomes.get(i);
            if( g.getAA().size()==0 ) os.write( g.getSciName()+" "+g.getTaxid()+ " has no protein files.\n");
            if( g.getDNA().size()==0 ) os.write( g.getSciName()+" "+g.getTaxid()+" has no DNA files.\n");
            os.flush();
        }
    }
    
    // removes all genomes that have a taxids that is an internal node of the lineage of another genome
    public void validate(Config c) throws Exception{
        c.log("Validating...\n");
        int i=0;
        int taxids[] = new int[genomes.size()];
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()) taxids[i++] = (it.next()).intValue();
        
        for(i=0; i<taxids.length; i++){
            for(int j=0; j<taxids.length; j++){
                if( i==j || !genomes.containsKey(taxids[j]) || !genomes.containsKey(taxids[i]) ) continue;
                Genome g = genomes.get(taxids[j]);
                for( int k=0; k<g.getTaxidLineage().size(); ++k){
                    if( g.getTaxidLineage().get(k).intValue() == taxids[i] ){
                        c.log("Merging " + genomes.get(taxids[i]).getSciName() + " into " + g.getSciName() +"\n");
                        g.merge(genomes.get(taxids[i]));
                        genomes.remove(taxids[i]);
                        break;
                    }
                }
            }
        }
        c.log("done.\n");
    }
    
    public void formatdb(Config c) throws Exception{
        Iterator<Integer> it = genomes.keySet().iterator();
        while( it.hasNext() ){
            Genome g = genomes.get(it.next());
            c.log("Formatting " + g.getSciName() + "," + g.getTaxid() + "\n");
            c.log(g.distDirsToString());
            c.log("====> " + g.getDataDir() + "\n");
            try{
                g.formatGenome();
            }catch(Exception e){
                c.log(e.getMessage()+"\n"+e.getStackTrace());
            }
            c.log("Formatting done.\n");
        }
    }
    
    public void mergeAdd(Genome g2){
        Genome g1 = genomes.get(g2.getTaxid());
        g1.merge(g2);
    }
    
    public void add(Genome g){
        genomes.put(g.getTaxid(), g);
    }
    
    public boolean contains(Genome g){
        return genomes.containsKey(g.getTaxid());
    }
    public boolean containsTaxid(Integer t){
        return genomes.containsKey(t);
    }
    
    public void print(){
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()){
            Integer i = it.next();
            System.out.println(genomes.get(i).getSciName()+","+i);
        }
        System.out.println(genomes.size()+" genomes.");
    }
     
    public void printFull(){
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()){
            Integer i = it.next();
            System.out.println(genomes.get(i).toString());
        }
        System.out.println(genomes.size()+" genomes.");
    }
    
    public String toString(){
    	String out ="";
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()){
        	Integer i = it.next();
        	out +=" GENOME NR : "+i+" ------------------------------------------------------------\n ";
            
            out += genomes.get(i).toString()+"\n";
        }
        
        out += genomes.size()+" genomes \n";
        
        return out;
    }
    
    public Genome get(Integer taxid){ return genomes.get(taxid); }
    
    public Iterator<Integer> iterator(){ return genomes.keySet().iterator(); }
    
    public int size(){
        return genomes.keySet().size();
    }
    
    public void changeDirs(String current, String replace) throws Exception{
        Iterator<Integer> it = genomes.keySet().iterator();
        while(it.hasNext()){  
            Genome g = genomes.get(it.next());
            g.changeDirs(current, replace);
        }    
    }
}
