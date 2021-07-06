package com.arise.filter.route.script;

import javax.script.*;
import java.util.HashMap;

public class JSEngine {

    public static void main(String[] args) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        if (engine instanceof Compilable) {
            Compilable compEngine = (Compilable) engine;
            try {
                CompiledScript script = compEngine.compile(
                        "(function count() {\n" +
                                "    counter = counter + 1;\n" +
                                "    return counter;\n" +
                                "})();");
//                engine.put("counter", 1);
                System.out.println(script.eval(new SimpleBindings(new HashMap<String, Object>() {
                    {
                        put("counter", 1);
                    }
                })));
                System.out.println(script.eval());
                System.out.println(script.eval());
            } catch (ScriptException e) {
                System.err.println(e);
            }
        } else {
            System.err.println("Engine can't compile code");
        }
    }

}