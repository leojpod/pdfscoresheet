package com.leojpod.demo.pdfscoresheet;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.faceless.pdf2.PDF;
import org.faceless.pdf2.PDFPage;
import org.faceless.pdf2.PDFReader;
import org.faceless.pdf2.PDFStyle;
import org.faceless.pdf2.StandardFont;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.GameEvent.EventType;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.GameEvent.Time;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.GameEvent;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Goal;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Player;
import com.leojpod.demo.pdfscoresheet.ScoreSheetData.Team;

@SuppressWarnings("serial")
public class PDFScoreSheetServlet extends HttpServlet {
	
	public PDF generatePDF(ScoreSheetData data) throws MalformedURLException, IOException {
		data.updateScore();
		PDFReader reader = new PDFReader();
		reader.setSource(new URL("http://localhost:8080/pdfscoresheet/Template.pdf"));
		reader.load();
		PDF pdf = new PDF(reader);
		PDFPage roasterPage = pdf.getPage(0), eventPage = pdf.getPage(1);
		PDFStyle titleStyle = new PDFStyle();
		titleStyle.setFont(new StandardFont(StandardFont.HELVETICA), 36);
		titleStyle.setFillColor(Color.decode("0x44546A"));
		titleStyle.setTextAlign(PDFStyle.TEXTALIGN_CENTER);
		titleStyle.setTextLineSpacing(3);
		roasterPage.setStyle(titleStyle);
		//draw title
		String title = data.homeTeam.name + " vs " + data.visitorTeam.name;
		float x, y;
		x = roasterPage.getWidth() * 0.5f;
		y = roasterPage.getHeight() - (72 + titleStyle.getTextTop(title));
		roasterPage.drawText(title, x, y);
		title = data.homeScore + " - " + data.visitorScore;
		y -= 2 * titleStyle.getTextLineSpacing() + titleStyle.getTextTop(title);
		roasterPage.drawText(title, x, y);
		//team names
		x = 2.5f * 72;
		y = roasterPage.getHeight() - 72*3.34f + titleStyle.getTextLineSpacing() * 2;
		PDFStyle nameStyle = new PDFStyle(titleStyle);
		nameStyle.setFont(new StandardFont(StandardFont.HELVETICA), 16);
		nameStyle.setFillColor(Color.BLACK);
		roasterPage.setStyle(nameStyle);
		roasterPage.drawText(data.homeTeam.name, x, y);
		x = roasterPage.getWidth() - x;
		roasterPage.drawText(data.visitorTeam.name, x, y);
		//roaster!
		PDFStyle roasterStyle = new PDFStyle(nameStyle);
		roasterStyle.setFont(new StandardFont(StandardFont.HELVETICA), 10);
		roasterStyle.setTextAlign(PDFStyle.TEXTALIGN_TOP);
		roasterPage.setStyle(roasterStyle);
		y = roasterPage.getHeight() - 3.53f * 72 + roasterStyle.getTextLineSpacing();
		float xHomeOffset = 1.37f * 72,
				xFstName = 0.44f * 72,
				xLstName = 1.62f * 72,
				xVisitorOffset = 4.76f * 72,
				ySkip = 0.22f * 72,
				yStart = y - ySkip;
		//about to go through all the entered players
		y = yStart;
//		for(int i = 0; i < 12; i++){
		for (Player ply : data.homeTeam.roaster) { 
			roasterPage.drawText("#"+ply.number, xHomeOffset, y);
			roasterPage.drawText(ply.firstName, xHomeOffset + xFstName, y);
			roasterPage.drawText(ply.lastName, xHomeOffset + xLstName, y);
			y -= ySkip;
		}
//		}
		y = yStart;
		for (Player ply : data.visitorTeam.roaster) { 
			roasterPage.drawText("#"+ply.number, xVisitorOffset, y);
			roasterPage.drawText(ply.firstName, xVisitorOffset + xFstName, y);
			roasterPage.drawText(ply.lastName, xVisitorOffset + xLstName, y);
			y -= ySkip;
		}
		
		//deal with the events now
		PDFStyle periodStyle = new PDFStyle(roasterStyle);
		periodStyle.setTextAlign(PDFStyle.TEXTALIGN_CENTER);
		periodStyle.setTextAlign(PDFStyle.TEXTALIGN_TOP);
		periodStyle.setFont(new StandardFont(StandardFont.HELVETICABOLDOBLIQUE), 10);
		float timeOffset = 1.38f * 72,
				centerOffSet = 4.52f * 72,
				leftOffset = 1.86f * 72;
		eventPage.setStyle(roasterStyle);
		int period = 0;
		y = roasterPage.getHeight() - 1.68f * 72 + roasterStyle.getTextLineSpacing() - ySkip;
		for(GameEvent evt: data.events){
			if (evt.time.period > period) {
				//indicate the period
				period = evt.time.period;
				eventPage.setStyle(periodStyle);
				eventPage.drawText("Period #" + period, centerOffSet, y);
				y -= ySkip;
			}
			eventPage.setStyle(roasterStyle);
			eventPage.drawText(evt.time.minutes + ":" + evt.time.seconds, timeOffset, y);
			eventPage.drawText(evt.toString(), leftOffset, y);
			y -= ySkip;
		}
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
		System.out.println(match);
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
		resp.setContentType("application/force-download");
		resp.setHeader("Content-Disposition", "attachment");
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
				+ "'name': 'onion rings', 'players': "
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
