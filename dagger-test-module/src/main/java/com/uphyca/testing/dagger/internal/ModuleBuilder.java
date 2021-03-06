
package com.uphyca.testing.dagger.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class ModuleBuilder {

    private final Types typeUtil;
    private final String classPackage;
    private final String className;
    private final String testClassName;
    private final String targetClass;
    private final List<VariableElement> variableElements = new ArrayList<VariableElement>();

    ModuleBuilder(Types typeUtil, String classPackage, String className, String testClassName, String targetClass) {
        this.typeUtil = typeUtil;
        this.classPackage = classPackage;
        this.testClassName = testClassName;
        this.className = className;
        this.targetClass = targetClass;
    }

    String getFqcn() {
        return classPackage + "." + className;
    }

    String brewJava() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from dagger-test-module. Do not modify!\n");
        builder.append("package ")
               .append(classPackage)
               .append(";\n\n");
        builder.append("import javax.inject.Singleton;\n");
        builder.append("import dagger.Module;\n");
        builder.append("import dagger.Provides;\n\n");
        builder.append("@Module(injects = ")
               .append(targetClass)
               .append(".class, library = true, complete = false, overrides = true)\n");
        builder.append("public final class ")
               .append(className)
               .append(" {\n");
        builder.append("private final ")
               .append(testClassName)
               .append(' ')
               .append("mTestCase;\n\n");
        builder.append("public ")
               .append(className)
               .append('(')
               .append(testClassName)
               .append(' ')
               .append("testCase")
               .append(')')
               .append("{\n");
        builder.append("mTestCase = testCase;\n")
               .append("}\n\n");
        for (VariableElement each : variableElements) {
            TypeElement typeElement = (TypeElement) typeUtil.asElement(each.asType());
            builder.append("@Provides\n");
            builder.append("@Singleton\n");
            String qualifier = acquireQualifier(each);
            if (qualifier != null) {
                builder.append("@" + qualifier + "\n");
            }
            builder.append(typeElement.getQualifiedName());
            String[] types = acquireGenericTypes(each);
            if (types.length > 0) {
                builder.append('<');
                for (int i = 0, size = types.length; i < size; ++i) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    builder.append(types[i]);
                }
                builder.append('>');
            }
            builder.append(' ')
                   .append("provide$$")
                   .append(each.getSimpleName())
                   .append("(){\n");
            builder.append("return mTestCase.")
                   .append(each.getSimpleName())
                   .append(";\n");
            builder.append("}\n\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    public void addInjections(List<VariableElement> variableElements) {
        this.variableElements.addAll(variableElements);
    }

    private String[] acquireGenericTypes(VariableElement variable) {
        DeclaredType declaredType = (DeclaredType) variable.asType();
        List<String> types = new ArrayList<String>();
        for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
            TypeElement type = (TypeElement) typeUtil.asElement(typeMirror);
            types.add(type.getQualifiedName()
                          .toString());
        }
        return types.toArray(new String[types.size()]);
    }

    private String acquireQualifier(VariableElement variable) {
        for (AnnotationMirror each : variable.getAnnotationMirrors()) {
            DeclaredType annotationType = each.getAnnotationType();
            for (AnnotationMirror annotationMirror : annotationType.asElement()
                                                                   .getAnnotationMirrors()) {
                TypeElement type = (TypeElement) annotationMirror.getAnnotationType()
                                                                 .asElement();
                if (type.getQualifiedName()
                        .toString()
                        .equals("javax.inject.Qualifier")) {
                    TypeElement qualifierType = (TypeElement) annotationType.asElement();
                    String qualifiedName = qualifierType.getQualifiedName()
                                                        .toString();
                    if (!qualifiedName.equals("javax.inject.Named")) {
                        return qualifiedName;
                    }

                    Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = each.getElementValues();
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elementValue : elementValues.entrySet()) {

                        if (elementValue.getKey()
                                        .getSimpleName()
                                        .toString()
                                        .equals("value")) {
                            String value = elementValue.getValue()
                                                       .getValue()
                                                       .toString();
                            return qualifiedName + "(\"" + value + "\")";
                        }
                    }
                    return qualifiedName;
                }
            }
        }
        return null;
    }
}
