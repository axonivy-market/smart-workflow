package com.axonivy.utils.ai.output.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.axonivy.utils.ai.output.DynamicAgent;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.java.IJavaConfiguration;
import ch.ivyteam.ivy.java.IJavaConfigurationManager;

/**
 * Dynamic Agent interface creator, that allows us to identify the structured output/return type at runtime.
 * Can be removed once the DefaultAiServices of LC4j give us more freedom to define the output without defining an Agent interface.
 */
public class StructuredOutputAgent {

  private static final Map<Class<?>, Class<?>> CACHE = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public static <R> Class<? extends DynamicAgent<R>> agent(Class<R> outputType) {
    return (Class<? extends DynamicAgent<R>>) CACHE.computeIfAbsent(outputType,
        StructuredOutputAgent::defineAgent);
  }

  @SuppressWarnings("unchecked")
  private static <R> Class<? extends DynamicAgent<R>> defineAgent(Class<R> outputType) {
    String interfaceName = "com/axonivy/utils/ai/output/DynamicAgentInterface" + outputType.getSimpleName();
    String methodName = "chat";
    String methodDescriptor = Type.getMethodDescriptor(Type.getType(outputType), Type.getType(String.class));
    byte[] classBytes = writeClass(interfaceName, methodName, methodDescriptor);
    var type = (Class<? extends DynamicAgent<R>>) new CustomClassLoader().defineClass("com.axonivy.utils.ai.output.DynamicAgentInterface" + outputType.getSimpleName(), classBytes);
    Ivy.log().debug("defined " + type);
    return type;
  }

  private static byte[] writeClass(String interfaceName, String methodName, String methodDescriptor) {
    ClassWriter cw = new ClassWriter(0);
    cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
        interfaceName, null, "java/lang/Object", new String[] {"com/axonivy/utils/ai/output/DynamicAgent"});
    MethodVisitor methods = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT,
        methodName,
        methodDescriptor,
        null,
        null);
    methods.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  static class CustomClassLoader extends ClassLoader {

    public CustomClassLoader() {
      super(caller());
    }

    private static ClassLoader caller() {
      IJavaConfiguration caller = IJavaConfigurationManager.instance().getJavaConfiguration(IProcessModelVersion.current());
      return caller.getClassLoader();
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
      return defineClass(name, bytecode, 0, bytecode.length);
    }

  }

}
