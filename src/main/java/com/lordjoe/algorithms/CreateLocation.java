package com.lordjoe.algorithms;

import javax.annotation.*;

/**
 * location where an instance of a class was created
 */
public class CreateLocation implements Comparable<CreateLocation> {
    private final Class m_TargetClass;
    private final String m_StackTrace;

    public CreateLocation(final Class pTargetClass, final String pStackTrace) {
        m_TargetClass = pTargetClass;
        m_StackTrace = pStackTrace;
    }

    public @Nonnull Class getTargetClass() {
        return m_TargetClass;
    }

    public @Nonnull String getStackTrace() {
        return m_StackTrace;
    }

    @Override
    public boolean equals(final @Nonnull Object o) {
        if (this == o) return true;
        if (getClass() != o.getClass()) return false;

        final CreateLocation that = (CreateLocation) o;

        if (!m_TargetClass.equals(that.m_TargetClass)) return false;
        //noinspection RedundantIfStatement
        if (!m_StackTrace.equals(that.m_StackTrace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = m_TargetClass.hashCode();
        result = 31 * result + m_StackTrace.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return m_TargetClass.toString() + "\n"
                + m_StackTrace;
    }


    @Override
    public int compareTo(final @Nonnull CreateLocation o) {
        Class me = getTargetClass();
        Class otherClass = o.getTargetClass();
        if (me != otherClass)
            return me.toString().compareTo(otherClass.toString());
        return getStackTrace().compareTo(o.getStackTrace());
    }
}
