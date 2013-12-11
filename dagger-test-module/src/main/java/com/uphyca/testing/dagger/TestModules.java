
package com.uphyca.testing.dagger;

import java.lang.reflect.Constructor;

import com.uphyca.testing.dagger.internal.TestModuleProcessor;

public abstract class TestModules {

    private TestModules() {
        throw new UnsupportedOperationException();
    }

    public static Object from(Object testCase) {
        try {
            Class<?> testModuleClass = Class.forName(testCase.getClass()
                                                             .getName() + TestModuleProcessor.SUFFIX);
            Constructor<?> constructor = testModuleClass.getConstructor(testCase.getClass());
            return constructor.newInstance(testCase);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Test class should annotated with @TestModule", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
