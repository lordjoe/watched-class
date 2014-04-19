package com.lordjoe.algorithms;

import org.junit.*;

import java.util.*;

/**
 * com.lordjoe.algorithms.WatchedClassTests
 * User: Steve
 * Date: 4/18/2014
 */
public class WatchedClassTests {

    public static final int MAX_STACK_DEPTH = 16;
    public static final int MINIMUM_TRACKED_OBJECTS = 10000;

    public static final Random RND = new Random();

    @Before
    public void clearData() {
        WatchedClass.setShowReports(false);
        WatchedClass.clearWatchData();
        WatchedClassV7.setShowReports(false);
        WatchedClassV7.clearWatchData();
         waitForGarbageCollection(0);
        numberTestClassCreated = 0;
    }

    private static int numberTestClassCreated = 0;

    protected static class TestCreateClass extends WatchedClass {
        boolean isFinalized = false;
        public TestCreateClass() {
            numberTestClassCreated++;
        }

        /**
         * check that finalize works
         * @throws Throwable
         */
        @Override
        public void finalize() throws Throwable {
            super.finalize();
            if(!isFinalized)  {
                isFinalized = true;
                numberTestClassCreated--;
            }

        }

    }

    protected static class TestCreateClass2 implements Watchable
    {
          public TestCreateClass2() {
            registerWatch();
        }

    }
    protected static class TestCreateClass3 implements Watchable
     {
           public TestCreateClass3() {
             registerWatch();
         }

     }

    protected int createTestObjects(int numberToCreate, final Set<Object> container,int type) {
        int maxDepth = 1;
        for (int i = 0; i < numberToCreate; i++) {
            int stackDepth = RND.nextInt(MAX_STACK_DEPTH);
            maxDepth = Math.max(stackDepth + 1, maxDepth);
            createTestObject(container, stackDepth,type);
        }
        return maxDepth;
    }

    private void createTestObject(final Set<Object> container, final int pStackDepth,int type) {
        if (pStackDepth <= 0) {
            switch (type)   {
                case 0 :
                    container.add(new TestCreateClass()); //
                    break;
                case 1 :
                     container.add(new TestCreateClass2()); //
                     break;
                case 2 :
                     container.add(new TestCreateClass3()); //
                     break;
             }
         }
        else {
            createTestObject(container, pStackDepth - 1,type);  // make a new stack
        }
    }



    private void validateCreateWithHolder(final int pNumberToCreate, final Set<Object> container,int type) {
        int numberCreateStacks = createTestObjects(pNumberToCreate, container,type);
        List<CreateLocation> createLocations = WatchedClass.getCreateLocations();
        Assert.assertEquals(numberCreateStacks, createLocations.size());
        List<CreateLocation> createLocationsV7 = WatchedClassV7.getCreateLocations();
        Assert.assertEquals(numberCreateStacks, createLocationsV7.size());
        Assert.assertEquals(container.size(), pNumberToCreate);

        int numberCreateStacks1 = createTestObjects(pNumberToCreate, container,1);
        int numberCreateStacks2 = createTestObjects(pNumberToCreate, container, 2);
        String report =  WatchedClass.buildReport();

        int survevingInstances = WatchedClass.getSurvivingInstanceCount(TestCreateClass.class);
        Assert.assertEquals(survevingInstances, pNumberToCreate);
        int survevingInstancesV7 = WatchedClassV7.getSurvivingInstanceCount(TestCreateClass.class);
        Assert.assertEquals(survevingInstancesV7, pNumberToCreate);
    }

    @Test
    public void testWatchCountWithDelete() {
        Set<Object> droppableHolder = new HashSet<Object>();
        int numberToCreate = MINIMUM_TRACKED_OBJECTS;
        validateCreateWithHolder(numberToCreate, droppableHolder,0);


        droppableHolder.clear(); // drop references
        droppableHolder = null;
        waitForGarbageCollection(0);
        // hopefully all are gone
        int survevingInstances = WatchedClass.getSurvivingInstanceCount(TestCreateClass.class);
        Assert.assertEquals(numberTestClassCreated, survevingInstances);


    }

    /**
     * http://stackoverflow.com/questions/1481178/forcing-garbage-collection-in-java
     */
    public static void waitForGarbageCollection(int remaining) {
        TestCreateClass obj = new TestCreateClass();
        java.lang.ref.WeakReference ref = new java.lang.ref.WeakReference<Object>(obj);
        obj = null;
        int survevingInstances = WatchedClass.getSurvivingInstanceCount(TestCreateClass.class);
        while (ref.get() != null && survevingInstances > remaining) {
            System.gc();
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);

            }
            System.gc();
        }
    }

}
