package com.rt.focuswatch;

import java.util.Calendar;

public class SourceDb {
    public static class Source {
        public final String name;
        public final String type;
        public final double initialCi;
        public final int day, month, year;

        public Source(String name, String type, double initialCi, int day, int month, int year) {
            this.name = name;
            this.type = type;
            this.initialCi = initialCi;
            this.day = day;
            this.month = month;
            this.year = year;
        }

        public double getCurrentCi() {
            Calendar src = Calendar.getInstance();
            src.set(year, month - 1, day, 0, 0, 0);
            long diffMs = System.currentTimeMillis() - src.getTimeInMillis();
            double days = diffMs / (1000.0 * 60.0 * 60.0 * 24.0);
            double halfLife = "Se75".equalsIgnoreCase(type) ? 120.0 : 74.0;
            return initialCi / Math.pow(2.0, days / halfLife);
        }
    }

    public static final Source[] ALL = new Source[] {
        new Source("35042P", "Ir192", 56.5, 31, 7, 2026),
        new Source("RIL294", "Se75", 80.68, 31, 3, 2026),
        new Source("97969M", "Se75", 84.5, 21, 4, 2026),
        new Source("RR3508", "Se75", 94.73, 24, 2, 2026),
        new Source("RR3820", "Se75", 84.28, 29, 5, 2026),
    };
}
