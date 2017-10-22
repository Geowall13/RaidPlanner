package org.superhelt.wow;

import org.superhelt.wow.dao.PlayerDao;
import org.superhelt.wow.dao.RaidDao;
import org.superhelt.wow.om.Encounter;
import org.superhelt.wow.om.Player;
import org.superhelt.wow.om.Raid;
import org.superhelt.wow.om.Signup;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class RaidViewer {
    private final RaidDao raidDao;
    private final PlayerDao playerDao;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public RaidViewer(RaidDao raidDao, PlayerDao playerDao) {
        this.raidDao = raidDao;
        this.playerDao = playerDao;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        listRaids(response.getWriter());

        if(request.getParameter("raid")!=null) {
            LocalDate raidStart = LocalDate.parse(request.getParameter("raid"), dateFormatter);
            Raid raid = raidDao.getRaid(raidStart);

            for(Encounter encounter : raid.encounters) {
                showBoss(response.getWriter(), raid, encounter);
            }

            listAbsentees(raid, response.getWriter());
        }
    }

    private void showBoss(PrintWriter writer, Raid raid, Encounter encounter) {
        writer.format("<div style=\"float: left; width: 400px;\"><h1>%s (%d)</h1>", encounter.boss, encounter.numParticipants());

        printPlayersOfRole(raid, encounter.boss, writer, encounter, Player.Role.Tank);
        printPlayersOfRole(raid, encounter.boss, writer, encounter, Player.Role.Healer);
        printPlayersOfRole(raid, encounter.boss, writer, encounter, Player.Role.Melee);
        printPlayersOfRole(raid, encounter.boss, writer, encounter, Player.Role.Ranged);

        writer.println("</div>");
    }

    private void printPlayersOfRole(Raid raid, Encounter.Boss boss, PrintWriter writer, Encounter encounter, Player.Role role) {
        int numWithRole = encounter.getPlayersOfRole(role).size();
        if(numWithRole>0) {
            writer.format("<h2>%s (%d)</h2>", role, numWithRole);
        } else {
            writer.format("<h2>%s</h2>", role);
        }

        encounter.getPlayersOfRole(role).forEach(p->writer.format("<a href=\"?raid=%s&boss=%s&action=removePlayer&player=%s\">%s</a><br/>\n",
                dateFormatter.format(raid.start), boss, p.name, p.classString()));
    }

    public void listRaids(PrintWriter writer) {
        List<Raid> raids = raidDao.getRaids();
        writer.println("<div style=\"float: left; width: 200px\"><h1>Raids</h1>");
        raids.forEach(r->writer.format("<a href=\"?raid=%s\">%s</a><br/>\n", r.start, r.start));
        writer.println("</div>");
    }

    private void listAbsentees(Raid raid, PrintWriter writer) {
        writer.println("<div style=\"float: left\">");

        if(raid.signups.stream().filter(s->s.type== Signup.Type.TENTATIVE).count()>0) {
            writer.println("<h2>Tentative</h2><ul>");
            raid.signups.stream().filter(s -> s.type == Signup.Type.TENTATIVE).forEach(s -> writer.format("<li>%s: %s</li>", s.player.classString(), s.comment));
            writer.println("</ul>");
        }

        if(raid.signups.stream().filter(s->s.type==Signup.Type.DECLINED).count()>0) {
            writer.println("<h2>Declined</h2><ul>");
            raid.signups.stream().filter(s -> s.type == Signup.Type.DECLINED).forEach(s -> writer.format("<li>%s: %s</li>", s.player.classString(), s.comment));
            writer.println("</ul>");
        }

        List<Player> knownPlayers = raid.signups.stream().map(s -> s.player).distinct().collect(Collectors.toList());
        if(knownPlayers.size() < playerDao.getPlayers().size()) {
            writer.println("<h2>Unknown</h2><ul>");
            playerDao.getPlayers().stream().filter(p -> !knownPlayers.contains(p)).forEach(p -> writer.format("<li>%s</li>", p.classString()));
            writer.println("</ul>");
        }

        writer.println("</div>");
    }
}