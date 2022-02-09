package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private int round_number;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives,
				final int round_number) {

			for (Player check : detectives)		//the following lines of code just deals with possible errors before assigning values in the constructor
				if (check.piece().isMrX())
					throw new IllegalArgumentException("Cannot have more than one mrX!");

			for (int i = 0; i < detectives.size(); i++) {
				List<Player> temp_list = new ArrayList<>(detectives);
				temp_list.remove(i);
				for (int j = 0; j < temp_list.size(); j++)
					if (detectives.get(i).piece() == temp_list.get(j).piece())
						throw new IllegalArgumentException("Cannot have same detective twice!");
			}

			for (int i = 0; i < detectives.size(); i++) {
				List<Player> temp_list = new ArrayList<>(detectives);
				temp_list.remove(i);
				for (int j = 0; j < temp_list.size(); j++)
					if (detectives.get(i).location() == temp_list.get(j).location())
						throw new IllegalArgumentException("Two detectives cannot be at the same location");
			}

			for (Player check : detectives)
				if (check.tickets().getOrDefault(Ticket.SECRET, 0) > 0)
					throw new IllegalArgumentException("Detectives cannot have the SECRET ticket!");

			for (Player check : detectives)
				if (check.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0)
					throw new IllegalArgumentException("Detectives cannot have the DOUBLE ticket!");

			if (Objects.isNull(mrX)) throw new NullPointerException("mrX cannot be null!");
			if (Objects.isNull(detectives)) throw new NullPointerException("Detectives cannot be null!");
			if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if (!mrX.piece().isMrX()) throw new IllegalArgumentException("No mrX present!");

				this.setup = setup;
				this.remaining = remaining;
				this.log = log;
				this.mrX = mrX;
				this.detectives = detectives;
				this.round_number = round_number;

				var temp_everyone = new ArrayList<Player>(detectives); // creating a temporary list to hold every single player
				temp_everyone.add(mrX);
				this.everyone = ImmutableList.copyOf(temp_everyone);

				if(getMovesof(detectives).isEmpty() || this.round_number == getTotalNumberOfRounds()) {  //if there are no moves left for any of the detectives or if the number of rounds available is finished, then make MrX the winner.
					this.winner = ImmutableSet.of(MrX.MRX);
					this.moves  = ImmutableSet.of();
				}
				else if(this.remaining.contains(MrX.MRX) && getMovesof(List.of(mrX)).isEmpty()){  // if MrX has no valid moves available, then make the detectives the winner
					this.winner = getAllDetectivePieces();
					this.moves  = ImmutableSet.of();
				}
				else if(checkIfAnyDetectiveAtMrXLocation()){ // if any detective catches MrX, the make the detectives the winner
					this.winner = getAllDetectivePieces();
					this.moves  = ImmutableSet.of();
				}
				else if(checkIfMrXSurrounded()){ // if any MrX is surrounded by detectives and cannot make a move, make detectives the winner
					this.winner = getAllDetectivePieces();
					this.moves  = ImmutableSet.of();
				}
				else{
					this.winner = ImmutableSet.of(); // if none of the above conditions are met, then the game continues and there is no winner yet
					this.moves = allRemainingPlayersMoves();
				}
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {	// method to get a set of all the pieces in the game
			Set<Piece> all_detectives = new HashSet<>();
			all_detectives.add(mrX.piece());
			for (Player temp : detectives)
				all_detectives.add(temp.piece());
			ImmutableSet<Piece> all_players = ImmutableSet.copyOf(all_detectives);
			return all_players;
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) { // this method tells you the location of the detective passed in as argument
			Boolean detective_found = false;
			Player get_player = null;
			for (Player temp : detectives) {
				if (temp.piece() == detective) {
					get_player = temp;
					detective_found = true;
				}
			}
			if (detective_found)
				return Optional.of(get_player.location());
			else
				return Optional.empty();	// if detective does not exist then return nothing
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) { // method to provide the ticket board of a piece

			Boolean found_piece = false;
			Player player_to_get = null;

			if (mrX.piece() == piece) {  // check if the piece is MrX
				player_to_get = mrX;
				found_piece = true;
			} else {
				for (Player temp : detectives)    // if not MrX, check if the piece is a detective
					if (temp.piece() == piece) {
						player_to_get = temp;
						found_piece = true;
					}
			}
			Player finalGet_player = player_to_get;

			TicketBoard ticketBoard = new TicketBoard() { // creating an inner class to hold the ticket board
				@Override
				public int getCount(@Nonnull Ticket ticket) {	// method to return the number of the tickets passed in as argument
					return finalGet_player.tickets().getOrDefault(ticket, 0);
				}
			};

			if (found_piece)	// if the piece exists, return its ticket board
				return Optional.of(ticketBoard);
			else
				return Optional.empty(); // if piece doesn't exist, return nothing
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return this.moves;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {	// the advance method is responsible for progression of the game
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move); // check if the move is possible or not

			if(move.commencedBy().isMrX()) {	// this whole section of the method is only if MrX has made the move
				var new_log = new ArrayList<LogEntry>(this.log); // store the existing log in a new log
				var all_detectives = new ArrayList<Piece>();

				for (Player temp : this.detectives)
					all_detectives.add(temp.piece()); // create a new list consisting of all the detective pieces, this will be used to update pieces with remaining turns when returning a new game state.

				int new_location = getNewLocationAfterMove(move);
				Iterable<Ticket> tickets_used = getTicketsUsed(move);
				List<LogEntry> new_entries = newLogEntries(move);

				Player new_mrX = this.mrX.use(tickets_used); // update MrX's available tickets, location, and log
				new_mrX = new_mrX.at(new_location);
				new_log.addAll(new_entries);

				return new MyGameState(this.setup,ImmutableSet.copyOf(all_detectives),ImmutableList.copyOf(new_log),new_mrX,this.detectives,this.round_number); // update the board
			}

			if(this.remaining.size() == 1 && move.commencedBy() != MrX.MRX) { // this section is only if there is one last detective left in remaining to play its turn
				Player detective_to_update = getPlayerFromPiece(move.commencedBy()); // get the player who made the move
				var new_detective_list = new ArrayList<Player>();
				Integer new_round_number = null;

				for(Player detective : this.detectives)
					if (detective != detective_to_update)
						new_detective_list.add(detective);	// get a new list of all the detectives besides the one who made the move

				Iterable<Ticket> tickets_used = move.tickets();
				int new_location = getNewLocationAfterMove(move);
				detective_to_update = detective_to_update.use(tickets_used); // update the detective's available tickets and location
				detective_to_update = detective_to_update.at(new_location);
				new_detective_list.add(detective_to_update);	// add the updated detective to the list of all the detectives
				Player new_mrX = this.mrX.give(tickets_used);	// add the tickets used by the detective to MrX's available moves
				new_round_number = this.round_number + 1 ; // increment the round number by 1 after the detective has made its move

				return new MyGameState(this.setup, ImmutableSet.of(MrX.MRX), this.log, new_mrX, new_detective_list,new_round_number); // notice how the set of remaining passed on to the new game state is just MrX
			}
			else {	                                       // this section is only if the previous 2 conditions were not met
				var new_remaining = new ArrayList<Piece>();

				for (Piece piece : this.remaining)
					if (piece != move.commencedBy())
						new_remaining.add(piece); // update the set of remaining simply by removing the piece who just made the move

				Player detective_to_update = getPlayerFromPiece(move.commencedBy()); // get the player who made the move
				var new_detective_list = new ArrayList<Player>();

				for(Player detective : this.detectives)
					if (detective != detective_to_update)
						new_detective_list.add(detective);	// get a new list of all the detectives besides the one who made the move

				Iterable<Ticket> tickets_used = move.tickets();
				int new_location = getNewLocationAfterMove(move);

				detective_to_update = detective_to_update.use(tickets_used);	// update the detective's available tickets and location
				detective_to_update = detective_to_update.at(new_location);
				new_detective_list.add(detective_to_update);	// add the updated detective to the list of all detectives
				Player new_mrX = this.mrX.give(tickets_used);	// add the tickets used by the detective to MrX's available moves
				int new_round_number = this.round_number + 1;	// increment the round number by 1

				var remaining_players = new ArrayList<Player>();
				for(Piece piece : new_remaining)
					remaining_players.add(getPlayerFromPiece(piece)); // convert the set of pieces in remaining to a new list of the corresponding players

				if(getMovesof(remaining_players).isEmpty())		// if the players in the set of remaining have no moves left, the update remaining to consist of MrX only
					return new MyGameState(this.setup,ImmutableSet.of(MrX.MRX),this.log,new_mrX,new_detective_list,new_round_number);
				else
					return new MyGameState(this.setup,ImmutableSet.copyOf(new_remaining),this.log,new_mrX,new_detective_list,new_round_number); // otherwise, just return the new game state with new_remaining
			}
		}

		@Nonnull
		private ImmutableList<Player> getEveryone(){
			return this.everyone;
		}

		public Player getPlayerFromPiece(Piece piece){	// this method gives u the player corresponding to the piece entered as an argument
			Player player = null;
			for(Player check : this.everyone)
				if (check.piece() == piece)
					player = check;
			return player;
		}

		private ImmutableSet<Move> allRemainingPlayersMoves(){	// this method returns a set of all the available moves for all the players who are still in the set of remaining

			final var all_moves = new ArrayList<Move>();

			for(Piece temp : this.remaining) {		// add all the single moves and double moves for each of the players in remaining and store it in all_moves
				Player player = getPlayerFromPiece(temp);
				all_moves.addAll(makeSingleMoves(this.setup,this.detectives,player, player.location()));
				all_moves.addAll(makeDoubleMoves(this.setup,this.detectives,player, player.location(),this.round_number));
			}

			return ImmutableSet.copyOf(all_moves);
		}

		private ImmutableSet<Move> getMovesof(List<Player> list){ // this method will return all the available moves of the players who are part of the list which is handed in as an argument
			var temp = new ArrayList<Move>();

			for(Player player : list) {		// add all the single and double moves of the players present in the list passed as an argument and store it in temp
				temp.addAll(makeSingleMoves(this.setup, this.detectives, player, player.location()));
				temp.addAll(makeDoubleMoves(this.setup,this.detectives,player,player.location(),this.round_number));
			}

			return ImmutableSet.copyOf(temp);
		}

		private ImmutableSet<Piece> getAllDetectivePieces(){ // this method simply just converts all the players in this.detectives into a set of their corresponding pieces
			var temp = new ArrayList<Piece>();

			for(Player player : this.detectives)
				temp.add(player.piece());

			return ImmutableSet.copyOf(temp);
		}

		private boolean checkIfAnyDetectiveAtMrXLocation(){ // this method just checks if any detective is present at the same location as MrX
			boolean check = false;

			for(Player player : this.detectives)
				if(player.location() == this.mrX.location())
					check = true;

			return check;
		}

		private int getTotalNumberOfRounds() {return this.setup.rounds.size();}	// this method just returns the total number of rounds of the game

		private ImmutableSet<Integer>getAllAvailableDestinationsofMrX(){	// this method returns a set of all the possible locations MrX can travel to
			Set<Integer> set_to_return = new HashSet<Integer>();

			for(Move move : getMovesof(List.of(this.mrX))){		// for each and every single move MrX has available, I have added the list of possible destinations MrX can go to
				set_to_return.add(move.visit(new Visitor<Integer>() {
					@Override
					public Integer visit(SingleMove move) {
						return move.destination;
					} // return new destination for single move

					@Override
					public Integer visit(DoubleMove move) {
						return move.destination1;
					}	// return new destination for double move
				}));
			}
			return ImmutableSet.copyOf(set_to_return);
		}

		private Boolean checkIfMrXSurrounded(){ // this method checks if there is already a detective present at all the destinations MrX can possibly travel to.

			var bool_list = new ArrayList<Boolean>();

			for(Integer location : getAllAvailableDestinationsofMrX()){
				for(Player detective : this.detectives)
					if(location == detective.location())
						bool_list.add(true);	// for every destination at which there is already a detective present, add a boolean value to the list
			}
			if(getAllAvailableDestinationsofMrX().isEmpty())
				return false;
			else
				return bool_list.size() == getAllAvailableDestinationsofMrX().size(); // if bool_list contains as many items as the number of available destinations MrX can travel to, then he is surrounded
		}

		@Nonnull
		private List<LogEntry> newLogEntries(Move move){ // this method returns a list which contains the new updated log of MrX after a move
			var new_log = new ArrayList<LogEntry>(this.log);

			return move.visit(new Visitor<List<LogEntry>>() { // create an inner class
				@Override
				public List<LogEntry> visit(SingleMove move) { // this is if the move is a single move
					if(!setup.rounds.get(new_log.size()))	// check if the move needs to stay hidden or the location has to be revealed
						return List.of(LogEntry.hidden(move.ticket));
					else
						return List.of(LogEntry.reveal(move.ticket,move.destination));
				}
				@Override
				public List<LogEntry> visit(DoubleMove move) { // this is only if the move is a double move
					var list_to_return = new ArrayList<LogEntry>();

					if(!setup.rounds.get(new_log.size())) // check if at the current round MrX has to stay hidden or reveal himself
						list_to_return.add(LogEntry.hidden(move.ticket1));
					else
						list_to_return.add(LogEntry.reveal(move.ticket1,move.destination1));

					if(!setup.rounds.get(new_log.size() + 1))	// as it is a double move, check if the second destination of MrX has to stay hidden or be revealed
						list_to_return.add(LogEntry.hidden(move.ticket2));
					else
						list_to_return.add(LogEntry.reveal(move.ticket2,move.destination2));

					return list_to_return;
				}
			});
		}
	}

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives,0);
	}

	private static Iterable<Ticket> getTicketsUsed(Move move){	// this method returns the list of tickets used for a move given as an argument
		return move.visit(new Visitor<Iterable<Ticket>>() { // create an inner class to see if move was single or double
			@Override
			public Iterable<Ticket> visit(SingleMove move) {
				return move.tickets();
			}
			@Override
			public Iterable<Ticket> visit(DoubleMove move) {
				return move.tickets();
			}
		});
	}

	private static int getNewLocationAfterMove(Move move){	// this method returns the new location of a player it has completed a move
		return move.visit(new Visitor<Integer>() {
			@Override
			public Integer visit(SingleMove move) {
				return move.destination;
			}
			@Override
			public Integer visit(DoubleMove move) {
				return move.destination2;
			}
		});
	}

	private static ImmutableSet<SingleMove> makeSingleMoves(	// this method returns a set of all the possible single moves of the player
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source) {

		final var singleMoves = new ArrayList<SingleMove>();
		Boolean detective_at_dest = false;

			for (int destination : setup.graph.adjacentNodes(source)) { // iterate through the list of all adjacent locations to the players current location one by one
				detective_at_dest = false;
				for (Player check : detectives) {
					if (destination == check.location())
						detective_at_dest = true;
				}
				if (detective_at_dest) continue; // only go to the next line of code if there is no detective already present at the destination

				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) { // iterate through all the possible transportation routes one by one
					if (player.has(t.requiredTicket()))
						singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination)); // if the player has the tickets required then just add the move to the list of single moves to return
				}
				if (player.has(Ticket.SECRET))
					singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination)); // making sure to add the secret move if ticket for it is present
			}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<DoubleMove> makeDoubleMoves( // this method returns a set of all the possible double moves of the player
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source,
			int round_number) {

		final var singleMoves = makeSingleMoves(setup,detectives,player,source);
		final var doubleMoves = new ArrayList<DoubleMove>();

		Boolean detective_at_dest2 = false;

		if(player.has(Ticket.DOUBLE) &&  round_number < setup.rounds.size() - 1) {	// check if there is more than 1 round left for a double move to be possible

			for (SingleMove temp : singleMoves) {
				int destination1 = temp.destination;
				Ticket ticket1 = temp.ticket;

				for (int destination2 : setup.graph.adjacentNodes(destination1)) {	// iterate through the list of all adjacent locations to the first destination of the double move one by one
					detective_at_dest2 = false;
					for (Player check : detectives) {
						if (destination2 == check.location())
							detective_at_dest2 = true;
					}
					if (detective_at_dest2) continue; // only go to the next line of code if there is no detective already present

					if (player.has(Ticket.SECRET))
						doubleMoves.add(new DoubleMove(player.piece(), source, ticket1, destination1, Ticket.SECRET, destination2)); // add the secret move if player has the tickets for it

					for (Transport t : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) { // iterate through all the possible transportation routes one by one
						Ticket ticket2 = t.requiredTicket();

						if (player.has(ticket1) && player.has(ticket2)) {	// if the player has both the tickets then continue
							if (ticket1 == ticket2 && player.hasAtLeast(ticket1, 2))	// if both the tickets are the same check whether or not the player has at least 2 of that ticket
								doubleMoves.add(new DoubleMove(player.piece(), source, ticket1, destination1, ticket2, destination2)); // add the move to the list of double moves if the conditions are made

							else if (ticket1 != ticket2)
								doubleMoves.add(new DoubleMove(player.piece(), source, ticket1, destination1, ticket2, destination2)); // otherwise, if the tickets are not the same, there is no need to check if there are 2 of them present and can be added to the list directly
						}
					}
				}
			}
		}
		return ImmutableSet.copyOf(doubleMoves);
	}
}

