package belatracker.controller;

import belatracker.model.TournamentFormat;
import belatracker.service.PlayerService;
import belatracker.service.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tournaments")
public class TournamentController {

    private final TournamentService service;
    private final PlayerService playerService;

    public TournamentController(TournamentService service, PlayerService playerService) {
        this.service = service;
        this.playerService = playerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tournaments", service.getAll());
        return "tournaments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("players", playerService.getAllPlayers());
        return "tournaments/new";
    }

    @PostMapping("/save")
    public String save(@RequestParam String name,
                       @RequestParam int targetScore,
                       @RequestParam(name = "playerIds", required = false) List<Long> playerIds,
                       RedirectAttributes ra) {
        try {
            if (playerIds == null || playerIds.isEmpty())
                throw new IllegalArgumentException("Odaberi igrače za turnir.");
            var t = service.create(name, TournamentFormat.GROUPS_KNOCKOUT, targetScore, playerIds);
            return "redirect:/tournaments/" + t.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tournaments/new";
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public String view(@PathVariable Long id, Model model) {
        try {
            service.sync(id);
            var t = service.get(id);
            model.addAttribute("tournament", t);
            model.addAttribute("groups", service.buildGroups(id));
            model.addAttribute("rounds", service.knockoutRounds(id));
            return "tournaments/view";
        } catch (Exception e) {
            model.addAttribute("error", "Greška pri učitavanju turnira: " + e.getMessage());
            model.addAttribute("tournaments", service.getAll());
            return "tournaments/list";
        }
    }

    // ---- ručni unos rezultata ----
    @GetMapping("/{id}/quick/{tmId}")
    @Transactional
    public String quickForm(@PathVariable Long id, @PathVariable Long tmId, Model model) {
        model.addAttribute("tm", service.getTournamentMatch(tmId));
        model.addAttribute("tournamentId", id);
        return "tournaments/quick-result";
    }

    @PostMapping("/{id}/quick/{tmId}")
    public String quickSave(@PathVariable Long id, @PathVariable Long tmId,
                            @RequestParam int scoreA, @RequestParam int scoreB,
                            RedirectAttributes ra) {
        try {
            service.quickResult(tmId, scoreA, scoreB);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/tournaments";
    }
}
