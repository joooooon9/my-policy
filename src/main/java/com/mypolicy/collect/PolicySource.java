package com.mypolicy.collect;

import java.util.List;

/**
 * An official notice source (one API). Each implementation is a Spring bean; the scheduler collects
 * every one of them. fetch() is called outside any database transaction and may throw on a failed call.
 */
public interface PolicySource {

    /** Stored as policy.source; never change it once notices are stored under it. */
    String name();

    /** Every notice the source currently lists. */
    List<SourcePolicy> fetch() throws Exception;
}
