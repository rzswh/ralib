/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.automata.xml;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class RegisterAutomatonLoaderTest extends RaLibTestSuite {

    @Test
    public void testLoadingAutomaton1() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login.xml");

        logger.log(Level.FINE, "Printed: {0}", loader.getRegisterAutomaton());
        checkImportExportConsistency(loader.getRegisterAutomaton(), loader.getConstants());
    }

    @Test
    public void testLoadingAutomaton2() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/abp.output.xml");

        logger.log(Level.FINE, "Printed: {0}", loader.getRegisterAutomaton());
        checkImportExportConsistency(loader.getRegisterAutomaton(), loader.getConstants());
    }
    
    @Test
    public void testLoadingGearAutomaton() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/gear.xml");

        logger.log(Level.FINE, "Printed: {0}", loader.getRegisterAutomaton());
        checkImportExportConsistency(loader.getRegisterAutomaton(), loader.getConstants());
    }    
    
    
    @Test
    public void testLoadingCoolerAutomaton() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/cooler.xml");

        logger.log(Level.FINE, "Printed: {0}", loader.getRegisterAutomaton());
        checkImportExportConsistency(loader.getRegisterAutomaton(), loader.getConstants());
    }
    
    @Test
    public void testLoadingCounterAutomaton() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/counter.xml");

        logger.log(Level.FINE, "Printed: {0}", loader.getRegisterAutomaton());
        checkImportExportConsistency(loader.getRegisterAutomaton(), loader.getConstants());
    }
    
    public void checkImportExportConsistency(de.learnlib.ralib.automata.RegisterAutomaton ra, Constants consts) {
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
        RegisterAutomatonExporter.write(ra, consts, stream);
        ByteArrayInputStream bi = new ByteArrayInputStream(stream.toByteArray());
        RegisterAutomatonImporter importer = new RegisterAutomatonImporter(bi);
        Assert.assertEquals(importer.getRegisterAutomaton().toString(), ra.toString());
    }
}
