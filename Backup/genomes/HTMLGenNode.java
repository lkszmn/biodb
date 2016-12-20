/*
 * HTMLGenNode.java
 *
 * Created on January 5, 2006, 3:14 PM
 */
package genomes;
import java.util.Vector;
/**
 *
 * @author chris
 */
public class HTMLGenNode implements Comparable<HTMLGenNode>{
    
    private HTMLGenNode parent;
    private final Integer id;
    private Vector<HTMLGenNode> childs;
    private final String sciName; 
    /** Creates a new instance of HTMLGenNode */
    public HTMLGenNode(Integer id, HTMLGenNode parent, String sciName) {
        this.id     = id;
        this.parent = parent;
        this.childs = new Vector<HTMLGenNode>();
        this.sciName = sciName;
    }
    
    public HTMLGenNode(Integer id, String sciName) {
        this.id    = id;
        this.sciName = sciName;  
        this.childs = new Vector<HTMLGenNode>();
    }
    
    public HTMLGenNode getChild(final Integer child){
        int l = childs.size();
        for(int i=0; i<l; i++){
            if( childs.get(i).id.equals(child) ){
                return childs.get(i);
            }
        }
        return null;
    }
    
    public HTMLGenNode getChild(int i){
        return childs.get(i);
    }
    
    public HTMLGenNode getParent(){
        return parent;
    }
    
    public Vector<HTMLGenNode> getChilds(){ return childs; }
    
    public void setParent(HTMLGenNode n){
        this.parent = n;
    }
    
    public void setChilds(Vector<HTMLGenNode> v){
        this.childs = v;
    }
    
    public void addChild(HTMLGenNode c){ childs.add(c); }
    public Integer getId(){ return id; }
    public String getSciName(){ return sciName; }
    
    public void deleteChild(HTMLGenNode c){
        int l = childs.size();
        for(int i=0; i<l; i++){
            if( childs.get(i) == c ){
                childs.remove(i);
                break;
            }
        }
    }
    
    public int compareTo(HTMLGenNode g){
        return this.getSciName().compareTo(g.getSciName());
    }
    
    public boolean isLeaf(){ if(childs.size()==0) return true; return false; }
}
