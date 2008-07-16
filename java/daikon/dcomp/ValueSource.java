package daikon.dcomp;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.io.PrintWriter;

import daikon.chicory.*;
import daikon.util.WeakIdentityHashMap;
import daikon.util.SimpleLog;
import daikon.util.ArraysMDE;
import daikon.util.Stopwatch;

/**
 * Class used in dataflow that creates a tree the defines the way that
 * each value is created.
 */
public class ValueSource {

  /** Description of the value, includes its source if it is a constant **/
  String descr;

  /** Stack trace of the location where this node was created **/
  Throwable stack_trace = null;

  /** Left subtree for binary/unary operations **/
  ValueSource left;

  /** Right subtree for binary operations **/
  ValueSource right;

  private static final String lineSep = System.getProperty("line.separator");

  ValueSource (String descr) { this.descr = descr; }
  ValueSource (String descr, Throwable stack_trace) {
    this.descr = descr;
    this.stack_trace = stack_trace;
  }
  ValueSource (String descr, Throwable stack_trace, ValueSource left,
               ValueSource right) {
    this.descr = descr;
    this.stack_trace = stack_trace;
    this.left = left;
    this.right= right;
  }

  /**
   * Returns a map from each test sequence variable (identified by
   * local-stores) to the values they are compared against.  The
   * compare_to param is presumed to be compared to all of the test
   * sequence variables found.
   */
  public Map<String,Set<String>> get_var_compares (String compare_to) {

    Map<String,Set<String>> var_compares
      = new LinkedHashMap<String,Set<String>>();
    for (String var : get_vars()) {
      Set<String> compare_to_set = var_compares.get (var);
      if (compare_to_set == null) {
        compare_to_set = new LinkedHashSet<String>();
        var_compares.put (var, compare_to_set);
      }
      compare_to_set.add (compare_to);
    }
    return var_compares;
  }

  /**
   * Returns a set of all of the test sequence variables in the tree
   * The source name of the variable is placed in the set.  This is
   * obtained from the local variable table information for the test
   * sequence
   */
  public Set<String> get_vars() {

    Set<String> varnames = new LinkedHashSet<String>();
    for (ValueSource vs : get_node_list()) {
      System.out.printf ("get_vars: processing node %s%n", vs.descr);
      if (vs.descr.startsWith ("local-store")) {
        int local_index = Integer.decode(vs.descr.split (" ")[1]);
        String local_name = DFInstrument.test_seq_locals[local_index];
        varnames.add (local_name);
      }
    }
    return varnames;
  }

  /**
   * Returns a list of all of the nodes in the tree
   */
  public List<ValueSource> get_node_list() {
    List<ValueSource> vs_list = new ArrayList<ValueSource>();
    add_node_list (vs_list);
    return vs_list;
  }

  /**
   * Add all of the nodes in this tree to vs_list
   */
  private void add_node_list (List<ValueSource> vs_list) {
    vs_list.add (this);
    if (left != null)
      left.add_node_list (vs_list);
    if (right != null)
      right.add_node_list (vs_list);
  }

  public Throwable get_stack_trace() {
    return stack_trace;
  }
  public String toString() {
    String left_descr = "-";
    if (left != null)
      left_descr = left.toString();
    String right_descr = "-";
    if (right != null)
      right_descr = right.toString();
    return String.format ("(%s %s/%s)", descr, left_descr, right_descr);
  }

  public String tree_dump () {
    return tree_dump (0);
  }

  public String tree_dump(int indent) {

    String indent_str = "";
    for (int i = 0; i < indent; i++)
      indent_str += " ";
    String out = String.format ("%s-%s", indent_str, descr);
    if (stack_trace != null) {
      indent_str += "   ";
      for (StackTraceElement ste : stack_trace.getStackTrace()) {
        if (ste.getClassName().startsWith ("daikon.dcomp.DCRuntime"))
          continue;
        out += String.format ("%n%s%s", indent_str, ste);
      }
    }
    if (left != null)
      out += lineSep + left.tree_dump (indent+2);
    if (right != null)
      out += lineSep + right.tree_dump (indent+2);
    return out;
  }
}