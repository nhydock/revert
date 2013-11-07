package com.kgp.game;

// JumperSprite.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A sprite can move left/right, jump and stand still.
 In fact, a sprite doesn't move horizontally at all, so
 the left and right movement requests only change various
 status flags, not its locx value.

 The sprite has looping images for when it is moving
 left or right, and single images for when it is
 standing still or jumping.

 The sprite stores its world coordinate in (xWorld, yWorld).

 Jumping has a rising and falling component. Rising and 
 falling can be stopped by the sprite hitting a brick.

 The sprite's movement left or right can be stopped by hitting 
 a brick.

 A sprite will start falling if it walks off a brick into space.

 Brick queries (mostly about collision detection) are sent
 to the BricksManager object.
 */

import java.awt.*;

import com.kgp.imaging.ImagesLoader;
import com.kgp.imaging.Sprite;
import com.kgp.level.BricksManager;

public class JumperSprite extends Sprite {
	private static double DURATION = 0.5; // secs
	// total time to cycle through all the images

	private static final int NOT_JUMPING = 0;
	private static final int RISING = 1;
	private static final int FALLING = 2;
	// used by vertMoveMode
	// (in J2SE 1.5 we could use a enumeration for these)

	private static final int MAX_UP_STEPS = 8;
	// max number of steps to take when rising upwards in a jump

	private int period; // in ms; the game's animation period

	private boolean isFacingRight,
					isHit,
					isStill,
					isAttacking,
					isJumping;

	private int vertMoveMode;
	/* can be NOT_JUMPING, RISING, or FALLING */
	private int vertStep; // distance to move vertically in one step
	private int upCount;

	private BricksManager brickMan;
	private int moveSize; // obtained from BricksManager

	private int normalHeight;
	private int tileHeight; //this sprite's height in tiles
	
	/*
	 * the current position of the sprite in 'world' coordinates. The x-values
	 * may be negative. The y-values will be between 0 and pHeight.
	 */
	private Point world;
	/*
	 * the current position of the sprite in the tilemap coordinates
	 */
	private Point map;

	private int mode;

	public JumperSprite(int w, int h, int brickMvSz, BricksManager bm, ImagesLoader imsLd, int p) {
		super(w / 2, h / 2, w, h, imsLd, "royer01");
		// standing center screen, facing right
		moveSize = brickMvSz;
		// the move size is the same as the bricks ribbon

		brickMan = bm;
		period = p;
		setVelocity(0, 0); // no movement

		isFacingRight = true;
		isStill = true;

		/*
		 * Adjust the sprite's y- position so it is standing on the brick at its
		 * mid x- psoition.
		 */
		this.position.y = brickMan.findFloor(0, 0, false) - getHeight();

		this.world = new Point(0, 0);
		
		//set a normal height from the initial standing position
		//this allows for landing in a somewhat natural looking animation
		this.normalHeight = this.getHeight();
		this.tileHeight = this.getHeight() / bm.getBrickHeight();

		System.out.println(this.tileHeight);
		
		vertMoveMode = NOT_JUMPING;
		vertStep = brickMan.getBrickHeight() / 2;
		// the jump step is half a brick's height
		upCount = 0;
	}

	public void moveLeft()
	/*
	 * Request that the sprite move to the left. It doesn't actually move, but
	 * changes its image and status flags.
	 */
	{
		setImage("royer_walking");
		loopImage(period, DURATION); // cycle through the images
		isFacingRight = false;
		isStill = false;
	}

	public void moveRight()
	/*
	 * Request that the sprite move to the right. It doesn't actually move, but
	 * changes its image and status flags.
	 */
	{
		setImage("royer_walking");
		loopImage(period, DURATION); // cycle through the images
		isFacingRight = true;
		isStill = false;
	}

	public void stayStill()
	/*
	 * Request that the sprite stops. It stops the image animation and sets the
	 * isStill status flag.
	 */
	{
		setImage("royer01");
		loopImage(period, DURATION);
		isStill = true;
	}

	/*
	 * The sprite is asked to jump. It sets its vertMoveMode to RISING, and
	 * changes its image. The y- position adjustment is done in updateSprite().
	 */
	public void jump()
	{
		if (vertMoveMode == NOT_JUMPING) {
			vertMoveMode = RISING;
			upCount = 0;
			this.setImage("royer_jmp");
		}
	}

	public void updateSprite()
	/*
	 * Although the sprite is not moving in the x-direction, we must still
	 * update its (xWorld, yWorld) coordinate. Also, if the sprite is jumping
	 * then its y position must be updated with moveVertically(). updateSprite()
	 * should only be called after collsion checking with willHitBrick()
	 */
	{
		if (!isStill) { // moving
			if (vertMoveMode == NOT_JUMPING) // if not jumping
				checkIfFalling(); // may have moved out into empty space
			world.x += this.stepNext();
			world.x %= brickMan.getMapWidth();
			this.map = brickMan.worldToMap(world.x, world.y);
		}

		// vertical movement has two components: RISING and FALLING
		if (vertMoveMode == RISING)
			updateRising();
		else if (vertMoveMode == FALLING)
			updateFalling();
		
		this.position.x = this.world.x;
		this.position.y = this.world.y - this.getHeight() - brickMan.getBrickHeight();
		
		this.flipX = !this.isFacingRight;
		super.updateSprite();
	} // end of updateSprite()

