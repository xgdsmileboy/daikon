package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.SequenceLength;
import utilMDE.*;
import java.util.Iterator;

/**
 * Represents the invariant "x == r (mod m)" where x is a long scalar, r is
 * the remainder, and m is the modulus.
 **/

public class Modulus
  extends SingleScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff Modulus invariants should be considered.
   **/
  public static boolean dkconfig_enabled = false;

  long modulus = 0;
  long remainder = 0;

  // An arbitrarily-chosen value used for computing the differences among
  // all the values.  Arbitrary initial value 2222 will be replaced by the
  // first actual value seen.
  long value1 = 2222;
  // used for initializing value1
  boolean no_samples_seen = true;

  private Modulus(PptSlice ppt) {
    super(ppt);
  }

  public static Modulus instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    VarInfo x = ppt.var_infos[0];
    if ((x.derived instanceof SequenceLength)
         && (((SequenceLength) x.derived).shift != 0)) {
      // do not instantiate x-1 = a (mod b).  Instead, choose a different a.
      Global.implied_noninstantiated_invariants += 1;
      return null;
    }

    return new Modulus(ppt);
  }

  public String repr() {
    return "Modulus" + varNames() + ": "
      + "modulus=" + modulus + ",remainder=" + remainder;
  }

  public String format_using(OutputFormat format) {
    String name = var().name.name_using(format, var());

    if (format == OutputFormat.DAIKON) {
      return var().name.name() + " == " + remainder + "  (mod " + modulus + ")";
    }

    if (format == OutputFormat.IOA) {
      return "mod(" + var().name.ioa_name() + ", " + modulus + ") = " + remainder;
    }

    if (format.isJavaFamily()) {
      name = var().name.name_using(format, var());
      if (var().type.isFloat()) {
        return "daikon.Quant.fuzzy.eq(" + name + " % " + modulus + ", " + remainder + ")";
      } else {
        return name + " % " + modulus + " == " + remainder;
      }
    }

    //   if (format == OutputFormat.JAVA
    //     || format == OutputFormat.JML) {
    //   return var().name.name() + " % " + modulus + " == " + remainder;
    //  }

    if (format == OutputFormat.SIMPLIFY) {
      if (modulus > 0) {
        return "(EQ (MOD " + var().name.simplify_name() + " "
          + simplify_format_long(modulus) + ") "
          + simplify_format_long(remainder) + ")";
      } else {
        return format_too_few_samples(format, null);
      }
    }

    return format_unimplemented(format);
  }

  public InvariantStatus check_modified(long value, int count) {
    if (modulus == 1) {
      // We shouldn't ever get to this case; the invariant should have been
      // destroyed instead.
      throw new Error("Modulus = 1");
    } else if (no_samples_seen) {
      return InvariantStatus.NO_CHANGE;
    } else if (value == value1) {
      // no new information, so nothing to do
      return InvariantStatus.NO_CHANGE;
    } else if (modulus == 0) {
      // only one value seen so far
      // REACHABLE?
      if (modulus == 1) {
        return InvariantStatus.FALSIFIED;
      }
    } else {
      long new_modulus_long = Math.abs(MathMDE.gcd(modulus, value1 - value));
      int new_modulus;
      if (new_modulus_long > Integer.MAX_VALUE
          || (new_modulus_long < Integer.MIN_VALUE)) {
        new_modulus = 1;
      } else {
        new_modulus = (int) new_modulus_long;
        Assert.assertTrue(new_modulus > 0);
      }
      if (new_modulus != modulus) {
        if (new_modulus == 1) {
          return InvariantStatus.FALSIFIED;
        }
      }
    }
    Assert.assertTrue(modulus != 1);
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus add_modified(long value, int count) {
    if (modulus == 1) {
      // We shouldn't ever get to this case; the invariant should have been
      // destroyed instead.
      throw new Error("Modulus = 1");
      // Assert.assertTrue(falsified);
      // // We already know this confidence fails
      // return;
    } else if (no_samples_seen) {
      value1 = value;
      no_samples_seen = false;
      return InvariantStatus.NO_CHANGE;
    } else if (value == value1) {
      // no new information, so nothing to do
      return InvariantStatus.NO_CHANGE;
    } else if (modulus == 0) {
      // only one value seen so far
      long new_modulus = Math.abs(value1 - value);

      if (new_modulus == 1) {
        return InvariantStatus.FALSIFIED;
      }
      modulus = new_modulus;
      remainder = MathMDE.mod_positive(value, modulus);
    } else {
      long new_modulus_long = Math.abs(MathMDE.gcd(modulus, value1 - value));
      int new_modulus;
      if (new_modulus_long > Integer.MAX_VALUE
          || (new_modulus_long < Integer.MIN_VALUE)) {
        new_modulus = 1;
      } else {
        new_modulus = (int) new_modulus_long;
        Assert.assertTrue(new_modulus > 0);
      }
      if (new_modulus != modulus) {
        if (new_modulus == 1) {
          return InvariantStatus.FALSIFIED;
        } else {
          remainder = remainder % new_modulus;
          modulus = new_modulus;
        }
      }
    }
    Assert.assertTrue(modulus != 1);
    return InvariantStatus.NO_CHANGE;
  }

  //  public InvariantStatus check_modified(long value, int count) {}


  protected double computeConfidence() {
    if (modulus == 1)
      return Invariant.CONFIDENCE_NEVER;
    if (modulus == 0) {
      return Invariant.CONFIDENCE_UNJUSTIFIED;
    }
    double probability_one_elt_modulus = 1 - 1.0/modulus;
    // return 1 - Math.pow(probability_one_elt_modulus, ppt.num_mod_samples());
    return 1 - Math.pow(probability_one_elt_modulus, ppt.num_samples());
  }

  public boolean isSameFormula(Invariant other) {
    Modulus otherModulus = (Modulus) other;

    boolean thisMeaningless = (modulus == 0 || modulus == 1);
    boolean otherMeaningless = (otherModulus.modulus == 0 ||
                                otherModulus.modulus == 1);

    if (thisMeaningless && otherMeaningless) {
      return true;
    } else {
      return
        (modulus != 1) &&
        (modulus != 0) &&
        (modulus == otherModulus.modulus) &&
        (remainder == otherModulus.remainder);
    }
  }

  public boolean isExclusiveFormula(Invariant other) {
    if ((modulus == 0) || (modulus == 1))
      return false;

    // Weak test, can be strengthened.
    //  * x = 1 mod 4  is exclusive with  x = 6 mod 8
    //  * x = 1 mod 4  is exclusive with  x = 0 mod 2
    //  * x = 0 mod 4  is exclusive with  1 <= x <= 3
    if (other instanceof Modulus) {
      return ((modulus == ((Modulus) other).modulus)
              && (remainder != ((Modulus) other).remainder));
    } else if (other instanceof NonModulus) {
      return ((NonModulus) other).hasModulusRemainder(modulus, remainder);
    }

    return false;
  }

  // Look up a previously instantiated invariant.
  public static Modulus find(PptSlice ppt) {
    Assert.assertTrue(ppt.arity() == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof Modulus)
        return (Modulus) inv;
    }
    return null;
  }

}
