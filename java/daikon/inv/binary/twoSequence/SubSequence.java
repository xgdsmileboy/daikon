package daikon.inv.binary.twoSequence;

import daikon.*;
import daikon.inv.*;
import daikon.derive.*;
import daikon.derive.binary.*;

import java.util.*;
import utilMDE.*;

class SubSequence extends TwoSequence {

  public boolean var1_in_var2 = true;
  public boolean var2_in_var1 = true;

  protected SubSequence(PptSlice ppt) {
    super(ppt);
  }

  public static SubSequence instantiate(PptSlice ppt) {
    VarInfo var1 = ppt.var_infos[0];
    VarInfo var2 = ppt.var_infos[1];
    // System.out.println("SubSequence.isObviousDerived(" + format() + ") = "
    //                    + ((SubSequence.isObviousDerived(var1(), var2()))
    //                       || (SubSequence.isObviousDerived(var2(), var1()))));
    if ((SubSequence.isObviousDerived(var1, var2))
        || (SubSequence.isObviousDerived(var2, var1))) {
      Global.implied_noninstantiated_invariants++;
      return null;
    }

    return new SubSequence(ppt);
  }

  public String repr() {
    double probability = getProbability();
    return "SubSequence" + varNames() + ": "
      + "1in2=" + var1_in_var2
      + ",2in1=" + var2_in_var1
      + ",no_invariant=" + no_invariant
      + "; probability = " + probability;
  }

  public String format() {
    if (var1_in_var2 && var2_in_var1) {
      return var1().name + " is a {sub,super}sequence of " + var2().name;
    } else {
      VarInfo subvar = (var1_in_var2 ? var1() : var2());
      VarInfo supervar = (var1_in_var2 ? var2() : var1());
      return subvar.name + " is a subsequence of " + supervar.name;
    }
  }


  public void add_modified(long[] a1, long[] a2, int count) {
    if (var1_in_var2 && (ArraysMDE.indexOf(a2, a1) == -1)) {
        var1_in_var2 = false;
        if (!var2_in_var1) {
          destroy();
          return;
        }
    }
    if (var2_in_var1 && (ArraysMDE.indexOf(a1, a2) == -1)) {
        var2_in_var1 = false;
        if (!var1_in_var2) {
          destroy();
          return;
        }
    }
    Assert.assert(var1_in_var2 || var2_in_var1);
  }


  protected double computeProbability() {
    if (no_invariant)
      return Invariant.PROBABILITY_NEVER;
    else if (var1_in_var2 && var2_in_var1)
      return Invariant.PROBABILITY_UNKNOWN;
    else
      return Invariant.PROBABILITY_JUSTIFIED;
  }

  // This is abstracted out so it can be called by SuperSequence as well.
  static boolean isObviousDerived(VarInfo subvar, VarInfo supervar) {
    // System.out.println("static SubSequence.isObviousDerived(" + subvar.name + ", " + supervar.name + ") " + subvar.isDerivedSubSequenceOf() + " " + supervar.isDerivedSubSequenceOf());

    VarInfo subvar_super = subvar.isDerivedSubSequenceOf();
    if (subvar_super == null)
      return false;

    if ((subvar_super == supervar)
        // whatever we come up with, it will be obvious from the
        // IntComparison relationship over the lengths.
        || (subvar_super == supervar.isDerivedSubSequenceOf()))
      return true;

    return false;
  }


  // Look up a previously instantiated SubSequence relationship.
  public static SubSequence find(PptSlice ppt) {
    Assert.assert(ppt.arity == 2);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof SubSequence)
        return (SubSequence) inv;
    }
    return null;
  }


  // A subseq B[0..n] => A subseq B
  // Two ways to go about this:
  //   * look at all subseq relationships, see if one is over a variable of
  //     interest
  //   * look at all variables derived from the

  // (Seems overkill to check for other transitive relationships.
  // Eventually that is probably the right thing, however.)
  public boolean isObviousImplied() {

    // System.out.println("checking isObviousImplied for: " + format());

    if (var1_in_var2 && var2_in_var1) {
      // Suppress this invariant; we should get an equality invariant from
      // elsewhere.
      return true;
    } else {
      VarInfo subvar = (var1_in_var2 ? var1() : var2());
      VarInfo supervar = (var1_in_var2 ? var2() : var1());

      Ppt ppt_parent = ppt.parent;
      Vector derivees = supervar.derivees;
      // For each variable derived from supervar ("B")
      for (int i=0; i<derivees.size(); i++) {
        Derivation der = (Derivation) derivees.elementAt(i);
        if (der instanceof SequenceScalarSubsequence) {
          // If that variable is "B[0..n]"
          VarInfo supervar_part = der.getVarInfo();
          if (supervar_part.isCanonical()) {
            PptSlice ss_ppt = ppt.parent.getView(subvar, supervar_part);
            // System.out.println("  ... considering " + supervar_part.name);
            // if (ss_ppt == null) {
            //   System.out.println("      no ppt for " + subvar.name + " " + supervar_part.name);
            //   Assert.assert(ppt.parent.getView(supervar_part, subvar) == null);
            // }
            if (ss_ppt != null) {
              SubSequence ss = SubSequence.find(ss_ppt);
              if ((ss != null) && ss.justified()) {
                return true;
              }
            }
          }
        }
      }
      return false;
    }
  }


  public boolean isSameFormula(Invariant other)
  {
    Assert.assert(other instanceof SubSequence);
    return true;
  }

}
