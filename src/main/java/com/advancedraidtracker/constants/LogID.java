package com.advancedraidtracker.constants;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.advancedraidtracker.constants.ParseType.*;
import static com.advancedraidtracker.constants.RaidRoom.*;
import static com.advancedraidtracker.utility.datautility.DataPoint.*;

/**
 * Convenience class for the possible keys used to log events. The parameters each of these events should include can be found in
 * RoomData.
 */
@Getter
public enum LogID
{
    ENTERED_RAID(0, true, ANY,
            new ParseObject(AGNOSTIC),
            "Entered Raid"),
    PARTY_MEMBERS(1, true, ANY,
            new ParseObject(AGNOSTIC),
            "Party Members", "Player1", "Player2", "Player3", "Player4", "Player5", "Player6", "Player7", "Player8"),
    HAMMER_HIT(2, true, ANY,
            new ParseObject(INCREMENT, PLAYER_HAMMER_HIT_COUNT),
            new ParseObject(DWH, DEFENSE),
            "Hammer Hit", "Player", "Room Tick"),
    BGS_HIT(3, true, ANY_TOB,
            new ParseObject(INCREMENT, PLAYER_BGS_HIT_COUNT),
            new ParseObject(ADD_TO_VALUE, PLAYER_BGS_DAMAGE),
            new ParseObject(BGS, DEFENSE),
            "BGS Hit", "Player", "Damage", "Room Tick"),
    LEFT_TOB(4, true, ANY_TOB,
            new ParseObject(LEFT_RAID),
            "Left TOB", "Last Room Tick", "Last Room Name"),
    PLAYER_DIED(5, true, ANY,
            new ParseObject(INCREMENT, DEATHS),
            "Played Died", "Player", "Room Tick"),
    ENTERED_NEW_TOB_REGION(6, true, ANY_TOB,
            new ParseObject(RAID_SPECIFIC),
            "Entered New TOB Region", "Room (Int)"),
    HAMMER_ATTEMPTED(7, true, ANY,
            new ParseObject(INCREMENT, PLAYER_HAMMER_ATTEMPTED),
            "DWH Attempted", "Player"),
    DAWN_DROPPED(800, false, VERZIK,
            new ParseObject(MAP, DAWN_DROPS),
            "Dawnbringer appeared", "Room Tick"),
    WEBS_STARTED(901, false, VERZIK,
            new ParseObject(MAP, WEBS_THROWN),
            "Webs Thrown", "Room Tick"),
    PLAYER_ATTACK(801, false, ANY,
            new ParseObject(MANUAL_PARSE),
            "Player Animation", "Player:Room Tick", "Animation:Worn Items ~ Separated", "Spot animations", "Weapon:Interated Index:Interacted ID", "Matched Projectile:Interacted Name", "Room Name"),
    BLOOD_THROWN(9, true, MAIDEN,
            new ParseObject(INCREMENT, MAIDEN_BLOOD_THROWN),
            "Maiden blood thrown"),
    BLOOD_SPAWNED(10, true, MAIDEN,
            new ParseObject(INCREMENT, MAIDEN_BLOOD_SPAWNED),
            "Blood Spawned"),
    CRAB_LEAK(11, true, MAIDEN,
            new ParseObject(MANUAL_PARSE),
            "Crab Leaked", "Position", "Health"),
    MAIDEN_SPAWNED(12, true, MAIDEN,
            new ParseObject(ROOM_START_FLAG),
            "Spawned"),
    MAIDEN_70S(13, true, MAIDEN,
            new ParseObject(SET, MAIDEN_70_SPLIT),
            "70s", "Room Tick"),
    MAIDEN_50S(14, true, MAIDEN,
            new ParseObject(SPLIT, MAIDEN_50_SPLIT, MAIDEN_7050_DURATION, MAIDEN_70_SPLIT),
            "50s", "Room Tick"),
    MAIDEN_30S(15, true, MAIDEN,
            new ParseObject(SPLIT, MAIDEN_30_SPLIT, MAIDEN_5030_SPLIT, MAIDEN_50_SPLIT),
            "30s", "Room Tick"),
    MAIDEN_DESPAWNED(17, true, MAIDEN,
            new ParseObject(SPLIT, TIME, MAIDEN_SKIP_SPLIT, MAIDEN_30_SPLIT),
            new ParseObject(ADD_TO_VALUE, CHALLENGE_TIME),
            new ParseObject(ROOM_END_FLAG),
            "Despawned", "Room Tick"),
    MATOMENOS_SPAWNED(18, true, MAIDEN,
            new ParseObject(MANUAL_PARSE),
            "Crab Spawned", "Crab Description"),
    MAIDEN_SCUFFED(19, true, MAIDEN,
            new ParseObject(MANUAL_PARSE),
            "Scuffed", "Current Proc"),
    BLOAT_SPAWNED(20, true, BLOAT,
            new ParseObject(ROOM_START_FLAG),
            "Spawned"),
    BLOAT_DOWN(21, true, BLOAT,
            new ParseObject(INCREMENT, BLOAT_DOWNS),
            "Down", "Room Tick"),
    BLOAT_DESPAWN(23, true, BLOAT,
            new ParseObject(SET, TIME),
            new ParseObject(ADD_TO_VALUE, CHALLENGE_TIME),
            new ParseObject(ROOM_END_FLAG),
            "Despawned", "Room Tick"),
    BLOAT_HP_1ST_DOWN(24, true, BLOAT,
            new ParseObject(SET, BLOAT_HP_FIRST_DOWN),
            "HP at First Down", "Bloat HP"),
    BLOAT_SCYTHE_1ST_WALK(25, true, BLOAT,
            new ParseObject(INCREMENT, BLOAT_FIRST_WALK_SCYTHES),
            "First Walk Scythes", "Player", "Room Tick"),
    NYLO_PILLAR_SPAWN(30, true, NYLOCAS,
            new ParseObject(ROOM_START_FLAG),
            "Pillar Spawn"),
    NYLO_STALL(31, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_STALLS_TOTAL),
            new ParseObject(INCREMENT_IF_GREATER_THAN, NYLO_STALLS_POST_20, "Wave", 19),
            new ParseObject(INCREMENT_IF_LESS_THAN, NYLO_STALLS_PRE_20, "Wave", 20),
            "Stall", "Wave", "Room Tick", "Nylos Alive"),
    RANGE_SPLIT(32, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_SPLITS_RANGE),
            "Range Split", "Wave", "Room Tick"),
    MAGE_SPLIT(33, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_SPLITS_MAGE),
            "Mage Split", "Wave", "Room Tick"),
    MELEE_SPLIT(34, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_SPLITS_MELEE),
            "Melee Split", "Wave", "Room Tick"),
    LAST_WAVE(35, true, NYLOCAS,
            new ParseObject(SET, NYLO_LAST_WAVE),
            "Last Wave", "Room Tick"),
    LAST_DEAD(36, true, NYLOCAS,
            new ParseObject(SPLIT, NYLO_LAST_DEAD, NYLO_CLEANUP, NYLO_LAST_WAVE),
            "Last Dead", "Room Tick"),
    NYLO_WAVE(37, true, NYLOCAS,
            new ParseObject(MANUAL_PARSE),
            "Wave", "Wave Number", "Room Tick"),
    BOSS_SPAWN(40, true, NYLOCAS,
            new ParseObject(SPLIT, NYLO_BOSS_SPAWN, NYLO_CLEANUP, NYLO_LAST_WAVE),
            "Boss Spawn", "Room Tick"),
    MELEE_PHASE(41, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_ROTATIONS_TOTAL),
            new ParseObject(INCREMENT, NYLO_ROTATIONS_MELEE),
            "Melee Phase", "Room Tick"),
    MAGE_PHASE(42, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_ROTATIONS_TOTAL),
            new ParseObject(INCREMENT, NYLO_ROTATIONS_MAGE),
            "Mage Phase", "Room Tick"),
    RANGE_PHASE(43, true, NYLOCAS,
            new ParseObject(INCREMENT, NYLO_ROTATIONS_TOTAL),
            new ParseObject(INCREMENT, NYLO_ROTATIONS_RANGE),
            "Range Phase", "Room Tick"),
    NYLO_DESPAWNED(45, true, NYLOCAS,
            new ParseObject(SPLIT, TIME, NYLO_BOSS_DURATION, NYLO_BOSS_SPAWN),
            new ParseObject(ADD_TO_VALUE, CHALLENGE_TIME),
            new ParseObject(ROOM_END_FLAG),
            "Despawn", "Room Tick"),
    //POSSIBLY_DEPRECATED_NYLO_PILLAR_DESPAWNED(46, true, NYLOCAS, "Pillar Despawn", "Room Tick"),
    SOTETSEG_STARTED(51, true, SOTETSEG,
            new ParseObject(ROOM_START_FLAG),
            "Started"),
    SOTETSEG_FIRST_MAZE_STARTED(52, true, SOTETSEG,
            new ParseObject(SET, SOTE_M1_SPLIT),
            "First Maze Start", "Room Tick"),
    SOTETSEG_FIRST_MAZE_ENDED(53, true, SOTETSEG,
            new ParseObject(SPLIT, SOTE_P2_SPLIT, SOTE_M1_DURATION, SOTE_M1_SPLIT),
            "First Maze End", "Room Tick"),
    SOTETSEG_SECOND_MAZE_STARTED(54, true, SOTETSEG,
            new ParseObject(SPLIT, SOTE_M2_SPLIT, SOTE_P2_DURATION, SOTE_P2_SPLIT),
            "Second Maze Start", "Room Tick"),
    SOTETSEG_SECOND_MAZE_ENDED(55, true, SOTETSEG,
            new ParseObject(SPLIT, SOTE_P3_SPLIT, SOTE_M2_DURATION, SOTE_M2_SPLIT),
            new ParseObject(SUM, SOTE_MAZE_SUM, SOTE_M1_DURATION, SOTE_M2_DURATION),
            "Second Maze End", "Room Tick"),
    SOTETSEG_ENDED(57, true, SOTETSEG,
            new ParseObject(SPLIT, TIME, SOTE_P3_DURATION, SOTE_P3_SPLIT),
            new ParseObject(ADD_TO_VALUE, CHALLENGE_TIME),
            new ParseObject(ROOM_END_FLAG),
            "Room End", "Room Tick"),
    XARPUS_STARTED(61, true, XARPUS,
            new ParseObject(ROOM_START_FLAG),
            "Started"),
    XARPUS_HEAL(62, true, XARPUS,
            new ParseObject(ADD_TO_VALUE, XARP_HEALING),
            "Heal"),
    XARPUS_SCREECH(63, true, XARPUS,
            new ParseObject(SET, XARP_SCREECH),
            "Screech", "Room Tick"),
    XARPUS_DESPAWNED(65, true, XARPUS,
            new ParseObject(SPLIT, TIME, XARP_POST_SCREECH, XARP_SCREECH),
            new ParseObject(ROOM_END_FLAG),
            "Despawned", "Room Tick"),
    //DEPRECATED_VERZIK_SPAWNED(70, true, VERZIK, "Spawned"),
    VERZIK_P1_START(71, true, VERZIK,
            new ParseObject(ROOM_START_FLAG),
            "P1 Start"),
    VERZIK_P1_DESPAWNED(73, true, VERZIK,
            new ParseObject(SET, VERZIK_P2_SPLIT, -13),
            "P1 Despawned", "Room Tick"),
    VERZIK_P2_END(74, true, VERZIK,
            new ParseObject(SPLIT, VERZIK_P3_SPLIT, VERZIK_P2_DURATION, VERZIK_P2_SPLIT),
            "P2 End", "Room Tick"),
    VERZIK_P3_DESPAWNED(76, true, VERZIK,
            new ParseObject(SPLIT, TIME, VERZIK_P3_DURATION, VERZIK_P3_SPLIT),
            new ParseObject(ADD_TO_VALUE, CHALLENGE_TIME),
            new ParseObject(ROOM_END_FLAG),
            "P3 Despawned", "Room Tick"),
    VERZIK_BOUNCE(77, true, VERZIK,
            new ParseObject(INCREMENT, VERZIK_BOUNCES),
            VERZIK_BOUNCES,
            "Bounce", "Player", "Room Tick"),
    VERZIK_CRAB_SPAWNED(78, true, VERZIK,
            new ParseObject(INCREMENT_IF_GREATER_THAN, VERZIK_P3_CRABS_SPAWNED, VERZIK_P2_DURATION, 1),
            new ParseObject(INCREMENT_IF_LESS_THAN, VERZIK_P2_CRABS_SPAWNED, VERZIK_P2_DURATION, 2),
            new ParseObject(INCREMENT, VERZIK_CRABS_SPAWNED),
            "Crab Spawned", "Room Tick"),
    VERZIK_P2_REDS_PROC(80, true, VERZIK,
            new ParseObject(MANUAL_PARSE),
            "Reds Proc", "Room Tick"),
    LATE_START(98, true, ANY_TOB,
            new ParseObject(RAID_SPECIFIC),
            "Joined Raid After Start", "Room Name"),
    SPECTATE(99, true, ANY_TOB,
            new ParseObject(RAID_SPECIFIC),
            "Is Spectating"),
    //DEPRECATED_1(998, true, ANY_TOB, "DEPRECATED"),
    //DEPRECATED_2(999, true, ANY_TOB, "DEPRECATED"),

    //DEPRECATED_BLOAT_HAND(975, false, BLOAT, "Bloat Hand", "Game Object ID", "RegionX", "RegionY", "Room Tick"),
    //DEPRECATED_BLOAT_DIRECTION(976, false, BLOAT, "Bloat Direction on instance creation", "Orientation (Runelite Angle)", "NPC Index"),

    PARTY_COMPLETE(100, true, ANY_TOB,
            RAID_SPECIFIC,
            "Party Is Complete"),
    PARTY_INCOMPLETE(101, true, ANY_TOB,
            RAID_SPECIFIC,
            "Party Is Not Complete"),
    PARTY_ACCURATE_PREMAIDEN(102, true, ANY_TOB,
            RAID_SPECIFIC,
            "Party Is Complete Prior To Maiden"),

    MAIDEN_DINHS_SPEC(111, true, MAIDEN,
            MANUAL_PARSE,
            "Dinhs Spec", "Player", "Primary Target:Primary Target HP", "Targets~HP : Separated", "Targets Below 27hp"), //Player, tick, primary target:primary target hp, targets~hp:,stats:stats
    MAIDEN_DINHS_TARGET(112, true, MAIDEN,
            MANUAL_PARSE,
            "Dinhs Target"), //

    MAIDEN_CHIN_THROWN(113, true, MAIDEN,
            MANUAL_PARSE,
            "Chin Thrown", "Player", "Distance"), //player, distance

    ACCURATE_MAIDEN_START(201, true, MAIDEN,
            ACCURATE_START,
            "Accurate Maiden Start"),
    ACCURATE_BLOAT_START(202, true, BLOAT,
            ACCURATE_START,
            "Accurate Bloat Start"),
    ACCURATE_NYLO_START(203, true, NYLOCAS,
            ACCURATE_START,
            "Accurate Nylo Start"),
    ACCURATE_SOTE_START(204, true, SOTETSEG,
            ACCURATE_START,
            "Accurate Sote Start"),
    ACCURATE_XARP_START(205, true, XARPUS,
            ACCURATE_START,
            "Accurate Xarpus Start"),
    ACCURATE_VERZIK_START(206, true, VERZIK,
            ACCURATE_START,
            "Accurate Verzik Start"),

    ACCURATE_MAIDEN_END(301, true, MAIDEN,
            ACCURATE_END,
            "Accurate Maiden End"),
    ACCURATE_BLOAT_END(302, true, BLOAT,
            ACCURATE_END,
            "Accurate Bloat End"),
    ACCURATE_NYLO_END(303, true, NYLOCAS,
            ACCURATE_END,
            "Accurate Nylo End"),
    ACCURATE_SOTE_END(304, true, SOTETSEG,
            ACCURATE_END,
            "Accurate Sote End"),
    ACCURATE_XARP_END(305, true, XARPUS,
            ACCURATE_END,
            "Accurate Xarpus End"),
    ACCURATE_VERZIK_END(306, true, VERZIK,
            ACCURATE_END,
            "Accurate Verzik End"),
    IS_HARD_MODE(401, true, ANY_TOB,
            MANUAL_PARSE,
            "Is Hard Mode"),
    IS_STORY_MODE(402, true, ANY_TOB,
            MANUAL_PARSE,
            "Is Story Mode"),
    THRALL_ATTACKED(403, false, ANY_TOB,
            MANUAL_PARSE,
            "Thrall Attacked", "Player", "Type"), // player, type
    THRALL_DAMAGED(404, false, ANY_TOB,
            MANUAL_PARSE,
            "Thrall Damaged", "Player", "Damage"), // player, damage
    VENG_WAS_CAST(405, false, ANY_TOB,
            MANUAL_PARSE,
            "Veng Cast", "Target", "Source"), //target, source
    VENG_WAS_PROCCED(406, false, ANY_TOB,
            MANUAL_PARSE,
            "Veng Procced", "Player", "Damage"), //player, source of veng, damage
    PLAYER_STOOD_IN_THROWN_BLOOD(411, true, MAIDEN,
            MANUAL_PARSE,
            "Player Stood In Thrown Blood", "Player", "Damage", "Ticks blood was alive for"), //player, damage, blood tick
    PLAYER_STOOD_IN_SPAWNED_BLOOD(412, true, MAIDEN,
            MANUAL_PARSE,
            "Player Stood In Spawned Blood", "Player", "Damage"),  //player, damage
    CRAB_HEALED_MAIDEN(413, true, MAIDEN,
            MANUAL_PARSE,
            "Crab Healed Maiden", " Heal Amount"), //damage
    /*VERZIK_PURPLE_HEAL(701, true, VERZIK, "Purple Heal"), //unimplemented
    VERZIK_RED_AUTO(702, true, VERZIK, "Red Auto"), //unimplemented
    VERZIK_THRALL_HEAL(703, true, VERZIK, "Thrall Heal"), //unimplemented
    VERZIK_PLAYER_HEAL(704, true, VERZIK, "Player Heal"), //unimplemented*/

    KODAI_BOP(501, true, ANY_TOB,
            MANUAL_PARSE,
            "Kodai Bop", "Player"),
    DWH_BOP(502, true, ANY_TOB,
            MANUAL_PARSE,
            "DWH Bop", "Player"),
    BGS_WHACK(503, true, ANY_TOB,
            MANUAL_PARSE,
            "BGS Whack", "Player"),
    CHALLY_POKE(504, true, ANY_TOB,
            MANUAL_PARSE,
            "Chally Poke", "Players"),
    THRALL_SPAWN(410, false, ANY_TOB,
            MANUAL_PARSE,
            "Thrall Spawn", "Owner", "Room Tick", "Npc ID", "Room Name"),
    THRALL_DESPAWN(498, false, ANY_TOB,
            MANUAL_PARSE,
            "Thrall Despawn", "Owner", "Room Tick"),
    DAWN_SPEC(487, false, VERZIK,
            MANUAL_PARSE,
            "Dawn Spec", "Player", "Room Tick Damage Applied"),
    DAWN_DAMAGE(488, false, VERZIK,
            MANUAL_PARSE,
            "Dawn Damage", "Damage", "Room Tick"),
    MAIDEN_PLAYER_DRAINED(530, true, MAIDEN,
            MANUAL_PARSE,
            "Player Drained", "Player", "Room Tick"),
    MAIDEN_AUTO(531, true, MAIDEN,
            MANUAL_PARSE,
            "Maiden Auto", "Player targeted", "Room Tick"),

    UPDATE_HP(576, false, ANY_TOB,
            MANUAL_PARSE,
            "Update Boss HP", "HP", "Tick", "Room"), //Hp is in jagex format (744 -> 74.4%)
    ADD_NPC_MAPPING(587, false, ANY_TOB,
            MANUAL_PARSE,
            "Update NPC Mappings", "NPC Index", "Description", "Room"),


    UNKNOWN(-1, false, ANY_TOB,
            MANUAL_PARSE,

            "Unknown"),

    @Deprecated
    ENTERED_TOA(1000, true, ANY_TOA,
            AGNOSTIC,
            "Entered TOA"),
    @Deprecated
    TOA_PARTY_MEMBERS(1001, true, ANY_TOA,
            AGNOSTIC,
            "Party Members", "Player1", "Player2", "Player3", "Player4", "Player5", "Player6", "Player7", "Player8"),
    @Deprecated
    LEFT_TOA(1004, true, ANY_TOA,
            RAID_SPECIFIC,
            "Left TOA", "Room Tick", "Last Room"),
    ENTERED_NEW_TOA_REGION(1006, true, ANY_TOA,
            RAID_SPECIFIC,
             "Entered New TOA Region", "Region"),
    INVOCATION_LEVEL(1100, true, ANY_TOA,
            SET, TOA_INVOCATION_LEVEL,
            "Invocation Level", "Raid Level"),
    RAID_TIMER_START(1101, true, ANY,
            RAID_SPECIFIC,
            "Raid Timer Start", "Client Tick"),
    TOA_CRONDIS_START(1010, true, CRONDIS,
            ROOM_START_FLAG,
            "Crondis Start", "Room Tick"),
    TOA_CRONDIS_FINISHED(1011, true, CRONDIS,
            MANUAL_PARSE,
            "Crondis Finished", "Room Tick"),
    TOA_CRONDIS_WATER(1012, true, CRONDIS,
            MANUAL_PARSE,
            "Crondis Water", "Heal", "Room Tick"),
    TOA_CRONDIS_CROC_DAMAGE(1013, true, CRONDIS,
            MANUAL_PARSE,
            "Crondis Croc Damage", "Damage", "Room Tick"),

    TOA_ZEBAK_START(1020, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Start", "Room Tick"),
    TOA_ZEBAK_FINISHED(1021, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Finished", "Room Tick"),
    TOA_ZEBAK_JUG_PUSHED(1022, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Jug Pushed", "Player", "Room Tick"),
    TOA_ZEBAK_ENRAGED(1023, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Enraged", "Room Tick"),
    TOA_ZEBAK_BOULDER_ATTACK(1024, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Boulder Attack", "Room Tick"),
    TOA_ZEBAK_WATERFALL_ATTACK(1025, true, ZEBAK,
            MANUAL_PARSE,
            "Zebak Waterfall Attack", "Room Tick"),

    TOA_SCABARAS_START(1030, true, SCABARAS,
            MANUAL_PARSE,
            "Scabaras Start", "Room Tick"),
    TOA_SCABARAS_FINISHED(1031, true, SCABARAS,
            MANUAL_PARSE,
            MANUAL_PARSE,
            "Scabaras End", "Room Tick"),

    TOA_KEPHRI_START(1040, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Start", "Client Tick"),
    TOA_KEPHRI_PHASE_1_END(1041, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri P1 End", "Room Tick"),
    TOA_KEPHRI_SWARM_1_END(1042, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Swarm1 End", "Room Tick"),
    TOA_KEPHRI_PHASE_2_END(1043, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri P2 End", "Room Tick"),
    TOA_KEPHRI_SWARM_2_END(1044, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Swarm2 End", "Room Tick"),
    TOA_KEPHRI_FINISHED(1045, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Finished", "Room Tick"),
    TOA_KEPHRI_HEAL(1046, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Swarm Heal", "Room Tick", "Heal"),
    TOA_KEPHRI_SWARM_SPAWN(1047, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Swarm Spawn", "Room Tick"),
    TOA_KEPHRI_BOMB_TANKED(1048, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Bomb Tanked", "Player", "Room Tick"),
    TOA_KEPHRI_DUNG_THROWN(1049, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Dung Thrown", "Room Tick"),
    TOA_KEPHRI_MELEE_ALIVE_TICKS(1350, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Melee Alive Ticks", "Ticks Alive"),
    TOA_KEPHRI_MELEE_HEAL(1351, true, KEPHRI,
            MANUAL_PARSE,
            "Kephri Melee Heal", "Room Tick"),


    TOA_APMEKEN_START(1050, true, APMEKEN,
            MANUAL_PARSE,
            "Apmeken Start", "Client Tick"),
    TOA_APMEKEN_FINISHED(1051, true, APMEKEN,
            MANUAL_PARSE,
            "Apmeken Finished", "Room Tick"),
    TOA_APMEKEN_VOLATILE_SPAWN(1052, true, APMEKEN,
            MANUAL_PARSE,
            "Volatile Spawn", "Room Tick"),
    TOA_APMEKEN_SHAMAN_SPAWN(1053, true, APMEKEN,
            MANUAL_PARSE,
            "Shaman Spawn", "Room Tick"),
    TOA_APMEKEN_CURSED_SPAWN(1054, true, APMEKEN,
            MANUAL_PARSE,
            "Cursed Spawn", "Room Tick"),
    TOA_BABA_START(1060, true, BABA,
            MANUAL_PARSE,
            "Baba Start", "Client Tick"),
    TOA_BABA_PHASE_1_END(1061, true, BABA,
            MANUAL_PARSE,
            "Baba P1 End", "Room Tick"),
    TOA_BABA_BOULDER_1_END(1062, true, BABA,
            MANUAL_PARSE,
            "Baba Boulder1 End", "Room Tick"),
    TOA_BABA_PHASE_2_END(1063, true, BABA,
            MANUAL_PARSE,
            "Baba P2 End", "Room Tick"),
    TOA_BABA_BOULDER_2_END(1064, true, BABA,
            MANUAL_PARSE,
            "Baba Boulder2 End", "Room Tick"),
    TOA_BABA_FINISHED(1065, true, BABA,
            MANUAL_PARSE,
            "Baba Finished", "Room Tick"),
    TOA_BABA_BOULDER_THROW(1066, true, BABA,
            MANUAL_PARSE,
            "Boulder Throw", "Room Tick"),
    TOA_BABA_BOULDER_BROKEN(1067, true, BABA,
            MANUAL_PARSE,
            "Boulder Broken", "Room Tick"),

    TOA_HET_START(1070, true, HET,
            MANUAL_PARSE,
            "Het Start", "Client Tick"),
    TOA_HET_FINISHED(1071, true, HET,
            MANUAL_PARSE,
            "Het Finished", "Room Tick"),
    TOA_HET_PLAYED_MINED_OBELISK(1072, true, HET,
            MANUAL_PARSE,
            "Het Player Mined Obelisk", "Player", "Room Tick"),
    TOA_HET_DOWN(1073, true, HET,
            MANUAL_PARSE,
            "Het Down", "Room Tick"),

    TOA_AKKHA_START(1080, true, AKKHA,
            MANUAL_PARSE,
            "Akkha Start", "Client Tick"),
    TOA_AKKHA_PHASE_1_END(1081, true, AKKHA,
            MANUAL_PARSE,
            "Akkha P1 End", "Room Tick"),
    TOA_AKKHA_SHADOW_1_END(1082, true, AKKHA,
            MANUAL_PARSE,
            "Akkha Shadow1 End", "Room Tick"),
    TOA_AKKHA_PHASE_2_END(1083, true, AKKHA,
            MANUAL_PARSE,
            "Akkha P2 End", "Room Tick"),
    TOA_AKKHA_SHADOW_2_END(1084, true, AKKHA,
            MANUAL_PARSE,
            "Akkha Shadow2 End", "Room Tick"),
    TOA_AKKHA_PHASE_3_END(1085, true, AKKHA,
            MANUAL_PARSE,
            "Akkha P3 End", "Room Tick"),
    TOA_AKKHA_SHADOW_3_END(1086, true, AKKHA,
            MANUAL_PARSE,"Akkha Shadow3 End", "Room Tick"),
    TOA_AKKHA_PHASE_4_END(1087, true, AKKHA,
            MANUAL_PARSE,"Akkha P4 End", "Room Tick"),
    TOA_AKKHA_SHADOW_4_END(1088, true, AKKHA,
            MANUAL_PARSE,"Akkha Shadow4 End", "Room Tick"),
    TOA_AKKHA_PHASE_5_END(1089, true, AKKHA,
            MANUAL_PARSE,"Akkha P5 End", "Room Tick"),
    TOA_AKKHA_FINISHED(1090, true, AKKHA,
            MANUAL_PARSE,"Akkha Finished", "Room Tick"),
    TOA_AKKHA_NULLED_HIT_ON_SHADOW(1091, true, AKKHA,
            MANUAL_PARSE,"Shadow nulled hit", "Player", "Room Tick", "Weapon"),
    TOA_AKKHA_NULLED_HIT_ON_AKKHA(1092, true, AKKHA,
            MANUAL_PARSE,"Akkha nulled hit", "Player", "Room Tick", "Weapon"),

    TOA_WARDENS_START(1200, true, WARDENS,
            MANUAL_PARSE,"Wardens Start", "Client Tick"),
    TOA_WARDENS_P1_END(1201, true, WARDENS,
            MANUAL_PARSE,"Wardens P1 End", "Room Tick"),
    TOA_WARDENS_P2_END(1202, true, WARDENS,
            MANUAL_PARSE,"Wardens P2 End", "Room Tick"),
    TOA_WARDENS_ENRAGED(1203, true, WARDENS,
            MANUAL_PARSE,"Wardens Enraged", "Room Tick"),
    TOA_WARDENS_FINISHED(1204, true, WARDENS,
            MANUAL_PARSE,"Wardens Finished", "Room Tick"),
    TOA_WARDENS_SKULLS_STARTED(1205, true, WARDENS,
            MANUAL_PARSE,"Wardens Skull Started", "Room Tick"),
    TOA_WARDENS_SKULLS_ENDED(1206, true, WARDENS,
            MANUAL_PARSE,"Wardens Skulls Ended", "Room Tick"),
    TOA_WARDENS_CORE_SPAWNED(1207, true, WARDENS,
            MANUAL_PARSE,"Wardens Core Spawned", "Room Tick"),
    TOA_WARDENS_CORE_DESPAWNED(1208, true, WARDENS,
            MANUAL_PARSE,"Wardens Core Despawned", "Room Tick"),


    ;
    /*
    2:DWH //Player, 0, 0, 0, 0
    3:BGS //Player, Damage, 0, 0, 0
    4:Left tob region //Last region, 0, 0, 0, 0
    5:Player died //Player, 0, 0, 0, 0
    6:Entered new tob region //Region, 0, 0, 0, 0 // Regions: (Bloat 1, Nylo 2, Sote 3, Xarpus 4, Verzik 5)
    8: DB Specs // Player, DMG

    10: Blood Spawned //room time, 0, 0, 0, 0
    11: Crab leaked //room time, Description (E.G. N1 30s), Last known health, Current maiden dealth, 0
    12: Maiden spawned //0, 0, 0, 0, 0
    13: Maiden 70s //room time, 0, 0, 0, 0
    14: Maiden 50s //room time, 0, 0, 0, 0
    15: Maiden 30s //room time, 0, 0, 0, 0
    16: Maiden 0 hp //room time, 0, 0, 0, 0
    17: Maiden despawned //room time, 0, 0, 0, 0
    18: Matomenos spawned //position, 0, 0, 0, 0
    19: Maiden Scuffed

    20: Bloat spawned //0, 0, 0, 0, 0
    21: Bloat down //Room time, 0, 0, 0, 0
    22: Bloat 0 HP //room time, 0, 0, 0, 0
    23: Bloat despawn //room time, 0, 0, 0, 0
    24: Bloat HP on 1st down //HP, 0, 0, 0,0

    30: Nylo pillars spawned //0, 0, 0, 0 ,0
    31: Nylo stall //Wave, room time, 0, 0, 0
    32: Range split //Wave, room time, 0, 0, 0
    33: Mage split //Wave, room time, 0, 0, 0
    34: Melee split //Wave, room time, 0, 0, 0
    35: Last wave //Room time, 0, 0, 0, 0
    40: Boss spawn //Room time, 0, 0, 0, 0
    41: Melee rotation //room time, 0, 0, 0, 0
    42: Mage rotation //room time, 0, 0, 0, 0
    43: Range rotation //room time, 0, 0, 0, 0
    44: Nylo 0 HP // room time, 0, 0, 0, 0
    45: Nylo despawned // room time, 0, 0, 0, 0

    5x: sote

    60: xarpus spawned //0, 0, 0, 0, 0
    61: xarpus room started //0, 0, 0, 0, 0
    62: xarpus heal //amount, room time, 0, 0, 0
    63: xarpus screech //room time, 0, 0, 0, 0
    64: xarpus 0 hp //room time, 0, 0, 0, 0
    65: xarpus despawned //room time, 0, 0, 0, 0

    70: verzik spawned //room time, 0, 0, 0, 0
    71: verzik p1 started //0, 0, 0, 0, 0
    72: verzik p1 0 hp //room time, 0, 0, 0, 0
    73: verzik p1 despawned //room time, 0, 0, 0, 0
    80: verzik p2 reds proc // room time, 0, 0, 0, 0
    74: verzik p2 end
    75: verzik p3 0 hp
    76: verzik p3 despawned
    77: verzik bounce //player, room time, 0, 0, 0

    100: party complete
    101: party incomplete
    102: party accurate pre maiden

    201 accurate maiden start
    202 accurate bloat start
    203 accurate nylo start
    204 accurate sote start
    205 accurate xarp start
    206 accurate verzik start

    301 accurate maiden end
    302 accurate bloat end
    303 accurate nylo end
    304 accurate sote end
    305 accurate xarp end
    306 accurate verzik end
    */
    //public final Object[] arguments;

    private static final Map<Integer, LogID> mapper;

    static
    {
        mapper = new HashMap<>();
        for (LogID id : values())
        {
            mapper.put(id.id, id);
        }
    }
    final int id;
    String commonName;
    final RaidRoom room;
    final boolean simple;
    public final List<ParseObject> parseObjects = new ArrayList<>();
    public final List<String> stringArgs = new ArrayList<>();

    LogID(int id, boolean simple, RaidRoom room, Object... arguments)
    {
        this.id = id;
        this.simple = simple;
        this.room = room;
        commonName = "unknown";
        for(Object obj : arguments)
        {
            if(obj instanceof ParseObject)
            {
                parseObjects.add((ParseObject) obj);
            }
            else if(obj instanceof String)
            {
                if(commonName.equals("unknown"))
                {
                    commonName = (String) obj;
                }
                else
                {
                    stringArgs.add((String)obj);
                }
            }
        }
    }

    public static boolean isSimple(int value)
    {
        return valueOf(value).simple;
    }

    public static LogID valueOf(int value)
    {
        return mapper.getOrDefault(value, UNKNOWN);
    }
}