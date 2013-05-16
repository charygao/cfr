package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.EnumClassRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.NonStaticLifter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.StaticLifter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.DeadMethodRemover;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.getopt.CFRState;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 * Date: 14/05/2013
 * Time: 06:22
 * <p/>
 * Analysis which needs to be performed on the whole classfile in one go, once we've
 * performed other basic code analysis.
 */
public class CodeAnalyserWholeClass {
    public static void wholeClassAnalysis(ClassFile classFile, CFRState state) {
        /*
         * Whole class analysis / transformation - i.e. if it's an enum class, we will need to rewrite
         * several methods.
         */
        EnumClassRewriter.rewriteEnumClass(classFile, state);

        /*
         * All constructors of inner classes should have their first argument removed,
         * and it should be marked as hidden.
         */
        if (classFile.isInnerClass()) {
            fixInnerClassConstructors(classFile, state);
        }

        if (state.getBooleanOpt(CFRState.LIFT_CONSTRUCTOR_INIT)) {
            liftStaticInitialisers(classFile, state);

            liftNonStaticInitialisers(classFile, state);
        }

        if (state.getBooleanOpt(CFRState.REMOVE_DEAD_METHODS)) {
            removeDeadMethods(classFile, state);
        }
    }

    private static void fixInnerClassConstructors(ClassFile classFile, CFRState state) {
        if (classFile.testAccessFlag(AccessFlag.ACC_STATIC)) return;

        for (Method method : classFile.getConstructors()) {
            Op04StructuredStatement.fixInnerClassConstruction(state, method, method.getAnalysis());
        }
    }

    private static Method getStaticConstructor(ClassFile classFile) {
        Method staticInit;
        try {
            staticInit = classFile.getMethodByName(MiscConstants.STATIC_INIT_METHOD);
        } catch (NoSuchMethodException e) {
            return null;
        }
        return staticInit;
    }

    /* As much as possible, lift code from a <clinit> method into the declarations.
     * Because we can put arbitrary code in a clinit, this isn't always possible, however
     * we want to try because
     * a) it looks tidier!
     * b) interfaces MAY have static initialisers, but MAY NOT have clinit methods.
     *    (in java 1.7)
     */
    private static void liftStaticInitialisers(ClassFile classFile, CFRState state) {
        Method staticInit = getStaticConstructor(classFile);
        if (staticInit == null) return;
        new StaticLifter(classFile).liftStatics(staticInit);
    }

    private static void liftNonStaticInitialisers(ClassFile classFile, CFRState state) {
        new NonStaticLifter(classFile).liftNonStatics();
    }

    /*
     * Some methods can be completely removed if they're empty other than comments.
     *
     * default constructor
     * static constructor
     *
     * Obviously, this step has to come AFTER any constructor rewriting (static lifting)
     */
    private static void removeDeadMethods(ClassFile classFile, CFRState state) {
        Method staticInit = getStaticConstructor(classFile);
        if (staticInit != null) {
            DeadMethodRemover.removeDeadMethod(classFile, staticInit);
        }

        // If there's only one constructor, and it's the default (0 args, public, non final)
        // with no code, we can remove it.
        tryRemoveConstructor(classFile);
    }

    private static void tryRemoveConstructor(ClassFile classFile) {
        List<Method> constructors = classFile.getConstructors();
        if (constructors.size() != 1) return;
        Method constructor = constructors.get(0);

        // 0 args.
        MethodPrototype methodPrototype = constructor.getMethodPrototype();
        if ((methodPrototype.getArgs().size() - methodPrototype.getNumHiddenArguments()) > 0) return;
        // public, non final.
        if (constructor.testAccessFlag(AccessFlagMethod.ACC_FINAL)) return;
        if (!constructor.testAccessFlag(AccessFlagMethod.ACC_PUBLIC)) return;

        if (!MiscStatementTools.isDeadCode(constructor.getAnalysis())) return;
        classFile.removePointlessMethod(constructor);
    }
}
