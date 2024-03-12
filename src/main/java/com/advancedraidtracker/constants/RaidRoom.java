package com.advancedraidtracker.constants;

import java.util.Arrays;
import java.util.Optional;

public enum RaidRoom
{
    MAIDEN(0, "Maiden"), BLOAT(1, "Bloat"), NYLOCAS(2, "Nylocas"), SOTETSEG(3, "Sotetseg"), XARPUS(4, "Xarpus"), VERZIK(5, "Verzik"), UNKNOWN(-1, "Unknown"), ANY_TOB(6, "Any TOB"),
    CRONDIS(7, "Crondis"), ZEBAK(8, "Zebak"), SCABARAS(9, "Scabaras"), KEPHRI(10, "Kephri"), APMEKEN(11, "Apmeken"), BABA(12, "Baba"), HET(13, "Het"), AKKHA(14, "Akkha"), WARDENS(15, "Wardens"), ANY_TOA(16, "Any TOA"),
    ANY(17, "Any"), ALL(18, "All"),

    ;

    public final int value;
    public final String name;

    RaidRoom(int value, String name)
    {
        this.value = value;
        this.name = name;
    }

    public static RaidRoom valueOf(int number)
    {
        Optional<RaidRoom> o = Arrays.stream(values()).filter(room -> room.value == number).findFirst();
        return o.orElse(UNKNOWN);
    }


    public static RaidRoom getRoom(String room)
    {
        return RaidRoom.valueOf(room.toUpperCase());
    }
}
