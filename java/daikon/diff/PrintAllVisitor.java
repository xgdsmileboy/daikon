package daikon.diff;

import java.io.*;

public class PrintAllVisitor implements NodeVisitor {
  
  private StringWriter sw;
  private PrintWriter pw;
  private boolean verbose;

  public PrintAllVisitor(boolean verbose) {
    this.verbose = verbose;
    sw = new StringWriter();
    pw = new PrintWriter(sw);
  }

  public String getOutput() {
    return sw.toString();
  }

  public void visitRootNode(RootNode node) {
    pw.println("root");
  }

  public void visitPptNode(PptNode node) {
    pw.print("  " + "<");
    if (node.getPpt1() == null) {
      pw.print((String) null);
    } else {
      pw.print(node.getPpt1().name);
    }
    pw.print(", ");
    if (node.getPpt2() == null) {
      pw.print((String) null);
    } else {
      pw.print(node.getPpt2().name);
    }
    pw.println(">");
  }

  public void visitInvNode(InvNode node) {
    pw.print("    " + "<");
    if (node.getInv1() == null) {
      pw.print((String) null);
    } else {
      if (verbose)
        pw.print(node.getInv1().repr_prob());
      else
        pw.print(node.getInv1().format());
    }
    pw.print(", ");
    if (node.getInv2() == null) {
      pw.print((String) null);
    } else {
      if (verbose)
        pw.print(node.getInv2().repr_prob());
      else
        pw.print(node.getInv2().format());
    }
    pw.println(">");
  }

}
