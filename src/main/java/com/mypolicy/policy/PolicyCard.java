package com.mypolicy.policy;

import java.util.List;

/** One notice in the list, ready to show. institution is who runs it (may be empty). */
public record PolicyCard(long id, Region region, String institution, String title, String summary, String benefit,
        String deadline, List<String> checkNotes) {
}
