/*
 * Tree.java
 *
 * Created on January 5, 2006, 3:05 PM
 */
package genomes;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;

/**
 * 
 * @author chris
 * @updated: Joern Marialke November 2011
 */
public class HTMLGenTree {
	private final Config config;
	private GenomesDB db;
	private HTMLGenNode dnaRoot;
	private HTMLGenNode aaRoot;
	private int MaxDepth;

	/** Creates a new instance of Tree */
	public HTMLGenTree(Config c) throws Exception {
		this.config = c;
		load(c.DBFILE);
		make();
	}

	public HTMLGenTree(GenomesDB db, Config c) throws Exception {
		System.out.println("HTMLGenTree L38 Generating HTML TREE!");  
		this.config = c;
		this.db = db;
		make();
	}

	private void make() throws Exception {
		System.out.println("HTMLGenTree L45 running Make\n");
		Iterator<Integer> it = db.iterator();
		while (it.hasNext()) {
			Integer taxid = it.next();
			Genome g = db.get(taxid);
			 System.out.println("HTMLGenTree L50 " +g.toString());
			 //System.out.println( "DNA: "+g.getDNAFiles());
			if (g.hasDNA())
				dnaRoot = insert(g.getTaxidLineage(), g.getLineage(), dnaRoot);
			aaRoot = insert(g.getTaxidLineage(), g.getLineage(), aaRoot);
			if (dnaRoot == null)
				dnaRoot = new HTMLGenNode(0, "no dna files");
		}

		System.out.println("HTMLGenTree L59 Compressing the protein-tree...");
		compress(aaRoot);
		System.out.println("HTMLGenTree L61 Compressing the DNA-tree...");
		compress(dnaRoot);

		if (config.ULTRA_COMPRESS) {
			System.out
					.println("HTMLGenTree L66 Ultra-compressing the protein-tree below level="
							+ config.ULTRA_COMPRESS_LEVEL + " ...");
			ultra_compress(aaRoot, config.ULTRA_COMPRESS_LEVEL, 0);
			System.out.println("Ultra-compressing the DNA-tree below level="
					+ config.ULTRA_COMPRESS_LEVEL + " ...");
			ultra_compress(dnaRoot, config.ULTRA_COMPRESS_LEVEL, 0);
		}

		if (config.DEBUG) {
			System.out.println("Writing debug-html-trees...");
			printHTMLTree();
			// printJavascriptTree();
			// db.print();
		}

		System.out.println("HTMLGenTree L81 Writing rhtml and js files...");
		createFiles();
		System.out.println("done.\n");
		toDo();
	}

	private HTMLGenNode insert(Vector<Integer> tax, Vector<String> names,
			HTMLGenNode root) {
		if (root != null) {
			HTMLGenNode parent = root;
			for (int i = 1; i < tax.size() - 1; ++i) {
				HTMLGenNode n = parent.getChild(tax.get(i));
				if (n != null) {
					parent = n;
				} else {
					n = new HTMLGenNode(tax.get(i), parent, names.get(i));
					parent.addChild(n);
				}
				parent = n;
			}
			parent.addChild(new HTMLGenNode(tax.lastElement(), parent, names
					.lastElement()));
		} else {
			root = new HTMLGenNode(tax.get(0), null, names.get(0));
			insert(tax, names, root);
		}
		return root;
	}

	private void createFiles() throws Exception {

		BufferedWriter tw = new BufferedWriter(new FileWriter(new File(
				config.WEB_DIR, "proteintree.js")));
		tw.write("var tree=[");
		tw.write(getJavascriptTree(aaRoot).toString());
		tw.write("];");
		tw.close();

		tw = new BufferedWriter(new FileWriter(new File(config.WEB_DIR,
				"dnatree.js")));
		tw.write("var tree=[");
		tw.write(getJavascriptTree(dnaRoot).toString());
		tw.write("];");
		tw.close();

		tw = new BufferedWriter(new FileWriter(new File(config.WEB_DIR,
				"_proteintree.rhtml")));
		tw.write("<div id=\"gtree\" class=\"gtreebox\">\n");
		printRHTMLTree(aaRoot, 0, tw);
		tw.write("</div>\n");
		tw.close();

		tw = new BufferedWriter(new FileWriter(new File(config.WEB_DIR,
				"_dnatree.rhtml")));
		tw.write("<div id=\"gtree\">\n");
		printRHTMLTree(dnaRoot, 0, tw);
		tw.write("</div>\n");
		tw.close();
	}

