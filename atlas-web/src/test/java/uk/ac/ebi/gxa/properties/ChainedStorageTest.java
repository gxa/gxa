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
import java.util.Set;
import java.util.Collections;
import java.util.Collection;

/**
 * @author pashky
 */
public class ChainedStorageTest {

    public static class TestStorage implements Storage {
        public void setProperty(String name, String value) {
            fail("Not implemented");
        }

        public String getProperty(String name) {
            fail("Not implemented");
            return null;
        }

        public boolean isWritePersistent() {
            fail("Not implemented");
            return false;
        }

        public Collection<String> getAvailablePropertyNames() {
            fail("Not implemented");
            return null;
        }

        public void reload() {
            fail("Not implemented");
        }
    }

    @Test
    public void test_getProperty() {
        ChainedStorage storage = new ChainedStorage();
        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
                    }
                },
                new TestStorage() {
                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return "abc";
                    }
                }
        ));
        assertEquals("abc", storage.getProperty("dummy"));

        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
                    }
                },
                new TestStorage() {
                    public String getProperty(String name) {
                        assertEquals("dummy", name);
                        return null;
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
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return false;
                    }
                },
                new TestStorage() {
                    public void setProperty(String name, String value) {
                        assertEquals("dummy", name);
                        assertEquals("hoppa", value);
                    }
                    public boolean isWritePersistent() {
                        return true;
                    }
                }
        ));
        storage.setProperty("dummy", "hoppa");

        // check if first storage gets the value, if there're no persistent ones
        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public void setProperty(String name, String value) {
                        assertEquals("dummy", name);
                        assertEquals("hoppa", value);
                    }
                    public boolean isWritePersistent() {
                        return false;
                    }
                },
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return false;
                    }
                }
        ));
        storage.setProperty("dummy", "hoppa");
    }

    @Test
    public void test_isWritePersistent() {
        ChainedStorage storage = new ChainedStorage();
        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return false;
                    }
                },
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return false;
                    }
                }
        ));
        assertFalse(storage.isWritePersistent());

        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return false;
                    }
                },
                new TestStorage() {
                    public boolean isWritePersistent() {
                        return true;
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
                new TestStorage() {
                    public void reload() {
                        ++counter[0];
                    }
                },
                new TestStorage() {
                    public void reload() {
                        ++counter[0];
                    }
                }
        ));
        storage.reload();
        assertEquals(2, counter[0]);
    }

    @Test
    public void test_getAvailablePropertyNames() {
        ChainedStorage storage = new ChainedStorage();
        storage.setStorages(Arrays.<Storage>asList(
                new TestStorage() {
                    public Collection<String> getAvailablePropertyNames() {
                        return Collections.singleton("1");
                    }
                },
                new TestStorage() {
                    public Collection<String> getAvailablePropertyNames() {
                        return Collections.singleton("2");
                    }
                }
        ));
        assertEquals(2, storage.getAvailablePropertyNames().size());
        assertTrue(storage.getAvailablePropertyNames().containsAll(Arrays.asList("1","2")));
    }
}
