package com.kuinox.enchantmentfixes;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

import static java.lang.Math.round;


//Constants are here: https://minecraft.gamepedia.com/Tutorials/Enchantment_mechanics
public class EnchantmentListener implements Listener {

    abstract class MinMaxEnchantability {
        /**
         * Returns the minimal value of enchantability needed on the enchantment level passed.
         */
        public int getMinEnchantability(int enchantmentLevel) {
            return 1 + enchantmentLevel * 10;
        }

        /**
         * Returns the maximum value of enchantability needed on the enchantment level passed.
         */
        public int getMaxEnchantability(int enchantmentLevel) {
            return this.getMinEnchantability(enchantmentLevel) + 5;
        }
    }

    class MinMaxConst extends MinMaxEnchantability {
        int _minConst;
        int _maxConst;

        MinMaxConst(int minConst, int maxConst) {
            _minConst = minConst;
            _maxConst = maxConst;
        }

        @Override
        public int getMaxEnchantability(int enchantmentLevel) {
            return _maxConst;
        }

        @Override
        public int getMinEnchantability(int enchantmentLevel) {
            return _minConst;
        }
    }

    class MinMaxDamage extends MinMaxEnchantability {
        /**
         * Holds the base factor of enchantability needed to be able to use the enchant.
         */
        private final int[] BASE_ENCHANTABILITY = new int[]{1, 5, 5};

        /**
         * Holds how much each level increased the enchantability factor to be able to use this enchant.
         */
        private final int[] LEVEL_ENCHANTABILITY = new int[]{11, 8, 8};

        /**
         * Used on the formula of base enchantability, this is the 'window' factor of values to be able to use thing
         * enchant.
         */
        private final int[] THRESHOLD_ENCHANTABILITY = new int[]{20, 20, 20};

        /**
         * Defines the type of damage of the enchantment, 0 = all, 1 = undead, 2 = arthropods
         */
        final int damageType;

        MinMaxDamage(int damageTypeIn) {
            this.damageType = damageTypeIn;
        }

        @Override
        public int getMinEnchantability(int enchantmentLevel) {
            return BASE_ENCHANTABILITY[this.damageType] + (enchantmentLevel - 1) * LEVEL_ENCHANTABILITY[this.damageType];
        }

        @Override
        public int getMaxEnchantability(int enchantmentLevel) {
            return this.getMinEnchantability(enchantmentLevel) + THRESHOLD_ENCHANTABILITY[this.damageType];
        }
    }

    class MinMaxLootBonus extends MinMaxEnchantability {
        @Override
        public int getMinEnchantability(int enchantmentLevel) {
            return 15 + (enchantmentLevel - 1) * 9;
        }

        @Override
        public int getMaxEnchantability(int enchantmentLevel) {
            return super.getMinEnchantability(enchantmentLevel) + 50;
        }
    }

    enum ProtectionType {
        ALL(1, 11),
        FIRE(10, 8),
        FALL(5, 6),
        EXPLOSION(5, 8),
        PROJECTILE(3, 6);
        private final int minEnchantability;
        private final int levelCost;

        ProtectionType(int minimal, int levelCost) {
            this.minEnchantability = minimal;
            this.levelCost = levelCost;
        }

        public int getMinimalEnchantability() {
            return this.minEnchantability;
        }

        public int getEnchantIncreasePerLevel() {
            return this.levelCost;
        }
    }

    class MinMaxProtection extends MinMaxEnchantability {

        ProtectionType _protectionType;

        MinMaxProtection(ProtectionType protectionType) {
            _protectionType = protectionType;
        }

        @Override
        public int getMinEnchantability(int enchantmentLevel) {
            return _protectionType.getMinimalEnchantability() + (enchantmentLevel - 1) * _protectionType.getEnchantIncreasePerLevel();
        }

        @Override
        public int getMaxEnchantability(int enchantmentLevel) {
            return this.getMinEnchantability(enchantmentLevel) + this._protectionType.getEnchantIncreasePerLevel();
        }

    }


