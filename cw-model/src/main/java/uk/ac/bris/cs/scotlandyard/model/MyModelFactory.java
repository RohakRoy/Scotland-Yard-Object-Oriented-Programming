package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.*;

import java.util.ArrayList;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model{

		private GameState currentState;
		private ImmutableSet<Observer> observers;

		private MyModel(
				final GameSetup setup,
				final Player MrX,
				final ImmutableList<Player> detectives) {

			this.currentState = new MyGameStateFactory().build(setup,MrX,detectives);
			this.observers    = ImmutableSet.of();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return this.currentState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException("Observer cannot be null!");
			for(Observer check : this.observers)	// check through all the observers already present
				if(check == observer) throw new IllegalArgumentException("Observer already registered!");
			List<Observer> temp = new ArrayList<>(observers);
			temp.add(observer); // when an observer is added, add it to the list of observers in the constructor
			this.observers = ImmutableSet.copyOf(temp);	// update the set of observers in MyModel
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException();
			boolean observer_found = false;
			for(Observer check : this.observers) // iterate through all the observers to check if the one to unregister even exists or not
				if (check == observer) {
					observer_found = true;
					break;
				}
			if(!observer_found) throw new IllegalArgumentException("Observer not registered!");
			List<Observer> temp = new ArrayList<>(observers);
			temp.remove(observer);	// if observer exists, then remove it from the list of observers
			this.observers = ImmutableSet.copyOf(temp); // update the set of observers in MyModel
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() { // getter method to access the set of observers
			return this.observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) { // this method is used to choose a move and then update the board
			this.currentState = currentState.advance(move);
			for(Observer observer : this.observers){ // iterate through each and every observer in the set present in MyModel in order to update them one by one
				if(currentState.getWinner().isEmpty())	// check if there is a winner yet or not in the current state of the game
					observer.onModelChanged(getCurrentBoard(), Observer.Event.MOVE_MADE); // if there is not a winner yet, simply notify the observer that a move has been made
				else
					observer.onModelChanged(getCurrentBoard(), Observer.Event.GAME_OVER); // if a winner is declare, simply notify the observer that the game is over
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		return new MyModel(setup,mrX,detectives);
	}
}
