/*
 * Search.java
 *
 * Created on January 5, 2006, 10:37 AM
 */
package genomes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
/**
 *
 * @author chris
 */
public class Search {
    
    private GenomesDB db;
    private Config c;
    /** Creates a new instance of Search */
    public Search(Config c) throws Exception{
        this.c = c;
        db = GenomesDB.load( c.DBFILE );
        if(c.REPORT_MODE==Config.REPORT.DEBUG){
            c.log("SEARCH L024 Loaded " + c.DBFILE + "  (# " +db.size()+ " genomes)\n");
        }
       
    }
    
    public String getFiles(LinkedList<Integer> taxids){
        Iterator<Integer> it = taxids.iterator();
        StringBuffer ret = new StringBuffer();
        while(it.hasNext()){
            Genome g = db.get(it.next());
            //System.out.println(g.toString());
            //System.out.println("-----------------------------------\n");
            if(g!=null)
                if(c.DATA_MODE==Config.MODE.PEPTIDE)
                    ret.append( g.getAAFiles() );
                else
                    ret.append( g.getDNAFiles() );
        }
        return ret.toString();
    }
    
    public int countMatches(String exp) throws Exception{
         return (eval(0,exp.length(), new HashMap<Integer, Genome>(), exp)).size();
    }
    
    
    public String search(final String exp) throws Exception{
    	System.out.println("SEARCH L51 - "+exp+"\n");
        StringBuffer res = new StringBuffer();
        HashMap<Integer, Genome> m = eval(0,exp.length(), new HashMap<Integer, Genome>(), exp);
        if(c.REPORT_MODE==Config.REPORT.ALL){
        	System.out.println("SEARCH L055 Searching with Config.REPORT.All\n");
            Iterator<Integer> it = m.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                System.out.println("SEARCH WHILE 59 taxid "+taxid+"\n");
                Genome g = m.get(taxid);
                res.append(g.getSciName()+" "+g.getTaxid()+"\n");
            }
            res.append("\nSEARCH L63 Matched "+m.keySet().size()+" genomes.");
            
        }else  if(c.DATA_MODE== Config.MODE.PEPTIDE && c.REPORT_MODE==Config.REPORT.FILES){
        	System.out.println("SEARCH L066 Searching with Config.Mode.Peptide\n");
            Iterator<Integer> it = m.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = m.get(taxid);
                res.append(g.getAAFiles());
            }
        }else  if(c.DATA_MODE== Config.MODE.DNA && c.REPORT_MODE==Config.REPORT.FILES){
            Iterator<Integer> it = m.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = m.get(taxid);
                res.append(g.getDNAFiles());
            }
        }else  if(c.REPORT_MODE==Config.REPORT.TAXIDS){
            Iterator<Integer> it = m.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = m.get(taxid);
                res.append(g.getTaxid()+" ");
            }
        }else  if(c.REPORT_MODE==Config.REPORT.DEBUG){
        	System.out.println("SEARCH L088 Searching with c.Repoprt_Mode == Config.REPORT.DEBUG\n");
            Iterator<Integer> it = m.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = m.get(taxid);
                res.append(g.toString()+"\n");
            }
            res.append("\nSEARCH LINE 95 Matched "+m.keySet().size()+" genomes.");
        }else throw new Exception("Unknown mode: datamode="+c.DATA_MODE+" reportmode="+c.REPORT_MODE);
        
        
        
        return res.toString();
    }
    
