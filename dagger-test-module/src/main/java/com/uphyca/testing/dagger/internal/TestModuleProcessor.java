
package com.uphyca.testing.dagger.internal;

import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import com.uphyca.testing.dagger.TestModule;
import com.uphyca.testing.dagger.TestProvides;

@SupportedAnnotationTypes("com.uphyca.testing.dagger.TestModule")
public class TestModuleProcessor extends AbstractProcessor {

    public static final String SUFFIX = "$$TestModule";

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportTypes = new LinkedHashSet<String>();
        supportTypes.add(TestModule.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        Map<TypeElement, ModuleBuilder> targetClassMap = new LinkedHashMap<TypeElement, ModuleBuilder>();
        findAndParseTargets(env, targetClassMap);
        for (Map.Entry<TypeElement, ModuleBuilder> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            ModuleBuilder builder = entry.getValue();
            try {
                writeJavaFile(typeElement, builder);
            } catch (IOException e) {
                error(typeElement, "Unable to write injector for type %s: %s", typeElement, e.getMessage());
            }
        }
        return true;
    }

    private void findAndParseTargets(RoundEnvironment env, Map<TypeElement, ModuleBuilder> targetClassMap) {
        for (Element element : env.getElementsAnnotatedWith(TestModule.class)) {
            parseTestModule(element, targetClassMap);
        }
    }

    private void parseTestModule(Element element, Map<TypeElement, ModuleBuilder> targetClassMap) {
        TypeElement typeElement = (TypeElement) element;
        ModuleBuilder moduleBuilder = getOrCreateTargetClass(targetClassMap, typeElement, typeElement);
        InjectVisitor visitor = new InjectVisitor();
        findInjections(typeElement, visitor);
        moduleBuilder.addInjections(visitor.getVariableElements());
    }

    private void findInjections(TypeElement typeElement, InjectVisitor visitor) {
        for (Element each : typeElement.getEnclosedElements()) {
            each.accept(visitor, null);
        }
    }

    private String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName()
                   .toString()
                   .substring(packageLen)
                   .replace('.', '$');
    }

    private ModuleBuilder getOrCreateTargetClass(Map<TypeElement, ModuleBuilder> targetClassMap, TypeElement typeElement, TypeElement injectionTypeElement) {
        ModuleBuilder moduleBuilder = targetClassMap.get(typeElement);
        if (moduleBuilder == null) {
            String targetType = injectionTypeElement.getQualifiedName()
                                                    .toString();
            String classPackage = getPackageName(typeElement);
            String className = getClassName(typeElement, classPackage) + SUFFIX;
            moduleBuilder = new ModuleBuilder(typeUtils, classPackage, className, typeElement.getQualifiedName()
                                                                                             .toString(), targetType);
            targetClassMap.put(typeElement, moduleBuilder);
        }
        return moduleBuilder;
    }

    private void error(Element element, String message, Object... args) {
        processingEnv.getMessager()
                     .printMessage(ERROR, String.format(message, args), element);
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type)
                           .getQualifiedName()
                           .toString();
    }

    private void writeJavaFile(TypeElement type, ModuleBuilder builder) throws IOException {
        JavaFileObject jfo = filer.createSourceFile(builder.getFqcn(), type);
        Writer writer = jfo.openWriter();
        writer.write(builder.brewJava());
        writer.flush();
        writer.close();
    }

    private static class InjectVisitor extends SimpleElementVisitor6<Void, Void> {

        private final List<VariableElement> mVariableElements = new ArrayList<VariableElement>();

        @Override
        public Void visitVariable(VariableElement r, Void p) {
            for (AnnotationMirror each : r.getAnnotationMirrors()) {

                TypeElement type = (TypeElement) each.getAnnotationType()
                                                     .asElement();
                if (type.getQualifiedName()
                        .toString()
                        .equals(TestProvides.class.getName())) {
                    mVariableElements.add(r);
                }
            }
            return super.visitVariable(r, p);
        }

        public List<VariableElement> getVariableElements() {
            return mVariableElements;
        }
    }
}
