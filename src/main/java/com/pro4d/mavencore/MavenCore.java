package com.pro4d.mavencore;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
public final class MavenCore extends JavaPlugin implements Listener {

    private List<Inventory> inventories;

    private boolean useNMS = false;

    private Method asNMSCopyMethod;
    private Method saveNMSItemStackMethod;
    private Class<?> nbtTagCompoundObject;

    private final String INV = "[inv]";
    private final String INVENTORY = "[inventory]";

    //to ensure players cannot run the command to show players inventory without clicking the message in chat
    public static final String PASSWORD = "Lnk_Wnywja_(:";

    private MConfig config;

    String[] signText = new String[4];

    int refillGUISize = 9;
    String refillGUIName;
    Map<Integer, ItemStack> refillGUI;

    boolean removeGlassBottles;
    boolean stackPotions;

    String invHoverMsg;
    String invChatMsg;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        config = new MConfig("config", this);
        refillGUI = new HashMap<>();
        loadConfig();

        new CopyInvCommand(this);

        inventories = new ArrayList<>();

        if(!getServer().getVersion().contains("1.8.8")) {
            useNMS = true;

            Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
            asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

            Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
            nbtTagCompoundObject = ReflectionUtil.getNMSClass("NBTTagCompound");
            saveNMSItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundObject);
        }
    }


    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if(!event.hasBlock()) return;
        if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        Block b = event.getClickedBlock();
        if(b.getType() != Material.SIGN_POST  && b.getType() != Material.WALL_SIGN) return;

        Sign sign = (Sign) b.getState();
        if(!Arrays.equals(sign.getLines(), signText)) return;

        Player player = event.getPlayer();
        player.openInventory(createAndFillPotionInv());
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if(message.contains("[item]")) {
            ItemStack item = player.getItemInHand();
            if(item.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Can not display air.");
                return;
            }

            String name;
            if(item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                name = item.getItemMeta().getDisplayName();
            } else name = item.getType().name();

            int pos = -1;
            for (int c = 0; c < name.toCharArray().length;) {
                if(ChatColor.COLOR_CHAR == name.toCharArray()[c]) pos = c;
                c++;
            }

            if(pos == -1) {
                name = new StringBuilder(name).insert(0, "[").toString();
                name = ChatColor.AQUA + name;

            } else name = new StringBuilder(name).insert(pos + 2, "[").toString();

            name = new StringBuilder(name).insert(name.length(), "]").toString();

            TextComponent itemHover = new TextComponent(name);
            itemHover.setUnderlined(true);

            String nmsItem;
            if(!useNMS) {
                nmsItem = CraftItemStack.asNMSCopy(item).save(new NBTTagCompound()).toString();
            } else {nmsItem = getJSONStringFromItem(item);}
            itemHover.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(nmsItem).create()));

            String[] messageSplit = message.split("(?=\\[item])|(?<=\\[item])");

            TextComponent replaced = new TextComponent("");

            for(int i = 0; i < messageSplit.length;) {
                if(messageSplit[i].equalsIgnoreCase("[item]")) {
                    replaced.addExtra(itemHover);
                } else {replaced.addExtra(messageSplit[i]);}
                i++;
            }
            String format = event.getFormat().split("%2")[0].replace("$s", "");

            String pName = player.getDisplayName(); //may need to be altered depending on other chat plugins?

            TextComponent n = new TextComponent(format.replace("%1", pName));
            n.addExtra(replaced);

            player.getServer().getOnlinePlayers().forEach(p -> p.spigot().sendMessage(n));
            event.setCancelled(true);
        }

        if(message.contains(INVENTORY) || message.contains(INV)) {
            String invName = invChatMsg;
            if(invName.contains("{player}")) invName = invName.replace("{player}", player.getDisplayName());
            if(invHoverMsg.contains("{player}")) invHoverMsg = invHoverMsg.replace("{player}", player.getDisplayName());

            TextComponent invClick = new TextComponent(invName);

            invClick.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(invHoverMsg).create()));
            invClick.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/displayinv " + player.getName() + " " + MavenCore.PASSWORD));

            String[] messageSplit = message.split("(?=\\[inv])|(?<=\\[inv])|(?=\\[inventory])|(?<=\\[inventory])");

            TextComponent replaced = new TextComponent("");
            for(int i = 0; i < messageSplit.length;) {
                if(messageSplit[i].equalsIgnoreCase(INV) || messageSplit[i].equalsIgnoreCase(INVENTORY)) {
                    replaced.addExtra(invClick);
                } else {
                    String nextMessage = messageSplit[i];
                    replaced.addExtra(nextMessage);
                }
                i++;
            }

            String format = event.getFormat().split("%2")[0].replace("$s", "");
            String pName = player.getDisplayName(); //may need to be altered depending on other chat plugins?

            TextComponent n = new TextComponent(format.replace("%1", pName));
            n.addExtra(replaced);

            player.getServer().getOnlinePlayers().forEach(p -> p.spigot().sendMessage(n));
            event.setCancelled(true);
        }

    }

    @EventHandler
    private void onClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == null) return;
        if(inventories.contains(event.getClickedInventory())) event.setCancelled(true);
    }

    @EventHandler
    private void onDrink(PlayerItemConsumeEvent event) {
        if(!removeGlassBottles) return;
        if(event.getItem().getType().equals(Material.POTION)) {
            Inventory inv = event.getPlayer().getInventory();
            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                for(int s = 0; s < inv.getSize();) {
                    if(inv.getItem(s) != null && inv.getItem(s).getType() == Material.GLASS_BOTTLE) {
                        inv.setItem(s, null);
                    }
                    s++;
                }
            }, 1L);
        }
    }

    @EventHandler
    private void onDrop(ItemSpawnEvent event) {
        if(!stackPotions) return;
        if(event.getEntity().getItemStack().getType() != Material.POTION) return;
        Potion potionDropped = Potion.fromItemStack(event.getEntity().getItemStack());

        List<Entity> droppedItems = event.getEntity().getNearbyEntities(12, 5, 12).stream().filter(e -> e.getType() == EntityType.DROPPED_ITEM).collect(Collectors.toList());
        List<Entity> notBottles = new ArrayList<>();
        for(Entity e : droppedItems) {
            Item i = (Item) e;
            if(i.getItemStack().getType() != Material.POTION) {
                notBottles.add(e);
                continue;
            }
            Potion p = Potion.fromItemStack(i.getItemStack());
            if(p.getType() != potionDropped.getType()) notBottles.add(e);
        }

        droppedItems.removeAll(notBottles);

        if(droppedItems.size() == 0) return;
        event.getEntity().remove();

        Item i = (Item) droppedItems.get(0);
        ItemStack item = i.getItemStack();
        int potionCount = 2;

        if(item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            try {
                int count = Integer.parseInt(name);
                potionCount = potionCount + count - 1;
            } catch (NumberFormatException ignored) {}
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(String.valueOf(potionCount));
        item.setItemMeta(meta);
    }

    @EventHandler
    private void onPickup(PlayerPickupItemEvent event) {
        if(!stackPotions) return;
        if(event.getItem().getType() != EntityType.DROPPED_ITEM) return;

        ItemStack item = event.getItem().getItemStack();
        if(item.getType() != Material.POTION) return;
        if(item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            try {
                int count = Integer.parseInt(name);
                Potion pot = Potion.fromItemStack(item);

                Potion potion = new Potion(pot.getType(), pot.getLevel());
                potion.setSplash(pot.isSplash());
                if(!pot.getType().isInstant()) potion.setHasExtendedDuration(pot.hasExtendedDuration());

                ItemStack potionItemStack = potion.toItemStack(1);
                ItemStack[] potions = new ItemStack[count];

                for(int c = 0; c < count;) {
                    //event.getPlayer().getInventory().addItem(potionItemStack);
                    potions[c] = potionItemStack;
                    c++;
                }

                Map<Integer, ItemStack> leftOver = event.getPlayer().getInventory().addItem(potions);
                if(leftOver.isEmpty()) {
                    event.getItem().remove();
                } else {
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(String.valueOf(leftOver.keySet().size()));
                    item.setItemMeta(meta);
                    event.getItem().setItemStack(item);
                }

                event.setCancelled(true);
            } catch (NumberFormatException ignored) {}
        }
    }


    private String getJSONStringFromItem(ItemStack item) {
        Object itemAsJsonString;
        try {
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            Object tagCompound = nbtTagCompoundObject.newInstance();
            itemAsJsonString = saveNMSItemStackMethod.invoke(nmsItemStack, tagCompound);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to serialize ItemStack to NMS item", t);
            return null;
        }
        return itemAsJsonString.toString();
    }

    private Inventory createAndFillPotionInv() {
        Inventory inv = Bukkit.createInventory(null, refillGUISize, ChatColor.translateAlternateColorCodes('&', refillGUIName));
        for(int s: refillGUI.keySet()) {
            inv.setItem(s - 1, refillGUI.get(s));
        }
        return inv;
    }

    private void loadConfig() {
        FileConfiguration fileConfig = config.getConfig();

        List<String> sign = new ArrayList<>();
        sign.add("&8[&4Refill&8]");
        sign.add("Click to refill");
        sign.add("");
        sign.add("");

        if(fileConfig.isList("sign-text")) {
            signText = fileConfig.getStringList("sign-text").toArray(new String[0]);
        } else signText = sign.toArray(new String[0]);

        for(int s = 0; s < signText.length;) {
            if(signText[s].toCharArray().length > 15) signText[s] = sign.get(s);
            signText[s] = ChatColor.translateAlternateColorCodes('&', signText[s]);
            s++;
        }

        refillGUISize = fileConfig.getInt("gui-size", 9);
        if(refillGUISize % 9 != 0) {
            getLogger().warning(ChatColor.RED + "GUI Size can only be a multiple of 9, setting to default...");
            refillGUISize = 9;
        }
        refillGUIName = fileConfig.getString("Name", "&cRefill");

        Map<Integer, ItemStack> defaultItems = new HashMap<>();

        Potion pot = new Potion(PotionType.INSTANT_HEAL, 1);
        pot.setSplash(true);
        for(int n = 0; n < refillGUISize;) {
            defaultItems.put(n, pot.toItemStack(1));
            n++;
        }

        if(fileConfig.contains("inventory.Items") && fileConfig.isList("inventory.Items")) {
            List<String> items = fileConfig.getStringList("inventory.Items");
            Map<Integer, ItemStack> itemMap = new HashMap<>();

            for (String i : items) {
                String[] iSplit = i.split(",");
                if(iSplit.length != 3) continue;

                int slot;
                try {slot = Integer.parseInt(iSplit[0].split("Slot:")[1]);
                } catch (NumberFormatException e) {continue;}

                if(slot <= 0) {
                    getLogger().warning("Inventory slot #" + slot + " exceeds inventory size of " + refillGUISize + ". Setting to appropriate value.");
                    slot = 1;
                }
                if(slot > refillGUISize) {
                    getLogger().warning("Inventory slot #" + slot + " exceeds inventory size of " + refillGUISize + ". Setting to appropriate value.");
                    slot = refillGUISize - 1;
                }

                int itemID;

                try {
                    itemID = Integer.parseInt(iSplit[1].split(":")[1]);
                } catch (NumberFormatException e) {continue;}

                ItemStack itemStack = new ItemStack(Material.getMaterial(itemID));
                if(iSplit[1].split(":").length == 3) {
                    if (Material.getMaterial(itemID) == Material.POTION) {
                        int potType;
                        try {
                            potType = Integer.parseInt(iSplit[1].split(":")[2]);
                        } catch (NumberFormatException e) {continue;}
                        Potion potion = new Potion(PotionType.INSTANT_HEAL);
                        switch (potType) {
                            case 8226:
                                potion.setType(PotionType.SPEED);
                                potion.setLevel(2);
                                break;

                            case 16421:
                                potion.setLevel(2);
                                potion.setType(PotionType.INSTANT_HEAL);
                                potion.setSplash(true);
                                break;
                        }

                        itemStack = potion.toItemStack(1);
                    }
                }

                try {itemStack.setAmount(Integer.parseInt(iSplit[2].split("Amount: ")[1]));
                } catch (NumberFormatException e) {continue;}

                itemMap.put(slot, itemStack);
            }
            if(!itemMap.isEmpty()) refillGUI = itemMap;
        } else refillGUI = defaultItems;

        removeGlassBottles = fileConfig.getBoolean("remove-bottle", true);
        stackPotions = fileConfig.getBoolean("stack-potions", true);

        invChatMsg = ChatColor.translateAlternateColorCodes('&', fileConfig.getString("chat-message", "&c{player}'s Inventory"));
        invHoverMsg = ChatColor.translateAlternateColorCodes('&', fileConfig.getString("hover-message", "Click to view {player}'s inventory"));
    }

    public List<Inventory> getInventories() {return inventories;}
}