/*    private void printNames(HashMap<Integer, Genome> h){
        Iterator<Integer> it = h.keySet().iterator();
        while( it.hasNext() ){
            Integer taxid = it.next();
            Genome g = db.get(taxid);
            System.out.println("HIT "+g.getSciName()+","+taxid);
        }
        System.out.println("Number of matching sets: " + h.keySet().size());
    }
    */
    
    private HashMap<Integer, Genome> eval(int l, int r, HashMap<Integer, Genome> h, final String exp) throws Exception{
        if(l<r){
            if(exp.charAt(l)=='('){
                int c = getClose(l, exp);
                HashMap<Integer, Genome> tmp = eval(l+1, c-1, new HashMap<Integer, Genome>(), exp);
                return eval(c, r, tmp, exp);
            }else if(exp.charAt(l)=='&' | exp.charAt(l)=='|'){
                HashMap<Integer, Genome> tmp = eval(l+1, r, new HashMap<Integer, Genome>(), exp);
                return mergeSets(tmp, h, exp.charAt(l) );
            }else if(exp.charAt(l)=='!'){
                if(exp.charAt(l+1)=='('){
                    int c = getClose(l+1, exp);
                    HashMap<Integer, Genome> tmp = eval(l+1, c, new HashMap<Integer, Genome>(), exp);
                    tmp = getComplement(tmp);
                    return eval(c, r, tmp, exp);
                }else if(exp.charAt(l+1)=='!'){
                    return getComplement( eval(l+1, r, new HashMap<Integer, Genome>(), exp) );
                }else{
                    String token = getToken(l+1, exp);
                    HashMap<Integer, Genome> tmp = getSet(token.trim());
                    tmp = getComplement(tmp);
                    return eval(l+1+token.length(), r, tmp, exp);
                }
            }else{
                String token  = getToken(l, exp);
                HashMap<Integer, Genome> tmp = getSet(token.trim());
                return eval(l+token.length(), r, tmp, exp);
            }
        }else
            return h;
    }
    
    private HashMap<Integer, Genome> mergeSets(HashMap<Integer, Genome> a, HashMap<Integer, Genome> b, char op) throws Exception{
        HashMap<Integer,Genome> res = new HashMap<Integer, Genome>();
        if( op == '&' ){
            Iterator<Integer> it = a.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = db.get(taxid);
                if( b.containsKey(taxid) ){ res.put(taxid, g); }
            }
        }else if( op == '|'){
            res = b;
            Iterator<Integer> it = a.keySet().iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = db.get(taxid);
                if( !res.containsKey(taxid) ){ res.put(taxid, g); }
            }
        }else throw new Exception("Unknown operator!");
        
        return res;
        
    }
    
    
    //get string of variable
    private String getToken(int i, final String exp){
        int c=i;
        while(c<exp.length() && exp.charAt(c)!='(' && exp.charAt(c)!=')' && exp.charAt(c)!='!' && exp.charAt(c)!='&' && exp.charAt(c)!='|'){
            c++;
        }
        return exp.substring(i,c);
    }
    
    //                                      |
    //get corresponding close bracket - ...)....
    private int getClose(int i, final String exp) throws Exception{
        int count=1;
        i++;
        while(count!=0 && i<exp.length()){
            if(exp.charAt(i)=='(') count++;
            else if(exp.charAt(i)==')') count--;
            i++;
        }
        if(count!=0){
            throw new Exception("Cannot get corresponding 'close' bracket!");
        }
        return i;
    }
    
    //returns a hash of all genomes that match keyword s
    private HashMap<Integer, Genome> getSet(String s){
        
        HashMap<Integer, Genome> res = new HashMap<Integer, Genome>();
        Iterator<Integer> it = db.iterator();
        
        // first check for exact matches!
        while( it.hasNext() ){
            Integer taxid = it.next();
            Genome g = db.get(taxid);
            if( g.matches(s) ){ res.put(taxid, g); }
        }
        
        //if the keyword (s) does not match any genomes try to find genomes that contain it as substring
        if(res.size()==0){
            it = db.iterator();
            while( it.hasNext() ){
                Integer taxid = it.next();
                Genome g = db.get(taxid);
                if( g.substring_matches(s) ){ res.put(taxid, g); }
            }
        }
        
        return res;
    }
    
    //returns a hash containing the complemented set of genomes
    private HashMap<Integer, Genome> getComplement(HashMap<Integer, Genome> h){
        HashMap<Integer, Genome> res = new HashMap<Integer, Genome>();
        Iterator<Integer> it = db.iterator();
        while( it.hasNext() ){
            Integer taxid = it.next();
            if( h.containsKey(taxid)) continue;
            res.put(taxid, db.get(taxid));
        }
        return res;
        
    }
    
    
}
