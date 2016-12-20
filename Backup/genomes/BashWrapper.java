/*
 * BashWrapper.java
 *
 * Created on May 6, 2005, 1:57 PM
 */
package genomes;
import java.util.concurrent.Callable;
import java.lang.Process;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 *
 * @author chris
 */
public class BashWrapper implements Callable<Integer>{
    private final String command;
    private StringWriter stdout;
    private StringWriter stderr;
    /** Creates a new instance of BashWrapper */
    public BashWrapper(final String command) {
        this.command = command;
        this.stdout = new StringWriter();
        this.stderr = new StringWriter();
    }
    
    public Integer call() throws Exception{
        String cmd[] = { "/bin/bash", "-c", command };
        Process p = Runtime.getRuntime().exec(cmd);
        
     //   StreamReaderThread out = new StreamReaderThread( p.getInputStream(), stdout);
     //   StreamReaderThread err = new StreamReaderThread( p.getErrorStream(), stderr);

     //   out.start();
     //   err.start();
        
        BufferedWriter buffy = new BufferedWriter(stdout);
        BufferedReader in    = new BufferedReader( new InputStreamReader(p.getInputStream()));
        while(p.waitFor() !=0){
        	while(in.ready()){
        		buffy.write(in.readLine());
        	}
        }
        in.close();
        buffy.close();
        int ret = p.waitFor();
        
       
        if( ret != 0 ) throw new Exception("Error executing "+command+"\n " + stdout.getBuffer().toString() + "\n" + stderr.getBuffer().toString() );
  //      out.close();
  //      err.close();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
        
        return 0;
    }
    
/*    public Integer call() throws Exception{
        String cmd[] = { "/bin/bash", "-c", command };
        Process p = Runtime.getRuntime().exec(cmd);
        
        StreamReaderThread out = new StreamReaderThread( p.getInputStream(), stdout);
        StreamReaderThread err = new StreamReaderThread( p.getErrorStream(), stderr);

        out.start();
        err.start();
        
        int ret = p.waitFor();
        p.destroy();
        if( ret != 0 ) throw new Exception("Error executing "+command+"\n " + stdout.getBuffer().toString() + "\n" + stderr.getBuffer().toString() );
        out.close();
        err.close();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
        
        return 0;
    } */
    
    
    
    // Added Destroy method to reduce the count of open Filehandles on system
    public void destroy(){
    	try {
			stdout.close();
			stderr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    public String getStdout(){ return stdout.getBuffer().toString(); }
    public String getStderr(){ return stderr.getBuffer().toString(); }
    
}
