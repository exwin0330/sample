package com.example.sample.flow;

import java.io.File;
import java.util.Collections;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

public class SootInitializer {
    public static void initialize(String className) {
        String sourceDirectory = System.getProperty("user.dir") +
                File.separator + "build" +
                File.separator + "intermediates" +
                File.separator + "javac" +
                File.separator + "debug" +
                File.separator + "classes";
        String androidJarPath = "C:/Users/ereve/AppData/Local/Android/Sdk/platforms/android-29/android.jar";
        String sootPath = "libs/soot-4.5.0-jar-with-dependencies.jar";

        String sootClassPath =
                sourceDirectory + File.pathSeparator +
                        androidJarPath + File.pathSeparator +
                        sootPath;

        G.reset();

        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));

        Options.v().set_android_jars(androidJarPath);
        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("cg.spark", "on");

        Options.v().set_keep_line_number(true);
        Options.v().set_keep_offset(true);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_whole_program(true);
        Options.v().set_output_format(Options.output_format_none);

        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "optimize:false");
        Options.v().setPhaseOption("jb.cp", "enabled:true");
        Options.v().setPhaseOption("jb.dae", "enabled:true");
        Options.v().setPhaseOption("jb.cp-agg", "enabled:true");

        Scene.v().addBasicClass(className, SootClass.BODIES);
        Scene.v().loadNecessaryClasses();
    }
}
