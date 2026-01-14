package sri;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

/**
 * Java API wrapper for the Sri Ruby interpreter.
 *
 * Example usage:
 * <pre>
 *     Object result = Sri.eval("10 + 20");
 *     System.out.println(result); // 30
 * </pre>
 */
public class Sri {
    private static final IFn evalString;

    static {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("sri.core"));
        evalString = Clojure.var("sri.core", "eval-string");
    }

    /**
     * Evaluate Ruby code and return the result.
     *
     * @param rubyCode the Ruby code to evaluate
     * @return the result of evaluation
     */
    public static Object eval(String rubyCode) {
        return evalString.invoke(rubyCode);
    }
}
