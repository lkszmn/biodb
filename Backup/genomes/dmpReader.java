/*
 * dmpReader.java
 *
 * Created on December 20, 2005, 9:44 AM
 */
package genomes;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;
import java.util.Collections;
/**
 * WHATEVER THIS READS .......
 * @author chris
 */
public class dmpReader implements Serializable{
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<Integer,HashMap<String,String>> names;
    private HashMap<Integer,Node> nodes;
    private HashMap<Integer,Integer> giprot2taxid;
    
    /** Creates a new instance of dmpReader */
    public dmpReader(Config c) throws Exception{
        names = new HashMap<Integer, HashMap<String,String>>(100000);
        nodes = new HashMap<Integer, Node>(100000);
        giprot2taxid = new HashMap<Integer,Integer>(100000);
        
        readNames(c.NAMES_DMP);
        readNodes(c.NODES_DMP);

        readGiProt2Taxid(c.GIPROT2TAXID);
        
    }
    
    public Integer getTaxid(final Integer gi){
        return giprot2taxid.get(gi);
    }
        
    public Integer getTaxid(String name){
        //return deepest node with this name in case of doubt
        Integer res = null;
        int depth = -1;
        Iterator<Integer> it = names.keySet().iterator();
        while(it.hasNext()){
            Integer key = it.next();
            HashMap<String,String> s = names.get(key);
            if(s.containsKey(name.toLowerCase())){
                int d = getDepth(key);
                if(d>depth){
                    res = key;
                    depth = d;
                }
            }
        }
        return res;
    }
    
    public int getDepth(Integer taxid){
        int d = 1;
        Node n = nodes.get(taxid);
        while( n.getTarget().intValue()!=1 ){
            n = nodes.get(n.getTarget());
            d++;
        }
        return d;
    }
    
    public HashMap<String,String> getNames(Integer taxid){
        return names.get(taxid);
    }
    
    public Vector<Integer> getTaxidVec(Integer taxid){
        Vector<Integer> res = new Vector<Integer>();
        res.add(taxid);
        Node n = nodes.get(taxid);
        while( n.getTarget().intValue()!=1 ){
            res.add(n.getTarget());
            n = nodes.get(n.getTarget());
        }
        res.add(n.getTarget());
        Collections.reverse(res);
        return res;
    }
    
    public Vector<String> getLineageVec(Integer taxid){
        Vector<String> res = new Vector<String>();
        Node n = nodes.get(taxid);
        res.add(names.get(taxid).get("#SCIENTIFICNAME#"));
        while( n.getTarget().intValue()!=1 ){
            res.add(names.get(n.getTarget()).get("#SCIENTIFICNAME#"));
            n = nodes.get(n.getTarget());
        }
        res.add(names.get(n.getTarget()).get("#SCIENTIFICNAME#"));
        Collections.reverse(res);
        return res;
    }
    
    
    public void readNames(File f) throws Exception{
    	System.out.println("Builder L107 ReadNames from : "+f.getAbsolutePath()+"\n");
        BufferedReader br = new BufferedReader( new FileReader(f) );
        String line;
        HashMap<String,String> namesM = new HashMap<String,String>();
        String prev = "1";
        while( (line=br.readLine())!=null){
            if(line.trim().equals("")) continue;
            String sl[]=parseNamesLine(line);
            if( !sl[0].equals(prev) ){
                names.put(new Integer(prev), namesM);
                namesM = new HashMap<String,String>();
                prev = sl[0];
            }
            if( sl[2].equalsIgnoreCase("scientific name")) {
                namesM.put(sl[1].toLowerCase(), "scientific name");
                namesM.put("#SCIENTIFICNAME#", sl[1]);
            } else namesM.put(sl[1].toLowerCase(),"");
            System.out.println("Builder L124 Scientific Names " +sl[0]+" "+sl[1]+" "+sl[2]+"\n");
        }
	br.close();
    }
    
    public void readNodes(File f) throws Exception{
    	System.out.println("Builder L130 ReadNodes from : "+f.getAbsolutePath()+"\n");
        BufferedReader br = new BufferedReader( new FileReader(f) );
        String line;
        while( (line=br.readLine())!=null){
            if(line.trim().equals("")) continue;
            String sl[]=parseNodesLine(line);
            nodes.put(new Integer(sl[0]), new Node(Integer.parseInt(sl[1]), sl[2]) );
            System.out.println("Builder L137 Node Names " +sl[0]+" "+sl[1]+" "+sl[2]+"\n");
        }
	br.close();
    }
    
    public void readGiProt2Taxid(File f) throws Exception{
        BufferedReader br = new BufferedReader( new FileReader(f) );
        String line;
        while( (line=br.readLine())!=null){
            if(line.trim().equals("")) continue;
            int i = line.indexOf('\t');
            Integer gi = new Integer( line.substring(0,i).trim() );
            Integer taxid = new Integer( line.substring(i+1).trim() );
            giprot2taxid.put(gi,taxid);
            //System.out.println(gi+" "+taxid);
        }
	br.close();
    }
    
    
    
    private String[] parseNamesLine(String row){
        int i = row.indexOf('|',1);
        int j = row.indexOf('|',i+1);
        String[] result = new String[3];
        
        result[0] = row.substring( 0,i ).trim();
        result[1] = row.substring( i+1,j).trim();
        i = row.indexOf('|',j+1);
        j = row.indexOf('|',i+1);
        result[2] = row.substring(i+1,j).trim();
        
        return result;
    }
    
    private String[] parseNodesLine(String row){
        int i = row.indexOf('|',1);
        int j = row.indexOf('|',i+1);
        String[] result = new String[3];
        
        result[0] = row.substring( 0,i ).trim();
        result[1] = row.substring( i+1,j).trim();
        i = row.indexOf('|',j+1);
        result[2] = row.substring(j+1,i).trim();
        
        return result;
    }
    
    
    public void printNames(){
        Iterator<Integer> it = names.keySet().iterator();
        while(it.hasNext()){
            Integer key = it.next();
            HashMap<String,String> s = names.get(key);
            System.out.println("KEY: "+key);
            Iterator<String> it2 = s.keySet().iterator();
            while(it2.hasNext()){
                System.out.println( it2.next() );
            }
        }
    }
    
    public void printNodes(){
        Iterator<Integer> it = nodes.keySet().iterator();
        while(it.hasNext()){
            Integer key = it.next();
            Node obj = nodes.get(key);
            System.out.println("KEY: "+key+" "+obj.toString());
        }
    }
    
}
