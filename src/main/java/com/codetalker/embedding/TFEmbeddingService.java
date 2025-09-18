package com.codetalker.embedding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TFEmbeddingService attempts a TensorFlow-backed embedding when possible.
 * It tries TFLite Interpreter first, then a SavedModelBundle reflective path.
 * On any reflection failure it falls back to the in-JVM EmbeddingService.
 */
public class TFEmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(TFEmbeddingService.class);

    private final EmbeddingService fallback;
    private final ModelLoader loader;
    private final AtomicInteger reflectionFallbackCount = new AtomicInteger(0);

    public TFEmbeddingService() {
        this(new ModelLoader(), new EmbeddingService());
    }

    public TFEmbeddingService(ModelLoader loader, EmbeddingService fallback) {
        this.loader = loader;
        this.fallback = fallback;
    }

    // Convenience constructor for tests
    public TFEmbeddingService(ModelLoader loader, int dim) {
        this(loader, new EmbeddingService(dim));
    }

    public float[] embed(String text) {
        if (loader.isMock()) return fallback.embed(text);

        try {
            if (!loader.isModelLoaded()) loader.load();
        } catch (Exception e) {
            logger.debug("ModelLoader.load failed, using fallback", e);
            reflectionFallbackCount.incrementAndGet();
            return fallback.embed(text);
        }

        // TFLite Interpreter path (use helper)
        Object interpreter = loader.getInterpreterInstance();
        if (interpreter != null) {
            int dim = fallback.getDim();
            float[] base = fallback.embed(text);
            float[][] input = new float[1][dim];
            float[][] output = new float[1][dim];
            System.arraycopy(base, 0, input[0], 0, Math.min(base.length, dim));

            Method run = findMethod(interpreter.getClass(), "run", 2);
            if (run != null) {
                boolean ok = safeInvokeVoid(interpreter, run, input, output);
                if (ok) {
                    return output[0];
                } else {
                    reflectionFallbackCount.incrementAndGet();
                }
            }
        }

        // SavedModelBundle path
        Object smb = loader.getSavedModelBundleInstance();
        if (smb != null) {
            int dim = fallback.getDim();
            float[] base = fallback.embed(text);
            float[][] input = new float[1][dim];
            float[][] output = new float[1][dim];
            System.arraycopy(base, 0, input[0], 0, Math.min(base.length, dim));

            Class<?> smbCls = smb.getClass();
            Method directRun = findMethod(smbCls, "run", 2);
            if (directRun != null) {
                boolean ok = safeInvokeVoid(smb, directRun, input, output);
                if (ok) return output[0];
                reflectionFallbackCount.incrementAndGet();
            } else {
                // try session()->runner()->run() using helpers
                Method sessionMethod = findMethod(smbCls, "session", 0);
                if (sessionMethod != null) {
                    Object session = safeInvokeReturn(smb, sessionMethod);
                    if (session != null) {
                        Method runnerMethod = findMethod(session.getClass(), "runner", 0);
                        Object runner = runnerMethod != null ? safeInvokeReturn(session, runnerMethod) : null;
                        if (runner != null) {
                            Method run0 = findMethod(runner.getClass(), "run", 0);
                            if (run0 != null) {
                                Object res = safeInvokeReturn(runner, run0);
                                float[] arr = extractFloatArrayFromResult(res);
                                if (arr != null) return arr;
                                reflectionFallbackCount.incrementAndGet();
                            }
                        }
                    }
                }
            }
        }

        reflectionFallbackCount.incrementAndGet();
        return fallback.embed(text);
    }

    public int getReflectionFallbackCount() { return reflectionFallbackCount.get(); }

    // --- Helpers for reflective invocation and result extraction ---

    private static Method findMethod(Class<?> cls, String name, int paramCount) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) return m;
        }
        return null;
    }

    private static Object safeInvoke(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // swallow and return null as a signal of failure
            return null;
        }
    }

    private static boolean safeInvokeVoid(Object target, Method method, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            logger.debug("Reflective invocation failed for {} on {}: {}", method.getName(), target == null ? "null" : target.getClass().getName(), e.toString());
            return false;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            logger.debug("Reflective invocation threw for {} on {}: {}", method.getName(), target == null ? "null" : target.getClass().getName(), cause.toString());
            return false;
        }
    }

    private static Object safeInvokeReturn(Object target, Method method) {
        return safeInvoke(target, method);
    }

    private static float[] extractFloatArrayFromResult(Object res) {
        if (res == null) return null;
        if (res.getClass().isArray()) {
            Class<?> comp = res.getClass().getComponentType();
            if (comp == float.class) return (float[]) res;
            if (comp != null && comp.isArray() && comp.getComponentType() == float.class) return ((float[][]) res)[0];
        }
        if (res instanceof java.util.List<?>) {
            java.util.List<?> l = (java.util.List<?>) res;
            if (!l.isEmpty() && l.get(0) instanceof float[]) return (float[]) l.get(0);
        }
        return null;
    }

}