    private Map<Enchantment, MinMaxEnchantability> _minmax_map = new Hashtable<Enchantment, MinMaxEnchantability>() {{
        //armor
        put(Enchantment.PROTECTION_ENVIRONMENTAL, new MinMaxProtection(ProtectionType.ALL));
        put(Enchantment.PROTECTION_FALL, new MinMaxProtection(ProtectionType.FALL));
        put(Enchantment.PROTECTION_FIRE, new MinMaxProtection(ProtectionType.FIRE));
        put(Enchantment.PROTECTION_PROJECTILE, new MinMaxProtection(ProtectionType.PROJECTILE));
        put(Enchantment.WATER_WORKER, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 1;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 40;
            }
        });
        put(Enchantment.PROTECTION_EXPLOSIONS, new MinMaxProtection(ProtectionType.EXPLOSION));
        put(Enchantment.OXYGEN, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 10 * enchantmentLevel;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 30;
            }
        });
        put(Enchantment.DEPTH_STRIDER, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return enchantmentLevel * 10;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 15;
            }
        });
        put(Enchantment.THORNS, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 10 + 20 * (enchantmentLevel - 1);
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
//sword
        put(Enchantment.DAMAGE_ALL, new MinMaxDamage(0));
        put(Enchantment.DAMAGE_ARTHROPODS, new MinMaxDamage(2));
        put(Enchantment.KNOCKBACK, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 5 + 20 * (enchantmentLevel - 1);
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
        put(Enchantment.DAMAGE_UNDEAD, new MinMaxDamage(1));
        put(Enchantment.FIRE_ASPECT, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 10 + 20 * (enchantmentLevel - 1);
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
        put(Enchantment.LOOT_BONUS_MOBS, new MinMaxLootBonus());
        put(Enchantment.SWEEPING_EDGE, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 5 + (enchantmentLevel - 1) * 9;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 15;
            }
        });
//tool
        put(Enchantment.DIG_SPEED, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 1 + 10 * (enchantmentLevel - 1);
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
        put(Enchantment.LOOT_BONUS_BLOCKS, new MinMaxLootBonus());
        put(Enchantment.SILK_TOUCH, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 15;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
//bow
        put(Enchantment.ARROW_DAMAGE, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 1 + (enchantmentLevel - 1) * 10;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return getMinEnchantability(enchantmentLevel) + 15;
            }
        });
        put(Enchantment.ARROW_FIRE, new MinMaxConst(20, 50));
        put(Enchantment.ARROW_KNOCKBACK, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 12 + (enchantmentLevel - 1) * 20;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 25;
            }
        });
        put(Enchantment.ARROW_INFINITE, new MinMaxConst(20, 50));
//fishing rod
        put(Enchantment.LUCK, new MinMaxLootBonus());
        put(Enchantment.LURE, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 15 + (enchantmentLevel - 1) * 9;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
//trident
        put(Enchantment.LOYALTY, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 5 + enchantmentLevel * 7;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return 50;
            }
        });
        put(Enchantment.IMPALING, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 1 + (enchantmentLevel - 1) * 8;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 20;
            }
        });
        put(Enchantment.RIPTIDE, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 10 + enchantmentLevel * 7;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return 50;
            }
        });
        put(Enchantment.CHANNELING, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 25;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return 50;
            }
        });