	/*
	 * private void printJavascriptTree() throws Exception{ BufferedWriter tw =
	 * new BufferedWriter( new FileWriter("aaTree.js") ); tw.write("tree=[");
	 * printJavascriptTree(aaRoot, tw); tw.write("];");
	 * 
	 * tw.close();
	 * 
	 * tw = new BufferedWriter( new FileWriter("dnaTree.js") );
	 * tw.write("tree=["); printJavascriptTree(dnaRoot, tw); tw.write("];");
	 * tw.close(); }
	 */

	/*
	 * private void printJavascriptTree(HTMLGenNode n, Writer w) throws
	 * Exception{ if( n.isLeaf() ){ w.write("["+n.getId()+",0]"); }else{
	 * w.write("["+n.getId()+",0,"); for(int i=0; i<n.getChilds().size(); i++){
	 * 
	 * printJavascriptTree(n.getChild(i), w);
	 * 
	 * if( i==n.getChilds().size()-1 ) w.write("]"); else w.write(","); } }
	 * w.flush(); }
	 */

	private StringBuffer getJavascriptTree(HTMLGenNode n) throws Exception {
		StringBuffer ret = new StringBuffer();
		if (n.isLeaf()) {
			ret.append("[" + n.getId() + ",0]");
		} else {
			ret.append("[" + n.getId() + ",0,");
			for (int i = 0; i < n.getChilds().size(); i++) {
				ret.append(getJavascriptTree(n.getChild(i)));
				if (i == n.getChilds().size() - 1)
					ret.append("]");
				else
					ret.append(",");
			}
		}
		return ret;
	}

