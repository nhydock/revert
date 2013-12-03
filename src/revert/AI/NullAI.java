package revert.AI;

import revert.Entities.Actor;

/**
 * Enemy AI that does nothing
 * @author nhydock
 */
public class NullAI implements EnemyAi{

	@Override
	public void attack(Actor a) {
		// Do Nothing
	}

	@Override
	public void inView(Actor a) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void outOfView(Actor a) {
		// Do Nothing
	}

	@Override
	public void aggress(Actor a) {
		// Do Nothing
	}

	@Override
	public float viewRange() {
		return -1;
	}

	@Override
	public float aggressRange() {
		return -1;
	}

}
