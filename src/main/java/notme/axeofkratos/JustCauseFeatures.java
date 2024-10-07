package notme.axeofkratos;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class JustCauseFeatures implements Listener {

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, Brain.getInstance());
        
        ItemStack grapplingHook = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = grapplingHook.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE+"Гак-Кішка");
        meta.setCustomModelData(7);
        grapplingHook.setItemMeta(meta);
        
        NamespacedKey key = new NamespacedKey(Brain.getInstance(), "grappling_hook");
        ShapelessRecipe recipe = new ShapelessRecipe(key, grapplingHook);
        
        recipe.addIngredient(Material.FISHING_ROD);
        recipe.addIngredient(Material.IRON_INGOT);
        
        if (Bukkit.getRecipe(key) == null) {
        	Bukkit.addRecipe(recipe);
        }
        
        Brain.getInstance().getLogger().info(String.valueOf(getClass().getSimpleName()) + " module has been initialized!");
    }
    
    @EventHandler
    public void onPlayerCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && result.getType() == Material.FISHING_ROD) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 7) {
                if (!event.getWhoClicked().hasPermission("perks.grappling_hook")) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "Придбайте перк, щоб скрафтити гак-кішку.");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCraftItem(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null && result.getType() == Material.FISHING_ROD) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 7) {
                if (!event.getView().getPlayer().hasPermission("perks.grappling_hook")) {
                    event.getView().getPlayer().sendMessage(ChatColor.RED + "Придбайте перк, щоб скрафтити гак-кішку.");
                    event.getInventory().setResult(null);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && player.hasMetadata("hook")) {
            	player.removeMetadata("hook", Brain.getInstance());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Entity hookedEntity = event.getCaught();
        Location hookLocation = event.getHook().getLocation();
        ItemMeta item = player.getInventory().getItem(event.getHand()).getItemMeta();
        if (!item.hasCustomModelData() || item.getCustomModelData() != 7) {
        	return;
        }
             
        event.getHook().setVelocity(event.getHook().getVelocity().multiply(2));
        
        if (event.getState() == PlayerFishEvent.State.IN_GROUND ||
        	event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY ||
        		(!hookLocation.add(1,0,0).getBlock().isPassable() ||
        		!hookLocation.add(0,1,0).getBlock().isPassable() ||
        		!hookLocation.add(0,0,1).getBlock().isPassable() ||
        		!hookLocation.add(-1,0,0).getBlock().isPassable() ||
        		!hookLocation.add(0,-1,0).getBlock().isPassable() ||
        		!hookLocation.add(0,0,-1).getBlock().isPassable())
        	) {

            player.setMetadata("hook", new FixedMetadataValue(Brain.getInstance(), true));

            Vector direction;
            if (hookedEntity != null) {
                direction = hookedEntity.getLocation().add(0, 3, 0).toVector().subtract(player.getLocation().toVector()).normalize();
            } else {
                direction = hookLocation.add(0, 3, 0).toVector().subtract(player.getLocation().toVector()).normalize();
            }

            double distance = player.getLocation().distance(hookedEntity != null ? hookedEntity.getLocation() : hookLocation);
            Vector velocity = direction.multiply(distance * 0.2);

            if (distance < 5) {
                velocity.setY(velocity.getY() + 1);
            }

            player.getWorld().spawnParticle(Particle.CRIT_MAGIC, player.getLocation(), 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);

            player.setVelocity(velocity);
            player.setFallDistance(0);

        }
    }
}
