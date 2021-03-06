package revert.MainScene;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import revert.Entities.Actor;
import revert.Entities.Bullet;
import revert.Entities.BulletFactory;
import revert.Entities.Enemy;
import revert.Entities.EnemyFactory;
import revert.Entities.Player;
import revert.MainScene.notifications.ActorsRemoved;
import revert.MainScene.notifications.PlayerAttackNotification;
import revert.MainScene.notifications.WorldNotification;
import revert.util.BrickManager;

import com.kgp.core.Game;

/**
 * World class that keeps track of all entities and alterations to the world
 * @author nhydock
 *
 */
public class World extends Observable implements Observer{

	private static int HIT_BONUS = 10;
	private static int KILL_BONUS = 100;
	
	/**
	 * The playable entity in the world
	 */
	Player player;
	
	/**
	 * All the current enemies available for attacking
	 */
	Set<Enemy> enemies;
	
	/**
	 * All the waves of enemies queued for the world
	 */
	int waves;
	int currentWave;
	
	/**
	 * All the actors added to this world
	 */
	Set<Actor> allActors;
	
	/**
	 * Projectiles sent by the player
	 */
	ArrayList<Bullet> bullets;

	//tiles of the level
	private BrickManager level;

	private EnemyFactory enemyFactory;
	private BulletFactory bulletFactory;
	
	int score;
	float time;
	int timeBonus;
	
	private boolean lock;
	
	public final float gravity;
	
	public World()
	{
		this.enemies = new HashSet<Enemy>();
		this.allActors = new HashSet<Actor>();
		this.bullets = new ArrayList<Bullet>();
		gravity = 100f;
	}
	
	/**
	 * Perform update operations on the objects in the world
	 */
	public void update()
	{
		/**
		 * Update what actors are still alive within this scene
		 */
		{
			Set<Actor> dead = new HashSet<Actor>();
			for (Actor a : allActors)
			{
				if (!a.isAlive())
				{
					if (a instanceof Enemy)
					{
						dead.add(a);
					}
				}
				else
				{
					a.updateSprite();
				}
			}
			
			for (Actor a : dead)
			{
				allActors.remove(a);
				if (a instanceof Enemy){
					enemies.remove(a);
					score += KILL_BONUS;
				}
				this.deleteObserver(a);
			}
			this.setChanged();
			this.notifyObservers(new ActorsRemoved(dead));
		}
		
		/**
		 * Update Visibility of Actors
		 */
		for (Actor a : this.allActors)
		{
			this.setChanged();
			this.notifyObservers(a);
		}
		
		/**
		 * Perform bullet update
		 */
		while (lock);
		lock = true;
		for (int i = 0; i < bullets.size();)
		{
			Bullet b = bullets.get(i);
			b.updateSprite();
			boolean dead = false;
			for (Enemy e : enemies)
			{
				if (e.hit(b))
				{
					score += HIT_BONUS;
					dead = true;
					break;
				}
			}
			
			//remove bullets that hit bricks
			if (!dead && this.level.brickExists(this.level.worldToMap(b.getXPosn(), b.getYPosn())))
			{
				dead = true;
			}
			
			if (!dead &&
			   ((b.getXPosn() > player.getPosn().x + player.getPWidth()/2) ||
			   (b.getXPosn() < player.getPosn().x - player.getPWidth()/2) ||
			   (b.getYPosn() > player.getPosn().y + player.getPHeight()/2) ||
			   (b.getYPosn() < player.getPosn().y - player.getPHeight()/2))){
				dead = true;
			}
			
			if (dead) {
				bullets.remove(i);
			}
			else {
				i++;
			}
		}
		lock = false;
		
		this.level.update(this.player.getRealXPosn(), this.player.getRealYPosn());
		
		this.time += Game.getDeltaTime();
		this.timeBonus = (int)Math.max(0, ((currentWave * 30) - time) * 1000);
		
		this.setChanged();
		this.notifyObservers(new WorldNotification(score, time));
		
		if (this.enemies.size() <= 0 && currentWave < waves && !player.isJumping())
		{
			this.startWave();
		}
	}
	
	public boolean done() {
		return this.currentWave == waves && this.enemies.size() <= 0;
	}
	
	/**
	 * Create enemies from a list of type data
	 * @param waveData - array formatted in [enemyNum][x, y, type]
	 * @return ArrayList of instantiated enemy objects in the world
	 */
	public Set<Enemy> genEnemies(int[][] waveData)
	{
		Set<Enemy> list = new HashSet<Enemy>();
		
		for (int i = 0; i < waveData.length; i++)
		{
			int[] data = waveData[i];
			Enemy e = enemyFactory.generateEnemy(data[2]);
			e.setPosition(data[0], data[1]);
			e.stop();
			list.add(e);
			this.addObserver(e);
		}
		
		return list;
	}
	
	
	/**
	 * Sets the enemies for the level
	 */
	public void startWave()
	{
		this.enemies = genEnemies(enemyFactory.createWave(5));
		
		this.allActors.clear();
		this.allActors.addAll(enemies);
		this.allActors.add(player);
		
		this.setChanged();
		this.notifyObservers(allActors);
		
		currentWave++;
	}

	/**
	 * Sets the user controlled player entity belonging to this world instance
	 * @param player
	 */
	public void setPlayer(Player player) {
		this.player = player;
		this.bulletFactory = new BulletFactory(this, player);
		this.allActors.add(player);
	}

	/**
	 * Sets the brick level of the world
	 * @param bricksMan
	 */
	public void setLevel(BrickManager bricksMan) {
		this.level = bricksMan;
		this.enemyFactory = new EnemyFactory(this, bricksMan.getSpawnPoints());
	}
	
	public BrickManager getLevel()
	{
		return this.level;
	}
	
	/**
	 * Start a new game
	 */
	public void init()
	{
		this.waves = 5;
	}

	/**
	 * @return the world's pixel width (same as the level's)
	 */
	public int getHeight() {
		return this.level.getMapHeight();
	}
	
	/**
	 * @return the world's pixel height (same as the level's)
	 */
	public int getWidth() {
		return this.level.getMapWidth();
	}

	/**
	 * @return the player being managed by the world
	 */
	public Player getPlayer() {
		return this.player;
	}
	
	/**
	 * @return the list of enemies currently being managed by the world
	 */
	public Set<Enemy> getEnemies() {
		return this.enemies;
	}
	
	/**
	 * Renders the current state of the world to the screen
	 * @param g
	 */
	public void display(Graphics2D g)
	{
		this.level.display(g);
		this.player.drawSprite(g);
		
		for (Enemy e : enemies)
		{
			e.drawSprite(g);
		}
		
		while (lock);
		lock = true;
		for (int i = 0; i < bullets.size(); i++)
		{
			Bullet b = bullets.get(i);
			b.drawSprite(g);
		}
		lock = false;
	}

	public void add(Bullet b) {
		this.bullets.add(b);
	}
	
	public void add(Enemy e) {
		this.enemies.add(e);
		this.allActors.add(e);
	}

	/**
	 * Handle notifications from the controller
	 */
	@Override
	public void update(Observable o, Object args) {
		if (o instanceof Controller)
		{
			if (args instanceof PlayerAttackNotification)
			{
				if (player.hasAmmo() && !player.isJumping())
				{
					this.add(bulletFactory.spawnBullet());
				}
			}
		}
	}
}
