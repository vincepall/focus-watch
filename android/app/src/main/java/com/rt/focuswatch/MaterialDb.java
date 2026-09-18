package com.rt.focuswatch;

public class MaterialDb {
    public static class Material {
        public final String name;
        public final String werkstoff;
        public final String group;
        public final double f150;
        public final double f200;
        public final double f250;
        public final double f300;

        public Material(String name, String werkstoff, String group, double f150, double f200, double f250, double f300) {
            this.name = name;
            this.werkstoff = werkstoff;
            this.group = group;
            this.f150 = f150;
            this.f200 = f200;
            this.f250 = f250;
            this.f300 = f300;
        }

        public double getFactor(int kv) {
            if (kv == 150) return f150;
            if (kv == 200) return f200;
            if (kv == 250) return f250;
            return f300;
        }
    }

    public static final Material[] ALL = new Material[] {
        new Material("Hastelloy C-276", "2.4819 / UNS N10276", "Hastelloy", 8.26, 3.79, 2.65, 2.17),
        new Material("Hastelloy C-22", "2.4602 / UNS N06022", "Hastelloy", 5.39, 2.91, 2.19, 1.87),
        new Material("Hastelloy C-4", "2.4610 / UNS N06455", "Hastelloy", 3.22, 2.12, 1.75, 1.57),
        new Material("Hastelloy C-2000", "2.4675 / UNS N06200", "Hastelloy", 3.05, 2.05, 1.71, 1.54),
        new Material("Hastelloy B-2", "2.4617 / UNS N10665", "Hastelloy", 6.86, 3.4, 2.47, 2.07),
        new Material("Hastelloy B-3", "2.4600 / UNS N10675", "Hastelloy", 9.06, 3.98, 2.75, 2.23),
        new Material("Hastelloy X", "2.4665 / UNS N06002", "Hastelloy", 2.08, 1.59, 1.4, 1.31),
        new Material("Hastelloy G-30", "2.4603 / UNS N06030", "Hastelloy", 2.78, 1.92, 1.61, 1.46),
        new Material("Hastelloy G-35", "2.4643 / UNS N06035", "Hastelloy", 1.82, 1.47, 1.33, 1.26),
        new Material("Hastelloy N", "2.4607 / UNS N10003", "Hastelloy", 3.51, 2.24, 1.82, 1.62),
        new Material("Hastelloy W", "2.4612 / UNS N10004", "Hastelloy", 5.06, 2.8, 2.13, 1.83),
        new Material("Inconel 625 (Alloy 625)", "2.4856 / UNS N06625", "Inconel", 2.5, 1.8, 1.55, 1.42),
        new Material("Inconel 718 (Alloy 718)", "2.4668 / UNS N07718", "Inconel", 1.79, 1.45, 1.32, 1.24),
        new Material("Inconel 600 (Alloy 600)", "2.4816 / UNS N06600", "Inconel", 1.47, 1.29, 1.22, 1.18),
        new Material("Inconel 601 (Alloy 601)", "2.4851 / UNS N06601", "Inconel", 1.24, 1.16, 1.12, 1.1),
        new Material("Inconel 617 (Alloy 617)", "2.4663 / UNS N06617", "Inconel", 2.04, 1.59, 1.41, 1.32),
        new Material("Inconel 686 (Alloy 686)", "2.4606 / UNS N06686", "Inconel", 7.12, 3.43, 2.45, 2.03),
        new Material("Inconel 690 (Alloy 690)", "2.4642 / UNS N06690", "Inconel", 1.26, 1.17, 1.12, 1.1),
        new Material("Inconel X-750", "2.4669 / UNS N07750", "Inconel", 1.53, 1.33, 1.25, 1.2),
        new Material("Inconel 725 (Alloy 725)", "UNS N07725", "Inconel", 2.17, 1.64, 1.44, 1.34),
        new Material("Incoloy 800 / 800H (Alloy 800)", "1.4876 / UNS N08800/N08810", "Incoloy", 1.11, 1.07, 1.06, 1.05),
        new Material("Incoloy 825 (Alloy 825)", "2.4858 / UNS N08825", "Incoloy", 1.39, 1.24, 1.18, 1.14),
        new Material("Incoloy 925 (Alloy 925)", "UNS N09925", "Incoloy", 1.39, 1.24, 1.18, 1.14),
        new Material("Incoloy A-286", "1.4980 / UNS S66286", "Incoloy", 1.13, 1.08, 1.06, 1.05),
        new Material("Incoloy 803", "UNS S35045", "Incoloy", 1.1, 1.07, 1.06, 1.05),
        new Material("Monel 400 (Alloy 400)", "2.4360 / UNS N04400", "Monel", 2.18, 1.69, 1.5, 1.4),
        new Material("Monel K-500 (Alloy K-500)", "2.4375 / UNS N05500", "Monel", 1.8, 1.48, 1.35, 1.28),
        new Material("Stellite 6 (CoCr-A)", "2.4994 / CoCr-A", "Kobaltbasis", 3.4, 2.18, 1.77, 1.57),
        new Material("Stellite 21 (CoCr-E)", "CoCr-E", "Kobaltbasis", 1.46, 1.28, 1.2, 1.16),
        new Material("Stellite 12 (CoCr-B)", "CoCr-B", "Kobaltbasis", 3.8, 2.4, 1.9, 1.6),
        new Material("Haynes 25 (Alloy L-605)", "2.4964 / UNS R30605", "Kobaltbasis", 3.2, 2.4, 2.0, 1.7),
        new Material("Haynes 188", "2.4683 / UNS R30188", "Kobaltbasis", 3.0, 2.3, 1.9, 1.6),
        new Material("MP35N", "2.4999 / UNS R30035", "Kobaltbasis", 2.13, 1.63, 1.44, 1.34),
        new Material("Titanium Grade 1 & 2 (CP Ti)", "3.7035 / UNS R50400", "Titanium", 0.54, 0.71, 0.78, 0.9),
        new Material("Titanium Grade 5 (Ti-6Al-4V)", "3.7164 / UNS R56400", "Titanium", 0.54, 0.71, 0.78, 0.9),
        new Material("Titanium Grade 7 (Ti-Pd)", "3.7235 / UNS R52400", "Titanium", 0.54, 0.71, 0.78, 0.9),
        new Material("Titanium Grade 9 (Ti-3Al-2.5V)", "3.7194 / UNS R56320", "Titanium", 0.54, 0.71, 0.78, 0.9),
        new Material("Titanium Grade 12 (Ti-Mo-Ni)", "3.7105 / UNS R53400", "Titanium", 0.54, 0.71, 0.78, 0.9),
        new Material("Koolstofstaal (Referentie)", "1.0460 / P250GH / C22", "RVS & Duplex", 1.0, 1.0, 1.0, 1.0),
        new Material("RVS 304 / 304L", "1.4301 / 1.4307 / 304L", "RVS & Duplex", 0.99, 1.0, 1.0, 1.0),
        new Material("RVS 316 / 316L", "1.4401 / 1.4404 / 316L", "RVS & Duplex", 1.14, 1.09, 1.07, 1.06),
        new Material("RVS 316Ti", "1.4571 / AISI 316Ti", "RVS & Duplex", 1.11, 1.07, 1.05, 1.04),
        new Material("RVS 321", "1.4541 / AISI 321", "RVS & Duplex", 0.96, 0.98, 0.98, 0.98),
        new Material("RVS 347", "1.4550 / AISI 347", "RVS & Duplex", 1.03, 1.02, 1.01, 1.01),
        new Material("RVS 310S", "1.4845 / AISI 310S", "RVS & Duplex", 0.98, 0.99, 0.99, 0.99),
        new Material("RVS 309S", "1.4828 / AISI 309S", "RVS & Duplex", 0.94, 0.96, 0.97, 0.98),
        new Material("Duplex 2205", "1.4462 / UNS S32205", "RVS & Duplex", 1.08, 1.04, 1.03, 1.02),
        new Material("Lean Duplex 2101", "1.4162 / UNS S32101", "RVS & Duplex", 0.88, 0.92, 0.93, 0.94),
        new Material("Lean Duplex 2304", "1.4362 / UNS S32304", "RVS & Duplex", 0.92, 0.94, 0.96, 0.96),
        new Material("Super Duplex 2507", "1.4410 / UNS S32750", "RVS & Duplex", 1.14, 1.08, 1.06, 1.05),
        new Material("Super Duplex Zeron 100", "1.4501 / UNS S32760", "RVS & Duplex", 1.32, 1.19, 1.13, 1.1),
        new Material("254 SMO (6Mo)", "1.4547 / UNS S31254", "RVS & Duplex", 1.38, 1.23, 1.16, 1.13),
        new Material("904L", "1.4539 / UNS N08904", "RVS & Duplex", 1.32, 1.19, 1.14, 1.11),
        new Material("Alloy 20 (Carpenter 20)", "2.4660 / UNS N08020", "RVS & Duplex", 1.38, 1.23, 1.17, 1.13),
        new Material("Sanicro 28 (Alloy 28)", "1.4563 / UNS N08028", "RVS & Duplex", 1.18, 1.1, 1.07, 1.05),
        new Material("Alloy 31", "1.4562 / UNS N08031", "RVS & Duplex", 1.41, 1.24, 1.17, 1.13),
        new Material("253 MA", "1.4835 / UNS S30815", "RVS & Duplex", 0.94, 0.96, 0.97, 0.97),
        new Material("17-4 PH (AISI 630)", "1.4542 / UNS S17400", "RVS & Duplex", 0.93, 0.94, 0.95, 0.96),
        new Material("16Mo3 (15Mo3)", "1.5415 / 16Mo3", "Ketelstaal", 1.0, 1.0, 1.0, 1.0),
        new Material("13CrMo4-5 (P11 / T11)", "1.7335 / A335 P11", "Ketelstaal", 1.01, 1.0, 1.0, 1.0),
        new Material("10CrMo9-10 (P22 / T22)", "1.7380 / A335 P22", "Ketelstaal", 1.03, 1.02, 1.01, 1.01),
        new Material("11CrMo9-10", "1.7383", "Ketelstaal", 1.01, 1.0, 1.0, 1.0),
        new Material("14MoV6-3", "1.7715", "Ketelstaal", 1.0, 1.0, 1.0, 1.0),
        new Material("X10CrMoVNb9-1 (P91 / T91)", "1.4903 / A335 P91", "Ketelstaal", 0.97, 0.97, 0.98, 0.98),
        new Material("X10CrWMoVNb9-2 (P92 / T92)", "1.4901 / A335 P92", "Ketelstaal", 1.47, 1.27, 1.19, 1.14),
        new Material("VM12-SHC (12Cr)", "1.4915", "Ketelstaal", 1.36, 1.21, 1.14, 1.11),
        new Material("Koper Zuiver (Cu-ETP / Cu-DHP)", "2.0060 / 2.0090 / CW004A", "Koper & Brons", 2.42, 1.79, 1.55, 1.43),
        new Material("Messing / Geelkoper (CuZn37)", "2.0321 / CW508L", "Koper & Brons", 2.21, 1.66, 1.45, 1.34),
        new Material("Messing (CuZn39Pb3)", "2.0401 / CW614N", "Koper & Brons", 5.04, 2.8, 2.12, 1.8),
        new Material("Tinbrons (CuSn8)", "2.1030 / CW453K", "Koper & Brons", 3.99, 2.4, 1.9, 1.66),
        new Material("Tinbrons (CuSn6)", "2.1020 / CW452K", "Koper & Brons", 3.52, 2.23, 1.8, 1.6),
        new Material("Roodbrons (Rg7 / CuSn7ZnPb)", "2.1090 / CC493K", "Koper & Brons", 8.45, 3.82, 2.65, 2.15),
        new Material("Aluminiumbrons (CuAl10Fe5Ni5)", "2.0966 / CW307G", "Koper & Brons", 1.25, 1.13, 1.08, 1.05),
        new Material("Cupro-nikkel 90/10", "2.0872 / CW352H", "Koper & Brons", 2.36, 1.76, 1.54, 1.42),
        new Material("Cupro-nikkel 70/30", "2.0882 / CW354H", "Koper & Brons", 2.4, 1.79, 1.56, 1.45),
        new Material("Nikkel 200 / 201 (Zuiver Nikkel)", "2.4066 / UNS N02200", "Exotisch & Speciaal", 2.19, 1.71, 1.52, 1.42),
        new Material("Zirkonium 702 (Zr 702)", "UNS R60702", "Exotisch & Speciaal", 2.8, 2.1, 1.8, 1.5),
        new Material("Zirkonium 705 (Zr 705)", "UNS R60705", "Exotisch & Speciaal", 2.8, 2.1, 1.8, 1.5),
        new Material("Tantaal (Zuiver Ta)", "2.4560 / UNS R05200", "Exotisch & Speciaal", 11.0, 5.5, 4.2, 3.5),
        new Material("Niobium (Columbium)", "UNS R04200", "Exotisch & Speciaal", 2.5, 2.0, 1.7, 1.5),
        new Material("Molybdeen (Puur Mo)", "UNS R03600", "Exotisch & Speciaal", 3.5, 2.5, 2.0, 1.7),
        new Material("Wolfraam (Tungsten)", "W 99.95%", "Exotisch & Speciaal", 14.0, 7.0, 5.0, 4.0),
        new Material("Lood (Pb)", "2.3020 / PB970R", "Exotisch & Speciaal", 12.0, 5.0, 4.0, 3.2),
        new Material("Tin (Sn)", "Sn 99.9%", "Exotisch & Speciaal", 3.0, 2.2, 1.8, 1.5),
        new Material("Zink (Zn)", "Zn 99.9%", "Exotisch & Speciaal", 1.4, 1.3, 1.2, 1.1),
        new Material("Aluminium Zuiver (1050A)", "3.0255 / EN AW-1050A", "Exotisch & Speciaal", 0.12, 0.18, 0.25, 0.3),
        new Material("Aluminium AlMg3", "3.3535 / EN AW-5754", "Exotisch & Speciaal", 0.14, 0.18, 0.25, 0.3),
        new Material("Aluminium AlMg4.5Mn", "3.3547 / EN AW-5083", "Exotisch & Speciaal", 0.14, 0.18, 0.25, 0.3),
        new Material("Magnesium Zuiver", "AZ31 / Zuiver Mg", "Exotisch & Speciaal", 0.05, 0.08, 0.1, 0.12),
    };
}
