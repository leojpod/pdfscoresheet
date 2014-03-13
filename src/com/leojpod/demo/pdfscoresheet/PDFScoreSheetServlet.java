package com.leojpod.demo.pdfscoresheet;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.faceless.pdf2.PDF;
import org.faceless.pdf2.PDFPage;
import org.faceless.pdf2.PDFStyle;
import org.faceless.pdf2.StandardFont;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.GameEvent.EventType;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.GameEvent.Time;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Goal;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Player;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Team;

@SuppressWarnings("serial")
public class PDFScoreSheetServlet extends HttpServlet {
	
	public PDF generatePDF(ScoreSheetData data) {
		PDF pdf = new PDF();
		PDFPage roasterPage = pdf.newPage("A4"), eventPage = pdf.newPage("A4");
		PDFStyle style = new PDFStyle();
		style.setFont(new StandardFont(StandardFont.TIMES), 24);
		style.setFillColor(Color.BLACK);
		roasterPage.setStyle(style);
		roasterPage.drawText("Hello!", 100, roasterPage.getHeight() * 0.5f);
		eventPage.setStyle(style);
		eventPage.drawText("Here will be the events", 10.0f, 5.5f);
		return pdf;
	}
	public ScoreSheetData extractDataFromRequest(HttpServletRequest req) {
		return extractDataFromParam(req.getParameter("data"));
	}
	public ScoreSheetData extractDataFromParam(String param) {
		//NOTE: for a real application you would want to have a better parameter checking than here
		//		and also a better error handling than the default GSON system. 
		JsonElement data = new JsonParser().parse(param);
//		IOException extractionError = new IOException("Incorrect json data");
//		if (!data.isJsonObject()){
//			throw extractionError;
//		}
		JsonObject match = data.getAsJsonObject();
		/* JSON for team is something like: 
		 * {	name: "Smart bobcats", 
		 * 		players: [{firstName: "John", lastName: "smith", number:42}, ...]
		 * } 
		 */
		JsonObject homeTeamJson = match.getAsJsonObject("hometeam");
		JsonObject visitorTeamJson = match.getAsJsonObject("visitorteam");
		Team home, visitor;
		
		home = new Team(homeTeamJson.get("name").getAsString());
		for (JsonElement ply: homeTeamJson.get("players").getAsJsonArray()) {
			JsonObject player = ply.getAsJsonObject();
			// JSON should look like :
			// 		{firstName: "John", lastName: "smith", number:42}
			home.roaster.add(
					new Player(player.get("firstName").getAsString(), 
							player.get("lastName").getAsString(), 
							player.get("number").getAsInt(),
							home));
		}
		visitor = new Team(visitorTeamJson.get("name").getAsString());
		for (JsonElement ply: visitorTeamJson.get("players").getAsJsonArray()) {
			JsonObject player = ply.getAsJsonObject();
			visitor.roaster.add(
					new Player(player.get("firstName").getAsString(), 
							player.get("lastName").getAsString(), 
							player.get("number").getAsInt(),
							visitor));
		}
		ScoreSheetData scoreSheetData = new ScoreSheetData(home, visitor);
		
		JsonArray events = match.getAsJsonArray("events");
		for (JsonElement evt : events) {
			JsonObject event = evt.getAsJsonObject();
			/*	JSON should look like:
			 *  	{ time:"00:15" type:"goal", info: { team: "home"/"visitor", scorer:42, 
			 *  	 with optionally: 
			 *  		firstAssist: 11, secondAssist: 71}
			 *  or 
			 *  	{ time:"55:58" type:"penalty", info: { team: "home"/"visitor", type:"high stick", minutes:"2+10",
			 *  	with optionally: 
			 *  		number: 27 }
			 */
			int minutes, seconds;
			String[] timeBuffer = event.get("time").getAsString().split(":");
			minutes = Integer.parseInt(timeBuffer[0]); seconds = Integer.parseInt(timeBuffer[1]);
			Time time = new Time(minutes, seconds);
			JsonObject info = event.getAsJsonObject("info");
			EventType type = EventType.valueOf(event.get("type").getAsString());
			switch (type) {
			case goal:
				String team = info.get("team").getAsString();
				Player scorer, firstAssist = null, secondAssist = null;
				if (team.equals("home")) {
					scorer = home.getPlayerByNumber(info.get("scorer").getAsInt());
					if (info.has("firstAssist")) {
						firstAssist = home.getPlayerByNumber(info.get("firstAssist").getAsInt());
						if (info.has("secondAssist")) {
							secondAssist = home.getPlayerByNumber(info.get("secondAssist").getAsInt());
						}
					} 
				} else { 
					scorer = visitor.getPlayerByNumber(info.get("scorer").getAsInt());
					if (info.has("firstAssist")) {
						firstAssist = visitor.getPlayerByNumber(info.get("firstAssist").getAsInt());
						if (info.has("secondAssist")) {
							secondAssist = visitor.getPlayerByNumber(info.get("secondAssist").getAsInt());
						}
					} 
				}
				scoreSheetData.events.add(new Goal(time, scorer, firstAssist, secondAssist));
				break;
			case penalty:
				throw new RuntimeException("Not Implemented yet!");
			default:
				break;
			}
		}
		return scoreSheetData;
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		OutputStream ostream = resp.getOutputStream();
		PDF pdf = generatePDF(extractDataFromRequest(req));
		pdf.render(ostream);
		
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//test run
		String testString  = "{'hometeam': {"
				+ "'name': 'bobcats', 'players': "
				+ "[{'firstName': 'john', 'lastName': 'smith', 'number': 42}, "
				+ " {'firstName': 'foo', 'lastName': 'bar', 'number': 82}, "
				+ " {'firstName': 'camel', 'lastName': 'case', 'number': 52}]"
				+ "}, "
				+ "'visitorteam': {"
				+ "'name': 'ring com', 'players': "
				+ "[{'firstName': 'sam', 'lastName': 'sagasse', 'number': 42}, "
				+ " {'firstName': 'frodon', 'lastName': 'bag', 'number': 23}, "
				+ " {'firstName': 'gimly', 'lastName': '', 'number': 99}]"
				+ "}, "
				+ "'events': ["
				+ "{ 'time': '00:15', 'type':'goal', 'info': { 'team': 'home', 'scorer':42 } },"
				+ "{ 'time': '42:15', 'type':'goal', 'info': { 'team': 'home', 'scorer':42, 'firstAssist': 82, 'secondAssist': 52} },"
				+ "{ 'time': '58:15', 'type':'goal', 'info': { 'team': 'home', 'scorer':42, 'firstAssist': 52} }"
				+ "]"
				+ "}";
		testString = testString.replace('\'', '"');
		System.out.println(testString);
		OutputStream ostream = resp.getOutputStream();
		PDF pdf = generatePDF(extractDataFromParam(testString));
		pdf.render(ostream);
		
	}
}
