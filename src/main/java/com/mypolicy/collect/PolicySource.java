package com.mypolicy.collect;

import java.util.List;
import java.util.Set;

import com.mypolicy.policy.Region;

/**
 * An official notice source (one API). Each implementation is a Spring bean; the scheduler collects
 * every one of them. fetch() is called outside any database transaction and may throw on a failed call.
 */
public interface PolicySource {

    /** Stored as policy.source; never change it once notices are stored under it. */
    String name();

    /**
     * The service regions this source collects now. A region collected for the first time fills the list
     * without new-notice events; the search form offers only regions some source collects.
     */
    Set<Region> regions();

    /** Every notice the source currently lists, in the regions above. */
    List<SourcePolicy> fetch() throws Exception;
}
