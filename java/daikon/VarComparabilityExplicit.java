package daikon;

import java.util.*;
import java.io.*;

import com.oroinc.text.regex.*;

import utilMDE.*;


/**
 * A (explicit) VarComparable is:
 * <ul>
 *   <li>a list of names of comparable variables (in the "base" slot).
 *     If base == null, then every comparability check succeeds; the variable
 *     is considered comparable to every other variable.
 *   <li>for array types:  also, for each index, a list of variable names
 *     that are comparable to the index
 *   <li>If the alias slot is set to a String,
 *     then treat this variable as if it has the specified name.
 *     The point of "alias" is that an explicit VarComparability lists names of
 *     other varibles to which it is comparable; but we introduce new variables
 *     which we want to be considered comparable to others.  So we have to
 *     indicate what names those newly-introduced varaibles are comparable
 *     (actually, identical in terms of VarComparability) to.
 *   <li>special:  the string "always" means always comparable
 *     I represent this by base == null
 * </ul>
 **/
public final class VarComparabilityExplicit extends VarComparability implements java.io.Serializable {

  // All strings that appear in VarComparabilityExplicit objects are interned.

  String[] base;
  String[][] indices;
  int dimensions;		// indicates how many of the indices are in use;
				// there may be more indices than this
  String alias;

  // These are caches to avoid recomputation.  Their contents are are not
  // used for direct comparison but are constructed on demand (say, when a
  // derived varible is created and must be given a VarComparability).
  private VarComparabilityExplicit cached_element_type;
  private VarComparabilityExplicit[] indexTypes; // access through indexType() accessor


  VarComparabilityExplicit(String[] base, String[][] indices, int dimensions,
			   String alias) {
    this.base = base;
    this.indices = indices;
    this.dimensions = dimensions;
    this.alias = alias;
    indexTypes = new VarComparabilityExplicit[dimensions];
  }

  public VarComparability makeAlias(String name) {
    return new VarComparabilityExplicit(base, indices, dimensions, name);
  }

  public boolean alwaysComparable() {
    return (base == null);
  }

  public VarComparability elementType() {
    Assert.assert(dimensions > 0);
    if (cached_element_type == null) {
      cached_element_type = new VarComparabilityExplicit(base, indices, dimensions-1, alias);
    }
    return cached_element_type;
  }

  public VarComparability indexType(int dim) {
    if (indexTypes[dim] == null) {
      indexTypes[dim] = new VarComparabilityExplicit(indices[dim], null, 0,
						((alias == null)
						 ? null
						 : VarNames.indexVar(alias, dim)));
    }
    return indexTypes[dim];
  }

  final static PatternMatcher re_matcher = Global.regexp_matcher;

  static VarComparabilityExplicit parse(String rep, ProglangType vartype) {
    String rep_ = rep;          // for debugging

    if (vartype.isArray()) {
      // The VarComparability is of the form
      //  (var1 var2 var3)[var1 var2 var3][var1 var2 var3]
      int dims = vartype.dimensions();
      // Permit "[]" to appear in variable names.
      re_matcher.matches(rep, dims_regexp(dims));
      MatchResult match = re_matcher.getMatch();
      // "+2" because "groups" includes the whole match (group 0).
      Assert.assert(match.groups() == dims+2);

      String base_raw = match.group(1);
      String[] indices_raw = new String[dims];
      for (int i=0; i<dims; i++) {
	indices_raw[i] = match.group(i+2);
      }

      // I could consider interning the arrays themselves; is that
      // worthwhile?  Probably not...

      String[] new_base = ws_split_to_interned_array(base_raw);
      String[][] new_indices = new String[dims][];
      for (int i=0; i<dims; i++) {
	new_indices[i] = ws_split_to_interned_array(indices_raw[i]);
      }

      return new VarComparabilityExplicit(new_base, new_indices, dims, null);

    } else {
      // scalar variable
      // would this be faster?
      //   ((rep.charAt(0) == '(') && (rep.charAt(rep.length-1) == '('))
      if (rep.startsWith("(") && rep.endsWith(")")) {
	rep = rep.substring(1, rep.length()-1);
      }
      String[] new_base = ws_split_to_interned_array(rep);
      return new VarComparabilityExplicit(new_base, null, 0, null);
    }
  }

  static Vector dims_regexps = new Vector();
  static Perl5Pattern dims_regexp(int dims) {
    if (dims_regexps.size() <= dims) {
      // add all the elements up to the missing one
      for (int i=dims_regexps.size(); i<=dims; i++) {
	StringBuffer regexp = new StringBuffer("^\\(([^)]*)\\)");
	for (int j=0; j<dims; j++)
	  regexp.append("\\[\\(?((?:[^\\]\\)]|\\[\\])*)\\)?\\]");
	regexp.append("$");
	try {
	  dims_regexps.add(Global.regexp_compiler.compile(regexp.toString()));
	} catch (Exception e) {
          e.printStackTrace();
	  throw new Error(e.toString());
	}
      }
    }
    return (Perl5Pattern) dims_regexps.elementAt(dims);
  }



  /**
   * Split on whitespace and return an array of the resulting words, interned.
   **/
  static final String[] ws_split_to_interned_array(String s) {
    PatternMatcher re_matcher = Global.regexp_matcher;

    Vector vec = Util.split(re_matcher, Global.ws_regexp, s);
    String[] result = (String[]) vec.toArray(new String[0]);
    Intern.internStrings(result);
    return result;
  }


  // This is the key function of the class.
  // I could also add a member version.
  static boolean compatible(String name1_, VarComparabilityExplicit type1,
			    String name2_, VarComparabilityExplicit type2) {
    if (type1.alwaysComparable() || type2.alwaysComparable())
      return true;

    if (type1.dimensions != type2.dimensions)
      return false;
    int dims = type1.dimensions;

    String name1 = name1_, name2 = name2_;
    if (type1.alias != null)
      name1 = type1.alias;
    if (type2.alias != null)
      name2 = type2.alias;

    Assert.assert((ArraysMDE.indexOf(type1.base, name2) == -1)
		  == (ArraysMDE.indexOf(type2.base, name1) == -1));
    if (ArraysMDE.indexOf(type1.base, name2) == -1)
      return false;

    // Check each dimension of array

    for (int i=0; i<dims; i++) {
      String indexvar1 = VarNames.indexVar(name1, i);
      String indexvar2 = VarNames.indexVar(name2, i);
      if (!compatible(indexvar1, (VarComparabilityExplicit)type1.indexType(i),
                      indexvar2, (VarComparabilityExplicit)type2.indexType(i)))
	return false;
    }

    return true;
  }
  
  // Interning is lost when an object is serialized and deserialized.
  // Manually re-intern any interned fields upon deserialization.
  private void readObject(ObjectInputStream in) throws
  IOException, ClassNotFoundException {
    in.defaultReadObject();

    Intern.internStrings(base);
    for (int i=0; i<dimensions; i++) {
      Intern.internStrings(indices[i]);
    }
  }


}
