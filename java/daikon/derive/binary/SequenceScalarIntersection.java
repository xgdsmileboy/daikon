// ***** This file is automatically generated from SequencesIntersection.java.jpp

package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;
import org.apache.log4j.Category;

/**
 * Intersection between two comparable sequences.
 **/
public final class SequenceScalarIntersection
  extends BinaryDerivation
{
  public static final Category debug = Category.getInstance("daikon.derive.binary.SequenceScalarIntersection");

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SequenceScalarIntersection  derived variables should be generated.
   **/
  public static boolean dkconfig_enabled = false;

  public SequenceScalarIntersection (VarInfo vi1, VarInfo vi2) {
    super(vi1, vi2);
  }

  public ValueAndModified computeValueAndModifiedImpl(ValueTuple full_vt) {
    debug.debug("Computing value and modified");

    int mod1 = base1.getModified(full_vt);
    int mod2 = base2.getModified(full_vt);

    Object val1 = base1.getValue(full_vt);
    if (val1 == null)
      return ValueAndModified.MISSING_NONSENSICAL;
    long [] val1_array = (long []) val1;
    Object val2 = base2.getValue(full_vt);
    if (val2 == null)
      return ValueAndModified.MISSING_NONSENSICAL;
     long [] val2_array = (long []) val2;

    long [] tmp = new long [val1_array.length + val2_array.length];
    int size = 0;
    for (int i=0; i<val1_array.length; i++) {
      long  v = val1_array[i];
      if ((ArraysMDE.indexOf(val2_array, v)!=-1) &&
          (size==0 || (ArraysMDE.indexOf(ArraysMDE.subarray(tmp, 0, size), v)==-1)))
        tmp[size++] = v;
    }

    long [] intersect = ArraysMDE.subarray(tmp, 0, size);
    intersect = (long  []) Intern.intern(intersect);

    int mod = (((mod1 == ValueTuple.UNMODIFIED)
                && (mod2 == ValueTuple.UNMODIFIED))
               ? ValueTuple.UNMODIFIED
               : ValueTuple.MODIFIED);
    return new ValueAndModified(intersect, mod);
  }

  protected VarInfo makeVarInfo() {
    debug.debug ("Computing varInfo");

    VarInfoName name = base1.name.applyIntersection(base2.name);
    ProglangType type = base1.type;
    ProglangType file_rep_type = base1.file_rep_type;
    VarComparability compar = base1.comparability.elementType();
    VarInfoAux aux = base1.aux;
    return new VarInfo(name, type, file_rep_type, compar, aux);
  }

  public  boolean isSameFormula(Derivation other) {
    return (other instanceof SequenceScalarIntersection);
  }

}
