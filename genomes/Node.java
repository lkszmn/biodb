

/**
 *
 * @author  chris
 */

package genomes;
public class Node {
    
    private final String hirarchy;
    private final Integer target;
    /** Creates a new instance of NodeObject */
    public Node(int Target, String Hirarchy) {
        hirarchy = Hirarchy.trim();
        target = new Integer(Target);
    }
    
    public String getHirarchy(){
        return hirarchy;
    }
    public Integer getTarget(){
        return target;
    }
    
    public String toString(){
        return hirarchy+" TARGET:"+target;
    }
}
