package com.mypolicy.policy;

import java.util.List;

/** One notice in the list, ready to show. */
public record PolicyCard(long id, Region region, String title, String summary, String benefit,
        String deadline, List<String> checkNotes) {
}