//crossbow
        put(Enchantment.QUICK_CHARGE, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 12 + (enchantmentLevel - 1) * 20;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return this.getMinEnchantability(enchantmentLevel) + 25;
            }
        });
        put(Enchantment.MULTISHOT, new MinMaxConst(20, 50));
        put(Enchantment.PIERCING, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 15 + (enchantmentLevel - 1) * 9;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
//everything
        put(Enchantment.DURABILITY, new MinMaxEnchantability() {
            @Override
            public int getMinEnchantability(int enchantmentLevel) {
                return 5 + (enchantmentLevel - 1) * 8;
            }

            @Override
            public int getMaxEnchantability(int enchantmentLevel) {
                return super.getMinEnchantability(enchantmentLevel) + 50;
            }
        });
    }};

    private Map<Enchantment, Integer> _weight_map = new Hashtable<Enchantment, Integer>() {{
        //armor
        put(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
        put(Enchantment.PROTECTION_FALL, 5);
        put(Enchantment.PROTECTION_FIRE, 5);
        put(Enchantment.PROTECTION_PROJECTILE, 5);
        put(Enchantment.WATER_WORKER, 2);
        put(Enchantment.PROTECTION_EXPLOSIONS, 2);
        put(Enchantment.OXYGEN, 2);
        put(Enchantment.DEPTH_STRIDER, 2);
        put(Enchantment.THORNS, 1);
//sword
        put(Enchantment.DAMAGE_ALL, 10);
        put(Enchantment.DAMAGE_ARTHROPODS, 5);
        put(Enchantment.KNOCKBACK, 5);
        put(Enchantment.DAMAGE_UNDEAD, 5);
        put(Enchantment.FIRE_ASPECT, 2);
        put(Enchantment.LOOT_BONUS_MOBS, 2);
        put(Enchantment.SWEEPING_EDGE, 2);
//tool
        put(Enchantment.DIG_SPEED, 10);
        put(Enchantment.LOOT_BONUS_BLOCKS, 2);
        put(Enchantment.SILK_TOUCH, 1);
//bow
        put(Enchantment.ARROW_DAMAGE, 10);
        put(Enchantment.ARROW_FIRE, 2);
        put(Enchantment.ARROW_KNOCKBACK, 2);
        put(Enchantment.ARROW_INFINITE, 1);
//fishing rod
        put(Enchantment.LUCK, 2);
        put(Enchantment.LURE, 2);
//trident
        put(Enchantment.LOYALTY, 5);
        put(Enchantment.IMPALING, 2);
        put(Enchantment.RIPTIDE, 2);
        put(Enchantment.CHANNELING, 1);
//crossbow
        put(Enchantment.QUICK_CHARGE, 10);
        put(Enchantment.MULTISHOT, 3);
        put(Enchantment.PIERCING, 30);
//everything
        put(Enchantment.DURABILITY, 5);
    }};


    private Map<Material, Integer> _enchantability = new Hashtable<Material, Integer>() {{
        put(Material.WOODEN_AXE, 15);
        put(Material.WOODEN_HOE, 15);
        put(Material.WOODEN_PICKAXE, 15);
        put(Material.WOODEN_SHOVEL, 15);
        put(Material.WOODEN_SWORD, 15);

        put(Material.STONE_AXE, 5);
        put(Material.STONE_HOE, 5);
        put(Material.STONE_PICKAXE, 5);
        put(Material.STONE_SHOVEL, 5);
        put(Material.STONE_SWORD, 5);

        put(Material.IRON_AXE, 14);
        put(Material.IRON_HOE, 14);
        put(Material.IRON_PICKAXE, 14);
        put(Material.IRON_SHOVEL, 14);
        put(Material.IRON_SWORD, 14);

        put(Material.DIAMOND_AXE, 10);
        put(Material.DIAMOND_HOE, 10);
        put(Material.DIAMOND_PICKAXE, 10);
        put(Material.DIAMOND_SHOVEL, 10);
        put(Material.DIAMOND_SWORD, 10);

        put(Material.GOLDEN_AXE, 22);
        put(Material.GOLDEN_HOE, 22);
        put(Material.GOLDEN_PICKAXE, 22);
        put(Material.GOLDEN_SHOVEL, 22);
        put(Material.GOLDEN_SWORD, 22);

        put(Material.LEATHER_BOOTS, 15);
        put(Material.LEATHER_LEGGINGS, 15);
        put(Material.LEATHER_CHESTPLATE, 15);
        put(Material.LEATHER_HELMET, 15);

        put(Material.IRON_BOOTS, 9);
        put(Material.IRON_LEGGINGS, 9);
        put(Material.IRON_CHESTPLATE, 9);
        put(Material.IRON_HELMET, 9);

        put(Material.CHAINMAIL_BOOTS, 12);
        put(Material.CHAINMAIL_LEGGINGS, 12);
        put(Material.CHAINMAIL_CHESTPLATE, 12);
        put(Material.CHAINMAIL_HELMET, 12);

        put(Material.DIAMOND_BOOTS, 10);
        put(Material.DIAMOND_LEGGINGS, 10);
        put(Material.DIAMOND_CHESTPLATE, 10);
        put(Material.DIAMOND_HELMET, 10);

        put(Material.GOLDEN_BOOTS, 25);
        put(Material.GOLDEN_LEGGINGS, 25);
        put(Material.GOLDEN_CHESTPLATE, 25);
        put(Material.GOLDEN_HELMET, 25);

        put(Material.FISHING_ROD, 1);
        put(Material.BOOK, 1);
        put(Material.BOW, 1);
        put(Material.CROSSBOW, 1);
        put(Material.TRIDENT, 1);

    }};


    private Map<Player, Long> _seeds;
    private Logger _m;
    private Plugin _plugin;

    EnchantmentListener(Plugin plugin) {
        _seeds = new HashMap<>();
        _m = plugin.getLogger();
        _plugin = plugin;
    }

    private Random getPlayerRandom(Plugin plugin, Player player, int button) {
        if (!_seeds.containsKey(player)) {
            _seeds.put(player, (new Random()).nextLong());//time based seed, i guess it shouldn't be bad.
        }
        List<World> worlds = plugin.getServer().getWorlds();
        long seed = 0;
        if (worlds.size() != 0) {
            seed = worlds.get(0).getSeed();
        }
        Random rand = new Random((player.getUniqueId().getLeastSignificantBits() | seed) + player.getStatistic(Statistic.ITEM_ENCHANTED));
        long newSeed = rand.nextLong();//one seed for each button.
        for (int i = 0; i < button; i++) {
            newSeed = rand.nextLong();
        }
        return new Random(newSeed);//should be reboot proof
    }

    @EventHandler
    public void onEnchantProposal(PrepareItemEnchantEvent e) {

        if (e.getOffers()[0] == null && e.getOffers()[1] == null && e.getOffers()[2] == null)
            return; //Minecraft didn't proposed any enchantments, we shouldn't find one.
        if (!_enchantability.containsKey(e.getItem().getType())) {
            _m.warning("Minecraft found at least one enchantment but we didn't knew this item could be enchanted. Displaying uncorrected enchantments.");
            return;
        }

        for (int i = 0; i < e.getOffers().length; i++) {
            if (e.getOffers()[i] == null) continue;
            Random playerRandom = getPlayerRandom(_plugin, e.getEnchanter(), i);
            int modifiedEnchantLevel = getModifiedEnchantLevel(playerRandom, e.getItem().getType(), e.getOffers()[i].getCost());
            CustomOffer ourOffer = getCustomNewCustomOffer(playerRandom, e.getItem(), modifiedEnchantLevel);
            if (ourOffer == null) return;
            e.getOffers()[i].setEnchantment(ourOffer.enchantment);
            e.getOffers()[i].setEnchantmentLevel(ourOffer.level);
        }
    }

    private CustomOffer getCustomNewCustomOffer(Random playerRandom, ItemStack item, int modifiedEnchantLevel) {
        List<Enchantment> allEnchantsThatCanBeApplied = getEnchantsThatCanBeApplied(item);


        LinkedHashMap<Enchantment, Integer> possiblesOffers = getEnchantsForThisEnchantability(allEnchantsThatCanBeApplied, modifiedEnchantLevel);
        if (possiblesOffers.size() == 0) {
            _m.warning("I didn't found any offer when the game could.");
            return null;
        }
        Enchantment weightedSelected = getEnchantmentRandomlyOnWeight(playerRandom, possiblesOffers);
        if (weightedSelected == null) return null;
        CustomOffer offer = new CustomOffer(possiblesOffers.get(weightedSelected), weightedSelected, possiblesOffers);
        possiblesOffers.remove(weightedSelected);
        return offer;
    }

    private Enchantment getEnchantmentRandomlyOnWeight(Random playerRandom, HashMap<Enchantment, Integer> possiblesOffers) {
        int w = playerRandom.nextInt(getTotalWeight(possiblesOffers.keySet()));
        for (Enchantment curr : possiblesOffers.keySet()) {
            w -= _weight_map.get(curr);
            if (w < 0) {
                return curr;
            }
        }
        _m.warning("Algorithm error !");
        return null;
    }

    class CustomOffer {
        int level;
        Enchantment enchantment;
        HashMap<Enchantment, Integer> possibleEnchantments;

        CustomOffer(int level, Enchantment enchantment, HashMap<Enchantment, Integer> possibleEnchantments) {
            this.level = level;
            this.enchantment = enchantment;
            this.possibleEnchantments = possibleEnchantments;
        }
    }


    private int getTotalWeight(Set<Enchantment> enchantments) {
        int sum = 0;
        for (Enchantment enchantment : enchantments) {
            sum += _weight_map.get(enchantment);
        }
        return sum;
    }

    private LinkedHashMap<Enchantment, Integer> getEnchantsForThisEnchantability(List<Enchantment> allEnchantsThatCanBeApplied, int enchantability) {
        LinkedHashMap<Enchantment, Integer> output = new LinkedHashMap<>();
        for (Enchantment curr : allEnchantsThatCanBeApplied) {
            int level = getEnchantLevelForItem(curr, enchantability);
            if (level == 0) continue;
            output.put(curr, level);
        }
        return output;
    }

    private List<Enchantment> getEnchantsThatCanBeApplied(ItemStack item) {
        List<Enchantment> output = new LinkedList<>();
        for (int i = 0; i < Enchantment.values().length; i++) {
            Enchantment curr = Enchantment.values()[i];
            if ((curr.canEnchantItem(item) || item.getType() == Material.BOOK)
                    && !curr.isTreasure()) {
                output.add(curr);
            }
        }
        return output;
    }


    private int getEnchantLevelForItem(Enchantment enchantment, int enchantability) {
        MinMaxEnchantability minMax = _minmax_map.get(enchantment);
        for (int i = enchantment.getMaxLevel(); i > 0; i--) {
            boolean first = minMax.getMaxEnchantability(i) >= enchantability;
            boolean second = minMax.getMinEnchantability(i) <= enchantability;
            if (first && second) {
                return i;
            }
        }
        return 0;
    }

    private int getModifiedEnchantLevel(Random r, Material material, int enchantLevel) {
        int enchantability = _enchantability.get(material);

        // Generate a random number between 1 and 1+(enchantability/2), with a triangular distribution
        float enchantability_2 = enchantability / 2;
        int rand_enchantability = 1 + r.nextInt((int) (enchantability_2 / 2 + 1)) + r.nextInt((int) (enchantability_2 / 2 + 1));
        // Choose the enchantment level
        int k = enchantLevel + rand_enchantability;

        // A random bonus, between .85 and 1.15
        float rand_bonus_percent = 1 + (r.nextFloat() + r.nextFloat() - 1) * 0.15f;

        // Finally, we calculate the level
        int final_level = round(k * rand_bonus_percent);
        if (final_level < 1) final_level = 1;
        return final_level;
    }

    @EventHandler
    public void onEnchantFinish(EnchantItemEvent e) {
        Player currentPlayer = e.getEnchanter();
        if (!_seeds.containsKey(currentPlayer)) {
            _m.severe("Probably a bug: Player did an enchantment without triggering onEnchantProposal.");
            return;
        }
        Random playerRandom = getPlayerRandom(_plugin, e.getEnchanter(), e.whichButton());
        int modifiedEnchLevel = getModifiedEnchantLevel(playerRandom, e.getItem().getType(), e.getExpLevelCost());
        CustomOffer offer = getCustomNewCustomOffer(playerRandom, e.getItem(), modifiedEnchLevel);
        if (offer == null) return;
        e.getEnchantsToAdd().clear();
        e.getEnchantsToAdd().put(offer.enchantment, offer.level);
        int rand = playerRandom.nextInt(50);
        while (rand <= modifiedEnchLevel) {
            offer.possibleEnchantments.keySet().removeIf(enchantment -> {
                for (Enchantment curr : e.getEnchantsToAdd().keySet()) {
                    if (enchantment.conflictsWith(curr)) {
                        return true;
                    }
                }
                return false;
            });
            if (offer.possibleEnchantments.isEmpty()) break;
            Enchantment additionalEnchant = getEnchantmentRandomlyOnWeight(playerRandom, offer.possibleEnchantments);
            e.getEnchantsToAdd().put(additionalEnchant, offer.possibleEnchantments.get(additionalEnchant));
            offer.possibleEnchantments.remove(additionalEnchant);
            modifiedEnchLevel = modifiedEnchLevel / 2;
            rand = playerRandom.nextInt(50);
        }
        _seeds.remove(currentPlayer);
    }
}
