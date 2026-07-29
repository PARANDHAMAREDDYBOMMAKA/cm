package com.claimguard.fraud;

import java.util.List;
import java.util.Locale;

public final class EditingSoftware {

    private static final List<String> EDITORS = List.of(
            "photoshop",
            "gimp",
            "canva",
            "illustrator",
            "inkscape",
            "pixelmator",
            "affinity",
            "paint.net",
            "lightroom",
            "snapseed",
            "picsart",
            "figma",
            "coreldraw",
            "acrobat",
            "nitro pro",
            "foxit",
            "ilovepdf",
            "smallpdf");

    private EditingSoftware() {
    }

    public static boolean isEditor(String software) {
        if (software == null) {
            return false;
        }
        String value = software.toLowerCase(Locale.ENGLISH);
        return EDITORS.stream().anyMatch(value::contains);
    }
}
