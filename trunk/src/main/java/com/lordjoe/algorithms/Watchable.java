package com.lordjoe.algorithms;

import java.util.*;

/**
 * com.lordjoe.algorithms.Watchable
 * implement this and add registerWatch to all constructors
 * User: Steve
 * Date: 4/18/2014
 */
public interface Watchable {

    static List<ObjectCreationListener> createListeners = new ArrayList<>();
    public static void addCreateListener(ObjectCreationListener added) {
        createListeners.add(added) ;
    }
    // register an objects creation with listeners
    public static void registerObjectWatch(Object registerer) {
        createListeners.stream().forEach(u -> u.onObjectCreate(registerer));
     }
    /**
     * if the implementation does not extend Watched Class then all all constructors should call this
     */
    public default void registerWatch()
    {
        registerObjectWatch(this);
    }

}
