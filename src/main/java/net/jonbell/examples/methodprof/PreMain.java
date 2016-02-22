package net.jonbell.examples.methodprof;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;

import net.jonbell.examples.methodprof.inst.MethodProfilingCV;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import sun.misc.Unsafe;

public class PreMain {
    public static Object helper;
    public static String helper_name = "net/jonbell/PreloadHelper";

    public static void premain(final String args, final Instrumentation inst) {
        if (args == null)
            throw new IllegalArgumentException("Please pass an arg (with =) to the javaagent to indicate where to save the output");
        System.err.println("On exit, will dump method coverage to " + args);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                File f = new File(args);
                FileWriter fw = null;
                try {
                    fw = new FileWriter(f);
                    HashSet<String> hit = ProfileLogger.dump();
                    for (String s : hit)
                        fw.write(s + "\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));
        inst.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                try {
                    if (className.startsWith("net/jonbell"))
                        return classfileBuffer;
                    ClassReader cr = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new MethodProfilingCV(cw, classBeingRedefined != null);
                    cr.accept(cv, 0);
                    return cw.toByteArray();
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }
        }, true);
        ArrayList<Class<?>> toRetransform = new ArrayList<Class<?>>();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (!c.getName().startsWith("net.jonbell") && inst.isModifiableClass(c) && c != Object.class && c != Unsafe.class)
                toRetransform.add(c);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, helper_name, null, "java/lang/Object", new String[0]);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, new String[0]);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        for (Class<?> c : toRetransform) {
            String key = "__instHit" + c.getName().replace('.', '_');
            cw.visitField(Opcodes.ACC_PUBLIC, key, "Z", null, 0);
            for (Method m : c.getDeclaredMethods()) {
                String methodKey = key + m.getName() + Type.getMethodDescriptor(m).hashCode();
                cw.visitField(Opcodes.ACC_PUBLIC, methodKey, "Z", null, 0);
            }
            for (Constructor m : c.getDeclaredConstructors()) {
                String methodKey = key + "_init_" + Type.getConstructorDescriptor(m).hashCode();
                cw.visitField(Opcodes.ACC_PUBLIC, methodKey, "Z", null, 0);
            }
            {
                String methodKey = key + "_clinit_" + "()V".hashCode();
                cw.visitField(Opcodes.ACC_PUBLIC, methodKey, "Z", null, 0);
            }
        }
        mv = null;
        int i = 0;
        int n = 0;
        for (Class<?> c : toRetransform) {
            if (i % 50 == 0) {
                if (mv != null) {
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "__dumpMethodsHit" + n, "()V", null, null);
                mv.visitCode();
                n++;
            }
            String key = "__instHit" + c.getName().replace('.', '_');
            for (Method m : c.getDeclaredMethods()) {
                String methodKey = key + m.getName().replace('<', '_').replace('>', '_') + Type.getMethodDescriptor(m).hashCode();
                Label ok = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, helper_name, methodKey, "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, ok);
                mv.visitLdcInsn(c.getName().replace('.', '/') + "." + m.getName() + Type.getMethodDescriptor(m)); //Get the fully qualified name of this method
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ProfileLogger.class), "methodHit", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTFIELD, helper_name, methodKey, "Z");
                mv.visitLabel(ok);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            for (Constructor m : c.getDeclaredConstructors()) {
                String methodKey = key + "_init_".replace('>', '_') + Type.getConstructorDescriptor(m).hashCode();
                Label ok = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, helper_name, methodKey, "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, ok);
                mv.visitLdcInsn(c.getName().replace('.', '/') + "." + "<init>" + Type.getConstructorDescriptor(m)); //Get the fully qualified name of this method
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ProfileLogger.class), "methodHit", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTFIELD, helper_name, methodKey, "Z");
                mv.visitLabel(ok);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            {
                String methodKey = key + "_clinit_".replace('>', '_') + "()V".hashCode();
                Label ok = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, helper_name, methodKey, "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, ok);
                mv.visitLdcInsn(c.getName().replace('.', '/') + ".<clinit>()V"); //Get the fully qualified name of this method
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ProfileLogger.class), "methodHit", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitInsn(Opcodes.ICONST_0);

                mv.visitFieldInsn(Opcodes.PUTFIELD, helper_name, methodKey, "Z");
                mv.visitLabel(ok);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitFieldInsn(Opcodes.PUTFIELD, helper_name, key, "Z");

            i++;
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "__dumpMethodsHit", "()V", null, null);
        mv.visitCode();
        for (i = 0; i < n; i++) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, helper_name, "__dumpMethodsHit" + i, "()V", false);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        Unsafe theUnsafe = Unsafe.getUnsafe();
        byte[] b = cw.toByteArray();
        Class<?> helper_cl = theUnsafe.defineClass(helper_name, b, 0, b.length, PreMain.class.getClassLoader(), null);
        try {
            helper = helper_cl.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            inst.retransformClasses(toRetransform.toArray(new Class[toRetransform.size()]));
        } catch (UnmodifiableClassException e) {
            e.printStackTrace();
        }

    }
}
