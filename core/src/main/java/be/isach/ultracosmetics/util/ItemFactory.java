package be.isach.ultracosmetics.util;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.log.SmartLogger.LogLevel;
import be.isach.ultracosmetics.version.VersionManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by sacha on 03/08/15.
 */
public class ItemFactory {
    private static final List<XMaterial> DYES = getDyes();
    private static boolean noticePrinted = false;
    public static ItemStack fillerItem;

    public static ItemStack create(XMaterial material, String displayName, String... lore) {
        return rename(material.parseItem(), displayName, lore);
    }

    public static ItemStack createColored(String oldMaterialName, byte data, String displayName, String... lore) {
        ItemStack itemStack;
        if (VersionManager.IS_VERSION_1_13) {
            itemStack = new ItemStack(BlockUtils.getBlockByColor(oldMaterialName, data), 1);
        } else {
            itemStack = new MaterialData(BlockUtils.getOldMaterial(oldMaterialName), data).toItemStack(1);
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        if (lore != null) {
            List<String> finalLore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            for (String s : lore)
                if (s != null)
                    finalLore.add(ChatColor.translateAlternateColorCodes('&', s));
            itemMeta.setLore(finalLore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack rename(ItemStack itemstack, String displayName) {
        return rename (itemstack, displayName, (String[])null);
    }


    public static ItemStack rename(ItemStack itemstack, String displayName, String... lore) {
        ItemMeta meta = itemstack.getItemMeta();
        meta.setDisplayName(displayName);
        if (lore != null) {
            List<String> finalLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            for (String s : lore) {
                if (s != null) {
                    finalLore.add(ChatColor.translateAlternateColorCodes('&', s));
                }
            }
            meta.setLore(finalLore);
        }
        itemstack.setItemMeta(meta);
        return itemstack;
    }

    public static void fillInventory(Inventory inventory) {
        if (SettingsManager.getConfig().getBoolean("Fill-Blank-Slots-With-Item.Enabled")) {
            if (fillerItem == null) {
                ItemStack itemStack = getItemStackFromConfig("Fill-Blank-Slots-With-Item.Item");
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(ChatColor.GRAY + "");
                itemStack.setItemMeta(itemMeta);
                fillerItem = itemStack;
            }
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null
                        || inventory.getItem(i).getType() == Material.AIR)
                    inventory.setItem(i, fillerItem);
            }
        }
    }

    public static ItemStack getItemStackFromConfig(String path) {
        XMaterial mat = getFromConfigInternal(path);
        if (mat != null) return mat.parseItem();
        return create(XMaterial.BEDROCK, "&cError parsing material", "&cFailed to parse material");
    }

    public static XMaterial getXMaterialFromConfig(String path) {
        XMaterial mat = getFromConfigInternal(path);
        return mat == null ? XMaterial.BEDROCK : mat;
    }

    private static XMaterial getFromConfigInternal(String path) {
        String fromConfig = UltraCosmeticsData.get().getPlugin().getConfig().getString(path);
        if (MathUtils.isInteger(fromConfig) || fromConfig.contains(":")) {
            if (!noticePrinted) {
                UltraCosmeticsData.get().getPlugin().getSmartLogger().write(LogLevel.ERROR, "UltraCosmetics no longer supports numeric IDs, please replace it with a material name.");
                noticePrinted = true;
            }
            UltraCosmeticsData.get().getPlugin().getSmartLogger().write(LogLevel.ERROR, "Offending config path: " + path);
            return null;
        }
        // null if not found
        return XMaterial.matchXMaterial(fromConfig).orElse(null);
    }

    public static ItemStack createSkull(String url, String name) {
        ItemStack head = create(XMaterial.PLAYER_HEAD, name);

        if (url.isEmpty()) return head;

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwner("Notch");
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", url));
        Method setProfileMethod = null;
        try {
            setProfileMethod = headMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
        } catch (NoSuchMethodException | SecurityException ignored) {}
        try {
            // if available, we use setProfile(GameProfile) so that it sets both the profile field and the
            // serialized profile field for us. If the serialized profile field isn't set
            // ItemStack#isSimilar() and ItemStack#equals() throw an error.
            if (setProfileMethod == null) {
                Field profileField = headMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(headMeta, profile);
            } else {
                setProfileMethod.setAccessible(true);
                setProfileMethod.invoke(headMeta, profile);
            }
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | SecurityException | InvocationTargetException e1) {
            e1.printStackTrace();
        }
        head.setItemMeta(headMeta);
        return head;
    }

    public static ItemStack createColouredLeather(Material armourPart, int red, int green, int blue) {
        ItemStack itemStack = new ItemStack(armourPart);
        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        leatherArmorMeta.setColor(Color.fromRGB(red, green, blue));
        itemStack.setItemMeta(leatherArmorMeta);
        return itemStack;
    }

    public static ItemStack addGlow(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(itemMeta);
        return item;
    }

    public static boolean areSame(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) {
            return false;
        }

        if (a.getData().getData() != b.getData().getData()) {
            return false;
        }
        if ((a.hasItemMeta() && !b.hasItemMeta())
                || (!a.hasItemMeta() && b.hasItemMeta())) {
            return false;
        }
        if (!a.hasItemMeta() && !b.hasItemMeta()) {
            return true;
        }
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();

        return am.getDisplayName().equalsIgnoreCase(bm.getDisplayName());
    }

    public static boolean haveSameName(ItemStack a, ItemStack b) {
        if (a.hasItemMeta() && b.hasItemMeta()) {
            if (a.getItemMeta().hasDisplayName() && b.getItemMeta().hasDisplayName()) {
                return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
            }
        }
        return false;
    }

    private static List<XMaterial> getDyes() {
        // 16 dyes
        List<XMaterial> dyes = new ArrayList<>(16);
        for (XMaterial mat : XMaterial.VALUES) {
            if (mat.toString().endsWith("_DYE")) {
                dyes.add(mat);
            }
        }
        return dyes;
    }

    public static ItemStack getRandomDye() {
        return DYES.get(ThreadLocalRandom.current().nextInt(DYES.size())).parseItem();
    }
}
