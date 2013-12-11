
package com.uphyca.testing.dagger.internal;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
               .append(".class, complete = false, overrides = true)\n");
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
            builder.append(typeElement.getQualifiedName())
                   .append(' ')
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
}
