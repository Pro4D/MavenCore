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
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        new CopyInvCommand(this);
        inventories = new ArrayList<>();

        if(!getServer().getVersion().contains("1.8.8")) {
            useNMS = true;
            getLogger().info("Using NMS");

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
        if(Arrays.stream(sign.getLines()).noneMatch(s -> s.contains("[Refill]"))) return;

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
            String invName = ChatColor.AQUA + "[Inventory]";

            TextComponent invClick = new TextComponent(invName); //PLACEHOLDER

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
        if(event.getItem().getType().equals(Material.POTION)) {
            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> event.getPlayer().setItemInHand(new ItemStack(Material.AIR)),1L);
        }
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        ItemStack itemStack = item.getItemStack();
        if(itemStack.getType() != Material.POTION) return;
        Potion potionDropped = Potion.fromItemStack(itemStack);

        List<Entity> droppedItems = item.getNearbyEntities(8, 2, 8).stream().filter(e -> e.getType() == EntityType.DROPPED_ITEM).collect(Collectors.toList());
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

        int potionCount = droppedItems.size() + 1;

//        for(Entity e : droppedItems) {
//            Item i = (Item) e;
//            ItemStack pot = i.getItemStack();
//            if(pot.getItemMeta() != null && pot.getItemMeta().hasDisplayName()) {
//                String number = pot.getItemMeta().getDisplayName();
//                try {
//                    int count = Integer.parseInt(number) - 1;
//                    potionCount = potionCount + count;
//                } catch (NumberFormatException ignored) {}
//            }
//        }

        if(potionCount < 5) return;

        droppedItems.forEach(Entity::remove);

        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(String.valueOf(potionCount));
        itemStack.setItemMeta(meta);
        item.setItemStack(itemStack);

    }

    @EventHandler
    private void onPickup(PlayerPickupItemEvent event) {
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

                for(int c = 0; c < count;) {
                    event.getPlayer().getInventory().addItem(potionItemStack);
                    c++;
                }
                event.getItem().remove();
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
        getLogger().info("JSON: " + itemAsJsonString.toString());
        return itemAsJsonString.toString();
    }

    private Inventory createAndFillPotionInv() {
        Inventory inv = Bukkit.createInventory(null, 36);
        for(int i = 0; i < inv.getSize(); i++) {
            Potion potion = new Potion(PotionType.INSTANT_HEAL, 1);
            potion.setSplash(true);
            ItemStack item = potion.toItemStack(1);
            inv.setItem(i, item);
        }
        return inv;
    }

    public List<Inventory> getInventories() {return inventories;}

}