	private void printHTMLTree() throws Exception {

		MaxDepth = getMaxDepth(aaRoot);

		FileWriter w = new FileWriter(new File(config.WEB_DIR, "aaTree.html"));

		w.write("<!DOCTYPE HTML PUBLIC>\n");
		w.write("<html>\n");
		w.write("<head>\n");
		w.write("<title>Genomes Tree Protein</title>\n");
		w.write("<script LANGUAGE=\"JavaScript\" src=\"aaTree.js\"> </script>\n");
		w.write("<script LANGUAGE=\"JavaScript\" src=\"TreeFunctions.js\"> </script>\n");
		w.write("<input name=\"submitform\" type=\"button\" class=\"toolbutton\" value=\"Use selected genomes\" onclick=\"document.writeln(gettaxids());\"/>");
		w.write("</head>\n");
		w.write("<body>");
		w.write("<label>Genomes</label>");
		w.write("<label id=\"count_label\">0</label>");
		w.write("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");
		
		printHTMLTree(aaRoot, 0, w);
		
		w.write("</table>\n");
		w.write("</body>\n");
		w.write("</html>\n");

		w.flush();
		w.close();

		MaxDepth = getMaxDepth(dnaRoot);

		w = new FileWriter(new File(config.WEB_DIR, "dnaTree.html"));

		w.write("<!DOCTYPE HTML PUBLIC>\n");
		w.write("<html>\n");
		w.write("<head>\n");
		w.write("<title>Genomes Tree DNA</title>\n");
		w.write("<script LANGUAGE=\"JavaScript\" src=\"dnaTree.js\"> </script>\n");
		w.write("<script LANGUAGE=\"JavaScript\" src=\"TreeFunctions.js\"> </script>\n");
		w.write("<input name=\"submitform\" type=\"button\" class=\"toolbutton\" value=\"Use selected genomes\" onclick=\"document.writeln(gettaxids());\"/>");
		w.write("</head>\n");
		w.write("<body>");
		w.write("<label>Genomes</label>");
		w.write("<label id=\"count_label\">0</label>");
		w.write("<br/><div>");
		w.write("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

		printHTMLTree(dnaRoot, 0, w);

		w.write("</table>\n");
		w.write("</div>");
		w.write("</body>\n");
		w.write("</html>\n");

		w.flush();
		w.close();

	}

	private void printHTMLTree(HTMLGenNode n, int d, Writer w) throws Exception {
		w.write("<tr>\n");
		for (int i = 0; i < d; i++) {
			w.write("<td style=\" width:20px;\">&nbsp;</td>\n");
		}
		if (n.getChilds().size() == 0) {
			w.write("<td class=\"genomeleaf\" colspan=\"" + (MaxDepth - d + 1)
					+ "\"><INPUT type=\"checkbox\" id=\"" + n.getId()
					+ "\" onclick=\"checker(this);\"><label for=\"" + n.getId()
					+ "\">" + n.getSciName() + "</label></td>\n");
		} else {
			w.write("<td colspan=\"" + (MaxDepth - d + 1)
					+ "\"><INPUT type=\"checkbox\" id=\"" + n.getId()
					+ "\" onclick=\"checker(this);\"><label for=\"" + n.getId()
					+ "\">" + n.getSciName() + "</label></td>\n");
		}
		w.write("</tr>\n");
		Vector<HTMLGenNode> tmp = n.getChilds();
		Collections.sort(tmp);
		for (int i = 0; i < tmp.size(); i++) {
			printHTMLTree(tmp.get(i), d + 1, w);
		}
	}

	private void printRHTMLTree(HTMLGenNode n, int d, Writer w)
			throws Exception {

		// final int width = 20*d;
		final String hdiv = n.getId() + "gdiv";
		final String timg = n.getId() + "gimg";
		final String gchk = n.getId() + "gchk";
		// w.write("<span class=\"genomeindent\" style=\"width:"+width+"px;\">&nbsp;</span>\n");
		if (n.getChilds().size() == 0) {
			w.write("<div class=\"genomeleaf\">");
			w.write("<input type=\"checkbox\" id=\"" + gchk + "\" name=\""
					+ gchk + "\" onclick=\"checker(this);\">");
			w.write("<label class=\"genomelabel\" for=\"" + gchk + "\">"
					+ n.getSciName() + "</label>");
			w.write("</div>\n");
		} else {
			w.write("<div>");
			w.write("<a href=\"javascript:toggleplusminus($('" + timg
					+ "'),$('" + hdiv + "'));\">");
			w
					.write("<img id=\""
							+ timg
							+ "\" class=\"genomeimg\" src=\"/images/minus.gif\" alt=\"+-\" />");
			w.write("</a>");
			// no name for inner node so that these are not send with the html
			// form
			w.write("<input type=\"checkbox\" id=\"" + gchk
					+ "\" onclick=\"checker(this);\">");
			w.write("<label class=\"genomelabel\" for=\"" + gchk + "\">"
					+ n.getSciName() + "</label>");
			w.write("</div>\n");
		}

		if (n.getChilds().size() != 0)
			w.write("<div id=\"" + hdiv + "\" class=\"genomeindent\" >");
		Vector<HTMLGenNode> tmp = n.getChilds();
		Collections.sort(tmp);
		for (int i = 0; i < tmp.size(); i++) {
			printRHTMLTree(tmp.get(i), d + 1, w);
		}
		if (n.getChilds().size() != 0)
			w.write("</div>");
	}

	private void compress(HTMLGenNode n) {
		if (n.getChilds().size() == 1) {
			if (n.getChild(0).isLeaf()) {
				/*
				 * HTMLGenNode parent = n.getParent(); parent.deleteChild(n);
				 * n.getChild(0).setParent( parent );
				 * parent.addChild(n.getChilds().get(0));
				 */
			} else {
				HTMLGenNode ch = n.getChild(0);
				for (int i = ch.getChilds().size() - 1; i > -1; i--) {
					ch.getChild(i).setParent(n);
				}
				n.setChilds(ch.getChilds());
				compress(n);
			}
		} else {
			for (int i = n.getChilds().size() - 1; i > -1; i--) {
				compress(n.getChild(i));
			}
		}
	}

	private void ultra_compress(HTMLGenNode n, int level, int current_level) {
		if (current_level > level) {
			n.setChilds(makeSubsequentNodesToLeafs(n));
		} else {
			for (int i = n.getChilds().size() - 1; i > -1; i--) {
				ultra_compress(n.getChild(i), level, current_level + 1);
			}
		}
	}

	private Vector<HTMLGenNode> makeSubsequentNodesToLeafs(HTMLGenNode n) {
		Vector<HTMLGenNode> ret = new Vector<HTMLGenNode>();
		System.out.println("Get leaves(genomes) of " + n.getSciName() + " ("
				+ n.getId() + ")");
		for (int i = n.getChilds().size() - 1; i > -1; i--) {
			if (n.getChild(i).isLeaf()) {
				ret.add(n.getChild(i));
			} else {
				Vector<HTMLGenNode> tmp = makeSubsequentNodesToLeafs(n
						.getChild(i));
				for (int j = tmp.size() - 1; j > -1; --j)
					tmp.get(j).setParent(n);
				ret.addAll(tmp);
			}
		}
		return ret;
	}

	private int getMaxDepth(HTMLGenNode n) {
		if (n == null)
			return 0;
		if (n.isLeaf()) {
			return 0;
		} else {
			int max = 0;
			for (int i = n.getChilds().size() - 1; i > -1; i--) {
				max = Math.max(max, getMaxDepth(n.getChild(i)) + 1);
			}
			return max;
		}
	}

	private void load(File db) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(db));
		this.db = (GenomesDB) ois.readObject();
		ois.close();
	}

	/*
	 * private String getColor(int d){ //int r = Integer.valueOf("E6", 16);
	 * //int g = Integer.valueOf("F2", 16); //int b = Integer.valueOf("F2", 16);
	 * int r = Integer.valueOf("FF", 16); int g = Integer.valueOf("FF", 16); int
	 * b = Integer.valueOf("FF", 16); double f = 1.0-(d/(double)MaxDepth); r =
	 * (int)Math.round(f*r); // g = (int)Math.round(f*g); // b =
	 * (int)Math.round(f*b); if(d%2==0) return
	 * String.format("#%02x%02x%02x",220,255,190); else return
	 * String.format("#%02x%02x%02x",220,255,230); }
	 * 
	 * private String getMainDomains(){ StringBuffer dom = new
	 * StringBuffer("threedomains=["); getMainDomainsRec(aaRoot, dom);
	 * dom.append("];"); return dom.toString(); }
	 */

	/*
	 * private void getMainDomainsRec(HTMLGenNode n, StringBuffer dom){ boolean
	 * done = false; for(int i=0; i<n.getChilds().size(); i++){ if(
	 * n.getChild(i).getSciName().equalsIgnoreCase("bacteria") ){
	 * dom.append("\""+n.getChild(i).getId().toString()+"\"");
	 * if(i!=n.getChilds().size()-1) dom.append(", "); done = true; }else if(
	 * n.getChild(i).getSciName().equalsIgnoreCase("archaea") ){
	 * dom.append("\""+n.getChild(i).getId().toString()+"\"");
	 * if(i!=n.getChilds().size()-1) dom.append(", "); done = true; }else if(
	 * n.getChild(i).getSciName().equalsIgnoreCase("eukaryota") ){
	 * dom.append("\""+n.getChild(i).getId().toString()+"\"");
	 * if(i!=n.getChilds().size()-1) dom.append(", "); done = true; } } if(done)
	 * return; else for(int i=0; i<n.getChilds().size(); i++)
	 * getMainDomainsRec(n.getChild(i), dom); }
	 */

	public void toDo() {
		System.out
				.println("Now go to a shell with write access to the web directories and execute:\n");

		System.out.print(" cp "
				+ new File(config.BASE_DIR, "/web/dnatree.js").toString());
		System.out
				.print(" /cluster/www/toolkit/public/javascripts/dnatree.js;\n");

		System.out.print(" cp "
				+ new File(config.BASE_DIR, "/web/proteintree.js").toString());
		System.out
				.print(" /cluster/www/toolkit/public/javascripts/proteintree.js;\n");

		System.out.print(" cp "
				+ new File(config.BASE_DIR, "/web/_dnatree.rhtml").toString());
		System.out
				.print(" /cluster/www/toolkit/app/views/genomes/_dnatree.rhtml;\n");

		System.out.print(" cp "
				+ new File(config.BASE_DIR, "/web/_proteintree.rhtml")
						.toString());
		System.out
				.print(" /cluster/www/toolkit/app/views/genomes/_proteintree.rhtml;\n");
	}
}
