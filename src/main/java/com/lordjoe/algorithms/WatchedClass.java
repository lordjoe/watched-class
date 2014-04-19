package com.lordjoe.algorithms;


import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * com.lordjoe.algorithms.WatchedClass
 * BY making this a superclass a count will be made of where objects
 * are constructed and how many remain which can be used for tracking memory leaks
 * User: Steve
 * Date: 3/19/14
 */
@SuppressWarnings("UnusedDeclaration")
public class WatchedClass implements Watchable {

    // implement the interface with a method
    public static final ObjectCreationListener REGISTER_LISTENER = WatchedClass::registerObject;

    public static void registerObject(final Object created) {
        if (created instanceof Watchable) {
            Watchable watchable = (Watchable) created;
            //noinspection UnnecessaryLocalVariable,UnusedDeclaration,UnusedAssignment
            Class realClass = created.getClass(); // look at the creating class
            WatchedClass.WatchedClassHolder holder = WatchedClass.getHolder(watchable);  // get a structure remembering where these were created
            String codeLocation = WatchedClass.buildCreateLocation();  // grab stack traces
            holder.register(watchable, codeLocation);          // remember objects of this type were created here

        }

    }


     static {
        Watchable.addCreateListener(REGISTER_LISTENER);  // register
    }

    /**
     * if true print reports to stdout
     */
    private static boolean gShowReports = true;

    public static boolean isShowReports() {
        return gShowReports;
    }

    public static void setShowReports(final boolean pShowReports) {
        gShowReports = pShowReports;
    }

    /**
     * we keep a list of created classes and a  WatchedClassHolder recording use
     */
    private static final Map<Class, WatchedClassHolder> gWatchUsages = new ConcurrentHashMap<>();
    /**
     * interned Strings of the stack trace to where the objects were created -
     * may not tell who is holding a reference but at least adds a point to look
     */
    private static final Set<String> gLocations = new HashSet<>();

    /**
     * clear all watch data
     */
    public static void clearWatchData() {
        gWatchUsages.clear();
        gLocations.clear();
    }

    /**
     * return locations in the code where all instances of WatchedClass were created
     *
     * @return list where objects were created
     */
    public static @Nonnull List<CreateLocation> getCreateLocations() {
        List<CreateLocation> holder = new ArrayList<>();
        for (WatchedClassHolder wc : gWatchUsages.values()) {
            holder.addAll(wc.getCreateLocations());
        }
        Collections.sort(holder);
        return holder;
    }

    /**
     * build a string describing all classes and their uses
     *
     * @return report on surviving classes
     */
    public synchronized @Nonnull static String buildReport() {
        // gWatchUsages.keySet().stream().
        //         sorted().mapToInt(u -> 1).
        // gWatchUsages.keySet().stream().sorted().
        String ret = gWatchUsages.keySet().stream().
                map(aClass -> aClass.getSimpleName() + " " + gWatchUsages.get(aClass).getUseCount()).
                sorted().
                collect(joining("\n"));
        // java 7
        StringBuilder sb = new StringBuilder();
        for (Class aClass : gWatchUsages.keySet()) {
            WatchedClassHolder wc = gWatchUsages.get(aClass);
            sb.append(aClass.getSimpleName());
            //noinspection StringConcatenationInsideStringBufferAppend
            sb.append(" " + wc.getUseCount());
            sb.append("\n");
        }
        String other = sb.toString();
        return ret;
    }

    /**
     * how many instances of this class currently exist
     *
     * @param aClass class in question
     * @return number instances not nulled
     */
    public static int getSurvivingInstanceCount(Class<? extends Watchable> aClass) {
        WatchedClassHolder ret = gWatchUsages.get(aClass);
        if (ret != null)
            return ret.getUseCount();
        return 0;

    }

    /**
     * return the holder associated with a class building it as needed
     *
     * @param item class to look
     * @return associated holder
     */
    protected synchronized static @Nonnull WatchedClassHolder getHolder(@Nonnull Watchable item) {
        Class<? extends Watchable> aClass = item.getClass();
        WatchedClassHolder ret = gWatchUsages.get(aClass);
        if (ret == null) {
            ret = new WatchedClassHolder(aClass);
            gWatchUsages.put(aClass, ret);
        }
        return ret;
    }

    public static void registerObject(@Nonnull Watchable target) {
        Class realClass = target.getClass(); // look at the creating class
        WatchedClassHolder holder = getHolder(target);  // get a structure remembering where these were created
        String codeLocation = buildCreateLocation();  // grab stack traces
        holder.register(target, codeLocation);          // remember objects of this type were created here

    }

    /**
     * use the stack trace to report where in the code an object was created
     * we know we are in  buildCreateLocation and  WatchedClass<imit>
     *     we know we are in  buildCreateLocation and  WatchedClass<imit>
     */
      private static final int NUMBER_CONSTANT_FRAMES = 3;
     public static @Nonnull String buildCreateLocation() {
        Throwable t = new RuntimeException();   // this gets a stack trace
        StackTraceElement[] stackTrace = t.getStackTrace(); // join frames  starting with 3 into ins string
        return Arrays.stream(stackTrace, NUMBER_CONSTANT_FRAMES, stackTrace.length).
                map(c -> c.toString()).
                collect(joining("\n"));
    }


    /**
     * base class which records details of creation and remembers instances still 'alive'
     */
    protected WatchedClass() {
        registerWatch();
    }


    /**
     * remember all instances of a class created using a WeakHashMap
     * so as instances are garbage collected they are forgotten
     */
    public static class WatchedClassHolder {
        private final Class<? extends Watchable> m_Target;
        private final WeakHashMap<Watchable, String> m_Instances = new WeakHashMap<>();
        private long m_LastClean = System.currentTimeMillis();

        public WatchedClassHolder(final Class<? extends Watchable> pTarget) {
            m_Target = pTarget;
        }

        public int getUseCount() {
//            Set<Watchable> watchedClasses = m_Instances.keySet();
//            int non_null_size = watchedClasses.stream().
//                    filter(u  -> u != null).
//                    mapToInt(u -> 1).
//                    sum();
//            int size = watchedClasses.size();
            return m_Instances.size();
        }

        public Class<? extends Watchable> getTarget() {
            return m_Target;
        }

        /**
         * return a new list of locations in the code (stack traces) where
         * instances of the target class were created
         *
         * @return as above
         */
        public List<CreateLocation> getCreateLocations() {
            synchronized (m_Instances) {
                return m_Instances.values().stream().
                        map(loc -> new CreateLocation(getTarget(), loc)).
                        distinct().
                        sorted().
                        collect(toList());
            }

            //                Set<CreateLocation> locs = new HashSet< >();
//                         for (String loc : m_Instances.values()) {
//                    locs.add(new CreateLocation(getTarget(), loc));
//                }
//            }
//           List<CreateLocation> holder = new ArrayList< >(locs);
//            Collections.sort(holder);//            return holder;

        }

        /**
         * remember the creation of an instance
         *
         * @param x        object to registerWatch
         * @param location create location
         */
        public void register(@Nonnull Watchable x, @Nonnull String location) {
            synchronized (m_Instances) {
                if (m_Instances.containsKey(x))
                    return; // already done
                m_Instances.put(x, location);
            }
            synchronized (gLocations) {
                if (!gLocations.contains(location) && isShowReports()) {
                    System.out.println("==== New Build Location ====");
                    System.out.println(location);
                    gLocations.add(location);
                }
            }
        }

    }

}
