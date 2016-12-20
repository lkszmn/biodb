/*
 * StreamReaderThread.java
 *
 * Created on April 28, 2005, 8:28 AM
 */
package genomes;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Writer;
/**
 * Thread which can read from a stream while another process is printing to it.
 * @author chris
 */
public class StreamReaderThread extends Thread{
    /**
     * Source
     */
    private BufferedReader in;
    /**
     * Target
     */
    private BufferedWriter out;
    /**
     * Number of bytes that have been passed.
     */
    private long byteCount = 0L;
    
    private boolean state;
    /**
     * Creates a new instance of StreamReaderThread
     * @param in Source
     * @param out Target
     */
    public StreamReaderThread(InputStream in, Writer out) {
        this.in =  new BufferedReader( new InputStreamReader(in) );
        this.out = new BufferedWriter( out );
        this.state = true;
    }
    
    /**
     * This is called via start running independently.
     */
    public void run(){
        try{
            char buffer[] = new char[4096];
            int size;
            while( (size=in.read(buffer))!=-1 && state){
                out.write(buffer, 0, size);
                byteCount += size;
            }
            out.flush(); 	
        }catch(Exception e){
            System.err.println(e);
        }
        state = false;
    }
    
    /**
     * Returns the number of bytes which passed this thread.
     * @return Number of bytes.
     */
    public long getByteCount(){ return byteCount; }
    
    /**
     * Print a string to this threads' targetstream.
     * @param s Some text.
     * @throws java.lang.Exception blub
     */
    public void print(String s) throws Exception{
        byteCount += s.getBytes().length;
        out.write(s);
    }
   
    public void close(){
	try{
	in.close();
	out.close();
	
	}catch(Exception e){
		System.err.println(e);
	}
    } 
    public void forceStop(){
        state=false;
    }
    
    public boolean isRunning(){
       return state;
    }
}
