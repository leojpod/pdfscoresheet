package com.leojpod.demo.pdfscoresheet;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ScoreSheetData {
	//Model classes :
	public static class Player implements Comparable<Player>{
		public final String firstName, lastName;
		public final int number;
		public final Team team;
		
		public Player(String firstName, String lastName, int number, Team team){
			this.firstName = firstName; this.lastName = lastName; this.number = number; this.team = team;
		}

		@Override
		public int compareTo(Player o) {
			return this.number - o.number;
		}
		
		@Override
		public String toString() {
			return "#" + number + " " + firstName.charAt(0) + ". " + lastName;
		}
	}
	public static abstract class GameEvent implements Comparable<GameEvent> {
		public static enum EventType {
			penalty, goal;
		}
		public static class Time implements Comparable<Time>{
			public final int period, minutes, seconds;

			
			public Time(int minutes, int seconds) {
				this.minutes = minutes; this.seconds = seconds;
				period = minutes / 20 + 1;
			}
			@Override
			public int compareTo(Time o) {
				return this.toSeconds() - o.toSeconds();
			}
			public int toSeconds() {
				return minutes * 60 + seconds;
			}
		}
		public final Time time;
		public GameEvent(Time time){
			this.time = time;
		}
		
		@Override
		public int compareTo(GameEvent o) {
			return this.time.compareTo(o.time);
		}
	}
	public static class Goal extends GameEvent{
		public final Player scorer;
		public final Player firstAssist, secondAssist;
		public Goal(Time time, Player scorer, Player firstAssist, Player secondAssist){
			super(time); this.scorer = scorer; this.firstAssist = firstAssist; this.secondAssist = secondAssist;
		}
		public Goal(Time time, Player scorer, Player firstAssist) {
			this(time, scorer, firstAssist, null);
		}
		public Goal(Time time, Player scorer) {
			this(time, scorer, null, null);
		}
		@Override
		public String toString() {
			String str = "Goal for " + scorer.team.name + " scored by " + scorer;
			str += (firstAssist == null) ?
					"" : (" assisted by " + firstAssist + ((secondAssist == null)?
							"" : " and " + secondAssist));
			return str;
		}
	}
	public static class Penalty extends GameEvent {
		//TODO
		public enum PenaltyType {
			TOBEDEFINED;
		}
		public Penalty(Time time, PenaltyType type){
			super(time);
//			TODO
		}
	}
	public static class Team {
		public final String name;
		public final SortedSet<Player> roaster;
		
		public Team(String name){
			this.name = name;
			this.roaster = new TreeSet<>();
		}
		public Player getPlayerByNumber(int number) {
			for (Player ply: roaster) {
				if (ply.number > number) {
					throw new IllegalArgumentException("the team " + name + " has no player #" + number);
				} else if (ply.number == number) {
					return ply;
				}
			}
			throw new IllegalArgumentException("the team " + name + " has no player #" + number);
		}
	}
	
	
	public final Team homeTeam, visitorTeam;
	public final List<GameEvent> events;
	public int homeScore, visitorScore;
	
	public ScoreSheetData(Team home, Team visitor) {
		homeTeam = home; visitorTeam = visitor;
		events = new LinkedList<>();
	}
	
	public void updateScore() {
		homeScore = visitorScore = 0;
		for(GameEvent e : events){
			if (e instanceof Goal) {
				Goal goal = (Goal) e;
				if (goal.scorer.team == homeTeam) {
					homeScore += 1;
				} else {
					// should be visitor team for as long as hockey is played only with 2 teams at a time... 
					visitorScore += 1;
				}
			} //else it doen't change the score
		}
	}
}