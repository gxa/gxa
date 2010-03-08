/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */
package uk.ac.ebi.gxa.properties;

import static junit.framework.Assert.*;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author pashky
 */
public class ChainedStorageTest {

    @Test
    public void test_getProperty() {
        ChainedStorage storage = new ChainedStorage();
        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return "abc";
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        assertEquals("abc", storage.getProperty("dummy"));

        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        assertNull(storage.getProperty("dummy"));        
    }

    @Test
    public void test_setProperty() {
        ChainedStorage storage = new ChainedStorage();

        // check if only write persistent storage gets the value
        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        assertEquals("dummy", name);
                        assertEquals("hoppa", value);
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return true;
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        storage.setProperty("dummy", "hoppa");

        // check if first storage gets the value, if there're no persistent ones
        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        assertEquals("dummy", name);
                        assertEquals("hoppa", value);
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        storage.setProperty("dummy", "hoppa");
    }

    @Test
    public void test_isWritePersistent() {
        ChainedStorage storage = new ChainedStorage();
        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        assertFalse(storage.isWritePersistent());

        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return false;
                    }

                    public void reload() {
                        fail();
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        return true;
                    }

                    public void reload() {
                        fail();
                    }
                }
        ));
        assertTrue(storage.isWritePersistent());
    }

    @Test
    public void test_reload() {
        ChainedStorage storage = new ChainedStorage();
        final int[] counter = new int[1];
        counter[0] = 0;
        storage.setStorages(Arrays.<Storage>asList(
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;
                    }

                    public void reload() {
                        ++counter[0];
                    }
                },
                new Storage() {
                    public void setProperty(String name, String value) {
                        fail();
                    }

                    public String getProperty(String name) {
                        fail();
                        return null;
                    }

                    public boolean isWritePersistent() {
                        fail();
                        return false;
                    }

                    public void reload() {
                        ++counter[0];
                    }
                }
        ));
        storage.reload();
        assertEquals(2, counter[0]);
    }

}