	private void checkIfFalling()
	/*
	 * If the left/right move has put the sprite out in thin air, then put it
	 * into falling mode.
	 */
	{
		// could the sprite move downwards if it wanted to?
		// test its center x-coord, base y-coord
		int yTrans = brickMan.checkBrickTop(world.x, world.y, vertStep);
		// System.out.println("checkIfFalling: " + yTrans);
		if (yTrans != 0) // yes it could
			vertMoveMode = FALLING; // set it to be in falling mode
	} // end of checkIfFalling()

	private void updateRising()
	/*
	 * Rising will continue until the maximum number of vertical steps is
	 * reached, or the sprite hits the base of a brick. The sprite then switches
	 * to falling mode.
	 */
	{
		if (upCount == MAX_UP_STEPS) {
			vertMoveMode = FALLING; // at top, now start falling
			upCount = 0;
		} else {
			int yTrans = brickMan.checkBrickBase(world.x, world.y - this.getHeight(), vertStep);
			world.y -= yTrans; // update position
			if (yTrans <= 0) { // hit the base of a brick
				vertMoveMode = FALLING; // start falling
				upCount = 0;
			} else { // can move upwards another step
				upCount++;
			}
		}
	} // end of updateRising()

	/**
	 * Falling will continue until the sprite hits the top of a brick. The game
	 * only allows a brick ribbon which has a complete floor, so the sprite must
	 * eventually touch down.
	 * 
	 * Falling mode can be entered without a corresponding rising sequence, for
	 * instance, when the sprite walks off a cliff.
	 */
	private void updateFalling()
	{
		int yTrans = brickMan.checkBrickTop(world.x, world.y + (this.normalHeight - this.getHeight()), vertStep);
		world.y += yTrans;
		if (yTrans < vertStep)
		{
			finishJumping();
		}
	}
	
	/**
	 * Causes Royer to advance up the tilemap if the next tile is only 1 up
	 * Does not cause Royer to think he's jumping
	 */
	private int stepNext()
	{
		Point nextBrick;
		if (this.isFacingRight)
		{
			nextBrick = brickMan.worldToMap(world.x + moveSize, world.y - 1);
		}
		else
		{
			nextBrick = brickMan.worldToMap(world.x - moveSize, world.y - 1);
		}
		
		//if the brick is the same as what we're currently on then we do nothing and just let
		// royer continue on his way across the brick
		if (nextBrick.equals(this.map) || !brickMan.brickExists(nextBrick))
			return (this.isFacingRight) ? moveSize : -moveSize;
		
		//if the next brick even exists, we check the brick above it to see if it's empty
		for (int i = 1; i < this.tileHeight; i++)
		{
			nextBrick = new Point(nextBrick.x, nextBrick.y - 1);
			
			//if it isn't, then we run into a wall and step
			if (brickMan.brickExists(nextBrick))
			{	
				this.stayStill();
				return 0;
			}
		}
		
		world.y -= brickMan.getBrickHeight(); // update position
		return (this.isFacingRight) ? moveSize : -moveSize;
	}

	private void finishJumping() {
		this.vertMoveMode = NOT_JUMPING;
		this.upCount = 0;

		if (isStill) { // change to running image, but not looping yet
			if (isFacingRight)
				setImage("royer01");
			else
				// facing left
				setImage("royer01");
		}
		else {
			setImage("royer_walking");
			loopImage(period, DURATION);
		}
	}

	public int getXWorldPosn() {
		return this.world.x;
	}

	public int getYWorldPosn() {
		return this.world.y;
	}

	public Point getWorldPosn() {
		return new Point(world);
	}

	/**
	 * @return the direction that the player is facing
	 */
	public int getDirection() {
		if (!isStill) {
			if (isFacingRight)
				return 1;
			else
				return -1;
		}
		return 0;
	}

	/**
	 * @return the current attack mode of the player
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * @param i - the attack mode to set the player to
	 */
	public void setMode(int i) {
		this.mode = i;
	}

	/**
	 * Sets attack mode to one kind higher
	 */
	public void nextMode() {
		this.mode++;
		if (this.mode > 2)
			this.mode = 0;
	}
	
	/**
	 * Sets attack mode to one kind lower
	 */
	public void prevMode() {
		this.mode--;
		if (this.mode < 0)
			this.mode = 2;
	}

	public boolean isJumping() {
		return this.vertMoveMode != NOT_JUMPING;
	}
	
	public boolean isStill() {
		return this.isStill;
	}

}

