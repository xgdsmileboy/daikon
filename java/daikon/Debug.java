package daikon;

import daikon.*;
import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

import utilMDE.*;

/**
 * Debug class used with the log4j logger to create standardized output.
 * It can be setup to track combinations of classes, program points,
 * and variables.  The most common class to track is an invariant, but
 * any class can be used.
 *
 * This allows detailed information about a particular class/ppt/variable
 * combination to be printed without getting lost in a mass of other
 * information (which is a particular problem in Daikon due to the volume
 * of data considered).
 *
 * Note that each of the three items (class, ppt, variable) must match
 * in order for a print to occur.
 **/

public class Debug {

  /** Debug Logger */
  public static final Logger debugTrack = Logger.getLogger ("daikon.Debug");

  /**
   * List of classes for logging. Each name listed is compared to the
   * fully qualified class name.  If it matches (shows up anywhere in
   * the class name) it will be included in debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackInvariant
    = {
      // "IntEqual",
      "IntGreaterEqual",
      "SequenceScalarSubscriptFactory",
    };

  /**
   * List of Ppts for logging. Each name listed is compared to
   * the full program point name. If it matches (shows up anywhere in
   * the ppt name) it will be included in the debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackPpt
    = {
      // "misc.Compar1.main(java.lang.String[]):::ENTER"
      "DataStructures.DisjSets.find(int):::EXIT"
    };

  /**
   * List of variable names for logging. Each name listed is compared
   * to each variable in turn.  If each matches exactly it will be
   * included in track debug prints.  This is not a regular expression
   * match.  Note that the number of variables must match the slice
   * exactly.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[][] debugTrackVars
    = {
      // { "this.topOfStack", "size(this.theArray[])-1" },
      { "size(this.s[])-1",  "orig(this.s[post(x-1)])" },
      { "size(this.s[])-1",  "orig(this.s[post(x)-1])" },
      { "orig(this.s[post(x-1)])", "size(this.s[])-1"  },
      // { "size(this.s[])-1",  "orig(this.s[post(x)])" },
      { "orig(this.s[])",    "x" },
      { "x",                 "orig(this.s[])" },
    };

  // cached standard parts of the debug print so that multiple calls from
  // the same context don't have to repeat these each time

  /** cached class */
  public Class cache_class;

  /** cached ppt */
  public Ppt cache_ppt;

  /** cached variables */
  public VarInfo cache_vis[];

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  public Debug (Class c, Ppt ppt, VarInfo[] vis) {
    set (c, ppt, vis);
  }

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  void set (Class c, Ppt ppt, VarInfo[] vis) {
    cache_class = c;
    cache_ppt = ppt;
    cache_vis = vis;
    if (c == null)
      System.out.println ("Class = null");
    if (ppt == null)
      System.out.println ("ppt = null");
    if (vis == null)
      System.out.println ("vis = null");
    for (int i = 0; i < vis.length; i++)
      if (vis[i] == null)
        System.out.println ("vis[" + i + "] == null");
  }


  /**
   * Determines whether or not traceback information is printed for each
   * call to log.
   *
   * @see #log(Logger, Class, Ppt, String)
   */
  public static boolean dkconfig_showTraceback = false;

  /**
   * Determines whether or not detailed info (such as from add_modified)
   * is printed
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logDetailed()
   */
  public static boolean dkconfig_logDetail = false;

  /**
   * Returns whether or not detailed logging is on.  Note that this check
   * is not performed inside the logging calls themselves, it must be
   * performed by the caller.
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logOn()
   */

  public static boolean logDetail () {
    return (dkconfig_logDetail && debugTrack.isDebugEnabled());
  }

  /**
   * Returns whether or not logging is on.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static boolean logOn() {
    return debugTrack.isDebugEnabled();
  }

  /**
   * Logs the cached class, cached ppt, cached variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   */

