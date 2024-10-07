package notme.axeofkratos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChickenParachute implements Listener {

	private Plugin plugin = Brain.getInstance();
	
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, Brain.getInstance());
        Brain.getInstance().getLogger().info(String.valueOf(getClass().getSimpleName()) + " module has been initialized! (Perm: perks.parachute)");
    }

    private final HashMap<UUID, List<Chicken>> parachute = new HashMap<UUID, List<Chicken>>();
    
	private void openParachute(Player player) {

    	if (parachute.containsKey(player.getUniqueId())) {
    		closeParachute(player);
    	}

    	if (!parachute.containsKey(player.getUniqueId())) {
    		spawnChickens(player);
    	}
    	
        player.setMetadata("parachute", new FixedMetadataValue(Brain.getInstance(), true));
    	
        new BukkitRunnable() {
			@SuppressWarnings("deprecation")
			@Override
            public void run() {
				if (player.isGliding()) {
                	closeParachute(player);
                    this.cancel();
                    return;
				}
                if (!player.isSneaking()) {
                	closeParachute(player);
                    this.cancel();
                    return;
                }
                if (player.isOnGround()) {
                	closeParachute(player);
                    this.cancel();
                    return;
                }
                if (!isInAir(player)) {
                	closeParachute(player);
                    this.cancel();
                    return;
                }
            	if (!parachute.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
            	}
                applySlowFall(player);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
    }

	private void closeParachute(Player player) {
	    removeChickens(parachute.getOrDefault(player.getUniqueId(), new ArrayList<Chicken>()));
	    parachute.remove(player.getUniqueId());
	    if (player.hasMetadata("parachute")) {
	        player.removeMetadata("parachute", Brain.getInstance());
	    }
	}

    private List<Chicken> spawnChickens(Player player) {
        List<Chicken> chickens = new ArrayList<>();
        Location playerLocation = player.getLocation();

        chickens.add(spawnChicken(playerLocation.add(0, 2, 0), player));
        chickens.add(spawnChicken(playerLocation.add(1, 2, 0), player));
        chickens.add(spawnChicken(playerLocation.add(-1, 2, 0), player));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PARROT_FLY, 1.0f, 1.0f);
        parachute.put(player.getUniqueId(), chickens);
        return chickens;
    }

    private Chicken spawnChicken(Location location, Player player) {
        Chicken chicken = player.getWorld().spawn(location, Chicken.class);
        chicken.setInvulnerable(true);
        chicken.setLeashHolder(player);
        return chicken;
    }

    private void applySlowFall(Player player) {
        Vector playerVelocity = player.getVelocity();
        player.setVelocity(new Vector(playerVelocity.getX(), -0.3, playerVelocity.getZ()));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1);
	    player.setFallDistance((float) 0);
    }

    private void removeChickens(List<Chicken> chickens) {
        for (Chicken chicken : chickens) {
        	chicken.setLeashHolder(null);
            chicken.remove();
        }
    }
    
    @EventHandler
    public void onEntityUnleash(EntityUnleashEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Chicken) {
        	Chicken chicken = (Chicken) entity;
            parachute.forEach((uuid, chickens) -> {
                if (chickens.contains(chicken)) {
                    chicken.setLeashHolder(null);
                }
            });
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && player.hasMetadata("parachute")) {
            	closeParachute(player);
                event.setCancelled(true);
            }
        }
    }

	@SuppressWarnings("deprecation")
	@EventHandler
    public void onPlayerFish(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("perks.parachute")) {
        	return;
        }
        
        if (player.isFlying()) {
        	return;
        }
        
        if (player.getGameMode() != GameMode.SURVIVAL) {
        	return;
        }
        
        if (!player.isOnGround() && isInAir(player)) {
        	openParachute(player);
        }		
    }
	
	private boolean isInAir(Player player) {
	    Location loc = player.getLocation().subtract(0, 1, 0);
	    Block blockUnder = loc.getBlock();
	    return !blockUnder.getType().isSolid();
	}
	
}
