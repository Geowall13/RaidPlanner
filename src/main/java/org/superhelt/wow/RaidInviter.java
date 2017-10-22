package org.superhelt.wow;

import org.superhelt.wow.dao.PlayerDao;
import org.superhelt.wow.dao.RaidDao;
import org.superhelt.wow.om.Player;
import org.superhelt.wow.om.Raid;
import org.superhelt.wow.om.Signup;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RaidInviter {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RaidDao raidDao;
    private final PlayerDao playerDao;

    public RaidInviter(RaidDao raidDao, PlayerDao playerDao) {
        this.raidDao = raidDao;
        this.playerDao = playerDao;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        listRaids(writer);
        if(request.getParameter("raid")!=null) {
            LocalDate raidStart = LocalDate.parse(request.getParameter("raid"), dateFormatter);
            Raid raid = raidDao.getRaid(raidStart);

            String action = request.getParameter("action");
            if(action !=null) {
                switch (action) {
                    case "signup":
                        signup(request, writer, raid);
                        break;
                    case "unsign":
                        unsign(request, writer, raid);
                        break;
                }
            }

            raid = raidDao.getRaid(raidStart);

            printSignupForm(writer, raid);
            printSignups(writer, raid);
        }
    }

    private void printSignups(PrintWriter writer, Raid raid) {
        writer.println("<div><h1>Signups</h1><ul>");
        for(Signup signup : raid.signups) {
            writer.println("<li><form method=\"post\">");
            switch(signup.type) {
                case ACCEPTED:
                    writer.format("%s signed up for the raid", signup.player.classString());
                    break;
                case TENTATIVE:
                    writer.format("%s is tentative with the following comment: %s", signup.player.classString(), signup.comment);
                    break;
                case DECLINED:
                    writer.format("%s declined the raid with the following comment: %s", signup.player.classString(), signup.comment);
                    break;
            }
            writer.format("<input type=\"hidden\" name=\"raid\" value=\"%s\"/><input type=\"hidden\" name=\"player\" value=\"%s\"/>" +
                    "<input type=\"hidden\" name=\"action\" value=\"unsign\"><input type=\"submit\" value=\"remove\"></form></li>", dateFormatter.format(raid.start), signup.player.name);
        }

        writer.println("</ul></div>");
    }

    private void signup(HttpServletRequest request, PrintWriter writer, Raid raid) {
        String[] players = request.getParameterValues("player");
        for(String playerName : players) {
            Player player = playerDao.getByName(playerName);
            Signup.Type type = Signup.Type.valueOf(request.getParameter("type"));
            String comment = request.getParameter("comment");

            if (type != Signup.Type.ACCEPTED && (comment == null || comment.isEmpty())) {
                writer.format("<h2>Signups of type %s require a comment</h2>", type);
            } else {
                Signup signup = new Signup(LocalDateTime.now(), player, type, comment);
                raidDao.addSignup(raid, signup);
            }
        }
    }

    private void unsign(HttpServletRequest request, PrintWriter writer, Raid raid) {
        String player = request.getParameter("player");

        raidDao.removeSignup(raid, player);

    }

    private void printSignupForm(PrintWriter writer, Raid raid) {
        writer.format("<div><h1>%s</h1>", dateFormatter.format(raid.start));

        writer.format("<form method=\"post\"><input type=\"hidden\" name=\"action\" value=\"signup\"><input type=\"hidden\" name=\"raid\" value=\"%s\"/>", raid.start);

        for(Player player : playerDao.getPlayers()) {
            if(!raid.signups.stream().anyMatch(s->s.player.name.equals(player.name))) {
                writer.format("<input type=\"checkbox\" name=\"player\" value=\"%s\">%s<br/>", player.name, player.classString());
            }
        }
        writer.println("<select name=\"type\">");
        for(Signup.Type type : Signup.Type.values()) {
            writer.format("<option value=\"%s\">%s</option>", type, type);
        }
        writer.println("</select><input type=\"text\" name=\"comment\" maxlength=\"200\" placeholder=\"comment if not accepted\"/>");
        writer.println("<input type=\"submit\"></form>");
        writer.println("</div>");
    }

    public void listRaids(PrintWriter writer) {
        List<Raid> raids = raidDao.getRaids();
        writer.println("<div><h1>Raids</h1>");
        raids.forEach(r->writer.format("<a href=\"?raid=%s\">%s</a><br/>\n", r.start, r.start));
        writer.println("</div>");
    }
}
