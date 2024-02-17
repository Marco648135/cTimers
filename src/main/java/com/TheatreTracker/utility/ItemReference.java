package com.TheatreTracker.utility;

public class ItemReference
{

    public static String[][] ITEMS = {
            {"none", "elite void", "void knight", "blood fury"},
            {
                    "torva",
                    "defender",
                    "scythe",
                    "osmumten",
                    "blade",
                    "halberd",
                    "melee",
                    "dagger",
                    "inquisitor",
                    "claws",
                    "rapier",
                    "bulwark",
                    "hammer",
                    "primordial",
                    "ferocious",
                    "infernal",
                    "fire cape",
                    "bandos",
                    "godsword",
                    "dragon boots",
                    "whip",
                    "tentacle",
                    "salve",
                    "faceguard",
                    "torso",
                    "joint",
                    "torture",
                    "serpentine helm",
            },
            {
                    "masori",
                    "bow",
                    "range",
                    "blowpipe",
                    "anguish",
                    "assembler",
                    "pegasian",
                    "buckler",
                    "chinchompa",
                    "vambraces"
            },
            {
                    "ancestral",
                    "virtus",
                    "occult",
                    "tormented",
                    "tumeken",
                    "staff",
                    "mage",
                    "eternal",
                    "imbued",
                    "trident",
                    "kodai",
                    "ice",
                    "ward",
            }
    };

    public static String getNameFromID(int id)
    {
        for(Items item : Items.values())
        {
            if(item.matches(id))
            {
                return item.getName();
            }
        }
        return "Unknown";
    }
}
