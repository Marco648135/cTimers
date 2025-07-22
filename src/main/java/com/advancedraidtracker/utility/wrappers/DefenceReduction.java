package com.advancedraidtracker.utility.wrappers;

public class DefenceReduction {

    public enum TYPE {
      BGS,
      MAUL,
      SHADOW_BARRAGE,
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

    @Override
    public String toString() {
        String typeStr = this.type == TYPE.MAUL ? "MAUL" :
                              type == TYPE.BGS ? "BGS" :
                              "SHADOWBARRAGE";
        return "DefenceReduction[" + typeStr + ",t" + tick + "," + player + "," + damage + "]";
    }
}