  public void log (Logger debug, String msg) {

    log (debug, cache_class, cache_ppt, cache_vis, msg);
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt, String msg) {

    log (debug, inv_class, ppt, ppt.var_infos, msg);
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the log4j logger.  The class, ppt, and variables are
   * checked against those described in {@link #debugTrackInvariant},
   * {@link #debugTrackPpt}, and {@link #debugTrackVars}.  Only
   * those that match are printed.  Variables will match if they are
   * in the same equality set.  The information is written as: <p>
   *
   * <code> class: ppt : var1 : var2 : var3 : msg </code> <p>
   *
   * Note that if {@link #debugTrack} is not enabled then
   * nothing is printed.  It is somewhat faster to check {@link #logOn()}
   * directly rather than relying on the check here. <p>
   *
   * Other versions of this method (noted below) work without the Logger
   * parameter and take class, ppt, and vis from the cached values
   *
   * @param debug       A second Logger to query if debug tracking is turned
   *                    off or does not match.  If this logger is
   *                    enabled, the same information will be written
   *                    to it.  Note that the information is never
   *                    written to both loggers.
   * @param inv_class   The class.  Can be obtained in a static context
   *                    by ClassName.class
   * @param ppt         Program point
   * @param vis         Variables at the program point.  These are sometimes
   *                    different from the ones in the ppt itself.
   * @param msg         String message to log
   *
   * @see #logOn()
   * @see #logDetail()
   * @see #log(Class, Ppt, VarInfo[], String)
   * @see #log(Class, Ppt, String)
   * @see #log(Logger, String)
   * @see #log(String)
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt,
                          VarInfo[] vis, String msg) {

    // Try to log via the logger first
    if (log (inv_class, ppt, vis, msg))
      return;

    // If debug isn't turned on, there is nothing to do
    if (!debug.isDebugEnabled())
      return;

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    for (int i = 0; i < vis.length; i++) {
      VarInfo v = vis[i];
      vars += v.name.name() + ": ";
    }
    for (int i = vis.length; i < 3; i++)
      vars += ": ";

    debug.debug (class_str + ": " + ppt.ppt_name.getFullNamePoint()
                 + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }
  }

 /**
  * Logs a description of the cached class, ppt, and variables and the
  * specified msg via the log4j logger as described in {@link
  * #log(Logger, Class, Ppt, VarInfo[], String)}
  *
  * @return whether or not it logged anything
  */

  public boolean log (String msg) {
    if (!logOn())
      return (false);
    return (log (cache_class, cache_ppt, cache_vis, msg));
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, String msg) {

    return (log (inv_class, ppt, ppt.var_infos, msg));
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the log4j logger as described in {@link #log(Logger,
   * Class, Ppt, String)}.  Accepts vis because sometimes the
   * variables are different from those in the ppt.
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, VarInfo vis[],
                             String msg) {

    if (!debugTrack.isDebugEnabled())
      return (false);

    // Make sure the class matches
    if ((debugTrackInvariant.length > 0) && (inv_class != null)) {
      if (!strContainsElem (inv_class.getName(), debugTrackInvariant))
        return (false);
    }

    // Make sure the Ppt matches
    if (debugTrackPpt.length > 0) {
      if (!strContainsElem (ppt.name, debugTrackPpt))
        return (false);
    }

    // Make sure the variables match our equality set
    // If our variable is not the leader, keep track of our variables name
    // so we can display it also.
    String ourvars[] = new String[3];
    if (debugTrackVars.length > 0) {
      boolean match = false;
      outer: for (int i = 0; i < debugTrackVars.length; i++) {
        String[] cv = debugTrackVars[i];
        if (cv.length != vis.length)
          continue;
        for (int j = 0; j < cv.length; j++) {
          boolean this_match = false;
          Set evars = null;
          if (vis[j].equalitySet != null)
            evars = vis[j].equalitySet.getVars();
          if (evars != null) {
            for (Iterator iter = evars.iterator(); iter.hasNext(); ) {
              VarInfo v = (VarInfo) iter.next();
              if (cv[j].equals ("*") || cv[j].equals (v.name.name())) {
                this_match = true;
                if (!v.isCanonical())
                  ourvars[j] = cv[j];
                break;
              }
            }
          } else { // sometimes, no equality set
            if (cv[j].equals ("*") || cv[j].equals (vis[j].name.name()))
              this_match = true;
          }
          if (!this_match)
            continue outer;
          // if (!cv[j].equals ("*") && !cv[j].equals (vis[j].name.name()))
          //  continue outer;
        }
        match = true;
        break outer;
      }
      if (!match)
        return (false);
    }

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    for (int i = 0; i < vis.length; i++) {
      VarInfo v = vis[i];
      vars += v.name.name();
      if (ourvars[i] != null)
        vars += " {" + ourvars[i] + "}";
      vars += ": ";
    }
    for (int i = vis.length; i < 3; i++)
      vars += ": ";

    debugTrack.debug (class_str + ": " + ppt.ppt_name.getFullNamePoint()
                       + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }

    return (true);
  }

  /**
   * Looks for an element in arr that is contained in str.
   */

  private static boolean strContainsElem (String str, String[] arr) {

    for (int i = 0; i < arr.length; i++) {
      if (str.indexOf (arr[i]) >= 0)
        return (true);

    }
    return (false);
  }


}
