/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coffee;

import com.uphyca.testing.dagger.TestModule;
import com.uphyca.testing.dagger.TestModules;
import com.uphyca.testing.dagger.TestProvides;
import dagger.ObjectGraph;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;

@TestModule
public class CoffeeMakerTest {

    @Inject CoffeeMaker coffeeMaker;
    @TestProvides @Mock Heater heater;

    @Before public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new DripCoffeeModule(), TestModules.from(this)).
                    inject(this);
    }

    @Test public void testHeaterIsTurnedOnAndThenOff() {
        Mockito.when(heater.isHot()).thenReturn(true);
        coffeeMaker.brew();
        Mockito.verify(heater, Mockito.times(1)).on();
        Mockito.verify(heater, Mockito.times(1)).off();
    }
}
