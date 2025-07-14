package com.advancedraidtracker.utility.wrappers;

public class DefenceReduction {

    public enum TYPE {
      BGS,
      MAUL
    };

    public String player;
    public int tick;
    public TYPE type;
    public int damage;

    public DefenceReduction(String p, int t, TYPE ty, int d)
    {
        this.player = p;
        this.tick = t;
        this.type = ty;
        this.damage = d;
    }
}
