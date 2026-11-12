package com.mypolicy.collect;

/** Counts for one collection of one source. updated counts notices whose content changed. */
public record CollectResult(int fetched, int created, int updated, int cancelled) {
}
