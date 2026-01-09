package me.theTWIXhunter.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.theTWIXhunter.LockedChestsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeyManager {
    
    private final LockedChestsPlugin plugin;
    private final NamespacedKey keyIdKey;
    private final NamespacedKey blankKeyKey;
    
    public KeyManager(LockedChestsPlugin plugin) {
        this.plugin = plugin;
        this.keyIdKey = new NamespacedKey(plugin, "chest_key_id");
        this.blankKeyKey = new NamespacedKey(plugin, "blank_key");
    }
    
    public void registerBlankKeyCrafting() {
        ItemStack blankKey = createBlankKey();
        NamespacedKey recipeKey = new NamespacedKey(plugin, "blank_chest_key");
        
        boolean shapeless = plugin.getConfig().getBoolean("crafting-recipe.shapeless", false);
        var ingredients = plugin.getConfig().getConfigurationSection("crafting-recipe.ingredients");
        
        try {
            if (shapeless) {
                // Create shapeless recipe
                ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, blankKey);
                
                for (String key : ingredients.getKeys(false)) {
                    String materialName = ingredients.getString(key);
                    Material material = Material.valueOf(materialName);
                    recipe.addIngredient(material);
                }
                
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Registered blank key shapeless crafting recipe.");
            } else {
                // Create shaped recipe
                ShapedRecipe recipe = new ShapedRecipe(recipeKey, blankKey);
                
                List<String> shape = plugin.getConfig().getStringList("crafting-recipe.shape");
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));
                
                for (String key : ingredients.getKeys(false)) {
                    String materialName = ingredients.getString(key);
                    Material material = Material.valueOf(materialName);
                    recipe.setIngredient(key.charAt(0), material);
                }
                
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Registered blank key shaped crafting recipe.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register crafting recipe: " + e.getMessage());
        }
    }
    
    public ItemStack createBlankKey() {
        String materialName = plugin.getConfig().getString("key.material", "TRIPWIRE_HOOK");
        Material material = Material.valueOf(materialName);
        
        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();
        
        String name = plugin.getConfig().getString("blank-key.name", "&7Blank Key")
                .replace("&", "§");
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("blank-key.lore")) {
            lore.add(line.replace("&", "§"));
        }
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(blankKeyKey, PersistentDataType.BYTE, (byte) 1);
        
        key.setItemMeta(meta);
        return key;
    }
    
    public ItemStack createKey(String chestId) {
        // Generate unique key ID
        String keyId = UUID.randomUUID().toString();
        
        String materialName = plugin.getConfig().getString("key.material", "TRIPWIRE_HOOK");
        Material material = Material.valueOf(materialName);
        
        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();
        
        String name = plugin.getConfig().getString("key.name", "&6Chest Key")
                .replace("&", "§")
                .replace("%id%", keyId.substring(0, Math.min(8, keyId.length())));
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("key.lore")) {
            lore.add(line.replace("&", "§").replace("%id%", keyId.substring(0, Math.min(8, keyId.length()))));
        }
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(keyIdKey, PersistentDataType.STRING, keyId);
        
        key.setItemMeta(meta);
        return key;
    }
    
    public boolean isBlankKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(blankKeyKey, PersistentDataType.BYTE);
    }
    
    public boolean isKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(keyIdKey, PersistentDataType.STRING);
    }
    
    public String getKeyId(ItemStack item) {
        if (!isKey(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(keyIdKey, PersistentDataType.STRING);
    }
    
    public boolean hasKeyInInventory(org.bukkit.entity.Player player, String chestId) {
        // No longer used - keys are not tied to specific chests
        return false;
    }
    
    public ItemStack createKeyWithSpecificId(String keyId) {
        String materialName = plugin.getConfig().getString("key.material", "TRIPWIRE_HOOK");
        Material material = Material.valueOf(materialName);
        
        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();
        
        String name = plugin.getConfig().getString("key.name", "&6Chest Key")
                .replace("&", "§")
                .replace("%id%", keyId.substring(0, Math.min(8, keyId.length())));
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("key.lore")) {
            lore.add(line.replace("&", "§").replace("%id%", keyId.substring(0, Math.min(8, keyId.length()))));
        }
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(keyIdKey, PersistentDataType.STRING, keyId);
        
        key.setItemMeta(meta);
        return key;
    }
}

